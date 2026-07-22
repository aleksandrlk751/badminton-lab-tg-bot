# Badminton LAB Telegram Bot

Telegram-бот для любителей бадминтона (комьюнити ЛАБ). Анализирует игроков и турниры,
помогает оценивать сыгранность соперников и (в перспективе) подбирать партнёров,
строить граф связанности игроков и работать как лайв-помощник на турнире.

> **Статус:** этапы 0–4 завершены (worker: слепок r77, core: метрики, bot: поиск/карточка/соперники).  
> Следующий шаг — этап 5 (H2H для пар).  
> Бриф, план, spike-отчёт — в [`docs/`](.).

## Что уже работает

| Компонент | Статус |
|---|---|
| Парсеры badminton4u.ru (fixtures + unit-тесты) | ✅ этап 0 |
| Maven multi-module: `core`, `worker`, `bot` | ✅ этап 1 |
| PostgreSQL + Flyway V1 (схема БД) | ✅ этап 1 |
| Worker: слепок r77 (турниры, пары, матчи, профили, `rival_summary`), идемпотентный upsert | ✅ этап 2 |
| Метрики (S, форма, прогноз P3) в `core.metrics` | ✅ этап 3 |
| Telegram-бот: поиск, карточка, соперники (H2H и график — заглушки) | ✅ этап 4 |

## Слепок региона (worker, этап 2)

Worker собирает завершённые парные турниры региона за N лет: турниры и пары, участие,
матчи pair-vs-pair (`gamesd`), профили игроков с рейтингами и историей, а также сводку
соперников `rival_summary` (пересобирается из матчей). Слепок **идемпотентен** — повторный
запуск не создаёт дублей.

Параметры — через `.env` (см. [`.env.example`](../.env.example), секции `PARSER_*` / `SNAPSHOT_*`).
Формулы и все константы с дефолтами — в [`FORMULAR.md`](FORMULAR.md).

### Варианты запуска слепка

Поведение задаётся двумя независимыми флагами и параметрами окна:

| Переменная | Дефолт | Назначение |
|---|---|---|
| `SNAPSHOT_RUN_ON_STARTUP` | `false` | Разовый слепок при старте worker (**синхронно**), dev-триггер |
| `SNAPSHOT_SCHEDULED_ENABLED` | `false` | Регулярный слепок по расписанию (`@Scheduled`) |
| `SNAPSHOT_CRON` | `0 0 4 * * *` | Cron регулярного запуска (зона Europe/Moscow) |
| `SNAPSHOT_REGION` | `r77` | Код региона (Москва и МО) |
| `SNAPSHOT_YEARS_BACK` | `3` | Глубина окна в годах от текущей даты |
| `SNAPSHOT_DISCIPLINES` | `D,MD,WD,XD` | Набор дисциплин (по одной или несколько) |
| `SNAPSHOT_MAX_TOURNAMENTS` | `0` | `0` = полный слепок; `>0` — «дымовой» режим на N турниров |
| `SNAPSHOT_TOURNAMENT_IDS` | *(пусто)* | Точечный импорт по ID через запятую; при `run-on-startup=true` — **вместо** полного слепка |

**Сочетания (типовые сценарии):**

| Сценарий | Ключевые переменные |
|---|---|
| Дымовой прогон (быстрая проверка) | `SNAPSHOT_RUN_ON_STARTUP=true`, `SNAPSHOT_MAX_TOURNAMENTS=3`, `SNAPSHOT_DISCIPLINES=WD` |
| Полный разовый слепок r77 | `SNAPSHOT_RUN_ON_STARTUP=true`, `SNAPSHOT_MAX_TOURNAMENTS=0`, `SNAPSHOT_DISCIPLINES=D,MD,WD,XD` |
| Точечный re-import турниров | `SNAPSHOT_RUN_ON_STARTUP=true`, `SNAPSHOT_TOURNAMENT_IDS=12125,12126` |
| Регулярный слепок (прод) | `SNAPSHOT_SCHEDULED_ENABLED=true`, `SNAPSHOT_CRON=0 0 4 * * *`, `SNAPSHOT_MAX_TOURNAMENTS=0` |
| Только сборка данных без запуска | оба флага `false` (worker поднимется, слепок не стартует) |

> Если задан непустой `SNAPSHOT_TOURNAMENT_IDS`, при `run-on-startup=true` выполняется точечный импорт,
> а не полный слепок. Запись `participation`/`match` использует ID из задачи импорта (не из навигации HTML).

> Флаги независимы: можно включить и разовый, и регулярный одновременно.
> Повторный запуск при уже идущем слепке игнорируется (guard в `SnapshotService`).

**Запуск через `dev` (dev-триггер):**

```powershell
.\dev postgres up
# в .env выставить нужные SNAPSHOT_* (см. таблицу выше)
.\dev worker start
# слепок выполнится синхронно при старте; прогресс и итог — в .run\worker.log
```

**Запуск напрямую через Maven (переопределение переменных на один запуск):**

```powershell
$env:SNAPSHOT_RUN_ON_STARTUP='true'; $env:SNAPSHOT_MAX_TOURNAMENTS='3'; $env:SNAPSHOT_DISCIPLINES='WD'
.\mvnw.cmd -pl worker spring-boot:run
```

Проверить наполнение БД:

```powershell
docker exec badminton-lab-postgres psql -U badminton -d badminton_lab -c "select count(*) from match;"
```

## Быстрый старт

### 1. Конфигурация

```powershell
copy .env.example .env
```

Заполните в `.env`:
- `BOT_TOKEN` — токен от [@BotFather](https://t.me/BotFather)
- `TELEGRAM_BOT_ENABLED=true` — чтобы бот подключился к Telegram

Файл `.env` **не коммитится** (см. `.gitignore`).

### 2. Локальные команды (`dev`)

Из корня репозитория — короткие команды без ручной настройки `JAVA_HOME` и переменных:

| Команда | Действие |
|---|---|
| `.\dev help` | Справка |
| `.\dev postgres up` | Поднять PostgreSQL (Docker) |
| `.\dev postgres down` | Остановить PostgreSQL |
| `.\dev postgres status` | Статус контейнера |
| `.\dev worker start` | Запустить worker (Flyway + Spring Boot) |
| `.\dev worker stop` | Остановить worker |
| `.\dev bot start` | Запустить Telegram-бот |
| `.\dev bot stop` | Остановить бота |
| `.\dev verify` | `mvnw clean verify` (все модули) |
| `.\dev test` | Unit-тесты парсера |

Алиасы: `dev.cmd` = `dev.ps1`. Worker/bot пишут логи в `.run/*.log`, PID — в `.run/*.pid`.

**Типичный сценарий:**

```powershell
.\dev postgres up
.\dev worker start
.\dev bot start
# Telegram: отправить боту /start
# ...
.\dev bot stop
.\dev worker stop
.\dev postgres down
```

### 3. PostgreSQL и порт

Docker-контейнер слушает **localhost:5433** (не 5432), чтобы не конфликтовать с локально
установленным PostgreSQL. Порт задаётся в `.env` (`DB_PORT=5433`) и `docker-compose.yml`.

Проверка схемы после старта worker:

```powershell
docker exec badminton-lab-postgres psql -U badminton -d badminton_lab -c "\dt"
```

## Требования

- **Java 17+** (целевая версия проекта — 17; JDK 18 на машине разработчика тоже подходит)
- **Maven** — wrapper в репозитории (`mvnw.cmd`)
- **Docker** — для локального PostgreSQL
- **Telegram bot token** — для модуля `bot`

## Структура репозитория

```
core/            — JPA-сущности, репозитории, Flyway-миграции, конфиг метрик
worker/          — парсер badminton4u.ru, фоновый слепок (этап 2+)
bot/             — Telegram long polling
dev.ps1, dev.cmd — локальные команды запуска/остановки
docker-compose.yml
.env.example     — шаблон конфигурации (.env — локально, не в git)

docs/README.md   — этот файл
docs/BRIEF.md    — бриф: решения, формулы, открытые вопросы
docs/PLAN.md     — план реализации по этапам
docs/FORMULAR.md — формулы и константы (дефолты, локализация, описание)
docs/spike-parser.md — отчёт spike парсера (этап 0)
docs/schema.sql  — черновик DDL (источник для Flyway V1)
AGENTS.md        — инструкции для AI-агентов
```

## Возможности (roadmap)

### MVP (в работе)
- Поиск игрока по нику/ФИО.
- Карточка игрока: рейтинги по дисциплинам, история рейтинга.
- **H2H:** история встреч, индекс S, форма, прогноз P3.

### После MVP
- Подбор партнёра на турнир.
- Граф связанности игроков и турниров.
- Лайв-помощник на турнире.

Стек: **Java 17**, Spring Boot 3, PostgreSQL (+ pg_trgm), bot + worker.

## Источник данных

Основной источник — [`badminton4u.ru`](https://badminton4u.ru) (серверный HTML, стабильные ID).
Сбор — вежливый парсинг: rate-limit, кеширование, честный User-Agent.
Старт по региону **Москва и МО (`r77`)**, глубина **3 года**, обновление раз в сутки.

Официальные формулы рейтинга ЛАБ — as-is (см. [`BRIEF.md`](BRIEF.md)):
- дельта одиночки: `(100 - (R_поб - R_проигр)) / 10`;
- рейтинг пары: `(A + B) / 2`.

## Правовая заметка

Проект использует публично доступные данные community-платформы. Сбор ведётся бережно
(ограничение частоты запросов, кеш). При росте нагрузки — согласование доступа/API с владельцами сайта.
