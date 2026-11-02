# WordMint design rules

Read this before changing any UI. Full tokens are in `docs/DESIGN_SYSTEM.md`.

## Identity

- New words are copper and mastered words are verdigris. That is data, not decoration.
- Terms, translations, definitions and IPA are set in Gentium Book Plus. Controls and numbers are set in Instrument Sans.
- Words are typeset like dictionary entries: list rows with hairlines, not cards.
- The glass tube is the signature. It always shows a real value, and nothing else in the app competes with it.

## Glass

- Glass goes only on things that float above moving content: navigation, study dock, list header, save dock.
- Never put glass behind text a person reads for meaning. Flashcards, rows, charts and dialogs are opaque.
- Always use `GlassSurface`. Never write ad hoc translucency in a screen.
- Below API 31 glass must stay legible with no blur.

## Do not

- Purple, pink or multi-hue gradients, or gradients used as decoration. The canvas ramp is the only background gradient.
- Cards nested inside cards. Identical cards repeated down a screen.
- Pills as a default container. The capsule shape belongs to the navigation bar and the tube.
- ALL-CAPS labels, eyebrow labels above headings, or text joined by middle dots.
- Accenting one word of a headline.
- Numbered markers unless the content is truly a sequence. Dictionary senses qualify.
- Decorative icons. Emoji used as icons.
- Infinite or idle animation, shimmer, floating decoration.
- Fake data, placeholder statistics, sample charts. If a number cannot be computed from stored data, it does not appear.
- Marketing copy such as "Unlock your potential". Mascots, confetti, XP, or cartoon rewards.
- Hardcoded colors, sizes, radii or user-facing strings in screens. Use `WordMintTheme`, `Spacing`, `Radii` and string resources.
- Hardcoded review intervals. Every interval comes from `SpacedRepetitionScheduler`.

## Language

- Name things as users would: "words", "review", "reminder". Never "card state" or "stability" without plain wording.
- Button text says what happens, and the name stays the same through the flow. "Save word" leads to "In your words".
- Ratings are self-assessment. Say "self-rated". Never call them scores or correct answers.
- Errors say what happened and what to do. They do not apologize.
- Every string exists in both `values` (English) and `values-tr` (Turkish).

## Interaction

- Every visible control works. Do not ship a control without behavior.
- Every rating is saved at once, so leaving a session never needs a warning.
- Swipes never replace buttons for rating a card.
- Destructive actions confirm first and name what will be lost.
- Touch targets are at least 48 dp, with semantics on every custom control.
- Motion answers an action or a changed value.

## Before finishing UI work

Check the real app in light and dark mode, and take screenshots. Ask whether this screen could be mistaken for a template. If so, fix it.
