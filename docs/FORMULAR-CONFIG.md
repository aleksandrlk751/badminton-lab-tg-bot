# Константы и операционные параметры

> Дефолты метрик (`badminton-lab.metrics.*`) и worker (`badminton-lab.parser.*`, `badminton-lab.snapshot.*`).  
> **Формулы** — [`FORMULAR.md`](FORMULAR.md). Смысл метрик — [`BRIEF.md`](BRIEF.md) §4, §6.

---

## 1. Константы метрик — дефолты и локализация

Все ключи — под `badminton-lab.metrics.*` в
[`core/src/main/resources/application-core.yml`](../core/src/main/resources/application-core.yml),
связаны с полями
[`MetricsProperties`](../core/src/main/java/ru/badmintonlab/core/config/MetricsProperties.java).

| Символ | Дефолт | Ключ конфига | Поле `MetricsProperties` | Применяется (FORMULAR §) | Описание |
|---|---|---|---|---|---|
| `H` | `180` | `half-life-days` | `halfLifeDays` | 2.1, 2.2, 2.3, 2.4, 2.8 | Период полураспада (дни) для `S`, `Form`, `Stability` |
| `W_max` | `0.8` | `early-decay-max` | `earlyDecayMax` | 2.1, 2.2, 2.8 | Потолок веса свежести в начале первого периода `(0.5, 1]` |
| `α` | `0.5` | `early-decay-power` | `earlyDecayPower` | 2.1, 2.2, 2.8 | Крутизна спада веса в первом периоде (`> 0`; `1` = прежняя форма кривой) |
| `ε` | `11.5` | `stability-surprise-threshold` | `stabilitySurpriseThreshold` | 2.8 | Порог \|δ\| для учёта сюрприза матча |
| `S_neutral` | `0.8` | — (константа в `StabilityService`) | `NEUTRAL_BETWEEN_SCORE` | 2.8 | `S^between` при нейтральном турнире в паре |
| `zone2` | `70` | `stability-zones.zone2-min` | `stabilityZones.zone2Min` | 2.8.1 | Нижняя граница зоны 🟡 |
| `zone3` | `80` | `stability-zones.zone3-min` | `stabilityZones.zone3Min` | 2.8.1 | Нижняя граница зоны ⚪ |
| `zone4` | `86` | `stability-zones.zone4-min` | `stabilityZones.zone4Min` | 2.8.1 | Нижняя граница зоны 🟢 |
| `zone5` | `92` | `stability-zones.zone5-min` | `stabilityZones.zone5Min` | 2.8.1 | Нижняя граница зоны 🔥 |
| `k` | `0.5` | `form-k` | `formK` | 2.3, 2.4 | Вклад формы в эффективный рейтинг |
| `S_ref` | `1.0` | `s-ref` | `sRef` | 2.3, 2.4 | Опорная сыгранность для веса смешивания `w` |
| `Bmax` | `20.0` | `b-max` | `bMax` | 2.4 | Максимальный бонус парного рейтинга за сыгранность |
| `S0` | `1.0` | `s0` | `s0` | 2.4 | Масштаб насыщения бонуса пары |
| `w1` | `0.35` | `w1` | `w1` | 2.5 | Вес компонента `C_limit` в score |
| `w2` | `0.25` | `w2` | `w2` | 2.5 | Вес компонента `C_delta` в score |
| `w3` | `0.25` | `w3` | `w3` | 2.5 | Вес компонента `C_S` в score |
| `w4` | `0.10` | `w4` | `w4` | 2.5 | Вес компонента `C_form` в score |
| `w5` | `0.05` | `w5` | `w5` | 2.5 | Вес компонента `C_accent` (δ по типу турнира) |
| `D_scale` | `10.0` | `d-scale` | `dScale` | 2.5 | Масштаб сигмоиды для `Δ_joint` и `δ_cat` |
| `T` | `12` | `partner-history-months` | `partnerHistoryMonths` | 2.5 | Окно «успешной истории» для сортировки блока «Уже играли» (bot); **не** обрезает C_delta |
| `F_form` | `10.0` | `partner-form-scale` | `partnerFormScale` | 2.5 | Масштаб Form кандидата для `C_form` |
| `k_stab` 🔥 | `1.1` | `partner-form-stability.super-stable` | `partnerFormStability.superStable` | 2.5 | Множитель Form при зоне «супер стабилен» |
| `k_stab` 🟢 | `1.0` | `partner-form-stability.stable` | `partnerFormStability.stable` | 2.5 | «Стабилен» |
| `k_stab` ⚪ | `0.85` | `partner-form-stability.middle` | `partnerFormStability.middle` | 2.5 | «Середина» |
| `k_stab` 🟡 | `0.7` | `partner-form-stability.unstable` | `partnerFormStability.unstable` | 2.5 | «Нестабилен» |
| `k_stab` 🔴 | `0.5` | `partner-form-stability.very-unstable` | `partnerFormStability.veryUnstable` | 2.5 | «Супер нестабилен» |
| `H_accent` | `180` | `game-accent.half-life-days` | `gameAccent.halfLifeDays` | 2.7 | Полураспад для игрового акцента |
| `W_max_accent` | `0.8` | `game-accent.early-decay-max` | `gameAccent.earlyDecayMax` | 2.7 | Потолок веса свежести акцента |
| `α_accent` | `0.5` | `game-accent.early-decay-power` | `gameAccent.earlyDecayPower` | 2.7 | Крутизна спада акцента |
| `W_min` | `3.0` | `game-accent.min-weight-sum` | `gameAccent.minWeightSum` | 2.7 | Мин. сумма весов для показа акцента |
| `display_window` | `180` | `game-accent.display-window-days` | `gameAccent.displayWindowDays` | 2.7 | Окно подсчёта числа игр на экране (дни) |

> Инварианты (`MetricsProperties`): `halfLifeDays > 0`, `earlyDecayMax ∈ (0.5, 1]`, `earlyDecayPower > 0`,
> `partnerHistoryMonths > 0`.
> Ожидается `w1 + w2 + w3 + w4 + w5 = 1` (сейчас `0.35 + 0.25 + 0.25 + 0.10 + 0.05`), но это **не** валидируется в коде.

### 1.1 Пробелы / TODO

| Символ | Статус | Локализация |
|---|---|---|
| `S_ref_partner` | `s-ref-partner` (дефолт = `s-ref`) | `sRefPartner` — §2.5 `FORMULAR.md` |

---

## 2. Операционные параметры (парсер и слепок)

Не формулы, но дефолтные константы worker. Переопределяются через `.env`
(см. [`.env.example`](../.env.example)); бинды —
[`ParserProperties`](../worker/src/main/java/ru/badmintonlab/worker/config/ParserProperties.java) и
[`SnapshotProperties`](../worker/src/main/java/ru/badmintonlab/worker/config/SnapshotProperties.java),
значения — в [`worker/src/main/resources/application.yml`](../worker/src/main/resources/application.yml).

### 2.1 Парсер (`badminton-lab.parser.*`)

| Параметр | Дефолт | Env | Описание |
|---|---|---|---|
| `base-url` | `https://badminton4u.ru/` | `PARSER_BASE_URL` | Базовый URL источника |
| `threads` | `8` | `PARSER_THREADS` | Размер пула потоков слепка |
| `max-rps` | `10` | `PARSER_MAX_RPS` | Глобальный лимит запросов/сек (вежливый парсинг) |
| `connect-timeout-ms` | `15000` | `PARSER_CONNECT_TIMEOUT_MS` | Таймаут HTTP-запроса |
| `retry-max` | `3` | `PARSER_RETRY_MAX` | Число повторов при ошибке (404 не ретраится) |
| `retry-backoff-ms` | `1000` | `PARSER_RETRY_BACKOFF_MS` | Базовый backoff (экспоненциальный) |
| `user-agent` | `BadmintonLabBot/0.1 …` | `PARSER_USER_AGENT` | Честный User-Agent с контактом |

### 2.2 Слепок (`badminton-lab.snapshot.*`)

| Параметр | Дефолт | Env | Описание |
|---|---|---|---|
| `region-code` | `r77` | `SNAPSHOT_REGION` | Регион (Москва и МО) |
| `years-back` | `3` | `SNAPSHOT_YEARS_BACK` | Глубина окна (годы) |
| `disciplines` | `D,MD,WD,XD` | `SNAPSHOT_DISCIPLINES` | Набор дисциплин (раздельные запросы по типам) |
| `run-on-startup` | `false` | `SNAPSHOT_RUN_ON_STARTUP` | Разовый синхронный слепок при старте (dev) |
| `max-tournaments` | `0` | `SNAPSHOT_MAX_TOURNAMENTS` | `0` = без лимита; `>0` — «дымовой» режим |
| `scheduled-enabled` | `false` | `SNAPSHOT_SCHEDULED_ENABLED` | Регулярный запуск по cron |
| `cron` | `0 0 4 * * *` | `SNAPSHOT_CRON` | Расписание (зона Europe/Moscow) |
| `tournament-ids` | *(пусто)* | `SNAPSHOT_TOURNAMENT_IDS` | Точечный импорт по ID (через запятую); при `run-on-startup=true` заменяет полный слепок |

Варианты сочетаний — [`README.md`](../README.md) → «Варианты запуска слепка».

---

## 3. Калибровка

Дефолты метрик — **стартовые плейсхолдеры**. Калибровка на реальных данных — `PLAN.md` v2+, `BRIEF.md` §11.
До калибровки менять значения только через конфиг/YAML, без правок формул в коде.
