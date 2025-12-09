package ru.sin.profiling;

import java.util.*;

/**
 * ОПТИМИЗИРОВАННАЯ ВЕРСИЯ после анализа flame graph
 *
 * Исправления на основе профилирования:
 * 1. ✓ Использование StringBuilder вместо String concatenation
 * 2. ✓ Эффективное использование коллекций (initial capacity, Set вместо List)
 * 3. ✓ Кэширование вычислений и битовые операции
 * 4. ✓ Переиспользование объектов, правильная итерация по Map
 *
 * Ожидаемый результат: ~5-10x ускорение
 */
public class OptimizedApplicationExample {

    private static final int ITERATIONS = 1_000_000;
    private static final int DATA_SIZE = 1000;

    // ОПТИМИЗАЦИЯ: Кэшируем часто используемые значения
    private static final double CACHED_SQRT = Math.sqrt(123.456);

    public static void main(String[] args) {
        System.out.println("=== Optimized Application Example ===");
        System.out.println("Оптимизированная версия с исправленными performance проблемами.\n");

        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Запустите профилирование:");
        System.out.println("  ./scripts/profile_app.sh " + ProcessHandle.current().pid() + "\n");

        System.out.println("Начинаем через 5 секунд...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long startTime = System.currentTimeMillis();

        System.out.println("Запуск оптимизированных операций...\n");

        for (int round = 0; round < 5; round++) {
            System.out.printf("Раунд %d/%d...%n", round + 1, 5);

            // ИСПРАВЛЕНО: Эффективная работа со строками
            processStringsOptimized();

            // ИСПРАВЛЕНО: Эффективная работа с коллекциями
            processCollectionsOptimized();

            // ИСПРАВЛЕНО: Оптимизированные вычисления
            heavyComputationOptimized();

            // ИСПРАВЛЕНО: Минимизация аллокаций
            createObjectsOptimized();

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.printf("  Прошло: %d сек%n", elapsed);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("\n=== Результаты ===");
        System.out.printf("Общее время: %d мс (%.2f сек)%n", totalTime, totalTime / 1000.0);
        System.out.println("\n✓ Оптимизация успешна!");
        System.out.println("Сравните время выполнения и flame graph с неоптимизированной версией.");
    }

    // ИСПРАВЛЕНО: Использование StringBuilder
    private static void processStringsOptimized() {
        StringBuilder result = new StringBuilder(DATA_SIZE * 20); // Pre-allocate capacity
        for (int i = 0; i < DATA_SIZE; i++) {
            result.append("Item_").append(i).append(',');
        }
        blackhole(result.length());
    }

    // ИСПРАВЛЕНО: Использование Set для быстрого поиска + initial capacity
    private static void processCollectionsOptimized() {
        // Pre-allocate capacity to avoid resizing
        Set<Integer> numbers = new HashSet<>(DATA_SIZE);

        for (int i = 0; i < DATA_SIZE; i++) {
            numbers.add(i);
        }

        // O(1) lookup в Set вместо O(n) в List
        int found = 0;
        for (int i = 0; i < DATA_SIZE / 10; i++) {
            if (numbers.contains(i * 2)) {
                found++;
            }
        }

        blackhole(found);
    }

    // ИСПРАВЛЕНО: Кэширование значений и битовые операции
    private static void heavyComputationOptimized() {
        double result = 0;

        // Кэшируем sin и cos от константы
        double sinValue = Math.sin(CACHED_SQRT);
        double cosValue = Math.cos(CACHED_SQRT);
        double product = sinValue * cosValue;

        for (int i = 0; i < ITERATIONS / 10; i++) {
            // Используем кэшированное значение
            result += product;

            // Битовый сдвиг вместо деления
            if ((i & 1) == 0) { // Эквивалентно i % 2 == 0
                result += i >> 1; // Эквивалентно i / 2
            }
        }

        blackhole(result);
    }

    // ИСПРАВЛЕНО: Переиспользование объектов и правильная итерация по Map
    private static void createObjectsOptimized() {
        // Pre-allocate capacity
        Map<String, DataPoint> data = new HashMap<>(DATA_SIZE);

        for (int i = 0; i < DATA_SIZE; i++) {
            // Используем string literal (intern pool) вместо new String()
            String key = "key_" + i;
            DataPoint point = new DataPoint(i, Math.random());
            data.put(key, point);
        }

        // Эффективная итерация через entrySet() - один lookup
        double sum = 0;
        for (Map.Entry<String, DataPoint> entry : data.entrySet()) {
            sum += entry.getValue().getValue();
        }

        blackhole(sum);
    }

    static class DataPoint {
        private final int id;
        private final double value;

        public DataPoint(int id, double value) {
            this.id = id;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public double getValue() {
            return value;
        }
    }

    // Prevent dead code elimination
    private static void blackhole(Object obj) {
        if (System.currentTimeMillis() < 0) {
            System.out.println(obj);
        }
    }

    private static void blackhole(double value) {
        if (System.currentTimeMillis() < 0) {
            System.out.println(value);
        }
    }

    private static void blackhole(int value) {
        if (System.currentTimeMillis() < 0) {
            System.out.println(value);
        }
    }
}
