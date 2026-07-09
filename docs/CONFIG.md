# Scheduler Config

All scheduler defaults live in `SchedulerConfig.kt` and `SchedulerFeatureFlags.kt`.

| Constant | Default | Meaning |
| --- | ---: | --- |
| `DEFAULT_MINIMUM_SPACING_MINUTES` | 90 | Default minimum target before word can change; user-adjustable in the Schedule tab |
| `BASE_HALF_LIFE_HOURS` | 2.0 | Starting estimated half-life |
| `HALF_LIFE_GROWTH_FACTOR` | 1.45 | Half-life growth per effective exposure |
| `MAX_HALF_LIFE_HOURS` | 720 | Cap at 30 days |
| `REVIEW_RECALL_THRESHOLD` | 0.80 | Below this, word becomes review-eligible |
| `TOO_SOON_RECALL_THRESHOLD` | 0.90 | Above this, avoid repeating too soon |
| `INACTIVITY_PAUSE_THRESHOLD_MINUTES` | 90 | Long inactivity/sleep-like pause threshold |
| `DEFAULT_NEW_WORD_RATIO` | 0.70 | Default new-word scheduling share |
| `DEFAULT_REVIEW_WORD_RATIO` | 0.30 | Default review-word scheduling share |
| `OVERDUE_HEAVY_REVIEW_RATIO` | 0.50 | Review share when many words are overdue |
| `LOW_OVERDUE_REVIEW_RATIO` | 0.20 | Review share when few words are overdue |
| `SCREEN_EXPOSURE_UNIT_MINUTES` | 10 | Converts eligible screen time to exposure units |
| `MAX_SCREEN_EXPOSURE_UNITS_PER_SESSION` | 3.0 | Prevents accidental long sessions from dominating |
| `STABLE_MIN_EFFECTIVE_EXPOSURES` | 8 | Stable threshold |
| `STABLE_MIN_DISTINCT_DAYS` | 3 | Stable spacing threshold |
| `STABLE_MIN_HALF_LIFE_HOURS` | 72 | Stable memory-strength threshold |
| `MASTERED_MIN_EFFECTIVE_EXPOSURES` | 12 | Mastered threshold |
| `MASTERED_MIN_DISTINCT_DAYS` | 5 | Mastered spacing threshold |
| `MASTERED_MIN_HALF_LIFE_HOURS` | 168 | Mastered memory-strength threshold |
| `RARE_MAINTENANCE_REVIEW_DAYS` | 30 | Rare resurfacing interval for mastered/retired words |

## Phone-State Multipliers

| State | Multiplier |
| --- | ---: |
| `ASLEEP_OR_INACTIVE` | 0.0 |
| `LOCKED_IDLE` | 0.1 |
| `GLANCE_OPPORTUNITY` | 0.5 |
| `ACTIVE_PHONE_USE` | 1.0 |
| `FULL_APP_LEARNING` | 1.5 |
| `MOVING_OR_WORKOUT` | 0.3 |
| `UNKNOWN` | 0.3 |

## Feature Flags

| Flag | Default |
| --- | --- |
| `ENABLE_PASSIVE_TELEMETRY` | `true` |
| `ENABLE_CONTEXT_AWARE_PAUSE` | `true` |
| `ENABLE_HALF_LIFE_SCHEDULER` | `true` |
| `ENABLE_MASTERED_RETIRED_STATES` | `true` |
| `ENABLE_PASSIVE_PROMPTS` | `true` |
| `ENABLE_LEARN_PAGE_STATS` | `true` |
| `ENABLE_HSK_FILTERS` | `true` |
| `ENABLE_ACTIVITY_RECOGNITION` | `false` |
| `ENABLE_DND_CONTEXT` | `false` |
