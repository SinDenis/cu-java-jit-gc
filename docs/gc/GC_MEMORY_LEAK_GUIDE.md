# Garbage Collection и Memory Leak Analysis - Практическое Руководство

Этот документ содержит примеры утечек памяти в Java и подробные инструкции по их обнаружению и устранению с помощью heap dump анализа.

## Содержание

1. [Обзор примеров](#обзор-примеров)
2. [Быстрый старт](#быстрый-старт)
3. [Снятие Heap Dump](#снятие-heap-dump)
4. [Анализ Heap Dump](#анализ-heap-dump)
5. [Сравнение До и После](#сравнение-до-и-после)
6. [GC Мониторинг](#gc-мониторинг)
7. [Инструменты анализа](#инструменты-анализа)
8. [Типичные паттерны утечек](#типичные-паттерны-утечек)

---

## Обзор примеров

### 1. Static Collection Leak (MemoryLeakExample)

**Проблема:** Статическая коллекция `ACTIVE_SESSIONS` постоянно растет и никогда не очищается.

**Симптомы:**
- Постоянный рост используемой памяти
- Частые Full GC, которые не освобождают память
- Eventual OutOfMemoryError

**Исправление (MemoryLeakFixedExample):**
- ✓ Автоматическое удаление старых объектов (TTL - Time To Live)
- ✓ Ограничение максимального размера коллекции
- ✓ Периодическая очистка устаревших данных

### 2. Listener/Callback Leak (ListenerLeakExample)

**Проблема:** Объекты регистрируются как listeners, но никогда не отписываются. EventBus держит ссылки на все объекты.

**Симптомы:**
- Объекты, которые должны быть garbage collected, остаются в памяти
- Количество listeners постоянно растет
- Увеличение времени обработки событий

**Исправление (ListenerLeakFixedExample):**
- ✓ Метод `unregister()` для явной отписки
- ✓ Использование `WeakReference` для автоматической очистки
- ✓ Паттерн `AutoCloseable` с try-with-resources
- ✓ Периодическая очистка мертвых ссылок

---

## Быстрый старт

### Шаг 1: Запуск примера С утечкой

```bash
# Вариант 1: Static Collection Leak (быстрее достигает OOM)
./gradlew runMemoryLeak

# Вариант 2: Listener Leak
./gradlew runListenerLeak

# С ограничением памяти для быстрого воспроизведения (512MB)
./gradlew runMemoryLeakSmallHeap

# С GC логами
./gradlew runMemoryLeakWithGCLogs
```

**Что наблюдать:**
- Открыть отдельный терминал и запустить: `jstat -gcutil <pid> 1000`
- Память постоянно растет
- Full GC срабатывает чаще, но не освобождает память
- В конце - OutOfMemoryError

### Шаг 2: Снять Heap Dump

**Вариант A: Снять вручную во время работы**
```bash
# Найти PID процесса
jps -l | grep MemoryLeak

# Снять heap dump
jcmd <pid> GC.heap_dump heap_with_leak.hprof

# Или через jmap
jmap -dump:live,format=b,file=heap_with_leak.hprof <pid>
```

**Вариант B: Автоматический dump при OOM**

Приложение уже запущено с флагом `-XX:+HeapDumpOnOutOfMemoryError`, поэтому heap dump создастся автоматически при OOM.

### Шаг 3: Запуск исправленной версии

```bash
# Исправленная версия Static Collection
./gradlew runMemoryLeakFixed

# Исправленная версия Listener
./gradlew runListenerLeakFixed

# Снять heap dump для сравнения
jcmd <pid> GC.heap_dump heap_without_leak.hprof
```

**Что наблюдать:**
- Память остается стабильной
- Периодические сообщения об очистке: "🧹 Очистка: удалено N объектов"
- GC работает эффективно
- Никакого OutOfMemoryError

### Шаг 4: Сравнить Heap Dumps

```bash
# Открыть в VisualVM
jvisualvm

# Или в Eclipse MAT
# File -> Open Heap Dump -> выбрать heap_with_leak.hprof
```

---

## Снятие Heap Dump

### Методы снятия heap dump

#### 1. jcmd (Рекомендуется)

```bash
# Найти PID Java процесса
jps -l

# Снять heap dump
jcmd <pid> GC.heap_dump filename=heap_dump.hprof

# Снять только live объекты (запускает Full GC перед dump)
jcmd <pid> GC.heap_dump filename=heap_dump_live.hprof -live
```

**Преимущества:**
- Современный способ (Java 7+)
- Надежный и быстрый
- Встроенный в JDK

#### 2. jmap

```bash
# Снять все объекты
jmap -dump:format=b,file=heap_dump.hprof <pid>

# Снять только live объекты
jmap -dump:live,format=b,file=heap_dump_live.hprof <pid>

# Посмотреть histogram объектов в памяти
jmap -histo:live <pid> | head -50
```

**Примечание:** `jmap` может быть deprecated в будущем, используйте `jcmd`.

#### 3. Автоматический dump при OOM

Добавить JVM флаги при запуске:
```bash
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=./heap_dumps/ \
     -XX:OnOutOfMemoryError="echo 'OOM occurred at %date%'" \
     YourApplication
```

#### 4. Через JMX (JConsole, VisualVM)

**JConsole:**
1. Подключиться к процессу
2. MBeans → com.sun.management → HotSpotDiagnostic
3. Operations → dumpHeap
4. Указать путь и нажать "dumpHeap"

**VisualVM:**
1. Подключиться к процессу
2. Monitor tab → "Heap Dump" button
3. Dump сохранится автоматически

---

## Анализ Heap Dump

### Что искать в heap dump

#### 1. Dominator Tree (Доминаторы)

Показывает объекты, которые удерживают больше всего памяти.

**В VisualVM / Eclipse MAT:**
- Открыть "Dominator Tree"
- Искать объекты с большим "Retained Heap"

**Для MemoryLeakExample:**
```
java.util.ArrayList (ACTIVE_SESSIONS)
  ↳ Retained Heap: ~1000 MB
  ↳ Contains: 1000+ UserSession objects
  ↳ Each UserSession: ~1MB (byte[] + ArrayList)
```

#### 2. Histogram

Показывает количество экземпляров каждого класса.

**Команда:**
```bash
jmap -histo:live <pid> | head -30
```

**Пример вывода с утечкой:**
```
 num     #instances         #bytes  class name
----------------------------------------------
   1:         10523      1052300000  [B  (byte arrays)
   2:         10523         252552  ru.sin.gc.MemoryLeakExample$UserSession
   3:         10523         252552  java.util.ArrayList
```

**Пример вывода БЕЗ утечки:**
```
 num     #instances         #bytes  class name
----------------------------------------------
   1:           500       50000000  [B
   2:           500          12000  ru.sin.gc.MemoryLeakFixedExample$UserSession
   3:           500          12000  java.util.ArrayList
```

**Ключевая разница:** Количество объектов стабилизируется vs постоянно растет.

#### 3. Leak Suspects Report (Eclipse MAT)

Eclipse MAT автоматически находит подозрительные объекты.

**Открыть в MAT:**
1. File → Open Heap Dump
2. Выбрать heap_with_leak.hprof
3. MAT автоматически покажет "Leak Suspects Report"

**Что увидите:**
```
Problem Suspect 1:
One instance of "java.util.ArrayList" loaded by "<system class loader>"
occupies 1,024,000,000 (97.3%) bytes.

The instance is referenced by:
  ru.sin.gc.MemoryLeakExample.ACTIVE_SESSIONS (static field)
```

#### 4. GC Roots (Корни GC)

Показывает, почему объект не собирается GC.

**Путь к GC Root для утекающего объекта:**
```
UserSession@0x12345678
  ↳ held by ArrayList$ElementData[527]
    ↳ held by ArrayList.elementData
      ↳ held by MemoryLeakExample.ACTIVE_SESSIONS (static field)
        ↳ GC Root: Java Static
```

**Интерпретация:** Объект достижим через статическое поле → не может быть собран GC.

---

## Сравнение До и После

### Метрики для сравнения

| Метрика | С утечкой | Без утечки |
|---------|-----------|------------|
| Retained Heap | Постоянно растет | Стабильный |
| Количество объектов | Растет линейно | Ограниченное |
| Full GC Frequency | Очень часто | Редко |
| GC Pause Time | Увеличивается | Стабильное |
| Dominator Object | Огромный ArrayList | Контролируемый размер |

### Визуальное сравнение в VisualVM

**С утечкой:**
```
Heap Usage Graph:
┌─────────────────────────────────────────┐
│                                    ▗▄▄▄▄│ ← OOM
│                              ▗▄▄▄▄▀▀▀▀▀▀│
│                        ▗▄▄▄▄▀▀▀▀▀       │
│                  ▗▄▄▄▄▀▀▀▀▀             │
│            ▗▄▄▄▄▀▀▀▀▀                   │
│      ▗▄▄▄▄▀▀▀▀▀                         │
│▄▄▄▄▀▀▀▀▀                                │
└─────────────────────────────────────────┘
Time →
```

**Без утечки:**
```
Heap Usage Graph:
┌─────────────────────────────────────────┐
│      ▄▄▄     ▄▄▄     ▄▄▄     ▄▄▄       │
│    ▄▀   ▀▄ ▄▀   ▀▄ ▄▀   ▀▄ ▄▀   ▀▄     │ ← Стабильно
│  ▄▀       ▀       ▀       ▀       ▀▄   │
│ ▀                                   ▀   │
│                                         │
│                                         │
│                                         │
└─────────────────────────────────────────┘
Time →
```

### Анализ Retained Heap

**В Eclipse MAT:**

1. Открыть оба dump'а
2. Compare → Compare with another heap dump
3. Сравнить top objects

**Ключевые отличия:**

**MemoryLeakExample:**
```
ArrayList @ 0x... : ACTIVE_SESSIONS
  Shallow Heap: 24 bytes
  Retained Heap: 1,050,000,000 bytes (1000 MB)
  Object Count: 10,523
```

**MemoryLeakFixedExample:**
```
ArrayList @ 0x... : ACTIVE_SESSIONS
  Shallow Heap: 24 bytes
  Retained Heap: 50,000,000 bytes (50 MB)
  Object Count: 500 (MAX_SESSIONS limit)
```

---

## GC Мониторинг

### jstat - Real-time GC Statistics

```bash
# Мониторинг GC каждую секунду
jstat -gc <pid> 1000

# Более читаемый формат (в процентах)
jstat -gcutil <pid> 1000

# С временными метками
jstat -gcutil -t <pid> 1000
```

**Вывод jstat -gcutil:**
```
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT
  0.00  95.23  45.12  78.54  94.23  89.12   142    1.234    23   3.456   4.690
```

**Расшифровка:**
- **S0, S1**: Survivor spaces (0-100%)
- **E**: Eden space (0-100%)
- **O**: Old generation (0-100%) ← **Смотрите сюда!**
- **M**: Metaspace (0-100%)
- **YGC**: Young GC count
- **YGCT**: Young GC time
- **FGC**: Full GC count ← **И сюда!**
- **FGCT**: Full GC time
- **GCT**: Total GC time

**Признаки утечки:**
```
При утечке:
O:  50% → 65% → 78% → 89% → 95% → 98% → OOM
FGC: 5  →  10  →  23  →  45  →  89  → 234

Без утечки:
O:  40% → 55% → 42% → 48% → 45% → 50%
FGC: 5  →   6  →   7  →   8  →   9  →  10
```

### GC Logs

**Включить GC логи:**
```bash
java -Xlog:gc*:file=gc.log:time,level,tags \
     -XX:+UseG1GC \
     YourApplication
```

**Для Java 8:**
```bash
java -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:gc.log \
     YourApplication
```

**Анализировать GC лог:**
```bash
# Визуальный анализ
# Используйте GCViewer или GCEasy (https://gceasy.io/)

# Или вручную ищите паттерны:
grep "Full GC" gc.log | wc -l  # Количество Full GC
grep "Full GC" gc.log | tail -20  # Последние Full GC
```

**Пример записи Full GC с утечкой:**
```
[2025-12-09T10:23:45.678+0000][info][gc] GC(234) Pause Full (Allocation Failure)
[2025-12-09T10:23:47.123+0000][info][gc] GC(234) Old: 1920M->1918M(2048M)
                                                           ^^^^^^^^ ← Почти не освободилось!
```

**Без утечки:**
```
[2025-12-09T10:23:45.678+0000][info][gc] GC(10) Pause Young
[2025-12-09T10:23:45.723+0000][info][gc] GC(10) Old: 512M->123M(2048M)
                                                           ^^^^^^^^ ← Много освободилось
```

---

## Инструменты анализа

### 1. VisualVM (Рекомендуется для начинающих)

**Установка:**
```bash
# macOS
brew install --cask visualvm

# Linux
# Скачать с https://visualvm.github.io/

# Или использовать встроенный в JDK
jvisualvm
```

**Использование:**
1. Запустить VisualVM
2. Найти процесс в списке "Local"
3. Double-click для открытия

**Вкладки:**
- **Monitor**: Real-time heap, threads, CPU
- **Sampler**: CPU и Memory profiling
- **Threads**: Thread dumps, deadlock detection
- **Heap Dump**: Анализ памяти

**Анализ Heap Dump в VisualVM:**
1. File → Load → Выбрать heap_with_leak.hprof
2. Summary: Общая информация
3. Objects: Histogram объектов
4. OQL Console: Запросы к heap dump

**Полезные OQL запросы:**
```javascript
// Найти все UserSession объекты
select s from ru.sin.gc.MemoryLeakExample$UserSession s

// Найти объекты больше 1MB
select s from java.lang.Object s where sizeof(s) > 1048576

// Найти все ArrayList с размером > 1000
select s from java.util.ArrayList s where s.size > 1000
```

### 2. Eclipse MAT (Memory Analyzer Tool)

**Установка:**
```bash
# Скачать с https://www.eclipse.org/mat/downloads.php
# Или установить как Eclipse plugin
```

**Преимущества:**
- Автоматический Leak Suspects Report
- Powerful OQL engine
- Dominator Tree analysis
- Path to GC Roots

**Основные фичи:**

**A. Leak Suspects Report**
- Автоматически находит подозрительные объекты
- Показывает retained heap
- Предлагает вероятные причины утечки

**B. Dominator Tree**
- Показывает объекты и их "dominees"
- Объект является dominator, если все пути к его dominee проходят через него
- Полезно для понимания структуры памяти

**C. Histogram**
- Группировка по классам
- Количество экземпляров
- Shallow vs Retained heap

**D. Path to GC Roots**
- Правый клик на объект → Path to GC Roots
- Показывает цепочку ссылок до корня
- Помогает понять, почему объект жив

### 3. JConsole (Встроен в JDK)

**Запуск:**
```bash
jconsole <pid>
```

**Возможности:**
- Real-time monitoring
- Manual GC trigger
- Heap dump через MBean
- Thread monitoring

### 4. Java Mission Control (JMC)

**Скачать:**
- https://www.oracle.com/java/technologies/javase/products-jmc8-downloads.html

**Использование:**
- Flight Recorder для low-overhead profiling
- Детальный анализ GC, allocations, threads
- Event browser

---

## Типичные паттерны утечек

### 1. Static Collection Leak ✓ (Наш пример)

**Паттерн:**
```java
class Cache {
    private static final Map<Key, Value> cache = new HashMap<>();

    public static void put(Key k, Value v) {
        cache.put(k, v);  // Никогда не удаляется!
    }
}
```

**Решение:**
- Использовать `WeakHashMap`
- Добавить eviction policy (LRU cache)
- Ограничить размер
- Использовать готовые библиотеки (Guava Cache, Caffeine)

### 2. Listener Leak ✓ (Наш пример)

**Паттерн:**
```java
button.addActionListener(listener);
// Забыли: button.removeActionListener(listener);
```

**Решение:**
- Всегда вызывать `removeListener()`
- Использовать `WeakReference` для listeners
- Паттерн `AutoCloseable`

### 3. ThreadLocal Leak

**Паттерн:**
```java
private static ThreadLocal<HeavyObject> threadLocal = new ThreadLocal<>();

public void process() {
    threadLocal.set(new HeavyObject());
    // Забыли: threadLocal.remove();
}
```

**Признаки:**
- В долгоживущих thread pools
- Каждый thread держит свою копию

**Решение:**
```java
try {
    threadLocal.set(new HeavyObject());
    // use it
} finally {
    threadLocal.remove();  // Обязательно!
}
```

### 4. Unclosed Resources

**Паттерн:**
```java
InputStream is = new FileInputStream("file.txt");
// Забыли: is.close();
```

**Решение:**
```java
try (InputStream is = new FileInputStream("file.txt")) {
    // use it
}  // Автоматически закроется
```

### 5. Mutable Static Fields

**Паттерн:**
```java
class Service {
    private static List<Handler> handlers = new ArrayList<>();

    public void registerHandler(Handler h) {
        handlers.add(h);
    }
}
```

**Решение:**
- Сделать поле non-static
- Добавить cleanup метод
- Использовать dependency injection

---

## Практические рекомендации

### Workflow для обнаружения утечек

1. **Запустить приложение с GC логами**
   ```bash
   -Xlog:gc*:file=gc.log -XX:+HeapDumpOnOutOfMemoryError
   ```

2. **Мониторить через jstat**
   ```bash
   jstat -gcutil <pid> 1000
   ```

3. **Наблюдать за Old Generation**
   - Если постоянно растет → вероятна утечка
   - Если Full GC не помогает → точно утечка

4. **Снять heap dump**
   ```bash
   jcmd <pid> GC.heap_dump heap.hprof
   ```

5. **Анализировать в MAT**
   - Открыть Leak Suspects Report
   - Проверить Dominator Tree
   - Найти Path to GC Roots

6. **Исправить код**
   - Добавить cleanup
   - Ограничить коллекции
   - Использовать WeakReference где нужно

7. **Верифицировать исправление**
   - Запустить снова
   - Снять heap dump
   - Сравнить с предыдущим

### Когда снимать heap dump

**Хорошие моменты:**
- После запуска, когда приложение стабилизировалось (baseline)
- Когда Old Gen достиг ~70-80%
- Перед и после Full GC (jmap -dump:live)
- При подозрении на утечку
- Автоматически при OOM

**Плохие моменты:**
- Во время Young GC (нестабильная картина)
- Слишком рано после старта (еще не разогрелось)
- На production без необходимости (создает паузу)

### Размер heap dump

Heap dump может быть очень большим:
- 1GB heap → ~1GB dump file
- 8GB heap → ~8GB dump file

**Рекомендации:**
- Снимать на диск с достаточным местом
- Использовать `-dump:live` для меньшего размера (только живые объекты)
- Сжать после снятия: `gzip heap.hprof`
- Для production: настроить `-XX:HeapDumpPath` на большой диск

---

## Дополнительные ресурсы

### Онлайн инструменты
- **GCEasy**: https://gceasy.io/ - анализ GC логов
- **Heap Hero**: https://heaphero.io/ - анализ heap dumps онлайн
- **FastThread**: https://fastthread.io/ - анализ thread dumps

### Документация
- [Java GC Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Eclipse MAT Documentation](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Fwelcome.html)
- [Understanding Memory Leaks](https://www.baeldung.com/java-memory-leaks)

### Книги
- "Java Performance" by Scott Oaks
- "Optimizing Java" by Ben Evans
- "Troubleshooting Java Performance" by Erik Ostermueller

---

## Быстрая справка команд

```bash
# Найти PID
jps -l | grep MemoryLeak

# Снять heap dump
jcmd <pid> GC.heap_dump heap.hprof

# Histogram в реальном времени
jmap -histo:live <pid> | head -30

# GC statistics
jstat -gcutil <pid> 1000

# Thread dump
jcmd <pid> Thread.print

# Force Full GC (только для тестирования!)
jcmd <pid> GC.run

# Посмотреть JVM флаги
jcmd <pid> VM.flags

# System properties
jcmd <pid> VM.system_properties
```

---

Теперь вы готовы находить и устранять утечки памяти в Java приложениях! 🎯
