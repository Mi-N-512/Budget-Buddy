# Changelog

Every offline release has a matching tag in this repository. APKs for the current release are on the [Releases page](https://github.com/Mi-N-512/Budget-Buddy/releases/latest).

## 3.0 Stable: two editions, one offline app

- Added bundled offline OCR in a separate OCR edition, with receipt review, local category suggestions and a detailed record view.
- Added a No OCR edition that installs side by side with its own package and leaves the recognition model out.
- Added the full interactive tutorial across the real Home, Budget, Transaction, OCR, Analytics and Achievement screens.
- Fixed live theme updates and OLED layout shifts, made every screen use the exact selected gauge colours and darkened the neutral yellow so it reads in Light mode.
- Hardened security: no network permission, no exported internal screens, no cleartext traffic and no cloud backup. Every Kotlin class is now documented.

## 2.7: data integrity and flow

- A monthly limit now works without category budgets.
- Totals are rebuilt from saved transactions instead of being adjusted step by step.
- Unfinished transactions are kept only when you choose to keep them.
- Added curated category icons and moved to the `com.budgetbuddy` package.

## 2.6.1: theme lifecycle fix

- Material You is stable, and OLED black is now truly black. Appearance is applied at the right point when a screen opens, and OLED surfaces are kept separate from ordinary dark surfaces.

## 2.6: theme studio

- Added Light, Dark, Material You, OLED black and Custom colours, exact gauge palettes and a gauge needle, with live previews.

## 2.5: safer budgeting

- Fixed the receipt-placeholder crash and counted uncategorised expenses.
- Clarified the maximum budget, added categories, made income categories optional and introduced Material You.

## 2.4: currencies and gauge consistency

- Euro became the default, with 45 searchable offline currencies.
- The gauge and Budster now use one shared spending-status calculation.

## 2.3: receipt reliability

- Rebuilt camera capture, gallery selection, validation and storage cleanup.
- Added locked and unlocked achievement badges.

## 2.2: meet Budster

- Introduced Budster, dark mode and first-run onboarding.
- Fixed navigation, made analytics safe when you go over budget, and moved the camera permission request to the first Home visit.

## 2.1: interface hardening

- Added the launcher icon, better contrast, an immersive layout, consistent navigation, category selectors and safer camera behaviour.

## 2.0: offline reset

- Replaced the online account and database with a local profile, local transactions, receipt attachments, editing and deleting, budget rebuilding and achievements.

## Before 2.0: the online prototype

Budget Buddy began as an online prototype built around an account and a remote database. It had no offline mode, one fixed look, no OCR and no tutorial. It was rebuilt from 2.0 so that it works without a network and keeps your finances on the phone. The online account, remote database and old service configuration were removed completely. That version predates the history in this repository.
