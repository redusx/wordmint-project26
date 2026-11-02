# Learning architecture

## Layers

```
ui/screens + ui/components    stateless Compose, render immutable state
viewmodel/                    StateFlow<UiState>, user actions
domain/                       pure Kotlin, no Android imports
  scheduling/                 SpacedRepetitionScheduler, Fsrs6Scheduler
  study/                      StudyQueuePolicy, LearningDay, LearningStatus
  progress/                   ProgressCalculator
  repository/                 interfaces the ViewModels depend on
data/                         Room, Retrofit, ML Kit, SharedPreferences
reminders/                    WorkManager worker, scheduler, notification, receiver
AppContainer                  manual dependency wiring, created by the Application
```

Composables never query the database and never compute an interval.

## Choosing the FSRS implementation

| Candidate | Algorithm | License | Tests | Verdict |
|---|---|---|---|---|
| [FsrsKt](https://github.com/TheRockYT/FsrsKt) 1.0.1 | FSRS-6 | GPL-3.0 | None in the repository's source tree | Rejected: GPL would force the app's license, and it is a two-star project without tests |
| [fsrs-kotlin](https://github.com/LordDarthArt/fsrs-kotlin) 0.1.0 | FSRS-5 | n/a | n/a | Rejected: wrong algorithm version |
| [android-fsrs](https://github.com/open-spaced-repetition/android-fsrs) | Unverified | n/a | n/a | Rejected: unmaintained since 2023 |
| [py-fsrs](https://github.com/open-spaced-repetition/py-fsrs) 6.3.2 | FSRS-6 | MIT | Extensive | Used as the reference for an isolated port |

**Decision:** `domain/scheduling/Fsrs6Scheduler.kt` is a line-faithful port of py-fsrs 6.3.2 (license kept in `third_party/licenses/MIT-py-fsrs.txt`). It has no Android dependency.

**Verification:**

- `third_party/generate_fsrs_vectors.py` runs the real py-fsrs and writes 120 randomized review histories (1,705 reviews) to `app/src/test/resources/fsrs_reference_vectors.json`. `Fsrs6SchedulerTest` replays them and compares state, step, stability, difficulty and due time.
- py-fsrs's own published expectations are also asserted: the interval history `[0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21]` and the memo-state test (stability 53.62691, difficulty 6.3574867).

## Scheduling model

- Default FSRS-6 weights (21 parameters), desired retention 0.9, learning steps 1 and 10 minutes, relearning step 10 minutes, maximum interval 36,500 days.
- **New** maps onto py-fsrs's initial Learning state at step 0 with no memory state. The app keeps New as a distinct stored state so it can be counted and filtered.
- **Python semantics that are preserved on purpose:** whole elapsed days use floor division, and interval rounding is half-to-even.
- **Fuzz differs in one way.** py-fsrs draws from a global RNG. Here the draw is seeded from the word ID and repetition count. This is what makes the interval on each button exactly the interval applied, because preview and review are the same pure computation. The fuzz range formula is unchanged.

```kotlin
interface SpacedRepetitionScheduler {
    val algorithmVersion: String
    fun review(memory: CardMemory, rating: Rating, nowMillis: Long): ReviewOutcome
    fun preview(memory: CardMemory, nowMillis: Long): Map<Rating, ReviewOutcome>
    fun retrievability(memory: CardMemory, nowMillis: Long): Double
}
```

## Persistence

Database version 7. Vocabulary content and learning progress are separate tables.

```
words 1 ──── 1 card_scheduling     current state; deleted with the word
words 1 ──── n review_logs         immutable; wordId becomes NULL when the word is deleted
```

**`words`:** `id`, `term`, `translation`, `sourceLang` (language of the term), `targetLang` (language of the translation), `termKey`, `meanings` (JSON with part of speech, definitions, examples, synonyms), `translatedDefinitions` (JSON), `phonetic`, `audioUrl`, `hint`, `createdAt`, `updatedAt`. A unique index on `(termKey, sourceLang, targetLang)` prevents duplicates per language pair.

**`card_scheduling`:** `wordId` (primary key, foreign key with cascade), `state`, `step`, `stability`, `difficulty`, `dueAt`, `lastReviewAt`, `reps`, `lapses`, `schedulerVersion`. Indexed on `dueAt` and `state`.

**`review_logs`:** `id`, `wordId` (foreign key, set null), `rating`, `reviewedAt`, `learningDay`, `elapsedMillis`, `durationMillis`, `previousDueAt`, `newDueAt`, `intervalMillis`, previous and new `state`, `step`, `stability` and `difficulty`, `dailyNewGoal`, `algorithmVersion`. Indexed on `wordId` and `learningDay`.

History survives word deletion so past activity stays correct. All timestamps are UTC epoch milliseconds. `learningDay` is the local date at review time (rollover 04:00), stored as an epoch day so that travel cannot rewrite history.

### Writing a review

`StudyDao.recordReview` is a single `@Transaction`: read the current card, compute the outcome, update `card_scheduling`, insert the log. The scheduler runs inside the transaction against the row as stored, so a double tap cannot apply a stale preview: the ViewModel passes the card's `reps`, and a mismatch is ignored.

### Migration 6 → 7

Implemented in Kotlin (`Migration6To7`) because it must parse JSON and handle non-ASCII case folding:

1. **Normalize direction.** The old `english` column always held the headline word. It becomes `term`, and `turkish` becomes `translation`. Where the old `sourceLang` was not English, the two language columns swap and each stored definition's primary and secondary text swap too, so that primary is always the term's language.
2. **Merge duplicates** by `(termKey, sourceLang, targetLang)`, keeping the richest row and all distinct translations.
3. **Create a New card** for every word, due immediately, so existing vocabulary enters the learning system.
4. **Create** `review_logs` and indexes.

Versions 1 to 5 predate any exported schema. Those versions were already destroyed by the old fallback when moving to 6, and that explicit fallback (`fallbackToDestructiveMigrationFrom(1..5)`) stays. From 6 onward there is no destructive path.

## Review queue

`StudyQueuePolicy` is pure and deterministic. Input: candidate cards, `now`, new cards already started today, daily goal. Output: the ordered queue.

1. Review-state cards with `dueAt <= now`, oldest due first.
2. Learning and relearning cards with `dueAt <= now`, oldest due first.
3. New cards, oldest first, limited to `max(0, goal - startedToday)`.
4. If nothing else remains, learning and relearning cards due within 20 minutes, soonest first. This is what makes same-day steps complete inside one session.

Ties break on word ID. The queue is recomputed from the database after each rating, so exit and resume need no saved session.

## Time

`java.time` through core library desugaring (minSdk 24). `LearningDay` computes `(instant - 4h)` in the current zone. All "today" logic flows through it, and `java.time.Clock` is injected for tests.

## Reminders

```
Settings change ─┐
App start ───────┼→ ReminderScheduler.sync() ─ unique one-time work, delay to next local HH:mm
Time/zone change ┘                                        │
                                                          ▼
                                                   ReminderWorker
                       disabled → stop │ no permission or nothing to do → reschedule silently
                       already sent this learning day → reschedule silently
                       otherwise → notify with real count, tap opens Study, reschedule
```

- **WorkManager with a self-rescheduling one-time request**, rather than `PeriodicWorkRequest`, which drifts and cannot follow a wall-clock time through DST. Delivery is inexact by design, so no exact-alarm permission is needed.
- `ReminderTime.nextTrigger` is pure and tested across DST, zones and same-minute edges.
- `TIMEZONE_CHANGED` and `TIME_SET` trigger a resync. WorkManager survives reboot by itself.
- Disabling cancels the unique work and the worker also rechecks the preference.
- Android 13+: the app explains first, then requests `POST_NOTIFICATIONS`. If denied, the app stays fully usable and Settings explains how to allow it.
