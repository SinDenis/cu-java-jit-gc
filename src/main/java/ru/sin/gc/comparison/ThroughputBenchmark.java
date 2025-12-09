package ru.sin.gc.comparison;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Throughput Benchmark - оптимизация для максимальной производительности
 *
 * Этот бенчмарк создает много объектов и выполняет вычисления.
 * Цель: максимизировать общую производительность (operations/sec)
 * Паузы GC здесь менее критичны.
 *
 * Лучшие GC для throughput:
 * - Parallel GC (лучший throughput, но длинные паузы)
 * - G1 GC (хороший баланс)
 */
public class ThroughputBenchmark {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 5;
    private static final int OPERATIONS_PER_ITERATION = 100_000;

    static class DataPoint {
        private final long timestamp;
        private final double value;
        private final String label;
        private final byte[] payload; // Занимает память

        public DataPoint(long timestamp, double value, String label) {
            this.timestamp = timestamp;
            this.value = value;
            this.label = label;
            this.payload = new byte[1024]; // 1KB на объект
        }

        public double compute() {
            return Math.sin(value) * Math.cos(timestamp) + label.hashCode();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Throughput Benchmark ===");
        System.out.println("GC: " + getGCName());
        System.out.println("Heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        System.out.println("\nЦель: Максимизировать throughput (операций в секунду)");
        System.out.println("Паузы GC менее критичны.\n");

        // Прогрев
        System.out.println("Прогрев JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runIteration(OPERATIONS_PER_ITERATION / 10);
        }

        // Бенчмарк
        System.out.println("\nЗапуск бенчмарка...\n");

        long totalOperations = 0;
        long totalTime = 0;
        List<Long> iterationTimes = new ArrayList<>();

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            long ops = runIteration(OPERATIONS_PER_ITERATION);
            long endTime = System.nanoTime();

            long elapsed = endTime - startTime;
            iterationTimes.add(elapsed);

            totalOperations += ops;
            totalTime += elapsed;

            double opsPerSec = (ops * 1_000_000_000.0) / elapsed;
            System.out.printf("Итерация %2d: %,10d ops, %,8d ms, %,.0f ops/sec%n",
                i + 1, ops, elapsed / 1_000_000, opsPerSec);
        }

        // Статистика
        System.out.println("\n" + "=".repeat(60));
        System.out.println("РЕЗУЛЬТАТЫ");
        System.out.println("=".repeat(60));

        double avgOpsPerSec = (totalOperations * 1_000_000_000.0) / totalTime;
        System.out.printf("Общая производительность: %,.0f ops/sec%n", avgOpsPerSec);
        System.out.printf("Всего операций: %,d%n", totalOperations);
        System.out.printf("Общее время: %,d ms%n", totalTime / 1_000_000);

        // Статистика по итерациям
        iterationTimes.sort(Long::compareTo);
        long p50 = iterationTimes.get(iterationTimes.size() / 2);
        long p95 = iterationTimes.get((int) (iterationTimes.size() * 0.95));
        long p99 = iterationTimes.get((int) (iterationTimes.size() * 0.99));
        long max = iterationTimes.get(iterationTimes.size() - 1);

        System.out.println("\nВремя на итерацию:");
        System.out.printf("  p50: %,6d ms%n", p50 / 1_000_000);
        System.out.printf("  p95: %,6d ms%n", p95 / 1_000_000);
        System.out.printf("  p99: %,6d ms%n", p99 / 1_000_000);
        System.out.printf("  max: %,6d ms%n", max / 1_000_000);

        printMemoryStats();
    }

    private static long runIteration(int operations) {
        List<DataPoint> dataPoints = new ArrayList<>(operations);
        long computed = 0;

        // Создаем много объектов
        for (int i = 0; i < operations; i++) {
            DataPoint dp = new DataPoint(
                System.nanoTime(),
                Math.random() * 1000,
                "data_" + i
            );
            dataPoints.add(dp);

            // Выполняем вычисления
            if (i % 100 == 0) {
                for (DataPoint point : dataPoints) {
                    computed += (long) point.compute();
                }
            }
        }

        // Финальные вычисления
        for (DataPoint point : dataPoints) {
            computed += (long) point.compute();
        }

        return operations;
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
