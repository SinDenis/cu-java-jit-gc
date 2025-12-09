package ru.sin.profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * НЕОПТИМИЗИРОВАННЫЙ ПРИМЕР для профилирования
 *
 * Эта программа содержит несколько performance проблем:
 * 1. Inefficient string concatenation в цикле
 * 2. Неэффективное использование коллекций
 * 3. Избыточные вычисления
 * 4. Ненужные аллокации объектов
 *
 * Цель: Использовать async-profiler для выявления hot spots
 * и оптимизации узких мест.
 */
public class SlowApplicationExample {

    private static final int ITERATIONS = 1_000_000;
    private static final int DATA_SIZE = 1000;

    public static void main(String[] args) {
        System.out.println("=== Slow Application Example ===");
        System.out.println("Эта программа содержит несколько performance проблем.");
        System.out.println("Используйте async-profiler для выявления hot spots.\n");

        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Запустите профилирование:");
        System.out.println("  ./scripts/profile_app.sh " + ProcessHandle.current().pid() + "\n");

        // Даем время пользователю запустить profiler
        System.out.println("Начинаем через 5 секунд...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long startTime = System.currentTimeMillis();

        // Запускаем неэффективные операции
        System.out.println("Запуск операций...\n");

        for (int round = 0; round < 5; round++) {
            System.out.printf("Раунд %d/%d...%n", round + 1, 5);

            // ПРОБЛЕМА 1: String concatenation в цикле
            processStrings();

            // ПРОБЛЕМА 2: Неэффективная работа с коллекциями
            processCollections();

            // ПРОБЛЕМА 3: Избыточные вычисления
            heavyComputation();

            // ПРОБЛЕМА 4: Много мелких объектов
            createManyObjects();

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.printf("  Прошло: %d сек%n", elapsed);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println("\n=== Результаты ===");
        System.out.printf("Общее время: %d мс (%.2f сек)%n", totalTime, totalTime / 1000.0);
        System.out.println("\nПрофилирование завершено!");
        System.out.println("Проанализируйте flame graph и найдите узкие места.");
    }

    // ПРОБЛЕМА 1: String concatenation в цикле (очень медленно!)
    private static void processStrings() {
        String result = "";
        for (int i = 0; i < DATA_SIZE; i++) {
            // Каждая операция += создает новый String объект
            result += "Item_" + i + ",";
        }
        // Используем результат, чтобы избежать dead code elimination
        blackhole(result.length());
    }

    // ПРОБЛЕМА 2: Неэффективная работа с коллекциями
    private static void processCollections() {
        List<Integer> numbers = new ArrayList<>();

        // Медленное добавление без initial capacity
        for (int i = 0; i < DATA_SIZE; i++) {
            numbers.add(i);
        }

        // Неэффективный поиск в списке вместо Set
        int found = 0;
        for (int i = 0; i < DATA_SIZE / 10; i++) {
            if (numbers.contains(i * 2)) {
                found++;
            }
        }

        blackhole(found);
    }

    // ПРОБЛЕМА 3: Избыточные вычисления
    private static void heavyComputation() {
        double result = 0;

        for (int i = 0; i < ITERATIONS / 10; i++) {
            // Вычисляем одно и то же значение много раз
            double value = Math.sqrt(123.456);
            result += Math.sin(value) * Math.cos(value);

            // Неэффективное деление вместо битового сдвига
            if (i % 2 == 0) {
                result += i / 2;
            }
        }

        blackhole(result);
    }

    // ПРОБЛЕМА 4: Создание множества мелких объектов
    private static void createManyObjects() {
        Map<String, DataPoint> data = new HashMap<>();

        for (int i = 0; i < DATA_SIZE; i++) {
            // Создаем новый объект при каждой итерации
            String key = new String("key_" + i); // Лишний String()
            DataPoint point = new DataPoint(i, Math.random());
            data.put(key, point);
        }

        // Неэффективная итерация
        double sum = 0;
        for (String key : data.keySet()) {
            sum += data.get(key).getValue(); // Двойной lookup
        }

        blackhole(sum);
    }

    // Простой класс данных
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
