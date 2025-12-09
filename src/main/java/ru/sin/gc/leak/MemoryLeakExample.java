package ru.sin.gc.leak;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ПРИМЕР С УТЕЧКОЙ ПАМЯТИ #1: Static Collection Leak
 *
 * Проблема: Статическая коллекция постоянно растет и никогда не очищается.
 * Это самый распространенный тип утечки памяти в Java приложениях.
 *
 * Приложение будет работать ~40 минут, постепенно заполняя память,
 * пока не произойдет OutOfMemoryError.
 */
public class MemoryLeakExample {

    // УТЕЧКА: Статическая коллекция, которая никогда не очищается
    private static final List<UserSession> ACTIVE_SESSIONS = new ArrayList<>();

    // Счетчики для статистики
    private static long totalSessionsCreated = 0;
    private static long totalDataAllocated = 0;

    static class UserSession {
        private final String sessionId;
        private final long createdAt;
        private final byte[] sessionData; // Занимает память
        private final List<String> activityLog; // Также занимает память

        public UserSession(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = System.currentTimeMillis();
            // Каждая сессия занимает ~1MB памяти
            this.sessionData = new byte[1024 * 1024]; // 1MB данных
            this.activityLog = new ArrayList<>();

            // Заполняем лог активностью
            Random random = new Random();
            for (int i = 0; i < 100; i++) {
                activityLog.add("Action_" + random.nextInt(1000) + "_at_" + System.currentTimeMillis());
            }
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void addActivity(String activity) {
            activityLog.add(activity);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Memory Leak Example (Static Collection) ===");
        System.out.println("Это приложение демонстрирует утечку памяти через статическую коллекцию.");
        System.out.println("Оно будет работать ~40 минут или до OutOfMemoryError.\n");

        printMemoryInfo();
        System.out.println("\nИнструкции:");
        System.out.println("1. Запустите приложение");
        System.out.println("2. Мониторьте память через jconsole, VisualVM или JMC");
        System.out.println("3. Снимите heap dump через: jcmd <pid> GC.heap_dump heap_leak.hprof");
        System.out.println("4. Проанализируйте heap dump в VisualVM или Eclipse MAT\n");
        System.out.println("Начинаем создание сессий...\n");

        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        int iteration = 0;

        try {
            // Работаем ~40 минут
            while (System.currentTimeMillis() - startTime < 40 * 60 * 1000) {
                iteration++;

                // Создаем новые сессии (имитируем новых пользователей)
                for (int i = 0; i < 10; i++) {
                    String sessionId = "SESSION_" + totalSessionsCreated++;
                    UserSession session = new UserSession(sessionId);

                    // ПРОБЛЕМА: Добавляем в статическую коллекцию и никогда не удаляем
                    ACTIVE_SESSIONS.add(session);
                    totalDataAllocated += 1024 * 1024; // ~1MB на сессию

                    // Имитируем активность в случайных сессиях
                    if (!ACTIVE_SESSIONS.isEmpty()) {
                        Random random = new Random();
                        int randomIndex = random.nextInt(ACTIVE_SESSIONS.size());
                        ACTIVE_SESSIONS.get(randomIndex).addActivity("Activity at " + System.currentTimeMillis());
                    }
                }

                // Имитируем обработку
                Thread.sleep(100);

                // Выводим статистику каждую минуту
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastReportTime >= 60_000) {
                    printStatus(startTime);
                    lastReportTime = currentTime;
                }
            }

            System.out.println("\n✓ Программа отработала 40 минут без ошибок.");
            System.out.println("Финальная статистика:");
            printStatus(startTime);

        } catch (OutOfMemoryError e) {
            System.err.println("\n✗ OutOfMemoryError произошел!");
            System.err.println("Время работы: " + ((System.currentTimeMillis() - startTime) / 1000 / 60) + " минут");
            printStatus(startTime);

            System.err.println("\nПричина: Статическая коллекция ACTIVE_SESSIONS продолжала расти");
            System.err.println("и никогда не очищалась, пока не заполнила всю доступную память.");

            // Пытаемся снять heap dump автоматически
            System.err.println("\nПопытка снять heap dump...");
            System.err.println("Используйте: jcmd " + ProcessHandle.current().pid() + " GC.heap_dump heap_leak_oom.hprof");

            throw e;

        } catch (InterruptedException e) {
            System.out.println("\nПрограмма прервана пользователем.");
            printStatus(startTime);
        }
    }

    private static void printStatus(long startTime) {
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 1000 / 60;

        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│ Статистика утечки памяти:                                  │");
        System.out.println("├────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Время работы:          %3d минут                           │%n", elapsedMinutes);
        System.out.printf("│ Активных сессий:       %,10d                          │%n", ACTIVE_SESSIONS.size());
        System.out.printf("│ Создано сессий всего:  %,10d                          │%n", totalSessionsCreated);
        System.out.printf("│ Аллоцировано данных:   %,10d MB                       │%n", totalDataAllocated / 1024 / 1024);
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

        System.out.printf("│ Max память:            %,10d MB                       │%n", maxMemory / 1024 / 1024);
        System.out.printf("│ Выделено JVM:          %,10d MB                       │%n", totalMemory / 1024 / 1024);
        System.out.printf("│ Используется:          %,10d MB (%.1f%%)              │%n",
            usedMemory / 1024 / 1024,
            (usedMemory * 100.0 / maxMemory));
        System.out.printf("│ Свободно:              %,10d MB                       │%n", freeMemory / 1024 / 1024);
    }
}
