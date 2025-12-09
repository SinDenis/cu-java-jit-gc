package ru.sin.jit;

/**
 * Демонстрация inline оптимизации JIT компилятора.
 * JIT компилятор встраивает маленькие методы прямо в вызывающий код,
 * устраняя overhead вызова метода.
 */
public class InlineOptimizationExample {

    private static final int ITERATIONS = 100_000_000;

    public static void main(String[] args) {
        System.out.println("=== Inline Optimization Example ===");
        System.out.println("Разогреваем JVM...");

        // Прогрев JVM - важно для активации JIT
        for (int i = 0; i < 20_000; i++) {
            calculateWithMethodCalls(i);
            calculateInlined(i);
        }

        System.out.println("\nНачинаем измерение после разогрева:");

        // Тест с вызовами методов (будут заинлайнены)
        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += calculateWithMethodCalls(i);
        }
        long end1 = System.nanoTime();

        // Тест с inline вычислениями
        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum2 += calculateInlined(i);
        }
        long end2 = System.nanoTime();

        System.out.println("С методами: " + (end1 - start1) / 1_000_000 + " ms (sum=" + sum1 + ")");
        System.out.println("Inline код: " + (end2 - start2) / 1_000_000 + " ms (sum=" + sum2 + ")");
        System.out.println("\nПосле JIT оптимизации производительность должна быть похожей,");
        System.out.println("так как JIT встроит маленькие методы в основной код.");
    }

    // Маленькие методы - кандидаты на inline
    private static int add(int a, int b) {
        return a + b;
    }

    private static int multiply(int a, int b) {
        return a * b;
    }

    private static int square(int x) {
        return multiply(x, x);
    }

    private static int calculateWithMethodCalls(int x) {
        int result = square(x);
        result = add(result, x);
        result = multiply(result, 2);
        return result;
    }

    private static int calculateInlined(int x) {
        // Тот же код, но без вызовов методов
        int result = x * x;
        result = result + x;
        result = result * 2;
        return result;
    }
}
