# Завершённые этапы (0–6)

> Архив: цели, задачи, DoD и даты закрытия.  
> **Активный план** (этапы 7–10, техдолг, roadmap) — [`PLAN.md`](PLAN.md).

---

## Этап 0 — Spike парсера (парные данные) ✓

**Статус:** завершён (2026-07-21). Отчёт и выводы: **[`spike-parser.md`](spike-parser.md)**.

**Цель:** подтвердить, что с badminton4u.ru можно стабильно получить данные для парных игроков.

**Задачи (выполнено):**
- HTML-fixtures: список турниров, будущий/прошедший парный турнир, профиль игрока, `gamesd` турнира.
- Прототип парсеров в `worker`: пары, pair-vs-pair, `external_key` матчей, агрегатор соперников.
- Зафиксировано: регистрация SSR/AJAX; `/rivals` отвергнут — соперники из `gamesd`.

**Итог spike (кратко):**
- Pair-vs-pair **GO** через `gamesd/?tourID=` (SSR, 4 игрока на матч).
- Регистрация: SSR в `#tour-reg-list1` **или** AJAX (`POST /?ajax`) — worker поддержит оба варианта.
- Модуль `worker`, Java **17** (spike), **5** fixtures, 5 парсеров + агрегатор, unit-тесты green.
- Эталоны: игрок [18499](https://badminton4u.ru/players/18499), турниры [12713](https://badminton4u.ru/tournaments/12713) / [12834](https://badminton4u.ru/tournaments/12834).

**DoD:**
- [x] Отчёт в [`spike-parser.md`](spike-parser.md): go/no-go по pair-vs-pair.
- [x] ≥5 fixtures в `worker/src/test/resources/html/` (фактически **5**).
- [x] Парсер проходит unit-тесты на fixtures для: турнир, пара в регистрации, итоговая строка пары.

**Оценка:** 2–4 дня.

---

## Этап 1 — Каркас проекта ✓

**Статус:** завершён (2026-07-21).

**Цель:** собираемый multi-module проект + локальная инфраструктура.

**Задачи (выполнено):**
- Maven parent + модули: **`core`**, **`worker`**, **`bot`**.
- `docker-compose.yml`: PostgreSQL (+ pg_trgm), порт **5433**.
- Flyway V1: перенос [`schema.sql`](schema.sql) в `core/src/main/resources/db/migration/`.
- `.env.example`, **`dev.ps1` / `dev.cmd`** (см. [`README.md`](../README.md)).

**DoD:**
- [x] `./mvnw clean verify` проходит.
- [x] Postgres + миграции; bot отвечает на `/start`.

**Оценка:** 3–5 дней.

---

## Этап 2 — Worker: полный слепок региона ✓

**Цель:** ежедневный слепок Москва/МО за 3 года в PostgreSQL.

**Статус:** реализовано и проверено дымовым прогоном. Полный 3-летний слепок r77 — за пользователем.

**Ключевое:** pipeline `worker.snapshot`, `gamesd`, `rival_summary` (вариант C), идемпотентный upsert,
`@Scheduled` + dev-trigger, `SNAPSHOT_TOURNAMENT_IDS`. Регистрация будущих турниров — в **этапе 6**
(`UpcomingTournamentsSyncService`).

**DoD:** дымовой прогон green; идемпотентность; unit-тесты парсеров/fixtures.

Подробности — исторический текст в git / [`data-model.md`](data-model.md), [`README.md`](../README.md) § слепок.

**Оценка:** 1–2 недели.

---

## Этап 3 — Метрики (core) ✓

**Статус:** реализовано (2026-07-22). `core.metrics`, unit-тесты без БД.

**Сервисы:** `PlayabilityIndexService`, `FormService`, `PairRatingService`, `ForecastService`, `MetricMath`;
позже — `PartnerScoreService` (**этап 6 ✓**), `GameAccentService`, `StabilityService`.

**DoD:** формулы = [`BRIEF.md`](BRIEF.md) / [`FORMULAR.md`](FORMULAR.md); тесты green.

**Оценка:** 4–6 дней.

---

## Этап 4 — Bot: shell + поиск + карточка ✓

**Статус:** реализовано (2026-07-22). `UpdateDispatcher`, поиск pg_trgm, карточка, соперники.

**DoD:** сборка green; unit-тесты диспетчера. Ручные сценарии BRIEF §5 и нагрузка поиска — открыты.

**Примечание (2026-07-24):** H2H-wizard и кнопка H2H **не заглушки** — базовый flow в боте; DoD H2H — **этап 7**.
«История рейтинга» — заглушка (**v2**).

**Оценка:** 1 неделя.

---

## Этап 5 — Worker: пол игрока ✓

**Статус:** реализовано (2026-07-23). Flyway V2 (`player.sex`), справочник + fallback, `PairCompositionService`.

**DoD:** ~99,96% `sex` на r77; unit-тесты парсера и `PairCompositionService`.

**Блокирует:** этап **7** (H2H — тип пары), этап **6** (фильтр кандидатов по полу).

**Оценка:** 2–4 дня.

---

## Этап 6 — Подбор партнёра на турнир ✓

**Статус:** реализовано (2026-07-24). `UpcomingTournamentsSyncService`, `PartnerScoreService`, bot-flow «🤝 Партнёр на турнир».

**DoD:** сервис + unit-тесты; bot-flow. Открыто: ручная ревизия на 3 турнирах, стабильность score.

**Зависимости:** этапы 2, 3, 5.

**Оценка:** 1 неделя.
