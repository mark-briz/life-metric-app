# Life Metric CX Improvement Proposal

This document reviews the current customer experience (CX) and proposes a consistent, easier interface for **Home**, **Blood Pressure**, **Weight**, **Workout**, and **Profile**. Every capability that exists today is listed in [Functionality that must stay](#functionality-that-must-stay) and must remain available in the new CX.

The bias is **ease of use**: fewer decisions, one obvious next action, and the same interaction pattern in every major area. Workout is the highest-priority fix.

---

## Why this needs work

The app already has a recognizable shell: five tabs, a dashboard, cards, and add/edit/delete for readings. That foundation is sound. The experience breaks down in three places.

1. **The four major areas do not share one interaction model.** Blood pressure and weight use “hub + add/edit form + confirm delete.” Workout uses two overlapping screens (detail *and* edit) plus an entry form that saves on every keystroke. Profile auto-saves personal settings on the same page as data-destroying actions.
2. **Workout is a maze.** Logging a session requires a plan (buried under Profile), immediately creates an empty workout, then asks the user to choose between “open” and “edit.” Date and plan live on one screen; sets live on both. Changing a plan silently wipes entries.
3. **The primary action is hard to find.** The add control is a small circle in the top-right, not the bottom-right FAB people expect on Android. Empty states sometimes dead-end (for example “Create a plan first” with no path to create one).

Industry-standard health and fitness apps (Apple Health, Google Fit, Strong, Hevy, Fitbit) all use the same basic loop: **see the latest value → scan a trend → add a new record → edit that record in place**. Life Metric should use that loop everywhere.

---

## Design principles

These are the CX methods the new interface should follow.

| Principle | What it means here |
|---|---|
| **Jakob’s Law** | Match patterns people already know: bottom tabs, hub + form, FAB to add, tap a row to open/edit, confirm before delete. |
| **Consistency and standards** | One screen template, one form template, one list-row template, one empty-state template across BP, Weight, Workout, and Profile. |
| **Recognition over recall** | Show previous weight/reps, last plan, last reading. Do not make users remember where plans live or which icon means “open” vs “edit.” |
| **Progressive disclosure** | Hub shows latest + trend + recent list. Advanced settings (moving average, theme, import) live one tap deeper. |
| **Hick’s Law** | One primary action per screen. Secondary actions (edit, delete, manage plans) are visible but quieter. |
| **Error prevention** | Do not persist empty workouts. Confirm when a plan change would wipe sets. Confirm import and erase. |
| **Visibility of system status** | Explicit Save on record forms. “Saved” feedback on settings. Never silently discard or overwrite. |
| **User control** | Cancel reverts. Back never leaves mystery drafts. Undo or confirm for destructive actions. |
| **Jobs to be done** | Fast log after a workout or reading; later, review trends and adjust goals. Optimize the log path first. |

---

## Functionality that must stay

The new CX is a presentation and flow change. It must not drop capabilities.

### App shell and first run

- Boot splash (“Life Metric”)
- One-time welcome + health disclaimer (must scroll, then checkbox, then Get started)
- Disclaimer acceptance persisted on device
- Five tabs: Home, BP, Weight, Workout, Profile
- Tabs hidden on nested screens
- Four themes (Mint, Ocean, Sunset, Dark) with live preview swatches
- All data stored only on this phone, protected by the device lock
- Starter plans on first launch: **Upper Body Strength** and **Lower Body Focus**, each with their current activities

### Home / Dashboard

- Latest BP (systolic/diastolic + heart rate)
- Latest weight, vs-prior delta, and date
- Workout sessions in last 30 days, last session relative date
- Blood pressure gauge, AHA-style status badge, crisis copy when applicable
- BP trend chart (systolic, diastolic, trailing moving average)
- Weight value, vs-prior delta, weight history chart with goal line
- BMI gauge (current + goal needle) when height is set; prompt to set height when it is not
- Workout chips: sessions (30d), sets logged (30d), last session + plan name
- Weekly session bar chart (last 8 weeks)
- Tapping a dashboard section opens that tab

### Blood pressure

- Add / edit / delete readings
- Fields: date, time (default now), systolic, diastolic, heart rate
- Validation: required date/time; whole numbers ≥ 1; diastolic lower than systolic
- Status classification: Normal, Elevated, Stage 1, Stage 2, Hypertensive Crisis
- Crisis warning: “Seek emergency care if symptoms occur.”
- Most-recent hero card
- History chart of recent readings + trailing average
- Recent list (last 30), each with date/time, values, status badge, edit, delete
- Confirm delete with the reading’s date, time, and values
- Missing-record screen if a reading was deleted

### Weight

- Add / edit / delete scale readings
- Fields: date, time (default now), weight in lb
- Weight stepper (− / + by 0.1 lb) and free-text decimal entry
- Suggested weight defaults to the latest reading (or 150.0 if none)
- Rounding to the nearest tenth
- Last-recorded hero card
- Full history chart with dashed goal line when a goal exists
- Weight target progress: start, percent complete, target, target-to-lose, weight lost
- Empty copy when start reading or target is missing, pointing at Profile
- BMI gauge when height is set; otherwise prompt to set height on Profile
- Recent readings (last 30) with up / down / no-change arrows vs the previous reading
- Confirm delete; if the deleted reading was the goal start, clear that start id
- Missing-record screen

### Workout logging

- List sessions grouped by date
- Create, open, edit, and delete a session
- Session date
- Attach a fitness plan; changing plan currently clears entries (must remain possible, but confirm first)
- Activity-type-aware labels: Add set / Add segment / Add block / Add note / Add entry
- Entry schemas:
  - **load**: weight + repetitions; prefill from previous load for the same plan + activity
  - **distance_time**: distance (mi) + time (`mm:ss` or minutes)
  - **duration**: minutes
  - **sport**: minutes + optional intensity (easy / moderate / hard)
  - **interval**: rounds, work seconds, rest seconds
  - **note**: optional free-text note
- Extra cycling metrics: resistance, average speed (mph), average RPM, average / max heart rate
- Tabata default suggestion (8 rounds, 20s work, 10s rest) should be offered when that type is used
- Group multiple sets of the same activity
- Entry summaries (e.g. `135 lb · 8 reps`, `2.5 mi · 22:10`, `4 rounds · 20s work · 10s rest`)
- Session summary line on the list
- Confirm delete of a session (plan name, date, entry count) and of an entry
- Block adding entries until a plan is selected
- “Create a plan first” when no plans exist — but the new CX must provide a button to create one
- Missing-workout / missing-entry / plan-required screens

### Plans and activities (today under Profile)

- List plans with type label, description, and activity count
- Create / edit plan: name, activity family, activity type, optional description
- All current families and types in `ActivityCatalog` (strength, cardio, mobility, conditioning, sports, recovery)
- Plan detail: list activities, add / edit / delete activity (name + optional description)
- Confirm remove activity
- Name required for plan and activity

### Profile, data, and legal

- Height in feet + inches (valid range 3'0"–8'0")
- Weight goal (lb) with − / + 0.1 stepper
- Starting weight picker from existing scale readings (disabled until a reading exists; can clear selection)
- BP moving-average window, 2–60 readings, default 10, − / + stepper
- Theme picker
- Export all data as JSON (share sheet)
- Import JSON with confirmation summarizing plan / workout / weight / BP counts
- Import replaces profile, plans, workouts, weight, BP, and theme
- Double-confirm erase all data; erase resets to starter plans and keeps disclaimer acceptance
- Health disclaimer screen
- About screen (app name, version, on-device storage note)
- Privacy copy: data stays on this phone and is protected by the device lock
- Validation errors for height, goal, and BP window

### Charts, gauges, and domain rules

- BP gauge zones and needle
- BMI categories and dual needles (current + goal)
- Weight chart goal line
- Workout weekly bars
- Relative dates: Today / Yesterday / *n* days ago
- Pace calculation for distance + time (keep available for summaries)

---

## What is inconsistent today

Use this as the gap list the new CX closes.

| Pattern | Blood pressure | Weight | Workout | Profile |
|---|---|---|---|---|
| Hub layout | Hero + chart + recent list | Hero + chart + goal + BMI + recent list | Date groups only; no hero or chart | Long single page of settings |
| Add control | Top-right + button | Top-right + button | Top-right + button | None (plans have their own) |
| Open vs edit | Tap icons only; no row tap | Same | Tap row = detail; pencil = different edit screen | Tap plan name = edit metadata; tap count = activities |
| Form save | Cancel + Save | Cancel + Save | Session metadata saves instantly; entries save on every keystroke | Personal fields save on every keystroke |
| Form title | Add / Edit reading | Record / Edit weight | Always “Edit workout” / “Edit entry,” even when new | Mix of inline and nested |
| Empty state | CTA to add first reading | CTA to record weight | CTA to create workout, but plan setup is elsewhere | CTA on plans only |
| Delete | Confirm dialog | Confirm dialog | Confirm on list and detail; entry delete on form has no confirm | Double confirm for erase |
| Related setup | BP window lives on Profile | Goal + start + height live on Profile | Plans live on Profile | Owns everyone else’s setup |

Workout-specific confusion (highest pain):

1. **Two screens do the same job.** `WorkoutDetailScreen` and `WorkoutFormScreen` both list entries and allow add/edit/delete. Only the form can change date or plan. Users cannot tell which they need.
2. **New workout is created before the user commits.** `createEmptyWorkout()` writes a session immediately. Backing out leaves an empty “Upper Body Strength · 0 entries” row.
3. **Plans are not part of Workout.** The first-time path is Workout → “Create a plan first” → no button → Profile → Workout Plans → New plan → family → type → save → add activities → back to Workout.
4. **Autosave on the entry form** writes invalid or half-typed values as the user types. There is no Save, and Cancel/Back cannot undo.
5. **Changing the plan wipes every entry with no warning.**
6. **Vocabulary is overloaded:** plan, workout, activity, machine (in data), set, segment, block, entry, schema, family, type.
7. **Previous load is invisible.** Weight/reps are silently filled. Users do not know they can trust or override the suggestion.
8. **List rows are crowded.** Label, summary, edit, and delete compete on one line.

---

## The shared CX system

Every major area uses the same three building blocks. This is the consistency requirement.

### 1. Hub screen (tab)

```
Title
One-line purpose

┌─────────────────────────────┐
│ HERO                         │
│ Latest value · when          │
│ Status / delta / plan        │
└─────────────────────────────┘

┌─────────────────────────────┐
│ INSIGHT                      │
│ Chart, gauge, or progress    │
└─────────────────────────────┘

HISTORY
┌ row ┐  tap to open/edit
┌ row ┐
┌ row ┐

                    [ + Add ]
```

Rules:

- **Title** is the area name: Blood pressure, Weight, Workouts, Profile.
- **Hero** is the newest record, or an empty-state card with one button.
- **Insight** is the chart or progress that makes the history useful.
- **History** is a reverse-chronological list. Tap the row to open/edit. Delete is a trailing action or overflow, not a second competing destination.
- **Add** is a Material 3 FAB above the tab bar (bottom-end), labeled for accessibility. Remove the top-right circle.
- Pull the same hero / insight / history blocks onto Home as tappable summaries (already the case; keep it).

### 2. Record form (add or edit)

```
[ ← Area name ]

Add {record}  |  Edit {record}
Short help only when new.

Fields (date/time first, then values)

[ Cancel ]     [ Save ]
```

Rules:

- New and edit are the **same form**. Title is the only difference.
- Date and time default to now. Date-only for workout sessions (keep today’s behavior).
- **Cancel** or Back discards unsaved field changes.
- **Save** validates, then writes, then pops back to the hub (or session).
- Inline field errors, same component as today (`LabeledField`).
- Missing-record screens stay, with a Back action to the hub.

Use this form for: BP reading, weight reading, workout session composer (see below), plan, activity, and workout entry.

### 3. History row

```
Primary          135 / 82 mmHg
Secondary        Tue, Oct 21 · 7:14 AM
Trailing         [status or delta]   ⋮
```

Rules:

- The **row is the hit target**. Tap = edit that record (or open the session composer for a workout).
- Trailing content is status (BP), change arrow (weight), or set count (workout).
- Overflow (⋮) or swipe-to-delete for destroy. Always confirm, using the same dialog copy style as today.
- Do not put two icons that navigate to two different screens.

### 4. Empty state

Always three parts: **what’s missing**, **why it matters**, **the one button that unblocks the user**.

If the unblock action lives in another area (height, plan, starting weight), the button navigates there. Never leave a dead end.

### 5. Settings group (Profile)

Profile is not a hub of readings. It uses the Android/iOS settings pattern:

```
PERSONAL
  Height                  5' 10"     >
GOALS & CHARTS
  Weight goal             175.0 lb   >
  BP average window       10         >
APPEARANCE
  Theme                   Mint       >
WORKOUTS
  Plans                   2          >
DATA
  Export / Load / Erase
LEGAL & APP
  Disclaimer / About
```

Tap a row to open a focused editor. Auto-save is fine **inside** a small settings editor after a valid value; do not mix auto-save fields with Erase on one scrolling page.

---

## Workout CX (priority redesign)

Goal: logging a session should feel like Strong / Hevy / Apple Fitness — **pick a plan, add sets, done** — without losing plans, schemas, previous load, or cycling metrics.

### Target flow

```
Workouts hub
    │
    ├─ [ + Log workout ] ──► Session composer (new)
    │                            │
    │                            ├─ Date
    │                            ├─ Plan  (Manage plans →)
    │                            └─ Entries grouped by activity
    │                                 └─ Add set / segment / block
    │                                        └─ Entry form (Save / Cancel)
    │
    └─ tap a past session ──► same Session composer (edit)
```

Delete `WorkoutDetailScreen` as a separate destination. Detail and edit become **one session composer**. That single change removes the “open vs edit” confusion.

### Hub

Match BP and Weight.

- **Hero:** last session date, plan name, activity-type label, summary (`3 sets`, `2.4 mi · 24:10`, etc.).
- **Insight:** keep the 30-day chips and the 8-week bar chart (move them from Home-only into the Workout tab as well).
- **History:** date headers, then one row per session. Tap opens the composer. Overflow deletes (same confirm copy as today).
- **FAB:** “Log workout”.

If there are no plans, the empty state button is **Create a plan** (navigates to the plan form), not “Create your first workout.”

### Session composer (replaces detail + form)

This is the workout record form.

1. **Date** (picker, default today). Changing date saves the session once it exists.
2. **Plan** dropdown showing `Name · Type`. A text button beside it: **Manage plans**.
3. **Entries**, grouped by activity, with Set 1 / Set 2 when there are multiples (keep current grouping).
4. Primary button using the catalog label: **Add set** / **Add segment** / **Add block** / **Add note**.
5. App bar action **Done** returns to the hub.

Draft rule (error prevention):

- Do **not** write a workout to storage when the user taps Log workout.
- Keep the session in memory until the user sets a plan **and** either saves the first entry or taps Done with at least one entry.
- If they Back out with no entries, discard silently (nothing to lose).
- If they Back out with unsaved entry-form changes, confirm.
- If a plan change would clear existing entries, confirm: “Switch to *Lower Body Focus*? This removes 8 sets from this session.” Cancel keeps the current plan.

New vs edit title: **Log workout** / **Edit workout** (never “Edit workout” for a new session).

### Entry form

Match the BP/Weight save model.

- Title: **Add set** (or segment/block/note) / **Edit set**.
- Activity/segment picker first.
- Schema fields next, then extra metrics (cycling resistance before distance; speed / RPM / HR after).
- **Previous load** as a visible suggestion, not a silent overwrite:

  > Last time: 50 lb × 12. [Use]

  Tapping Use fills the fields. The user can ignore it.
- Tabata: when the activity type is Tabata and fields are empty, prefill 8 / 20 / 10 and say so.
- **Cancel** discards. **Save** validates and writes.
- Stop calling `persist()` on every keystroke.

### Plans live with workouts

Plans are a workout concept. Keep them reachable from Profile, but the primary path is inside Workout.

```
Workouts hub → Manage plans → plan list
                               ├─ tap row → plan (activities)
                               ├─ overflow → edit plan name/type
                               └─ FAB → new plan
```

On the plan list, **one tap opens the plan** (activities). That matches how a workout row opens the session. Editing name / family / type is overflow or a pencil on the plan screen — not a different destination from tapping the title.

The current two-dropdown create flow (family, then type) stays; it is how 40 activity types stay scannable. Add one line of helper text: “This chooses what you log: weight and reps, distance and time, rounds, or a note.”

When a plan has no activities, the empty state is **Add an activity**, with an example: “Chest Press, Easy run, 20-minute stretch.”

### Vocabulary (use everywhere)

| Say | Do not say in the UI |
|---|---|
| Plan | Template, program (unless we add those later) |
| Workout or session | — |
| Activity | Machine |
| Set / Segment / Block / Note | Entry (except in generic counts: “3 entries”) |
| Log workout | New workout, Create workout |

Keep catalog-driven labels (`Add set` vs `Add segment`) — they teach the schema without a tutorial.

### First-time happy path (must be possible in under a minute)

1. Open Workout.
2. Starter plans already exist, so the hub is not empty of plans.
3. Tap **Log workout**.
4. Plan defaults to the last-used plan, or the first starter plan.
5. Tap **Add set**. Activity defaults to the first activity (or last-used on that plan). Previous load appears if any.
6. Enter weight and reps. Save.
7. Done.

If the user deleted all plans, step 2 is **Create a plan** from the same tab.

---

## Blood pressure CX

Keep the current information architecture; restyle it onto the shared hub + form.

### Hub

- **Hero:** `118 / 76 mmHg · 64 bpm`, status badge, date/time. Crisis warning stays in the hero when status is Crisis.
- **Insight:** the existing chart and “Systolic · diastolic · trailing average” caption. Add a one-line legend (solid = reading, faint = average) so the four lines are recognizable.
- **History:** last 30 readings as shared rows. Tap = edit. Overflow = delete (same confirm body as today).
- **FAB:** Add reading.

### Form

No field changes. Use the shared form chrome:

- Title Add reading / Edit reading.
- Date + time, systolic + diastolic on one row, heart rate.
- Cancel / Save.
- Keep all validation messages.

Optional ease-of-use (does not add new data): after a valid save, show the status badge on the form for a moment (“Stage 1 Hypertension”) so the user sees the classification before the hub appears. This is feedback, not a new feature.

### Setup that affects BP

The moving-average window stays configurable. Surface a short link under the chart: “Average of last 10 readings · Change” → Profile → BP average window. Users should not have to guess that this lives under Profile.

---

## Weight CX

Same hub + form as BP.

### Hub

- **Hero:** `182.4 lb`, date/time, vs-prior delta (green down / accent up, same meaning as today).
- **Insight, stacked in this order:**
  1. History chart (goal line when set).
  2. Weight target card (start, %, target, lost) **or** the existing empty cards that send the user to set start/target.
  3. BMI gauge, or the height prompt.
- **History:** last 30 readings with change arrows. Tap = edit. Overflow = delete.
- **FAB:** Record weight.

### Form

Keep date/time, − / + 0.1 lb stepper, decimal field, suggested last weight, Cancel / Save, and the same validation.

### Setup that affects Weight

Height, goal, and starting reading stay in Profile (they are profile data). On the Weight hub, empty goal/BMI cards must have a button, not only copy:

- “Set height” → Profile personal editor
- “Choose starting weight” → goal editor (disabled until a reading exists, same rule)
- “Set target weight” → goal editor

The Weight tab remains the place you *see* progress; Profile remains the place you *set* it.

---

## Profile CX

Profile is overloaded: identity, appearance, two other tabs’ settings, plan management, backup, legal, and a destroy action sit on one scroll. Split it into grouped settings without removing anything.

### Groups and destinations

| Group | Rows | Editor behavior |
|---|---|---|
| **Personal** | Height | Feet + inches fields, auto-save when valid, same 3'0"–8'0" rule |
| **Goals & charts** | Weight goal, Starting weight, BP average | Goal stepper + start picker; window stepper 2–60. Auto-save when valid |
| **Appearance** | Theme | Same four theme cards with swatches |
| **Workouts** | Plans | Existing plan list / form / activities, also linked from Workout |
| **Data** | Export data, Load data, Erase all data | Same share, file picker, import summary confirm, double-confirm erase |
| **Legal** | Health disclaimer | Existing full text |
| **App** | About | Name, version, on-device note |
| Footer | Privacy sentence about device lock | Stay visible on the Profile hub |

### Rules

- The Profile hub is a list of rows (reuse `SettingsRow`). No number fields on the hub itself.
- Export / Load keep their progress copy (“Preparing your JSON file…”, “Loading data…”).
- Erase stays visually dangerous (`DangerOutlineButton`) and **last**, after a confirmation pair.
- Import confirmation still lists plan, workout, weight, and BP counts and still replaces everything on the device.
- Height and goal validation messages stay; show them on the small editor, not a buried line at the bottom of a long page.

---

## Home and first run

### Welcome

Keep the legal gate (scroll, accept, Get started). After accept, do **not** dump users into a blank dashboard with no next step.

Add a **first-session coach card** on Home only while all three logs are empty (it disappears once any reading or workout exists):

1. Record blood pressure
2. Record weight (unlocks BMI after height)
3. Log a workout (starter plans are ready)

Each line is a button into that area’s add form. This is onboarding, not new functionality.

### Dashboard

Keep the three tappable sections and the top chips. Align them with the hub language:

- Same hero numbers as each tab.
- Same empty copy.
- Add a compact **Log** affordance on each card (BP, Weight, Workout) so a return visit can add without switching tabs first. Destination is the same add form the tab FAB uses.

Do not add new metrics to Home that the tabs do not already compute.

---

## Shared UI details

These make the four areas feel like one product.

### Navigation

- Keep the five-tab bar and current routes conceptually (home, bp, weight, workouts, profile).
- Nested screens: back label is the parent area name (“Blood pressure”, “Workouts”, “Profile”).
- Workout plans opened from Workout back to Workouts; opened from Profile back to Profile.
- Hide tabs on forms, as today.

### Add button

- Material 3 FAB, bottom-end, above the nav bar.
- Icon + content description (`Add reading`, `Record weight`, `Log workout`, `New plan`, `Add an activity`).
- Do not place a second full-width primary button on hubs that already have a FAB, except inside an empty state.

### Feedback

- After Save on a form, pop back immediately (today’s behavior).
- After auto-save settings, a subtle “Saved” is enough.
- After delete, stay on the list. Optional: snackbar Undo for reading/session delete (confirm dialog can remain). Import and erase stay confirm-only — they are too large for undo.

### Charts

- Keep current drawing and math.
- Workout bars: add a light Y-axis or a “sessions” caption so a tall bar is readable.
- BP chart: keep the four series; add the short legend noted above.
- Charts stay display-only (no new drill-in required).

### Accessibility

- Every icon button already needs a content description; keep that and extend it to the new FAB and overflow.
- Minimum 48 dp hit targets on rows, FAB, and steppers.
- Do not use color alone for weight up/down or BP status (keep the arrow / badge label).
- Honor the selected theme for contrast, including Dark.

### Copy tone

Short, concrete, present tense. Lead lines are one sentence. Error text says how to fix the field. Prefer “Record weight” / “Add reading” / “Log workout” over “New” / “Create” / “Submit.”

---

## Proposed information architecture

```
Welcome (first launch only)
 └── Home
      ├── Blood pressure
      │    ├── Add / Edit reading
      │    └── (link) BP average window → Profile
      ├── Weight
      │    ├── Record / Edit weight
      │    └── (links) Height, goal, start → Profile
      ├── Workouts
      │    ├── Log / Edit session
      │    │    └── Add / Edit set|segment|block|note
      │    └── Plans
      │         ├── New / Edit plan
      │         └── Plan activities
      │              └── Add / Edit activity
      └── Profile
           ├── Height
           ├── Weight goal + starting weight
           ├── BP average window
           ├── Theme
           ├── Plans (same graph as Workouts → Plans)
           ├── Export / Load / Erase
           ├── Disclaimer
           └── About
```

No feature moves off the product. Setup that other tabs depend on is **linked from those tabs**, not only discoverable by hunting in Profile.

---

## Implementation phases

Ship in this order so Workout gets better before visual polish.

### Phase 1 — Workout clarity (highest impact)

- Merge detail + edit into one session composer.
- Stop creating empty workouts on FAB tap; persist on first saved entry or Done.
- Confirm plan change when entries would be cleared.
- Entry form: Save / Cancel; visible previous-load suggestion; Tabata defaults.
- “Manage plans” from the composer and hub; empty state CTA to create a plan.
- Unify titles: Log workout / Edit workout / Add set / Edit set.

### Phase 2 — Shared chrome

- Bottom FAB on every hub.
- Shared history row (tap to edit, overflow to delete).
- Shared empty-state component with a required action.
- Profile grouped into settings rows + small editors.
- Links from BP/Weight insights to the settings they depend on.

### Phase 3 — Home and first-run ease

- First-session coach card.
- Dashboard Log actions.
- Chart legends / workout Y-axis caption.
- Optional delete Undo snackbar.

### Phase 4 — Polish

- Done/Saved feedback.
- Hit targets and content descriptions pass.
- Copy pass using the vocabulary table.
- Verify export/import/erase and all schemas still behave as in the inventory.

---

## Acceptance checks

The new CX is done only when all of the following are true.

1. A new user can accept the disclaimer, record BP, record weight, set height and a weight goal, and log a strength session with two sets — without leaving a mystery empty workout and without opening a second “edit” screen to change the date.
2. Blood pressure, weight, and workout hubs all show **hero → insight → history → FAB**.
3. Every add/edit form for a record has **Cancel and Save** (or equivalent Done for a session that is already persisting entries).
4. Plans can be created from the Workout tab. Profile still lists the same plans.
5. Changing a plan with existing sets asks first. Back on a new empty session writes nothing.
6. Previous load, cycling extra metrics, interval fields, sport intensity, notes, starter plans, charts, gauges, BMI, goal progress, themes, export, import, erase, and disclaimer are all still reachable.
7. Destructive actions still confirm. Import still summarizes counts. Erase still asks twice.
8. A user who only ever used the old Workout detail screen can do the same jobs on the single session composer.

---

## Out of scope

These would help later but are not required to make the CX consistent or to keep current functionality:

- Accounts, cloud sync, or login
- Units other than lb / mmHg / miles
- Social, coaching, or notifications
- New activity types or new metrics beyond what `ActivityCatalog` already defines
- Interactive chart scrubbing or date-range filters
- Using unused `firstName` / `lastName` profile fields

If those are added later, they should adopt the same hub / form / settings patterns above.
