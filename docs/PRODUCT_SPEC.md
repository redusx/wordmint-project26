# WordMint 2.0 product specification

## Purpose

Help a person find a foreign word, save it, and actually remember it. The saved list is not the product; the daily review is.

## Principles

1. **One question per screen.** Today answers "what now?". Words answers "what do I have?". Progress answers "is it working?".
2. **Local-first.** No account and no backend. Vocabulary and history live in Room on the device. The network is used only for dictionary lookups, language packs and optional audio.
3. **Honest numbers.** Everything shown is computed from stored data. Ratings are labeled as self-rated, never as scores.
4. **The algorithm decides.** Users rate recall; they never set dates.

## Information architecture

```
First run → Onboarding (languages, daily goal, reminder)

Floating glass navigation
├── Today       what to do now → Study session (full screen, no nav) → Session summary
├── Dictionary  look up, translate, hear, save
├── Words       search, filter, sort → Word details → Edit, delete
└── Progress    activity, states, recall, history

Today header → Settings
Reminder notification → Study session
```

Four destinations, because lookup and review are the two recurring jobs and each deserves one tap. Settings is rare, so it stays out of the bar.

## Screen inventory

| Screen | Job | States |
|---|---|---|
| Onboarding | Language pair, daily new-word goal, optional reminder with explained permission | Step 1 to 3, pack downloading, permission denied |
| Today | Say what to do next and start it | Loading, no words yet, work waiting, in progress, all clear with next-due time |
| Study | Recall, reveal, rate | Loading, question, answer, saving, nothing to review, summary |
| Dictionary | Look up a word and save it | Idle, packs missing, downloading, loading, result, no dictionary entry, offline, already saved, saved |
| Words | Manage vocabulary | Loading, empty library, no matches |
| Word details | See content, schedule and history; edit and delete | Loading, not found (deleted) |
| Progress | Activity, distribution, recall, consistency, history | Loading, no reviews yet, too few reviews for recall |
| Settings | Languages, daily goal, reminder, appearance | Permission denied, pack states |

## User journeys

**First run.** Pick the language being learned and the language to translate into, and packs download in the background. Choose a daily new-word goal. See why a reminder helps, then choose to turn it on; only then does the system permission prompt appear. Today shows "Look up your first word".

**Save a word.** Dictionary → type → translation, IPA, definitions and examples appear → correct the translation if wanted → Save. The action becomes "In your words". The word is immediately a new card.

**Daily review.** Notification or Today → Study → see the word, try to recall, reveal, hear it, rate it. Each rating is written immediately. Leaving is safe at any time; Today then says "Continue". Summary shows words reviewed, time spent and self-rated breakdown.

**Repair a mistake.** Words → search → details → edit the translation, or delete with confirmation. Scheduling is kept on edit.

**Check progress.** Progress → reviews per day (7 or 30 days), consistency, state distribution, recall from reviews of older cards, recent history.

## Study behavior

- **Queue order:** due reviews, then due learning and relearning cards, then new cards within today's allowance. Learning cards due within 20 minutes are shown at the end instead of ending the session early.
- **Daily new-word goal** limits only new cards. Due cards are never hidden by it.
- **Day boundary** is 04:00 local time, so a late-night session belongs to the day the user feels it belongs to.
- **Scope:** the queue spans every saved word, including words from an older language pair.
- **Rating buttons** show the interval the scheduler will actually apply, computed by the same call that applies it.

## Status vocabulary

| Status | Meaning |
|---|---|
| New | Never reviewed |
| Learning | In learning or relearning steps, or memory stability under 21 days |
| Mastered | In review with stability of 21 days or more |
| Due | Not new, and its due time has passed (a filter that overlaps Learning and Mastered) |

## Metrics and how they are computed

| Metric | Definition |
|---|---|
| Reviews per day | Count of review logs by the learning day stored when each review happened |
| Streak | Consecutive learning days with at least one review, ending today or yesterday |
| Goal met | New cards started that day reached the goal in force at the time, stored on each log |
| Self-rated recall | Of the last 30 days' reviews of cards in Review state, the share not rated Again. Shown only with at least 10 such reviews |
| State distribution | Current status of every saved word |

## Out of scope

Accounts, cloud sync, leaderboards, XP, gamified mascots, sharing, decks, AI-generated content.
