package ru.sin.jit;

/**
 * Демонстрация оптимизации циклов JIT компилятором.
 * Включает loop unrolling, loop hoisting и другие оптимизации.
 */
public class LoopOptimizationExample {

    private static final int ITERATIONS = 50_000_000;

    public static void main(String[] args) {
        System.out.println("=== Loop Optimization Example ===");
        System.out.println("Разогреваем JVM...");

        int[] data = new int[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }

        // Прогрев
        for (int i = 0; i < 20_000; i++) {
            sumArrayWithInvariant(data);
            sumArrayOptimized(data);
        }

        System.out.println("\nТест 1: Loop hoisting (вынос инвариантов из цикла)");

        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += sumArrayWithInvariant(data);
        }
        long end1 = System.nanoTime();

        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum2 += sumArrayOptimized(data);
        }
        long end2 = System.nanoTime();

        System.out.println("С инвариантом в цикле: " + (end1 - start1) / 1_000_000 + " ms (sum=" + sum1 + ")");
        System.out.println("Оптимизированный:      " + (end2 - start2) / 1_000_000 + " ms (sum=" + sum2 + ")");
        System.out.println("JIT должен оптимизировать оба варианта до похожей производительности.");

        System.out.println("\nТест 2: Loop unrolling (разворачивание циклов)");
        testLoopUnrolling();
    }

    // Инвариант (data.length) пересчитывается каждую итерацию
    private static int sumArrayWithInvariant(int[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum;
    }

    // Инвариант вынесен заранее
    private static int sumArrayOptimized(int[] data) {
        int sum = 0;
        int length = data.length;
        for (int i = 0; i < length; i++) {
            sum += data[i];
        }
        return sum;
    }

    private static void testLoopUnrolling() {
        int[] arr = new int[10000];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i % 100;
        }

        // Прогрев
        for (int i = 0; i < 10_000; i++) {
            simpleLoop(arr);
            unrolledLoop(arr);
        }

        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < 100_000; i++) {
            sum1 += simpleLoop(arr);
        }
        long end1 = System.nanoTime();

        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < 100_000; i++) {
            sum2 += unrolledLoop(arr);
        }
        long end2 = System.nanoTime();

        System.out.println("Обычный цикл:        " + (end1 - start1) / 1_000_000 + " ms");
        System.out.println("Развернутый цикл:    " + (end2 - start2) / 1_000_000 + " ms");
        System.out.println("JIT сам разворачивает простые циклы для лучшей производительности.");
    }

    private static long simpleLoop(int[] arr) {
        long sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }

    // Вручную развернутый цикл (обрабатывает 4 элемента за итерацию)
    private static long unrolledLoop(int[] arr) {
        long sum = 0;
        int i = 0;
        int length = arr.length;

        // Обработка по 4 элемента
        for (; i < length - 3; i += 4) {
            sum += arr[i];
            sum += arr[i + 1];
            sum += arr[i + 2];
            sum += arr[i + 3];
        }

        // Обработка оставшихся элементов
        for (; i < length; i++) {
            sum += arr[i];
        }

        return sum;
    }
}
