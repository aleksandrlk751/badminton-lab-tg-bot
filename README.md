# Badminton LAB Telegram Bot

Telegram-бот для любителей бадминтона (комьюнити ЛАБ). Анализирует игроков и турниры,
оценивает сыгранность соперников, подбирает партнёров на турнир, в перспективе — граф связанности и лайв-помощник.

> **Статус:** этапы **0–6 ✓** (слепок r77, пол, метрики, bot shell, **подбор партнёра**).  
> **Текущий:** этап **7** — H2H DoD (дисциплина, тип пары, P3, lazy `games`). Далее: 8 TG → 9 лайв → 10 VPS. График рейтинга — v2.  
> Документация — [`docs/README.md`](docs/README.md). Навигация по коду — [`docs/ROUTING.md`](docs/ROUTING.md).

## Что уже работает

| Компонент | Статус |
|---|---|
| Парсеры badminton4u.ru (fixtures + unit-тесты) | ✅ этап 0 |
| Maven multi-module: `core`, `worker`, `bot` | ✅ этап 1 |
| PostgreSQL + Flyway (V1 схема, V2 `player.sex`) | ✅ этапы 1, 5 |
| Worker: слепок r77, `rival_summary`, будущие турниры + регистрация | ✅ этапы 2, 6 |
| Метрики (S, форма, P3, partner score, акцент, стабильность) | ✅ этапы 3, 6 |
| Bot: поиск, карточка, соперники, H2H (wizard, DoD — этап 7), подбор партнёра | ✅ этапы 4–6 |
| График рейтинга на карточке | заглушка (**v2**) |

## Слепок региона (worker, этап 2)

Worker собирает завершённые парные турниры региона за N лет: турниры и пары, участие,
матчи pair-vs-pair (`gamesd`), профили игроков с рейтингами и историей, а также сводку
соперников `rival_summary` (пересобирается из матчей). Слепок **идемпотентен** — повторный
запуск не создаёт дублей.

Параметры — через `.env` (см. [`.env.example`](.env.example), секции `PARSER_*` / `SNAPSHOT_*`).
Формулы — [`docs/FORMULAR.md`](docs/FORMULAR.md); дефолты YAML — [`docs/FORMULAR-CONFIG.md`](docs/FORMULAR-CONFIG.md).

### Варианты запуска слепка

| Переменная | Дефолт | Назначение |
|---|---|---|
| `SNAPSHOT_RUN_ON_STARTUP` | `false` | Разовый слепок при старте worker (**синхронно**), dev-триггер |
| `SNAPSHOT_SCHEDULED_ENABLED` | `false` | Регулярный слепок по расписанию (`@Scheduled`) |
| `SNAPSHOT_CRON` | `0 0 4 * * *` | Cron регулярного запуска (зона Europe/Moscow) |
| `SNAPSHOT_REGION` | `r77` | Код региона (Москва и МО) |
| `SNAPSHOT_YEARS_BACK` | `3` | Глубина окна в годах от текущей даты |
| `SNAPSHOT_DISCIPLINES` | `D,MD,WD,XD` | Набор дисциплин |
| `SNAPSHOT_MAX_TOURNAMENTS` | `0` | `0` = полный слепок; `>0` — «дымовой» режим |
| `SNAPSHOT_TOURNAMENT_IDS` | *(пусто)* | Точечный импорт по ID |
| `SNAPSHOT_SYNC_SEX_ON_STARTUP` | `false` | Только sync пола |
| `SNAPSHOT_INFER_SEX_FROM_NAMES_ON_STARTUP` | `false` | Fallback пола по ФИО (офлайн) |

**Сочетания:** дымовой — `RUN_ON_STARTUP=true`, `MAX_TOURNAMENTS=3`, `DISCIPLINES=WD`; полный r77 — `MAX_TOURNAMENTS=0`, все дисциплины. Подробнее — [`docs/FORMULAR-CONFIG.md`](docs/FORMULAR-CONFIG.md) §2.2.

```powershell
.\dev postgres up
# .env: SNAPSHOT_*
.\dev worker start
```

## Синхронизация пола (worker, этап 5)

См. [`docs/spike-parser.md`](docs/spike-parser.md) §7, [`docs/PLAN-COMPLETED.md`](docs/PLAN-COMPLETED.md) этап 5.

```powershell
$env:SNAPSHOT_SYNC_SEX_ON_STARTUP='true'
$env:SNAPSHOT_RUN_ON_STARTUP='false'
.\mvnw.cmd -pl worker spring-boot:run
```

Покрытие r77 (2026-07-23): **~99,96%** с `sex` (4913/4915).

## Быстрый старт

```powershell
copy .env.example .env
# BOT_TOKEN, TELEGRAM_BOT_ENABLED=true
.\dev postgres up
.\dev worker start
.\dev bot start
```

| Команда | Действие |
|---|---|
| `.\dev help` | Справка |
| `.\dev postgres up` / `down` / `status` | PostgreSQL (Docker, порт **5433**) |
| `.\dev bot start` / `stop` | Telegram-бот |
| `.\dev worker start` / `stop` | Worker (слепок) |
| `.\dev verify` | `mvnw clean verify` |
| `.\dev test` | Unit-тесты парсера |

## Требования

Java **17+**, Maven wrapper, Docker (PostgreSQL), Telegram bot token.

## Структура репозитория

```
README.md          — этот файл (запуск, слепок)
AGENTS.md          — инструкции для AI-агентов
core/ worker/ bot/ — модули Maven
docs/              — бриф, план, формулы, UX (индекс: docs/README.md)
```

## Источник данных

[`badminton4u.ru`](https://badminton4u.ru) — регион **r77**, **3 года**, вежливый парсинг. Подробности — [`docs/data-model.md`](docs/data-model.md).

## Правовая заметка

Публичные данные community-платформы; rate-limit и кеш. При росте нагрузки — согласование с владельцами сайта.
