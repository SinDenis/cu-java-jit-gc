# Быстрая справка команд

## Запуск примеров

### JIT Optimization Examples
```bash
./gradlew runInlineExample           # Inline оптимизации
./gradlew runLoopExample             # Loop оптимизации
./gradlew runEscapeExample           # Escape analysis
./gradlew runDeadCodeExample         # Dead code elimination
./gradlew runBranchExample           # Branch prediction

# С JIT debug выводом
./gradlew runInlineExampleDebug
./gradlew runEscapeExampleDebug
```

### GC & Memory Leak Examples
```bash
# С утечкой памяти
./gradlew runMemoryLeak              # Static collection leak (1GB, ~40 мин)
./gradlew runMemoryLeakSmallHeap     # Static collection leak (512MB, ~5-10 мин)
./gradlew runListenerLeak            # Listener leak (1GB)
./gradlew runListenerLeakSmallHeap   # Listener leak (256MB, очень быстро)

# Исправленные версии
./gradlew runMemoryLeakFixed
./gradlew runListenerLeakFixed

# С GC логами
./gradlew runMemoryLeakWithGCLogs
./gradlew runMemoryLeakFixedWithGCLogs
```

## Мониторинг и анализ

### Найти Java процессы
```bash
jps -l
```

### GC Мониторинг
```bash
# Через скрипт (интерактивный)
./scripts/gc_monitor.sh

# Напрямую
jstat -gcutil <pid> 1000    # Обновление каждую секунду
jstat -gcutil -t <pid> 1000 # С временными метками
```

### Heap Dump

**Снять heap dump:**
```bash
# Через скрипт (интерактивный)
./scripts/heap_dump.sh

# Напрямую - все объекты
jcmd <pid> GC.heap_dump heap_all.hprof

# Напрямую - только живые объекты (запускает Full GC)
jcmd <pid> GC.heap_dump heap_live.hprof -all=false

# Или через jmap
jmap -dump:live,format=b,file=heap.hprof <pid>
```

**Анализировать heap dump:**
```bash
# Через скрипт (интерактивный)
./scripts/analyze_heap.sh heap.hprof

# Histogram
jmap -histo heap.hprof | head -30

# Открыть в VisualVM
jvisualvm --openfile heap.hprof

# Открыть в Eclipse MAT
mat heap.hprof
```

### Быстрый histogram живых объектов
```bash
jmap -histo:live <pid> | head -30
```

### Thread dump
```bash
jcmd <pid> Thread.print
# или
jstack <pid>
```

### JVM флаги процесса
```bash
jcmd <pid> VM.flags
jcmd <pid> VM.system_properties
```

### Force Full GC (только для тестирования!)
```bash
jcmd <pid> GC.run
```

## Полезные JVM флаги

### Heap size
```bash
-Xmx1g          # Максимальный heap
-Xms512m        # Начальный heap
```

### GC Logging
```bash
# Java 9+
-Xlog:gc*:file=gc.log:time,level,tags

# Java 8
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log
```

### Heap Dump на OOM
```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=./heap_dumps/
-XX:OnOutOfMemoryError="echo 'OOM occurred'"
```

### JIT Debug
```bash
-XX:+PrintCompilation              # Показать когда методы компилируются
-XX:+UnlockDiagnosticVMOptions     # Разблокировать диагностические флаги
-XX:+PrintInlining                 # Показать inline решения
-XX:+PrintEscapeAnalysis           # Показать escape analysis
```

### GC Выбор
```bash
-XX:+UseG1GC            # G1 GC (по умолчанию Java 9+)
-XX:+UseParallelGC      # Parallel GC
-XX:+UseSerialGC        # Serial GC
-XX:+UseZGC             # ZGC (low-latency)
-XX:+UseShenandoahGC    # Shenandoah GC (low-pause)
```

## Workflow для обнаружения утечки

### Шаг 1: Запуск с утечкой
```bash
# Терминал 1
./gradlew runMemoryLeakSmallHeap
```

### Шаг 2: Найти PID
```bash
# Терминал 2
jps -l | grep MemoryLeak
# Запомнить PID, например: 12345
```

### Шаг 3: Мониторинг
```bash
# Терминал 2
jstat -gcutil 12345 1000

# Следите за:
# O (Old Gen) - должен расти
# FGC - количество Full GC должно расти
```

### Шаг 4: Снять heap dump
```bash
# Когда Old Gen > 70%
jcmd 12345 GC.heap_dump heap_dumps/heap_leak_70pct.hprof
```

### Шаг 5: Дождаться OOM
Heap dump создастся автоматически при OOM:
`heap_dumps/heap_leak_oom.hprof`

### Шаг 6: Анализ
```bash
# Histogram
jmap -histo heap_dumps/heap_leak_oom.hprof | head -30

# Или открыть в VisualVM
jvisualvm --openfile heap_dumps/heap_leak_oom.hprof
```

### Шаг 7: Запустить исправленную версию
```bash
# Терминал 1
./gradlew runMemoryLeakFixed

# Терминал 2 - мониторинг
jstat -gcutil <new_pid> 1000

# Снять heap dump для сравнения
jcmd <new_pid> GC.heap_dump heap_dumps/heap_fixed.hprof
```

### Шаг 8: Сравнить
```bash
./scripts/analyze_heap.sh heap_dumps/heap_leak_oom.hprof
./scripts/analyze_heap.sh heap_dumps/heap_fixed.hprof

# Или в Eclipse MAT:
# File -> Compare To Another Heap Dump
```

## Признаки утечки памяти

### В jstat -gcutil
```
✗ Утечка:
O:  50% → 65% → 78% → 89% → 95% → 98% → OOM
FGC: 5  →  10  →  23  →  45  →  89  → 234

✓ Нормально:
O:  40% → 55% → 42% → 48% → 45% → 50%
FGC: 5  →   6  →   7  →   8  →   9  →  10
```

### В Heap Dump (Eclipse MAT)
```
✗ Утечка:
- Dominator Tree: Один объект занимает >50% heap
- Histogram: Аномально много экземпляров одного класса
- Path to GC Root: Все ведут к одному static полю

✓ Нормально:
- Распределение памяти равномерное
- Нет доминирующих объектов
- Разнообразные пути к GC roots
```

## Инструменты

### VisualVM
```bash
# macOS
brew install --cask visualvm

# Запуск
jvisualvm

# Или встроенный в JDK (до Java 8)
jvisualvm
```

### Eclipse MAT
Скачать: https://www.eclipse.org/mat/downloads.php

### GCEasy (онлайн)
https://gceasy.io/ - загрузить gc.log

### jconsole (встроен)
```bash
jconsole <pid>
```

## Дополнительная информация

- **[JIT_OPTIMIZATION_GUIDE.md](JIT_OPTIMIZATION_GUIDE.md)** - Подробное руководство по JIT
- **[GC_MEMORY_LEAK_GUIDE.md](GC_MEMORY_LEAK_GUIDE.md)** - Подробное руководство по GC
- **[README.md](README.md)** - Основная документация проекта
