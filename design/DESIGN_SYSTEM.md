# Design System

This document defines PageHarbor's initial visual identity foundation. It is intentionally small and should evolve only when implementation needs are clear.

## Color Tokens

Initial palette:

- Primary blue: `#245FC7`.
- Primary dark: `#173E85`.
- Accent blue: `#8CB9F1`.
- Dark slate: `#172033`.
- Light surface: `#F8FAFD`.
- White: `#FFFFFF`.
- Error: `#B3261E`.
- Dark background: `#101827`.
- Dark surface: `#172033`.
- Dark text: `#E6ECF5`.

Light theme guidance:

- Use `#F8FAFD` for the app background.
- Use `#172033` for primary text.
- Use `#245FC7` for primary actions and key brand accents.
- Use `#173E85` for stronger secondary emphasis.
- Use `#B3261E` for errors.

Dark theme guidance:

- Use `#101827` for the app background.
- Use `#172033` for surfaces.
- Use `#E6ECF5` for primary text.
- Use `#8CB9F1` for primary actions and key brand accents.
- Use `#F2B8B5` for errors.

WCAG AA notes:

- `#172033` on `#F8FAFD` passes AA for normal text.
- `#245FC7` on `#FFFFFF` passes AA for normal text.
- `#FFFFFF` on `#245FC7` passes AA for button text.
- `#E6ECF5` on `#101827` passes AA for normal text.
- `#8CB9F1` on `#101827` passes AA for normal text.

Accessibility takes precedence over strict color matching.

## Typography

Use Android system / Material 3 typography. Do not add a custom font dependency.

Hierarchy:

- App title: `headlineLarge`.
- Headline: `headlineMedium`.
- Body: `bodyLarge` and `bodyMedium`.
- Supporting label: `labelLarge`.
- Button: Material 3 button text.
- Status label: `labelLarge`, debug/development only where applicable.

## Shape

Use a restrained Material 3 shape scale:

- Buttons: medium rounding, enough to feel approachable without becoming toy-like.
- Dialogs: large rounding.
- Cards: small to medium rounding when cards are genuinely needed.
- App icon geometry: rounded adaptive icon background with a simple upright page and soft harbor curve.

Avoid overly rounded UI and decorative nested card layouts.

## Spacing

Use a 4 dp base grid.

Common spacing values:

- 4 dp.
- 8 dp.
- 12 dp.
- 16 dp.
- 24 dp.
- 32 dp.

Use generous whitespace on main screens, especially around the primary action.

## Motion

Keep motion subtle and functional.

- Respect reduced-motion settings where relevant.
- Do not add animation solely for branding.
- Do not add animations as part of this phase.

## Iconography

Use Material icons for normal UI actions where available.

The PageHarbor brand mark is custom. It should represent the product identity and should not be reused as a generic UI icon.

## Action hierarchy

Use filled buttons for Scan document, Save PDF, and Copy text; outlined buttons for Save Searchable PDF, Share PDF, Export Pages, Recognize Text, and Scan Again; and text buttons for Privacy, About, clear, and discard actions. Searchable-PDF progress should name the current local stage—recognizing text, generating PDF, or saving—without exposing document content or file paths. Normal scan UI uses product copy such as “Scan complete” and page-ready summaries, never scanner diagnostics.
