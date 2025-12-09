# GC Comparison Guide - Сравнение Garbage Collectors

Подробное руководство по сравнению различных Garbage Collector'ов в Java с практическими примерами и интерпретацией результатов.

## Содержание

1. [Обзор Garbage Collectors](#обзор-garbage-collectors)
2. [Ключевые метрики](#ключевые-метрики)
3. [Benchmarks и их цели](#benchmarks-и-их-цели)
4. [Запуск бенчмарков](#запуск-бенчмарков)
5. [Интерпретация результатов](#интерпретация-результатов)
6. [Рекомендации по выбору GC](#рекомендации-по-выбору-gc)
7. [Настройка GC](#настройка-gc)

---

## Обзор Garbage Collectors

### 1. Serial GC (`-XX:+UseSerialGC`)

**Описание:** Самый простой GC, single-threaded.

**Характеристики:**
- ✅ Минимальный memory footprint
- ✅ Простой и предсказуемый
- ❌ Останавливает приложение для всех сборок (Stop-The-World)
- ❌ Не использует multi-core

**Когда использовать:**
- Маленькие приложения (heap < 100MB)
- Single-core окружение
- Клиентские приложения
- Контейнеры с ограниченной памятью

**Не использовать:**
- Multi-core серверы
- Большие heap (> 1GB)
- Latency-sensitive приложения

### 2. Parallel GC (`-XX:+UseParallelGC`)

**Описание:** Multi-threaded GC, оптимизированный для throughput.

**Характеристики:**
- ✅ Максимальный throughput
- ✅ Эффективно использует multi-core для GC
- ✅ Хорош для batch processing
- ❌ Длинные Stop-The-World паузы
- ❌ Плохо для latency-sensitive приложений

**Когда использовать:**
- Batch processing
- Data analytics
- Когда throughput важнее latency
- Многопоточные серверы без требований к latency

**Не использовать:**
- REST APIs с требованиями к latency
- Real-time системы
- Интерактивные приложения

### 3. G1 GC (`-XX:+UseG1GC`)

**Описание:** Generational GC с предсказуемыми паузами. **Default с Java 9+**.

**Характеристики:**
- ✅ Баланс между throughput и latency
- ✅ Предсказуемые паузы (`-XX:MaxGCPauseMillis=N`)
- ✅ Хорошо работает с большими heap (> 4GB)
- ✅ Инкрементальные сборки
- ⚠️ Больше CPU overhead чем Parallel GC

**Когда использовать:**
- Универсальный выбор для большинства приложений
- Heap > 4GB
- Когда нужен баланс throughput/latency
- Микросервисы и REST APIs

**Настройка:**
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200        # Целевое время паузы (200ms default)
-XX:G1HeapRegionSize=16M        # Размер региона (1-32MB)
-XX:InitiatingHeapOccupancyPercent=45  # Когда начинать concurrent marking
```

### 4. ZGC (`-XX:+UseZGC`)

**Описание:** Ultra-low latency GC. **Требует Java 15+ для production**.

**Характеристики:**
- ✅ Паузы < 10ms (обычно < 1ms)
- ✅ Работает с heap до 16TB
- ✅ Concurrent: большая часть работы параллельна с приложением
- ✅ Не зависит от размера heap
- ❌ Больше CPU overhead (~15-20%)
- ❌ Больше memory overhead

**Когда использовать:**
- Ultra-low latency требования (p99 < 10ms)
- Очень большие heap (> 100GB)
- Real-time системы
- Trading systems, gaming servers

**Настройка:**
```bash
-XX:+UseZGC
-XX:ZCollectionInterval=5       # Минимальный интервал между GC (секунды)
-XX:ZAllocationSpikeTolerance=2 # Tolerance для allocation spikes
```

**Требования:**
- Java 15+ (production-ready с Java 15)
- Linux x64, macOS, Windows
- Минимум 8GB heap рекомендуется

### 5. Shenandoah GC (`-XX:+UseShenandoahGC`)

**Описание:** Low-latency concurrent GC. **Требует специальную сборку OpenJDK**.

**Характеристики:**
- ✅ Паузы < 10ms
- ✅ Concurrent compaction
- ✅ Меньше memory overhead чем ZGC
- ❌ Немного меньше throughput
- ❌ Не входит в Oracle JDK

**Когда использовать:**
- Low latency требования
- Heap 4-100GB
- Когда ZGC недоступен
- Альтернатива ZGC с меньшим memory footprint

**Настройка:**
```bash
-XX:+UseShenandoahGC
-XX:ShenandoahGCMode=iu         # "incremental update" mode (default)
```

**Доступность:**
- OpenJDK 12+ (Red Hat сборки)
- Не в Oracle JDK
- AdoptOpenJDK включает Shenandoah

---

## Ключевые метрики

### Latency Метрики

**Определение:** Время отклика приложения на запрос.

**Ключевые показатели:**
- **p50 (median)**: 50% запросов быстрее этого значения
- **p95**: 95% запросов быстрее (важно для SLA)
- **p99**: 99% запросов быстрее (**критично для latency-sensitive!**)
- **p999**: 99.9% запросов быстрее
- **max**: Максимальная latency

**На что влияет GC:**
- GC паузы напрямую добавляются к latency
- Stop-The-World паузы = latency spike для всех запросов

**Цели по latency:**
```
Web API (общий):
  p99 < 100ms, p999 < 500ms

Trading/Gaming (real-time):
  p99 < 10ms, p999 < 50ms

Batch processing:
  latency не критична
```

### Throughput Метрики

**Определение:** Количество работы, выполненной за единицу времени.

**Ключевые показатели:**
- **Operations/second**: Количество операций
- **GC Overhead**: Процент времени в GC vs полезной работы
- **Throughput**: `100% - GC Overhead`

**На что влияет GC:**
- Время, потраченное на GC, не выполняется полезная работа
- Concurrent GC крадет CPU у приложения

**Цели по throughput:**
```
High throughput:
  GC Overhead < 1%

Acceptable:
  GC Overhead < 5%

Problem:
  GC Overhead > 10%
```

### GC Pause Метрики

**Определение:** Время, когда приложение полностью остановлено (Stop-The-World).

**Ключевые показатели:**
- **Young GC pause**: Обычно короткие (< 10ms)
- **Old GC pause**: Могут быть долгими (100ms+)
- **Full GC pause**: Самые длинные (секунды)
- **GC pause frequency**: Как часто происходят паузы

**Примеры:**
```
Serial GC:
  Все паузы Stop-The-World
  Young GC: 5-50ms
  Full GC: 100ms - 5sec

Parallel GC:
  Young GC: 5-20ms (parallel)
  Full GC: 50ms - 2sec (parallel)

G1 GC:
  Young GC: 5-20ms
  Mixed GC: 10-50ms
  Concurrent marking: < 1ms паузы

ZGC:
  Все паузы: < 10ms (обычно < 1ms)
  Независимо от размера heap
```

### Memory Метрики

**Определение:** Использование памяти и overhead GC.

**Ключевые показатели:**
- **Heap usage**: Сколько heap используется
- **GC memory overhead**: Дополнительная память для GC структур
- **Allocation rate**: MB/sec новых объектов
- **Promotion rate**: Скорость перемещения в Old Gen

**Overhead по GC:**
```
Serial/Parallel GC:
  ~2-5% memory overhead

G1 GC:
  ~10-15% memory overhead
  Зависит от количества регионов

ZGC:
  ~15-20% memory overhead
  Для colored pointers и load barriers

Shenandoah:
  ~10-15% memory overhead
```

---

## Benchmarks и их цели

### 1. ThroughputBenchmark

**Цель:** Измерить максимальную производительность (ops/sec).

**Что тестирует:**
- Массовое создание объектов
- Вычисления
- Общий throughput

**Ожидаемые результаты:**
```
Parallel GC:  ★★★★★ Лучший throughput
G1 GC:        ★★★★☆ Хороший throughput
Serial GC:    ★★☆☆☆ Низкий throughput
ZGC:          ★★★☆☆ Средний (из-за overhead)
```

**Куда смотреть:**
- **Operations/second** - чем больше, тем лучше
- **Время на итерацию (p50, p95)** - должно быть стабильным
- **GC overhead** - чем меньше, тем лучше

### 2. LatencyBenchmark

**Цель:** Измерить latency и стабильность времени отклика.

**Что тестирует:**
- Latency каждого запроса
- P99, p999 latency
- Impact GC пауз на latency

**Ожидаемые результаты:**
```
ZGC:          ★★★★★ Лучшая p99 latency
Shenandoah:   ★★★★★ Отличная p99 latency
G1 GC:        ★★★☆☆ Приемлемая p99 latency
Parallel GC:  ★★☆☆☆ Высокая p99 latency
Serial GC:    ★☆☆☆☆ Очень высокая p99 latency
```

**Куда смотреть:**
- **p99 latency** - критично! Должна быть < 10ms для real-time
- **p999 latency** - показывает worst-case
- **Max latency** - показывает longest GC pause
- **Запросы > 1ms** - процент медленных запросов

### 3. MixedWorkloadBenchmark

**Цель:** Реалистичная нагрузка с короткими и долгоживущими объектами.

**Что тестирует:**
- Young Generation pressure
- Old Generation growth
- Cache management
- Burst handling

**Ожидаемые результаты:**
```
G1 GC:        ★★★★★ Лучший баланс
ZGC:          ★★★★☆ Отличная latency
Parallel GC:  ★★★☆☆ Хороший throughput, но latency
Shenandoah:   ★★★★☆ Хорошая latency
Serial GC:    ★★☆☆☆ Подходит только для малого heap
```

**Куда смотреть:**
- **Throughput** - ops/sec
- **Latency (p95, p99)** - стабильность
- **GC frequency** - как часто срабатывает
- **Cache size** - стабильность долгоживущих объектов

### 4. AllocationBenchmark

**Цель:** Тест максимальной allocation rate и Young GC.

**Что тестирует:**
- Как часто срабатывает Young GC
- Длительность Young GC
- Способность обрабатывать высокую allocation rate

**Ожидаемые результаты:**
```
Parallel GC:  ★★★★★ Быстрый Young GC
G1 GC:        ★★★★☆ Хороший Young GC
ZGC:          ★★★★☆ Concurrent, минимальные паузы
Serial GC:    ★★☆☆☆ Медленный Young GC
```

**Куда смотреть:**
- **Allocation rate** - MB/sec
- **Young GC frequency** - сборок/sec
- **Young GC pause time** - должна быть < 10ms
- **GC overhead** - процент времени в GC

---

## Запуск бенчмарков

### Быстрый запуск

```bash
# Скомпилировать
./gradlew build

# Throughput тест с разными GC
./gradlew runThroughputSerial      # Serial GC
./gradlew runThroughputParallel    # Parallel GC
./gradlew runThroughputG1          # G1 GC
./gradlew runThroughputZGC         # ZGC (требуется Java 15+)

# Latency тест с разными GC
./gradlew runLatencySerial
./gradlew runLatencyParallel
./gradlew runLatencyG1
./gradlew runLatencyZGC

# Mixed workload
./gradlew runMixedSerial
./gradlew runMixedParallel
./gradlew runMixedG1
./gradlew runMixedZGC

# Allocation test
./gradlew runAllocationSerial
./gradlew runAllocationParallel
./gradlew runAllocationG1
./gradlew runAllocationZGC
```

### Запуск с GC логами

```bash
# С детальными GC логами
./gradlew runThroughputG1WithLogs
./gradlew runLatencyZGCWithLogs

# Логи сохраняются в gc_benchmarks/
```

### Автоматическое сравнение всех GC

```bash
# Запустить все GC для одного бенчмарка и сравнить
./scripts/compare_gc.sh throughput

# Или для latency
./scripts/compare_gc.sh latency

# Для всех бенчмарков
./scripts/compare_gc.sh all
```

---

## Интерпретация результатов

### Throughput Benchmark

**Пример вывода:**
```
=== Throughput Benchmark ===
GC: PS Scavenge, PS MarkSweep
Heap: 4096 MB

РЕЗУЛЬТАТЫ
Общая производительность: 1,234,567 ops/sec
p50 время: 1,200 ms
p95 время: 1,450 ms
p99 время: 1,800 ms
```

**Интерпретация:**
- **ops/sec** - главная метрика для throughput
  - Serial GC: ~500k-800k ops/sec
  - Parallel GC: ~1.2M-1.5M ops/sec (лучший)
  - G1 GC: ~1M-1.3M ops/sec
  - ZGC: ~900k-1.1M ops/sec

- **p99 время** - показывает вариативность
  - Если p99 >> p50, значит есть outliers (вероятно GC паузы)
  - Parallel GC обычно показывает большую вариативность

### Latency Benchmark

**Пример вывода:**
```
=== Latency Benchmark ===
GC: ZGC

Latency (время отклика):
  p50:     120.45 μs
  p99:   1,234.56 μs  ← ВАЖНО!
  p999:  5,678.90 μs
  max:  12,345.67 μs

Запросы с latency > 1ms: 1,234 (1.23%)
Возможные GC паузы (> 10ms): 5 (0.005%)
```

**Интерпретация:**
- **p99 latency** - критичная метрика
  - ZGC/Shenandoah: < 2ms (отлично)
  - G1 GC: 5-20ms (хорошо)
  - Parallel GC: 20-100ms (плохо для latency)
  - Serial GC: 50-500ms (очень плохо)

- **Запросы > 10ms** - вероятно GC паузы
  - ZGC: ~0.001% (почти нет)
  - G1 GC: ~0.1-1%
  - Parallel GC: ~5-10%

- **Max latency** - показывает longest pause
  - Если max >> p999, был rare spike (Full GC?)

### Mixed Workload Benchmark

**Пример вывода:**
```
=== Mixed Workload Benchmark ===
GC: G1 Young Generation, G1 Old Generation

РЕЗУЛЬТАТЫ
Throughput: 12,345 ops/sec

Latency операций:
  p50:  1,234 μs
  p95:  5,678 μs
  p99:  9,012 μs

GC Статистика:
  G1 Young Generation:
    Коллекций: 1,234
    Время: 12,345 ms
    Средняя пауза: 10.00 ms

  G1 Old Generation:
    Коллекций: 5
    Время: 123 ms
    Средняя пауза: 24.60 ms

GC overhead: 2.15%
Throughput: 97.85%
```

**Интерпретация:**
- **Throughput (ops/sec)** - общая производительность
  - Должен быть стабильным на протяжении теста
  - Падение throughput = проблемы с GC

- **Young GC frequency** - нормально ~1-10 сборок/сек
  - Слишком часто (> 20/sec) = маленький Young Gen или высокая allocation rate
  - Слишком редко (< 0.1/sec) = большой Young Gen, но длинные паузы

- **Old GC (Full GC)** - должны быть редкими
  - > 1 Full GC/min = проблема (memory leak или undersized heap)
  - Full GC pause > 1sec = большая проблема для latency

- **GC overhead** - процент времени в GC
  - < 1% - отлично
  - 1-5% - хорошо
  - 5-10% - приемлемо
  - \> 10% - проблема! Нужно увеличить heap или сменить GC

### Allocation Benchmark

**Пример вывода:**
```
=== Allocation Rate Benchmark ===
GC: G1 Young Generation

РЕЗУЛЬТАТЫ
Allocation rate: 1,234 MB/sec
Object creation rate: 1,234,567 obj/sec

GC Статистика:
  G1 Young Generation:
    Коллекций: 456
    Время: 4,567 ms
    Средняя пауза: 10.01 ms
    Частота: 15.20 сборок/сек

GC overhead: 1.52%
Throughput: 98.48%
```

**Интерпретация:**
- **Allocation rate** - скорость создания объектов
  - Parallel GC: обычно fastest (> 1.5 GB/sec)
  - G1 GC: ~1-1.5 GB/sec
  - ZGC: ~800MB-1.2 GB/sec (concurrent overhead)

- **Young GC frequency** - как часто собирается Young Gen
  - Parallel/Serial: зависит от Eden size
  - G1: adaptive, ~5-20 сборок/сек
  - ZGC: concurrent, меньше заметных пауз

- **Средняя Young GC pause** - критична
  - ZGC: < 1ms
  - Parallel/G1: 5-15ms (ok)
  - > 50ms = проблема

---

## Рекомендации по выбору GC

### Decision Tree

```
Выбор GC зависит от:
1. Требования к latency
2. Размер heap
3. Throughput vs Latency приоритет
4. Доступная Java версия

START HERE:
│
├─ Требования к latency p99 < 10ms?
│  ├─ ДА → ZGC (Java 15+) или Shenandoah
│  └─ НЕТ ↓
│
├─ Heap size > 4GB?
│  ├─ ДА → G1 GC (default choice)
│  └─ НЕТ ↓
│
├─ Batch processing / throughput критичен?
│  ├─ ДА → Parallel GC
│  └─ НЕТ ↓
│
└─ Маленькое приложение (< 100MB heap)?
   ├─ ДА → Serial GC
   └─ НЕТ → G1 GC
```

### Use Cases

#### Web Application / REST API

**Характеристики:**
- Средний heap (1-8 GB)
- Latency важна (p99 < 100ms)
- Throughput важен

**Рекомендация:**
1. **G1 GC** (default, универсальный выбор)
   ```bash
   -XX:+UseG1GC
   -XX:MaxGCPauseMillis=100
   -Xmx4g
   ```

2. **ZGC** (если p99 < 10ms критично)
   ```bash
   -XX:+UseZGC
   -Xmx4g
   ```

#### Batch Processing / Analytics

**Характеристики:**
- Большие объемы данных
- Latency не критична
- Throughput критичен

**Рекомендация:**
1. **Parallel GC** (максимальный throughput)
   ```bash
   -XX:+UseParallelGC
   -Xmx16g
   ```

2. **G1 GC** (если нужен баланс)
   ```bash
   -XX:+UseG1GC
   -Xmx16g
   ```

#### Trading / Gaming / Real-time

**Характеристики:**
- Ultra-low latency (p99 < 10ms)
- Предсказуемость критична
- Большой heap возможен

**Рекомендация:**
1. **ZGC** (лучший выбор)
   ```bash
   -XX:+UseZGC
   -Xmx16g
   -XX:ZCollectionInterval=5
   ```

2. **Shenandoah** (альтернатива ZGC)
   ```bash
   -XX:+UseShenandoahGC
   -Xmx16g
   ```

#### Microservices / Containers

**Характеристики:**
- Ограниченная память (< 1GB)
- Быстрый startup
- Средняя latency

**Рекомендация:**
1. **G1 GC** (с маленьким heap)
   ```bash
   -XX:+UseG1GC
   -Xmx512m
   -XX:MaxGCPauseMillis=50
   ```

2. **Serial GC** (если очень мало памяти)
   ```bash
   -XX:+UseSerialGC
   -Xmx256m
   ```

---

## Настройка GC

### Общие флаги

```bash
# Heap size
-Xmx<size>      # Максимальный heap (например, -Xmx4g)
-Xms<size>      # Начальный heap (обычно = Xmx)

# GC logging (Java 9+)
-Xlog:gc*:file=gc.log:time,level,tags

# GC logging (Java 8)
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:gc.log
```

### G1 GC Tuning

```bash
-XX:+UseG1GC

# Целевое время паузы (default: 200ms)
-XX:MaxGCPauseMillis=100

# Размер региона (1-32MB, auto by default)
-XX:G1HeapRegionSize=16M

# Когда начинать concurrent marking (default: 45%)
-XX:InitiatingHeapOccupancyPercent=35

# Размер Young Generation (default: 5-60% heap)
-XX:G1NewSizePercent=5          # Минимум
-XX:G1MaxNewSizePercent=60      # Максимум

# Сколько Mixed GC после concurrent cycle
-XX:G1MixedGCCountTarget=8
```

**Пример конфигурации:**
```bash
java -Xmx4g -Xms4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:G1HeapRegionSize=8M \
  -Xlog:gc*:file=gc.log:time \
  -jar app.jar
```

### ZGC Tuning

```bash
-XX:+UseZGC

# Минимальный интервал между GC (секунды)
-XX:ZCollectionInterval=5

# Tolerance для allocation spikes
-XX:ZAllocationSpikeTolerance=2

# Soft max heap (soft limit, может превысить)
-XX:SoftMaxHeapSize=8g
```

**Пример конфигурации:**
```bash
java -Xmx16g -Xms16g \
  -XX:+UseZGC \
  -XX:ZCollectionInterval=5 \
  -XX:+ZGenerational \    # Generational ZGC (Java 21+)
  -Xlog:gc*:file=gc.log:time \
  -jar app.jar
```

### Parallel GC Tuning

```bash
-XX:+UseParallelGC

# Количество GC threads
-XX:ParallelGCThreads=8

# Целевой GC time ratio (default: 99 = 1% времени в GC)
-XX:GCTimeRatio=99

# Maximum pause time goal (best effort)
-XX:MaxGCPauseMillis=500

# Размер Young Generation
-XX:NewRatio=2              # Old/Young ratio
-XX:SurvivorRatio=8         # Eden/Survivor ratio
```

**Пример конфигурации:**
```bash
java -Xmx8g -Xms8g \
  -XX:+UseParallelGC \
  -XX:ParallelGCThreads=8 \
  -XX:GCTimeRatio=99 \
  -Xlog:gc*:file=gc.log:time \
  -jar app.jar
```

---

## Мониторинг GC в Production

### Real-time мониторинг

```bash
# jstat - GC statistics
jstat -gcutil <pid> 1000

# Следить за:
# O (Old Gen) - не должен постоянно расти
# FGC (Full GC count) - должен быть редким
```

### GC Log Analysis

**Загрузить GC лог в:**
- **GCEasy**: https://gceasy.io/ - онлайн анализ
- **GCViewer**: https://github.com/chewiebug/GCViewer - desktop tool

**Что искать:**
- **GC pause time trends** - растут ли паузы?
- **Heap usage после GC** - падает ли?
- **Full GC frequency** - как часто?
- **Allocation rate** - стабильная ли?

### JVM Metrics

**Prometheus + Micrometer:**
```java
// jvm.gc.pause - GC pause times
// jvm.gc.memory.allocated - allocation rate
// jvm.memory.used - heap usage
```

**Grafana dashboards:**
- JVM (Micrometer) dashboard
- GC metrics visualization

---

## Troubleshooting

### Проблема: Высокая latency (p99 > 100ms)

**Симптомы:**
- p99/p999 latency намного выше p50
- Max latency очень высокая
- Периодические спайки в latency

**Диагностика:**
```bash
# Проверить GC паузы
jstat -gcutil <pid> 1000
# Смотреть на FGC (Full GC) - если растет, это проблема

# Посмотреть GC лог
# Искать длинные паузы (> 100ms)
```

**Решение:**
1. **Увеличить heap** (если Old Gen > 90%)
   ```bash
   -Xmx8g  # было 4g
   ```

2. **Переключиться на low-latency GC**
   ```bash
   -XX:+UseZGC  # вместо Parallel/G1
   ```

3. **Tune G1 GC** (если используется G1)
   ```bash
   -XX:MaxGCPauseMillis=50  # было 200
   -XX:InitiatingHeapOccupancyPercent=35  # было 45, начинать раньше
   ```

### Проблема: Низкий throughput

**Симптомы:**
- Низкий ops/sec
- Высокий GC overhead (> 10%)
- Частые GC

**Диагностика:**
```bash
# Проверить GC overhead
jstat -gcutil <pid> 1000
# Считать: (FGC_time + YGC_time) / total_time

# Посмотреть allocation rate
jstat -gc <pid> 1000
# EC (Eden Capacity) - как быстро заполняется
```

**Решение:**
1. **Увеличить heap**
   ```bash
   -Xmx16g  # было 8g
   ```

2. **Переключиться на throughput GC**
   ```bash
   -XX:+UseParallelGC  # максимальный throughput
   ```

3. **Tune Young Generation**
   ```bash
   -XX:NewRatio=1  # 50% heap для Young Gen
   ```

### Проблема: Memory Leak

**Симптомы:**
- Old Generation постоянно растет
- Full GC не освобождает память
- Eventual OutOfMemoryError

**Диагностика:**
```bash
# Мониторить Old Gen
jstat -gcutil <pid> 1000
# O (Old Gen) - если растет 50% -> 60% -> 70% -> 80%, это leak

# Снять heap dump
jcmd <pid> GC.heap_dump heap.hprof
```

**Решение:**
- Не GC проблема! Это application problem
- Анализировать heap dump (см. GC_MEMORY_LEAK_GUIDE.md)
- Искать утечки памяти в коде

---

## Дополнительные ресурсы

### Документация
- [Java GC Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc/Main)
- [G1 GC Documentation](https://www.oracle.com/technical-resources/articles/java/g1gc.html)

### Инструменты
- [GCEasy](https://gceasy.io/) - GC log analysis
- [GCViewer](https://github.com/chewiebug/GCViewer) - GC log viewer
- [Java Mission Control](https://www.oracle.com/java/technologies/javase/products-jmc8-downloads.html) - Profiling

### Статьи
- [ZGC: A Scalable Low-Latency Garbage Collector](https://www.oracle.com/technical-resources/articles/java/zgc.html)
- [Shenandoah GC](https://wiki.openjdk.org/display/shenandoah/Main)
- [G1 GC Deep Dive](https://www.oracle.com/technetwork/tutorials/tutorials-1876574.html)

---

## Quick Reference

```bash
# Запуск всех бенчмарков с разными GC
./scripts/compare_gc.sh all

# Throughput сравнение
./gradlew runThroughputSerial runThroughputParallel runThroughputG1 runThroughputZGC

# Latency сравнение
./gradlew runLatencySerial runLatencyParallel runLatencyG1 runLatencyZGC

# Анализ GC логов
# Загрузить gc.log в https://gceasy.io/

# Мониторинг в runtime
jstat -gcutil <pid> 1000
```

---

**Совет:** Начните с G1 GC (default), измерьте метрики, и только потом переключайтесь на другой GC если есть конкретная проблема!
