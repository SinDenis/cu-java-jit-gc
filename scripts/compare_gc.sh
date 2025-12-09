#!/bin/bash
# Скрипт для сравнения разных Garbage Collectors

set -e

BENCHMARK_TYPE=${1:-throughput}

echo "=== GC Comparison Script ==="
echo ""

if [ "$BENCHMARK_TYPE" = "help" ] || [ "$BENCHMARK_TYPE" = "--help" ]; then
    echo "Использование: $0 [benchmark_type]"
    echo ""
    echo "Доступные benchmark types:"
    echo "  throughput  - Тест максимальной производительности"
    echo "  latency     - Тест времени отклика (важна p99 latency)"
    echo "  mixed       - Реалистичная смешанная нагрузка"
    echo "  allocation  - Тест allocation rate и Young GC"
    echo "  all         - Все бенчмарки (долго!)"
    echo ""
    echo "Примеры:"
    echo "  $0 throughput   # Сравнить throughput всех GC"
    echo "  $0 latency      # Сравнить latency всех GC"
    echo "  $0 all          # Запустить все бенчмарки"
    exit 0
fi

# Проверка Java версии
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "Java Version: $JAVA_VERSION"

# ZGC доступен только с Java 15+
if [ "$JAVA_VERSION" -lt 15 ]; then
    echo "⚠️  ZGC требует Java 15+. Текущая версия: $JAVA_VERSION"
    echo "    ZGC бенчмарки будут пропущены."
    SKIP_ZGC=true
else
    SKIP_ZGC=false
    echo "✓ ZGC поддерживается"
fi

echo ""

# Создать директорию для результатов
mkdir -p gc_benchmarks
RESULTS_FILE="gc_benchmarks/comparison_${BENCHMARK_TYPE}_$(date +%Y%m%d_%H%M%S).txt"

echo "Результаты будут сохранены в: $RESULTS_FILE"
echo ""

# Функция для запуска бенчмарка с GC
run_benchmark() {
    local gc_name=$1
    local task_name=$2

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Запуск: $gc_name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Запуск с перенаправлением вывода
    if ./gradlew $task_name 2>&1 | tee -a "$RESULTS_FILE"; then
        echo ""
        echo "✓ $gc_name завершен"
    else
        echo ""
        echo "✗ $gc_name завершился с ошибкой"
    fi

    echo ""
    echo "Нажмите Enter для продолжения или Ctrl+C для отмены..."
    read -r
}

# Функция для запуска всех GC для бенчмарка
run_all_gcs_for_benchmark() {
    local bench_type=$1

    case $bench_type in
        throughput)
            echo "=== Throughput Benchmark Comparison ===" | tee "$RESULTS_FILE"
            run_benchmark "Serial GC" "runThroughputSerial"
            run_benchmark "Parallel GC" "runThroughputParallel"
            run_benchmark "G1 GC" "runThroughputG1"
            if [ "$SKIP_ZGC" = false ]; then
                run_benchmark "ZGC" "runThroughputZGC"
            fi
            ;;
        latency)
            echo "=== Latency Benchmark Comparison ===" | tee "$RESULTS_FILE"
            run_benchmark "Serial GC" "runLatencySerial"
            run_benchmark "Parallel GC" "runLatencyParallel"
            run_benchmark "G1 GC" "runLatencyG1"
            if [ "$SKIP_ZGC" = false ]; then
                run_benchmark "ZGC" "runLatencyZGC"
            fi
            ;;
        mixed)
            echo "=== Mixed Workload Benchmark Comparison ===" | tee "$RESULTS_FILE"
            run_benchmark "Serial GC" "runMixedSerial"
            run_benchmark "Parallel GC" "runMixedParallel"
            run_benchmark "G1 GC" "runMixedG1"
            if [ "$SKIP_ZGC" = false ]; then
                run_benchmark "ZGC" "runMixedZGC"
            fi
            ;;
        allocation)
            echo "=== Allocation Benchmark Comparison ===" | tee "$RESULTS_FILE"
            run_benchmark "Serial GC" "runAllocationSerial"
            run_benchmark "Parallel GC" "runAllocationParallel"
            run_benchmark "G1 GC" "runAllocationG1"
            if [ "$SKIP_ZGC" = false ]; then
                run_benchmark "ZGC" "runAllocationZGC"
            fi
            ;;
        all)
            echo "=== Running ALL Benchmarks ===" | tee "$RESULTS_FILE"
            run_all_gcs_for_benchmark throughput
            run_all_gcs_for_benchmark latency
            run_all_gcs_for_benchmark mixed
            run_all_gcs_for_benchmark allocation
            ;;
        *)
            echo "Неизвестный тип бенчмарка: $BENCHMARK_TYPE"
            echo "Используйте: throughput, latency, mixed, allocation, или all"
            exit 1
            ;;
    esac
}

# Основной запуск
echo "Начинаем сравнение GC для: $BENCHMARK_TYPE"
echo ""
echo "ВАЖНО: Каждый бенчмарк займет время. Рекомендуется:"
echo "  - Закрыть другие приложения"
echo "  - Дождаться завершения каждого теста"
echo "  - Не прерывать выполнение"
echo ""
echo "Нажмите Enter для начала или Ctrl+C для отмены..."
read -r

# Компиляция
echo "Компиляция проекта..."
./gradlew build

run_all_gcs_for_benchmark "$BENCHMARK_TYPE"

# Итоги
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Сравнение завершено!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Результаты сохранены в: $RESULTS_FILE"
echo ""
echo "Для анализа результатов:"
echo "  cat $RESULTS_FILE"
echo ""
echo "Для детального анализа GC логов:"
echo "  Загрузите файлы из gc_benchmarks/ на https://gceasy.io/"
echo ""
echo "Рекомендации по выбору GC см. в docs/gc/GC_COMPARISON_GUIDE.md"
