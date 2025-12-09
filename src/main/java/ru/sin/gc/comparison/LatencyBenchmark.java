package ru.sin.gc.comparison;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Latency Benchmark - оптимизация для минимальных пауз
 *
 * Этот бенчмарк симулирует latency-sensitive приложение (например, REST API).
 * Важны короткие и предсказуемые паузы GC.
 *
 * Лучшие GC для low latency:
 * - ZGC (паузы < 10ms независимо от размера heap)
 * - Shenandoah GC (паузы < 10ms)
 * - G1 GC (с правильной настройкой: -XX:MaxGCPauseMillis=50)
 */
public class LatencyBenchmark {

    private static final int WARMUP_DURATION_SEC = 5;
    private static final int BENCHMARK_DURATION_SEC = 30;
    private static final int REQUEST_INTERVAL_MICROS = 100; // 10,000 req/sec

    // Размер "запроса" - будет аллоцировать память
    private static final int REQUEST_SIZE = 10; // объектов на запрос

    private static final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

    static class Request {
        private final long startTime;
        private final List<byte[]> data;

        public Request() {
            this.startTime = System.nanoTime();
            this.data = new ArrayList<>(REQUEST_SIZE);
            // Аллоцируем память (симулируем обработку запроса)
            for (int i = 0; i < REQUEST_SIZE; i++) {
                data.add(new byte[1024]); // 1KB на объект
            }
        }

        public Response process() {
            // Симулируем обработку
            int result = 0;
            for (byte[] bytes : data) {
                result += bytes.length;
            }
            return new Response(startTime, result);
        }
    }

    static class Response {
        private final long startTime;
        private final int result;

        public Response(long startTime, int result) {
            this.startTime = startTime;
            this.result = result;
        }

        public long getLatencyNanos() {
            return System.nanoTime() - startTime;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Latency Benchmark ===");
        System.out.println("GC: " + getGCName());
        System.out.println("Heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        System.out.println("\nЦель: Минимизировать latency (время отклика)");
        System.out.println("Throughput вторичен.\n");

        // Прогрев
        System.out.println("Прогрев JVM (" + WARMUP_DURATION_SEC + " сек)...");
        runBenchmark(WARMUP_DURATION_SEC, false);
        latencies.clear();

        // Бенчмарк
        System.out.println("\nЗапуск бенчмарка (" + BENCHMARK_DURATION_SEC + " сек)...");
        long totalRequests = runBenchmark(BENCHMARK_DURATION_SEC, true);

        // Анализ результатов
        analyzeResults(totalRequests);
    }

    private static long runBenchmark(int durationSec, boolean collect) throws InterruptedException {
        long startTime = System.nanoTime();
        long endTime = startTime + (durationSec * 1_000_000_000L);
        long requestCount = 0;
        long nextRequestTime = startTime;

        while (System.nanoTime() < endTime) {
            // Ожидаем следующего "запроса"
            long now = System.nanoTime();
            if (now < nextRequestTime) {
                long sleepNanos = nextRequestTime - now;
                if (sleepNanos > 1000) {
                    Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                }
                continue;
            }

            // Обрабатываем запрос
            Request req = new Request();
            Response resp = req.process();

            if (collect) {
                latencies.add(resp.getLatencyNanos());
            }

            requestCount++;
            nextRequestTime += REQUEST_INTERVAL_MICROS * 1000; // в наносекундах

            // Периодический вывод прогресса
            if (collect && requestCount % 10000 == 0) {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000_000;
                System.out.printf("  %d сек: %,d запросов обработано%n", elapsed, requestCount);
            }
        }

        return requestCount;
    }

    private static void analyzeResults(long totalRequests) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("РЕЗУЛЬТАТЫ");
        System.out.println("=".repeat(60));

        List<Long> sortedLatencies = new ArrayList<>(latencies);
        sortedLatencies.sort(Long::compareTo);

        long sum = 0;
        for (Long latency : sortedLatencies) {
            sum += latency;
        }

        double avgLatency = sum / (double) sortedLatencies.size();
        long p50 = sortedLatencies.get(sortedLatencies.size() / 2);
        long p90 = sortedLatencies.get((int) (sortedLatencies.size() * 0.90));
        long p95 = sortedLatencies.get((int) (sortedLatencies.size() * 0.95));
        long p99 = sortedLatencies.get((int) (sortedLatencies.size() * 0.99));
        long p999 = sortedLatencies.get((int) (sortedLatencies.size() * 0.999));
        long max = sortedLatencies.get(sortedLatencies.size() - 1);

        System.out.printf("Всего запросов: %,d%n", totalRequests);
        System.out.printf("Throughput: %,.0f req/sec%n", totalRequests / (double) BENCHMARK_DURATION_SEC);

        System.out.println("\nLatency (время отклика):");
        System.out.printf("  avg:  %,8.2f μs%n", avgLatency / 1000.0);
        System.out.printf("  p50:  %,8.2f μs%n", p50 / 1000.0);
        System.out.printf("  p90:  %,8.2f μs%n", p90 / 1000.0);
        System.out.printf("  p95:  %,8.2f μs%n", p95 / 1000.0);
        System.out.printf("  p99:  %,8.2f μs  ← ВАЖНО для latency-sensitive!%n", p99 / 1000.0);
        System.out.printf("  p999: %,8.2f μs%n", p999 / 1000.0);
        System.out.printf("  max:  %,8.2f μs%n", max / 1000.0);

        // Подсчет запросов с high latency
        long highLatencyThreshold = 1_000_000; // 1ms
        long highLatencyCount = sortedLatencies.stream()
            .filter(l -> l > highLatencyThreshold)
            .count();

        System.out.printf("\nЗапросы с latency > 1ms: %,d (%.2f%%)%n",
            highLatencyCount,
            (highLatencyCount * 100.0) / sortedLatencies.size());

        // Анализ GC пауз
        analyzeGCImpact(sortedLatencies);

        printMemoryStats();
    }

    private static void analyzeGCImpact(List<Long> sortedLatencies) {
        // Ищем "всплески" latency (вероятно GC паузы)
        long gcPauseThreshold = 10_000_000; // 10ms
        long suspectedGCPauses = sortedLatencies.stream()
            .filter(l -> l > gcPauseThreshold)
            .count();

        if (suspectedGCPauses > 0) {
            System.out.printf("\nВозможные GC паузы (latency > 10ms): %,d (%.3f%%)%n",
                suspectedGCPauses,
                (suspectedGCPauses * 100.0) / sortedLatencies.size());

            long maxGCPause = sortedLatencies.stream()
                .filter(l -> l > gcPauseThreshold)
                .max(Long::compareTo)
                .orElse(0L);

            System.out.printf("Максимальная пауза: %.2f ms%n", maxGCPause / 1_000_000.0);
        }
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
}
