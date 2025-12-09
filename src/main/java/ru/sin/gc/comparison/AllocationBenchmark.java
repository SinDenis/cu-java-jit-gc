package ru.sin.gc.comparison;

import java.util.ArrayList;
import java.util.List;

/**
 * Allocation Rate Benchmark - тест интенсивности аллокаций
 *
 * Этот бенчмарк создает объекты с максимальной скоростью,
 * демонстрируя как разные GC справляются с высокой allocation rate.
 *
 * Позволяет увидеть:
 * - Как часто срабатывает Young GC
 * - Длительность Young GC пауз
 * - Promotion rate в Old Gen
 */
public class AllocationBenchmark {

    private static final int DURATION_SEC = 30;
    private static final int ALLOCATION_SIZE = 1024; // 1KB на объект

    private static long totalAllocated = 0;
    private static long objectsCreated = 0;

    public static void main(String[] args) {
        System.out.println("=== Allocation Rate Benchmark ===");
        System.out.println("GC: " + getGCName());
        System.out.println("Heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        System.out.println("\nТест максимальной скорости создания объектов.");
        System.out.println("Демонстрирует частоту Young GC и promotion rate.\n");

        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        long lastObjectCount = 0;

        while (System.currentTimeMillis() - startTime < DURATION_SEC * 1000) {
            // Создаем объекты
            allocateBatch(10_000);

            // Периодический отчет
            long now = System.currentTimeMillis();
            if (now - lastReportTime >= 5000) {
                long elapsedSec = (now - startTime) / 1000;
                long intervalSec = (now - lastReportTime) / 1000;
                long createdInInterval = objectsCreated - lastObjectCount;
                long allocRateMBps = (createdInInterval * ALLOCATION_SIZE) / 1024 / 1024 / intervalSec;

                System.out.printf("[%2d сек] Создано: %,d объектов, Allocation rate: %,d MB/sec%n",
                    elapsedSec,
                    objectsCreated,
                    allocRateMBps
                );

                lastReportTime = now;
                lastObjectCount = objectsCreated;
            }
        }

        // Результаты
        analyzeResults(startTime);
    }

    private static void allocateBatch(int count) {
        List<byte[]> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(new byte[ALLOCATION_SIZE]);
            objectsCreated++;
            totalAllocated += ALLOCATION_SIZE;
        }
        // batch выходит из scope и становится доступен для GC
    }

    private static void analyzeResults(long startTime) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("РЕЗУЛЬТАТЫ");
        System.out.println("=".repeat(60));

        System.out.printf("Длительность: %d секунд%n", elapsed);
        System.out.printf("Создано объектов: %,d%n", objectsCreated);
        System.out.printf("Аллоцировано памяти: %,d MB%n", totalAllocated / 1024 / 1024);
        System.out.printf("Allocation rate: %,d MB/sec%n", (totalAllocated / 1024 / 1024) / elapsed);
        System.out.printf("Object creation rate: %,d obj/sec%n", objectsCreated / elapsed);

        printGCStats(elapsed);
        printMemoryStats();
    }

    private static void printGCStats(long elapsedSec) {
        System.out.println("\nСтатистика GC:");

        long totalGCTime = 0;
        long totalGCCount = 0;

        for (java.lang.management.GarbageCollectorMXBean gc :
            java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {

            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();

            System.out.printf("  %s:%n", gc.getName());
            System.out.printf("    Коллекций: %,d%n", count);
            System.out.printf("    Время: %,d ms%n", time);

            if (count > 0) {
                System.out.printf("    Средняя пауза: %.2f ms%n", time / (double) count);
                System.out.printf("    Частота: %.2f сборок/сек%n", count / (double) elapsedSec);
            }

            totalGCTime += time;
            totalGCCount += count;
        }

        if (totalGCCount > 0) {
            double gcOverhead = (totalGCTime / (double) (elapsedSec * 1000)) * 100;
            System.out.printf("\nОбщая статистика GC:%n");
            System.out.printf("  Всего коллекций: %,d%n", totalGCCount);
            System.out.printf("  Общее время GC: %,d ms%n", totalGCTime);
            System.out.printf("  GC overhead: %.2f%%%n", gcOverhead);
            System.out.printf("  Throughput: %.2f%% (время вне GC)%n", 100 - gcOverhead);
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
