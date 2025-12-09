package ru.sin.jit;

/**
 * Демонстрация Escape Analysis оптимизации.
 * JIT может аллоцировать объекты на стеке вместо кучи, если объект не "убегает" из метода.
 * Это уменьшает нагрузку на GC и улучшает производительность.
 */
public class EscapeAnalysisExample {

    private static final int ITERATIONS = 10_000_000;

    static class Point {
        private double x;
        private double y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double distance() {
            return Math.sqrt(x * x + y * y);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Escape Analysis Example ===");
        System.out.println("Разогреваем JVM...");

        // Прогрев
        for (int i = 0; i < 50_000; i++) {
            noEscape(i, i + 1);
            escapes(i, i + 1);
        }

        System.out.println("\nТест 1: Объект не убегает из метода (может быть аллоцирован на стеке)");

        long start1 = System.nanoTime();
        double sum1 = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum1 += noEscape(i, i + 1);
        }
        long end1 = System.nanoTime();

        System.out.println("Время выполнения: " + (end1 - start1) / 1_000_000 + " ms (sum=" + sum1 + ")");
        System.out.println("JIT может аллоцировать Point на стеке или полностью его элиминировать.");

        System.out.println("\nТест 2: Объект убегает из метода (должен быть в куче)");

        long start2 = System.nanoTime();
        Point[] points = new Point[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            points[i] = escapes(i, i + 1);
        }
        long end2 = System.nanoTime();

        System.out.println("Время выполнения: " + (end2 - start2) / 1_000_000 + " ms");
        System.out.println("Объект убегает из метода, поэтому JIT должен аллоцировать его в куче.");
        System.out.println("Создано объектов: " + points.length);

        System.out.println("\nТест 3: Scalar replacement");
        testScalarReplacement();
    }

    // Объект не убегает - JIT может оптимизировать
    private static double noEscape(double x, double y) {
        Point p = new Point(x, y);
        return p.distance(); // Объект используется только внутри метода
    }

    // Объект убегает - возвращается наружу
    private static Point escapes(double x, double y) {
        return new Point(x, y); // Объект уходит из метода
    }

    private static void testScalarReplacement() {
        System.out.println("\nScalar Replacement - JIT заменяет объект на его поля:");

        // Прогрев
        for (int i = 0; i < 50_000; i++) {
            calculateDistance(i, i + 1);
        }

        long start = System.nanoTime();
        double sum = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum += calculateDistance(i, i + 1);
        }
        long end = System.nanoTime();

        System.out.println("Время: " + (end - start) / 1_000_000 + " ms");
        System.out.println("JIT может полностью убрать объект Point и работать только с x, y напрямую.");
    }

    private static double calculateDistance(double x, double y) {
        Point p = new Point(x, y);
        // JIT видит, что нам нужны только x и y, поэтому может не создавать объект вообще
        return p.x * p.x + p.y * p.y;
    }
}
