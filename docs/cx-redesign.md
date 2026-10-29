# Life Metric CX Redesign Proposal

This document reviews the current Android app (October 2026 build) and proposes one consistent, easy-to-use customer experience (CX) across **Blood Pressure**, **Weight**, **Workout**, and **Profile**, plus Home. It follows [docs/cx-improvements.md](cx-improvements.md), most of which has shipped: the bottom-end FAB, tap-to-edit history rows, the in-memory workout draft, plan-switch confirmation, and the "Last time … Use" suggestion.

Since then the app has grown a lot: reminders, how-to guides, BP targets and paired readings, tags and notes, kg display, waist, rest timer, RPE, set types, history filters, sharing, PRs, and undo. Each area added these in its own way, so the four areas no longer feel like one product, and the workout session screen is the hardest place to get oriented.

**Every capability in the app today must remain reachable.** The [functionality preservation map](#functionality-preservation-map) lists each one and where it lives in the new CX.

---

## Summary

1. **Use one frame on every screen.** Add a Material 3 top app bar with a back arrow, title, and one overflow menu. All tertiary actions (How to …, Share …, Reminders, Settings) move into that menu, so they stop appearing as stacked text links inside chart cards.
2. **Use one hub template.** Each hub shows a Hero, then an Insight card, then a Goal card, then a History list with a single Filter button, plus a labeled Extended FAB. BP, Weight, and Workout follow it exactly, and Profile uses the settings template.
3. **Use one record form template.** Fields go in this order: When, Values, then a collapsed "Add details" section. Save sits in the top bar and at the bottom. Leaving with unsaved changes always asks first. Delete is in the form's overflow menu, and the save button always says **Save**.
4. **Give each area one settings page.** BP, Weight, and Workout each get an *Area settings* page. It opens from the hub's overflow menu and from Profile, so it's the same page reached two ways.
5. **Rebuild the workout session around plans and activities.** Start from a plan, see every activity in that plan as a card, tap **+ Add set** on the card, and enter the set in a bottom sheet prefilled from the last set. A sticky rest-timer bar and a **Finish** button stay on screen. Session details (date, duration, note) fold into one tappable summary line, and plan changes move to the overflow menu.

---

## Review method

The review used standard CX evaluation methods, applied to the code in `app/src/main/java/com/markbriz/lifemetric/ui`.

| Method | How it was applied |
|---|---|
| **Heuristic evaluation (Nielsen's 10)** | Each screen was scored for consistency, error prevention, visibility of status, recognition over recall, and minimalist design. Findings are rated by severity, from 1 (cosmetic) to 4 (blocks the task). |
| **Material Design 3 conformance** | Checked app bars, FABs, bottom sheets, filter chips, segmented buttons, snackbars, and full-screen dialogs against Android conventions, since users already know them (Jakob's Law). |
| **Task analysis** | Walked the core jobs end to end: record a BP reading, record a weight, log a two-set strength session, edit a past session, set a goal, back up data. |
| **Jobs to be done** | (1) Log the thing that just happened, fast. (2) See whether it is getting better. (3) Adjust goals and reminders occasionally. The design optimizes the jobs in that order. |
| **Competitive patterns** | Compared against Apple Health, Google Fit, Withings, Omron Connect, Strong, and Hevy for logging and hub patterns. |
| **WCAG 2.2 AA** | Checked timing, target size, color-only meaning, text alternatives for charts, and focus order. |

---

## What already works (keep)

- Five-tab shell with tabs hidden on nested screens.
- Tap a history row to edit, use the overflow menu to delete, confirm the delete, and offer Undo in a snackbar.
- Drafts that don't pollute history. Backing out of a new workout with no sets writes nothing.
- Schema-aware workout fields: sets, segments, blocks, notes, and cycling metrics.
- "Last time" with a **Use** button on the workout entry form.
- Named standards: AHA categories, CDC/NHLBI BMI, HHS weekly targets.
- Empty states that always offer a way forward (for example Set height, Create a plan, Choose starting weight).
- Local-only data with JSON export, import, and erase.

---

## Findings

### Cross-cutting inconsistencies

| # | Pattern | Blood pressure | Weight | Workout | Profile / plans | Sev |
|---|---|---|---|---|---|---|
| C1 | **Tertiary actions** | "How to measure", "Share report", "Reminders" are stacked text links inside the chart card | "How to weigh", "Share log", "Reminders", "Display units" in the chart card, repeated in the empty state | "How to log", "Share log", "Reminders" in the stats card. The composer adds "How to log", "Share session", "Manage plans" | n/a | 3 |
| C2 | **Hub section order** | Hero → Visit summary → History (chart) → Recent readings | Hero → History (chart) → Weight target → BMI → Recent readings | Manage plans row → Hero → This week → History (chips + 2 charts) → filters → sessions | Long list of 17 rows | 3 |
| C3 | **"History" means different things** | Chart | Chart | Stat chips + charts + links | n/a | 2 |
| C4 | **Range chips** | `7 · 10 · 30 · All` = *readings* | `7 · 30 · 90 · All` = *days*, shown twice (chart and list) with the same labels | `30d · All` mixed into one row with plan and family chips | n/a | 3 |
| C5 | **Save button label** | "Save reading" / "Save changes" | "Save weight" / "Save changes" | Entry: "Save". Session: "Done" | Plan: "Create plan" / "Save changes". Activity: "Add activity" / "Save activity" | 2 |
| C6 | **Cancel and unsaved changes** | Cancel, no confirm when leaving with edits | Cancel, no confirm | Entry form confirms discard. The session composer silently drops a draft's note and duration | Plan and activity forms have no Cancel and no confirm | 3 |
| C7 | **Save model** | Explicit Save | Explicit Save | Entries use explicit Save, but session date, duration, note, and rest-timer preference save **on every keystroke** | Editors autosave per keystroke, with errors appearing mid-typing | 3 |
| C8 | **Delete from the edit form** | Not available | Not available | Set delete is a trash icon in the composer | Activity: overflow on list only. **No way to delete a plan** | 2 |
| C9 | **Undo** | Undo | Undo | Session: Undo. Set delete: confirm only | Activity remove: confirm only | 2 |
| C10 | **Screen header** | Eyebrow "Vitals" | Eyebrow "Body weight" | Eyebrow "Workout log" | Eyebrows "App", "Settings · Fitness plans" | 1 |
| C11 | **Back navigation** | A text link inside the scrolling content, so it scrolls away. No top app bar anywhere | same | same | same | 2 |
| C12 | **FAB** | Icon-only "+" | Icon-only "+" | Icon-only "+", **hidden** when no plans exist | Icon-only | 2 |
| C13 | **Where settings live** | BP target, average window, reminders: Profile (linked from the hub) | Goal, start, units, waist, reminders, height: Profile (linked) | Plans: hub row + Profile. Rest timer: inside each session. Reminders: Profile | Profile owns every other area's settings | 3 |
| C14 | **"Last time" suggestion** | **Prefills** systolic, diastolic, and HR with the previous reading, so saving untouched records old numbers | Hint text only, field empty | Hint + **Use** button | n/a | 3 |
| C15 | **Save feedback** | Full "Saved" screen that **auto-dismisses after 1.4 s** (fails WCAG 2.2.1, timing adjustable) | Snackbar "Saved" | Snackbar "Saved" | Inline "Saved" text that fades | 2 |
| C16 | **Chart accessibility** | Line chart has no text alternative | Line chart has no text alternative | Bar charts have no text alternative | n/a | 2 |

### Blood pressure

- **B1 (sev 3):** Prefilling the form with the previous reading (C14) invites accidental duplicate readings. It's also the only area that does this.
- **B2 (sev 2):** The form is one long card: How to measure, date/time, time-of-day chips, previous-reading line, reading, HR, second-reading link and timer, live warnings, tags, note, then buttons. The optional context (tags, note) has the same visual weight as the required fields.
- **B3 (sev 2):** The BP gauge and AHA category zone chart appear only on Home, not on the BP tab.
- **B4 (sev 2):** The "Check this reading" warning reuses the delete dialog component, so the copy and button styling read like a destructive action.
- **B5 (sev 1):** The visit summary card mixes three ideas: the 7-day average, morning vs evening, and target attainment.

### Weight

- **W1 (sev 2):** The hero changes its label and meaning between "LAST RECORDED WEIGHT" and "7-DAY TREND" depending on data count.
- **W2 (sev 2):** Chart range and list range use identical chips with different jobs (C4).
- **W3 (sev 2):** "Waist" is a text link under the BMI gauge, while height is in Profile. Both are body measurements, but they sit in two places.
- **W4 (sev 1):** "How to weigh" is at the bottom of the form, while BP puts "How to measure" at the top.

### Workout (highest pain)

The pieces are all there, but the session screen is hard to read and to operate.

- **K1 (sev 4): The session screen is upside down.** The first card holds date, duration, note, rest-timer chips, the rest countdown, How to log, Share session, the **plan picker**, Manage plans, and the Add button, in that order. The most consequential choice (plan) is ninth, and it sits below a global preference (rest timer). The sets the user is actually logging appear *below* all of it.
- **K2 (sev 4): One Add button for the whole session.** Every set goes through: Add set (top of the screen) → full-screen form → pick the activity from a dropdown → fill in fields → Save → back to the composer → scroll up to Add set again. After a few sets the Add button and the rest timer have scrolled off screen.
- **K3 (sev 3): Mixed save models on one screen.** Typing in Duration or Note on a saved session writes to storage on every keystroke. Sets use an explicit Save. On a *new* session, a typed note or duration is silently discarded on Back if no set was saved yet.
- **K4 (sev 3): The rest timer is in the wrong place.** Its preference (Off/60/90/120) is changed from inside every session, and the countdown shows at the top of the first card, which is scrolled away while the user logs sets.
- **K5 (sev 3): Set rows are crowded.** Each row shows "Set n", a summary, a pencil, and a trash icon. The edit and delete targets sit next to each other, and the pattern differs from every other list in the app (tap the row, overflow to delete).
- **K6 (sev 3): The hub leads with setup.** "Manage plans" is the first element, above the last session. The stats card repeats the hero ("Last session") and buries three links. One chip row mixes date range, plan, and family filters.
- **K7 (sev 2): The plan model is never explained in context.** A plan is a routine *and* a single activity type. Users only find that out when switching plans wipes their sets. Plan detail has no "Edit plan" or "Start workout", and plans cannot be deleted.
- **K8 (sev 2): The vocabulary leaks.** Yoga and sport plans say "Add block". The activity field is labeled "Segment" for runs and "Activity" otherwise. History rows lead with the type ("Strength Training · Upper Body Strength") rather than the plan name the user picked.
- **K9 (sev 2): The entry form gives everything equal weight.** Required load fields, set-type chips, RPE, and up to five cycling metrics all appear at the same level.
- **K10 (sev 2): "Done" has no clear meaning.** It sits at the bottom of a long scroll. On a draft with no sets it silently discards, and on a saved session it only navigates back.

### Profile

- **P1 (sev 3):** Profile is a 17-row scroll mixing personal data, three areas' goals and reminders, appearance, plans, backup, legal, and a destructive action.
- **P2 (sev 3):** Text editors (height, goal, BP target, waist, window) save on every keystroke and show validation errors while the user is still typing (for example a 2-digit target).
- **P3 (sev 2):** "Display units" sits under Weight goals, but it also changes lift weights in Workout.
- **P4 (sev 1):** "Erase all data" sits between About and the privacy footer rather than at the end of the Data group.

### Home

- **H1 (sev 2):** The top chip row repeats what the three cards below show.
- **H2 (sev 2):** The BP card stacks a gauge, badge, delta, derived values, date, HR, 7-day line, trend chart, and zone chart, which is longer than the BP hub hero.
- **H3 (sev 1):** "Get started" disappears after the first record of *any* kind, so it never guides the remaining two.
- **H4 (sev 1):** "Log" is a text link. Its tap area is fine, but it looks like a secondary link rather than an action.

---

## The shared CX system

Every major area is built from the same seven parts. Consistency means the same part, in the same place, with the same behavior.

### 1. Screen frame

```
┌──────────────────────────────────────────┐
│ ←  Title                         [⋮]     │  Material 3 top app bar (pinned)
├──────────────────────────────────────────┤
│  content (scrolls)                       │
│                                          │
│                        [ + Add reading ] │  Extended FAB (hubs only)
├──────────────────────────────────────────┤
│  Home   BP   Weight   Workout   Profile  │  Navigation bar (hubs only)
└──────────────────────────────────────────┘
```

- **Top app bar** on every screen. Hubs have no back arrow. Nested screens use ← with the parent's name as the accessibility label ("Back to Blood pressure"). The bar stays pinned, so Back never scrolls away (fixes C11).
- **Drop the eyebrow and the one-line lead on hubs.** The tab already tells the user where they are. Keep a lead only on forms and guides, where it adds instructions (C10).
- **Overflow menu (⋮)** holds tertiary actions in the same order everywhere (fixes C1):

| Area | Overflow items (in order) |
|---|---|
| Blood pressure hub | How to measure · Share report · Blood pressure settings |
| Weight hub | How to weigh · Share log · Weight settings |
| Workout hub | Plans · How to log · Share log · Workout settings |
| Any record form (edit) | How to … · Delete |
| Workout session | Session details · Change plan · Share session · How to log · Delete workout |
| Plan detail | Edit plan · Delete plan¹ |

¹ Plan delete does not exist today. It's listed here as a recommended addition, with a confirm that names how many sessions reference the plan. It's not required to preserve current behavior.

- **Extended FAB** (icon + label) on hubs: **Add reading**, **Record weight**, **Log workout**. On Plans: **New plan**. On Plan detail: **Add activity**. The Workout FAB is always visible. With no plans, it opens the plan sheet's empty state ("Create a plan") rather than disappearing (fixes C12).

### 2. Hub template

```
HERO          latest value · status/delta · when
INSIGHT       one chart card with a labeled range control
GOAL          target / progress card (or empty state with one button)
HISTORY       [Filter ▾] [active chip ×] ...
              row  row  row ...   Show older
```

Rules:

- **Hero** uses one component (`HeroCard`) in all three areas and on Home. It shows the big value, one status line (badge or delta), and when it was recorded. Its label never changes meaning (fixes W1).
- **Insight** has one chart per card, with a **segmented button** whose labels include the unit: `Last 7 · 10 · 30 readings · All` (BP), `7 · 30 · 90 days · All` (Weight), `Sessions · Volume` and `8 weeks` (Workout). Each chart has a text alternative summarizing the trend (fixes C4, C16).
- **Goal** holds what the user is aiming for: BP target attainment, weight goal progress, and the HHS week. When nothing is set, the card is an empty state whose single button opens that area's settings.
- **History** uses `HistoryRow` everywhere, with the same tap and overflow behavior. Filtering moves into **one Filter button that opens a bottom sheet** (range, period, tags, plan, family). Active filters appear as dismissible chips above the list, and the user can clear all from there (fixes C4, K6).
- The section titles are always **Trend**, **Goal**, and **History**.

### 3. Record form template (add and edit)

```
┌──────────────────────────────────────────┐
│ ✕  Add reading                   Save    │  full-screen dialog app bar
├──────────────────────────────────────────┤
│ WHEN      Date · Time  [· Time of day]   │
│ VALUES    required numbers               │
│           Last time: 128 / 82 · 66 [Use] │
│ ▸ Add details   (tags, note, extras)     │  collapsed unless it has data
│                                          │
│ [ Cancel ]                  [ Save ]     │
└──────────────────────────────────────────┘
```

- **Same layout for add and edit.** Only the title changes: Add reading / Edit reading, Record weight / Edit weight, Add set / Edit set, New plan / Edit plan, Add activity / Edit activity (fixes C5).
- **The button always says Save.** Context comes from the title.
- **Last time is a suggestion, never a prefill.** Show "Last time: …" with **Use** in BP, Weight, and Workout. Typing or using the − / + stepper starts from the last value, as Weight's stepper does today (fixes C14, B1).
- **Optional context is collapsed** under "Add details": BP tags + note, weight tags + note, and workout set type, RPE, and optional extras. It opens automatically when editing a record that already has those values (fixes B2, K9).
- **Unsaved changes always confirm** on ✕, Cancel, or system Back: "Discard changes?" with the same wording everywhere (fixes C6).
- **Edit forms put Delete in the overflow menu**, using the same confirm and Undo as the hub (fixes C8).
- **Validation runs on Save and on leaving a field**, not on every keystroke. Error text says how to fix the problem, using the current messages.
- **Warnings that are not errors** (BP very high or very low) use a neutral `ConfirmDialog` ("Check this reading" with **Save anyway** and **Edit**), not the delete dialog (fixes B4).

### 4. Area settings page

One page per area, reached from the hub's overflow menu **and** from Profile. It's the same destination both ways, and Back returns to wherever the user came from (fixes C13).

| Page | Rows |
|---|---|
| **Blood pressure settings** | BP target · Average window · Reminders (morning / evening) |
| **Weight settings** | Weight goal & starting weight · Reminders · *Units* (shared) · *Height* (shared) · *Waist* (shared) |
| **Workout settings** | Plans · Rest timer default · Reminders · *Units* (shared) |

"Shared" rows edit the same Profile value, so the user can find them from whichever area they're in.

**One save rule for settings (fixes C7, P2):**

- **Toggles and choices apply immediately**: switches, theme, units, rest-timer default, reminder time pickers. Show the "Saved" snackbar.
- **Typed values use the record form template**: height, waist, weight goal, BP target, average window. That means Cancel / Save, validation on Save, and a discard confirm.

### 5. Empty state

Always three parts: **what's missing**, **why it matters**, and **one button**. If the fix lives elsewhere, the button goes there. Secondary links (How to …, Reminders) are not repeated in empty states, because they're already in the overflow menu.

### 6. Feedback

| Event | Feedback |
|---|---|
| Record saved | Return to the hub, briefly highlight the new or edited row, and show snackbar **Saved** |
| BP reading saved | Same, plus the category: snackbar **Saved · Stage 1 Hypertension** with a **Details** action that opens the categories dialog. This replaces the 1.4 s auto-dismiss screen, keeps the classification feedback, and passes WCAG 2.2.1 (fixes C15) |
| Setting changed | Snackbar **Saved** (replaces the inline fading text) |
| Record, set, session, or activity deleted | Confirm, then snackbar **Deleted · Undo**. The same applies to set and activity removal (fixes C9) |
| Import / erase | Confirm only (too large for undo); erase keeps its double confirm |

### 7. Vocabulary

| Say | Not |
|---|---|
| **Add reading** (BP), **Record weight**, **Log workout** | New, Create, Submit |
| **Save** | Save reading, Save weight, Save changes, Create plan, Add activity |
| **Plan**: a named routine of activities | Template, program |
| **Workout** (list, hub), **session** (in copy) | — |
| **Activity**, the same word for every schema | Machine, Segment (as a field label) |
| **Set** (load), **Segment** (distance), **Round set**² (interval), **Time** (duration, sport), **Note** | Block, Entry |
| **Finish** (end a session) | Done |
| **Trend**, **Goal**, **History** (hub sections) | Visit summary, Recent readings, This week (as section titles; keep them as card titles) |

² "Block" is opaque. If "Round set" tests poorly, keep **Block** for intervals only and stop using it for duration and sport.

---

## Workout CX (priority redesign)

### Mental model, stated once in the UI

> A **plan** is a list of activities you repeat, such as Upper Body Strength. A **workout** is one time you did a plan. Each activity gets **sets** (or segments, round sets, or time).

This sentence appears once on the plan picker sheet and in How to log. It also explains why changing plans clears sets.

### Target flow

```
Workout hub
  │
  ├─ [ + Log workout ] ──► Plan sheet ──► Session (new)
  │                        ├ Last used plan preselected
  │                        ├ other plans (last done: 3 days ago)
  │                        └ + New plan
  │
  ├─ tap a plan chip under the hero ("Start again") ──► Session (new)
  │
  └─ tap a past workout ──► Session (edit)

Session
  ├─ summary line: Today · 45 min · "travel gym"   (tap → Session details sheet)
  ├─ Activity card × N (every activity in the plan)
  │     ├ Last time 135 × 8 · PR 145 × 6
  │     ├ Set 1  135 lb × 8          (tap row → Edit set sheet; ⋮ → Delete)
  │     └ [ + Add set ]  ──► Set sheet (prefilled from previous set)
  ├─ sticky Rest bar (after a load/interval set)
  └─ sticky [ Finish ]
```

### Workout hub

```
HERO     Upper Body Strength · Strength Training
         4 sets · 8,400 lb · 45 min
         vs last time: +200 lb
         2 days ago
         [ Start again ]   ← starts a session with this plan

GOAL     This week (HHS)                    strength days 1/2 ▓▓▓░
                                            aerobic min 60/150 ▓▓░░░

TREND    [ Sessions | Volume ]   8-week bar chart
         Sessions (30d) 9 · Sets (30d) 41

HISTORY  [Filter ▾]
         Tue, Oct 27
           Upper Body Strength          ⋮
           Strength Training · 4 sets · 8,400 lb · 45 min
         ...
         Show older
                                         [ + Log workout ]
```

- **Plans moves into the overflow menu** (and the plan sheet), so the hub leads with results, not setup (fixes K6).
- The stat chips live inside the Trend card as a caption, so they no longer repeat the hero.
- **History rows lead with the plan name**, followed by type, summary, duration, and note (fixes K8).
- **Filter sheet:** Range (30 days / All), Plan, and Activity family, the same options as today in one place.

### Plan sheet (start a workout)

A modal bottom sheet that opens from **Log workout** (hub FAB and Home):

- Plans listed with type and "last done" relative date. The last-used plan is selected.
- Primary button **Start workout**. Secondary row **+ New plan**, which opens the plan form and returns to this sheet with the new plan selected.
- With no plans, the sheet shows the empty state: "Create a plan to start logging", with the **Create a plan** button.
- A plan with no activities can still be started. Its session shows the "Add an activity" empty state (keeps current behavior).

This removes the plan dropdown from the top of a new session. The draft stays in memory exactly as `startWorkout()` does today.

### Session screen (replaces `WorkoutFormScreen` layout)

**Top app bar:** ← · title = plan name · subtitle = date · overflow (Session details, Change plan, Share session, How to log, Delete workout). New sessions are titled **Log workout**, and saved ones **Edit workout**, as the subtitle prefix.

**Summary line** (first row, tappable): `Today · Add duration · Add note`. Tapping it opens the **Session details sheet**, which holds date, duration, and note with Cancel / Save. This sheet replaces keystroke autosave (fixes K3).

**Activity cards**, one for each activity in the plan, in plan order. Activities logged in this session move to the top in the order they were logged:

```
┌ Chest Press ─────────────────────────────┐
│ Last time 130 × 8 · PR 145 × 6           │
│ Set 1   135 lb × 8                    ⋮  │
│ Set 2   135 lb × 7 · RPE 9            ⋮  │
│ [ + Add set ]                            │
└──────────────────────────────────────────┘
┌ Lat Pulldown ────────────── not logged ──┐
│ Last time 100 × 10                       │
│ [ + Add set ]                            │
└──────────────────────────────────────────┘
```

- **+ Add set on each card** removes the activity picker step and keeps the add action next to the work (fixes K2). The label follows the schema vocabulary.
- **The set row is the tap target** for editing, and ⋮ → Delete confirms and offers Undo. This replaces the pencil and trash icons (fixes K5, C9).
- The "vs last time" and PR lines stay on the card, as they are today.
- Activities in the session that are no longer on the plan stay visible under their recorded name, with "Unknown activity" as the fallback (current behavior).

**Set sheet** (replaces the full-screen `WorkoutEntryFormScreen` for add and edit):

- A modal bottom sheet that expands to full screen when the keyboard opens or the cycling fields need room.
- The activity is preselected from the card. An **Activity** field stays in the sheet so the user can move a set to another activity (keeps current capability).
- **Prefill order:** the previous set in this session for this activity, then "Last time" (with **Use**), then Tabata defaults (8 / 20 / 10, with the existing caption), then empty.
- Schema fields come first, then **Add details**: set type (Warm-up / Working / Drop / Failure), RPE, and optional cycling metrics. Cycling **resistance** stays above distance, as today.
- **Save**, plus **Save & add another**, which keeps the sheet open and prefilled for the next set (Strong and Hevy pattern). Cancel confirms if there are changes.
- The first saved set commits the draft workout, as `upsertSavedEntry` does today.

**Sticky rest bar** (fixes K4):

```
┌──────────────────────────────────────────┐
│ Rest 1:12        [ −15s ] [ +15s ] [Skip]│
└──────────────────────────────────────────┘
```

- Appears above **Finish** after saving a working load or interval set, when the rest default isn't Off. It records `restSecTaken` as it does today.
- The default (Off / 60 / 90 / 120 s) moves to **Workout settings**. The bar's long-press or ⋮ offers "Change default" as a shortcut. The −15 s / +15 s buttons are an optional addition, not required for parity.

**Finish** (sticky primary button, fixes K10):

- On a saved session, Finish returns to the hub and shows the snackbar **Workout saved**.
- On a draft with no sets and no details, Finish returns silently, as today.
- On a draft with no sets but a typed duration or note, a dialog asks "Nothing logged yet. Discard this workout?" with **Discard** and **Keep logging**. System Back behaves the same way.

**Change plan** (in the overflow menu, fixes K1, K7): it opens the plan sheet. If sets exist, the current confirm text stays: "Switch to *Lower Body Focus*? This removes 8 sets from this session."

### Plans

- **Plans list** (from the Workout overflow menu, Workout settings, Profile, or the plan sheet). Tapping a row opens the plan. The overflow menu offers **Start workout** and **Edit**. The FAB is **New plan**.
- **Plan detail:** top bar actions are **Edit plan** and ⋮ (Delete plan¹). The activities list uses the history-row pattern, and the FAB is **Add activity**. A primary **Start workout** button sits under the description (fixes K7).
- **Plan form:** Name; **Activity type** as one searchable list grouped by family (replacing the two dropdowns); a live line "You'll log: weight and reps" that updates with the type; Description under Add details. It uses the record form template, so it gets Cancel / Save and a discard confirm (fixes C6).
- **Activity form:** Name and Description, with the record form template.

### Labels by schema

| Schema | Add button | Entry noun | Activity field |
|---|---|---|---|
| load | + Add set | set / sets | Activity |
| distance_time | + Add segment | segment / segments | Activity |
| interval | + Add round set (or Block) | round set / round sets | Activity |
| duration | + Add time | time entry / time entries | Activity |
| sport | + Add time | time entry / time entries | Activity |
| note | + Add note | note / notes | Activity |

### Happy path: log a two-set strength session

| Step | Today | New |
|---|---|---|
| 1 | Workout tab → FAB | Workout tab → **Log workout** (or **Start again** on the hero) |
| 2 | Composer opens; scroll past date, duration, note, rest, links, and plan | Plan sheet with the last plan selected → **Start workout** |
| 3 | **Add set** → full-screen form | **+ Add set** on the Chest Press card → sheet |
| 4 | Check or change the activity dropdown | Activity is already correct |
| 5 | Enter weight and reps (or **Use**) → Save → back to the composer | Enter weight and reps (or **Use**) → **Save & add another** |
| 6 | Scroll up → **Add set** → form → Save | Sheet is prefilled with set 1 → adjust reps → **Save** |
| 7 | Scroll down → **Done** | Rest bar runs; **Finish** is on screen |

The new path removes two full-screen round trips and the activity picker, and it never asks the user to scroll to find the next action.

---

## Blood pressure CX

**Hub**

- **Hero:** `118 / 76 mmHg · 64 bpm`, AHA badge (tap for categories), delta vs previous, derived values (pulse pressure / MAP), date · time · period, the low-BP hint, and the severe-reading instructions when they apply.
- **Trend:** the line chart with a `Last 7 · 10 · 30 readings · All` segmented button, the existing legend, and "Average of last 10 readings · **Change**" (opens Blood pressure settings → Average window). Below that, a **Category** view toggle shows the **BP gauge** and the **AHA zone chart**, which currently appear only on Home (fixes B3).
- **Goal:** the 7-day average card, split into the average with its badge and reading count, then morning vs evening, then "*n* of *m* at or under target" for 7 and 30 days, with **Change target** or **Set a target**.
- **History:** Filter sheet for period (All / Morning / Evening) and tags. Rows show the value, history line, and compact badge.
- **FAB:** Add reading. **Overflow:** How to measure · Share report · Blood pressure settings.

**Form (Add / Edit reading)**

- WHEN: date, time, time of day (auto-inferred from time until the user chooses, as today).
- VALUES: systolic / diastolic on one row, then heart rate, then "Last time: 128 / 82 · 66 bpm" with **Use** (no prefill), then the live high/low warnings.
- **Take a second reading** toggle: the 60 s countdown, second reading fields, and Remove. The saved value is still the average of both, with `averagedFrom` kept.
- Add details: tags and note (160 characters).
- Save → neutral "Check this reading" confirm when needed → hub with the **Saved · {category}** snackbar.

---

## Weight CX

**Hub**

- **Hero:** the big value is always the **7-day trend** when two or more readings exist. Otherwise it's the last weight, labeled "Latest". The second line always reads "Last: 182.4 lb · Oct 27 · 7:02 AM", followed by the trend delta (fixes W1).
- **Trend:** the weight chart (daily low, 7-day trend, dashed goal) with a `7 · 30 · 90 days · All` segmented button.
- **Goal:** start · % · target, the progress bar, target-to-change, change so far, weekly rate, and ETA with its "Estimate only" caption. The empty states stay as they are: **Choose starting weight**, then **Set target weight**.
- **Body measures:** the BMI gauge (current + goal needles, tap for categories) and the waist caption. Its **Edit** opens a small sheet with Height and Waist. If height is missing, the card shows the **Set height** empty state (fixes W3).
- **History:** Filter sheet for range (7/30/90/All days) and tags. Rows show weight, history line, and change arrow (the arrow keeps its icon and accessibility label, not color alone).
- **FAB:** Record weight. **Overflow:** How to weigh · Share log · Weight settings.

**Form (Record / Edit weight)**

- WHEN: date and time. VALUES: − / + stepper (0.1 in the display unit) around the decimal field, with "Last time: 182.4 lb" and **Use**. Add details: tags and note. Save.
- "How to weigh" moves to the overflow menu (fixes W4).

---

## Profile CX

Profile becomes a grouped settings hub with about eight top-level rows instead of seventeen (fixes P1). The data and editors stay the same; only the grouping and save behavior change.

```
YOU
  Height                         5' 10"   ›
  Waist                          34 in    ›
  Units                          lb       ›   (body weight and lift weight)

TRACKING
  Blood pressure settings  Target 130/80 · avg 10 · 2 reminders  ›
  Weight settings          Goal 175 lb from Oct 1 · reminder off ›
  Workout settings         2 plans · rest 90 s · reminder off    ›

APPEARANCE
  Theme                          Mint     ›

DATA
  Export data                                ›
  Load data                                  ›
  Erase all data                 (danger)    ›

ABOUT
  Health disclaimer                          ›
  About                          1.0.1       ›

All data is stored only on this phone and is protected by your device lock.
```

- The area settings rows open the same pages as the hub overflow menus (see [Area settings page](#4-area-settings-page)).
- **Units** moves to YOU because it affects body weight *and* lift weight (fixes P3). Weight settings and Workout settings both link to it.
- **Erase all data** comes last in DATA, styled red, and keeps its two confirms (fixes P4).
- Export and Load keep their progress text and the import summary (plans, workouts, weight readings, BP readings).
- Typed editors (Height, Waist, Weight goal, BP target, Average window) use Cancel / Save. Toggles and choices (reminders, theme, units, rest default) apply immediately with the **Saved** snackbar (fixes P2).

---

## Home CX

- **Remove the top chip row.** Each area card leads with the same `HeroCard` its hub uses, so the numbers appear once (fixes H1).
- **Each area card** has a hero, one compact insight, and a tonal **+ Log** button (fixes H2, H4):
  - BP: the hero (value, badge, delta, date), then the 7-day average line, then the recent trend chart. The gauge and zone chart move to the BP hub's Category view.
  - Weight: the hero (trend, last value, delta), then the weight chart and the compact BMI gauge (or the "Set height" prompt, which opens the Height editor).
  - Workout: the hero (last session summary), then the HHS week line, then the 8-week bars.
  - Tapping the card opens the tab, and **+ Log** opens the same add flow as the tab's FAB (Workout opens the plan sheet).
- **Get started checklist** stays visible until all three are done. Completed items show a check (fixes H3). It keeps the same three entries: Record blood pressure, Record weight, Log a workout.

---

## Accessibility (WCAG 2.2 AA)

- **No timed auto-dismiss** of meaningful content. The BP saved screen is replaced by a snackbar with an action (2.2.1).
- **Text alternatives for charts**, for example: "Blood pressure, last 10 readings. Systolic 118 to 131, latest 124. Diastolic 74 to 84, latest 78." The same goes for the weight and workout bars (1.1.1).
- **Section titles are headings** (`semantics { heading() }`) so TalkBack can jump between Hero, Trend, Goal, and History (1.3.1, 2.4.6).
- **Color is never the only signal.** Deltas keep their arrow or sign, and badges keep their text label (1.4.1).
- **Targets stay at least 48 dp.** Swapping the set row's pencil and trash for a row plus ⋮ also adds spacing between targets (2.5.8).
- **Focus order in sheets** goes title, then the first required field, then Save. Opening a sheet moves focus into it, and closing returns focus to the card that opened it (2.4.3).
- **Contrast** is checked in all four themes, including Dark, for badges on tinted backgrounds (1.4.3).

---

## Functionality preservation map

Every current capability, and where it lives in the new CX. "Moved" means a new location; "Same" means unchanged apart from styling.

### App shell

| Capability | New location |
|---|---|
| Boot splash, welcome + disclaimer (scroll, checkbox, Get started), acceptance persisted | Same |
| Five tabs; tabs hidden on nested screens | Same |
| Snackbar Undo for BP, weight, and session delete | Same, extended to sets and activities |
| Saved notices | Snackbar, everywhere |
| Four themes with swatches | Profile → Theme |
| Local-only storage, device-lock note | Profile footer; About |
| Starter plans (Upper Body Strength, Lower Body Focus) | Same |

### Home

| Capability | New location |
|---|---|
| Latest BP, weight trend/latest, last workout chips | Folded into each area card's hero |
| BP gauge, badge, low hint, delta, derived values, date, HR | BP card hero (gauge moved to BP hub → Category) |
| 7-day average line + AHA caption | BP card |
| BP trend chart | BP card |
| AHA zone chart | **Moved** to BP hub → Trend → Category |
| Weight value/trend, delta, history chart with goal | Weight card |
| BMI gauge or set-height prompt | Weight card |
| Sessions (30d), sets (30d), last session + plan, week line, 8-week bars | Workout card (hero + week + bars) |
| Get started rows | Checklist with completion marks |
| Tap card → tab; Log → add flow | Same (**+ Log** tonal button) |

### Blood pressure

| Capability | New location |
|---|---|
| Add / edit / delete readings; confirm with date, time, values; Undo | Same; Delete also in the edit form's ⋮ |
| Date, time, time-of-day (auto-inferred, lockable) | Form → WHEN |
| Systolic, diastolic, HR; validation messages | Form → VALUES |
| Previous reading suggestion | "Last time … Use" (no longer a prefill) |
| Paired second reading, 60 s wait, averaging, `averagedFrom`, `pairId` | Form → Take a second reading |
| Live high/low hints; save warning confirm | Same, neutral dialog |
| Tags (7) and note (160 characters) | Form → Add details |
| Saved status confirmation | Snackbar "Saved · {category}" + Details |
| Hero: value, delta, derived, badge + categories dialog, low hint, severe instructions, date · time · period | Hub → Hero |
| Chart with 7/10/30/All ranges, legend, moving average, Change window | Hub → Trend |
| 7-day average, morning/evening, target 7/30-day attainment, set/change target | Hub → Goal |
| Period and tag filters, Show older | Hub → History → Filter sheet |
| How to measure guide | Hub ⋮, form ⋮ |
| Share report (text + CSV) | Hub ⋮ |
| BP target, average window (2–60, default 10), morning/evening reminders with times | Blood pressure settings (hub ⋮ and Profile) |
| Missing-reading screen | Same |

### Weight

| Capability | New location |
|---|---|
| Add / edit / delete; confirm; Undo; clear goal start on delete (restored on Undo) | Same; Delete also in the edit form's ⋮ |
| Date, time, decimal entry, − / + 0.1 stepper in the display unit | Form |
| Last weight hint | "Last time … Use" |
| Tags (4) and note | Form → Add details |
| Hero: 7-day trend or latest, last value, date, delta | Hub → Hero (consistent label) |
| Chart: daily low, 7-day trend, goal line, 7/30/90/All ranges | Hub → Trend |
| Goal progress: start, %, target, change, weekly rate, ETA, empty states | Hub → Goal |
| BMI gauge (current + goal), categories dialog, waist caption, set-height empty state | Hub → Body measures |
| Range and tag filters, change arrows, Show older | Hub → History |
| How to weigh guide | Hub ⋮, form ⋮ |
| Share log (range) | Hub ⋮ |
| Goal + starting weight picker + 10% hint; units; reminders; height; waist | Weight settings (hub ⋮ and Profile) |
| Missing-reading screen | Same |

### Workout

| Capability | New location |
|---|---|
| Draft session in memory; first saved set commits; empty draft discarded on Back | Same (plus confirm when the draft has details) |
| Log workout / Edit workout titles | Session top bar |
| Session date, duration (optional), note (optional) | Session details sheet (Cancel / Save) |
| Plan picker with type label | Plan sheet (new session); ⋮ Change plan (existing) |
| Confirm plan switch that clears sets | Same |
| Manage plans from the session | ⋮ Change plan → plan sheet → + New plan; Workout hub ⋮ Plans |
| Empty plan → Add an activity | Same, on the session screen |
| Entries grouped by activity; Set n labels; summaries | Activity cards |
| Vs last time per activity; PR / Epley line | Activity card header |
| Edit / remove a set with confirm | Tap the row / ⋮ Delete, with confirm + Undo |
| Activity picker in the entry form | Set sheet (preselected, still changeable) |
| Schemas: load, distance_time, duration, sport (+ intensity), interval, note | Set sheet, same fields and validation |
| Hollow-entry rejection; RPE 1–10; set kind (4 types) | Set sheet (RPE and set kind under Add details) |
| Cycling metrics: resistance (before), speed, RPM, avg/max HR | Set sheet (resistance above distance; others under Add details) |
| Previous entry suggestion + Use; Tabata defaults + caption | Set sheet prefill order |
| Discard-changes confirm | Same |
| Rest timer preference (Off/60/90/120) | Workout settings (+ shortcut on the rest bar) |
| Rest countdown, Skip, `restSecTaken` recorded | Sticky rest bar |
| Share session (text + CSV) | Session ⋮ |
| Done | Finish |
| Hub hero: last session, summary, vs last time, relative date + type | Hub → Hero (+ Start again) |
| This week: strength days, aerobic minutes, HHS caption | Hub → Goal |
| Sessions (30d), sets (30d), last session chips | Trend card caption |
| Weekly session bars; weekly volume bars + caption | Trend card (Sessions / Volume toggle) |
| Range, plan, and family filters; Show older; date groups | Hub → History → Filter sheet |
| Delete session with confirm (plan, date, count) + Undo | Same |
| How to log guide | Hub ⋮, session ⋮ |
| Share log (filtered) | Hub ⋮ |
| Workout reminders | Workout settings |
| Create-a-plan empty state | Plan sheet empty state and hub empty state |
| Plans list (type, description, activity count), open, edit | Plans (row tap / ⋮) |
| Plan form: name, family → type (all 40 types, 6 families), description, helper line | Plan form (grouped type list + "You'll log") |
| Plan detail: activities list, add / edit / remove with confirm | Same + Edit plan + Start workout |
| Missing workout, entry, plan, and activity screens; plan-required screen | Same |
| Contextual back labels (Workouts / Plans / Profile) | Top app bar back (accessibility label = parent) |

### Profile and data

| Capability | New location |
|---|---|
| Height (3'0"–8'0"), waist, units | Profile → YOU |
| Weight goal, starting weight, BP target, BP window, all reminders, plans, rest default | Area settings pages (also linked from Profile → TRACKING) |
| Theme | Profile → APPEARANCE |
| Export JSON (share sheet), progress text | Profile → DATA |
| Load JSON, file picker, import summary confirm, replace all, result dialog | Profile → DATA |
| Erase all (double confirm, restores starter plans, keeps disclaimer acceptance) | Profile → DATA (last) |
| Health disclaimer, About (name, version, on-device note) | Profile → ABOUT |
| Notification permission request when enabling a reminder | Same |

---

## Component changes (implementation notes)

| Component | Change |
|---|---|
| `AppScreen` | Add a pinned `TopAppBar` (title, optional back, `actions` / overflow items). Drop `eyebrow` on hubs. Swap the FAB for `ExtendedFloatingActionButton`. Add an optional `bottomBar` slot for the rest bar and Finish |
| `HeroCard` (new) | Shared hero for BP, Weight, Workout, and Home |
| `ChartCard` (new) | Title, `SingleChoiceSegmentedButtonRow` range control, chart, legend, and `contentDescription` summary |
| `FilterSheet` (new) | `ModalBottomSheet` with sections; returns active filters rendered as `InputChip`s |
| `RecordFormScaffold` (new) | Full-screen dialog bar (✕, title, Save), Cancel / Save footer, dirty tracking, discard confirm, Delete in ⋮ |
| `AddDetails` (new) | Collapsible section that auto-expands when it has data |
| `LastTimeSuggestion` (new) | "Last time: …" + Use, shared by BP, Weight, and Workout |
| `ConfirmDialog` | Rename from `ConfirmDeleteDialog` with a `destructive` flag, so warnings aren't styled as deletes |
| `HistoryRow` | Unchanged; also used for set rows inside activity cards |
| `SettingsRow` | Unchanged; area settings rows show a multi-value summary |
| `WorkoutFormScreen` | Becomes the session screen: summary line, activity cards, sticky rest bar + Finish |
| `WorkoutEntryFormScreen` | Becomes the set sheet content (same state and validation code) |
| `PlanFormScreen` | Grouped type list; Cancel / Save via `RecordFormScaffold` |
| `AppViewModel` | Session details saved as one call. `startWorkout(planId)` accepts the chosen plan. Optional `deletePlan` |

Routes can stay as they are. New routes are `bpSettings`, `weightSettings`, and `workoutSettings`; sheets are hosted inside existing destinations.

---

## Implementation phases

### Phase 1: Workout clarity (highest impact)

- Plan sheet for starting a session; Start again on the hero.
- Session screen: summary line + details sheet, activity cards with per-card **+ Add set**, set rows with tap / ⋮.
- Set sheet with Save & add another and the prefill order.
- Sticky rest bar + Finish; rest default moved to Workout settings.
- Draft-with-details discard confirm; Undo for set delete.
- Schema label fixes (no "block" for duration or sport).

**Status: done.** Screens in `docs/phase1-screens/`.

### Phase 2: Shared frame

- Pinned top app bar with a standard overflow menu on every screen; remove eyebrows on hubs.
- Extended FAB everywhere; the Workout FAB is always visible.
- `RecordFormScaffold` for BP, Weight, plan, and activity forms: Save label, discard confirm, Delete in ⋮.
- "Last time … Use" in BP (stop prefilling) and Weight.

**Status: done.** Screens in `docs/phase2-screens/`. BP settings and Weight settings pages were pulled forward from Phase 3 so the hub ⋮ items have a destination; the Profile regroup remains in Phase 3. Plan delete is still deferred.

### Phase 3: Hubs and settings

- Hub order: Hero → Trend → Goal → History, with Filter sheets.
- Segmented range controls with units.
- Area settings pages; Profile regrouped; Units moved to YOU; typed editors switched to Save.
- BP Category view (gauge + zone chart) on the BP hub; Weight Body measures card.

**Status: done.** Screens in `docs/phase3-screens/`. The Workout hub follows acceptance check 1 (Trend before Goal) rather than the Workout hub sketch, so all three hubs share one order. Home still shows the gauge and zone chart until its Phase 4 rebuild, and toggle-style settings keep the inline "Saved" text until the Phase 4 snackbar.

### Phase 4: Home, feedback, accessibility

- Home cards built from `HeroCard`; chip row removed; + Log buttons; persistent checklist.
- BP saved snackbar with category; Saved snackbar for settings.
- Chart text alternatives, heading semantics, sheet focus order, theme contrast pass.

**Status: done.** Screens in `docs/phase4-screens/`. Home reuses each hub's hero. The BP saved snackbar includes the category and a Details action. Charts expose TalkBack summaries. Settings and rest-timer choices show the Saved snackbar. Saved history rows flash briefly. Sheets take focus on open and return it to the Filter or Edit control that opened them.

---

## Validation plan

Run before and after each phase so the redesign is measured, not assumed.

- **Moderated usability test:** 5 participants per round (Nielsen's rule of thumb for catching most issues), split between first-time users and people who track at least one metric today.
- **Tasks:**
  1. Record a BP reading with a second reading.
  2. Record a weight and set a goal from it.
  3. Log a strength session with two activities and three sets.
  4. Fix the reps on a set from yesterday.
  5. Turn on an evening BP reminder.
  6. Export your data.
- **Metrics:** task success rate, time on task, error count, Single Ease Question (SEQ) per task, and **System Usability Scale (SUS)** per session. Targets: ≥ 90 % success on tasks 1–4, SUS ≥ 80, and task 3 under 90 seconds for a returning user.
- **First-click test** on each hub: "Where would you change the BP target?" The goal is for all three areas' settings to be found from the hub at least 80 % of the time.

---

## Acceptance checks

The new CX is done when all of these are true:

1. BP, Weight, and Workout hubs all show **Hero → Trend → Goal → History**, with a labeled Extended FAB and the same overflow order.
2. Every add/edit form has ✕ / Save in the top bar, Cancel / Save at the bottom, a discard confirm, and Delete in ⋮ when editing.
3. No form prefills values from a previous record. All three areas show "Last time … Use".
4. Each area's settings page is reachable from its hub and from Profile, and it's the same page both ways.
5. A returning user can log a two-activity, three-set strength session **without the activity dropdown and without scrolling to find the next action**.
6. Session date, duration, and note never save on a keystroke, and a draft with typed details asks before discarding.
7. The rest countdown stays visible while sets are logged.
8. Every row in the [functionality preservation map](#functionality-preservation-map) is reachable. Export/import of older JSON still loads, erase still restores starter plans, and Undo still restores deleted records.
9. No meaningful content auto-dismisses on a timer, and every chart has a text alternative.

---

## Out of scope

- New metrics, new activity types, or new health standards
- Accounts, cloud sync, Health Connect, watches, GPS
- Mixed activity types in one session (it would change the plan model; revisit after Phase 1 testing)
- Changing stored units (lb, miles) or the export file format

If any of these are added later, they should reuse the hub, record form, settings, and sheet patterns above.
