#!/bin/bash
# Скрипт для снятия heap dump из запущенного Java процесса

set -e

echo "=== Heap Dump Helper ==="
echo ""

# Найти все Java процессы
echo "Доступные Java процессы:"
jps -l

echo ""
read -p "Введите PID процесса: " PID

if [ -z "$PID" ]; then
    echo "Ошибка: PID не указан"
    exit 1
fi

# Проверить, что процесс существует
if ! ps -p $PID > /dev/null 2>&1; then
    echo "Ошибка: Процесс с PID $PID не найден"
    exit 1
fi

# Создать директорию для heap dumps
mkdir -p heap_dumps

# Спросить, какой тип dump делать
echo ""
echo "Выберите тип heap dump:"
echo "1) Все объекты (включая мусор)"
echo "2) Только живые объекты (запускает Full GC перед dump)"
read -p "Ваш выбор (1/2): " CHOICE

TIMESTAMP=$(date +%Y%m%d_%H%M%S)

if [ "$CHOICE" = "2" ]; then
    FILENAME="heap_dumps/heap_live_${TIMESTAMP}.hprof"
    echo ""
    echo "Снятие heap dump с только живыми объектами..."
    echo "ВНИМАНИЕ: Будет запущен Full GC, это может занять время!"

    jcmd $PID GC.heap_dump -all=false $FILENAME
else
    FILENAME="heap_dumps/heap_all_${TIMESTAMP}.hprof"
    echo ""
    echo "Снятие heap dump всех объектов..."

    jcmd $PID GC.heap_dump $FILENAME
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Heap dump успешно сохранен: $FILENAME"

    # Получить размер файла
    SIZE=$(du -h $FILENAME | cut -f1)
    echo "  Размер: $SIZE"

    echo ""
    echo "Следующие шаги:"
    echo "1. Открыть в VisualVM:"
    echo "   jvisualvm --openfile $FILENAME"
    echo ""
    echo "2. Открыть в Eclipse MAT:"
    echo "   mat $FILENAME"
    echo ""
    echo "3. Базовый анализ через jhat (устаревший):"
    echo "   jhat -J-Xmx4g $FILENAME"
    echo "   Затем откройте: http://localhost:7000/"
    echo ""
    echo "4. Быстрый histogram:"
    echo "   jmap -histo $FILENAME | head -30"
else
    echo ""
    echo "✗ Ошибка при создании heap dump"
    exit 1
fi
