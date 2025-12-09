package ru.sin.gc.comparison;

import java.util.*;
import java.util.concurrent.*;

/**
 * Mixed Workload Benchmark - реалистичная смешанная нагрузка
 *
 * Симулирует реальное приложение с:
 * - Короткоживущими объектами (Young Gen)
 * - Долгоживущими объектами (Old Gen)
 * - Периодическими всплесками нагрузки
 *
 * Демонстрирует как разные GC справляются с реалистичной нагрузкой.
 */
public class MixedWorkloadBenchmark {

    private static final int DURATION_SEC = 60;
    private static final int LONG_LIVED_OBJECTS = 10_000;
    private static final int BURST_INTERVAL_MS = 5000;

    // "Кеш" долгоживущих объектов
    private static final Map<String, CachedObject> cache = new ConcurrentHashMap<>();

    // Метрики
    private static final List<Long> operationLatencies = new CopyOnWriteArrayList<>();
    private static volatile long totalOperations = 0;

    static class CachedObject {
        private final String key;
        private final long createdAt;
        private final byte[] data;
        private long lastAccessed;
        private int accessCount;

        public CachedObject(String key, int size) {
            this.key = key;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessed = this.createdAt;
            this.data = new byte[size];
            this.accessCount = 0;
        }

        public void access() {
            this.lastAccessed = System.currentTimeMillis();
            this.accessCount++;
        }

        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - lastAccessed > ttlMs;
        }
    }

    static class WorkerThread extends Thread {
        private final int workerId;
        private volatile boolean running = true;
        private long localOperations = 0;

        public WorkerThread(int workerId) {
            this.workerId = workerId;
            setName("Worker-" + workerId);
        }

        @Override
        public void run() {
            Random random = new Random(workerId);

            while (running) {
                long startTime = System.nanoTime();

                // Короткоживущие объекты
                List<byte[]> shortLived = new ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    shortLived.add(new byte[1024]); // 1KB
                }

                // Работа с кешем (долгоживущие объекты)
                String key = "cache_" + random.nextInt(LONG_LIVED_OBJECTS);
                CachedObject obj = cache.computeIfAbsent(key,
                    k -> new CachedObject(k, 10 * 1024)); // 10KB
                obj.access();

                // Симулируем обработку
                int result = 0;
                for (byte[] bytes : shortLived) {
                    result += bytes.length;
                }

                long latency = System.nanoTime() - startTime;
                operationLatencies.add(latency);

                localOperations++;

                // Небольшая задержка
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }

            totalOperations += localOperations;
        }

        public void shutdown() {
            running = false;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Mixed Workload Benchmark ===");
        System.out.println("GC: " + getGCName());
        System.out.println("Heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        System.out.println("\nСимулирует реальное приложение:");
        System.out.println("- Короткоживущие объекты (Young Gen)");
        System.out.println("- Долгоживущие объекты в кеше (Old Gen)");
        System.out.println("- Периодические всплески нагрузки");
        System.out.println("- Очистка устаревших записей кеша\n");

        // Инициализация кеша
        System.out.println("Инициализация кеша...");
        for (int i = 0; i < LONG_LIVED_OBJECTS / 2; i++) {
            cache.put("cache_" + i, new CachedObject("cache_" + i, 10 * 1024));
        }

        // Запуск воркеров
        int numWorkers = Runtime.getRuntime().availableProcessors();
        System.out.println("Запуск " + numWorkers + " воркеров...\n");

        List<WorkerThread> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            WorkerThread worker = new WorkerThread(i);
            workers.add(worker);
            worker.start();
        }

        // Periodic burst generator
        Thread burstThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(BURST_INTERVAL_MS);
                    generateBurst();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "BurstGenerator");
        burstThread.start();

        // Cache cleanup thread
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10_000);
                    cleanupCache();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "CacheCleanup");
        cleanupThread.start();

        // Мониторинг
        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;

        while (System.currentTimeMillis() - startTime < DURATION_SEC * 1000) {
            Thread.sleep(5000);

            long now = System.currentTimeMillis();
            long elapsedSec = (now - startTime) / 1000;
            long intervalSec = (now - lastReportTime) / 1000;

            System.out.printf("[%2d сек] Операций: %,d, Кеш: %d объектов, Память: %d MB%n",
                elapsedSec,
                totalOperations,
                cache.size(),
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
            );

            lastReportTime = now;
        }

        // Остановка
        System.out.println("\nОстановка воркеров...");
        for (WorkerThread worker : workers) {
            worker.shutdown();
        }
        for (WorkerThread worker : workers) {
            worker.join(5000);
        }
        burstThread.interrupt();
        cleanupThread.interrupt();

        // Результаты
        analyzeResults();
    }

    private static void generateBurst() {
        // Генерируем всплеск нагрузки
        List<byte[]> burst = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            burst.add(new byte[1024]); // 10MB всплеск
        }
        // Burst data будет собран GC
    }

    private static void cleanupCache() {
        long ttl = 30_000; // 30 секунд
        int removed = 0;

        Iterator<Map.Entry<String, CachedObject>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedObject> entry = iterator.next();
            if (entry.getValue().isExpired(ttl)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("  [Cleanup] Удалено из кеша: " + removed + " объектов");
        }
    }

    private static void analyzeResults() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("РЕЗУЛЬТАТЫ");
        System.out.println("=".repeat(60));

        System.out.printf("Всего операций: %,d%n", totalOperations);
        System.out.printf("Throughput: %,.1f ops/sec%n", totalOperations / (double) DURATION_SEC);

        if (!operationLatencies.isEmpty()) {
            List<Long> sorted = new ArrayList<>(operationLatencies);
            sorted.sort(Long::compareTo);

            long sum = sorted.stream().mapToLong(Long::longValue).sum();
            double avg = sum / (double) sorted.size();

            long p50 = sorted.get(sorted.size() / 2);
            long p95 = sorted.get((int) (sorted.size() * 0.95));
            long p99 = sorted.get((int) (sorted.size() * 0.99));
            long max = sorted.get(sorted.size() - 1);

            System.out.println("\nLatency операций:");
            System.out.printf("  avg: %,8.2f μs%n", avg / 1000.0);
            System.out.printf("  p50: %,8.2f μs%n", p50 / 1000.0);
            System.out.printf("  p95: %,8.2f μs%n", p95 / 1000.0);
            System.out.printf("  p99: %,8.2f μs%n", p99 / 1000.0);
            System.out.printf("  max: %,8.2f μs%n", max / 1000.0);
        }

        System.out.printf("\nФинальный размер кеша: %,d объектов%n", cache.size());

        printMemoryStats();
        printGCStats();
    }

    private static String getGCName() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
            .stream()
            .map(gc -> gc.getName())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Unknown");
    }

    private static void printMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        System.out.println("\nИспользование памяти:");
        System.out.printf("  Max:  %,d MB%n", runtime.maxMemory() / 1024 / 1024);
        System.out.printf("  Used: %,d MB%n", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        System.out.printf("  Free: %,d MB%n", runtime.freeMemory() / 1024 / 1024);
    }

    private static void printGCStats() {
        System.out.println("\nСтатистика GC:");
        java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().forEach(gc -> {
            System.out.printf("  %s:%n", gc.getName());
            System.out.printf("    Коллекций: %,d%n", gc.getCollectionCount());
            System.out.printf("    Время: %,d ms%n", gc.getCollectionTime());
        });
    }
}
