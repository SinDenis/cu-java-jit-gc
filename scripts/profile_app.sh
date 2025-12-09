#!/bin/bash
# Скрипт для профилирования Java приложений с async-profiler

set -e

PROFILER_VERSION="3.0"
PROFILER_DIR="$HOME/.async-profiler"
PROFILER_PATH="$PROFILER_DIR/async-profiler-${PROFILER_VERSION}-macos/lib/libasyncProfiler.dylib"

# Для Linux используйте:
# PROFILER_PATH="$PROFILER_DIR/async-profiler-${PROFILER_VERSION}-linux-x64/lib/libasyncProfiler.so"

OUTPUT_DIR="./profiling_results"
mkdir -p "$OUTPUT_DIR"

echo "=== Async-Profiler Helper Script ==="
echo ""

# Проверка async-profiler
check_profiler() {
    if [ ! -f "$PROFILER_PATH" ]; then
        echo "❌ async-profiler не найден!"
        echo ""
        echo "Установка async-profiler:"
        echo "1. Скачайте с https://github.com/async-profiler/async-profiler/releases"
        echo "2. Распакуйте в $PROFILER_DIR"
        echo ""
        echo "Для macOS:"
        echo "  mkdir -p $PROFILER_DIR && cd $PROFILER_DIR"
        echo "  curl -L https://github.com/async-profiler/async-profiler/releases/download/v${PROFILER_VERSION}/async-profiler-${PROFILER_VERSION}-macos.zip -o async-profiler.zip"
        echo "  unzip async-profiler.zip"
        echo ""
        echo "Для Linux:"
        echo "  mkdir -p $PROFILER_DIR && cd $PROFILER_DIR"
        echo "  curl -L https://github.com/async-profiler/async-profiler/releases/download/v${PROFILER_VERSION}/async-profiler-${PROFILER_VERSION}-linux-x64.tar.gz -o async-profiler.tar.gz"
        echo "  tar -xzf async-profiler.tar.gz"
        exit 1
    fi
    echo "✓ async-profiler найден: $PROFILER_PATH"
}

# Показать help
show_help() {
    echo "Использование: $0 <pid> [duration] [event]"
    echo ""
    echo "Параметры:"
    echo "  pid       - PID Java процесса для профилирования"
    echo "  duration  - Длительность профилирования в секундах (по умолчанию: 30)"
    echo "  event     - Событие для профилирования (по умолчанию: cpu)"
    echo ""
    echo "События:"
    echo "  cpu       - CPU профилирование (по умолчанию)"
    echo "  alloc     - Memory allocation профилирование"
    echo "  lock      - Lock contention профилирование"
    echo "  wall      - Wall-clock профилирование"
    echo ""
    echo "Примеры:"
    echo "  $0 12345           # CPU профилирование на 30 сек"
    echo "  $0 12345 60        # CPU профилирование на 60 сек"
    echo "  $0 12345 30 alloc  # Memory allocation профилирование"
    echo ""
    echo "Результаты сохраняются в: $OUTPUT_DIR"
}

# Получить параметры
PID=$1
DURATION=${2:-30}
EVENT=${3:-cpu}

if [ -z "$PID" ] || [ "$PID" = "-h" ] || [ "$PID" = "--help" ]; then
    show_help
    exit 0
fi

check_profiler

# Проверка PID
if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "❌ Процесс с PID $PID не найден!"
    echo ""
    echo "Доступные Java процессы:"
    jps -l
    exit 1
fi

PROCESS_NAME=$(ps -p "$PID" -o command= | head -1)
echo "✓ Процесс найден: $PROCESS_NAME"
echo ""

# Генерируем имя файла
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="$OUTPUT_DIR/profile_${EVENT}_${PID}_${TIMESTAMP}"

echo "Параметры профилирования:"
echo "  PID:        $PID"
echo "  Событие:    $EVENT"
echo "  Длительность: $DURATION сек"
echo "  Результат:  ${OUTPUT_FILE}.html"
echo ""

# Запуск профилирования
echo "Начинаем профилирование..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Старт профилирования
jcmd "$PID" JVMTI.agent_load "$PROFILER_PATH" start,event=$EVENT

echo "⏱  Профилирование запущено. Ожидание $DURATION секунд..."
echo ""

# Прогресс бар
for i in $(seq 1 $DURATION); do
    printf "\rПрогресс: ["
    PROGRESS=$((i * 50 / DURATION))
    for j in $(seq 1 50); do
        if [ $j -le $PROGRESS ]; then
            printf "="
        else
            printf " "
        fi
    done
    printf "] %d/%d сек" $i $DURATION
    sleep 1
done
echo ""
echo ""

# Остановка профилирования и генерация flamegraph
echo "Остановка профилирования и генерация flame graph..."
jcmd "$PID" JVMTI.agent_load "$PROFILER_PATH" stop,file="${OUTPUT_FILE}.html",flamegraph

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ Профилирование завершено!"
echo ""
echo "Результаты:"
echo "  Flame graph: ${OUTPUT_FILE}.html"
echo ""
echo "Открыть flame graph:"
echo "  open ${OUTPUT_FILE}.html"
echo ""

# Также сохраняем в JFR формате для Java Flight Recorder
echo "Генерация JFR файла..."
jcmd "$PID" JVMTI.agent_load "$PROFILER_PATH" stop,file="${OUTPUT_FILE}.jfr",jfr

echo "  JFR файл:    ${OUTPUT_FILE}.jfr"
echo ""
echo "Открыть JFR в JDK Mission Control:"
echo "  jmc ${OUTPUT_FILE}.jfr"
echo ""

# Советы по анализу
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Советы по анализу flame graph:"
echo ""
echo "1. Ширина блока = процент CPU времени"
echo "2. Высота = глубина стека вызовов"
echo "3. Ищите широкие блоки на верхних уровнях (hot spots)"
echo "4. Цвет обычно случайный (для различения)"
echo ""
echo "Что искать:"
echo "  • Широкие блоки = много времени CPU"
echo "  • String concatenation в циклах"
echo "  • Неэффективные коллекции"
echo "  • Избыточные вычисления"
echo "  • Лишние аллокации (при event=alloc)"
echo ""
echo "Документация: docs/profiling/ASYNC_PROFILER_GUIDE.md"
