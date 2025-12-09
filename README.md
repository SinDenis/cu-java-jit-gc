# Java JIT Optimization, GC & Performance Examples

Практические примеры JIT оптимизаций, анализа Garbage Collector и сравнения различных GC в Java с подробными инструкциями.

## Быстрый старт

### JIT Оптимизации

```bash
# 1. Собрать проект
./gradlew build

# 2. Запустить примеры JIT оптимизаций
./gradlew runInlineExample
./gradlew runLoopExample
./gradlew runEscapeExample
./gradlew runDeadCodeExample
./gradlew runBranchExample

# 3. Запустить с детальным выводом JIT оптимизаций
./gradlew runInlineExampleDebug
./gradlew runEscapeExampleDebug
```

### GC и Memory Leaks

```bash
# 1. Запустить пример С утечкой памяти
./gradlew runMemoryLeak               # Static collection leak
./gradlew runListenerLeak             # Listener leak

# 2. Запустить исправленную версию БЕЗ утечки
./gradlew runMemoryLeakFixed
./gradlew runListenerLeakFixed

# 3. Для быстрого воспроизведения (маленький heap)
./gradlew runMemoryLeakSmallHeap      # OOM за ~5-10 минут

# 4. Мониторинг GC в реальном времени
./scripts/gc_monitor.sh

# 5. Снять heap dump для анализа
./scripts/heap_dump.sh

# 6. Анализировать heap dump
./scripts/analyze_heap.sh heap_dumps/heap_leak.hprof
```

### GC Benchmarks - Сравнение Garbage Collectors

```bash
# Throughput бенчмарк с разными GC
./gradlew runThroughputSerial      # Serial GC
./gradlew runThroughputParallel    # Parallel GC (лучший throughput)
./gradlew runThroughputG1          # G1 GC (default)
./gradlew runThroughputZGC         # ZGC (требуется Java 15+)

# Latency бенчмарк (важна p99 latency)
./gradlew runLatencySerial
./gradlew runLatencyParallel
./gradlew runLatencyG1
./gradlew runLatencyZGC            # Лучшая latency (<10ms)

# Mixed workload (реалистичная нагрузка)
./gradlew runMixedG1
./gradlew runMixedZGC

# Allocation rate test
./gradlew runAllocationG1
./gradlew runAllocationZGC

# Автоматическое сравнение всех GC
./scripts/compare_gc.sh throughput # Сравнить throughput
./scripts/compare_gc.sh latency    # Сравнить latency
./scripts/compare_gc.sh all        # Все бенчмарки
```

### Profiling - Анализ производительности с async-profiler

```bash
# 1. Запустить приложение с проблемами производительности
./gradlew runSlowApp

# 2. В другом терминале: профилировать (30 сек)
./scripts/profile_app.sh <PID>

# 3. Открыть flame graph и найти hot spots
open profiling_results/profile_cpu_*.html

# 4. Запустить оптимизированную версию для сравнения
./gradlew runOptimizedApp

# 5. Профилировать оптимизированную версию
./scripts/profile_app.sh <PID>

# С Java Flight Recorder
./gradlew runSlowAppWithJFR
./gradlew runOptimizedAppWithJFR
```

## Что внутри

### JIT Optimization Examples

1. **InlineOptimizationExample** - Встраивание методов (method inlining)
2. **LoopOptimizationExample** - Оптимизация циклов (loop unrolling, hoisting)
3. **EscapeAnalysisExample** - Анализ утечки объектов и scalar replacement
4. **DeadCodeEliminationExample** - Удаление мертвого кода и constant folding
5. **BranchPredictionExample** - Предсказание ветвлений и его влияние

### Garbage Collection & Memory Leak Examples

1. **MemoryLeakExample** - Утечка через статическую коллекцию (~40 минут до OOM)
   - Демонстрирует самый распространенный тип утечки
   - Коллекция постоянно растет и никогда не очищается
   - Автоматическое создание heap dump при OOM

2. **MemoryLeakFixedExample** - Исправленная версия БЕЗ утечки
   - ✓ TTL (Time To Live) для автоматического удаления
   - ✓ Ограничение максимального размера коллекции
   - ✓ Периодическая очистка устаревших данных

3. **ListenerLeakExample** - Утечка через забытые listeners
   - Объекты регистрируются как слушатели, но не отписываются
   - EventBus держит ссылки на все объекты

4. **ListenerLeakFixedExample** - Исправленная версия с WeakReference
   - ✓ Метод `unregister()` для явной отписки
   - ✓ Использование `WeakReference` для автоматической очистки
   - ✓ Паттерн `AutoCloseable` с try-with-resources

### GC Comparison Benchmarks

1. **ThroughputBenchmark** - Тест максимальной производительности (ops/sec)
   - Лучший: Parallel GC ⭐⭐⭐⭐⭐
   - Хороший: G1 GC ⭐⭐⭐⭐
   - Средний: ZGC ⭐⭐⭐ (overhead из-за low-latency)

2. **LatencyBenchmark** - Тест времени отклика (критична p99 latency)
   - Лучший: ZGC ⭐⭐⭐⭐⭐ (p99 < 2ms)
   - Хороший: G1 GC ⭐⭐⭐ (p99 ~10-20ms)
   - Плохой: Parallel GC ⭐⭐ (p99 ~50-100ms)

3. **MixedWorkloadBenchmark** - Реалистичная смешанная нагрузка
   - Баланс: G1 GC ⭐⭐⭐⭐⭐ (default выбор)
   - Low latency: ZGC ⭐⭐⭐⭐
   - Throughput: Parallel GC ⭐⭐⭐

4. **AllocationBenchmark** - Тест allocation rate и Young GC
   - Быстрый Young GC: Parallel GC ⭐⭐⭐⭐⭐
   - Concurrent: ZGC ⭐⭐⭐⭐
   - Adaptive: G1 GC ⭐⭐⭐⭐

### Profiling Examples

1. **SlowApplicationExample** - Неоптимизированное приложение с performance проблемами:
   - ❌ String concatenation в циклах
   - ❌ Неэффективное использование коллекций (ArrayList.contains)
   - ❌ Избыточные вычисления (Math.sqrt в цикле)
   - ❌ Ненужные аллокации объектов
   - Цель: Использовать async-profiler для выявления hot spots

2. **OptimizedApplicationExample** - Оптимизированная версия после анализа flame graph:
   - ✓ StringBuilder вместо String concatenation
   - ✓ HashSet вместо ArrayList для поиска (O(1) вместо O(n))
   - ✓ Кэширование вычислений
   - ✓ Переиспользование объектов
   - Результат: ~5-10x ускорение

### Документация

📖 **[JIT_OPTIMIZATION_GUIDE.md](docs/jit/JIT_OPTIMIZATION_GUIDE.md)** - Полное руководство по JIT:
- Подробными инструкциями по запуску каждого примера
- Объяснением JVM флагов
- Интерпретацией результатов
- Продвинутыми темами (deoptimization, tiered compilation, OSR)

🗑️ **[GC_MEMORY_LEAK_GUIDE.md](docs/gc/GC_MEMORY_LEAK_GUIDE.md)** - Полное руководство по GC и утечкам:
- Как снимать heap dump (jcmd, jmap, автоматически при OOM)
- Анализ heap dump (VisualVM, Eclipse MAT)
- Сравнение heap dump до и после исправления
- GC мониторинг (jstat, GC logs)
- Типичные паттерны утечек памяти
- Инструменты анализа и их использование

⚡ **[GC_COMPARISON_GUIDE.md](docs/gc/GC_COMPARISON_GUIDE.md)** - Сравнение Garbage Collectors:
- Обзор всех GC (Serial, Parallel, G1, ZGC, Shenandoah)
- Когда какой GC использовать
- Latency vs Throughput trade-offs
- Настройка и тюнинг GC
- Интерпретация результатов бенчмарков
- Decision tree для выбора GC

🔥 **[ASYNC_PROFILER_GUIDE.md](docs/profiling/ASYNC_PROFILER_GUIDE.md)** - Профилирование с async-profiler:
- Установка и настройка async-profiler
- Анализ flame graph и выявление hot spots
- Типичные performance проблемы и их решения
- CPU, memory allocation, lock profiling
- Workflow оптимизации приложений
- Сравнение с JFR и VisualVM

🚀 **[QUICK_REFERENCE.md](docs/QUICK_REFERENCE.md)** - Быстрая справка команд

## Структура проекта

```
src/main/java/ru/sin/
├── jit/                                    # JIT оптимизации
│   ├── InlineOptimizationExample.java      # Inline оптимизации
│   ├── LoopOptimizationExample.java        # Оптимизации циклов
│   ├── EscapeAnalysisExample.java          # Escape analysis
│   ├── DeadCodeEliminationExample.java     # Dead code elimination
│   └── BranchPredictionExample.java        # Branch prediction
├── gc/                                     # Garbage Collection
│   ├── leak/                               # Memory leak examples
│   │   ├── MemoryLeakExample.java          # Утечка: static collection
│   │   ├── MemoryLeakFixedExample.java     # Исправленная версия
│   │   ├── ListenerLeakExample.java        # Утечка: listeners
│   │   └── ListenerLeakFixedExample.java   # Исправленная версия
│   └── comparison/                         # GC Benchmarks
│       ├── ThroughputBenchmark.java        # Throughput тест
│       ├── LatencyBenchmark.java           # Latency тест
│       ├── MixedWorkloadBenchmark.java     # Mixed workload
│       └── AllocationBenchmark.java        # Allocation rate
└── profiling/                              # Performance profiling
    ├── SlowApplicationExample.java         # Неоптимизированное приложение
    └── OptimizedApplicationExample.java    # Оптимизированная версия

docs/                                       # Документация
├── jit/
│   └── JIT_OPTIMIZATION_GUIDE.md           # Руководство по JIT
├── gc/
│   ├── GC_MEMORY_LEAK_GUIDE.md             # Руководство по утечкам
│   └── GC_COMPARISON_GUIDE.md              # Сравнение GC
├── profiling/
│   └── ASYNC_PROFILER_GUIDE.md             # async-profiler guide
└── QUICK_REFERENCE.md                      # Быстрая справка

scripts/                                    # Helper скрипты
├── heap_dump.sh                            # Снятие heap dump
├── gc_monitor.sh                           # Мониторинг GC
├── analyze_heap.sh                         # Анализ heap dump
├── compare_gc.sh                           # Сравнение GC
└── profile_app.sh                          # Профилирование с async-profiler
```

## Доступные Gradle задачи

### JIT Examples
```bash
# Просмотреть все JIT примеры
./gradlew tasks --group jit-examples

# Примеры задач:
./gradlew runInlineExample        # Запустить пример
./gradlew runInlineExampleDebug   # Запустить с JIT логами
./gradlew runAllExamples          # Запустить все JIT примеры
```

### GC Benchmarks
```bash
# Просмотреть все GC бенчмарки
./gradlew tasks --group gc-benchmarks

# Throughput tests:
./gradlew runThroughputSerial      # Serial GC
./gradlew runThroughputParallel    # Parallel GC (best throughput)
./gradlew runThroughputG1          # G1 GC (default)
./gradlew runThroughputZGC         # ZGC (Java 15+)

# Latency tests:
./gradlew runLatencySerial
./gradlew runLatencyParallel
./gradlew runLatencyG1
./gradlew runLatencyZGC            # Best latency (p99 < 10ms)

# Mixed workload:
./gradlew runMixedG1
./gradlew runMixedZGC

# Allocation rate:
./gradlew runAllocationG1
./gradlew runAllocationZGC

# Автоматическое сравнение:
./scripts/compare_gc.sh throughput # Сравнить все GC
./scripts/compare_gc.sh latency
./scripts/compare_gc.sh all        # Все бенчмарки
```

## Требования

- Java 11 или выше
- Gradle 8.14 (включен в wrapper)

## Полезные JVM флаги

```bash
# Показать когда методы компилируются
-XX:+PrintCompilation

# Показать inline решения
-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining

# Показать escape analysis
-XX:+PrintEscapeAnalysis -XX:+PrintEliminateAllocations

# Только интерпретатор (для сравнения)
-Xint

# Отключить escape analysis
-XX:-DoEscapeAnalysis
```

## Что наблюдать

### JIT Оптимизации

После прогрева JVM (первые 10-20 тысяч итераций) вы увидите:

✅ **Inline optimization**: Методы с вызовами работают так же быстро, как inline код
✅ **Loop optimization**: JIT автоматически разворачивает и оптимизирует циклы
✅ **Escape analysis**: Объекты, не покидающие метод, могут не аллоцироваться в heap
✅ **Dead code elimination**: Неиспользуемый код не влияет на производительность
✅ **Branch prediction**: Предсказуемые ветвления обрабатываются быстрее

### Memory Leaks

**Признаки утечки памяти:**
```
# Мониторинг через jstat
jstat -gcutil <pid> 1000

Old Generation (O):  50% → 65% → 78% → 89% → 95% → OOM
Full GC Count (FGC): 5   →  10  →  23  →  45  →  89
```

**После исправления:**
```
Old Generation (O):  40% → 55% → 42% → 48% → 45%  (стабильно)
Full GC Count (FGC): 5   →   6  →   7  →   8  →   9   (редко)
```

**Heap Dump анализ:**
- **С утечкой**: ArrayList содержит 10,000+ объектов, Retained Heap ~1GB
- **Без утечки**: ArrayList ограничен 500 объектами, Retained Heap ~50MB

### GC Benchmarks

**Для выбора GC:**
- **Throughput критичен** → Parallel GC
- **Latency < 10ms** → ZGC (Java 15+)
- **Баланс** → G1 GC (default)
- **Маленький heap** → Serial GC

**Что сравнивать:**
- **Throughput**: ops/sec - чем больше, тем лучше
- **Latency p99**: μs - чем меньше, тем лучше
- **GC overhead**: % - чем меньше, тем лучше
- **GC pause time**: ms - чем меньше, тем лучше

## Примеры запуска

### JIT Examples

**Базовый запуск:**
```bash
./gradlew runInlineExample
```

**С детальным выводом JIT:**
```bash
./gradlew runInlineExampleDebug
```

**Напрямую через java с кастомными флагами:**
```bash
# Скомпилировать
./gradlew classes

# Запустить с PrintAssembly (если установлен hsdis)
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly \
  -cp build/classes/java/main ru.sin.jit.InlineOptimizationExample

# Запустить только с интерпретатором (без JIT)
java -Xint \
  -cp build/classes/java/main ru.sin.jit.InlineOptimizationExample
```

### GC Examples

**Workflow для обнаружения утечки:**

1. **Запустить пример с утечкой:**
   ```bash
   # В одном терминале
   ./gradlew runMemoryLeakSmallHeap
   ```

2. **Мониторить GC (в другом терминале):**
   ```bash
   # Найти PID
   jps -l | grep MemoryLeak

   # Запустить мониторинг
   ./scripts/gc_monitor.sh
   # Выбрать режим 1 (gcutil)
   ```

3. **Снять heap dump (когда Old Gen > 70%):**
   ```bash
   ./scripts/heap_dump.sh
   # Выбрать опцию 2 (live objects)
   ```

4. **Дождаться OOM** (heap dump создастся автоматически)

5. **Запустить исправленную версию:**
   ```bash
   ./gradlew runMemoryLeakFixed
   ```

6. **Снять heap dump снова и сравнить:**
   ```bash
   ./scripts/analyze_heap.sh heap_dumps/heap_leak_oom.hprof
   ./scripts/analyze_heap.sh heap_dumps/heap_fixed.hprof
   ```

## Дополнительные ресурсы

### JIT Optimization
- [JIT Optimization Guide](docs/jit/JIT_OPTIMIZATION_GUIDE.md) - подробное руководство
- [Oracle HotSpot VM Options](https://www.oracle.com/technical-resources/articles/java/vmoptions-jsp.html)
- [JMH - Java Microbenchmark Harness](https://github.com/openjdk/jmh)

### Garbage Collection
- [GC & Memory Leak Guide](docs/gc/GC_MEMORY_LEAK_GUIDE.md) - подробное руководство по GC и утечкам
- [GC Comparison Guide](docs/gc/GC_COMPARISON_GUIDE.md) - сравнение всех GC, когда какой использовать
- [GCEasy](https://gceasy.io/) - анализ GC логов онлайн
- [Eclipse MAT](https://www.eclipse.org/mat/) - Memory Analyzer Tool
- [VisualVM](https://visualvm.github.io/) - профилирование и мониторинг

### Performance Profiling
- [Async-Profiler Guide](docs/profiling/ASYNC_PROFILER_GUIDE.md) - профилирование с async-profiler
- [async-profiler](https://github.com/async-profiler/async-profiler) - Low-overhead Java profiler
- [Flame Graphs](http://www.brendangregg.com/flamegraphs.html) - Visualization technique
- [JDK Mission Control](https://www.oracle.com/java/technologies/javase/products-jmc8-downloads.html) - JFR analyzer
