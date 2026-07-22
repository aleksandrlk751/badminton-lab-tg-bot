# Spike парсера (этап 0)

> Статус: **go** (2026-07-21).  
> Java spike: **17** (целевая — **17**, Spring Boot 3).

## Эталоны (от пользователя)

| Fixture | URL |
|---|---|
| `player-18499.html` | https://badminton4u.ru/players/18499 |
| `tournament-completed-12713.html` | https://badminton4u.ru/tournaments/12713 |
| `tournament-upcoming-12834.html` | https://badminton4u.ru/tournaments/12834 |

Дополнительно:

| Fixture | URL | Зачем |
|---|---|---|
| `tournaments-list-r77-pairs.html` | список r77 | парсер списка турниров |
| `games-tournament-12713.html` | `/gamesd/?tourID=12713` | pair-vs-pair матчи и агрегат соперников |
| `tournament-completed-12125.html` | `/tournaments/12125` | ID без `addComment` (alternate link) |
| `tournament-completed-12126.html` | `/tournaments/12126` | место `-` в итоговой таблице |
| `games-tournament-12125.html` | `/gamesd/?tourID=12125` | re-import эталон (25 матчей) |

## Сборка и тесты

```powershell
.\dev test
```

Или напрямую через Maven:

```powershell
.\mvnw.cmd -pl worker test
```

> Surefire: `forkCount=0` из-за кириллицы в `%TEMP%` на Windows.  
> Полная сборка всех модулей: `.\dev verify`. См. [`README.md`](README.md).

## Парсеры (unit-тесты green)

| Класс | Что извлекает |
|---|---|
| `TournamentListParser` | ID, дата, лимит, медалисты-пары |
| `TournamentResultsParser` | итоговая таблица пар |
| `TournamentRegistrationParser` | пары в `#tour-reg-list1` |
| `PlayerProfileParser` | ник, ФИО, город, рейтинг D, история |
| `TournamentGamesParser` | матчи 2v2 с `gamesd/?tourID=` |
| `RivalSummaryAggregator` | player↔opponent W/L из списка `PairMatch` (вариант C) |

## Ключевые выводы

### Pair-vs-pair — **GO**

Страница `gamesd/?tourID={id}` отдаёт **SSR-таблицу** с 4 игроками на матч (обе пары, рейтинги, счёт по сетам, дельты).  
Это основной источник для `match` / `match_player` и pair H2H.

`games/?user1ID&user2ID` — по-прежнему без строк в HTML (lazy JS); для MVP H2H деталей использовать `gamesd` или reverse-engineer AJAX позже.

### Регистрация — SSR или AJAX

- Турнир **12834**: пары уже в HTML (`#tour-reg-list1`, 14 пар).
- Турнир **12840** (ранний spike): пустые `#tour-reg-list1/2` → нужен POST `/?ajax` (поле `list` в JSON).

Worker поддерживает **оба** варианта регистрации (SSR и AJAX).

### `/rivals/{id}` — не используем

Spike: таблица на `/rivals/{id}` для парных игроков пустая. Соперники — только из `gamesd`
(`RivalSummaryAggregator`, вариант C). См. [`BRIEF.md`](BRIEF.md).

### external_key матча

```
badminton4u:game:{tournamentId}:{playedAt}:{sortedA}:vs{sortedB}:{scoreSets}:{stage}
```

## Go/no-go

| Компонент | Вердикт |
|---|---|
| Слепок турниров/участников | **GO** |
| Итоги пар | **GO** |
| Регистрация пар | **GO** (SSR + AJAX fallback) |
| Профиль игрока | **GO** |
| Rivals `/rivals` | **Не используем** |
| Соперники (агрегат из `gamesd`) | **GO** — `RivalSummaryAggregator` |
| Pair-vs-pair матчи | **GO** через `gamesd/?tourID=` |
| H2H `games/?user1&user2` | **Условно** — нужен AJAX или агрегация из `gamesd` |

**Этап 0 закрыт.** Следующий шаг — этап 2 (worker: слепок r77). Локальные команды: [`README.md`](README.md).

## Примеры для ручной сверки

Сверьте значения **парсера** (колонка «Парсер») с сайтом по указанному URL. Fixture — снимок HTML на момент spike.

### 1. Список турниров — `TournamentListParser`

| | |
|---|---|
| **Fixture** | `tournaments-list-r77-pairs.html` |
| **URL** | [список r77](https://badminton4u.ru/tournaments/?cities[]=r77&types[]=md&types[]=wd&types[]=xd&types[]=d&winners=1) |
| **Парсер** | id=`12713`, дата=`14.06.2026`, время=`12:00`, лимит=`550`, название=`Женская лига WDC`, doubles=`true` |
| **Медалисты** | 🥇 `19080` + `18870`; 🥈 `18153` + `16426`; 🥉 `35663` + `4396` |

### 2. Итоги турнира — `TournamentResultsParser`

| | |
|---|---|
| **Fixture** | `tournament-completed-12713.html` |
| **URL** | [tournaments/12713](https://badminton4u.ru/tournaments/12713) → вкладка «результаты» |
| **Парсер (1 место)** | `19080` + `18870`; рейтинги до `577` / `514`; пара `546`; дельта `+27.3`; матчи `5 (5-0)`; сеты `12 (10-2)` |
| **Парсер** | всего строк в таблице пар: **12** |

**Краевые случаи (зафиксировано на живых fixtures):**

| Случай | Поведение парсера |
|---|---|
| Место `-` в итоговой таблице | `lastPlace + 1` (пара без официального места); fixture `tournament-completed-12126.html` |
| Нет кнопки `addComment` | ID турнира из `link[rel=alternate]` или `img[src*=/tournaments/…/res-]`; fixture `tournament-completed-12125.html` |
| Повторная встреча пар (группа → финал) | Два разных `external_key` матча — не ошибка; ограничение `pair` — только партнёрство (два игрока на стороне) |

### 3. Регистрация пар — `TournamentRegistrationParser`

| | |
|---|---|
| **Fixture** | `tournament-upcoming-12834.html` |
| **URL** | [tournaments/12834](https://badminton4u.ru/tournaments/12834) → «Список зарегистрированных» |
| **Парсер** | id=`12834`, пар в списке: **14**, `#tour-reg-list2` пуст |
| **Пара №1** | `19045` + `19048`; рейтинги `409` / `308`; пара `359`; заявка `02.07.2026 16:38` |
| **Пара №2** | `19570` + `36808`; рейтинги `470` / `379`; пара `425` |

### 4. Профиль игрока — `PlayerProfileParser`

| | |
|---|---|
| **Fixture** | `player-18499.html` |
| **URL** | [players/18499](https://badminton4u.ru/players/18499) |
| **Парсер** | id=`18499`, nick=`Olya_fox`, ФИО=`Крупская Ольга Андреевна`, город=`Красногорск`, рука=`правая` |
| **Рейтинг D** | `535` |
| **История D (последние 3 точки)** | `09.03.2026 → 548`, `11.04.2026 → 549`, `14.06.2026 → 535` (график «последние 10» / `chartLastData_d`) |

### 5. Соперники — `RivalSummaryAggregator` (вариант C)

| | |
|---|---|
| **Fixture** | `games-tournament-12713.html` (тот же, что §6) |
| **Pipeline** | `TournamentGamesParser` → `RivalSummaryAggregator.aggregate()` |
| **Финал (19080 vs 18153)** | 19080: 1 победа; 18153: 1 поражение (player↔player из одного матча) |
| **Полный турнир** | у 19080 есть соперники с `games ≥ 2`; delta **не** в summary — только в `PairMatch` |

### 6. Матчи pair-vs-pair — `TournamentGamesParser`

| | |
|---|---|
| **Fixture** | `games-tournament-12713.html` |
| **URL** | [gamesd/?tourID=12713](https://badminton4u.ru/gamesd/?tourID=12713) |
| **Парсер (финал)** | дата `14.06.2026 12:00`, стадия `фин`, счёт **`1:2`** |
| **Сторона A** | `18153` (480) + `16426` (525); дельта **−5.7** |
| **Сторона B** | `19080` (577) + `18870` (514); дельта **+5.7** |
| **Парсер** | всего матчей в таблице: **23** |

Если расхождение — укажите URL, поле и значение на сайте; поправим парсер или fixture.
