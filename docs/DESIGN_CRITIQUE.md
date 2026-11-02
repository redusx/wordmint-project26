# Design critique of the running build

Everything here was checked on a Pixel 9 Pro emulator (API 35) from real screenshots, in light and dark themes, in English and Turkish, and at 360 dp width with 130% text. Two passes were made, and the issues below were fixed unless marked otherwise.

## What works

- **One screenshot is enough to recognize it.** The graduated tube on Today, the dictionary-style type and the verdigris palette are not borrowed from a template.
- **Glass is selective.** It appears on the navigation bar, list headers and study dock. The flashcard is opaque and calm.
- **Dark mode is a separate design.** Deep spruce and a lit liquid, not an inversion.

## Issues found and fixed

| Issue | Evidence | Fix |
|---|---|---|
| Outgoing flashcard briefly showed the next card's content | Lint error on `AnimatedContent` | The transition now renders the state it was given |
| Scrolling text passed under the status bar | Progress screenshot | Status inset moved outside the scroll area |
| Lists collided with floating titles | Words screenshot | Title moved inside a full-bleed glass header |
| Fixed header height would hide content at large text | Layout review | Header height is measured |
| Two screens of text overlapped during tab changes | Words screenshot mid-transition | The old screen leaves before the new one fades in |
| Copper and bronze looked identical under deuteranopia (ΔE 0.4) | Palette validator | Marks use brighter brass, worst pair now ΔE 20 |
| Muted text fell to 4.4:1 at the bottom of the canvas | Contrast calculation | Darkened text and lightened the ramp |
| Darker bars meant both "goal reached" and "selected" | Progress screenshot | Selection is now a mark on the baseline |
| Selection shown by color alone on text tabs | Settings screenshot | Shared `TextTab` with an underline |
| "In 24 h" instead of "In 1 d" | Session summary | Units are chosen after rounding |
| Fresh machine translation shown over the user's saved one | Dictionary, `resilient` | The saved translation is shown, read-only |
| Consistency grid ignored weekdays | Progress screenshot | Rows are now weekdays, Monday first |
| Language rows did not look tappable | Onboarding and Settings | Added a chevron |
| Bottom sheet showed canvas below its rounded bottom corners | Edit word screenshot | Sheets are rounded on top only |
| Percent written as 85% in Turkish | Turkish screenshot | Locale-aware formatting gives %85 |
| Old indigo launcher icon | Splash | New tube icon, adaptive and legacy |

## Known limits

- Cold start and first translation are slow on the emulator with a debug build (about 20 seconds). Most of that is bytecode verification, ML Kit's model load and emulator speed. Release performance on a real phone was not measured.
- Parts of speech stay in English because the dictionary source is English.
- Arabic and CJK terms use the system font, which was not reviewed visually.
