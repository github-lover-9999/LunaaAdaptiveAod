# Extractable components

The existing v1.2 code has no reusable UI component classes; the entire settings screen is assembled inside `SettingsActivity`. For v1.3 the implementation should introduce small private builders rather than external dependencies.

## SettingsCard
- Source target: `SettingsActivity.java` private builder
- Category: basic
- Description: Rounded dark surface grouping related controls.
- Extractable props: title, subtitle, content view
- Hardcoded: native platform widgets, dark AMOLED tokens

## SegmentedModeControl
- Source target: `SettingsActivity.java` private builder
- Category: basic
- Description: Two-option Automatic / Manual selector.
- Extractable props: selected mode
- Hardcoded: labels and accent treatment

## LabeledSlider
- Source target: `SettingsActivity.java` private builder
- Category: basic
- Description: Label + current value + SeekBar + helper copy.
- Extractable props: label, value, progress, helper
