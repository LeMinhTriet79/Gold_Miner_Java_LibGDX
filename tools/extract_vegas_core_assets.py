import json
import shutil
import subprocess
from pathlib import Path
from PIL import Image

VEGAS_SOURCE = Path(r"C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source")
PROJECT_ROOT = Path(__file__).resolve().parent.parent
THEME_DIR = PROJECT_ROOT / "assets" / "themes" / "vegas"
SPRITES_OUT = THEME_DIR / "sprites"
UI_OUT = THEME_DIR / "ui"
BACKGROUNDS_OUT = THEME_DIR / "backgrounds"
BGM_OUT = THEME_DIR / "audio" / "bgm"
SFX_OUT = THEME_DIR / "audio" / "sfx"
FONTS_OUT = PROJECT_ROOT / "assets" / "fonts"

VEGAS_FRAME_DURATION = round(1.0 / 18.0, 4)
MINER_WORLD_SCALE = 0.27
FX_WORLD_SCALE = 0.52


def ensure_dirs() -> None:
    for d in (SPRITES_OUT, UI_OUT, BACKGROUNDS_OUT, BGM_OUT, SFX_OUT, FONTS_OUT):
        d.mkdir(parents=True, exist_ok=True)


def extract_static_sprite(
    src_rel: str,
    dest_name: str,
    out_dir: Path = SPRITES_OUT,
    rel_prefix: str = "sprites",
    crop_box: tuple[int, int, int, int] | None = None,
    auto_crop: bool = True,
) -> str:
    src_path = VEGAS_SOURCE / src_rel
    if not src_path.exists():
        raise FileNotFoundError(f"Missing static sprite source: {src_path}")
    im = Image.open(src_path).convert("RGBA")
    if crop_box is not None:
        im = im.crop(crop_box)
    if auto_crop:
        bbox = im.getbbox()
        if bbox:
            im = im.crop(bbox)
    out_path = out_dir / f"{dest_name}.png"
    im.save(out_path, "PNG")
    return f"{rel_prefix}/{dest_name}.png"


def extract_button_states(btn_id: str, key_prefix: str, sprites_map: dict[str, str], auto_crop: bool = True) -> None:
    for suffix, fname in (("up", "1_up.png"), ("over", "2_over.png"), ("down", "3_down.png")):
        sprites_map[f"{key_prefix}_{suffix}"] = extract_static_sprite(
            f"buttons/{btn_id}/{fname}",
            f"{key_prefix}_{suffix}",
            out_dir=UI_OUT,
            rel_prefix="ui",
            auto_crop=auto_crop,
        )


def extract_background(src_rel: str, dest_name: str, crop_box=None) -> str:
    src_path = VEGAS_SOURCE / src_rel
    if not src_path.exists():
        raise FileNotFoundError(f"Missing background source: {src_path}")
    im = Image.open(src_path).convert("RGBA")
    if crop_box is not None:
        im = im.crop(crop_box)
    out_path = BACKGROUNDS_OUT / f"{dest_name}.png"
    im.save(out_path, "PNG")
    return f"backgrounds/{dest_name}.png"


def extract_animation_sequence(
    sprite_id: str,
    frame_numbers: list[int],
    anim_key: str,
    world_scale: float,
    play_mode: str = "LOOP",
    frame_duration: float = VEGAS_FRAME_DURATION,
    anchor_x_override: float | None = None,
    anchor_y: float = 0.0,
    anchor_from_first_frame: bool = True,
) -> dict:
    """Compute union bounding box across all frames and crop uniformly to prevent jitter."""
    src_dir = VEGAS_SOURCE / "sprites" / sprite_id
    for stale in SPRITES_OUT.glob(f"{anim_key}_*.png"):
        stale.unlink()

    images = []
    first_bbox = None
    ux0, uy0, ux1, uy1 = None, None, None, None

    for idx, fnum in enumerate(frame_numbers):
        fpath = src_dir / f"{fnum}.png"
        if not fpath.exists():
            raise FileNotFoundError(f"Missing animation frame: {fpath}")
        im = Image.open(fpath).convert("RGBA")
        images.append(im)
        bb = im.getbbox()
        if idx == 0:
            first_bbox = bb
        if bb:
            ux0 = bb[0] if ux0 is None else min(ux0, bb[0])
            uy0 = bb[1] if uy0 is None else min(uy0, bb[1])
            ux1 = bb[2] if ux1 is None else max(ux1, bb[2])
            uy1 = bb[3] if uy1 is None else max(uy1, bb[3])

    if ux0 is None:
        raise ValueError(f"Empty animation sequence: {sprite_id} {frame_numbers}")

    union_box = (ux0, uy0, ux1, uy1)
    union_w = ux1 - ux0
    union_h = uy1 - uy0

    if anchor_x_override is not None:
        anchor_x = anchor_x_override
    elif anchor_from_first_frame and first_bbox is not None:
        f1_center_x = (first_bbox[0] + first_bbox[2]) * 0.5
        anchor_x = round((f1_center_x - ux0) / union_w, 4)
    else:
        anchor_x = 0.5

    frame_paths = []
    for idx, im in enumerate(images, start=1):
        cropped = im.crop(union_box)
        out_name = f"{anim_key}_{idx:02d}.png"
        cropped.save(SPRITES_OUT / out_name, "PNG")
        frame_paths.append(f"sprites/{out_name}")

    return {
        "frameDuration": round(frame_duration, 4),
        "playMode": play_mode,
        "drawWidth": round(union_w * world_scale, 2),
        "drawHeight": round(union_h * world_scale, 2),
        "anchorX": anchor_x,
        "anchorY": anchor_y,
        "frames": frame_paths,
    }


def extract_audio(src_rel: str, out_dir: Path, dest_key: str, ffmpeg_bin: str | None) -> str:
    src_path = VEGAS_SOURCE / src_rel
    if not src_path.exists():
        raise FileNotFoundError(f"Missing audio source: {src_path}")

    rel_folder = "audio/bgm" if out_dir == BGM_OUT else "audio/sfx"
    if ffmpeg_bin:
        out_path = out_dir / f"{dest_key}.ogg"
        subprocess.run(
            [ffmpeg_bin, "-y", "-i", str(src_path), "-vn", "-c:a", "libvorbis", "-q:a", "5", str(out_path)],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return f"{rel_folder}/{dest_key}.ogg"

    out_path = out_dir / f"{dest_key}.mp3"
    shutil.copy2(src_path, out_path)
    return f"{rel_folder}/{dest_key}.mp3"


def copy_vietnamese_font() -> None:
    src_font = Path(r"C:\Windows\Fonts\arialbd.ttf")
    if src_font.exists():
        shutil.copy2(src_font, FONTS_OUT / "arialbd.ttf")
        print(f"Copied Vietnamese TTF font -> {FONTS_OUT / 'arialbd.ttf'}")


def main() -> None:
    if not VEGAS_SOURCE.exists():
        raise FileNotFoundError(f"Vegas source directory not found: {VEGAS_SOURCE}")

    ensure_dirs()
    copy_vietnamese_font()
    ffmpeg_bin = shutil.which("ffmpeg")
    print(f"ffmpeg detected: {ffmpeg_bin if ffmpeg_bin else 'NO (keeping .mp3)'}")

    sprites = {
        # Cart & Standard Hook compatibility key
        "cart_standard": extract_static_sprite("sprites/DefineSprite_2693/1.png", "cart_standard"),
        "hook_claw": extract_static_sprite("sprites/DefineSprite_2826/1.png", "hook_claw"),

        # 4 Claws across 3 states (SWINGING closed = 2823, EXTENDING open = 2826, RETRACTING = 2830)
        "claw_1_closed": extract_static_sprite("sprites/DefineSprite_2823/1.png", "claw_1_closed"),
        "claw_2_closed": extract_static_sprite("sprites/DefineSprite_2823/2.png", "claw_2_closed"),
        "claw_3_closed": extract_static_sprite("sprites/DefineSprite_2823/3.png", "claw_3_closed", crop_box=(0, 0, 26, 24)),
        "claw_4_closed": extract_static_sprite("sprites/DefineSprite_2823/4.png", "claw_4_closed"),

        "claw_1_open": extract_static_sprite("sprites/DefineSprite_2826/1.png", "claw_1_open"),
        "claw_2_open": extract_static_sprite("sprites/DefineSprite_2826/2.png", "claw_2_open"),
        "claw_3_open": extract_static_sprite("sprites/DefineSprite_2826/3.png", "claw_3_open"),
        "claw_4_open": extract_static_sprite("sprites/DefineSprite_2826/4.png", "claw_4_open"),

        "claw_1_retract": extract_static_sprite("sprites/DefineSprite_2830/1.png", "claw_1_retract"),
        "claw_2_retract": extract_static_sprite("sprites/DefineSprite_2826/2.png", "claw_2_retract"),
        "claw_3_retract": extract_static_sprite("sprites/DefineSprite_2830/3.png", "claw_3_retract"),
        "claw_4_retract": extract_static_sprite("sprites/DefineSprite_2830/4.png", "claw_4_retract"),

        # Australia Mine Items
        "gold_small": extract_static_sprite("sprites/DefineSprite_1844/1.png", "gold_small"),
        "gold_small_med": extract_static_sprite("sprites/DefineSprite_1846/1.png", "gold_small_med"),
        "gold_medium": extract_static_sprite("sprites/DefineSprite_1849/1.png", "gold_medium"),
        "gold_large": extract_static_sprite("sprites/DefineSprite_1851/1.png", "gold_large"),
        "gold_mega": extract_static_sprite("sprites/DefineSprite_1861/1.png", "gold_mega"),
        "gold_pan": extract_static_sprite("sprites/DefineSprite_1865/1.png", "gold_pan"),

        "rock_small": extract_static_sprite("sprites/DefineSprite_1881/1.png", "rock_small"),
        "rock_medium": extract_static_sprite("sprites/DefineSprite_1878/1.png", "rock_medium"),
        "rock_large": extract_static_sprite("sprites/DefineSprite_1873/1.png", "rock_large"),

        "diamond": extract_static_sprite("sprites/DefineSprite_1943/1.png", "diamond"),
        "mystery_bag": extract_static_sprite("sprites/DefineSprite_1884/1.png", "mystery_bag"),
        "tnt_barrel": extract_static_sprite("sprites/DefineSprite_1940/1.png", "tnt_barrel"),

        "skull": extract_static_sprite("sprites/DefineSprite_1951/1.png", "skull"),
        "bone_small": extract_static_sprite("sprites/DefineSprite_1955/1.png", "bone_small"),
        "bone_long_v": extract_static_sprite("sprites/DefineSprite_1958/1.png", "bone_long_v"),
        "bone_long_h": extract_static_sprite("sprites/DefineSprite_1959/1.png", "bone_long_h"),
        "skeleton": extract_static_sprite("sprites/DefineSprite_1963/1.png", "skeleton"),

        "gem_1": extract_static_sprite("sprites/DefineSprite_1989/1.png", "gem_1"),
        "gem_2": extract_static_sprite("sprites/DefineSprite_1990/1.png", "gem_2"),
        "gem_3": extract_static_sprite("sprites/DefineSprite_1991/1.png", "gem_3"),
        "gem_4": extract_static_sprite("sprites/DefineSprite_1992/1.png", "gem_4"),

        # Tracks 1..9 cropped consistently to 800px stage band [273..1073]
        "track_w1": extract_static_sprite("sprites/DefineSprite_1816/1.png", "track_w1", crop_box=(273, 0, 1073, 73), auto_crop=False),

        # Backgrounds & Shop UI chrome
        "bg_sky_w1": extract_background("images/1784.png", "bg_sky_w1"),
        "bg_cave_w1": extract_background("images/1785.png", "bg_cave_w1"),
        "bg_mine_w1": extract_background("images/1787.png", "bg_mine_w1", crop_box=(0, 0, 960, 370)),
        "bg_pass_w1": extract_background("sprites/DefineSprite_1467/1.png", "bg_pass_w1"),
        "bg_result_w1": extract_background("images/1716.jpg", "bg_result_w1"),
        "bg_shop_interior": extract_background("images/1484.jpg", "bg_shop_interior"),
        "bg_shop_frame": extract_background("images/1501.png", "bg_shop_frame"),

        "shop_desc_banner": extract_static_sprite("images/1503.png", "shop_desc_banner", out_dir=UI_OUT, rel_prefix="ui"),
        "shop_speech_bubble": extract_static_sprite("sprites/DefineSprite_1556/1.png", "shop_speech_bubble", out_dir=UI_OUT, rel_prefix="ui"),
        "ui_dynamite_icon": extract_static_sprite("buttons/DefineButton2_1522/1_up.png", "ui_dynamite_icon", out_dir=UI_OUT, rel_prefix="ui"),
        "btn_sound_off": extract_static_sprite("sprites/DefineSprite_3038/2.png", "btn_sound_off", out_dir=UI_OUT, rel_prefix="ui", auto_crop=False),
    }

    for tn in range(1, 10):
        sprites[f"track_{tn}"] = extract_static_sprite(
            f"sprites/DefineSprite_1816/{tn}.png",
            f"track_{tn}",
            crop_box=(273, 0, 1073, 73),
            auto_crop=False,
        )

    # Authentic Vegas Buttons (Up / Over / Down)
    extract_button_states("DefineButton2_3020", "btn_menu", sprites, auto_crop=False)
    extract_button_states("DefineButton2_3030", "btn_pause", sprites, auto_crop=False)
    extract_button_states("DefineButton2_3036", "btn_sound_on", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1578", "btn_next", sprites, auto_crop=False)

    # W1 Australia Shop Item Buttons (preserve exact art bounds per button)
    extract_button_states("DefineButton2_1509", "shop_strength", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1513", "shop_oil", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1517", "shop_clover", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1522", "shop_dynamite", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1527", "shop_watch", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1535", "shop_polish", sprites, auto_crop=False)
    extract_button_states("DefineButton2_1538", "shop_cable", sprites, auto_crop=False)

    # Forensically verified Animations (with union bounding box)
    animations = {
        # Miner states: strictly aligned so miner never jumps or swaps wrong pose
        "miner_idle": extract_animation_sequence(
            "DefineSprite_2707", [1], "miner_idle", MINER_WORLD_SCALE, play_mode="LOOP", anchor_x_override=0.5, anchor_y=0.0
        ),
        "miner_down": extract_animation_sequence(
            "DefineSprite_2707", [1, 2, 3, 4, 5], "miner_down", MINER_WORLD_SCALE, play_mode="NORMAL", anchor_x_override=0.5, anchor_y=0.0
        ),
        # IMPORTANT: odd frames only [1,3..19] skip the baked "Tap <- ->" tutorial arrows on even frames!
        "miner_up": extract_animation_sequence(
            "DefineSprite_2741", [1, 3, 5, 7, 9, 11, 13, 15, 17, 19], "miner_up", MINER_WORLD_SCALE,
            play_mode="LOOP", frame_duration=round(2.0 / 18.0, 4), anchor_x_override=0.4915, anchor_y=0.1642
        ),
        "miner_throw": extract_animation_sequence(
            "DefineSprite_2741", list(range(29, 38)), "miner_throw", MINER_WORLD_SCALE,
            play_mode="NORMAL", anchor_x_override=0.4222, anchor_y=0.0
        ),
        "miner_brake": extract_animation_sequence(
            "DefineSprite_2765", list(range(1, 37)), "miner_brake", MINER_WORLD_SCALE,
            play_mode="NORMAL", anchor_x_override=0.1708, anchor_y=0.0606
        ),
        "miner_strength": extract_animation_sequence(
            "DefineSprite_2750", list(range(1, 7)), "miner_strength", MINER_WORLD_SCALE,
            play_mode="NORMAL", anchor_x_override=0.49, anchor_y=0.0
        ),

        # FX animations
        "fx_explosion": extract_animation_sequence(
            "DefineSprite_1938", list(range(1, 16)), "fx_explosion", FX_WORLD_SCALE,
            play_mode="NORMAL", anchor_x_override=0.5, anchor_y=0.5
        ),
        "fx_smoke": extract_animation_sequence(
            "DefineSprite_2824", list(range(1, 11)), "fx_smoke", 0.48,
            play_mode="NORMAL", anchor_x_override=0.5, anchor_y=0.5
        ),
        "fx_dynamite_fly": extract_animation_sequence(
            "DefineSprite_2740", list(range(1, 5)), "fx_dynamite_fly", 0.45,
            play_mode="LOOP", anchor_x_override=0.5, anchor_y=0.5
        ),

        # Australia Creatures (left-facing cycles; flipped horizontally in renderer when moving right)
        "bilby_walk": extract_animation_sequence(
            "DefineSprite_1985", [39, 40, 41, 42, 43, 45], "bilby_walk", 0.25,
            play_mode="LOOP", anchor_x_override=0.5, anchor_y=0.5
        ),
        "bilby_diamond_walk": extract_animation_sequence(
            "DefineSprite_1987", [14, 15, 16, 17, 18, 19, 20], "bilby_diamond_walk", 0.25,
            play_mode="LOOP", anchor_x_override=0.5, anchor_y=0.5
        ),
        "tasmanian_walk": extract_animation_sequence(
            "DefineSprite_2024", list(range(1, 11)), "tasmanian_walk", 0.28,
            play_mode="LOOP", anchor_x_override=0.5, anchor_y=0.5
        ),

        # W1 Shopkeeper Lex
        "lex_talk": extract_animation_sequence(
            "DefineSprite_1500", [1, 5, 10, 15, 20, 25, 30, 35], "lex_talk", 0.55,
            play_mode="LOOP", frame_duration=round(2.0 / 18.0, 4), anchor_x_override=0.5, anchor_y=0.0
        ),
        "lex_buy_arm": extract_animation_sequence(
            "DefineSprite_1554", [1, 5, 10, 15, 20, 25, 30, 35, 39], "lex_buy_arm", 0.55,
            play_mode="NORMAL", frame_duration=round(2.0 / 18.0, 4), anchor_x_override=0.5, anchor_y=0.0
        ),
    }

    music = {
        "bgm_menu": extract_audio("sounds/132.mp3", BGM_OUT, "bgm_menu", ffmpeg_bin),
        "bgm_w1": extract_audio("sounds/133.mp3", BGM_OUT, "bgm_w1", ffmpeg_bin),
    }

    sounds = {
        "sfx_hook_extend": extract_audio("sounds/Tha_Cau.mp3", SFX_OUT, "sfx_hook_extend", ffmpeg_bin),
        "sfx_hook_retract": extract_audio("sounds/Keo_Cau.mp3", SFX_OUT, "sfx_hook_retract", ffmpeg_bin),
        "sfx_coin": extract_audio("sounds/NhanTien.mp3", SFX_OUT, "sfx_coin", ffmpeg_bin),
        "sfx_tally": extract_audio("sounds/TinhTien.mp3", SFX_OUT, "sfx_tally", ffmpeg_bin),
        "sfx_explosion": extract_audio("sounds/1939.mp3", SFX_OUT, "sfx_explosion", ffmpeg_bin),
        "sfx_countdown_beep": extract_audio("sounds/1825.mp3", SFX_OUT, "sfx_countdown_beep", ffmpeg_bin),
        "sfx_pass": extract_audio("sounds/1479.mp3", SFX_OUT, "sfx_pass", ffmpeg_bin),
        "sfx_nopass": extract_audio("sounds/1730.mp3", SFX_OUT, "sfx_nopass", ffmpeg_bin),
        "sfx_heavy_grab": extract_audio("sounds/1872.mp3", SFX_OUT, "sfx_heavy_grab", ffmpeg_bin),
        "sfx_cable_brake": extract_audio("sounds/2771.mp3", SFX_OUT, "sfx_cable_brake", ffmpeg_bin),
        "sfx_shop_open": extract_audio("sounds/1553.mp3", SFX_OUT, "sfx_shop_open", ffmpeg_bin),
        "sfx_shop_buy": extract_audio("sounds/1559.mp3", SFX_OUT, "sfx_shop_buy", ffmpeg_bin),
        "sfx_shop_talk": extract_audio("sounds/1565.mp3", SFX_OUT, "sfx_shop_talk", ffmpeg_bin),
        "sfx_cart_1": extract_audio("sounds/2817.mp3", SFX_OUT, "sfx_cart_1", ffmpeg_bin),
        "sfx_cart_2": extract_audio("sounds/2818.mp3", SFX_OUT, "sfx_cart_2", ffmpeg_bin),
        "sfx_cart_3": extract_audio("sounds/2819.mp3", SFX_OUT, "sfx_cart_3", ffmpeg_bin),
        "sfx_gem_hit": extract_audio("sounds/3005.mp3", SFX_OUT, "sfx_gem_hit", ffmpeg_bin),
        "sfx_gem_combo": extract_audio("sounds/3010.mp3", SFX_OUT, "sfx_gem_combo", ffmpeg_bin),
    }

    theme_config = {
        "name": "vegas",
        "filter": "LINEAR",
        "sprites": sprites,
        "animations": animations,
        "sounds": sounds,
        "music": music,
    }

    theme_json_path = THEME_DIR / "theme.json"
    with theme_json_path.open("w", encoding="utf-8") as f:
        json.dump(theme_config, f, indent=2)

    print(
        f"Extracted {len(sprites)} static/bg/ui sprites, "
        f"{len(animations)} animations ({sum(len(a['frames']) for a in animations.values())} frames), "
        f"{len(sounds)} SFX, {len(music)} BGM -> {theme_json_path}"
    )


if __name__ == "__main__":
    main()
