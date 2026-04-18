# InkQuest — Narrative Quest Engine

Fabric-мод для Minecraft 1.20.1. Система квестов, ориентированная на нарративный геймплей.

InkQuest занимает нишу, которую не закрывают FTB Quests или BetterQuesting: это инструмент для авторов карт, которым нужен контроль над историей, а не таблица прогресса. Квест здесь — сюжетное событие с развитием: он проходит этапы, может завершиться успехом, провалом или быть пропущен. Вся конфигурация ведётся через JSON-датапаки и команды — без GUI-редактора и без зависимостей от сторонних модов.

## Содержание

- [Требования](#требования)
- [Структура квеста](#структура-квеста)
- [Быстрый старт](#быстрый-старт)
- [Условия выполнения](#условия-выполнения)
- [Хуки жизненного цикла](#хуки-жизненного-цикла)
- [HUD и книга квестов](#hud-и-книга-квестов)
- [Справочник команд](#справочник-команд)
- [Для разработчиков](#для-разработчиков)

---

## Требования

- Minecraft **1.20.1**
- Fabric Loader **≥ 0.16.14**
- Fabric API
- Java **17+**

---

## Структура квеста

Квест состоит из **этапов**, каждый этап содержит **одну обязательную задачу** и любое число **необязательных**. Этапы проходятся последовательно: следующий открывается только после завершения обязательной задачи текущего.

**Исходы квеста:**
- `success` — все обязательные задачи выполнены успешно или пропущены (но не все пропущены)
- `failure` — хотя бы одна обязательная задача провалена; квест завершается немедленно
- `skipped` — все обязательные задачи пропущены

Два типа квестов:
- **Статические** — описаны в датапаке, загружаются вместе с миром
- **Динамические** — создаются командами во время игры, хранятся в сохранении мира

---

## Быстрый старт

### Создание квеста через датапак

Создайте файл `data/<namespace>/quests/<id>.json` в датапаке:

```json
{
    "version": 1,
    "variant": 1,
    "title": "Побег",
    "description": "Здесь больше нельзя оставаться, но как же выбраться?",
    "tasks": {
        "push_the_lever": {
            "title": "Найти и опустить рычаг, открывающий люк",
            "description": "Когда один из стражей выходил, мне удалось услышать щелчок в дальней части комнаты"
        },
        "look_for_survivors": {
            "title": "Узнать что стало с выжившими",
            "description": "Нужно обыскать камеры, может удастся найти ещё кого-нибудь"
        }
    },
    "stages": [
        ["push_the_lever", "look_for_survivors"]
    ]
}
```

Первый элемент каждого этапа — обязательная задача, остальные — необязательные.

Поля `version` и `variant` обязательны. Текущая версия формата — `1.1` (`version: 1`, `variant: 1`).

### Создание квеста командами

```
/quest new example:escape "Побег" "Здесь больше нельзя оставаться, но как же выбраться?"
/quest modify example:escape tasks add required push_the_lever "Найти рычаг" "..."
/quest modify example:escape tasks add optional look_for_survivors "Найти выживших" "..."
```

### Выдача и управление

```
/quest give PlayerName example:escape
/quest give PlayerName example:escape pin    # выдать и сразу закрепить в HUD
/quest pin PlayerName example:escape         # закрепить в HUD
/quest complete PlayerName example:escape task push_the_lever success
/quest drop PlayerName example:escape
```

---

## Условия выполнения

Задачи могут завершаться **автоматически** (только для задач из датапаков):

> Условия проверяются каждый тик только у задач **закреплённых** (`pinned`) и **фоновых** (`background: true`) квестов.

### `score` — достижение значения scoreboard

```json
{
    "type": "score",
    "objective": "oak_logs_mined",
    "criterion": "minecraft.mined:minecraft.oak_log",
    "target": 5
}
```

Отображает прогресс-бар в HUD. Поддерживает восходящие и нисходящие условия (если `initial > target` — нужно опуститься ниже `target`).

### `predicate` — предикат Minecraft

```json
{
    "type": "predicate",
    "predicate": "example:has_key"
}
```

Бинарное условие, прогресс-бара нет.

### `all` — составное условие

```json
{
    "type": "all",
    "conditions": [
        { "type": "score", "objective": "gold_collected", "target": 100 },
        { "type": "predicate", "predicate": "example:has_key" }
    ]
}
```

Прогресс = число выполненных подусловий. Условия вкладываются без ограничений.

Для каждой задачи можно задать условие успеха и условие провала:

```json lines
{
    "tasks": {
        "my_task": {
            "success": { "condition": ... },
            "failure": { "condition": ... }
        }
    }
}
```

---

## Хуки жизненного цикла

К каждой задаче можно привязать функции датапака:

```json
{
    "tasks": {
        "my_task": {
            "lifetime": {
                "load":   "example:on_load",
                "tick":   "example:on_tick",
                "unload": "example:on_unload"
            },
            "success": {
                "condition": { "type": "predicate", "predicate": "example:condition" },
                "reward": {
                    "function": "example:on_success"
                }
            },
            "failure": {
                "reward": {
                    "function": "example:on_failure"
                }
            }
        }
    }
}
```

- `lifetime.load` — один раз при переходе на этап с задачей
- `lifetime.tick` — каждый тик, пока задача активна
- `lifetime.unload` — один раз, когда задача перестаёт быть активной
- `success.reward.function` / `failure.reward.function` — функция датапака при завершении задачи

---

## HUD и книга квестов

**HUD** — закреплённый квест отображается в углу экрана. Для score-условий показывается прогресс-бар, переходы анимированы.

**Книга квестов** — открывается клавишей `J`. Список активных и завершённых квестов с деталями, загружаемыми с сервера. Задачу можно закрепить прямо из книги.

**Закрепление:** один квест — одна задача в HUD. При выдаче управляется флагом `pin_mode`:
- `auto` — закрепить, если родительский квест был закреплён (по умолчанию)
- `force` — закрепить всегда
- `off` — не закреплять автоматически

**Фоновый квест** (`"background": true`) — условия проверяются даже когда квест не закреплён. Нужен для мультиплеерных счётчиков и фоновой логики.

---

## Справочник команд

Все команды требуют уровень оператора **2+**.

### Управление квестами игрока

| Команда | Описание |
|---------|----------|
| `/quest give <player> <id> [pin]` | Выдать квест игроку |
| `/quest drop <player> <id>` | Забрать квест (прогресс теряется) |
| `/quest pin <player> <id> [taskId]` | Закрепить квест в HUD |
| `/quest unpin <player> <id>` | Открепить квест из HUD |
| `/quest complete <player> <id> task <taskId> success\|failure\|skip` | Выполнить задачу |
| `/quest complete <player> <id> stage success\|failure\|skip full\|required` | Выполнить задачи активного этапа |
| `/quest complete <player> <id> success\|failure\|skip full\|required` | Выполнить весь квест |

### Создание и редактирование (только динамические квесты)

| Команда | Описание |
|---------|----------|
| `/quest new <id> [title] [desc]` | Создать квест |
| `/quest remove <id>` | Удалить квест |
| `/quest modify <id> title <text>` | Изменить название |
| `/quest modify <id> description <text>` | Изменить описание |
| `/quest modify <id> tasks add required <taskId> ...` | Добавить обязательную задачу |
| `/quest modify <id> tasks add optional <taskId> ...` | Добавить необязательную задачу |
| `/quest modify <id> tasks remove <taskId>` | Удалить задачу |
| `/quest modify <id> icon <item>` | Установить иконку |
| `/quest modify <id> index <n>` | Установить порядок сортировки |
| `/quest modify <id> background true\|false` | Фоновая проверка условий |
| `/quest modify <id> pin_mode auto\|off\|force` | Режим автозакрепления |
| `/quest purge <id>` | Снять квест со всех игроков (включая оффлайн) |

### Просмотр и проверка

| Команда | Описание |
|---------|----------|
| `/quest list [all\|static\|dynamic]` | Список всех квестов |
| `/quest list trackedby <player> [статус]` | Квесты игрока с фильтром по статусу |
| `/quest list tasks <id> [all\|required\|optional\|unused]` | Задачи квеста |
| `/quest test <player> <id> <статус>` | Проверка статуса (возвращает 0 или 1) |

**Статусы:** `active`, `complete`, `succeeded`, `failed`, `skipped`, `pinned`

### Интеграция с `/execute`

```
/execute if quest @s example:escape succeeded run say Квест пройден!
/execute unless quest @s example:escape active run ...
/execute if task @s example:escape push_the_lever succeeded run setblock ~ ~ ~ air
```

---

## Для разработчиков

### Сборка

```bash
./gradlew build
```

### Структура

```
src/main/java/k4k/travelcorequesting/
├── domain/          # Доменные модели (Quest, Task, условия)
├── questing/        # Бизнес-логика (менеджер, трекеры, события)
├── infra/           # Инфраструктура (команды, сеть, сериализация)
├── client/          # Клиентская часть (HUD, экраны, анимации)
└── common/          # Общие утилиты
```

### События

`ServerQuestManager` публикует события на каждом шаге жизненного цикла квеста — другие моды могут подписываться через `QuestEvents` и `QuestProgressEvents`.

### Документация

Полная документация находится в `docs/ДОКУМЕНТАЦИЯ/`.