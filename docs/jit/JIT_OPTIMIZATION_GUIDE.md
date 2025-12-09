# JIT Оптимизации в Java - Практическое Руководство

Этот документ содержит примеры различных JIT оптимизаций и инструкции по их наблюдению.

## Содержание

1. [Inline Optimization (Встраивание методов)](#1-inline-optimization)
2. [Loop Optimizations (Оптимизация циклов)](#2-loop-optimizations)
3. [Escape Analysis (Анализ утечки объектов)](#3-escape-analysis)
4. [Dead Code Elimination (Удаление мертвого кода)](#4-dead-code-elimination)
5. [Branch Prediction (Предсказание ветвлений)](#5-branch-prediction)
6. [Полезные JVM флаги](#полезные-jvm-флаги)
7. [Как интерпретировать результаты](#как-интерпретировать-результаты)

---

## 1. Inline Optimization

### Что это?
JIT компилятор встраивает маленькие методы прямо в вызывающий код, устраняя overhead вызова метода.

### Запуск

```bash
# Базовый запуск
./gradlew run -PmainClass=ru.sin.jit.InlineOptimizationExample

# С выводом информации о inline оптимизациях
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining \
  -cp build/classes/java/main ru.sin.jit.InlineOptimizationExample
```

### Куда смотреть

После запуска с флагами вы увидите вывод вроде:
```
@ 15   ru.sin.jit.InlineOptimizationExample::square (6 bytes)   inline
  @ 1   ru.sin.jit.InlineOptimizationExample::multiply (4 bytes)   inline
@ 19   ru.sin.jit.InlineOptimizationExample::add (4 bytes)   inline
```

**Ключевые моменты:**
- `inline` означает, что метод был встроен
- После прогрева JVM, производительность обоих вариантов (с методами и без) должна быть похожей
- Маленькие методы (< 35 байт байткода по умолчанию) — кандидаты на inline

### Дополнительные флаги

```bash
# Отключить inline (для сравнения)
java -XX:-Inline -cp build/classes/java/main ru.sin.jit.InlineOptimizationExample

# Изменить максимальный размер метода для inline
java -XX:MaxInlineSize=100 -cp build/classes/java/main ru.sin.jit.InlineOptimizationExample
```

---

## 2. Loop Optimizations

### Что это?
JIT оптимизирует циклы через:
- **Loop hoisting** — вынос инвариантов из цикла
- **Loop unrolling** — разворачивание цикла
- **Bounds check elimination** — удаление проверок границ

### Запуск

```bash
# Базовый запуск
./gradlew run -PmainClass=ru.sin.jit.LoopOptimizationExample

# С детальной информацией об оптимизациях
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+TraceLoopOpts \
  -cp build/classes/java/main ru.sin.jit.LoopOptimizationExample

# Посмотреть сгенерированный ассемблерный код (требуется hsdis)
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly \
  -cp build/classes/java/main ru.sin.jit.LoopOptimizationExample
```

### Куда смотреть

**Признаки loop unrolling:**
- Производительность простого и развернутого циклов становится одинаковой
- В выводе `-XX:+TraceLoopOpts` видно `unroll`

**Признаки loop hoisting:**
- JIT автоматически выносит `data.length` из цикла
- Оба варианта (с инвариантом и без) работают одинаково быстро

---

## 3. Escape Analysis

### Что это?
JIT анализирует, "убегает" ли объект из метода. Если нет, объект может быть:
- Аллоцирован на стеке (быстрее)
- Полностью элиминирован (scalar replacement)

### Запуск

```bash
# Базовый запуск
./gradlew run -PmainClass=ru.sin.jit.EscapeAnalysisExample

# С информацией об escape analysis
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintEscapeAnalysis \
  -cp build/classes/java/main ru.sin.jit.EscapeAnalysisExample

# С выводом элиминации аллокаций
java -XX:+PrintCompilation -XX:+DoEscapeAnalysis -XX:+PrintEliminateAllocations \
  -XX:+UnlockDiagnosticVMOptions \
  -cp build/classes/java/main ru.sin.jit.EscapeAnalysisExample

# Отключить escape analysis (для сравнения)
java -XX:-DoEscapeAnalysis \
  -cp build/classes/java/main ru.sin.jit.EscapeAnalysisExample
```

### Куда смотреть

**Признаки успешного escape analysis:**
- Метод `noEscape()` работает значительно быстрее `escapes()`
- В выводе `-XX:+PrintEliminateAllocations` видно `Eliminated allocation`
- С флагом `-XX:-DoEscapeAnalysis` производительность падает

**Scalar replacement:**
- Объект `Point` полностью заменяется на его поля (x, y)
- Аллокации вообще не происходит

---

## 4. Dead Code Elimination

### Что это?
JIT удаляет код, который не влияет на результат выполнения:
- Неиспользуемые переменные
- Недостижимый код
- Константные вычисления (constant folding)

### Запуск

```bash
# Базовый запуск
./gradlew run -PmainClass=ru.sin.jit.DeadCodeEliminationExample

# С информацией о компиляции
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions \
  -cp build/classes/java/main ru.sin.jit.DeadCodeEliminationExample

# Отключить оптимизации (для сравнения)
java -Xint \
  -cp build/classes/java/main ru.sin.jit.DeadCodeEliminationExample
```

### Куда смотреть

**Признаки dead code elimination:**
- Методы `withDeadCode()` и `withoutDeadCode()` работают с одинаковой скоростью
- Неиспользуемые вычисления не влияют на производительность
- С флагом `-Xint` (интерпретатор) разница будет заметна

**Constant folding:**
- `10 * 20 + 5` вычисляется на этапе компиляции
- Оба варианта (`withConstants` и `optimizedConstants`) одинаково быстры

---

## 5. Branch Prediction

### Что это?
CPU предсказывает результат условных переходов. JIT использует профилирование для оптимизации горячих путей.

### Запуск

```bash
# Базовый запуск
./gradlew run -PmainClass=ru.sin.jit.BranchPredictionExample

# С информацией о профилировании
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions \
  -cp build/classes/java/main ru.sin.jit.BranchPredictionExample
```

### Куда смотреть

**Что наблюдать:**
- Отсортированные данные обрабатываются быстрее (предсказуемые ветвления)
- Случайные данные медленнее на 20-50% (branch misprediction)
- Версия без ветвлений может быть быстрее на непредсказуемых данных

**Практический вывод:**
- Сортировка данных перед обработкой может улучшить производительность
- Избегайте непредсказуемых ветвлений в горячих циклах
- Рассмотрите bitwise трюки для устранения ветвлений

---

## Полезные JVM флаги

### Основные флаги для наблюдения за JIT

```bash
# Показать когда методы компилируются
-XX:+PrintCompilation

# Разблокировать диагностические флаги
-XX:+UnlockDiagnosticVMOptions

# Показать inline решения
-XX:+PrintInlining

# Показать escape analysis
-XX:+PrintEscapeAnalysis

# Показать элиминацию аллокаций
-XX:+PrintEliminateAllocations

# Показать оптимизации циклов
-XX:+TraceLoopOpts

# Показать ассемблерный код (требуется hsdis-amd64.dylib/so/dll)
-XX:+PrintAssembly

# Показать метод в ассемблере
-XX:CompileCommand=print,ru.sin.jit.InlineOptimizationExample::calculateWithMethodCalls
```

### Флаги для контроля JIT

```bash
# Отключить JIT (только интерпретатор)
-Xint

# Только C1 компилятор (быстрая компиляция)
-XX:TieredStopAtLevel=1

# Только C2 компилятор (агрессивные оптимизации)
-XX:-TieredCompilation

# Изменить порог компиляции (по умолчанию ~10000 вызовов)
-XX:CompileThreshold=5000

# Отключить inline
-XX:-Inline

# Отключить escape analysis
-XX:-DoEscapeAnalysis

# Показать статистику JIT после выполнения
-XX:+PrintCompilation -XX:+CITime
```

### Установка hsdis для PrintAssembly

**macOS:**
```bash
# Скачайте или соберите hsdis-amd64.dylib
# Поместите в: $JAVA_HOME/lib/server/
```

**Linux:**
```bash
# Установите через пакетный менеджер или соберите из исходников
# Поместите hsdis-amd64.so в: $JAVA_HOME/lib/server/
```

**Windows:**
```bash
# Скачайте hsdis-amd64.dll
# Поместите в: %JAVA_HOME%\bin\server\
```

---

## Как интерпретировать результаты

### PrintCompilation вывод

```
    182   52       3       ru.sin.jit.InlineOptimizationExample::calculateWithMethodCalls (20 bytes)
    183   53       4       ru.sin.jit.InlineOptimizationExample::calculateWithMethodCalls (20 bytes)
    185   52       3       ru.sin.jit.InlineOptimizationExample::calculateWithMethodCalls (20 bytes)   made not entrant
```

**Расшифровка:**
- `182` — timestamp (ms)
- `52` — compile_id
- `3` — compilation level (1-4: C1 уровни, 4: C2)
- Метод и размер в байтах
- `made not entrant` — старая версия деоптимизирована

### Compilation Levels

- **Level 0:** Интерпретатор
- **Level 1:** C1 без профилирования
- **Level 2:** C1 с легким профилированием
- **Level 3:** C1 с полным профилированием
- **Level 4:** C2 с агрессивными оптимизациями

### Типичный жизненный цикл метода

1. Метод выполняется в интерпретаторе (level 0)
2. После ~2000 вызовов — компиляция C1 (level 3)
3. Профилирование во время выполнения
4. После ~10000 вызовов — рекомпиляция C2 (level 4)
5. Возможна деоптимизация при изменении условий

---

## Общие рекомендации

### Для точных замеров:

1. **Всегда делайте прогрев JVM** — минимум 10-20 тысяч итераций перед замером
2. **Запускайте несколько раз** — JIT может компилировать в разное время
3. **Используйте JMH** — для серьезного бенчмаркинга (Java Microbenchmark Harness)
4. **Отключайте frequency scaling** — `cpupower frequency-set -g performance` (Linux)

### Типичные ошибки:

❌ Не прогревать JVM перед замером
❌ Мерить слишком короткие операции (< 1ms)
❌ Не учитывать GC паузы
❌ Верить одному запуску
❌ Не проверять, что код не был элиминирован полностью

### Проверка, что код не элиминирован:

```java
// ПЛОХО - результат не используется, может быть элиминирован
for (int i = 0; i < ITERATIONS; i++) {
    calculate(i);
}

// ХОРОШО - результат используется
long sum = 0;
for (int i = 0; i < ITERATIONS; i++) {
    sum += calculate(i);
}
System.out.println(sum); // Обязательно вывести!
```

---

## Продвинутые темы

### Deoptimization

JIT может деоптимизировать код если:
- Изменились предположения (например, появился новый подкласс)
- Произошло редкое условие
- Нужна отладочная информация

Смотреть: `-XX:+PrintDeoptimization`

### Tiered Compilation

Современная JVM использует многоуровневую компиляцию:
- C1 (client compiler) — быстрая компиляция с базовыми оптимизациями
- C2 (server compiler) — медленная компиляция с агрессивными оптимизациями

Смотреть: `-XX:+TieredCompilation` (включено по умолчанию)

### OSR (On-Stack Replacement)

Замена кода прямо во время выполнения длинного цикла.

Смотреть в `-XX:+PrintCompilation`: метки `%` и `@`

---

## Дополнительные ресурсы

- [JIT Compiler Overview](https://docs.oracle.com/en/java/javase/17/vm/java-hotspot-virtual-machine-performance-enhancements.html)
- [JVM Performance Tuning](https://www.oracle.com/technical-resources/articles/java/performance-tuning.html)
- [OpenJDK HotSpot Wiki](https://wiki.openjdk.org/display/HotSpot)
- [JMH - Java Microbenchmark Harness](https://github.com/openjdk/jmh)

---

## Быстрый старт

```bash
# 1. Скомпилировать все примеры
./gradlew build

# 2. Запустить все примеры по очереди с базовыми флагами
./gradlew run -PmainClass=ru.sin.jit.InlineOptimizationExample
./gradlew run -PmainClass=ru.sin.jit.LoopOptimizationExample
./gradlew run -PmainClass=ru.sin.jit.EscapeAnalysisExample
./gradlew run -PmainClass=ru.sin.jit.DeadCodeEliminationExample
./gradlew run -PmainClass=ru.sin.jit.BranchPredictionExample

# 3. Запустить с детальными логами JIT (выберите любой пример)
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining \
  -cp build/classes/java/main ru.sin.jit.InlineOptimizationExample
```
