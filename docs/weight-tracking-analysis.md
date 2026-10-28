# Weight Tracking: Analysis and Improvement Proposal

This document reviews Life Metric’s weight tracking against clinical screening guidance (CDC, NIH/NHLBI, WHO) and against the features people already use in dedicated weight apps. It is a product proposal, not a diagnosis tool. Existing capabilities listed in [What Life Metric already does](#what-life-metric-already-does) must stay available.

The bias is the same as the rest of the app: **local-only records, named standards, one obvious next action**. Suggestions that would turn Life Metric into a calorie diary, a smart-scale companion, or a cloud service are called out as later / out of scope.

---

## Why this needs work

The weight area already has a complete log loop: add and edit a reading, a history chart with a dashed goal line, a start-to-target progress card, a BMI gauge, vs-prior arrows, undo delete, and JSON backup. Blood pressure in this app is further along on *averages and protocol*. Weight is still built around **the latest raw scale number**.

That is the opposite of how serious consumer weight software works, and of how clinicians use a home scale.

1. **Each reading is treated as the truth.** Home shows `+1.2 lb vs prior`. The Weight tab paints an up or down arrow on every card. BMI is computed from the newest entry. A morning-to-evening swing of 2–4 lb from food, water, glycogen, and sodium is normal. Happy Scale, Libra, and *The Hacker’s Diet* exist because those daily jumps are mostly noise.
2. **The chart does not show time.** Points are equally spaced by reading index. A two-day gap looks the same as a two-month gap. The Weight tab plots every record; Home plots the last 10. There is no 7 / 30 / 90 day window, and no moving average — even though blood pressure already has a configurable trailing average.
3. **The goal UI assumes loss.** Progress math already handles a gain target, but the copy is always “Target to lose” and “Weight lost.” There is no rate (lb / week), no projected date, and no 10% initial-loss suggestion from NIH. Overshooting the target clamps at 100% with no “maintaining” state.
4. **BMI is a four-color stamp.** CDC and NHLBI split obesity into Class 1 / 2 / 3. The gauge clips at 35, so Class 2 and Class 3 look identical. The hub never says BMI is a **screening** measure, not a diagnosis, and does not mention waist or body composition.

Industry-standard weight software (Happy Scale, Libra, Apple Health, Withings, Fitbit, MyFitnessPal / Lose It) all use the same loop: **weigh at a consistent time → log the number → smooth the trend → compare the trend (not yesterday) to a goal → optionally share a period**. Life Metric covers “log” and a raw line. It does not yet cover the trend.

---

## What Life Metric already does

Inventory of the current weight product. New work should add to this, not replace it.

### Data and rules

- `WeightRecord`: id, date, time, weight (lb, stored to 0.1)
- Multiple readings per day are allowed
- `UserProfile.heightInches`, `weightGoalLb`, `weightGoalStartId`
- Stored only on the device; included in JSON export / import / erase
- Validation on the form: required date and time; numeric weight ≥ 0; rounded with `roundToTenth`
- Suggested new-reading value is the latest weight, or **150.0** if none exist
- Latest reading is the newest date + time, then id (`latestWeight`)
- `weightDelta` is latest minus the previous reading (not a weekly change)
- BMI uses the NHLBI / CDC adult imperial formula: `(703 × lb) / in²`
- Adult categories (`bmiCategory`), four buckets:
  - **Under** — BMI &lt; 18.5
  - **Healthy** — 18.5–24.9
  - **Over** — 25.0–29.9
  - **Obese** — &gt; 29.9
- Goal progress (`weightGoalProgress`) from a chosen start reading to `weightGoalLb`, using the latest weight as current. Percent is clamped 0–100. Gain targets are computed; the UI still labels them as loss.
- Deleting the start reading clears `weightGoalStartId`; undo restores both
- Unit tests cover the BMI example (182.3 lb at 70 in → Over) and goal percent (180 → 176 toward 170 = 40%)

### Weight tab

- Empty state with a path to record the first reading
- Hero: last recorded weight in lb, date and time
- History chart of **all** chronological readings; long-dashed green goal line when a target exists; integer Y ticks (min / mid / max); first / mid / last X date labels
- Weight target section **before** BMI:
  - If no start reading: empty state pointing at the goal editor
  - If no target: empty state pointing at the goal editor
  - Otherwise: start, percent complete, target, progress bar, “Target to lose,” “Weight lost”
- BMI gauge when height is set; otherwise prompt to set height
- Recent list (last 30): value, date/time, up / down / dash vs the previous chronological reading, tap to edit, overflow delete
- Confirm delete that quotes the date and weight; snackbar undo
- FAB: Record weight

### Add / edit form

- Titles Record weight / Edit weight
- Date and time default to now
- Weight field with − / + by 0.1 lb and free-text decimal
- Cancel / Save; inline field errors
- Missing-record screen if the id was deleted

### Home

- Top chip: latest weight and vs-prior delta (or the date if only one reading)
- First-session coach row to record weight
- Dashboard card: latest value, vs-prior, last-10 chart with goal line, compact BMI gauge (or height prompt)
- Log action on the card opens the same add form
- Tapping the section opens the Weight tab

### Profile

- Height (feet + inches), used only for BMI; valid range 3′0″–8′0″
- Weight goal editor: target lb with the same 0.1 stepper; starting-weight dropdown of existing readings (disabled until one exists)
- No weight reminder (blood pressure already has optional local morning / evening reminders)

### Shared chrome

- BMI gauge: four arcs from 12–35 (Under / Healthy / Over / Obese), current needle, optional goal needle
- Health disclaimer: not a medical device; BMI and labels are for personal record-keeping

---

## What is already strong

Keep these. They are differentiators against many free trackers.

| Strength | Why it matters |
|---|---|
| Explicit start reading for the goal | Most calorie apps treat “first ever weigh-in” as the start. Letting the user pick the reading that begins *this* attempt matches how people actually restart after a plateau. |
| 0.1 lb stepper + last-value prefill | Matches consumer scales and makes a daily log a few taps. |
| Multiple readings per day, with time | Apple Health and Withings keep timestamps. Happy Scale discards time and keeps the day’s low. Life Metric can later *derive* a daily representative without losing the raw log. |
| Goal line on the chart | Immediate visual of “where I am going.” |
| Local-only storage, export / import, undo delete | Dedicated weight apps often require an account. The privacy story is a feature. |
| BMI formula matches NHLBI | `703 × lb / in²` is the published US adult conversion. |
| Same hub + form pattern as blood pressure | Logging weight is already faster than logging a workout. |

---

## Standards used for this review

Guidelines agree on **how to interpret weight over time**. They disagree on extra body-composition gadgets. Life Metric should stay CDC/NHLBI-labeled (US-first, already implemented) and borrow the trend and goal patterns that every dedicated weight app shares.

### BMI (what a number means)

BMI = weight (kg) / height (m)². The imperial form Life Metric uses is the NHLBI equivalent.

| Source | Under | Healthy / normal | Overweight | Obesity |
|---|---|---|---|---|
| **CDC adult (20+)** | &lt; 18.5 | 18.5 to &lt; 25 | 25 to &lt; 30 | ≥ 30, then Class 1 (30–&lt;35), Class 2 (35–&lt;40), Class 3 (≥ 40) |
| **NHLBI / WHO international** | &lt; 18.5 | 18.5–24.9 | 25.0–29.9 | Class I 30.0–34.9, II 35.0–39.9, III ≥ 40 |
| **WHO Asian action points** (optional, not CDC default) | — | — | Increased risk at 23 | High risk at 27.5 |

Life Metric implements the **four coarse adult buckets**. It does **not** show obesity classes, Asian action points, or pediatric percentiles. It should not silently switch cutoffs. If a population picker is ever added, CDC/NHLBI adult must remain the default and the labels must name the source.

CDC’s current consumer language: BMI is a **quick screening** measure, not a diagnosis. It does not distinguish fat, muscle, or bone. For an individual it should sit next to blood pressure, waist, and a clinician’s exam — all of which this app already either tracks (BP) or could add as an optional field (waist).

### How a home scale should be used

Happy Scale, Libra, *The Hacker’s Diet* (John Walker), and most clinical “weigh yourself at home” sheets say the same thing:

1. **Same conditions every day.** First thing in the morning, after using the restroom, before breakfast, similar clothing or none.
2. **Weigh often.** Daily is better than weekly because smoothing needs samples. Skipping days is fine; the trend should tolerate gaps.
3. **Do not react to one day.** Water and gut contents move several pounds. The number that matters is the **trend** over 7 days or more.
4. **One representative value per day for the trend.** Happy Scale keeps the day’s **lowest** weight when importing from Apple Health. Life Metric can keep every raw reading and still compute a daily series for the chart.

### Goal-setting (clinical, not diet-industry)

| Source | Recommendation |
|---|---|
| **CDC** | Gradual, steady loss of about **1–2 lb per week** is more likely to last than faster loss. |
| **NIH / NHLBI obesity guideline** | Initial goal about **10% of baseline** over ~6 months, at 1–2 lb/week; then maintain. Prevention of further gain is a valid goal. |
| **NHLBI + CDC waist** | For BMI 25–34.9, waist adds risk information. High-risk cutoffs: **&gt; 40 in (men), &gt; 35 in (women)**. Waist adds little once BMI is ≥ 35. |

Life Metric’s goal is a single target pound value. That is enough if the hub also shows **rate** and whether that rate is in the 1–2 lb/week band. It should not invent a “healthy weight” from BMI and push it as the target.

### What dedicated software actually computes

These are not extra “wellness scores.” They are the numbers Happy Scale, Libra, Withings, and Apple Health Charts treat as the product.

| Metric | Typical method | Why apps show it |
|---|---|---|
| **Daily representative weight** | Lowest, first, or mean of that calendar day | Stops three same-day readings from drawing a scribble |
| **Trend / moving average** | 7-day simple MA, or exponential smoothing with ~7-day time constant (Libra; *Hacker’s Diet*) | The number people compare week to week |
| **Rate** | Trend now vs trend 7 or 28 days ago, expressed as lb/week | Tells you if the plan is working without reacting to Tuesday |
| **Projected date** | Linear or exponential extrapolation of the trend to the goal | Motivating, but must be labeled an estimate |
| **BMI** | From latest *or* from trend weight + height | Screening category; not a daily score |
| **Period change** | 7 / 30 / 90 day delta of the trend | What people screenshot |

Blood pressure in this app already has `trailingMovingAverage`. Weight should reuse that idea, not invent a new math library.

---

## How other products do it

Compared as **consumer weight-tracking software**, not as medical devices or calorie counters.

| Capability | Life Metric | Happy Scale (iOS) | Libra (Android) | Apple Health / Health Connect | Withings / Fitbit / Garmin | MyFitnessPal / Lose It |
|---|---|---|---|---|---|---|
| Manual weight + time | Yes, 0.1 lb | Date; time discarded on Health sync | Date + time | Yes | Auto from scale + manual | Yes |
| Units | lb only | lb / kg / st | lb / kg | Device locale | Device locale | lb / kg |
| Multiple per day | Yes | Imports day’s **lowest** | Yes; trend uses time-aware smoothing | Yes | Yes | Usually one featured daily |
| Raw line chart | Yes, equal-spaced points | Secondary | Secondary | Date-range | Date-range | Date-range |
| Smoothed trend | No | 4 methods (7-day MA, exponential, two advanced) | Exponential, default 7 days | Period charts, no Happy-Scale-style trend | Vendor smoothing | Light smoothing / weekly |
| Goal + % complete | Yes, start reading + target | Yes + projection | Yes + planning | Goal in Fitness / third-party | Yes | Yes, often from calorie plan |
| Gain vs lose copy | Loss labels only | Both | Both | Both | Both | Both |
| Rate (lb/week) | No | Yes | Yes | Sometimes via trends | Yes | Yes |
| BMI | 4-band gauge from latest | Yes | Yes + optional composition | Calculated | Yes + scale composition | Yes |
| Obesity classes | No; gauge clips at 35 | Uncommon | Sometimes | Category text | Sometimes | Sometimes |
| Waist / body fat | No | No (weight-only) | Optional composition | Via other apps | From smart scale | Optional |
| Notes / tags | No | Limited | Time-span events (vacation) | Limited | Notes in some apps | Food context, not scale |
| Reminders | No (BP has them) | Yes | Yes | Yes | Watch / scale nudge | Yes |
| Chart ranges | All / last 10 | Pinch + periods | Dynamic scroll | D / W / M / Y | D / W / M / Y | W / M / Y |
| Time-accurate X axis | No (index) | Yes | Yes | Yes | Yes | Yes |
| Clinician / share | Full-app JSON | Share chart | Share chart | PDF / export | PDF / partner | Premium reports |
| Smart scale / Health Connect | No | Apple Health | Withings + optional cloud | The hub | Native | HealthKit / Health Connect |
| On-device only | Yes | Optional iCloud | Optional Libra Cloud | iCloud Health | Cloud account | Account required |

**Google Fit** can store a weight and draw a basic chart. It is a hub, not a weight product. **Samsung Health** is similar on Galaxy. They matter later if Life Metric ever *exports* a reading; they are not the bar for the in-app log.

**MyFitnessPal and Lose It** win on food, not on scale math. Copy their unit toggle and weekly change, not their diary.

**Withings, Fitbit, Garmin, Renpho, Eufy** win on automatic capture and body-composition fields (fat %, muscle, water). Those numbers are method-dependent and often poorly reproducible. Life Metric should not chase bioimpedance unless a user types an optional field.

**The product to beat for a local weight tab is Libra** (Android) / **Happy Scale** (iOS): daily log, honest trend, goal, projection, no calorie database.

---

## Gap analysis

What a user can do today vs what guidelines and the comparison set expect.

| Job | Life Metric today | Gap |
|---|---|---|
| Log a reading in 10 seconds | Form is short; last value is prefilled | Allows 0 lb and has no upper bound; 150.0 as the empty default is arbitrary |
| See whether this week is better than last | Latest and vs-prior | No 7-day trend, no lb/week, no “vs 7 days ago” |
| Ignore scale noise | Every point and every arrow is raw | No moving average; arrows fire on same-day water swings |
| Read the chart honestly | Line + goal | X is reading index, not calendar time; Weight tab plots unbounded history; Home is last 10 with no control |
| Set a goal that matches intent | Start + target + % | Copy assumes loss; no maintain / gain; no 10% helper; no ETA |
| Understand BMI | Four-band gauge + short label | No “screening, not diagnosis”; no Class 1–3; needle saturates at 35; no source named |
| Weigh under consistent conditions | User must remember | No “how to weigh” sheet; no morning reminder (BP already has the reminder machinery) |
| Explain a high day | Nothing but the number | No note, no “after restaurant / salty meal / late weigh-in” tag |
| Review history | Last 30 rows | Older readings exist in the file but disappear from the UI; no date filter |
| Use kilograms | lb only | Metric users and many scales are kg-first |
| Share a period | Full-app JSON | Not a weight-only CSV/PDF a clinician or coach would want |
| Stay on a schedule | None | No optional local morning reminder |
| Combine with other body measures | Height + weight only | No waist, which NHLBI wants alongside BMI 25–34.9 |

### The vs-prior problem (fix first)

`weightDelta` and the reading-card arrows compare **adjacent raw points**. That is useful as “what did I type last time.” It is misleading as “am I gaining.”

A user who weighs 182.4 in the morning and 184.1 after dinner gets a red up-arrow and a Home chip of `+1.7 lb`, then a green down-arrow the next morning at 182.2. Nothing about fat mass changed.

Fix options, in order of fidelity:

1. **Keep vs-prior on the card as a small, muted glyph**, and put **vs 7-day trend** on the hero and Home chip.
2. **Compute arrows from daily representative weights**, not from intra-day duplicates.
3. Do not color a &lt; 0.5 lb change as success or danger; that is inside most bathroom-scale error.

Do not remove vs-prior. People like seeing the last step. Stop letting it be the headline.

### The chart-spacing problem

`WeightChart` maps `index / lastIndex` to X. Two readings a year apart with one in the middle produce three evenly spaced dots. After a year of daily logs the Weight-tab chart becomes an unreadable ribbon.

Blood pressure already limits the on-screen series (last 10) and has a window control. Weight should do the same: **range chips + time-scaled X** (or daily buckets), and a separate smoothed series.

---

## Design principles for weight

Same CX methods as `docs/cx-improvements.md`, applied to the scale.

| Principle | What it means here |
|---|---|
| **Trend over spike** | Hero and Home lead with the smoothed weight (or “not enough days yet”). Raw latest stays visible. |
| **Educate, do not diagnose** | Keep “not a medical device.” BMI is a named CDC/NHLBI screening category, not “you are obese” as a verdict. |
| **Named standard** | CDC/NHLBI adult BMI unless the user picks another published set. Show the source on the gauge caption. |
| **Recognition over recall** | Prefill last weight (already done). Offer “same time as last morning reading.” |
| **Progressive disclosure** | Hero stays one number + vs-trend. Classes, waist, notes, and projection sit one tap deeper. |
| **Error prevention** | Plausible weight range; do not persist 0 lb by accident; confirm delete (already done). |
| **User control** | Optional reminder, optional note, optional kg. Defaults stay lb and no reminder. |
| **Jobs to be done** | (1) Log after stepping off the scale. (2) See whether the last month moved toward the goal. (3) Know the goal is a loss, a gain, or a hold. |
| **Stay local** | New fields stay in the on-device JSON. No account, no scale pairing, and no Health Connect in the first phases. |

---

## Recommendations

Phased so the misleading vs-prior headline and the missing trend ship before smart scales or body-fat fields.

### Phase 1 — Honest numbers and safer logging

Highest impact, small surface area. Reuse `trailingMovingAverage`. No new navigation.

1. **Add a weight trend series** (default window 7 readings or 7 days — pick one and label it). Draw it on `WeightChart` the way BP draws its average. Keep the raw line, thinner. Put the **latest trend value** on the Weight hero and the Home chip, with raw latest as secondary text (`Trend 181.4 lb · last 182.1`).
2. **Change the Home chip and hero delta** to **vs 7-day trend** (or vs the trend from 7 days earlier), not vs the previous raw point. Keep the card arrows, but mute changes under 0.5 lb (use the existing dash).
3. **Tighten validation** without blocking real humans:
   - Soft-block or hard-block weights below 50 lb or above 700 lb (or similar consumer-scale band).
   - Do not allow a silent 0.0 save from the minus stepper.
   - Keep 0.1 lb resolution.
4. **Replace the empty-form default of 150.0** with a blank field and a hint (“Last time: 182.3 lb” when one exists). 150 is not a measurement.
5. **Fix goal copy** from the sign of `start − target`:
   - Loss: “Target to lose” / “Weight lost”
   - Gain: “Target to gain” / “Weight gained”
   - Hold (span ≈ 0): “Maintain” / distance from target
6. **Name BMI on the gauge:** “BMI 26.2 · Overweight · CDC adult screening.” One line. Not a lecture.
7. **Stop clipping the gauge at 35 without saying so.** Either extend the obese arc to 40+ (Class 3), or keep 12–40 and mark Class 1 / 2 / 3 tick labels. A BMI of 42 must not look like 35.

### Phase 2 — Time, rate, and a usable chart (the industry gap)

This is the main quality upgrade and the largest gap vs Happy Scale / Libra / Apple Health.

1. **Daily representative series for the chart.** Default: **lowest weight that calendar day** (Happy Scale’s Health-import rule). Keep every raw row in the list. Chart the daily series so three weigh-ins on Saturday do not draw a spike.
2. **Time-accurate X axis** (date, not index). Gaps should look like gaps.
3. **Range chips:** 7 / 30 / 90 / all, default 30 on the Weight tab and 10 on Home (preserves today’s dashboard). History list: “Show older” beyond 30.
4. **Rate card** under the target: `−1.3 lb / week` over the last 28 days of trend, with a muted caption “CDC suggests about 1–2 lb per week when losing.” Do not color a faster rate as success.
5. **Projected date** only when the trend is moving toward the target at ≥ ~0.2 lb/week: “At this rate, around Jun 2027.” Label it an estimate. Hide it on a plateau.
6. **Goal helper in Profile:** “10% of start is 162.0 lb” as a suggestion chip, not an automatic target. NIH initial-goal pattern.
7. **How to weigh** sheet, linked from the empty state and the form lead: morning, after restroom, before food, same clothes, daily if possible, trust the trend. Not a lecture on first launch.
8. **Optional local morning reminder**, reusing the blood-pressure reminder channel pattern. Default off. Copy: “Time to log a scale reading,” not a coaching nudge.

### Phase 3 — Context, units, and sharing

1. **Optional one-line note** on the form (salty meal, illness, vacation, after workout).
2. **Optional tags** (short list): Morning, Evening, After exercise, Travel. Morning can also be inferred from time, as BP already does with period.
3. **lb / kg display toggle** in Profile. Store one canonical unit (keep **lb in the file** for Expo export compatibility) and convert in the UI. 0.1 lb ≈ 0.05 kg; show kg to 1 decimal.
4. **Obesity classes** on the BMI caption when BMI ≥ 30: Class 1 / 2 / 3 with the CDC ranges. Keep the four-color arc; add the class as text.
5. **Optional waist (inches)** as a profile or dated measurement. Show NHLBI cutoffs only if the user also records sex — or show both cutoffs and let the user compare. Do not guess sex.
6. **Weight-only share:** on-device CSV or plain text of date, time, lb, note for a chosen range. Full-app JSON remains the backup.
7. **Filter recent readings** by date range and tag.
8. **BMI caveat on the guide** (tap the gauge, same pattern as the BP badge): does not measure fat vs muscle; athletes and older adults are often misclassified; talk to a clinician; source = CDC / NHLBI.

### Phase 4 — Later, only if the product expands

These match competitors but fight the local-only, manual-entry product.

- Health Connect read/write of `Weight` (and optional `BodyFat`)
- Bluetooth / Wi-Fi scales (Withings, Renpho, Garmin)
- Body-fat %, muscle mass, water % from bioimpedance (disclose method and error)
- Calorie or macro diary (MyFitnessPal’s job; do not take it)
- Pediatric BMI percentiles (different CDC charts; this app is adult)
- Pregnancy-specific guidance
- Asian WHO action-point cutoffs as an explicit, named setting
- Photo of the scale or progress photos
- Family profiles
- Interactive chart scrubbing and pinch-zoom
- Cloud sync

---

## Suggested information architecture (weight only)

```
Home
 └── Weight card
      ├── trend + raw latest
      ├── vs 7-day change (not vs prior raw)
      ├── last-10 (or selected range) chart: raw + trend + goal
      └── Log → Record weight

Weight (tab)
 ├── Hero (trend, raw latest, vs-week)
 ├── History chart (7 / 30 / 90 / all, time-scaled X)
 ├── Weight target (start, %, target, rate, optional ETA)
 ├── Body Mass Index (gauge + named CDC caption)
 └── Recent readings (show older, filter)
      ├── Record / Edit weight
      │    ├── date, time
      │    ├── weight (prefilled last; − / + 0.1)
      │    ├── optional note + tags
      │    └── Save / Cancel
      └── Share period (Phase 3)

Profile
 ├── Height                         (exists)
 ├── Weight goal + starting reading (exists)
 ├── 10% suggestion chip            (Phase 2)
 ├── Weight reminder                (Phase 2)
 └── Display units lb / kg          (Phase 3)
```

No new tab. Setup the hub depends on (goal, start, units) stays linked from the target section, as it is today.

---

## Data model additions (when a phase needs them)

Keep `WeightRecord` backward compatible. Import must still accept today’s four-field JSON.

```
WeightRecord
  id, date, time
  weight                 // lb, 0.1  — canonical storage
  note?                  // Phase 3
  tags?                  // Phase 3, small enum list
```

```
UserProfile
  heightInches           // exists
  weightGoalLb           // exists
  weightGoalStartId      // exists
  weightTrendWindow?     // Phase 1, default 7 (readings or days; pick one)
  weightMorningReminder? // Phase 2
  weightMorningReminderTime?
  weightDisplayKg?       // Phase 3, display only
```

Optional later, not required for Phases 1–2:

```
WaistRecord or profile.waistInches
  date, inches
```

Export, import, erase, and the account-file tests must include any new fields. Old files load with defaults. Expo `life-metric-account` v1 already ignores unknown keys; new fields default to empty / 0 / null on the Android side.

---

## Copy and labeling rules

- Keep **lb** as the stored unit. Display **kg** only behind an explicit toggle.
- Category names: **Underweight**, **Healthy weight**, **Overweight**, **Obesity** (add Class 1 / 2 / 3 when BMI ≥ 30). Today’s short “Under / Healthy / Over / Obese” can stay on the compact Home gauge if space requires it; the Weight tab should use the CDC words.
- On trends, say **“7-day trend is 181.4 lb”**, not “you weigh 181.4.”
- Rate caption: **“About 1–2 lb per week is the usual clinical pace when losing.”** Never “good” / “bad” on a faster week.
- Projection: **“Estimate only. Daily scale weight jumps around.”**
- BMI caption: **“CDC / NHLBI adult screening. Not a diagnosis.”**
- Goal empty states stay as they are (choose a start, set a target).
- Prefill hint: **“Last time: 182.3 lb.”** Same voice as workout previous load.
- Reminder: **“Log a scale reading.”** Not “you are off track.”

---

## Implementation phases (summary)

### Phase 1 — Honest numbers and safer logging

- 7-day (or 7-reading) trend on the chart, hero, and Home
- Vs-week on the headline; mute sub-0.5 lb arrows
- Realistic input range; no 150.0 ghost default
- Gain / lose / maintain copy
- Named BMI source; gauge that can show Class 3

### Phase 2 — Time, rate, and a usable chart

- Daily representative (default: day’s lowest)
- Time-scaled X; 7 / 30 / 90 / all
- lb/week rate; optional ETA; 10% goal chip
- How-to-weigh sheet
- Optional local morning reminder

### Phase 3 — Context, units, and sharing

- Note + tags; filters; show older
- lb / kg display
- Obesity classes in text; BMI guide sheet
- Optional waist
- Weight-only CSV / text share

### Phase 4 — Platform (explicitly later)

- Health Connect, scales, body-composition fields, calories, pediatric / pregnancy / Asian cutoffs

---

## Acceptance checks

The weight work is in good shape when all of the following are true.

1. After two weeks of daily weigh-ins that bounce ±2 lb around a slow decline, the **hero and Home lead with a falling trend**, not a red up-arrow from last night’s dinner.
2. Two readings a month apart are **visually a month apart** on the chart. Three readings on one Saturday do not dominate the line.
3. A gain goal says **gain**. A loss goal says **lose**. A target equal to start says **maintain**.
4. BMI **30 / 35 / 40** show Class 1 / 2 / 3 (or a gauge that does not flatten 42 to the same needle as 35). The caption names CDC/NHLBI and “screening.”
5. Classification of a **single** BMI still matches the existing unit test. New trend / daily-bucket logic has its own tests and does not change `calculateBmi` or `weightGoalProgress` boundaries unless those functions are intentionally extended (gain/maintain labels).
6. Export / import of older JSON (no notes, no reminder) still loads. Erase still clears weights. Undo delete still restores a start reading.
7. The disclaimer still states the app is not a device and that BMI is not a diagnosis.
8. Nothing in Phases 1–3 requires an account, a network call, or a paired scale.

---

## Out of scope for this proposal

- Turning Life Metric into a calorie, fasting, or macro tracker
- Diagnosing obesity, malnutrition, or eating disorders
- Pushing a BMI-derived “ideal weight” as the user’s target
- Cloud sync, login, or selling data
- Bluetooth scales or Health Connect in Phases 1–3
- Pediatric growth charts
- Bioimpedance body-fat as a primary metric
- Changing stored export units away from lb (display-only kg is in scope for Phase 3)

If those are added later, they should reuse the same hub, form, and “trend over spike, named standard, not a diagnosis” rules above.

---

## Sources

- CDC, *About Body Mass Index (BMI)* and *Adult BMI Categories* (screening language; adult cutoffs; Class 1–3)
- CDC, *Steps for Losing Weight* (about 1–2 lb per week)
- NHLBI / NIH, *Clinical Guidelines on the Identification, Evaluation, and Treatment of Overweight and Obesity in Adults* (BMI formula 703 × lb / in²; 10% initial goal; 1–2 lb/week; waist with BMI 25–34.9)
- NHLBI, *Classification of Overweight and Obesity by BMI, Waist Circumference, and Associated Disease Risks* (waist 40 in men / 35 in women)
- WHO expert consultation on Asian BMI action points (23 and 27.5) — reference only; not the Life Metric default
- Happy Scale support (daily morning weigh-in; moving average vs scale noise; day’s lowest on Health import)
- Libra, *What is a trend?* (time-aware exponential smoothing, default 7 days)
- John Walker, *The Hacker’s Diet* (exponential smoothing as the consumer-weight-tracking pattern Libra and Happy Scale follow)
- FTC, *Mobile Health App Developers: Best Practices* (minimize data; local-first fits Life Metric)
- Existing in-app review: `docs/bp-improvements.md` (averages, reminders, stay-local phasing) and `docs/cx-improvements.md` (hub + form, one primary action)
