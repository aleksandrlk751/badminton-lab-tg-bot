# Документация проекта

> **Запуск и слепок** — [`README.md`](../README.md) в корне репозитория.  
> **AI-агенты:** [`AGENTS.md`](../AGENTS.md) → [`ROUTING.md`](ROUTING.md).

## Основные документы

| Документ | Назначение |
|---|---|
| [`BRIEF.md`](BRIEF.md) | Продукт, UX-сценарии, метрики (смысл), решения, открытые вопросы |
| [`data-model.md`](data-model.md) | Источник badminton4u, сущности БД, слепок vs lazy |
| [`PLAN.md`](PLAN.md) | **Активный план:** этапы 7–10, техдолг, roadmap |
| [`PLAN-COMPLETED.md`](PLAN-COMPLETED.md) | Архив этапов 0–6 (DoD) |
| [`FORMULAR.md`](FORMULAR.md) | Формулы и локализация в коде |
| [`FORMULAR-CONFIG.md`](FORMULAR-CONFIG.md) | Дефолты YAML/env (метрики, parser, snapshot) |
| [`ROUTING.md`](ROUTING.md) | Какой код и какие docs читать под задачу |
| [`spike-parser.md`](spike-parser.md) | Контракты парсера, fixtures, spike |
| [`schema.sql`](schema.sql) | Черновик DDL (канон — Flyway в `core`) |
| [`messages/`](messages/) | UX-тексты и клавиатуры бота |

## Быстрые ссылки по задачам

- Парсер / fixtures → `spike-parser.md` + `worker/.../parser/`
- Миграция БД → `data-model.md` + `core/.../db/migration/`
- Метрика / калибровка → `FORMULAR.md` + `FORMULAR-CONFIG.md`
- Новый экран бота → `messages/NN-*.md` + `bot.view` / `bot.handler`
