# Learn Page Stats

The Learn page is designed so a normal user can understand progress quickly without seeing academic model internals.

## Primary Metrics

- `Seen`: words displayed at least once.
- `Familiar`: words in Familiar, Stable, or Mastered states.
- `Mastered`: words with enough passive exposure for rare maintenance.
- `Due Soon`: words whose estimated recall is dropping toward review range.

## Main Chart

The app uses a lightweight custom Android `View` with `Canvas` to draw a stacked area-style chart. This fits the existing View-based app and avoids adding a heavy chart dependency.

Shown layers:

- New
- Learning
- Familiar
- Stable
- Mastered

## HSK Filters

Filters are:

```text
All | HSK 1 | HSK 2 | HSK 3 | HSK 4 | HSK 5 | HSK 6 | Uncategorized
```

The current built-in and generated vocabulary do not include reliable HSK levels, so most words appear as Uncategorized until sourced levels are added.

## Daily Snapshots

`DailyLearningStats` stores compact aggregate shape for counts, exposure totals, unlocks, app opens, and movements to Familiar, Stable, and Mastered. The current implementation can derive the visible weekly series from compact progress rows and recent display sessions.

## Why Half-Life Is Hidden

Half-life and predicted recall are useful scheduling internals, but they can feel academic and overconfident. The UI surfaces friendly estimated progress instead.
