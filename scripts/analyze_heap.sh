#!/bin/bash
# Скрипт для быстрого анализа heap dump

set -e

if [ $# -eq 0 ]; then
    echo "=== Heap Dump Analyzer ==="
    echo ""
    echo "Использование: $0 <heap_dump.hprof>"
    echo ""
    echo "Доступные heap dumps:"
    if [ -d "heap_dumps" ]; then
        ls -lh heap_dumps/*.hprof 2>/dev/null || echo "  (нет файлов)"
    else
        echo "  Директория heap_dumps не найдена"
    fi
    exit 1
fi

HEAP_FILE=$1

if [ ! -f "$HEAP_FILE" ]; then
    echo "Ошибка: Файл $HEAP_FILE не найден"
    exit 1
fi

echo "=== Анализ Heap Dump ==="
echo "Файл: $HEAP_FILE"
echo "Размер: $(du -h $HEAP_FILE | cut -f1)"
echo ""

echo "Выберите действие:"
echo "1) Показать histogram (top 30 классов по памяти)"
echo "2) Открыть в VisualVM"
echo "3) Запустить jhat web interface (устаревший)"
echo "4) Показать информацию о файле"
echo "5) Сравнить два heap dump"
read -p "Ваш выбор (1-5): " CHOICE

case $CHOICE in
    1)
        echo ""
        echo "=== Top 30 классов по использованию памяти ==="
        echo ""
        jmap -histo $HEAP_FILE | head -35
        echo ""
        echo "Колонки:"
        echo "  #instances - количество экземпляров"
        echo "  #bytes     - занимаемая память в байтах"
        echo "  class name - имя класса"
        echo ""
        echo "Обратите внимание на:"
        echo "  - Большое количество одинаковых объектов"
        echo "  - Классы вашего приложения (ru.sin.*)"
        echo "  - byte[] массивы ([B)"
        ;;
    2)
        echo ""
        echo "Запуск VisualVM..."
        if command -v jvisualvm &> /dev/null; then
            jvisualvm --openfile "$HEAP_FILE" &
            echo "VisualVM запущен"
        else
            echo "VisualVM не найден. Установите через:"
            echo "  macOS: brew install --cask visualvm"
            echo "  или скачайте с https://visualvm.github.io/"
        fi
        ;;
    3)
        echo ""
        echo "Запуск jhat (требуется много памяти)..."
        echo "После запуска откройте: http://localhost:7000/"
        echo ""
        echo "ВНИМАНИЕ: jhat устарел, рекомендуется использовать VisualVM или Eclipse MAT"
        echo ""
        read -p "Продолжить? (y/n): " CONFIRM
        if [ "$CONFIRM" = "y" ]; then
            jhat -J-Xmx4g "$HEAP_FILE"
        fi
        ;;
    4)
        echo ""
        echo "=== Информация о heap dump ==="
        echo ""
        file "$HEAP_FILE"
        echo ""
        echo "Размер файла: $(du -h $HEAP_FILE | cut -f1)"
        echo "Дата создания: $(stat -f %Sm $HEAP_FILE 2>/dev/null || stat -c %y $HEAP_FILE)"
        echo ""
        echo "Быстрая статистика классов:"
        jmap -histo $HEAP_FILE | head -5
        echo ""
        echo "Всего классов в dump:"
        jmap -histo $HEAP_FILE | wc -l
        ;;
    5)
        echo ""
        read -p "Введите путь ко второму heap dump для сравнения: " HEAP_FILE2

        if [ ! -f "$HEAP_FILE2" ]; then
            echo "Ошибка: Файл $HEAP_FILE2 не найден"
            exit 1
        fi

        echo ""
        echo "=== Сравнение Heap Dumps ==="
        echo ""
        echo "Heap 1: $HEAP_FILE"
        echo "Heap 2: $HEAP_FILE2"
        echo ""

        echo "Top классов в первом dump:"
        jmap -histo $HEAP_FILE | head -15

        echo ""
        echo "Top классов во втором dump:"
        jmap -histo $HEAP_FILE2 | head -15

        echo ""
        echo "Для детального сравнения используйте Eclipse MAT:"
        echo "  File -> Compare To Another Heap Dump"
        ;;
    *)
        echo "Неверный выбор"
        exit 1
        ;;
esac

echo ""
echo "Для детального анализа рекомендуется:"
echo "  - VisualVM: jvisualvm --openfile $HEAP_FILE"
echo "  - Eclipse MAT: https://www.eclipse.org/mat/"
