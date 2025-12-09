package ru.sin.gc.leak;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ИСПРАВЛЕННАЯ ВЕРСИЯ: Listener WITHOUT Leak
 *
 * Исправления:
 * 1. Добавлен метод unregister() для отписки
 * 2. Использование WeakReference для автоматической очистки
 * 3. Паттерн try-with-resources для автоматического cleanup
 */
public class ListenerLeakFixedExample {

    // ИСПРАВЛЕНО: EventBus с механизмом отписки и weak references
    static class EventBus {
        // Используем WeakReference для автоматической очистки мертвых listeners
        private static final List<WeakReference<EventListener>> listeners = new CopyOnWriteArrayList<>();

        public static Registration register(EventListener listener) {
            WeakReference<EventListener> weakListener = new WeakReference<>(listener);
            listeners.add(weakListener);

            // Возвращаем объект Registration для возможности отписки
            return () -> unregister(weakListener);
        }

        private static void unregister(WeakReference<EventListener> weakListener) {
            listeners.remove(weakListener);
        }

        public static void publish(String event) {
            // Очищаем мертвые ссылки и публикуем события
            Iterator<WeakReference<EventListener>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<EventListener> weakListener = iterator.next();
                EventListener listener = weakListener.get();

                if (listener == null) {
                    // Объект был собран GC, удаляем weak reference
                    iterator.remove();
                } else {
                    listener.onEvent(event);
                }
            }
        }

        public static int getListenerCount() {
            // Считаем только живые listeners
            int count = 0;
            for (WeakReference<EventListener> ref : listeners) {
                if (ref.get() != null) count++;
            }
            return count;
        }

        public static void cleanup() {
            listeners.removeIf(ref -> ref.get() == null);
        }
    }

    interface EventListener {
        void onEvent(String event);
    }

    // Интерфейс для отписки
    interface Registration {
        void unregister();
    }

    // ИСПРАВЛЕНО: DataProcessor с поддержкой cleanup
    static class DataProcessor implements EventListener, AutoCloseable {
        private final String id;
        private final byte[] buffer; // 10MB данных
        private final List<String> processedEvents;
        private final Registration registration;

        public DataProcessor(String id) {
            this.id = id;
            this.buffer = new byte[10 * 1024 * 1024]; // 10MB
            this.processedEvents = new ArrayList<>();

            // ИСПРАВЛЕНО: Сохраняем Registration для последующей отписки
            this.registration = EventBus.register(this);
        }

        @Override
        public void onEvent(String event) {
            processedEvents.add(event);
        }

        public String getId() {
            return id;
        }

        // ИСПРАВЛЕНО: Явная отписка от событий
        @Override
        public void close() {
            registration.unregister();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Listener Leak FIXED Example ===");
        System.out.println("Демонстрация ИСПРАВЛЕННОЙ версии без утечки listener'ов.\n");

        printMemoryInfo();
        System.out.println("\nИсправления:");
        System.out.println("✓ Добавлен метод unregister() для явной отписки");
        System.out.println("✓ Использование WeakReference в EventBus");
        System.out.println("✓ AutoCloseable для автоматического cleanup");
        System.out.println("✓ Периодическая очистка мертвых ссылок\n");

        System.out.println("Начинаем создание процессоров...\n");

        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        long lastCleanupTime = startTime;
        int totalProcessorsCreated = 0;
        int totalProcessorsClosed = 0;

        try {
            // Работаем ~40 минут
            while (System.currentTimeMillis() - startTime < 40 * 60 * 1000) {

                // ИСПРАВЛЕНО: Используем try-with-resources для автоматического cleanup
                for (int i = 0; i < 5; i++) {
                    try (DataProcessor processor = new DataProcessor("PROC_" + totalProcessorsCreated++)) {

                        // Имитируем использование процессора
                        EventBus.publish("Event_" + System.currentTimeMillis());

                        // После выхода из блока try, processor.close() вызовется автоматически
                        totalProcessorsClosed++;
                    }
                }

                // Имитируем работу
                Thread.sleep(200);

                // Публикуем события периодически
                if (new Random().nextInt(10) == 0) {
                    EventBus.publish("Periodic_Event_" + System.currentTimeMillis());
                }

                // ИСПРАВЛЕНО: Периодическая очистка мертвых ссылок
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCleanupTime >= 30_000) {
                    EventBus.cleanup();
                    System.gc(); // Подсказка GC (не гарантирует немедленную сборку)
                    lastCleanupTime = currentTime;
                }

                // Выводим статистику каждую минуту
                if (currentTime - lastReportTime >= 60_000) {
                    printStatus(startTime, totalProcessorsCreated, totalProcessorsClosed);
                    lastReportTime = currentTime;
                }
            }

            System.out.println("\n✓ Программа успешно отработала 40 минут!");
            System.out.println("✓ Утечка памяти предотвращена благодаря правильному управлению listeners.");
            System.out.println("\nФинальная статистика:");
            printStatus(startTime, totalProcessorsCreated, totalProcessorsClosed);

        } catch (OutOfMemoryError e) {
            System.err.println("\n✗ OutOfMemoryError (не должно было случиться!)");
            printStatus(startTime, totalProcessorsCreated, totalProcessorsClosed);
            throw e;

        } catch (Exception e) {
            System.out.println("\nПрограмма прервана: " + e.getMessage());
            printStatus(startTime, totalProcessorsCreated, totalProcessorsClosed);
        }
    }

    private static void printStatus(long startTime, int totalCreated, int totalClosed) {
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 1000 / 60;

        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│ Статистика (БЕЗ утечки):                                   │");
        System.out.println("├────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Время работы:           %3d минут                          │%n", elapsedMinutes);
        System.out.printf("│ Создано процессоров:    %,10d                         │%n", totalCreated);
        System.out.printf("│ Закрыто процессоров:    %,10d                         │%n", totalClosed);
        System.out.printf("│ Активных listeners:     %,10d (должно быть ~0)        │%n", EventBus.getListenerCount());
        System.out.println("├────────────────────────────────────────────────────────────┤");
        printMemoryInfo();
        System.out.println("└────────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    private static void printMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.printf("│ Max память:             %,10d MB                      │%n", maxMemory / 1024 / 1024);
        System.out.printf("│ Выделено JVM:           %,10d MB                      │%n", totalMemory / 1024 / 1024);
        System.out.printf("│ Используется:           %,10d MB (%.1f%%)             │%n",
            usedMemory / 1024 / 1024,
            (usedMemory * 100.0 / maxMemory));
        System.out.printf("│ Свободно:               %,10d MB                      │%n", freeMemory / 1024 / 1024);
    }
}
