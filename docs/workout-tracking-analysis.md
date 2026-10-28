# Workout Tracking: Analysis and Improvement Proposal

This document reviews Life Metric’s workout tracking against public physical-activity guidance (HHS / CDC, ACSM) and against the features people already use in dedicated training apps. It is a product proposal, not a coaching plan or a diagnosis tool. Existing capabilities listed in [What Life Metric already does](#what-life-metric-already-does) must stay available.

The bias is the same as the rest of the app: **local-only records, named standards, one obvious next action**. Suggestions that would turn Life Metric into an AI coach, a GPS watch companion, a social network, or a cloud service are called out as later / out of scope. Screen-flow work already covered in [docs/cx-improvements.md](cx-improvements.md) is noted where it has shipped; this file is about **what to measure and show**, not how to redraw the composer.

---

## Why this needs work

The workout area now has a complete log loop: pick a plan, add sets or segments, save or discard, undo delete, and JSON backup. Blood pressure and weight in this app already lead with a **trend and a named standard**. Workout still leads with a **date-grouped list of sessions**.

That is the opposite of how serious consumer training software works, and of how clinicians and ACSM talk about activity.

1. **A session count is treated as the truth.** Home shows “Workouts (30d)” and an 8-week bar of *how many times you opened a session*. Three empty-feeling strength days and one long bike ride look the same. HHS asks for **minutes of aerobic work** and **days of muscle-strengthening**, not session count.
2. **Progress is invisible.** Strong, Hevy, and Fitbod exist because people need to see whether Chest Press went up. Life Metric already stores weight × reps and even prefills the last load, but the hub never shows volume, a vs-last-time delta, or a personal record. `formatPace` exists and is unused.
3. **The catalog is a type stamp, not a program.** A plan can be Strength *or* Cycling *or* Yoga. One session cannot mix a lift and a walk. Industry loggers treat a workout as a bag of exercises; Life Metric treats it as one `activityTypeId` with a list of `Machine` rows.
4. **Effort and rest are missing.** ACSM’s current resistance guidance emphasizes **high effort** (near failure or a few reps in reserve) and rest between sets. The entry form has load and reps. There is no RPE, rest timer, warm-up vs working set, or session clock.

Industry-standard workout software (Strong, Hevy, Apple Fitness, Google Fit, Garmin Connect) all use the same loop: **start a session → log sets against last time → rest → finish → see volume / PRs / weekly minutes**. Life Metric covers “log” and a weekly *count*. It does not yet cover progressive overload or the HHS week.

---

## What Life Metric already does

Inventory of the current workout product. New work should add to this, not replace it.

### Data and rules

- `FitnessPlan`: id, name, description, `activityTypeId`, category, list of `Machine` (shown as activities)
- `Workout`: id, **date only** (no start/end time), `planId`, `activityTypeId`, `entries`
- `WorkoutEntry`: id, plan/activity ids, `schema`, plus schema fields:
  - **load** — `weight` (lb), `repetitions`
  - **distance_time** — `distanceMi`, `durationSec`
  - **duration** — `durationSec`
  - **interval** — `rounds`, `workSec`, `restSec`
  - **sport** — `durationSec`, optional intensity (`easy` / `moderate` / `hard`)
  - **note** — free text
- Reusable telemetry on any entry (default 0): resistance, avg speed (mph), avg RPM, avg / max heart rate. **Cycling** is the only type that shows those fields today
- Stored only on the device; included in JSON export / import / erase
- Starter plans on first launch: **Upper Body Strength** and **Lower Body Focus**
- `ActivityCatalog`: 6 families, 40 types, schema-aware labels (Add set / segment / block / note)
- Tabata defaults: 8 rounds, 20s work, 10s rest
- Previous load (`findPreviousLoad`): latest weight × reps for the same **activity name** (case-insensitive, across plans)
- Last-used plan and last-used activity on that plan
- Duration parse: `mm:ss`, `h:mm:ss`, or minutes; pace helper `formatPace` (min/mi)
- Weekly buckets (`workoutSessionsByWeek`, 8 weeks) and `workoutsInLastDays(30)`
- Relative dates: Today / Yesterday / *n* days ago
- Unit tests cover last-used plan/activity, empty-session wording, and previous-load preference
- Changing a plan **clears entries** (confirmed in the UI)
- A new session is an **in-memory draft** (`startWorkout`). Back with no entries discards it. The first saved set writes the workout

### Workouts tab

- Empty state: **Create a plan** when none exist; **Log workout** when plans exist but no sessions
- **Manage plans** row on the hub
- FAB: Log workout (hidden until a plan exists)
- History grouped by date; row = type · plan name, then `summarizeWorkoutEntries`
- Overflow delete with confirm (plan, date, entry count); snackbar undo
- No hero, no chart, no filters, no “show older”

### Session composer (`WorkoutFormScreen`)

- Titles **Log workout** / **Edit workout**
- Date picker (default today); plan dropdown with type subtitle
- Confirm before a plan switch that would wipe sets
- Path to Manage plans and Add an activity when the plan is empty
- Entries grouped by activity; Set 1 / Set 2 when there are multiples; edit and delete icons
- Confirm remove of a set
- **Done** returns to the hub
- Missing-workout / plan-required screens

### Entry form (`WorkoutEntryFormScreen`)

- Titles from the catalog (Add set / Edit set, and the segment/block/note variants)
- Activity picker first; schema fields next; cycling extras (resistance before distance)
- **Previous load** as visible copy plus a **Use** button: `Last time: 50 lb × 12.`
- Explicit **Cancel / Save**; dirty Back confirms discard
- Load: weight and reps, 0 allowed
- Distance/time: miles + `mm:ss`
- Duration / sport: minutes; sport has optional intensity
- Interval: rounds, work seconds, rest seconds
- Note schema: multiline optional text
- Missing-entry screen

### Home

- Top chip: session count in the last 30 days and last-session relative date
- Dashboard card: Sessions (30d), Sets logged (30d), last session + plan name
- 8-week **session-count** bar chart
- Log action opens the same draft composer
- Tapping the section opens the Workouts tab

### Plans (Workout hub and Profile)

- List plans with type label, description, and activity count
- Create / edit: name, activity family, activity type, optional description
- Plan detail: add / edit / delete activities (name + optional description)
- Name required for plan and activity
- Profile still has a Plans count row

### Shared chrome

- Five-tab shell; tabs hidden on nested screens
- Health disclaimer: not a medical device
- Full-app JSON export / import / erase (erase restores starter plans)

---

## What is already strong

Keep these. They are the reason a workout logger belongs in Life Metric instead of sending people to a second app.

1. **One composer, not two destinations.** The old “open vs edit” split is gone. Log and edit are the same screen.
2. **Drafts do not pollute the list.** Backing out of Log workout without a set leaves nothing behind.
3. **Schema-aware logging.** Strength, rides, yoga, Tabata, pickup basketball, and a rest-day note use different fields without a generic “notes only” form.
4. **Previous load is visible and optional.** Same voice as weight’s “Last time: 182.3 lb.”
5. **Cycling telemetry is reusable.** Speed, RPM, and heart rate sit on `WorkoutEntry`, so running or rowing can opt in later without a new file format.
6. **Plans are first-class and local.** No account, no marketplace, no feed. Starter upper/lower plans unblock the first session.
7. **Export is already the backup.** Unknown keys stay ignored on Expo-style import; new fields can default.

---

## Where it falls short

### Versus public standards

| Standard | What it asks | What Life Metric shows |
|---|---|---|
| **HHS Physical Activity Guidelines for Americans, 2nd ed.** | Adults: 150–300 min/week moderate aerobic **or** 75–150 min vigorous (or a mix), spread across the week; muscle-strengthening **≥ 2 days/week** involving major muscle groups. Some activity is better than none. | Session count. Duration exists on cardio/sport/mobility entries but is not totaled into a week. Strength days are not counted as “muscle-strengthening days.” |
| **CDC consumer summary** | Same 150-minute / 2-day headline. Older adults also need **balance** work. | No minutes ring, no 2-of-7 strength-day chip, no balance/mobility week. |
| **ACSM resistance position (overview of reviews, 2025–26)** | Healthy adults: resistance **≥ 2 days/week**, all major muscle groups, **high effort**. Strength: heavier loads (~80% 1RM), 2–3 sets/exercise. Hypertrophy: weekly volume (~10 sets per muscle group). Consistency beats complexity. | Sets and load are stored. Effort, weekly sets per muscle, and “did you hit 2 days?” are not. |
| **ACSM / AHA FITT for hypertension (update)** | Aerobic most days; resistance 2–3 days, 2–4 sets of 8–12 reps on 8–10 major-group exercises; rest days between groups. Intensity can be RPE. | Intensity exists only on the **sport** schema. No RPE on lifts. No rest-day awareness beyond a Rest Day note type. |
| **Progressive overload (NSCA / common programming)** | Increase load, reps, sets, or density over time. Track volume load = weight × reps × sets, with the caveat that junk volume inflates the number. | Last load is prefilled. There is no volume number, no vs-last-session delta, no chart. |

Life Metric should **name** HHS and ACSM the way Weight names CDC/NHLBI. It should not invent a “you are untrained” verdict.

### Versus other software

| Capability | Strong | Hevy | Fitbod | Apple Fitness / Health | Google Fit | Strava / Garmin | Life Metric |
|---|---|---|---|---|---|---|---|
| Fast set log + last time | Yes | Yes | Yes (often pre-filled by algorithm) | Workout summary, not a lift log | Minutes / heart points | Activity file | Yes (load only) |
| Templates / routines | Yes | Yes | Generated session | Workouts / custom | Goals | Courses | **Plans** (one type each) |
| Rest timer | Yes | Yes | Yes | Watch workout clock | — | Device | No |
| RPE / RIR | Paid / per set | Per set | RIR per exercise | Effort on some workouts | — | Perceived effort on some sports | Sport intensity only |
| Warm-up / drop / failure sets | Yes | Yes | Partial | — | — | — | No |
| Volume and PRs | Yes | Yes | Yes | Rings / awards | Heart points | PRs on routes / FTP | No |
| Weekly aerobic minutes | Weak | Weak | Weak | Exercise minutes | Heart points / Move | Duration / TSS | Count only |
| Muscle map / coverage | Paid | Yes | Recovery % | — | — | — | No |
| Mixed lift + cardio in one session | Yes | Yes | Yes | Yes | Yes | Usually one sport | **No** (one type per plan) |
| Exercise library + demos | Yes | Yes | Large, video | Yes | — | Sport catalog | User-typed names |
| Share / export a period | CSV (paid) | Yes | Limited | Share sheet | Yes | Social by default | Full-app JSON only |
| Local, no account | No (account) | Account | Account | Apple ID | Google | Account | **Yes** |
| AI / adaptive program | No | Trainer (paid) | Core product | Coaching plans | — | Suggested workouts | Out of scope |

Life Metric should steal the **logger half of Strong** and the **week half of Apple Fitness**, not the social half of Strava or the algorithm half of Fitbod.

### Product bugs and thin spots (keep in the inventory)

- Hub has **no hero and no insight**, so Home is the only place the 8-week chart lives. BP and Weight already show hero + chart on their own tabs.
- `summarizeWorkoutEntries` for strength is “N sets.” It never says total volume or best set.
- `formatPace` is unused. A 3.10 mi · 28:12 run does not show 9:06 / mi.
- Workout **lift weight** ignores the Profile kg toggle (that toggle is body-weight display only).
- Load validation allows **0 lb × 0 reps**, which is indistinguishable from “forgot to type.”
- Session has **no clock** and no optional note. You cannot mark “home gym” vs “travel.”
- Starter activity names include typos (**Tricepts**, **Bicepts**).
- A session with nothing but 0s still counts in the 30-day chip once it is saved.
- Deleting the last set of a committed workout can leave a “Nothing logged” row in history.
- Previous load does not apply to distance, duration, or cycling extras.
- Interval `restSec` is the **work/rest prescription**, not the rest you actually took between lifts.

---

## Design principles for workouts

Same CX methods as `docs/cx-improvements.md`, applied to training.

| Principle | What it means here |
|---|---|
| **Progress over attendance** | Hero and Home lead with last session *quality* (volume, miles, minutes) and whether this week is on the HHS map. Session count stays secondary. |
| **Educate, do not coach** | Name HHS and ACSM. Never say “you failed chest day.” Never auto-increase the next weight. |
| **Named standard** | Adult week = HHS 150 min / 2 strength days unless the user picks another published target later. Show the source on the week card. |
| **Recognition over recall** | Keep last load / last plan. Show last time *on the set row*, not only on the add form. |
| **Progressive disclosure** | Hub: last session + this week + list. Volume charts, PRs, RPE, and rest timer sit one tap deeper or on the composer. |
| **Error prevention** | Keep draft discard. Do not persist 0×0 as a set. Confirm plan switch (already done). |
| **User control** | Optional rest timer, optional RPE, optional week target. Defaults stay off / unnamed. |
| **Jobs to be done** | (1) Log the session that just happened. (2) See if this week hit the public minimum. (3) See if a lift moved since last time. |
| **Stay local** | New fields stay in the on-device JSON. No account, no watch pairing, and no Health Connect in the first phases. |

---

## Recommendations

Phased so the missing week and the missing “vs last time” ship before rest timers, RPE, or GPS.

### Phase 1 — Honest session picture

Highest impact, small surface area. Reuse `summarizeWorkoutEntries`, `formatPace`, `workoutSessionsByWeek`. No new navigation.

1. **Give the Workouts tab a hero and the 8-week chart.** Same blocks Home already has: last session (plan, type, summary, relative date), 30-day chips, weekly bars. Home stays a compact copy. A date-only list is not a hub.
2. **Summaries that match the schema:**
   - Strength: `4 sets · 8,400 lb` (volume = Σ weight × reps) plus the best set (`135 × 8`) when useful.
   - Distance: keep miles + time; **add pace** from `formatPace` when both exist.
   - Duration / sport / mobility: total minutes.
   - Interval: rounds, and work/rest if present.
3. **Home chip** should not be only “7.” Prefer last-session summary (`Upper · 4 sets`) or this week’s minutes. Keep the 30-day count as the chip footnote.
4. **Do not count hollow sets.** Reject save of load 0×0, distance 0 with time 0, duration 0, interval 0 rounds. Same voice as weight’s empty-field error.
5. **Apply the body-weight unit toggle to lift weight** (still **store lb**). “Last time: 22.7 kg × 12.” One display setting, two surfaces.
6. **Fix starter copy.** Rename Tricepts / Bicepts on new installs. Do not rewrite names the user already edited.
7. **Show last time on the grouped set card**, not only inside the form. One line under the activity name.

### Phase 2 — The week and progressive overload (the industry gap)

This is the main quality upgrade and the largest gap vs Strong / Apple Fitness.

1. **HHS week card** on the Workout hub (and a one-line version on Home):
   - Muscle-strengthening days this calendar week / 2 (count a day if any `load` or strength-family session exists).
   - Aerobic minutes this week / 150 (sum `distance_time`, `duration`, `sport`, and conditioning `interval` work time). Caption: “HHS adult target is 150 minutes moderate activity and 2 strength days. Some is better than none.”
   - Do **not** color a short week as failure. A muted meter is enough.
2. **Vs last time per activity** on the composer: `Chest Press 135 × 8 · last time 130 × 8`. Green/neutral, not a lecture. Mute tiny noise (same load and reps).
3. **Volume trend** for strength plans: weekly volume for the last 8 weeks, or a sparkline on the hub when the last plan is a load schema. Caption that volume can rise from easier junk sets — ACSM cares about effort, not only tonnage.
4. **Optional session duration.** Either a start/end time or an elapsed minutes field on the composer. Date-only stays the default so existing rows do not break.
5. **How to log** sheet (same pattern as How to weigh): pick a plan, log every working set, trust last time, rest 2–3 minutes on hard lifts (ACSM-style), stop if pain is sharp. Linked from the empty state and the composer lead. Not a lecture on first launch.
6. **Optional local reminder** to log or to move, reusing the BP/weight reminder channel. Default off. Copy: “Log today’s session” or “Time for a strength day,” not “you skipped leg day.”
7. **Previous values for non-load schemas.** Last distance + time for a named segment; last minutes for yoga; last resistance / speed for cycling.

### Phase 3 — Context, effort, and sharing

1. **Optional session note** (one line: travel gym, short on sleep).
2. **Optional set tags** (short list): Warm-up, Working, Drop, Failure. Default Working. Volume and PRs ignore Warm-up.
3. **Optional RPE (1–10)** on load and interval sets. Label it Borg-style / “how hard,” not a medical score. Sport intensity can stay as the coarse easy/moderate/hard.
4. **Rest timer** on the composer after Save set. Local countdown, default off or 90s, user-changeable. Not a coaching nudge.
5. **Personal records** for an activity name: heaviest set, estimated 1RM only if we pick one published formula (Epley) and name it. Show on the activity card, not as a trophy case on Home.
6. **Workout-only share:** on-device text + CSV of date, plan, activity, lb, reps, miles, seconds, note for a chosen range or one session. Full-app JSON remains the backup.
7. **Filter history** by plan and by family (Strength / Cardio / Mobility / …).
8. **Lift-weight unit** already covered if Phase 1 reused the kg toggle; if not, add it here.

### Phase 4 — Later, only if the product expands

These match competitors but fight the local-only, manual-entry product.

- Health Connect read/write of `ExerciseSession` / `StrengthTraining`
- Wear OS / watch live logging
- GPS, Strava, Garmin, Apple Workout import
- Video exercise library and a global exercise catalog (legal and size cost)
- Mixed-type sessions (lifts and a walk in one `Workout` without switching plans)
- Supersets, circuits, and programmed rest as first-class structure
- Fitbod-style adaptive programming or Hevy Trainer
- Muscle heat maps and recovery scores
- Social feeds, leaderboards, follow lists
- Calorie estimates from MET tables (disclose error; do not become MyFitnessPal)
- Pediatric or pregnancy-specific programming
- Cloud sync and accounts
- 1RM testing protocol as a medical-style test

---

## Suggested information architecture (workout only)

```
Home
 └── Workout card
      ├── last session summary (not only “7 in 30d”)
      ├── this week: strength days / aerobic minutes
      ├── 8-week session (or volume) bars
      └── Log → session composer

Workouts (tab)
 ├── Hero (last session + vs last time)
 ├── This week (HHS 150 / 2)          (Phase 2)
 ├── History chart (8-week bars; later volume)
 └── Recent sessions (filter, show older)
      ├── Log / Edit workout
      │    ├── date [, optional duration]
      │    ├── plan  (Manage plans →)
      │    ├── entries grouped by activity
      │    │    └── Add set / segment / block
      │    │         └── last time · optional RPE · Save
      │    └── Done
      └── Share session / period     (Phase 3)

Profile
 └── Plans (count) — keep, but Workout owns the primary path
```

No new tab. Setup the hub depends on (plans, units, reminder) stays linked from Workout, as it is today.

---

## Data model additions (when a phase needs them)

Keep `Workout` / `WorkoutEntry` backward compatible. Import must still accept today’s fields.

```
Workout
  id, date, planId, activityTypeId, entries
  startedAt? / endedAt? / durationSec?   // Phase 2
  note?                                  // Phase 3
```

```
WorkoutEntry
  existing schema fields
  setKind?          // warmup | working | drop | failure   (Phase 3)
  rpe?              // 1–10                                 (Phase 3)
  restSecTaken?     // actual rest, not interval prescription (Phase 3)
```

```
UserProfile
  weightDisplayKg          // exists; reuse for lift display (Phase 1)
  workoutMorningReminder?  // Phase 2
  workoutMorningReminderTime?
```

Derived only (do not persist unless a phase needs a cache):

- `sessionVolumeLb` = Σ (weight × reps) for working load sets
- `aerobicMinutes` for a calendar week
- `strengthDays` for a calendar week
- `estimated1Rm` (Epley) only if Phase 3 ships PRs

Export, import, erase, and the account-file tests must include any new fields. Old files load with defaults. Expo `life-metric-account` v1 already ignores unknown keys.

---

## Copy and labeling rules

- Keep **lb** as the stored lift unit. Display **kg** only behind the existing toggle.
- Say **“Last time: 135 lb × 8.”** Same voice as weight and the current Use chip.
- Week caption: **“HHS adult target is 150 minutes and 2 strength days. Some is better than none.”** Never “behind” / “failed.”
- Volume caption: **“Volume is weight × reps × sets. Easy junk sets inflate it.”**
- Projection / PR: **“Estimate only.”** Name Epley if a 1RM appears.
- Reminder: **“Log today’s session.”** Not “you skipped.”
- Activity words stay user-facing: **plan, activity, set / segment / block.** Keep `machine` in code only.
- How-to sheet: log working sets, rest as needed, stop for sharp pain, talk to a clinician about a program. Not a prescription.

---

## Implementation phases (summary)

### Phase 1 — Honest session picture

- Hero + 8-week chart on the Workouts tab
- Schema-true summaries (volume, pace)
- Reject hollow 0×0 saves; apply kg display to lifts
- Last time on the grouped card; fix starter typos

### Phase 2 — The week and overload

- HHS 150 / 2 week card
- Vs last time on the composer
- Weekly volume trend
- Optional session duration; how-to sheet; optional reminder
- Previous values for cardio / cycling

### Phase 3 — Context, effort, and sharing

- Session note; warm-up vs working; optional RPE
- Rest timer; named PRs
- Workout-only CSV / text share; history filters

### Phase 4 — Platform (explicitly later)

- Health Connect, watches, GPS, video library, mixed sessions, AI programs, social, calories, cloud

---

## Acceptance checks

The workout work is in good shape when all of the following are true.

1. After a month of upper/lower splits, the **hero and Home can say the last lift moved** (or stayed), not only “12 sessions.”
2. A week with two strength days and 160 minutes of walking **reads as meeting the HHS adult headline**, with the source named. A quiet week is muted, not shaming.
3. A 3-mile run shows **pace**. A 4-set Chest Press shows **volume** (and best set), not only “4 sets.”
4. Backing out of Log workout still **leaves no empty row**. 0×0 will not save.
5. Export / import of older JSON (no notes, no RPE, no duration) still loads. Erase still restores starter plans. Undo delete still restores a session.
6. The disclaimer still states the app is not a device and not a training prescription.
7. Nothing in Phases 1–3 requires an account, a network call, a paired watch, or an exercise-video pack.

---

## Out of scope for this proposal

- Turning Life Metric into Strong social, Hevy Trainer, Fitbod, Strava, or MyFitnessPal
- Diagnosing overtraining, injury, or eating disorders
- Prescribing the next weight or a periodized block
- Cloud sync, login, or selling data
- GPS, Health Connect, or watches in Phases 1–3
- A global video exercise library
- Pediatric PE programming or pregnancy-specific plans
- Changing stored export units away from lb and miles (display-only kg is in scope)

If those are added later, they should reuse the same hub, composer, and “progress over attendance, named standard, not a coach” rules above.

---

## Sources

- U.S. Department of Health and Human Services, *Physical Activity Guidelines for Americans*, 2nd edition (adult 150–300 min moderate or 75–150 min vigorous; muscle-strengthening ≥ 2 days/week; some activity is better than none)
- CDC, *Physical Activity Basics* (consumer 150-minute / 2-day headline; older-adult balance)
- ACSM, *Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews* (PMC12965823; ≥ 2 days/week, all major groups, high effort; strength vs hypertrophy volume)
- ACSM, *FITT Recommendations for Hypertension* update (aerobic most days; resistance 2–3 days, 2–4 × 8–12 on major groups; RPE bands)
- NSCA / common programming practice: progressive overload and volume load (weight × reps × sets), with the junk-volume caveat
- Strong, Hevy, Fitbod, Apple Fitness, Google Fit, Garmin Connect, Strava — consumer logger patterns (last time, rest timer, PRs, weekly minutes)
- Existing in-app reviews: `docs/cx-improvements.md` (composer already shipped), `docs/bp-improvements.md` and `docs/weight-tracking-analysis.md` (named standard, stay-local phasing)
