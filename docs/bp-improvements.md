# Blood Pressure Tracking: Analysis and Improvement Proposal

This document reviews Life Metric’s blood pressure (BP) tracking against current clinical guidelines and against the features people already use in dedicated BP apps. It is a product proposal, not a diagnosis tool. Existing capabilities listed in [What Life Metric already does](#what-life-metric-already-does) must stay available.

The bias is the same as the rest of the app: **local-only records, AHA-accurate labels, one obvious next action**. Suggestions that would turn Life Metric into a medical device, a cloud service, or a cuff companion are called out as later / out of scope.

---

## Why this needs work

The BP area is already one of the strongest parts of the app. Classification matches the American Heart Association (AHA) adult categories, including Severe Hypertension. Home shows both a trend line and a category chart. Badges open a guide. Delete has undo. That foundation is sound.

The experience falls short of how home blood pressure monitoring (HBPM) is supposed to work, and of what Omron Connect, SmartBP, Apple Health, and similar apps now treat as table stakes.

1. **Each reading is treated as a verdict.** Guidelines classify from an *average* of several measurements, usually two readings a minute apart, morning and evening, for several days. Life Metric labels every single entry. The category dialog already says this is not a diagnosis, but the hub and Home still lead with one latest stamp.
2. **The log is thin.** A reading is date, time, systolic, diastolic, and a required heart rate. There is no note, arm, posture, before/after medication, or morning/evening tag. Competitors use those fields to explain *why* a number moved.
3. **Insight stops at “latest + last 10 + last 30.”** There is no period average, no morning vs evening split, no personal target, no pulse pressure or mean arterial pressure, and no clinician-ready report. Weight already has a goal and a vs-prior delta; BP does not.
4. **One visualization is misleading.** The Home gauge is driven only by systolic. A reading such as 118 / 82 is Stage 1 Hypertension, but the needle sits in the green “normal” band.

Industry-standard HBPM software (Apple Health Blood Pressure Log, Omron Connect, SmartBP, Blood Pressure Companion) all use the same loop: **measure with a validated cuff → log with context → average over a protocol window → show category and target → share a report**. Life Metric covers the first half of “log” and the category half of “show.”

---

## What Life Metric already does

Inventory of the current BP product. New work should add to this, not replace it.

### Data and rules

- `BloodPressureReading`: id, date, time, systolic, diastolic, heart rate (all required)
- Stored only on the device; included in JSON export / import / erase
- Validation: required date and time; whole numbers ≥ 1; diastolic lower than systolic
- Classification (`bloodPressureStatus`) using AHA adult rules, higher category wins:
  - **Normal** — systolic &lt; 120 **and** diastolic &lt; 80
  - **Elevated** — systolic 120–129 **and** diastolic &lt; 80
  - **Stage 1 Hypertension** — systolic 130–139 **or** diastolic 80–89
  - **Stage 2 Hypertension** — systolic ≥ 140 **or** diastolic ≥ 90
  - **Severe Hypertension** — systolic &gt; 180 **and/or** diastolic &gt; 120
- Unit tests cover the category boundaries, including mixed numbers (120 / 80 → Stage 1; 180 / 120 → Stage 2; 181 or 121 → Severe)
- Trailing moving average, window 2–60 readings (default 10), configurable in Profile
- Latest reading is the newest date + time, then id

### Blood pressure tab

- Empty state with a path to add the first reading
- Hero: `SYS / DIA mmHg · HR bpm`, status badge, date/time
- Severe warning copied from AHA consumer language (wait 1 minute and repeat; call the clinician if it stays high; call 911 for listed symptoms)
- History chart of the last 10 readings with systolic, diastolic, and trailing averages, plus a legend
- Link under the chart to change the average window
- Recent list (last 30): values, date/time, compact badge, tap to edit, overflow delete
- Confirm delete that quotes the reading; snackbar undo
- FAB: Add reading

### Add / edit form

- Titles Add reading / Edit reading
- Date and time default to now
- Systolic and diastolic on one row, then heart rate
- Cancel / Save; inline field errors
- Missing-record screen if the id was deleted

### Home

- Top chip: latest `SYS/DIA` and heart rate
- First-session coach row to record blood pressure
- Dashboard card: gauge, badge, date/time, heart rate, trend chart, category (zone) chart
- Log action on the card opens the same add form
- Tapping the section opens the BP tab

### Shared chrome

- Badge is a 48 dp control; tap opens the five-category guide
- Guide states AHA adult categories, the “higher number wins” rule, and “this is not a diagnosis”
- Category chart: diastolic 40–120, systolic 70–180, stepped zones, dashed crosshairs that stop at the reading, 180+ / 120+ axis labels
- Health disclaimer: not a medical device, not an emergency service, seek care for dangerous readings

---

## What is already strong

Keep these. They are differentiators against many free trackers.

| Strength | Why it matters |
|---|---|
| AHA adult categories, including Severe | Matches the 2017 and 2025 AHA/ACC office categories and the AHA public chart. Many apps still show JNC-7 “Prehypertension” or ESC grades without saying so. |
| “Higher category wins” explained in the dialog | Same rule the AHA uses when systolic and diastolic disagree. |
| Severe copy distinguishes “very high, no symptoms” from hypertensive emergency | Aligns with current AHA wording (wait, repeat, call the clinician; 911 if symptoms). |
| 2D category chart plus time-series chart | Most apps show only a line. The zone chart teaches *why* a reading is Stage 1 vs Stage 2. |
| Local-only storage and a real disclaimer | Dedicated BP apps often push cloud accounts. Life Metric’s privacy story is a feature. |
| Undo delete, explicit Save, shared hub + form | Already matches the CX system used by Weight and Workout. |
| Tested classification | Boundary cases are easy to get wrong (120/80, 180/120). Tests lock the contract. |

---

## Standards used for this review

Guidelines disagree on *thresholds*. They agree on *how to measure at home*. Life Metric should stay AHA-labeled (US-first, already implemented) and borrow protocol and reporting patterns that every major guideline shares.

### Classification (what a number means)

| Source | Normal / non-elevated | Elevated | Hypertension starts | Home (HBPM) hypertension |
|---|---|---|---|---|
| **AHA/ACC 2017 and 2025** (office) | &lt;120 and &lt;80 | 120–129 and &lt;80 | Stage 1 at 130 or 80; Stage 2 at 140 or 90 | Same numbers as office at the 120/80 and 130/80 correspondence points |
| **AHA consumer chart** | Same as above | Same | Same, plus **Severe / Hypertensive Crisis** &gt;180 and/or &gt;120 | Educational labels on a single reading |
| **ESC 2024** | Office &lt;120/70 | Office 120/70 to &lt;140/90 | Office ≥140 or ≥90 | Home ≥135/85 |
| **ESH 2023** | Similar to older European grades | — | Office ≥140/90 | Home ≥135/85 |
| **NICE NG136 (UK)** | — | — | Clinic ≥140/90 | Home or daytime ABPM ≥135/85; treatment targets differ by age |

Life Metric correctly implements the **AHA/ACC adult office categories** plus the AHA Severe band. It does **not** implement ESC/NICE home cutoffs (135/85) and should not silently switch. If a guideline picker is ever added, AHA must remain the default and the labels must name the source.

The 2025 AHA/ACC guideline **retires the 2017 document** but **keeps the same four office categories**. Treatment is more risk-based (PREVENT). That does not change how Life Metric should color a reading.

### Home monitoring protocol (how to collect numbers)

AHA/ACC 2017 Table 10, AHA “Monitoring Your Blood Pressure at Home,” AHA/ACC 2025 HBPM (Class I), and ESC 2024 Figure 4 all say essentially the same thing:

1. Use an **automatic, upper-arm, validated** cuff (not wrist or finger; not a watch). See [validatebp.org](https://www.validatebp.org).
2. **Prepare:** no smoking, caffeine, or exercise for 30 minutes; empty the bladder; sit quietly ≥ 5 minutes; don’t talk.
3. **Sit:** back supported, feet flat, legs uncrossed, bare upper arm, cuff at heart level.
4. **Repeat:** at least **two readings, 1–2 minutes apart**.
5. **When:** morning (before medication) and evening, same times, for **3–7 days** (Apple Health uses 7 wake + 7 bedtime for a risk log, or 28 daily readings to monitor known hypertension).
6. **Decide from the average** of those readings, not from one spike. Discard or keep the first rest reading per the clinician’s instructions; Life Metric should at least *offer* pairing and averaging.
7. **Do not use cuffless / smartwatch BP** as the accurate measurement. The 2025 AHA/ACC guideline says so explicitly. Apple Watch hypertension notifications are a *risk signal*, then they send the user to a cuff log.

### Derived numbers clinicians actually use

These are not extra “wellness scores.” They are standard calculations from the two pressures the app already stores.

| Metric | Formula | Why apps show it |
|---|---|---|
| **Pulse pressure (PP)** | systolic − diastolic | Wide PP (often ≥ 60 mm Hg) is a stiffness / isolated-systolic clue, common with age. |
| **Mean arterial pressure (MAP)** | (systolic + 2 × diastolic) / 3 | Single-number perfusion estimate; SmartBP and many clinical sheets show it. |
| **Period average** | mean SYS and DIA over a chosen window | This is what AHA/ESC/NICE want brought to the visit. |
| **Morning vs evening average** | split by clock | Detects morning surge and medication wear-off. Omron’s diary is built on this. |
| **Variability** | SD or range of SYS | Visit-to-visit variability is discussed in the 2025 guideline as extra information, not a diagnosis. |

### Measurement context the record should be able to carry

STRIDE BP / AHA technique checklists and SmartBP-style tags:

- Arm (left / right)
- Position (sitting / lying)
- Timing (before medication / after medication / morning / evening)
- Optional note (caffeine, poor sleep, headache, after a workout)
- Optional irregular-heartbeat flag if the cuff reported one

Life Metric stores none of these today.

---

## How other products do it

Compared as **consumer HBPM software**, not as medical devices.

| Capability | Life Metric | Apple Health (2025–26) | Omron Connect | SmartBP | Blood Pressure Companion |
|---|---|---|---|---|---|
| Manual SYS / DIA | Yes | Yes | Yes | Yes | Yes |
| Heart rate | Required | Optional / from cuff | From cuff | Optional | Optional |
| AHA category on each reading | Yes, + Severe | Region-aware (AHA / ESC / FIGO) on *log averages* | ESC/ESH ranges in some regions | AHA-style | AHA-style |
| Category education | Badge → 5-category dialog | In-log explanation | Reference values | In-app | In-app |
| 2D zone chart | Yes (Home) | No | No | Uncommon | Uncommon |
| Time-series + moving average | Last 10 on screen | Date-range charts | Day / week / month | 7 / 14 / 30 and filters | Charts + export |
| Two-reading / AM–PM protocol | No | 7-day AM/PM or 28-day daily log | Morning vs evening diary | AM/PM daily averages | Tags + filters |
| Reminders | No | Yes | Medication + measure | Yes (premium) | Yes |
| Notes / tags / arm | No | Limited | Lifestyle memos, medication | Tags (arm, sitting, meal, …) | Tags |
| Personal BP target | No | Implicit in log | Yes; calendar colors vs goal | Yes | Sometimes |
| Pulse pressure / MAP | No | No | Uncommon | Yes | Sometimes |
| Clinician PDF / CSV | JSON dump of the whole app | PDF of the completed log | PDF + CSV logbook | PDF + CSV | PDF |
| Bluetooth cuff / Health Connect | No (manual, local) | Via partner apps | Omron cuffs | Many cuffs + HealthKit | HealthKit |
| Technique coaching | Disclaimer only | Onboarding before a log | Device manuals | Help text | Help text |
| On-device only | Yes | iCloud Health | Cloud account | Optional cloud | Optional iCloud |

**Google Fit / Health Connect** is a hub, not a BP product. Samsung Health is similar on Galaxy. They matter later if Life Metric ever *exports* a reading; they are not the bar for the in-app log.

**Qardio** is listed in older roundups but the companion app left the stores after 2024. Do not copy a dead cloud-plus-cuff model.

---

## Gap analysis

What a user can do today vs what guidelines and the comparison set expect.

| Job | Life Metric today | Gap |
|---|---|---|
| Log a reading in 10 seconds | Form is short and clear | Heart rate is mandatory; last SYS/DIA/HR are not suggested (Weight already prefills the last scale value) |
| Trust the category | AHA math + tests + dialog | Home **gauge ignores diastolic**, so Stage 1 from DIA alone looks “normal” |
| Know if a value is plausible | `≥ 1` and DIA &lt; SYS | A cuff will never read 3 / 2. Typical consumer ranges are about SYS 50–250, DIA 30–150, HR 30–220. No warning for pulse pressure ≤ 0 (already blocked) or extremely wide PP |
| Follow HBPM protocol | User must remember | No “log a pair,” no 1-minute timer, no 7-day AM/PM checklist |
| See the number that matters for a visit | Latest hero + 10-point chart | No 7-day / 30-day mean, no “bring this to your appointment” card |
| Compare morning and evening | Time is stored, unused | No split, no filter |
| Set a target | Weight has a goal; BP does not | Cannot see % of readings at or under a clinician-given target (often &lt;130/80 in AHA care) |
| Explain a high day | Nothing but the number | No note, arm, medication timing, or workout link |
| Review history | Last 30 rows | Older readings exist in the file but disappear from the UI; no date-range filter |
| Share with a clinician | Full-app JSON | Not printable; mixes workouts and profile |
| Measure correctly | Disclaimer | No 30-second “how to sit” sheet or validatebp.org pointer |
| Notice low pressure | Only AHA high bands | Hypotension is out of AHA’s five labels but is in the disclaimer; a reading of 88 / 54 is called Normal |
| See derived vitals | HR only | No PP or MAP on the hero or form |
| Stay on a schedule | None | No optional local reminder (morning / evening) |

### The gauge problem (fix first)

`BpGauge` maps **systolic only** onto five equal arcs (80–120, 120–130, 130–140, 140–180, 180–200). Diastolic is printed under the arc and is not used for the needle.

That contradicts `bloodPressureStatus` and the zone chart. Isolated diastolic hypertension (for example 118 / 86) is Stage 1 in the badge and green on the gauge. Isolated systolic hypertension (for example 148 / 72) happens to look right because SYS is high.

Fix options, in order of fidelity:

1. **Drive the needle (or a second marker) from the classified status**, not from SYS alone.
2. Or **retire the gauge** and let the zone chart — which already plots both numbers — be the Home insight.

Do not leave both widgets disagreeing.

---

## Design principles for BP

Same CX methods as `docs/cx-improvements.md`, applied to vitals.

| Principle | What it means here |
|---|---|
| **One classification, named** | Always AHA adult categories unless the user picks another published set. Show the source on the guide. |
| **Educate, do not diagnose** | Keep “this is not a diagnosis.” Prefer averages and ranges over a single red badge as the “truth.” |
| **Protocol over more fields** | A paired-reading flow and a 7-day average help more than a medication database. |
| **Recognition over recall** | Prefill last SYS/DIA/HR; show “Last time: 128 / 78 · 64 bpm.” |
| **Progressive disclosure** | Hero stays three numbers + badge. Notes, arm, PP/MAP, and protocol sit one tap deeper or as optional form rows. |
| **Error prevention** | Plausible ranges; warn on Severe; do not require heart rate if the user skipped the pulse. |
| **User control** | Optional reminders, optional target, optional tags. Defaults stay as they are. |
| **Jobs to be done** | (1) Log after a cuff reading. (2) See whether this week is better than last. (3) Hand a week of averages to a clinician. |
| **Stay local** | New fields stay in the on-device JSON. No account, no cuff pairing, and no Health Connect in the first phases. |

---

## Recommendations

Phased so the misleading gauge and the “average, not a spike” job ship before Bluetooth or medications.

### Phase 1 — Correctness and faster logging

Highest impact, small surface area. No new navigation.

1. **Fix the Home gauge** so it cannot contradict the badge. Prefer status-driven color and needle, or remove the gauge.
2. **Tighten validation** without blocking real cuff values:
   - Systolic 50–250, diastolic 30–150, heart rate 30–220 when provided.
   - Keep “diastolic lower than systolic.”
   - Soft-warn (not block) on pulse pressure ≥ 80 or a reading in the Severe band before Save, in addition to the existing hub warning.
3. **Make heart rate optional.** Store `null` when blank. Show “— bpm” in lists. Import already requires a pulse today; accept missing HR on new records and treat a missing import field as unset.
4. **Prefill the add form** from the latest reading (same pattern as Weight). Date and time still default to now.
5. **Show vs-prior on the hero** (`+6 / +2 mmHg vs prior`, or “no change”), using the same newest-to-previous pairing as weight delta.
6. **Show pulse pressure and MAP** as secondary text on the hero and Home card (`PP 46 · MAP 93`). Compute only; do not add inputs.
7. **After a valid save**, flash the category on the form (“Stage 1 Hypertension”) so the user sees the label before the hub appears. Already suggested in the CX doc; still useful.

### Phase 2 — Protocol and averages (the guideline gap)

This is the main clinical-quality upgrade and the largest gap vs Apple Health / Omron / ESC / AHA HBPM.

1. **Paired reading (optional).** On the form, a control: “Add a second reading (wait 1 minute).” Offer a simple 60-second countdown. Saving the pair stores two records **or** one record with `systolic` / `diastolic` as the pair average plus the two raw values. Prefer **one displayed reading that is the average of the pair**, with the raw pair kept for edit/export, so the list does not double-count. Either model is fine if export stays lossless.
2. **Morning / evening chip** inferred from time (for example before 12:00 / after 16:00, with an override). Used for filters and averages, not for classification of a single row.
3. **Visit summary card** on the BP hub (and a compact line on Home) once enough data exists:
   - Last 7 days: count of readings, mean SYS/DIA, mean HR, AHA category of that *average*
   - Morning mean vs evening mean when both exist
   - Caption: “AHA categories on this card use the average, not one reading.”
4. **Personal target** in Profile (Goals & charts), same editor pattern as weight goal. Default unset. When set (for example 130 / 80), the hub shows how many of the last 7 and last 30 readings are at or under target. Do not invent a medical target; the user types what their clinician gave them.
5. **Date-range on the chart:** last 7 / 10 / 30 / all, default 10 to preserve today’s view. History list should be able to show more than 30 (paged or “Show older”).
6. **How to measure** sheet, linked from the empty state and the form lead: the seven AHA steps, plus “Use a validated upper-arm cuff” and a text link to validatebp.org. Not a lecture on first launch.

### Phase 3 — Context, history, and sharing

1. **Optional tags on the form** (multi-select, short list): Left arm, Right arm, Before medication, After medication, After exercise, After caffeine, Felt unwell. Plus a one-line note.
2. **Filter recent readings** by tag and by morning/evening.
3. **Clinician report** (share sheet, on-device): a one-page PDF or a plain-text / CSV that contains only BP — period averages, morning/evening split, min/max, each reading with time and tags. Do not require a new file format for backup; keep the existing JSON as the full backup.
4. **Optional local reminders** for morning and evening measurement. No server, no “you missed a dose” medical claim.
5. **Hypotension hint** when SYS &lt; 90 or DIA &lt; 60: do not invent an AHA category. Show a muted note (“This is lower than the usual adult range. The AHA chart is for high blood pressure.”) and keep the badge as Normal unless Severe/Stage rules apply (they will not).
6. **Isolated systolic / isolated diastolic** one-liner on the category dialog when only one number is in range (“Systolic is in Stage 2; diastolic is not”).

### Phase 4 — Later, only if the product expands

These match competitors but fight the local-only, manual-entry product.

- Bluetooth cuffs or Health Connect import
- Medication schedule and pill reminders (Omron premium)
- ESC / NICE classification as an explicit setting, with every badge labeled “ESC” or “AHA”
- Pregnancy-specific FIGO ranges (Apple Health does this when a pregnancy is logged)
- Workout correlation (“readings within 2 hours of a session”)
- White-coat / masked hypertension education that needs a separate “office reading” type
- Irregular heartbeat flag (only useful if a cuff supplies it)
- Family profiles
- kPa units
- Interactive chart scrubbing

---

## Suggested information architecture (BP only)

```
Home
 └── Blood pressure card
      ├── latest + badge + (fixed) insight
      ├── 7-day average (Phase 2)
      └── Log → Add reading

Blood pressure (tab)
 ├── Hero (latest, vs-prior, PP/MAP, Severe copy)
 ├── Visit summary (7-day mean, AM vs PM, vs target)
 ├── History chart (7 / 10 / 30 / all)
 ├── How to measure (link)
 └── Recent readings (filter, show older)
      ├── Add / Edit reading
      │    ├── date, time
      │    ├── SYS, DIA, optional HR (prefilled)
      │    ├── optional second reading + 1-minute wait
      │    ├── optional tags + note
      │    └── Save / Cancel
      └── Share report (Phase 3)

Profile → Goals & charts
 ├── BP average window          (exists)
 └── BP target SYS / DIA        (Phase 2)
```

No new tab. Setup that the hub depends on (window, target) stays linked from the chart, as it is today.

---

## Data model additions (when a phase needs them)

Keep `BloodPressureReading` backward compatible. Import must still accept today’s five-field JSON.

```
BloodPressureReading
  id, date, time
  systolic, diastolic
  heartRate            // nullable
  note?                // Phase 3
  tags?                // Phase 3, small enum list
  pairId?              // Phase 2, groups two cuff inflations
  averagedFrom?        // Phase 2, if the displayed row is a pair mean
```

```
UserProfile
  bpMovingAverageReadings   // exists
  bpTargetSystolic?         // Phase 2
  bpTargetDiastolic?        // Phase 2
```

Export, import, erase, and the account-file tests must include any new fields. Old files load with defaults.

---

## Copy and labeling rules

- Keep **mm Hg**, **systolic / diastolic**, **heart rate (bpm)**.
- Keep category names exactly: Normal, Elevated, Stage 1 Hypertension, Stage 2 Hypertension, Severe Hypertension.
- On averages, say **“7-day average is Stage 1 Hypertension”**, not “you have Stage 1 Hypertension.”
- Severe copy stays as it is (repeat, clinician, 911 + symptoms). Do not add a “Crisis” label unless AHA consumer materials change again.
- Empty heart rate: omit it from the hero (`118 / 76 mmHg`) instead of showing `0 bpm`.
- Prefill hint: **“Last time: 128 / 78 · 64 bpm.”** Same voice as workout previous load.

---

## Implementation phases (summary)

### Phase 1 — Correctness and faster logging

- Status-accurate Home gauge (or remove it)
- Realistic input ranges; optional heart rate; prefill last reading
- Vs-prior, pulse pressure, MAP on the hero
- Save acknowledgement with category

### Phase 2 — Protocol and averages

- Optional paired reading + 1-minute wait
- Morning / evening
- 7-day (and 30-day) average card; category of the average
- Optional personal target
- Chart ranges and full history
- How-to-measure sheet + validatebp.org

### Phase 3 — Context and sharing

- Tags and a note
- Filters
- On-device clinician PDF/CSV
- Optional local reminders
- Low-reading hint; isolated SYS/DIA line in the guide

### Phase 4 — Platform (explicitly later)

- Cuffs, Health Connect, medications, other guideline sets, pregnancy ranges

---

## Acceptance checks

The BP work is in good shape when all of the following are true.

1. A reading whose category is driven by **diastolic only** shows the same status on the badge, the dialog, the zone chart, and Home. The gauge no longer implies Normal.
2. Classification of a **single** reading still matches the existing unit tests. New average logic has its own tests and does not change those boundaries.
3. A user can log SYS/DIA without heart rate, and can accept a prefilled last reading in one tap.
4. After a week of morning and evening pairs, the hub shows a **7-day average** and morning vs evening, and says those averages are what to discuss with a clinician.
5. Export / import of older JSON (no tags, required heart rate) still loads. Erase still clears BP. Undo delete still works.
6. Severe warning copy is unchanged. The disclaimer still states the app is not a device and not an emergency service.
7. Nothing in Phases 1–3 requires an account, a network call, or a paired cuff.

---

## Out of scope for this proposal

- Changing AHA cutoffs to ESC 135/85 or NICE age-based targets (unless added later as a named setting)
- Putting the Severe / crisis zone back on the category chart (removed by product choice; Severe remains a badge + warning)
- Diagnosing white-coat or masked hypertension
- Cloud sync, login, or selling data
- Turning Life Metric into an FDA-cleared monitor
- Medication prescribing, dose advice, or “your BP is controlled”
- Using Apple Watch or any cuffless gadget as a BP source

If those are added later, they should reuse the same hub, form, and “named standard, not a diagnosis” rules above.

---

## Sources

- AHA, *Understanding Blood Pressure Readings* (consumer categories and Severe / emergency language)
- AHA, *Monitoring Your Blood Pressure at Home* (technique, validated upper-arm cuff, two readings)
- 2017 ACC/AHA hypertension guideline, Table 10–11 (HBPM procedure and office/home correspondence)
- 2025 AHA/ACC hypertension guideline (same office categories; HBPM Class I; caution on cuffless devices)
- 2024 ESC elevated BP and hypertension guideline (home ≥135/85; 3–7 day AM/PM protocol)
- NICE NG136 (clinic 140/90, home 135/85)
- Apple Support, *Log your blood pressure in the Health app* (7-day AM/PM log, 28-day monitor log, PDF, AHA/ESC/FIGO)
- Omron Connect / BP Diary (targets, morning vs evening, PDF/CSV, lifestyle memos)
- SmartBP App Store listing (tags, PP, MAP, AM/PM averages, PDF/CSV)
- [validatebp.org](https://www.validatebp.org) (US validated-device list referenced by AHA)
