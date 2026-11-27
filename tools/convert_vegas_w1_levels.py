import json
from pathlib import Path
from urllib.parse import parse_qs

VEGAS_LEVELS_DIR = Path(
    r"C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source\GoldMinerVegas\Levels"
)
PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUT_LEVELS_DIR = PROJECT_ROOT / "assets" / "data" / "levels"

# Verified 9 track bounds (DefineSprite_1816 in 800px stage) scaled to 480u width (x0.6)
TRACK_RANGES = {
    1: (12.0, 468.0),
    2: (155.0, 371.0),
    3: (229.0, 468.0),
    4: (12.0, 292.0),
    5: (123.0, 468.0),
    6: (189.0, 304.0),
    7: (171.0, 468.0),
    8: (12.0, 468.0),
    9: (243.0, 468.0),
}

# Verified object metadata (objectN -> type, assetKey, value, weight, radius, drawW, drawH, moveSpeed, animated)
OBJECT_DEFS = {
    2: ("gold_small", "gold_small", 50, 3, 4.2, 8.2, 7.6, 0.0, False),
    3: ("gold_small_med", "gold_small_med", 100, 7, 6.8, 14.5, 13.5, 0.0, False),
    4: ("gold_medium", "gold_medium", 250, 8, 10.8, 25.0, 20.0, 0.0, False),
    5: ("gold_large", "gold_large", 500, 9, 16.0, 40.0, 27.0, 0.0, False),
    6: ("gold_mega", "gold_mega", 1000, 11, 19.4, 42.0, 36.0, 0.0, False),
    7: ("gold_pan", "gold_pan", 25, 0, 5.8, 15.0, 8.2, 0.0, False),
    8: ("rock_large", "rock_large", 20, 9, 14.5, 39.0, 21.0, 0.0, False),
    9: ("rock_medium", "rock_medium", 11, 8, 9.0, 18.5, 19.0, 0.0, False),
    10: ("rock_small", "rock_small", 5, 4, 4.8, 10.5, 9.5, 0.0, False),
    11: ("mystery_bag", "mystery_bag", 0, 7, 9.5, 20.0, 19.0, 0.0, False),
    12: ("tnt_barrel", "tnt_barrel", 1, 2, 12.2, 24.5, 25.5, 0.0, False),
    13: ("diamond", "diamond", 600, 2, 4.8, 9.5, 9.0, 0.0, False),
    14: ("skull", "skull", 20, 2, 5.0, 11.0, 9.2, 0.0, False),
    15: ("bone_small", "bone_small", 5, 2, 5.8, 14.0, 11.2, 0.0, False),
    16: ("bone_long_v", "bone_long_v", 10, 5, 8.2, 8.8, 25.5, 0.0, False),
    17: ("bone_long_h", "bone_long_h", 10, 5, 8.2, 25.5, 8.8, 0.0, False),
    18: ("skeleton", "skeleton", 20, 7, 7.6, 17.5, 14.2, 0.0, False),
    21: ("bilby", "bilby_walk", 10, 3, 10.5, 23.0, 24.2, 42.1, True),
    22: ("bilby_diamond", "bilby_diamond_walk", 610, 5, 11.5, 24.0, 24.8, 40.0, True),
    23: ("gem_1", "gem_1", 100, 3, 5.0, 8.8, 10.5, 0.0, False),
    24: ("gem_2", "gem_2", 100, 3, 5.0, 9.2, 10.0, 0.0, False),
    25: ("gem_3", "gem_3", 100, 3, 5.0, 9.2, 11.0, 0.0, False),
    26: ("gem_4", "gem_4", 100, 3, 5.0, 8.8, 10.5, 0.0, False),
    27: ("tasmanian", "tasmanian_walk", 30, 7, 12.0, 30.2, 19.0, 16.2, True),
    28: ("tasmanian", "tasmanian_walk", 30, 7, 12.0, 30.2, 19.0, 16.2, True),
}

# Canonical rich sublevels selected from MGVegas_1_1..6.txt (showcasing TNT, Diamonds, Bilbies, Bones, 4 Gems)
CANONICAL_SUBLEVEL = {
    1: 2,  # val=1455, track=1, Gold 2..5, Pan 7, Rock 9, Bag 11, TNT 12, Bone 17
    2: 2,  # val=2589, track=5, Gold 2/4/5, Pan 7, Rock 9, TNT 12, Diamond 13, Skeleton 18, Bilby 21
    3: 2,  # val=2638, track=3, Gold 2..5, Pan 7, Rock 9, Bones 17/18, 4xBilby 21, 2xDiamond Bilby 22
    4: 3,  # val=3185, track=1, Gold 4, Rocks 8/9, 2xBag 11, 4xTNT 12, 2xDiamond 13, Bone 17, 2xDiamond Bilby 22
    5: 1,  # val=2402, track=7, Gold 2/3/5, Pan 7, Rock 9, 2xBag 11, 3xTNT 12, Skull 14, Bone 17
    6: 1,  # val=2412, track=2, Gold 2/4/5, 3xPan 7, Rocks 8/9, Bag 11, Bones, Diamond Bilby 22, All 4 Gems 23..26
}


def clamp(v: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, v))


def convert_level(lv: int, sub: int, running_goal: int, goal_changer: float) -> tuple[dict, int, float]:
    fp = VEGAS_LEVELS_DIR / f"MGVegas_1_{lv}.txt"
    raw = fp.read_text(encoding="utf-8", errors="replace")
    qs = parse_qs(raw)

    things = int(qs[f"things_{lv}_{sub}"][0])
    level_value = int(qs[f"levelValue_{lv}_{sub}"][0])
    track_n = int(qs[f"trackN_{lv}_{sub}"][0])
    min_x, max_x = TRACK_RANGES.get(track_n, (12.0, 468.0))

    items = []
    for i in range(1, things + 1):
        obj_n = int(qs.get(f"T{i}N_{lv}_{sub}", ["1"])[0])
        if obj_n == 1:
            continue
        if obj_n not in OBJECT_DEFS:
            raise ValueError(f"Unsupported objectN={obj_n} in MGVegas_1_{lv}.txt r={sub}")

        ox = float(qs[f"T{i}X_{lv}_{sub}"][0])
        oy = float(qs[f"T{i}Y_{lv}_{sub}"][0])

        # Map 800x600 Flash stage coords to 480x270 adjusted layout (mine area y in [0..192])
        wx = round(clamp(ox * 0.60, 18.0, 462.0), 1)
        wy = round(clamp(192.0 - (oy - 195.0) * (192.0 / 355.0), 14.0, 176.0), 1)

        t_name, asset_key, val, weight, rad, dw, dh, spd, animated = OBJECT_DEFS[obj_n]
        items.append(
            {
                "objectN": obj_n,
                "type": t_name,
                "assetKey": asset_key,
                "x": wx,
                "y": wy,
                "value": val,
                "weight": weight,
                "radius": rad,
                "drawWidth": dw,
                "drawHeight": dh,
                "moveSpeed": spd,
                "animated": animated,
            }
        )

    # Vegas setGoal() curve for fallback JSON goal (Easy defaults)
    if lv == 1:
        goal_changer = 0.39
        next_goal = 555
    else:
        if goal_changer < 0.42:
            goal_changer = round(goal_changer + 0.01, 4)
        next_goal = int(running_goal + level_value * goal_changer)

    level_json = {
        "level": lv,
        "world": 1,
        "themeHint": "australia",
        "trackN": track_n,
        "durationSeconds": 60,
        "goal": next_goal,
        "levelValue": level_value,
        "cartRange": {"minX": min_x, "maxX": max_x},
        "items": items,
    }
    return level_json, next_goal, goal_changer


def main() -> None:
    OUT_LEVELS_DIR.mkdir(parents=True, exist_ok=True)
    running_goal = 0
    goal_changer = 0.38
    for lv in range(1, 7):
        sub = CANONICAL_SUBLEVEL[lv]
        data, running_goal, goal_changer = convert_level(lv, sub, running_goal, goal_changer)
        out_path = OUT_LEVELS_DIR / f"level_{lv}.json"
        with out_path.open("w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
        print(
            f"Generated {out_path.name} (r={sub}): goal=${data['goal']}, "
            f"levelValue=${data['levelValue']}, track={data['trackN']} {data['cartRange']}, items={len(data['items'])}"
        )


if __name__ == "__main__":
    main()
