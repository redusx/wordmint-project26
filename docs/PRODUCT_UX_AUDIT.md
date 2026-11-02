# WordMint product and UX audit

Audit of the repository as it stood before the 2.0 work (`main` at `2338428`, plus the working-tree change `agp = "8.9.0"`, which was kept). Everything below comes from reading the code and running the build, not from the README.

## What the app actually is

A two-screen vocabulary notebook. Package and application ID are `com.example.wordstore`, the name was changed to WordMint only in `strings.xml`.

| Area | What exists | Evidence |
|---|---|---|
| Screens | `home` (word list plus detail dialog) and `add_word` (translate and save) | `MainActivity.kt` |
| Learning | None. No review, no scheduling, no goals, no statistics. "Statistics" on Home is the word count | `HomeScreen.kt` |
| Persistence | Room v6 with one `words` table | `AppDatabase.kt`, `Word.kt` |
| Dictionary | freedictionaryapi.com v1, English only | `DictionaryApi.kt` |
| Translation | ML Kit on-device, 10 of its 59 languages exposed | `DictionaryViewModel.kt` |
| Pronunciation | Streams `ssl.gstatic.com/.../<word>--_gb_1.mp3` | `PronunciationPlayer.kt` |
| Notifications | Model-download progress only | `ModelDownloadNotificationManager.kt` |
| Tests | Template tests only | `ExampleUnitTest.kt` |

## Stack, verified

Kotlin 2.0.21, AGP 8.9.0 (local change; 8.10.1 on GitHub), Gradle 8.11.1, Compose BOM 2024.09.00, Material 3, Room 2.6.1 through kapt, Retrofit 2.9 with Gson, Navigation Compose 2.7.7, ML Kit Translate 17.0.2, minSdk 24, target 35. The baseline `assembleDebug` passed. The README's "LiveData and StateFlow" is half true (LiveData only), and "60+ languages" is false in the UI.

## Defects that affect users

| # | Severity | Problem | Where |
|---|---|---|---|
| 1 | Critical | `fallbackToDestructiveMigration()` wipes all vocabulary on any schema bump. `exportSchema = false`, so there is no schema history either | `AppDatabase.kt` |
| 2 | High | Duplicates are unconstrained: `REPLACE` on an auto-generated key never conflicts, so saving a word twice stores it twice | `WordDao.kt` |
| 3 | High | `english` and `turkish` columns do not mean what they say. For fr→de, `english` holds German and `turkish` holds French | `saveWord` in `AddWordScreen.kt` |
| 4 | High | Pronunciation fails for many words. `resilient` returns HTTP 404 from the audio host while `book` returns 200. It never works for non-English terms | `getPronunciationUrl` |
| 5 | High | Light mode is broken: `AddWordScreen`, dialogs and most components hard-code dark glass colors and white text | `GlassColors` usages |
| 6 | Medium | `POST_NOTIFICATIONS` is declared and never requested, so download notifications silently vanish on Android 13+ | Manifest |
| 7 | Medium | Phrase lookup truncates to the first word: "take off" looks up "take" | `DictionaryRepository.kt` |
| 8 | Medium | An unknown word returns HTTP 200 with empty `entries`, which is treated as success with nothing to show | `DictionaryRepository.kt` |
| 9 | Medium | Entry-level `synonyms` and `antonyms` from the API are discarded; only sense-level ones are read and those are usually empty | `DictionaryModelsV1.kt` |
| 10 | Medium | A new network callback is registered on each visit to Add Word and is never unregistered | `DictionaryViewModel.kt` |
| 11 | Low | Save is fire-and-forget with navigation before the insert finishes, and errors are invisible | `saveWord` |
| 12 | Low | The phonetic section of the detail dialog is commented out, so the stored IPA is never shown | `HomeScreen.kt` |

## UX problems

- No reason to come back: the app has no answer to "what should I do today?"
- Words are saved and then forgotten, which is the failure the product should prevent.
- Home repeats one large glass card per word. Glass is applied to every element, so nothing has emphasis.
- Pure gray palette plus an indigo launcher icon and indigo window background: the brand does not exist.
- Destructive delete on a list row has no confirmation.
- The translate flow is two text fields and a button; dictionary results come after the fold, and the save button sits below all of them.
- Search, filter and sort are absent.
- Copy is hard-coded in Turkish inside Kotlin while `strings.xml` holds an unused parallel set, so the app cannot be localized.

## Architecture and technical debt

- `HomeScreen.kt` and `AddWordScreen.kt` are about 1,300 lines each and mix UI, persistence mapping and connectivity checks.
- `DictionaryRepository` builds a legacy DTO shape "for compatibility" and maps through `englishWord` and `turkishTranslation` with a five-branch `when`.
- `TranslationManager` keeps two unused translators and a whole unused EN/TR bulk-download path.
- Unused build surface: `viewBinding`, `activity_main.xml`, ConstraintLayout, Material XML, Moshi, Jsoup, and a `HUGGING_FACE_API_KEY` build field nothing reads.
- `runtime-livedata` is pinned to 1.8.1 while the BOM provides Compose 1.7, a silent version mismatch.
- Theme tokens exist in `Dimens.kt` and `Theme.kt` but screens bypass them with literals.
- Nine `build_log*.txt` files are committed.

## Risks to existing behavior during the rewrite

| Risk | Mitigation |
|---|---|
| Losing saved words | Real `6 → 7` migration, `6.json` exported from the unmodified code, migration tests, and an on-device upgrade test from the baseline APK |
| Legacy duplicates break a unique index | Merge them in the migration, keeping every distinct translation |
| Legacy rows with swapped meaning | Normalize in the migration so the learned term is always the front of the card |
| Existing language prefs | Keep the `word_store_prefs` file and its `last_source_lang` and `last_target_lang` keys |
| Translation and dictionary regressions | Keep the same API and ML Kit, with repository tests behind fakes and an on-device check |
| Applying AGP or dependency changes on top of the user's local edit | Do not touch `agp`. Add only what the features require |
