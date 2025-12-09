package ru.sin.gc.leak;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * ИСПРАВЛЕННАЯ ВЕРСИЯ: Static Collection WITHOUT Leak
 *
 * Исправления:
 * 1. Добавлено удаление старых сессий (TTL - time to live)
 * 2. Добавлено ограничение на максимальное количество сессий
 * 3. Периодическая очистка устаревших данных
 *
 * Это приложение будет работать стабильно и не вызовет OutOfMemoryError.
 */
public class MemoryLeakFixedExample {

    // ИСПРАВЛЕНО: Все еще статическая коллекция, но теперь с управлением размером
    private static final List<UserSession> ACTIVE_SESSIONS = new ArrayList<>();

    // Конфигурация для предотвращения утечки
    private static final int MAX_SESSIONS = 500; // Максимум сессий в памяти
    private static final long SESSION_TTL = 5 * 60 * 1000; // 5 минут TTL

    // Счетчики для статистики
    private static long totalSessionsCreated = 0;
    private static long totalSessionsRemoved = 0;
    private static long totalDataAllocated = 0;

    static class UserSession {
        private final String sessionId;
        private final long createdAt;
        private long lastAccessTime;
        private final byte[] sessionData;
        private final List<String> activityLog;

        public UserSession(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessTime = this.createdAt;
            this.sessionData = new byte[1024 * 1024]; // 1MB данных
            this.activityLog = new ArrayList<>();

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

        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > SESSION_TTL;
        }

        public void addActivity(String activity) {
            this.lastAccessTime = System.currentTimeMillis();
            activityLog.add(activity);
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Memory Leak FIXED Example ===");
        System.out.println("Это приложение демонстрирует ИСПРАВЛЕННУЮ версию без утечки памяти.");
        System.out.println("Оно будет работать стабильно в течение 40 минут.\n");

        printMemoryInfo();
        System.out.println("\nИсправления:");
        System.out.println("✓ Добавлено автоматическое удаление старых сессий (TTL: 5 минут)");
        System.out.println("✓ Ограничение на максимальное количество сессий: " + MAX_SESSIONS);
        System.out.println("✓ Периодическая очистка памяти\n");

        System.out.println("Инструкции:");
        System.out.println("1. Запустите приложение");
        System.out.println("2. Мониторьте память через jconsole, VisualVM или JMC");
        System.out.println("3. Снимите heap dump через: jcmd <pid> GC.heap_dump heap_fixed.hprof");
        System.out.println("4. Сравните с heap_leak.hprof\n");
        System.out.println("Начинаем создание сессий...\n");

        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        long lastCleanupTime = startTime;
        int iteration = 0;

        try {
            // Работаем ~40 минут
            while (System.currentTimeMillis() - startTime < 40 * 60 * 1000) {
                iteration++;

                // Создаем новые сессии (имитируем новых пользователей)
                for (int i = 0; i < 10; i++) {
                    String sessionId = "SESSION_" + totalSessionsCreated++;
                    UserSession session = new UserSession(sessionId);

                    // ИСПРАВЛЕНО: Проверяем лимит перед добавлением
                    synchronized (ACTIVE_SESSIONS) {
                        if (ACTIVE_SESSIONS.size() < MAX_SESSIONS) {
                            ACTIVE_SESSIONS.add(session);
                            totalDataAllocated += 1024 * 1024;
                        } else {
                            // Если достигли лимита, удаляем самую старую сессию
                            removeOldestSession();
                            ACTIVE_SESSIONS.add(session);
                        }
                    }

                    // Имитируем активность в случайных сессиях
                    if (!ACTIVE_SESSIONS.isEmpty()) {
                        Random random = new Random();
                        int randomIndex = random.nextInt(ACTIVE_SESSIONS.size());
                        ACTIVE_SESSIONS.get(randomIndex).addActivity("Activity at " + System.currentTimeMillis());
                    }
                }

                // ИСПРАВЛЕНО: Периодическая очистка устаревших сессий (каждые 30 секунд)
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCleanupTime >= 30_000) {
                    cleanupExpiredSessions();
                    lastCleanupTime = currentTime;
                }

                // Имитируем обработку
                Thread.sleep(100);

                // Выводим статистику каждую минуту
                if (currentTime - lastReportTime >= 60_000) {
                    printStatus(startTime);
                    lastReportTime = currentTime;
                }
            }

            System.out.println("\n✓ Программа успешно отработала 40 минут!");
            System.out.println("✓ OutOfMemoryError НЕ произошел благодаря управлению памятью.");
            System.out.println("\nФинальная статистика:");
            printStatus(startTime);

        } catch (OutOfMemoryError e) {
            System.err.println("\n✗ OutOfMemoryError произошел (не должно было случиться!)");
            System.err.println("Возможно, установлен слишком маленький heap size.");
            printStatus(startTime);
            throw e;

        } catch (InterruptedException e) {
            System.out.println("\nПрограмма прервана пользователем.");
            printStatus(startTime);
        }
    }

    // ИСПРАВЛЕНИЕ: Удаление устаревших сессий
    private static void cleanupExpiredSessions() {
        synchronized (ACTIVE_SESSIONS) {
            Iterator<UserSession> iterator = ACTIVE_SESSIONS.iterator();
            int removedCount = 0;

            while (iterator.hasNext()) {
                UserSession session = iterator.next();
                if (session.isExpired()) {
                    iterator.remove();
                    totalSessionsRemoved++;
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                System.out.println("🧹 Очистка: удалено " + removedCount + " устаревших сессий");
            }
        }
    }

    // ИСПРАВЛЕНИЕ: Удаление самой старой сессии при достижении лимита
    private static void removeOldestSession() {
        synchronized (ACTIVE_SESSIONS) {
            if (!ACTIVE_SESSIONS.isEmpty()) {
                UserSession oldest = ACTIVE_SESSIONS.stream()
                    .min((s1, s2) -> Long.compare(s1.getLastAccessTime(), s2.getLastAccessTime()))
                    .orElse(null);

                if (oldest != null) {
                    ACTIVE_SESSIONS.remove(oldest);
                    totalSessionsRemoved++;
                }
            }
        }
    }

    private static void printStatus(long startTime) {
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 1000 / 60;

        System.out.println("┌────────────────────────────────────────────────────────────┐");
        System.out.println("│ Статистика (БЕЗ утечки):                                   │");
        System.out.println("├────────────────────────────────────────────────────────────┤");
        System.out.printf("│ Время работы:          %3d минут                           │%n", elapsedMinutes);
        System.out.printf("│ Активных сессий:       %,10d / %d (макс)              │%n",
            ACTIVE_SESSIONS.size(), MAX_SESSIONS);
        System.out.printf("│ Создано сессий всего:  %,10d                          │%n", totalSessionsCreated);
        System.out.printf("│ Удалено сессий:        %,10d                          │%n", totalSessionsRemoved);
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
