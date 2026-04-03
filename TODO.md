# TODO:
- Книга с квестами
  - Отправить на клиент информацию о степени выполнения задачи
  - Отобразить на клиенте карточку квеста

# Квесты

- Книга с активными и пройденными квестами

## Описание квеста

```
class QuestTracker {
    private Quest quest  // квест, который отслеживается
    private List<TaskTracker> trackers - Контроль выполнения отдельных задач
    private int currentStage = 0;
    
    public Quest getQuest();
    public int getCurrentStage();
    public void tick();
    
    public void completeTask(PlayerEntity player, CommandExecutor executor, boolean giveReward);
    public void failTask(PlayerEntity player, CommandExecutor executor);
    
    public NbtCompound toNbt();
    public static QuestTracker fromNbt(NbtCompound nbt);
    
    private static TaskTracker {
        public QuestTask task;
        public int currentProgress = -1;
    }
}
```

Комманды:
```
Progress control:

Выдать квест игроку
/quest ( world | player <player_selector> ) give <quest_id>
    Данный квест у игрока уже есть - ОШИБКА

Отобрать квест у игрока
/quest ( world | player <player_selector> ) drop <quest_id>
    Данного квеста у игрока нет - ОШИБКА

Полностью завершить квест
/quest ( world | player <player_selector> ) complete <quest_id>[ required|full[ reward|norewards]]
    Данного квеста у игрока нет - ОШИБКА
    required - завершить только обязательные задачи (по-умолчанию)
    full - завершить все задачи
    reward - выдать награды за все завершённые задачи и за выполнение квеста (по-умолчанию)
    norewards - не выдавать наград

Полностью завершить текущий этап квеста
/quest ( world | player <player_selector> ) complete <quest_id> stage[ required|full[ reward|norewards]]
    Данного квеста у игрока нет - ОШИБКА
    required - завершить только обязательную задачу (по-умолчанию)
    full - завершить все задачи
    reward - выдать награды за все завершённые задачи (по-умолчанию)
    norewards - не выдавать наград

Завершить определённую задачу
/quest ( world | player <player_selector> ) complete <quest_id> task <task_id>[ reward|noreward]
    Данного квеста у игрока нет - ОШИБКА
    Данной задачи нет в квесте - ОШИБКА
    reward - выдать награду за задачу (по-умолчанию)
    noreward - не выдавать наград

Завершить квест провалом
/quest ( world | player <player_selector> ) fail <quest_id>
    Данного квеста у игрока нет - ОШИБКА

Завершить задачу провалом (опциональная задача пропадёт из списка, а обязательная приведёт к провалу всего квеста)
/quest ( world | player <player_selector> ) fail <quest_id> task <task_id>
    Данного квеста у игрока нет - ОШИБКА
    Данной задачи нет в квесте - ОШИБКА


Pin control:

Закрепление квеста
/quest ( world | player <player_selector> ) pin set <quest_id>
    Данного квеста у игрока нет - ОШИБКА

Закрепление задачи
/quest ( world | player <player_selector> ) pin set <quest_id> task <task_id>
    Данного квеста у игрока нет - ОШИБКА
    Данной задачи нет в квесте - ОШИБКА

Открепление квеста
/quest ( world | player <player_selector> ) pin reset


Information:

Получить список всех квестов игрока
/quest ( world | player <player_selector> ) list

Получить список всех задач или этапов квеста
/quest ( world | player <player_selector> ) list <quest_id> tasks|stages

Получить статус квеста
/quest ( world | player <player_selector> ) query <quest_id> status
    0 - Не выдан
    1 - Активен
    2 - Завершён

Получить информацию о том закреплён ли квест
/quest ( world | player <player_selector> ) query <quest_id> pin
    0 - Не закреплён
    1 - Закреплён

Получить индекс текущего этапа квеста
/quest ( world | player <player_selector> ) query <quest_id> stage current

Получить завершённость этапа квеста
/quest ( world | player <player_selector> ) query <quest_id> stage <index> completeness
    0 - Этап не пройден (не выполнена обязательная задача)
    1 - Этап пройден (обязательная задача выполнена)

Получить прогресс по задаче в процентах
/quest ( world | player <player_selector> ) query <quest_id> task <task_id> progress
    0 - Задача не выполнена
    1-99 - Уровень выполнения для задач поддерживающих прогресс
    100 - Задача выполнена полностью

Получить информацию о том выполнена ли задача
/quest ( world | player <player_selector> ) query <quest_id> task <task_id> completeness
    0 - Задача не выполнена
    1 - Задача выполнена

Получить информацию о том закреплена ли задача
/quest ( world | player <player_selector> ) query <quest_id> task <task_id> pin
    0 - Не закреплена
    1 - Закреплена

Debug:

Удалить прогресс по квестам, которых нет в загруженных в данный момент датапаках
/quest debug dropUnused

```


data/\<namespace>/quests/\<quest>.json
```json lines
{
  "version": 1,
  "variant": 0,
  
  "display": {
    "title": "<TEXT>",
    "description": "<TEXT>?",
    "group": "<TEXT>?",  // (default: identifier of the "Other" group)

    // Notify player if this quest is changed:
    //   task completed while quest is not pinned,
    //   this quest is given but not pinned,
    //   etc.
    "notify_changed": bool  // (default: true)
  },
  
  // World quests share progress between all players on the server. (default: false)
  "world": bool,
  
  // Ignored if "world" option is false. (default: all)
  // Determines who will get the reward when this quest is completed
  "reward_mode": "completed|all",
  
  // Allows this quest to progress while it is not pinned (default: true)
  //   Player will receive notifications of changes if "notify_changed" is set to true
  // Always true for server quests
  "background": bool,
  
  "tasks": {
    "task_id": {
      "title": "<TEXT>",
      "description": "<TEXT>?",
      
      // For world quests these functions will be called 
      "load": "<FUNCTION ID>?",  // Function to be called one time when stage with this task begins
      "tick": "<FUNCTION ID>?",  // Function to be called each tick from point where task is loaded until it is complete
      "fail": "<FUNCTION ID>?",  // Function to be called one time when task is failed
      "done": "<FUNCTION ID>?",  // Function to be called one time when task is done (succeeded or failed)
      
      "location": "<LOCATION ID>?",  // TOBE. Location to which compass will point when this task is pinned
      
      "condition": {
        "type": "predicate|score|kill|obtain|break",
        "predicate": "<PREDICATE ID>",  // Only for "predicate" type. Predicate to be tested
        "objective": "<OBJECTIVE>",  // Only for "score" type. Objective to be watched
        "entity": "<ENTITY ID>",  // Only for "kill-entities" type. Identifier or tag of entities to kill
        "item": "<ITEM ID>",  // Only for "collect-items" type. Identifier or tag of items to collect
        "block": "<BLOCK ID>",  // Only for "break-blocks" type. Identifier or tag of blocks to break
        "target": 0,  // Only for "score", "kill-entities", "collect-items" and "break-blocks" types. Target amount
      },
      
      "reward": {
        "experience": 0,
        "item": {"id": "<ITEM ID>", "count": 0},
        "loot": "<LOOT TABLE ID>",
        "function": "<FUNCTION ID>",
        "facts": [
          {
            "type": "world|player",
            "id": "<FACT ID>"
          }
        ]
      }
    }
  },
  
  // First task in each stage is required, other ones are optional.
  // Stage progresses only if required task is complete
  // If stage contains no tasks - there will be an error
  "stages": [
    ["<TASK ID>"]
  ],
  
  // ALL facts in ANY of the sublist must be present for this quest to be UNLOCK-ABLE
  "facts": [[
    {
      "type": "world|player",
      "id": "<FACT ID>"
    }
  ]],
  
  // ALL quests in ANY of the sublist must be complete for this quest to be UNLOCKED
  "dependencies": [["<QUEST ID>"]],

  // Mode in which quest is pinned upon unlocking.
  //   "auto" - pin if no other quest is pinned
  //   "off" - do not pin this quest
  //   "force" - force pin this quest
  // If there is no force-pins - first unlocked auto-pin quest will be pinned
  //   otherwise - last force-pined
  "pin": "auto|off|force",
  
  "fail": "<FUNCTION ID>?",
  
  "lore": ["<LORE ID>"]
}
```

## Компас

По-умолчанию HUD-компас отображается всё вермя, не показывая ничего.

Если в инвентаре появляется предмет компаса - в HUD-компасе появляется точка возрождения игрока

Если в инвентаре появляются часы - HUD-компас начинает показывать время (иконка с восходящим и заходящим солнцем)

При ПКМ клике по баннеру, баннер начинает отображаться в компасе



