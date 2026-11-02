# WordMint

A personal language-learning system for Android: look up a word, save it, and let spaced repetition decide when you see it again. Everything is stored on your device. There is no account and no backend.

## What it does

- **Today** tells you what to study now, using a glass tube that fills with real progress.
- **Study** is a focused flashcard session with Again, Hard, Good and Easy. Each button shows the exact interval it will apply.
- **Dictionary** combines definitions, IPA, examples and synonyms with on-device translation for 59 languages. Pronunciation uses a recorded voice when one exists and the device's speech engine otherwise.
- **Words** lets you search, filter by learning status, sort, edit and delete.
- **Progress** shows activity, consistency, self-rated recall and upcoming reviews, all computed from stored data.
- **Reminders** arrive at most once a day, and only when words are waiting.

## How it is built

| Area | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, the custom Patina design system |
| Architecture | MVVM with a pure Kotlin domain layer and manual dependency wiring in `AppContainer` |
| Scheduling | FSRS-6, ported from py-fsrs 6.3.2 and verified against it |
| Storage | Room, with exported schemas and tested migrations |
| Background | WorkManager |
| Translation | Google ML Kit, on device |
| Dictionary | freedictionaryapi.com through Retrofit |

## Documentation

| Document | Contents |
|---|---|
| [Product spec](docs/PRODUCT_SPEC.md) | Screens, journeys, metric definitions |
| [Learning architecture](docs/LEARNING_ARCHITECTURE.md) | FSRS choice, data model, migration, queue, reminders |
| [Design system](docs/DESIGN_SYSTEM.md) | Color, type, glass, motion, components |
| [Design rules](DESIGN_RULES.md) | Read before changing any UI |
| [Audit](docs/PRODUCT_UX_AUDIT.md) | The state of the app before version 2.0 |
| [Implementation plan](docs/IMPLEMENTATION_PLAN.md) | Stages and dependency decisions |

## Building

Requires Android Studio with SDK 35 and JDK 17. Minimum supported Android version is 7.0 (API 24).

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:connectedDebugAndroidTest
```

Language packs are about 30 MB each and download only when you ask for them.

## Third-party licenses

Fonts (SIL OFL) and the py-fsrs reference (MIT) are in [third_party/licenses](third_party/licenses).
