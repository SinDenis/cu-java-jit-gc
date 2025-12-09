package ru.sin.jit;

import java.util.Random;

/**
 * Демонстрация Branch Prediction и влияния предсказания ветвлений на производительность.
 * JIT компилятор использует профилирование для оптимизации горячих путей кода.
 */
public class BranchPredictionExample {

    private static final int SIZE = 32768;
    private static final int ITERATIONS = 10000;

    public static void main(String[] args) {
        System.out.println("=== Branch Prediction Example ===");

        int[] sortedData = new int[SIZE];
        int[] randomData = new int[SIZE];
        Random random = new Random(42);

        for (int i = 0; i < SIZE; i++) {
            sortedData[i] = i;
            randomData[i] = random.nextInt(256);
        }

        System.out.println("Разогреваем JVM...");

        // Прогрев на отсортированных данных
        for (int i = 0; i < 1000; i++) {
            sumIfGreaterThan128(sortedData);
        }

        System.out.println("\nТест 1: Отсортированный массив (предсказуемые ветвления)");

        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += sumIfGreaterThan128(sortedData);
        }
        long end1 = System.nanoTime();

        System.out.println("Время: " + (end1 - start1) / 1_000_000 + " ms (sum=" + sum1 + ")");
        System.out.println("Ветвления предсказуемы: сначала все false, потом все true");

        // Прогрев на случайных данных
        for (int i = 0; i < 1000; i++) {
            sumIfGreaterThan128(randomData);
        }

        System.out.println("\nТест 2: Случайный массив (непредсказуемые ветвления)");

        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum2 += sumIfGreaterThan128(randomData);
        }
        long end2 = System.nanoTime();

        System.out.println("Время: " + (end2 - start2) / 1_000_000 + " ms (sum=" + sum2 + ")");
        System.out.println("Ветвления непредсказуемы: случайные true/false");

        double diff = ((double)(end2 - start2) / (end1 - start1) - 1) * 100;
        System.out.println("\nРазница: " + String.format("%.1f", diff) + "%");
        System.out.println("Случайные данные медленнее из-за branch misprediction.");

        System.out.println("\nТест 3: Избежание ветвлений через битовые операции");
        testBranchFree();
    }

    private static long sumIfGreaterThan128(int[] data) {
        long sum = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] >= 128) {
                sum += data[i];
            }
        }
        return sum;
    }

    // Версия без ветвлений - использует битовые маски
    private static long sumIfGreaterThan128BranchFree(int[] data) {
        long sum = 0;
        for (int i = 0; i < data.length; i++) {
            // Если data[i] >= 128, то (data[i] - 128) >> 31 будет 0, иначе -1 (все биты 1)
            // ~mask даст нам 1 (все биты 1) если >= 128, иначе 0
            int t = data[i] - 128;
            int mask = ~(t >> 31); // 0 если < 128, -1 (все биты 1) если >= 128
            sum += data[i] & mask;
        }
        return sum;
    }

    private static void testBranchFree() {
        int[] randomData = new int[SIZE];
        Random random = new Random(42);
        for (int i = 0; i < SIZE; i++) {
            randomData[i] = random.nextInt(256);
        }

        // Прогрев
        for (int i = 0; i < 1000; i++) {
            sumIfGreaterThan128(randomData);
            sumIfGreaterThan128BranchFree(randomData);
        }

        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += sumIfGreaterThan128(randomData);
        }
        long end1 = System.nanoTime();

        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum2 += sumIfGreaterThan128BranchFree(randomData);
        }
        long end2 = System.nanoTime();

        System.out.println("С ветвлениями:      " + (end1 - start1) / 1_000_000 + " ms (sum=" + sum1 + ")");
        System.out.println("Без ветвлений:      " + (end2 - start2) / 1_000_000 + " ms (sum=" + sum2 + ")");
        System.out.println("Версия без ветвлений может быть быстрее на непредсказуемых данных.");
    }
}
