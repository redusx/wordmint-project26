# WordMint design system: Patina

## Concept

A word goes through the same change a minted coin does. It starts bright copper and, with time and handling, takes on verdigris. That gives the product a color story that is also data: **new words are copper, mastered words are verdigris**, and bronze sits between them.

Three references hold the system together:

| Reference | Used for |
|---|---|
| The dictionary page | Terms, IPA, parts of speech and senses are typeset, not boxed |
| Laboratory glass | The signature element: a graduated glass tube whose liquid level is real progress |
| Liquid glass | Translucent layers only where something floats above moving content |

## The signature element: the tube

`GlassTube` is the one place the app spends its boldness. A rim-lit glass vessel with liquid and a curved meniscus.

| Where | Form | What the level means |
|---|---|---|
| Today | Tall, graduated | Today's completed share of today's work |
| Study | Slim, horizontal | Session position |
| Words | Tiny, vertical | Current chance of recall for that word, from FSRS |
| Word details | Horizontal | Same recall chance |

The liquid moves only when its value changes. It never idles or ripples.

## Color

Tokens live in `ui/theme/Color.kt`. Screens read `WordMintTheme.colors`.

| Token | Light | Dark | Use |
|---|---|---|---|
| `canvas` | `#EDF1EF` | `#0D1A18` | Screen background |
| `canvasDeep` | `#E4EBE7` | `#091312` | Lower end of the canvas ramp |
| `surface` | `#FBFCFB` | `#162523` | Flashcard, dialogs, sheets (opaque) |
| `surfaceSunken` | `#E3E9E6` | `#10201D` | Input wells, chart tracks |
| `ink` | `#10211E` | `#E9F1EE` | Primary text |
| `inkSecondary` | `#3F514B` | `#AFC0BA` | Supporting text |
| `inkMuted` | `#586962` | `#8FA199` | Tertiary text, at least 4.5:1 on every surface |
| `hairline` | ink 12% | white 10% | Dividers |
| `primary` | `#0E6A5B` | `#63CDB8` | Main action, selection |
| `onPrimary` | `#FFFFFF` | `#06211C` | Text on primary |
| `copper` | `#A2522A` | `#E5A67F` | New words, due counts |
| `bronze` | `#7C6C1F` | `#CDB866` | Learning words |
| `verdigris` | `#14796A` | `#63CDB8` | Mastered words, the liquid |
| `danger` | `#A8321F` | `#F0907C` | Destructive actions, errors |
| `statusNew` | `#A2522A` | `#D9804A` | Marks for new words |
| `statusLearning` | `#C9A227` | `#E8CF73` | Marks for learning words |
| `statusMastered` | `#14796A` | `#3DB39D` | Marks for mastered words |

**Text and marks use different tokens.** `copper`, `bronze` and `verdigris` are text colors that meet 4.5:1. The `status*` tokens are for tubes and charts only. They were checked with a color-vision validator: copper and bronze were indistinguishable under deuteranopia (ΔE 0.4), so `statusLearning` is lighter brass (worst pair now ΔE 20). Brass has under 3:1 contrast on the light canvas, so status marks always come with a visible label and count.

**Ratings** get color only on the interval text, so the dock stays calm: Again `danger`, Hard `bronze`, Good `verdigris`, Easy `sea` (`#1C6488`, dark `#7FC3E0`).

**Light and dark are separately chosen.** Dark mode is deep spruce water, not an inverted light theme. Material You dynamic color is intentionally off, because the palette carries meaning.

## Type

| Family | Role | Why |
|---|---|---|
| Gentium Book Plus | Terms, translations, definitions, headlines, IPA | Scholarly, built for linguistics. Complete IPA, Cyrillic, Greek and Turkish coverage, so phonetics never fall back to a mismatched font |
| Instrument Sans | Controls, labels, numbers | Crisp and narrow, stays out of the way of the serif |

Both are bundled under the SIL Open Font License (`third_party/licenses`). Arabic and CJK terms use the system fallback.

| Style | Family | Size / line | Use |
|---|---|---|---|
| `displayLarge` | Gentium | 44 / 50 | Flashcard term |
| `displayMedium` | Gentium | 34 / 40 | Today headline, word details term, answer translation |
| `headlineMedium` | Gentium | 28 / 34 | Screen titles |
| `titleLarge` | Gentium | 22 / 28 | Section titles |
| `titleMedium` | Gentium | 20 / 26 | Term in a list |
| `bodyLarge` | Gentium | 17 / 26 | Definitions, examples |
| `bodyMedium` | Instrument | 15 / 22 | UI text |
| `bodySmall` | Instrument | 13 / 18 | Supporting UI text |
| `labelLarge` | Instrument SemiBold | 15 / 20 | Buttons |
| `labelMedium` | Instrument Medium | 13 / 18 | Row labels, tabs |
| `labelSmall` | Instrument Medium | 11 / 14 | Navigation labels, chart axes |
| `phonetic` | Gentium | 17 / 24 | IPA |
| `partOfSpeech` | Gentium Italic | 16 / 22 | Part of speech, example sentences |

Gentium has a small x-height, so its sizes run a step larger than usual.

## Spacing

A 4-point scale in `Spacing`: `xxs 2`, `xs 4`, `s 8`, `m 12`, `l 16`, `xl 24`, `xxl 32`, `xxxl 48`, `huge 64`. The screen gutter is `xl` on Today and `l` on dense lists.

## Shape

Radius follows hierarchy, so it is not the same everywhere.

| Token | Radius | Use |
|---|---|---|
| `Radii.s` | 10 | Input wells, small controls |
| `Radii.m` | 16 | Buttons |
| `Radii.l` | 24 | Docks, floating glass bars |
| `Radii.header` | 24, bottom only | Glass header running off the top of the screen |
| `Radii.xl` | 32 | Flashcard, sheets |
| Full | Capsule | Navigation bar and tube only |
| None | 0 | List rows, which are separated by hairlines |

## Depth and glass

Three levels only:

1. **Canvas.** A slow vertical tonal ramp, one hue.
2. **Paper.** Opaque `surface`. All reading happens here or on the canvas. No blur behind text a person must read for meaning.
3. **Glass.** `Modifier.glass` or `GlassSurface`: backdrop blur with a tint, a top-lit rim and a soft contact shadow.

**Glass is allowed on:** the navigation bar, the study rating dock, the word list search and filter header, the dictionary save dock.

**Glass is not allowed on:** flashcards, list rows, chart containers, dialogs.

On API 31+ Haze provides real backdrop blur. Below that the tint rises to about 90% opacity, so legibility holds with no blur at all.

## Iconography

A custom 24-unit outline set in `ui/theme/Icons.kt`: 1.6 stroke, round caps and joins. Every icon is functional and paired with a text label or content description. No emoji.

## Motion

Tokens in `ui/theme/Motion.kt`.

| Token | Value | Use |
|---|---|---|
| `Quick` | 140 ms | Press and small state changes |
| `Standard` | 280 ms | Screen transitions, content reveal |
| `Liquid` | Spring, low stiffness, soft bounce | Tube level, navigation lens |
| `Emphasized` | Cubic (0.2, 0, 0, 1) | Entering content |

| Interaction | Motion |
|---|---|
| Reveal answer | Term shrinks and rises, answer unfolds below it, dock swaps controls |
| Rate card | Card exits left, next enters from the right, tube rises, a one-line result confirms the interval |
| Change tab | Lens slides between tabs |
| Progress change | Liquid settles to the new level |

No infinite animations and no shimmer. Compose honors the system animator scale, so "Remove animations" in Android settings disables all of this.

## Components

| Component | File |
|---|---|
| `GlassSurface`, `glassHaze` | `ui/components/Glass.kt` |
| `GlassTube` | `ui/components/GlassTube.kt` |
| `GlassNavigationBar` | `ui/components/GlassNavigationBar.kt` |
| `PrimaryButton`, `SecondaryButton`, `QuietButton`, `IconAction` | `ui/components/Buttons.kt` |
| `ScreenTitle`, `SectionTitle`, `LabelValueRow`, `Hairline`, `MessageBlock` | `ui/components/Layout.kt` |
| `PronounceButton` | `ui/components/PronounceButton.kt` |
| `LanguagePickerSheet` | `ui/components/LanguagePicker.kt` |
| `WordEntryContent` (senses, examples, synonyms) | `ui/components/WordEntry.kt` |

## Component states

Every interactive component defines pressed, disabled and focused states. Disabled lowers contrast but keeps text readable. Touch targets are at least 48 dp, rating buttons at least 72 dp.

## Charts

Bars have 4 dp rounded data ends on a shared baseline, a recessive axis and direct labels on the selected bar only. Text uses ink tokens, never the series color. The state distribution uses the `status*` tokens with a legend and counts, so identity is never color alone.
