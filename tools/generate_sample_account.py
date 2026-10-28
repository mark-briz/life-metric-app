"""Generate a year of realistic Life Metric import JSON for a typical male user."""

from __future__ import annotations

import json
import math
import random
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

RNG = random.Random(42)
START = date(2025, 10, 28)
END = date(2026, 10, 28)
OUT = Path(__file__).resolve().parents[1] / "docs" / "sample-life-metric-account.json"

PLANS = [
    {
        "id": "plan-upper",
        "name": "Upper Body Strength",
        "description": "Chest, back, and shoulders — push and pull focused.",
        "activityTypeId": "strength_training",
        "category": "strength",
        "machines": [
            ("m-chest-press", "Chest Press"),
            ("m-lat-pulldown", "Lat Pulldown"),
            ("m-seated-row", "Seated Row"),
            ("m-shoulder-press", "Shoulder Press"),
            ("m-biceps-curl", "Biceps Curl"),
            ("m-triceps", "Triceps Extension"),
        ],
    },
    {
        "id": "plan-lower",
        "name": "Lower Body Focus",
        "description": "Legs and posterior chain for strength and stability.",
        "activityTypeId": "strength_training",
        "category": "strength",
        "machines": [
            ("m-leg-press", "Leg Press"),
            ("m-leg-curl", "Leg Curl"),
            ("m-calf-raise", "Calf Raise"),
            ("m-leg-extension", "Leg Extension"),
        ],
    },
    {
        "id": "plan-bike",
        "name": "Indoor Bike",
        "description": "Steady cardio on a stationary bike.",
        "activityTypeId": "cycling",
        "category": "cardio",
        "machines": [
            ("m-indoor-bike", "Indoor bike"),
            ("m-easy-spin", "Easy spin"),
        ],
    },
]

# Typical starting working loads (lb) and year-end targets for a deconditioned male.
LIFTS = {
    "m-chest-press": (85, 140),
    "m-lat-pulldown": (70, 120),
    "m-seated-row": (80, 130),
    "m-shoulder-press": (40, 75),
    "m-biceps-curl": (25, 45),
    "m-triceps": (30, 55),
    "m-leg-press": (180, 320),
    "m-leg-curl": (50, 90),
    "m-calf-raise": (90, 160),
    "m-leg-extension": (60, 110),
}


def daterange(start: date, end: date):
    cur = start
    while cur <= end:
        yield cur
        cur += timedelta(days=1)


def progress(day: date) -> float:
    return max(0.0, min(1.0, (day - START).days / 365.0))


def holiday_bump(day: date) -> float:
    bumps = [
        (date(2025, 11, 26), date(2025, 12, 1), 3.2),
        (date(2025, 12, 20), date(2026, 1, 3), 4.8),
        (date(2026, 4, 3), date(2026, 4, 12), 1.6),  # travel
        (date(2026, 7, 2), date(2026, 7, 8), 2.4),
    ]
    total = 0.0
    for first, last, amount in bumps:
        if first <= day <= last:
            mid = first + (last - first) / 2
            span = max(1, (last - first).days)
            falloff = 1 - abs((day - mid).days) / span
            total += amount * max(0.15, falloff)
    return total


def away(day: date) -> bool:
    windows = [
        (date(2025, 12, 24), date(2026, 1, 2)),
        (date(2026, 3, 14), date(2026, 3, 21)),
        (date(2026, 7, 18), date(2026, 7, 26)),
    ]
    return any(a <= day <= b for a, b in windows)


def sick(day: date) -> bool:
    return date(2026, 2, 9) <= day <= date(2026, 2, 13)


def uuid_weight(n: int) -> str:
    return f"{n:08x}-0000-4000-8000-{n:012x}"


def round_tenth(value: float) -> float:
    return round(value + 1e-9, 1)


def round_five(value: float) -> int:
    return int(5 * round(value / 5.0))


def iso(day: date) -> str:
    return day.isoformat()


def time_jitter(hour: int, minute: int, spread: int = 18) -> str:
    total = hour * 60 + minute + RNG.randint(-spread, spread)
    total = max(5, min(23 * 60 + 50, total))
    return f"{total // 60:02d}:{total % 60:02d}"


def weight_on(day: date) -> float:
    t = progress(day)
    # Asymptotic loss toward ~191 lb from 218.4 — about 27 lb in a year, not yet at 185.
    trend = 218.4 - 27.6 * (1 - math.exp(-2.15 * t))
    weekend = 0.8 if day.weekday() >= 5 else 0.0
    noise = RNG.gauss(0, 1.15)
    return round_tenth(max(175.0, trend + holiday_bump(day) + weekend + noise))


def bp_on(day: date, evening: bool) -> tuple[int, int, int]:
    t = progress(day)
    sys = 148 - 17.5 * t
    dia = 94 - 13.5 * t
    if evening:
        sys += 3.5
        dia += 1.5
    if day.month in (12, 1, 2):
        sys += 2.0
        dia += 1.0
    if day.month in (6, 7, 8):
        sys -= 1.5
    sys += RNG.gauss(0, 4.2)
    dia += RNG.gauss(0, 2.8)
    hr = 76 - 6 * t + RNG.gauss(0, 3.4)
    if evening:
        hr += 4
    return (
        int(round(max(108, min(168, sys)))),
        int(round(max(68, min(104, dia)))),
        int(round(max(54, min(102, hr)))),
    )


def lift_working(machine_id: str, day: date) -> tuple[int, int]:
    start, end = LIFTS[machine_id]
    load = start + (end - start) * progress(day) ** 1.15
    load += RNG.gauss(0, 3.5)
    load = round_five(max(start - 10, load))
    reps = RNG.choice([8, 8, 8, 10, 10, 12, 6])
    if RNG.random() < 0.12:
        load = max(start, load - 5)
        reps = 12
    return load, reps


def build_weight_records() -> tuple[list[dict], str]:
    records = []
    n = 0
    start_id = None
    for day in daterange(START, END):
        if away(day) and RNG.random() < 0.7:
            continue
        if sick(day) and RNG.random() < 0.5:
            continue
        if day.weekday() == 6 and RNG.random() < 0.55:
            continue
        if RNG.random() < 0.18:
            continue
        n += 1
        tags = ["morning"]
        note = ""
        weigh_time = time_jitter(6, 52, 25)
        if RNG.random() < 0.06:
            tags = ["evening"]
            weigh_time = time_jitter(20, 10, 40)
        if away(day):
            tags.append("travel")
            note = RNG.choice(["hotel scale", "parents' scale", ""])
        if RNG.random() < 0.04:
            tags.append("after_exercise")
        rec = {
            "id": uuid_weight(n),
            "date": iso(day),
            "time": weigh_time,
            "weight": weight_on(day),
            "tags": tags,
        }
        if note:
            rec["note"] = note
        records.append(rec)
        if start_id is None and "morning" in tags:
            start_id = rec["id"]
    return records, start_id or records[0]["id"]


def build_bp_readings() -> list[dict]:
    readings = []
    n = 0
    for day in daterange(START, END):
        if away(day) and RNG.random() < 0.55:
            continue
        if sick(day):
            # fewer, higher readings
            pass
        elif day.weekday() == 6 and RNG.random() < 0.4:
            continue
        elif RNG.random() < 0.16:
            continue

        def add(evening: bool):
            nonlocal n
            n += 1
            sys, dia, hr = bp_on(day, evening)
            tags = ["left_arm"] if RNG.random() < 0.86 else ["right_arm"]
            note = ""
            period = "evening" if evening else "morning"
            clock = time_jitter(20, 35, 35) if evening else time_jitter(6, 58, 22)
            if sick(day):
                sys += RNG.randint(6, 14)
                dia += RNG.randint(3, 8)
                tags.append("felt_unwell")
                note = "headache"
            elif not evening and RNG.random() < 0.08:
                sys += RNG.randint(5, 11)
                dia += RNG.randint(2, 6)
                hr += RNG.randint(4, 10)
                tags.append("after_caffeine")
            elif evening and RNG.random() < 0.07:
                sys -= RNG.randint(2, 6)
                dia -= RNG.randint(1, 4)
                tags.append("after_exercise")
            if progress(day) > 0.18 and RNG.random() < 0.55:
                tags.append("after_medication")
            elif progress(day) <= 0.18 and RNG.random() < 0.35:
                tags.append("before_medication")
            rec = {
                "id": f"bp-{n:04d}",
                "date": iso(day),
                "time": clock,
                "systolic": int(max(108, min(172, sys))),
                "diastolic": int(max(66, min(108, dia))),
                "heartRate": int(max(52, min(110, hr))),
                "period": period,
                "tags": tags,
            }
            if note:
                rec["note"] = note
            readings.append(rec)

        add(False)
        if sick(day) or (day.weekday() < 5 and RNG.random() < 0.42) or RNG.random() < 0.18:
            add(True)
    return readings


def sets_for(plan_id: str, day: date) -> list[tuple[str, int, int, str, int | None]]:
    """Return (machine_id, weight, reps, setKind, rpe)."""
    machines = [m[0] for m in next(p for p in PLANS if p["id"] == plan_id)["machines"]]
    chosen = machines[:]
    RNG.shuffle(chosen)
    if plan_id == "plan-upper":
        chosen = chosen[: RNG.choice([4, 5, 5, 6])]
    else:
        chosen = chosen[: RNG.choice([3, 4, 4])]
    out = []
    for machine in chosen:
        working, reps = lift_working(machine, day)
        warmup = round_five(working * 0.6)
        out.append((machine, max(20, warmup), 10, "warmup", None))
        sets = 3 if RNG.random() < 0.75 else 4
        for i in range(sets):
            load = working if i < sets - 1 else max(working - (5 if RNG.random() < 0.25 else 0), warmup + 10)
            rpe = RNG.choice([6, 7, 7, 8, 8, 8, 9])
            kind = "working"
            if i == sets - 1 and RNG.random() < 0.08:
                kind = "failure"
                rpe = 9
            elif i == sets - 1 and RNG.random() < 0.06:
                kind = "drop"
                load = round_five(working * 0.75)
                rpe = 8
            out.append((machine, load, reps if i == 0 else max(6, reps - RNG.choice([0, 0, 1, 2])), kind, rpe))
    return out


def bike_entry(day: date, easy: bool) -> dict:
    t = progress(day)
    miles = (9.5 if easy else 14.5) + 4.5 * t + RNG.gauss(0, 1.4)
    miles = round_tenth(max(6.0, min(22.0, miles)))
    pace_sec = (390 if easy else 330) - 25 * t + RNG.gauss(0, 12)
    duration = int(max(18 * 60, miles * pace_sec))
    speed = round_tenth(miles / (duration / 3600))
    resistance = round_tenth((4.5 if easy else 7.0) + 2.2 * t + RNG.gauss(0, 0.6))
    rpm = int((78 if easy else 86) + RNG.gauss(0, 4))
    avg_hr = int((118 if easy else 132) - 4 * t + RNG.gauss(0, 5))
    max_hr = avg_hr + RNG.randint(12, 28)
    return {
        "distanceMi": miles,
        "durationSec": duration,
        "resistance": max(1.0, resistance),
        "avgSpeedMph": speed,
        "avgRpm": max(60, rpm),
        "avgHeartRateBpm": max(95, avg_hr),
        "maxHeartRateBpm": max(110, max_hr),
    }


def build_workouts() -> list[dict]:
    workouts = []
    entry_n = 0
    w_n = 0

    def add_strength(day: date, plan_id: str):
        nonlocal entry_n, w_n
        w_n += 1
        entries = []
        for machine, weight, reps, kind, rpe in sets_for(plan_id, day):
            entry_n += 1
            item = {
                "id": f"entry-{entry_n:05d}",
                "planId": plan_id,
                "machineId": machine,
                "schema": "load",
                "weight": weight,
                "repetitions": reps,
                "setKind": kind,
            }
            if rpe:
                item["rpe"] = rpe
            if kind != "warmup" and RNG.random() < 0.55:
                item["restSecTaken"] = RNG.choice([75, 90, 90, 105, 120])
            entries.append(item)
        duration = 42 * 60 + RNG.randint(-8 * 60, 16 * 60)
        note = ""
        if RNG.random() < 0.08:
            note = RNG.choice(["short on sleep", "hotel gym", "left early", "legs still sore", "rushed before work"])
        activity = "strength_training"
        rec = {
            "id": f"workout-{w_n:04d}",
            "date": iso(day),
            "planId": plan_id,
            "activityTypeId": activity,
            "durationSec": duration,
            "entries": entries,
        }
        if note:
            rec["note"] = note
        workouts.append(rec)

    def add_bike(day: date, easy: bool):
        nonlocal entry_n, w_n
        w_n += 1
        entry_n += 1
        telemetry = bike_entry(day, easy)
        machine = "m-easy-spin" if easy else "m-indoor-bike"
        entry = {
            "id": f"entry-{entry_n:05d}",
            "planId": "plan-bike",
            "machineId": machine,
            "schema": "distance_time",
            "setKind": "working",
            **telemetry,
        }
        if RNG.random() < 0.35:
            entry["rpe"] = 5 if easy else RNG.choice([6, 7, 7, 8])
        duration = telemetry["durationSec"] + RNG.randint(60, 240)
        rec = {
            "id": f"workout-{w_n:04d}",
            "date": iso(day),
            "planId": "plan-bike",
            "activityTypeId": "cycling",
            "durationSec": duration,
            "entries": [entry],
        }
        if RNG.random() < 0.07:
            rec["note"] = RNG.choice(["easy spin", "watched a race", "legs heavy", "good legs today"])
        workouts.append(rec)

    for day in daterange(START, END):
        if away(day) or sick(day):
            if day.weekday() in (1, 5) and not sick(day) and RNG.random() < 0.35:
                add_bike(day, easy=True)
            continue
        wd = day.weekday()
        roll = RNG.random()
        if wd == 0 and roll > 0.16:
            add_strength(day, "plan-upper")
        elif wd == 1 and roll > 0.18:
            add_bike(day, easy=False)
        elif wd == 2 and roll > 0.14:
            add_strength(day, "plan-lower")
        elif wd == 3 and roll > 0.22:
            add_bike(day, easy=True)
        elif wd == 4 and roll > 0.45:
            add_strength(day, "plan-upper")
        elif wd == 5 and roll > 0.28:
            add_bike(day, easy=False)
    return workouts


def main() -> None:
    weights, start_id = build_weight_records()
    bps = build_bp_readings()
    workouts = build_workouts()
    plans = []
    for plan in PLANS:
        plans.append(
            {
                "id": plan["id"],
                "name": plan["name"],
                "description": plan["description"],
                "activityTypeId": plan["activityTypeId"],
                "category": plan["category"],
                "machines": [{"id": mid, "name": name} for mid, name in plan["machines"]],
            }
        )
    payload = {
        "format": "life-metric-account",
        "version": 1,
        "exportedAt": datetime(2026, 10, 28, 15, 12, 0, tzinfo=timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000Z"),
        "themeId": "mint",
        "data": {
            "plans": plans,
            "workouts": workouts,
            "weightRecords": weights,
            "bloodPressureReadings": bps,
            "profile": {
                "firstName": "James",
                "lastName": "Carter",
                "heightInches": 70,
                "weightGoalLb": 185.0,
                "bpMovingAverageReadings": 10,
                "weightGoalStartId": start_id,
                "bpTargetSystolic": 130,
                "bpTargetDiastolic": 80,
                "bpMorningReminder": True,
                "bpEveningReminder": True,
                "bpMorningReminderTime": "07:00",
                "bpEveningReminderTime": "19:30",
                "weightMorningReminder": True,
                "weightMorningReminderTime": "06:45",
                "weightDisplayKg": False,
                "waistInches": 38.5,
                "workoutMorningReminder": True,
                "workoutMorningReminderTime": "06:15",
                "workoutRestTimerSec": 90,
            },
        },
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(
        f"Wrote {OUT} "
        f"({len(weights)} weights, {len(bps)} BP, {len(workouts)} workouts, "
        f"{sum(len(w['entries']) for w in workouts)} entries)"
    )


if __name__ == "__main__":
    main()
