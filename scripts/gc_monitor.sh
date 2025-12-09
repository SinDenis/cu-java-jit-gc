#!/bin/bash
# Скрипт для мониторинга GC в реальном времени

set -e

echo "=== GC Monitor ==="
echo ""

# Найти все Java процессы
echo "Доступные Java процессы:"
jps -l

echo ""
read -p "Введите PID процесса для мониторинга: " PID

if [ -z "$PID" ]; then
    echo "Ошибка: PID не указан"
    exit 1
fi

# Проверить, что процесс существует
if ! ps -p $PID > /dev/null 2>&1; then
    echo "Ошибка: Процесс с PID $PID не найден"
    exit 1
fi

echo ""
echo "Выберите режим мониторинга:"
echo "1) gcutil - Статистика GC в процентах (рекомендуется)"
echo "2) gc - Детальная статистика размеров heap"
echo "3) gccapacity - Емкость различных областей памяти"
echo "4) gcnew - Статистика Young Generation"
echo "5) gcold - Статистика Old Generation"
read -p "Ваш выбор (1-5): " MODE

INTERVAL=1000  # По умолчанию 1 секунда

read -p "Интервал обновления в секундах [1]: " INPUT_INTERVAL
if [ ! -z "$INPUT_INTERVAL" ]; then
    INTERVAL=$((INPUT_INTERVAL * 1000))
fi

case $MODE in
    1)
        echo ""
        echo "Мониторинг GC (gcutil) - обновление каждые $(($INTERVAL / 1000)) сек"
        echo "Нажмите Ctrl+C для остановки"
        echo ""
        echo "Расшифровка колонок:"
        echo "  S0, S1 - Survivor spaces (% использования)"
        echo "  E      - Eden space (% использования)"
        echo "  O      - Old generation (% использования) ← ВАЖНО!"
        echo "  M      - Metaspace (% использования)"
        echo "  YGC    - Количество Young GC"
        echo "  YGCT   - Время Young GC (секунды)"
        echo "  FGC    - Количество Full GC ← ВАЖНО!"
        echo "  FGCT   - Время Full GC (секунды)"
        echo "  GCT    - Общее время GC"
        echo ""
        echo "ПРИЗНАКИ УТЕЧКИ:"
        echo "  - O (Old Gen) постоянно растет и не падает"
        echo "  - FGC (Full GC) происходит часто, но не помогает"
        echo "  - FGCT (время Full GC) увеличивается"
        echo ""
        sleep 2
        jstat -gcutil -t $PID $INTERVAL
        ;;
    2)
        echo ""
        echo "Мониторинг GC (gc) - детальные размеры"
        echo ""
        jstat -gc -t $PID $INTERVAL
        ;;
    3)
        echo ""
        echo "Мониторинг емкости памяти"
        echo ""
        jstat -gccapacity $PID $INTERVAL
        ;;
    4)
        echo ""
        echo "Мониторинг Young Generation"
        echo ""
        jstat -gcnew $PID $INTERVAL
        ;;
    5)
        echo ""
        echo "Мониторинг Old Generation"
        echo ""
        jstat -gcold $PID $INTERVAL
        ;;
    *)
        echo "Неверный выбор"
        exit 1
        ;;
esac
