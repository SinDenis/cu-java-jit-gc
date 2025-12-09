# Async-Profiler Guide - Профилирование и Оптимизация

Руководство по использованию async-profiler для выявления performance проблем и оптимизации Java приложений.

## Содержание

1. [Что такое async-profiler](#что-такое-async-profiler)
2. [Установка](#установка)
3. [Быстрый старт](#быстрый-старт)
4. [Примеры в проекте](#примеры-в-проекте)
5. [Анализ Flame Graph](#анализ-flame-graph)
6. [Типичные проблемы и их решения](#типичные-проблемы-и-их-решения)
7. [Альтернативные инструменты](#альтернативные-инструменты)

---

## Что такое async-profiler

**async-profiler** - это low-overhead профайлер для Java, использующий:
- AsyncGetCallTrace API для минимального влияния на производительность
- perf_events на Linux для hardware-level профилирования
- Flame graphs для визуализации результатов

**Преимущества:**
- ✅ Минимальный overhead (< 1%)
- ✅ Не требует перезапуска JVM
- ✅ Профилирование на production
- ✅ CPU, memory allocation, lock contention
- ✅ Flame graph visualization

**Альтернативы:**
- Java Flight Recorder (JFR) - встроен в JDK 11+
- VisualVM - GUI инструмент
- YourKit - коммерческий профайлер

---

## Установка

### macOS

```bash
# Создать директорию
mkdir -p ~/.async-profiler && cd ~/.async-profiler

# Скачать последнюю версию
curl -L https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-macos.zip -o async-profiler.zip

# Распаковать
unzip async-profiler.zip

# Проверить
ls async-profiler-3.0-macos/lib/libasyncProfiler.dylib
```

### Linux

```bash
# Создать директорию
mkdir -p ~/.async-profiler && cd ~/.async-profiler

# Скачать последнюю версию
curl -L https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz -o async-profiler.tar.gz

# Распаковать
tar -xzf async-profiler.tar.gz

# Проверить
ls async-profiler-3.0-linux-x64/lib/libasyncProfiler.so
```

### Настройка скрипта

Отредактируйте `scripts/profile_app.sh` и укажите правильный путь:

```bash
# Для macOS
PROFILER_PATH="$HOME/.async-profiler/async-profiler-3.0-macos/lib/libasyncProfiler.dylib"

# Для Linux
PROFILER_PATH="$HOME/.async-profiler/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so"
```

---

## Быстрый старт

### 1. Запустить приложение

```bash
# Компиляция
./gradlew build

# Запустить медленное приложение
./gradlew runSlowApp
```

Приложение выведет свой PID:
```
=== Slow Application Example ===
PID: 12345
Запустите профилирование:
  ./scripts/profile_app.sh 12345
```

### 2. Запустить профилирование

**В другом терминале:**

```bash
# CPU профилирование на 30 секунд
./scripts/profile_app.sh 12345

# Или с кастомными параметрами
./scripts/profile_app.sh 12345 60         # 60 секунд
./scripts/profile_app.sh 12345 30 alloc  # Memory allocation профилирование
```

### 3. Посмотреть результаты

```bash
# Открыть flame graph в браузере
open profiling_results/profile_cpu_12345_TIMESTAMP.html

# Или JFR файл в JDK Mission Control
jmc profiling_results/profile_cpu_12345_TIMESTAMP.jfr
```

---

## Примеры в проекте

### Пример 1: Медленное приложение (до оптимизации)

**SlowApplicationExample.java** содержит типичные performance проблемы:

#### Проблема 1: String concatenation в цикле

```java
// ❌ Медленно: создает множество String объектов
String result = "";
for (int i = 0; i < 1000; i++) {
    result += "Item_" + i + ",";  // O(n²) сложность!
}
```

**На flame graph:** Широкий блок `StringBuilder.append()` или `String.concat()`

#### Проблема 2: Неэффективные коллекции

```java
// ❌ Медленно: O(n) поиск в ArrayList
List<Integer> numbers = new ArrayList<>();
for (int i = 0; i < 1000; i++) {
    if (numbers.contains(i)) {  // Linear search!
        // ...
    }
}
```

**На flame graph:** Широкий блок `ArrayList.contains()` или `ArrayList.indexOf()`

#### Проблема 3: Избыточные вычисления

```java
// ❌ Медленно: вычисляем одно и то же значение многократно
for (int i = 0; i < 100_000; i++) {
    double value = Math.sqrt(123.456);  // Вычисляется каждый раз!
    result += Math.sin(value) * Math.cos(value);
}
```

**На flame graph:** Широкие блоки `Math.sqrt()`, `Math.sin()`, `Math.cos()`

#### Проблема 4: Ненужные аллокации

```java
// ❌ Медленно: создает лишние объекты
for (int i = 0; i < 1000; i++) {
    String key = new String("key_" + i);  // Лишний new String()!
}
```

**На flame graph (event=alloc):** Широкий блок с аллокациями `String.<init>`

### Пример 2: Оптимизированное приложение

**OptimizedApplicationExample.java** исправляет все проблемы:

#### Исправление 1: StringBuilder

```java
// ✅ Быстро: O(n) сложность
StringBuilder result = new StringBuilder(1000 * 20);
for (int i = 0; i < 1000; i++) {
    result.append("Item_").append(i).append(',');
}
```

#### Исправление 2: HashSet

```java
// ✅ Быстро: O(1) поиск в HashSet
Set<Integer> numbers = new HashSet<>(1000);
for (int i = 0; i < 1000; i++) {
    if (numbers.contains(i)) {  // Constant time!
        // ...
    }
}
```

#### Исправление 3: Кэширование

```java
// ✅ Быстро: вычисляем один раз
double cachedSqrt = Math.sqrt(123.456);
double sinValue = Math.sin(cachedSqrt);
double cosValue = Math.cos(cachedSqrt);
double product = sinValue * cosValue;

for (int i = 0; i < 100_000; i++) {
    result += product;  // Переиспользуем!
}
```

#### Исправление 4: String interning

```java
// ✅ Быстро: использует string pool
for (int i = 0; i < 1000; i++) {
    String key = "key_" + i;  // String literal
}
```

### Запуск примеров

```bash
# 1. Запустить медленную версию
./gradlew runSlowApp

# В другом терминале
./scripts/profile_app.sh <PID>

# 2. Запустить оптимизированную версию
./gradlew runOptimizedApp

# В другом терминале
./scripts/profile_app.sh <PID>

# 3. Сравнить результаты
# Ожидаемое ускорение: ~5-10x
```

---

## Анализ Flame Graph

### Как читать Flame Graph

```
┌─────────────────────────────────────────┐
│          main()                         │  ← Корень стека (всегда шире всех)
├──────────┬──────────┬───────────────────┤
│processStrings()│processCollections()│  ← Вызываемые методы
├──────────┤    │     │                   │
│StringBuilder.append()│                  │  ← "Листья" - где тратится время
└──────────┴──────────┴───────────────────┘

Ось X: Процент CPU времени (ширина блока)
Ось Y: Глубина стека вызовов (высота)
Цвет: Обычно случайный (для различения)
```

### Что искать

**1. Широкие блоки на верхних уровнях = Hot Spots**

```
┌──────────────────────────────────────────────┐
│                  main()                      │
├──────────────────────────────────────────────┤
│          processStrings() [80% CPU]          │  ← HOT SPOT!
└──────────────────────────────────────────────┘
```

**2. Глубокие стеки = Возможна оптимизация**

```
main() → process() → helper1() → helper2() → helper3() → work()
                                    ↑
                              Много вызовов
```

**3. Паттерны проблем:**

```
String concatenation:
  String.concat() / StringBuilder.append() (много раз)

Collection lookup:
  ArrayList.contains() / indexOf() (широкий блок)

Allocations (event=alloc):
  new String() / new ArrayList() (много раз)

Lock contention (event=lock):
  Monitor wait / synchronized блоки
```

### Интерактивные возможности

**Flame graph в браузере:**
- Клик на блок → zoom in (фокус на этот блок)
- Ctrl+F → поиск по имени метода
- Hover → показывает процент времени

---

## Типичные проблемы и их решения

### Проблема 1: String concatenation в циклах

**Симптомы:**
- Широкие блоки `StringBuilder.append()` или `String.concat()`
- `String.<init>` при event=alloc

**Решение:**
```java
// До
String s = "";
for (...) { s += "text"; }

// После
StringBuilder sb = new StringBuilder(capacity);
for (...) { sb.append("text"); }
String s = sb.toString();
```

### Проблема 2: Неэффективный поиск в коллекциях

**Симптомы:**
- Широкие блоки `ArrayList.contains()`, `ArrayList.indexOf()`
- O(n) сложность при большом количестве элементов

**Решение:**
```java
// До
List<T> list = new ArrayList<>();
if (list.contains(item)) { ... }  // O(n)

// После
Set<T> set = new HashSet<>();
if (set.contains(item)) { ... }    // O(1)
```

### Проблема 3: Избыточные вычисления

**Симптомы:**
- Широкие блоки `Math.*` методов
- Повторяющиеся вычисления в циклах

**Решение:**
```java
// До
for (int i = 0; i < N; i++) {
    double val = Math.sqrt(constant);  // Вычисляется N раз
    // ...
}

// После
double val = Math.sqrt(constant);  // Вычисляется 1 раз
for (int i = 0; i < N; i++) {
    // использовать val
}
```

### Проблема 4: Много мелких объектов

**Симптомы (event=alloc):**
- Много аллокаций `new String()`, `new ArrayList()`
- Высокое давление на Young GC

**Решение:**
```java
// До
for (...) {
    String s = new String("prefix_" + i);  // Лишний new String()
}

// После
for (...) {
    String s = "prefix_" + i;  // String interning
}

// Или переиспользовать объекты
List<T> reusableList = new ArrayList<>(capacity);
for (...) {
    reusableList.clear();
    // использовать reusableList
}
```

### Проблема 5: Неправильная итерация по Map

**Симптомы:**
- Широкие блоки `HashMap.get()` при итерации

**Решение:**
```java
// До (двойной lookup)
for (String key : map.keySet()) {
    Value v = map.get(key);  // Lookup!
}

// После (один lookup)
for (Map.Entry<String, Value> entry : map.entrySet()) {
    Value v = entry.getValue();
}
```

---

## Типы профилирования

### 1. CPU Profiling (по умолчанию)

```bash
./scripts/profile_app.sh <PID> 30 cpu
```

**Показывает:**
- Где тратится CPU время
- Hot methods
- Call stacks

**Когда использовать:**
- Приложение медленное
- Высокая CPU загрузка
- Оптимизация производительности

### 2. Memory Allocation Profiling

```bash
./scripts/profile_app.sh <PID> 30 alloc
```

**Показывает:**
- Где аллоцируются объекты
- Allocation rate
- Memory pressure

**Когда использовать:**
- Частые GC паузы
- High Young GC activity
- Memory leaks (в комбинации с heap dump)

### 3. Lock Contention Profiling

```bash
./scripts/profile_app.sh <PID> 30 lock
```

**Показывает:**
- Конкуренцию за locks
- Blocked threads
- Synchronization overhead

**Когда использовать:**
- Низкая CPU утилизация при высокой нагрузке
- Многопоточное приложение медленное
- Подозрение на lock contention

### 4. Wall-Clock Profiling

```bash
./scripts/profile_app.sh <PID> 30 wall
```

**Показывает:**
- Реальное время выполнения (включая I/O, sleep)
- Не только CPU

**Когда использовать:**
- Приложение медленное, но CPU низкая
- Много I/O операций
- Network calls, disk reads

---

## Альтернативные инструменты

### Java Flight Recorder (JFR)

**Встроен в JDK 11+:**

```bash
# Запустить с JFR
./gradlew runSlowAppWithJFR

# Или вручную
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -XX:FlightRecorderOptions=stackdepth=256 \
     -jar app.jar

# Открыть в JDK Mission Control
jmc recording.jfr
```

**Преимущества:**
- Встроен в JDK
- Low overhead
- Много метрик (CPU, memory, GC, I/O, exceptions)

**Недостатки:**
- Нет flame graph (нужен конвертер)
- GUI тяжелее для анализа

### VisualVM

```bash
# Установка (macOS)
brew install visualvm

# Запуск
jvisualvm
```

**Преимущества:**
- GUI интерфейс
- Real-time мониторинг
- Profiling + heap dump + threads

**Недостатки:**
- Выше overhead чем async-profiler
- Не для production

### Сравнение инструментов

| Инструмент | Overhead | Production | Flame Graph | GUI |
|-----------|----------|------------|-------------|-----|
| async-profiler | < 1% | ✅ | ✅ | ❌ |
| JFR | < 1% | ✅ | ⚠️ (через конвертер) | ✅ (JMC) |
| VisualVM | 5-10% | ❌ | ❌ | ✅ |
| YourKit | 5-10% | ⚠️ | ✅ | ✅ |

---

## Workflow оптимизации

### 1. Измерить baseline

```bash
# Запустить неоптимизированное приложение
./gradlew runSlowApp

# Засечь время
# Например: 45 секунд
```

### 2. Профилировать

```bash
# В другом терминале
./scripts/profile_app.sh <PID> 30 cpu
```

### 3. Анализировать flame graph

```bash
open profiling_results/profile_cpu_*.html
```

**Ищем:**
- Самые широкие блоки (hot spots)
- Неожиданно медленные методы
- Паттерны inefficient code

### 4. Оптимизировать

На основе flame graph:
- String concatenation → StringBuilder
- ArrayList.contains → HashSet.contains
- Repeated computation → cache
- etc.

### 5. Проверить результат

```bash
# Запустить оптимизированное приложение
./gradlew runOptimizedApp

# Засечь время
# Например: 8 секунд (5.6x ускорение!)

# Профилировать снова
./scripts/profile_app.sh <PID> 30 cpu

# Сравнить flame graphs
```

### 6. Итерировать

Повторять шаги 2-5 до достижения целевой производительности.

---

## Best Practices

### 1. Профилируйте на realistic workload

❌ **Плохо:** Профилирование на маленьких данных
```java
for (int i = 0; i < 10; i++) { ... }
```

✅ **Хорошо:** Профилирование на реалистичных объемах
```java
for (int i = 0; i < 1_000_000; i++) { ... }
```

### 2. Прогревайте JVM перед профилированием

```java
// Warm-up phase
for (int i = 0; i < 20_000; i++) {
    // Прогреть JIT
}

// Теперь профилировать
// ...
```

### 3. Профилируйте достаточно долго

```bash
# ❌ Слишком коротко
./scripts/profile_app.sh <PID> 5

# ✅ Достаточно для stable picture
./scripts/profile_app.sh <PID> 30
```

### 4. Используйте разные типы профилирования

```bash
# CPU
./scripts/profile_app.sh <PID> 30 cpu

# Allocation
./scripts/profile_app.sh <PID> 30 alloc

# Полная картина = CPU + allocation + locks (если многопоточно)
```

### 5. Профилируйте до и после оптимизации

Всегда сохраняйте baseline для сравнения.

---

## Troubleshooting

### Проблема: "async-profiler не найден"

**Решение:** Проверьте путь в `scripts/profile_app.sh`:

```bash
PROFILER_PATH="$HOME/.async-profiler/async-profiler-3.0-macos/lib/libasyncProfiler.dylib"
```

### Проблема: "Permission denied" на Linux

**Решение:** Разрешите perf_events:

```bash
# Временно
echo 1 | sudo tee /proc/sys/kernel/perf_event_paranoid

# Или постоянно
echo 'kernel.perf_event_paranoid=1' | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

### Проблема: Пустой flame graph

**Причины:**
1. Приложение завершилось до профилирования
2. Слишком короткое время профилирования
3. Приложение в idle состоянии

**Решение:**
- Увеличьте время профилирования
- Убедитесь что приложение активно работает
- Проверьте что PID правильный

---

## Дополнительные ресурсы

### Документация

- [async-profiler GitHub](https://github.com/async-profiler/async-profiler)
- [Flame Graphs](http://www.brendangregg.com/flamegraphs.html) - Brendan Gregg
- [Java Performance Tuning Guide](https://www.oracle.com/technical-resources/articles/java/performance.html)

### Инструменты

- [async-profiler](https://github.com/async-profiler/async-profiler) - CPU/allocation profiler
- [JDK Mission Control](https://www.oracle.com/java/technologies/javase/products-jmc8-downloads.html) - JFR viewer
- [VisualVM](https://visualvm.github.io/) - All-in-one profiler

### Статьи

- [Flame Graphs for Java](https://www.brendangregg.com/FlameGraphs/cpuflamegraphs.html)
- [Java Performance Patterns](https://www.infoq.com/articles/Java-Performance-Patterns/)

---

## Quick Reference

```bash
# Установка (macOS)
curl -L https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-macos.zip -o ~/.async-profiler/async-profiler.zip
unzip ~/.async-profiler/async-profiler.zip -d ~/.async-profiler

# Запуск примеров
./gradlew runSlowApp          # Медленное приложение
./gradlew runOptimizedApp     # Оптимизированное приложение

# Профилирование
./scripts/profile_app.sh <PID>           # CPU, 30 сек
./scripts/profile_app.sh <PID> 60        # CPU, 60 сек
./scripts/profile_app.sh <PID> 30 alloc  # Allocation, 30 сек

# Просмотр результатов
open profiling_results/profile_*.html    # Flame graph
jmc profiling_results/profile_*.jfr      # JFR viewer

# С Java Flight Recorder
./gradlew runSlowAppWithJFR
./gradlew runOptimizedAppWithJFR
```

---

**Начните с профилирования медленного приложения и оптимизируйте узкие места! 🔥📊**
