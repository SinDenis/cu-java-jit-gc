package ru.sin.gc.leak;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ПРИМЕР С УТЕЧКОЙ ПАМЯТИ #2: Listener/Callback Leak
 *
 * Проблема: Объекты подписываются на события, но никогда не отписываются.
 * Event источник хранит ссылки на всех listeners, предотвращая их сборку GC.
 *
 * Это очень распространенная утечка в GUI приложениях и event-driven архитектурах.
 */
public class ListenerLeakExample {

    // Event bus для публикации событий
    static class EventBus {
        private static final List<EventListener> listeners = new CopyOnWriteArrayList<>();

        public static void register(EventListener listener) {
            listeners.add(listener);
            // ПРОБЛЕМА: Нет механизма отписки!
        }

        public static void publish(String event) {
            for (EventListener listener : listeners) {
                listener.onEvent(event);
            }
        }

        public static int getListenerCount() {
            return listeners.size();
        }
    }

    interface EventListener {
        void onEvent(String event);
    }

    // Тяжелый объект, который хранит много данных
    static class DataProcessor implements EventListener {
        private final String id;
        private final byte[] buffer; // 10MB данных
        private final List<String> processedEvents;

        public DataProcessor(String id) {
            this.id = id;
            this.buffer = new byte[10 * 1024 * 1024]; // 10MB
            this.processedEvents = new ArrayList<>();

            // ПРОБЛЕМА: Подписываемся на события, но никогда не отписываемся
            EventBus.register(this);
        }

        @Override
        public void onEvent(String event) {
            processedEvents.add(event);
            // Обрабатываем событие
        }

        public String getId() {
            return id;
        }

        // ОТСУТСТВУЕТ метод cleanup/unregister!
    }

    public static void main(String[] args) {
        System.out.println("=== Listener Leak Example ===");
        System.out.println("Демонстрация утечки памяти через забытые listener'ы.");
        System.out.println("Объекты регистрируются как слушатели событий, но никогда не отписываются.\n");

        printMemoryInfo();
        System.out.println("\nПроблема:");
        System.out.println("✗ DataProcessor регистрируется в EventBus");
        System.out.println("✗ После использования процессор не нужен, но EventBus хранит ссылку");
        System.out.println("✗ GC не может собрать объекты, так как они \"живые\" через listener\n");

        System.out.println("Начинаем создание процессоров...\n");

        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        int totalProcessorsCreated = 0;

        try {
            // Работаем ~40 минут
            while (System.currentTimeMillis() - startTime < 40 * 60 * 1000) {

                // Создаем процессоры, которые "должны" быть временными
                for (int i = 0; i < 5; i++) {
                    DataProcessor processor = new DataProcessor("PROC_" + totalProcessorsCreated++);

                    // Имитируем использование процессора
                    EventBus.publish("Event_" + System.currentTimeMillis());

                    // ПРОБЛЕМА: После этого блока processor "должен" стать недостижимым
                    // и подлежать сборке GC, но EventBus все еще хранит на него ссылку!
                }

                // Имитируем работу
                Thread.sleep(200);

                // Публикуем события периодически
                if (new Random().nextInt(10) == 0) {
                    EventBus.publish("Periodic_Event_" + System.currentTimeMillis());
                }

                // Выводим статистику каждую минуту
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastReportTime >= 60_000) {
                    printStatus(startTime, totalProcessorsCreated);
                    lastReportTime = currentTime;
                }
            }

            System.out.println("\n✓ Программа отработала 40 минут.");
            System.out.println("Финальная статистика:");
            printStatus(startTime, totalProcessorsCreated);

        } catch (OutOfMemoryError e) {
            System.err.println("\n✗ OutOfMemoryError произошел!");
            System.err.println("Время работы: " + ((System.currentTimeMillis() - startTime) / 1000 / 60) + " минут");
            printStatus(startTime, totalProcessorsCreated);

            System.err.println("\nПричина: EventBus хранит ссылки на все созданные DataProcessor'ы.");
            System.err.println("Даже если локальные переменные вышли из области видимости,");
            System.err.println("объекты остаются \"живыми\" через listener механизм.");
            System.err.println("\nИспользуйте: jcmd " + ProcessHandle.current().pid() + " GC.heap_dump listener_leak_oom.hprof");

            throw e;

        } catch (InterruptedException e) {
            System.out.println("\nПрограмма прервана.");
            printStatus(startTime, totalProcessorsCreated);
        }
    }

    private static void printStatus(long startTime, int totalCreated) {
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 1000 / 60;

        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│ Статистика утечки (Listener Leak):                        │");
        System.out.println("├────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Время работы:           %3d минут                          │%n", elapsedMinutes);
        System.out.printf("│ Создано процессоров:    %,10d                         │%n", totalCreated);
        System.out.printf("│ Listeners в EventBus:   %,10d (!!)                    │%n", EventBus.getListenerCount());
        System.out.printf("│ Память на процессоры:   %,10d MB (~10MB каждый)      │%n",
            (long) EventBus.getListenerCount() * 10);
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
