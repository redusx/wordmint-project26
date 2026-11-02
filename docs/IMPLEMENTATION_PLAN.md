# Implementation plan

Each stage ends with a build. Status is updated as stages complete.

| Stage | Work | Verified by |
|---|---|---|
| 1. Audit | `docs/PRODUCT_UX_AUDIT.md`, baseline APK kept for the upgrade test | Baseline build |
| 2. Design | `docs/PRODUCT_SPEC.md`, `docs/LEARNING_ARCHITECTURE.md`, this plan | Review against the brief |
| 3. Design system | `docs/DESIGN_SYSTEM.md`, `DESIGN_RULES.md`, theme tokens, fonts, icons, glass, core components | Build, on-device screenshots |
| 4. Learning core | `Fsrs6Scheduler`, queue policy, Room v7, `Migration6To7`, repositories, settings | Unit tests with reference vectors, instrumented migration tests |
| 5. UI | Onboarding, Today, Study, Dictionary, Words, Word details, Progress, Settings | ViewModel tests, Compose tests, on-device checks |
| 6. Reminders | Worker, scheduler, permission flow, notification deep link | Unit tests, worker test, on-device notification |
| 7. QA | Full test run, lint, release build, baseline-to-new upgrade, light and dark screenshots, second design critique | Recorded in the final report |

## Dependency changes

**Added:** `work-runtime-ktx` (reminders), `lifecycle-runtime-compose` (lifecycle-aware state), `desugar_jdk_libs` (`java.time` on API 24), `haze` (real backdrop blur on API 31+, with a tinted fallback below), and test libraries (`coroutines-test`, `room-testing`, `work-testing`, `androidx.test`).

**Removed because unused:** ViewBinding, ConstraintLayout, Material XML, AppCompat, Moshi, Jsoup, LiveData, `HUGGING_FACE_API_KEY` field, `layout/activity_main.xml`.

**Deliberately unchanged:** AGP, Kotlin, Compose BOM, Room, Retrofit, Navigation, ML Kit, minSdk, targetSdk, `applicationId`, `namespace`, package names.

## Status

All seven stages are implemented and were verified on the final code: 76 unit tests and 43 instrumented tests pass, lint reports no errors, and debug and unsigned release builds succeed.

## Before a Play release

1. Replace `com.example.wordstore`. Google Play does not accept the `com.example` prefix, and the ID cannot change after publishing.
2. Add release signing.
3. Decide on R8. Minification is off, so the release APK is about 71 MB. Enabling it requires keep rules for the Gson models stored in Room.
4. Test on a real phone and on an API 24 to 30 device, where glass falls back to a tint.
