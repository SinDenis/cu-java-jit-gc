package ru.sin.jit;

/**
 * Демонстрация Dead Code Elimination - удаление мертвого кода.
 * JIT компилятор удаляет код, который не влияет на результат выполнения.
 */
public class DeadCodeEliminationExample {

    private static final int ITERATIONS = 100_000_000;

    public static void main(String[] args) {
        System.out.println("=== Dead Code Elimination Example ===");
        System.out.println("Разогреваем JVM...");

        // Прогрев
        for (int i = 0; i < 50_000; i++) {
            withDeadCode(i);
            withoutDeadCode(i);
        }

        System.out.println("\nТест 1: Код с мертвыми вычислениями");

        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += withDeadCode(i);
        }
        long end1 = System.nanoTime();

        System.out.println("С мертвым кодом: " + (end1 - start1) / 1_000_000 + " ms (sum=" + sum1 + ")");

        System.out.println("\nТест 2: Чистый код без лишних вычислений");

        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum2 += withoutDeadCode(i);
        }
        long end2 = System.nanoTime();

        System.out.println("Без мертвого кода: " + (end2 - start2) / 1_000_000 + " ms (sum=" + sum2 + ")");
        System.out.println("\nВремя выполнения должно быть одинаковым - JIT удалит неиспользуемые вычисления.");

        System.out.println("\nТест 3: Constant folding");
        testConstantFolding();
    }

    private static int withDeadCode(int x) {
        // Эти вычисления не используются
        int unused1 = x * 100;
        int unused2 = unused1 + 500;
        String unusedString = "Hello" + unused2;

        // Неиспользуемое условие
        if (x < 0) {
            int neverUsed = x * x;
        }

        // Только это возвращается
        return x * 2 + 1;
    }

    private static int withoutDeadCode(int x) {
        // Только необходимые вычисления
        return x * 2 + 1;
    }

    private static void testConstantFolding() {
        System.out.println("\nConstant Folding - JIT вычисляет константы на этапе компиляции:");

        // Прогрев
        for (int i = 0; i < 50_000; i++) {
            withConstants(i);
            optimizedConstants(i);
        }

        long start1 = System.nanoTime();
        long sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += withConstants(i);
        }
        long end1 = System.nanoTime();

        long start2 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum2 += optimizedConstants(i);
        }
        long end2 = System.nanoTime();

        System.out.println("С вычислением констант: " + (end1 - start1) / 1_000_000 + " ms");
        System.out.println("С готовыми константами: " + (end2 - start2) / 1_000_000 + " ms");
        System.out.println("JIT вычислит константы заранее и производительность будет одинаковой.");
    }

    private static int withConstants(int x) {
        int factor = 10 * 20 + 5; // 205 - константа, вычисляется на каждом вызове
        int multiplier = 3 * 7; // 21 - тоже константа
        return x * factor + multiplier;
    }

    private static int optimizedConstants(int x) {
        int factor = 205; // Уже вычислено
        int multiplier = 21; // Уже вычислено
        return x * factor + multiplier;
    }
}
