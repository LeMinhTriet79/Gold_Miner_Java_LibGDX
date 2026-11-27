import json
import math
import shutil
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

PROJECT_ROOT = Path(__file__).resolve().parent.parent
VEGAS_IMAGES = Path(r"C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source\images")
ARTIFACT_DIR = Path(r"C:\Users\Minh Triet\.gemini\antigravity\brain\757e0f6c-3160-4996-a906-b84c565ce796")
UPLOADED_DIR = ARTIFACT_DIR / ".user_uploaded"
SCRATCH_DIR = ARTIFACT_DIR / "scratch"

REF_16_9_PLAY = UPLOADED_DIR / "media_1795758188784_794c3d3f.jpg"
REF_4_3_CLEAN = UPLOADED_DIR / "media_1795759675734_05ac2f55.jpg"
REF_WORLD_HUB = UPLOADED_DIR / "media_1795763723501_7ce43bc7.jpg"
REF_GOLD_MINE = UPLOADED_DIR / "media_1795763729201_c57c79ea.jpg"

THEME_DIR = PROJECT_ROOT / "assets" / "themes" / "vegas"
UI_OUT = THEME_DIR / "ui"
BG_OUT = THEME_DIR / "backgrounds"
DATA_UI_OUT = PROJECT_ROOT / "assets" / "data" / "ui"
FONTS_OUT = PROJECT_ROOT / "assets" / "fonts"

# ==============================================================================
# Modern Vibrant Cozy Pixel Art Palette (Stardew Valley / Sea of Stars / Tropix)
# STRICTLY 32-bit RGBA: ZERO palette crushing / ZERO PIL.Image.quantize()
# ==============================================================================
OUTLINE = (43, 24, 16, 255)        # #2B1810 Warm Espresso Colored Outline
OUTLINE_MOSS = (30, 59, 42, 255)   # #1E3B2A Deep Moss Green Outline
OUTLINE_PLUM = (58, 30, 46, 255)   # #3A1E2E Warm Plum Shadow Outline

GOLD_HI = (255, 253, 224, 255)     # #FFFDE0 Cream Sparkle Highlight
GOLD_LEMON = (255, 227, 56, 255)   # #FFE338 Radiant Lemon Gold
GOLD_HONEY = (247, 168, 27, 255)   # #F7A81B Warm Honey Gold
GOLD_AMBER = (184, 90, 24, 255)    # #B85A18 Warm Amber Shadow

BRASS_HI = (255, 238, 128, 255)
BRASS_MID = (244, 180, 44, 255)
BRASS_SH = (180, 102, 24, 255)

WOOD_HI = (184, 122, 68, 255)
WOOD_MID = (148, 88, 46, 255)
WOOD_SEAM = (112, 60, 34, 255)
WOOD_INNER_SH = (84, 42, 34, 255)  # Warm plum-chocolate inner shadow

# Canonical 480x270 Australia Map node & trail geometry (top-down PIL pixel coords)
NODE_COORDS_480 = [
    (52, 186),   # Node 1: SW Outback trail start
    (100, 132),  # Node 2: Before creek bridge
    (188, 178),  # Node 3: Junction after upper wooden bridge
    (254, 146),  # Node 4: Central mesa mid plateau
    (324, 96),   # Node 5: Upper mesa cliff overlook
    (410, 118),  # Node 6: Golden Diamond Mine cave entrance
]
NODE_TARGETS = [555, 1150, 1900, 2800, 3800, 5000]

# Smooth walking polyline connecting all 6 nodes along the dirt trail -> wooden bridge -> mesa ramp -> railway
TRAIL_WAYPOINTS_480 = [
    (52, 186),   # 0: Node 1
    (74, 156),   # 1: Trail curve up
    (100, 132),  # 2: Node 2
    (132, 144),  # 3: Creek bank approach
    (154, 154),  # 4: Upper wooden bridge
    (188, 178),  # 5: Node 3
    (222, 162),  # 6: Mesa ramp ascent
    (254, 146),  # 7: Node 4
    (292, 118),  # 8: Mesa upper bend
    (324, 96),   # 9: Node 5
    (244, 212),  # 10: Rail bend south of mesa
    (334, 174),  # 11: Sunny railway approach
    (410, 118),  # 12: Node 6 cave entrance
]
NODE_WAYPOINT_INDICES = [0, 2, 5, 7, 9, 12]


def _blend_patch_feathered(
    dst: Image.Image, src_patch: Image.Image, dst_x: int, dst_y: int, feather_left: int = 12,
    feather_right: int = 12, feather_top: int = 10, feather_bottom: int = 0
) -> None:
    pw, ph = src_patch.size
    mask = Image.new("L", (pw, ph), 255)
    mp = mask.load()
    for y in range(ph):
        fy = 1.0
        if feather_top > 0 and y < feather_top:
            fy = min(fy, (y + 1) / float(feather_top))
        if feather_bottom > 0 and y >= ph - feather_bottom:
            fy = min(fy, (ph - y) / float(feather_bottom))
        for x in range(pw):
            fx = 1.0
            if feather_left > 0 and x < feather_left:
                fx = min(fx, (x + 1) / float(feather_left))
            if feather_right > 0 and x >= pw - feather_right:
                fx = min(fx, (pw - x) / float(feather_right))
            mp[x, y] = int(round(255 * min(fx, fy)))
    dst.paste(src_patch, (dst_x, dst_y), mask)


def _find_rail_y(img: Image.Image, y_min: int, y_max: int, x_min: int, x_max: int) -> int:
    best_y, best_score = (y_min + y_max) // 2, -1
    for y in range(y_min, y_max):
        s = 0
        for x in range(x_min, x_max, 4):
            r0, g0, b0 = img.getpixel((x, y))
            r1, g1, b1 = img.getpixel((x, y + 2))
            s += abs(r0 - r1) + abs(g0 - g1) + abs(b0 - b1)
        if s > best_score:
            best_score = s
            best_y = y
    return best_y


def _lift_warm_shadows_rgb(arr: np.ndarray, floor_rgb: tuple[int, int, int] = (43, 24, 16)) -> np.ndarray:
    """Shift harsh black/dark-gray shadows gently toward warm espresso brown (#2B1810) in full 32-bit precision."""
    out = arr.astype(np.float32)
    lum = (out[:, :, 0] + out[:, :, 1] + out[:, :, 2]) / 3.0
    shadow_weight = np.clip((48.0 - lum) / 48.0, 0.0, 1.0)[:, :, None]
    target = np.array(floor_rgb, dtype=np.float32)[None, None, :]
    out = out * (1.0 - 0.55 * shadow_weight) + target * (0.55 * shadow_weight)
    return np.clip(out, 0, 255).astype(np.uint8)


def _boost_gold_lemon_ramp(arr: np.ndarray, mask: np.ndarray) -> np.ndarray:
    """Shift golden nugget/treasure pixels onto vibrant lemon-gold (#B85A18 -> #F7A81B -> #FFE338 -> #FFFDE0)."""
    out = arr.astype(np.float32)
    r, g, b = out[:, :, 0], out[:, :, 1], out[:, :, 2]
    # Preserve dark brown outlines & red shirt while targeting bright/warm golden metal tones
    gold_mask = mask & (r > 135) & (g > 85) & (b < 145) & ((r - b) > 35)
    t = np.clip((0.55 * r + 0.45 * g - 75.0) / 165.0, 0.0, 1.0)

    # 3-stop hue-shifted ramp: Amber #B85A18 -> Honey #F7A81B -> Lemon #FFE338 -> Cream #FFFDE0
    stops = np.array([0.0, 0.36, 0.72, 1.0], dtype=np.float32)
    pal = np.array([
        [184.0, 90.0, 24.0],
        [247.0, 168.0, 27.0],
        [255.0, 227.0, 56.0],
        [255.0, 253.0, 224.0],
    ], dtype=np.float32)

    nr = np.interp(t, stops, pal[:, 0])
    ng = np.interp(t, stops, pal[:, 1])
    nb = np.interp(t, stops, pal[:, 2])

    out[:, :, 0] = np.where(gold_mask, nr, r)
    out[:, :, 1] = np.where(gold_mask, ng, g)
    out[:, :, 2] = np.where(gold_mask, nb, b)
    return np.clip(out, 0, 255).astype(np.uint8)


def _vibrant_rgba_resize(
    img_rgb: Image.Image,
    target_w: int,
    target_h: int,
    sat: float = 1.15,
    bright: float = 1.08,
    contrast: float = 1.04,
) -> Image.Image:
    """Crisp 32-bit RGBA pixel resize with Stardew-style vibrancy. Zero palette crushing / zero quantize()."""
    enhanced = ImageEnhance.Color(img_rgb).enhance(sat)
    enhanced = ImageEnhance.Brightness(enhanced).enhance(bright)
    enhanced = ImageEnhance.Contrast(enhanced).enhance(contrast)
    arr = _lift_warm_shadows_rgb(np.array(enhanced, dtype=np.uint8))
    warm_img = Image.fromarray(arr, mode="RGB")
    resized = warm_img.resize((target_w, target_h), Image.Resampling.BOX).filter(
        ImageFilter.UnsharpMask(radius=0.75, percent=105, threshold=3)
    )
    return resized.convert("RGBA")


def _draw_sparkle(img: Image.Image, cx: int, cy: int, arm: int = 3) -> None:
    """Draw a crisp #FFFDE0 4-point pixel sparkle."""
    hi = (255, 253, 224, 255)
    mid = (255, 240, 148, 220)
    w, h = img.size
    px = img.load()
    px[cx, cy] = hi
    for d in range(1, arm + 1):
        col = hi if d <= arm - 1 else mid
        for nx, ny in ((cx - d, cy), (cx + d, cy), (cx, cy - d), (cx, cy + d)):
            if 0 <= nx < w and 0 <= ny < h:
                px[nx, ny] = col


def _draw_cloud_cluster(draw: ImageDraw.ImageDraw, cx: int, cy: int, scale: float = 1.0) -> None:
    """Draw a soft fluffy cream-white pixel cloud with cool sky-tint underside and NO dark outlines."""
    cream = (255, 253, 244, 255)
    shade = (212, 236, 252, 255)
    puffs = [
        (-16, 2, 13, 9),
        (-6, -4, 15, 11),
        (8, -2, 14, 10),
        (17, 3, 11, 8),
    ]
    for dx, dy, rx, ry in puffs:
        x0 = int(round(cx + (dx - rx) * scale))
        y0 = int(round(cy + (dy - ry + 2) * scale))
        x1 = int(round(cx + (dx + rx) * scale))
        y1 = int(round(cy + (dy + ry) * scale))
        draw.ellipse((x0, y0, x1, y1), fill=shade)
    for dx, dy, rx, ry in puffs:
        x0 = int(round(cx + (dx - rx) * scale))
        y0 = int(round(cy + (dy - ry) * scale))
        x1 = int(round(cx + (dx + rx) * scale))
        y1 = int(round(cy + (dy + ry - 2) * scale))
        draw.ellipse((x0, y0, x1, y1), fill=cream)


def _detect_and_dim_sign_bulbs(scene_1024: Image.Image) -> list[dict]:
    """Detect marquee bulbs on GOLD MINER sign, export 480x270 coords, and dim lit bulbs neatly."""
    w, h = scene_1024.size
    px = scene_1024.load()

    rx_max, ry_max = 400, 195
    mask = [[False] * rx_max for _ in range(ry_max)]
    for y in range(ry_max):
        for x in range(rx_max):
            r, g, b = px[x, y]
            if b > 135 and b > r + 20 and g > 105:
                mask[y][x] = True

    vis = [[False] * rx_max for _ in range(ry_max)]
    bulbs = []
    for y in range(ry_max):
        for x in range(rx_max):
            if mask[y][x] and not vis[y][x]:
                q = deque([(x, y)])
                vis[y][x] = True
                pts = []
                while q:
                    cx, cy = q.popleft()
                    pts.append((cx, cy))
                    for dx in (-1, 0, 1):
                        for dy in (-1, 0, 1):
                            if dx == 0 and dy == 0:
                                continue
                            nx, ny = cx + dx, cy + dy
                            if 0 <= nx < rx_max and 0 <= ny < ry_max and mask[ny][nx] and not vis[ny][nx]:
                                vis[ny][nx] = True
                                q.append((nx, ny))
                if 16 <= len(pts) <= 350:
                    mx = sum(p[0] for p in pts) / len(pts)
                    my = sum(p[1] for p in pts) / len(pts)
                    bulbs.append((mx, my, pts))

    if bulbs:
        cx_avg = sum(b[0] for b in bulbs) / len(bulbs)
        cy_avg = sum(b[1] for b in bulbs) / len(bulbs)
        bulbs.sort(key=lambda b: (math.atan2(b[0] - cx_avg, -(b[1] - cy_avg)) % (2 * math.pi)))

    for _, _, pts in bulbs:
        for bx, by in pts:
            r, g, b = px[bx, by]
            lum = (r + g + b) / 3.0
            if lum > 175:
                px[bx, by] = (62, 134, 178)
            elif lum > 130:
                px[bx, by] = (46, 106, 146)

    bulb_json = []
    for mx, my, _ in bulbs:
        wx = round((mx / w) * 480.0, 2)
        wy = round(24.0 + (1.0 - (my / h)) * 246.0, 2)
        bulb_json.append({"x": wx, "y": wy})

    DATA_UI_OUT.mkdir(parents=True, exist_ok=True)
    with (DATA_UI_OUT / "marquee_bulbs.json").open("w", encoding="utf-8") as f:
        json.dump({"bulbs": bulb_json}, f, indent=2)
    print(f"Detected & dimmed {len(bulb_json)} sign bulbs -> {DATA_UI_OUT / 'marquee_bulbs.json'}")
    return bulb_json


def _apply_daylight_stardew_grade(scene_1024: Image.Image) -> Image.Image:
    """Upgrade sunset scene to sunny Stardew Valley daylight, blue sky + cream clouds, and lemon-gold nugget."""
    arr = np.array(scene_1024, dtype=np.uint8)
    h, w, _ = arr.shape

    # 1. Replace gloomy dark-orange/violet sunset sky (right half y<275) with crisp sunny sky-blue gradient + clouds
    r = arr[:, :, 0].astype(np.int32)
    g = arr[:, :, 1].astype(np.int32)
    b = arr[:, :, 2].astype(np.int32)

    is_sky_tone = (r > 110) & ((r - g) >= 18) & ((r - b) >= 10) & (g >= 72)
    visited = np.zeros((h, w), dtype=bool)
    sky_mask = np.zeros((h, w), dtype=bool)

    q: deque[tuple[int, int]] = deque()
    for sx in range(540, 1020, 20):
        for sy in (4, 18, 42):
            if is_sky_tone[sy, sx] and not visited[sy, sx]:
                visited[sy, sx] = True
                sky_mask[sy, sx] = True
                q.append((sx, sy))
    # Also seed sky pocket between GOLD MINER sign, wood tower & bluff (x=396..530, y=0..72)
    for sx in range(398, 535, 8):
        for sy in (2, 10, 22, 36):
            if is_sky_tone[sy, sx] and not visited[sy, sx]:
                visited[sy, sx] = True
                sky_mask[sy, sx] = True
                q.append((sx, sy))

    while q:
        cx, cy = q.popleft()
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = cx + dx, cy + dy
            if 394 <= nx < w and 0 <= ny < 268 and not visited[ny, nx]:
                if is_sky_tone[ny, nx]:
                    visited[ny, nx] = True
                    sky_mask[ny, nx] = True
                    q.append((nx, ny))

    # Paint sky_mask with crisp stepped daylight sky bands
    sky_grad = np.zeros((h, w, 3), dtype=np.uint8)
    for y in range(268):
        if y < 52:
            col = (76, 184, 240)
        elif y < 105:
            col = (118, 212, 252)
        elif y < 165:
            col = (168, 234, 255)
        else:
            col = (212, 246, 255)
        sky_grad[y, :, :] = col

    # Draw bright fluffy daylight clouds on the sky canvas before masking
    sky_canvas = Image.fromarray(sky_grad, mode="RGB").convert("RGBA")
    sd = ImageDraw.Draw(sky_canvas)
    _draw_cloud_cluster(sd, 675, 58, scale=2.1)
    _draw_cloud_cluster(sd, 865, 92, scale=1.7)
    _draw_cloud_cluster(sd, 560, 108, scale=1.2)
    sky_rgb = np.array(sky_canvas.convert("RGB"), dtype=np.uint8)
    arr[sky_mask] = sky_rgb[sky_mask]

    # 2. Atmospheric Perspective on distant sunset mountains (strictly non-green, non-dark, avoiding crates below y=200)
    mountain_region = np.zeros((h, w), dtype=bool)
    mountain_region[135:238, 610:1024] = True
    r2 = arr[:, :, 0].astype(np.int32)
    g2 = arr[:, :, 1].astype(np.int32)
    b2 = arr[:, :, 2].astype(np.int32)
    is_far_mountain = (
        mountain_region & (~sky_mask)
        & (r2 > 115) & (g2 < r2 - 6) & ((g2 - b2) < 38) & (b2 < 145)
    )
    lum_m = np.clip((r2 + g2 + b2) / (3.0 * 255.0), 0.25, 0.85)
    arr[is_far_mountain, 0] = np.clip(95 + lum_m[is_far_mountain] * 115, 0, 255).astype(np.uint8)
    arr[is_far_mountain, 1] = np.clip(122 + lum_m[is_far_mountain] * 115, 0, 255).astype(np.uint8)
    arr[is_far_mountain, 2] = np.clip(182 + lum_m[is_far_mountain] * 85, 0, 255).astype(np.uint8)

    # 3. Shift Gold Nugget on miner's hand ONLY (preserve skin fingers carefully: high G or bright yellow)
    nugget_box = np.zeros((h, w), dtype=bool)
    nugget_box[246:342, 344:440] = True
    arr = _boost_gold_lemon_ramp(arr, nugget_box)

    # Boost ore cart nuggets STRICTLY inside upper cart basin (y=282..318, x=906..1000) — never touch dirt below!
    cart_box = np.zeros((h, w), dtype=bool)
    cart_box[282:318, 906:1000] = True
    r3 = arr[:, :, 0].astype(np.int32)
    g3 = arr[:, :, 1].astype(np.int32)
    b3 = arr[:, :, 2].astype(np.int32)
    strict_cart_gold = cart_box & (r3 > 160) & (g3 > 115) & ((r3 - g3) < 55)
    arr = _boost_gold_lemon_ramp(arr, strict_cart_gold)

    # Lift sunlit vibrance and convert dark shadows gently toward #2B1810
    daylight = Image.fromarray(arr, mode="RGB")
    daylight = ImageEnhance.Color(daylight).enhance(1.16)
    daylight = ImageEnhance.Brightness(daylight).enhance(1.07)
    lifted = Image.fromarray(_lift_warm_shadows_rgb(np.array(daylight, dtype=np.uint8)), mode="RGB")
    return lifted


def prepare_reference_backdrop() -> None:
    ref_play = Image.open(REF_16_9_PLAY).convert("RGB")  # 1024 x 571
    ref_clean = Image.open(REF_4_3_CLEAN).convert("RGB")  # 1024 x 764

    scene = ref_play.crop((0, 0, 1024, 514)).copy()
    clean_scene = ref_clean.crop((0, 0, 1024, 686))

    rail_clean = _find_rail_y(clean_scene, 440, 560, 250, 600)
    rail_play = _find_rail_y(scene, 350, 460, 250, 520)

    # 1. Clean Top-Right Sky (replace account card x=760..1024, y=0..124)
    sky_patch = clean_scene.crop((655, 0, 985, 165)).resize((264, 124), Image.Resampling.LANCZOS)
    _blend_patch_feathered(scene, sky_patch, 760, 0, feather_left=10, feather_right=0, feather_top=0, feather_bottom=10)

    # 2. Clean Right-Center Hopper, Rails & Foreground (replace PLAY button x=554..878, y=214..514)
    hop_top_h = rail_play - 214
    hop_bot_h = 514 - rail_play
    hop_src_top = clean_scene.crop((642, 286, 866, rail_clean)).resize((324, hop_top_h), Image.Resampling.LANCZOS)
    hop_src_bot = clean_scene.crop((642, rail_clean, 866, 686)).resize((324, hop_bot_h), Image.Resampling.LANCZOS)
    hop_patch = Image.new("RGB", (324, 514 - 214))
    hop_patch.paste(hop_src_top, (0, 0))
    hop_patch.paste(hop_src_bot, (0, hop_top_h))
    _blend_patch_feathered(
        scene, hop_patch, 554, 214,
        feather_left=10, feather_right=12, feather_top=12, feather_bottom=0
    )

    # 3. Remove bottom-right Gemini sparkle (x=886..966, y=446..514)
    rock_patch = clean_scene.crop((780, 578, 864, 686)).resize((80, 68), Image.Resampling.LANCZOS)
    _blend_patch_feathered(scene, rock_patch, 886, 446, feather_left=12, feather_right=12, feather_top=12, feather_bottom=0)

    # Dim sign marquee bulbs cleanly
    _detect_and_dim_sign_bulbs(scene)

    # Preserve authentic warm sunset sky, mountains, pine silhouettes, and faceted gold nugget
    # in full 32-bit RGBA (ZERO quantize, ZERO sky flood-fill, ZERO brightness blowout, ZERO static '+' sparkles)
    scene_480 = (
        scene.resize((480, 246), Image.Resampling.BOX)
        .filter(ImageFilter.UnsharpMask(radius=0.7, percent=95, threshold=3))
        .convert("RGBA")
    )

    canvas = Image.new("RGBA", (480, 270), (0, 0, 0, 255))
    canvas.paste(scene_480, (0, 0))

    # Warm honey-gold bottom bar harmonized with the sunset scene
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((0, 246, 479, 246), fill=(74, 38, 22, 255))
    draw.rectangle((0, 247, 479, 247), fill=(255, 240, 168, 255))
    draw.rectangle((0, 248, 479, 248), fill=(250, 208, 84, 255))
    draw.rectangle((0, 249, 479, 268), fill=(236, 164, 32, 255))
    draw.rectangle((0, 269, 479, 269), fill=(196, 118, 22, 255))

    BG_OUT.mkdir(parents=True, exist_ok=True)
    canvas.save(BG_OUT / "bg_menu_vibrant.png", "PNG")
    # Save menu avatar in full 32-bit RGBA without quantize
    face_box = (133, 224, 377, 468)
    avatar = (
        ref_clean.crop(face_box)
        .resize((30, 30), Image.Resampling.BOX)
        .filter(ImageFilter.UnsharpMask(radius=0.7, percent=95, threshold=3))
        .convert("RGBA")
    )
    avatar.save(UI_OUT / "menu_avatar.png", "PNG")
    print(f"Saved clean 32-bit RGBA sunset backdrop -> {BG_OUT / 'bg_menu_vibrant.png'}")


def _lift_warm_shadows_image(img_rgb: Image.Image) -> Image.Image:
    return Image.fromarray(_lift_warm_shadows_rgb(np.array(img_rgb, dtype=np.uint8)), mode="RGB")


def _draw_pixel_pine_tree(draw: ImageDraw.ImageDraw, cx: int, base_y: int, scale: float = 1.0) -> None:
    """Draw a vibrant emerald cozy pixel pine tree with deep moss colored outline."""
    trunk_col = (132, 76, 42, 255)
    dark_g = (32, 118, 62, 255)
    mid_g = (58, 168, 80, 255)
    hi_g = (114, 222, 102, 255)

    tw = max(2, int(round(4 * scale)))
    th = max(4, int(round(8 * scale)))
    draw.rectangle((cx - tw // 2, base_y - th, cx + tw // 2, base_y), fill=trunk_col, outline=OUTLINE_MOSS)

    tiers = [(12, 10, 0), (10, 9, -6), (7, 8, -12)]
    for half_w, tier_h, y_off in tiers:
        rw = int(round(half_w * scale))
        rh = int(round(tier_h * scale))
        by = int(round(base_y - th + y_off * scale))
        pts = [(cx, by - rh), (cx - rw, by), (cx + rw, by)]
        draw.polygon(pts, fill=mid_g, outline=OUTLINE_MOSS)
        draw.polygon([(cx, by - rh + 1), (cx - rw + 2, by - 1), (cx - 1, by - 1)], fill=hi_g)
        draw.line((cx + 1, by - rh + 2, cx + rw - 1, by - 1), fill=dark_g)


def _fbm_pixel_grid(w: int, h: int, seed: int = 79) -> np.ndarray:
    """Deterministic multi-scale Stardew-style pixel tile texture field in [0, 1]."""
    rng = np.random.default_rng(seed)
    coarse = rng.random((max(4, h // 8), max(4, w // 8)), dtype=np.float32)
    mid = rng.random((max(8, h // 3), max(8, w // 3)), dtype=np.float32)
    fine = rng.random((h, w), dtype=np.float32)
    c_up = np.array(Image.fromarray((coarse * 255).astype(np.uint8)).resize((w, h), Image.Resampling.BILINEAR), dtype=np.float32) / 255.0
    m_up = np.array(Image.fromarray((mid * 255).astype(np.uint8)).resize((w, h), Image.Resampling.NEAREST), dtype=np.float32) / 255.0
    return 0.55 * c_up + 0.30 * m_up + 0.15 * fine


def prepare_hub_scenic_backdrop() -> None:
    """Paint rich 1:1 480x270 32-bit RGBA Stardew-style sunlit valley vista (zero miner, textured pixel grass & cliff)."""
    im = Image.new("RGBA", (480, 270), (76, 184, 240, 255))
    d = ImageDraw.Draw(im)

    # 1. Stepped Bright Sunny Daylight Sky with atmospheric sun glow
    sky_bands = [
        (0, 22, (70, 180, 242, 255)),
        (22, 48, (102, 204, 252, 255)),
        (48, 78, (144, 224, 255, 255)),
        (78, 114, (186, 240, 255, 255)),
        (114, 150, (220, 249, 255, 255)),
    ]
    for y0, y1, col in sky_bands:
        d.rectangle((0, y0, 479, y1), fill=col)

    # Sun + radial warm cream halo
    d.ellipse((346, 6, 418, 78), fill=(255, 248, 206, 255))
    d.ellipse((358, 18, 406, 66), fill=(255, 253, 224, 255))
    _draw_cloud_cluster(d, 88, 36, scale=1.3)
    _draw_cloud_cluster(d, 232, 26, scale=1.15)
    _draw_cloud_cluster(d, 344, 58, scale=0.95)
    _draw_cloud_cluster(d, 444, 34, scale=0.85)

    # 2. Distant Mountains — crisp Stardew stepped facets with ZERO dark outlines
    far_ridge = [(0, 128), (48, 84), (104, 114), (176, 70), (244, 108), (316, 68), (392, 106), (452, 78), (479, 96), (479, 152), (0, 152)]
    d.polygon(far_ridge, fill=(138, 174, 222, 255))
    for px, py, pw in ((176, 70, 32), (316, 68, 34), (48, 84, 24), (452, 78, 22)):
        d.polygon([(px, py), (px + pw, py + 16), (px, py + 44)], fill=(176, 210, 246, 255))

    mid_ridge = [(0, 138), (68, 102), (144, 130), (220, 96), (296, 126), (374, 92), (479, 124), (479, 165), (0, 165)]
    d.polygon(mid_ridge, fill=(104, 148, 202, 255))
    for px, py, pw in ((68, 102, 26), (220, 96, 28), (374, 92, 30)):
        d.polygon([(px, py), (px + pw, py + 14), (px, py + 38)], fill=(132, 174, 224, 255))

    # 3. Sunlit Emerald Valley Meadow with Stardew multi-tone pixel grass texture
    d.polygon([(0, 148), (110, 132), (240, 144), (370, 130), (479, 146), (479, 204), (0, 204)], fill=(76, 192, 92, 255))
    d.polygon([(0, 166), (160, 150), (320, 160), (479, 150), (479, 270), (0, 270)], fill=(104, 212, 102, 255))

    # Apply subtle pixel tile variation onto sunlit grass (y >= 152) via deterministic grid
    arr = np.array(im, dtype=np.uint8)
    tex = _fbm_pixel_grid(480, 270, seed=101)
    grass_mask = (arr[:, :, 1] >= 185) & (arr[:, :, 0] < 130) & (np.arange(270)[:, None] >= 134)
    arr[grass_mask & (tex > 0.68)] = [122, 224, 112, 255]
    arr[grass_mask & (tex < 0.28)] = [84, 194, 88, 255]
    im = Image.fromarray(arr, mode="RGBA")
    d = ImageDraw.Draw(im)

    # Warm sunlit gravel road curving gently across lower valley (textured 2-tone pebbles)
    for i in range(-10, 490, 3):
        py = int(226 + 9 * math.sin(i * 0.014))
        d.ellipse((i - 16, py - 11, i + 16, py + 11), fill=(198, 140, 88, 255))
        d.ellipse((i - 14, py - 9, i + 14, py + 9), fill=(242, 194, 128, 255))
        d.ellipse((i - 10, py - 5, i + 10, py + 5), fill=(252, 218, 162, 255))

    # 4. Left & Right Cozy Prospector Outpost Framing (tucked cleanly along outer edges)
    # Left cozy timber cottage with wood-plank roof, chimney & warm sunlit pine grove
    d.ellipse((6, 156, 52, 174), fill=(72, 162, 74, 255))  # soft grass shadow
    d.rectangle((12, 128, 46, 164), fill=WOOD_MID, outline=OUTLINE)
    for py_plank in range(134, 162, 6):
        d.line((13, py_plank, 45, py_plank), fill=WOOD_SEAM)
    d.polygon([(6, 130), (29, 110), (52, 130)], fill=(216, 94, 72, 255), outline=OUTLINE)
    d.polygon([(12, 126), (29, 113), (38, 126)], fill=(238, 124, 92, 255))
    d.rectangle((22, 140, 36, 154), fill=GOLD_LEMON, outline=OUTLINE)
    d.line((29, 140, 29, 154), fill=OUTLINE)
    d.line((22, 147, 36, 147), fill=OUTLINE)
    _draw_pixel_pine_tree(d, 58, 164, scale=1.12)
    _draw_pixel_pine_tree(d, 16, 194, scale=1.35)
    _draw_pixel_pine_tree(d, 48, 208, scale=0.95)

    # Right cozy timber water-tower with X-bracing, lantern & pine trees
    d.line((428, 132, 428, 174), fill=OUTLINE, width=2)
    d.line((456, 132, 456, 174), fill=OUTLINE, width=2)
    d.line((429, 136, 455, 168), fill=WOOD_MID, width=1)
    d.line((455, 136, 429, 168), fill=WOOD_MID, width=1)
    d.rectangle((422, 112, 462, 136), fill=WOOD_HI, outline=OUTLINE)
    for px_tank in range(428, 460, 6):
        d.line((px_tank, 113, px_tank, 135), fill=WOOD_SEAM)
    d.polygon([(418, 112), (442, 94), (466, 112)], fill=(196, 104, 54, 255), outline=OUTLINE)
    _draw_pixel_pine_tree(d, 412, 168, scale=1.08)
    _draw_pixel_pine_tree(d, 466, 186, scale=1.28)

    # Sunlit foreground grass blades, pebbles, and cozy flowers
    for x in range(8, 474, 14):
        gy = 244 + ((x * 11) % 20)
        d.line((x, gy, x - 2, gy - 4), fill=(62, 162, 72, 255))
        d.line((x + 2, gy, x + 4, gy - 5), fill=(162, 240, 124, 255))
        if (x % 28) == 8:
            d.rectangle((x - 1, gy - 6, x + 1, gy - 4), fill=GOLD_LEMON)
        elif (x % 42) == 22:
            d.rectangle((x - 1, gy - 6, x + 1, gy - 4), fill=(255, 136, 144, 255))

    d.rectangle((442, 224, 446, 258), fill=WOOD_MID, outline=OUTLINE)
    d.rectangle((438, 218, 450, 226), fill=GOLD_HI, outline=OUTLINE)
    d.rectangle((440, 220, 448, 224), fill=GOLD_LEMON)

    im.save(BG_OUT / "bg_hub_scenic.png", "PNG")
    print(f"Painted 100% textured 32-bit RGBA valley backdrop -> {BG_OUT / 'bg_hub_scenic.png'}")


def _draw_eucalyptus_tree(draw: ImageDraw.ImageDraw, cx: int, base_y: int, scale: float = 1.0) -> None:
    """Draw a lush, clustered Stardew-style Outback eucalyptus tree with soft ground shadow."""
    # Warm ground shadow
    sw = max(6, int(round(9 * scale)))
    draw.ellipse((cx - sw, base_y - 2, cx + sw, base_y + 4), fill=(198, 108, 62, 255))
    trunk = (158, 104, 70, 255)
    canopy_sh = (34, 142, 68, 255)
    canopy_mid = (68, 192, 88, 255)
    canopy_hi = (142, 238, 118, 255)

    tw = max(2, int(round(3 * scale)))
    th = max(6, int(round(12 * scale)))
    draw.rectangle((cx - tw // 2, base_y - th, cx + tw // 2, base_y), fill=trunk, outline=OUTLINE)

    # Clustered foliage puffs with crisp moss colored outline
    blobs = [(-6, -12, 6), (6, -11, 6), (-2, -16, 7), (3, -15, 6), (0, -11, 6)]
    for bx, by, r in blobs:
        px = int(round(cx + bx * scale))
        py = int(round(base_y + by * scale))
        rr = max(4, int(round(r * scale)))
        draw.ellipse((px - rr, py - rr, px + rr, py + rr), fill=canopy_mid, outline=OUTLINE_MOSS)
    for bx, by, r in ((-3, -16, 4), (2, -15, 3), (-6, -13, 3)):
        px = int(round(cx + bx * scale))
        py = int(round(base_y + by * scale))
        rr = max(2, int(round(r * scale)))
        draw.ellipse((px - rr, py - rr, px + rr, py + rr), fill=canopy_hi)
    draw.arc((cx - int(7 * scale), base_y - int(18 * scale), cx + int(7 * scale), base_y - int(6 * scale)),
             20, 145, fill=canopy_sh)


def _draw_kangaroo(draw: ImageDraw.ImageDraw, cx: int, cy: int, flip: bool = False) -> None:
    """Draw a cute tiny Outback kangaroo with warm ground shadow & brown outline."""
    draw.ellipse((cx - 5, cy + 4, cx + 5, cy + 8), fill=(198, 108, 62, 255))
    fur = (214, 124, 62, 255)
    fur_hi = (244, 174, 108, 255)
    d = -1 if flip else 1
    draw.polygon([(cx - d * 2, cy + 1), (cx - d * 9, cy + 3), (cx - d * 7, cy + 5), (cx - d * 1, cy + 3)],
                 fill=fur, outline=OUTLINE)
    draw.ellipse((cx - 4, cy - 3, cx + 4, cy + 5), fill=fur, outline=OUTLINE)
    draw.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=fur_hi)
    hx = cx + d * 3
    draw.ellipse((hx - 2, cy - 7, hx + 3, cy - 2), fill=fur_hi, outline=OUTLINE)
    draw.line((hx, cy - 7, hx - d, cy - 10), fill=OUTLINE)
    draw.line((hx + d, cy - 7, hx + d * 2, cy - 10), fill=OUTLINE)
    draw.line((cx - 1, cy + 5, cx + d * 4, cy + 6), fill=OUTLINE)


def prepare_australia_clean_map() -> None:
    """Paint 100% original, richly textured 480x270 32-bit RGBA Australia Outback map (ZERO baked stars/locks/numbers!)."""
    # Start with organic multi-octave Outback desert & ochre earth texture grid (zero flat-vector feel)
    tex = _fbm_pixel_grid(480, 270, seed=42)
    yy, xx = np.mgrid[0:270, 0:480]

    # Smooth sunny desert gradient from warm terracotta (top-left) to sunlit golden ochre (SE)
    sand_blend = np.clip((xx / 480.0) * 0.45 + (yy / 270.0) * 0.55, 0.0, 1.0)
    terracotta = np.array([242.0, 138.0, 74.0], dtype=np.float32)
    ochre = np.array([252.0, 198.0, 122.0], dtype=np.float32)
    base_rgb = terracotta * (1.0 - sand_blend[:, :, None]) + ochre * sand_blend[:, :, None]

    # Add Stardew-style organic stepped sand ripples & warm clay variations
    base_rgb[tex > 0.64] += np.array([12.0, 14.0, 12.0], dtype=np.float32)
    base_rgb[tex < 0.30] -= np.array([14.0, 16.0, 10.0], dtype=np.float32)
    arr = np.zeros((270, 480, 4), dtype=np.uint8)
    arr[:, :, :3] = np.clip(base_rgb, 0, 255).astype(np.uint8)
    arr[:, :, 3] = 255

    im = Image.fromarray(arr, mode="RGBA")
    d = ImageDraw.Draw(im)

    # Tiny natural Outback rocks & red-sand tufts scattered in desert background
    for rx, ry in ((142, 48), (118, 88), (88, 122), (38, 152), (168, 206), (262, 40), (358, 42), (394, 192)):
        d.ellipse((rx - 5, ry - 2, rx + 5, ry + 3), fill=(214, 104, 62, 255), outline=OUTLINE)
        d.line((rx - 3, ry - 1, rx + 2, ry - 1), fill=(246, 154, 96, 255))

    # 1. Upper-left Layered Red Sandstone Bluff + Cozy Timber Mine Tunnel
    d.polygon([(0, 0), (118, 0), (132, 34), (92, 66), (54, 72), (0, 88)], fill=(208, 86, 56, 255), outline=OUTLINE)
    d.polygon([(0, 0), (112, 0), (120, 22), (76, 42), (0, 48)], fill=(242, 134, 78, 255))
    d.line((8, 62, 86, 48), fill=(164, 62, 44, 255))
    # Mine tunnel & crates
    d.rectangle((34, 52, 60, 80), fill=WOOD_MID, outline=OUTLINE)
    d.rectangle((39, 58, 55, 80), fill=(58, 30, 32, 255))
    d.rectangle((66, 64, 80, 76), fill=WOOD_HI, outline=OUTLINE)
    d.line((66, 64, 80, 76), fill=WOOD_SEAM)
    d.rectangle((84, 62, 96, 74), fill=WOOD_HI, outline=OUTLINE)
    d.line((84, 62, 96, 74), fill=WOOD_SEAM)

    # 2. Sparkling Turquoise Outback Creek with rippled sandy banks
    creek_pts = [
        (172, 94), (168, 118), (146, 144), (134, 170), (130, 196),
        (154, 222), (186, 246), (210, 274),
    ]
    d.line(creek_pts, fill=(252, 220, 154, 255), width=26, joint="curve")
    d.line(creek_pts, fill=(28, 128, 144, 255), width=20, joint="curve")
    d.line(creek_pts, fill=(40, 198, 218, 255), width=16, joint="curve")
    d.line(creek_pts, fill=(92, 234, 246, 255), width=9, joint="curve")
    for wx, wy in ((162, 114), (154, 130), (140, 166), (138, 198), (158, 222), (174, 236), (196, 258)):
        d.line((wx - 3, wy, wx + 2, wy + 1), fill=(255, 253, 240, 255))

    # 3. Winding Dirt Trail connecting Left Mine -> Node 1 -> Node 2 -> Upper Bridge -> Node 3 -> Node 4 -> Node 5
    main_dirt_path = [
        (46, 82), (76, 110), (100, 132), (74, 156), (52, 186), (76, 158), (100, 132),
        (132, 144), (154, 154), (188, 178),
    ]
    d.line(main_dirt_path, fill=(208, 130, 74, 255), width=15, joint="curve")
    d.line(main_dirt_path, fill=(252, 218, 146, 255), width=11, joint="curve")

    # Central Uluru Red Sandstone Mesa (stratified cliff face + warm plateau + dirt ramp to Node 4 & 5)
    d.polygon(
        [(196, 128), (222, 68), (288, 56), (350, 76), (366, 126), (314, 168), (226, 178)],
        fill=(196, 76, 56, 255), outline=OUTLINE
    )
    d.polygon(
        [(204, 122), (228, 74), (286, 64), (344, 82), (346, 112), (286, 146), (224, 156)],
        fill=(242, 124, 72, 255), outline=OUTLINE
    )
    # Cliff strata cracks & sunlit top edge
    d.line((232, 80, 286, 70), fill=(252, 162, 102, 255))
    d.line((206, 144, 238, 166), fill=(156, 54, 44, 255))
    d.line((312, 156, 352, 124), fill=(156, 54, 44, 255))
    d.rectangle((318, 122, 338, 148), fill=WOOD_MID, outline=OUTLINE)
    d.rectangle((322, 127, 334, 148), fill=(58, 30, 32, 255))

    # Trail ramp going smoothly up mesa through Node 4 (254,146) -> Node 5 (324,96)
    mesa_ramp = [(188, 178), (224, 162), (254, 146), (292, 118), (324, 96)]
    d.line(mesa_ramp, fill=(208, 130, 74, 255), width=14, joint="curve")
    d.line(mesa_ramp, fill=(252, 218, 146, 255), width=10, joint="curve")
    # Small pebbles along dirt trails for texture
    for px, py in ((62, 170), (88, 142), (118, 140), (208, 168), (274, 132), (306, 106)):
        d.point((px, py), fill=(196, 124, 70, 255))
        d.point((px + 3, py + 2), fill=(255, 240, 198, 255))

    # Wooden Plank Footbridges over creek
    for bx, by in ((144, 150), (178, 240)):
        bridge = [(bx - 16, by - 7), (bx + 14, by + 10), (bx + 8, by + 19), (bx - 22, by + 2)]
        d.polygon(bridge, fill=WOOD_HI, outline=OUTLINE)
        for t in range(1, 5):
            px0 = int(bx - 16 + t * 5)
            py0 = int(by - 7 + t * 3)
            d.line((px0, py0, px0 - 6, py0 + 9), fill=WOOD_SEAM)

    # 4. Iron Railway Track sweeping smoothly from Node 3 -> southern curve -> Node 6 Cave Entrance
    rail_path = [(192, 184), (218, 206), (244, 212), (290, 196), (334, 174), (376, 146), (410, 118)]
    d.line(rail_path, fill=(210, 148, 92, 255), width=14, joint="curve")
    for seg in range(len(rail_path) - 1):
        x0, y0 = rail_path[seg]
        x1, y1 = rail_path[seg + 1]
        dist = math.hypot(x1 - x0, y1 - y0)
        steps = max(2, int(dist / 6.0))
        dx, dy = (x1 - x0) / dist, (y1 - y0) / dist
        nx_perp, ny_perp = -dy * 5.0, dx * 5.0
        for s in range(steps):
            u = s / float(steps)
            mx, my = x0 + (x1 - x0) * u, y0 + (y1 - y0) * u
            d.line((mx - nx_perp, my - ny_perp, mx + nx_perp, my + ny_perp), fill=OUTLINE, width=2)
    for offset in (-2.5, 2.5):
        pts_rail = []
        for idx, (rx, ry) in enumerate(rail_path):
            tx = rail_path[min(idx + 1, len(rail_path) - 1)][0] - rail_path[max(idx - 1, 0)][0]
            ty = rail_path[min(idx + 1, len(rail_path) - 1)][1] - rail_path[max(idx - 1, 0)][1]
            tl = max(1.0, math.hypot(tx, ty))
            pts_rail.append((rx - (ty / tl) * offset, ry + (tx / tl) * offset))
        d.line(pts_rail, fill=(96, 86, 108, 255), width=2)
        d.line(pts_rail, fill=(224, 236, 250, 255), width=1)

    # 5. Cozy Prospector Shack along lower rail + barrels
    d.line([(200, 250), (266, 232), (324, 214)], fill=(252, 218, 146, 255), width=9, joint="curve")
    d.ellipse((314, 216, 360, 228), fill=(214, 152, 92, 255))
    d.rectangle((318, 194, 356, 222), fill=WOOD_MID, outline=OUTLINE)
    for py_s in range(200, 220, 5):
        d.line((319, py_s, 355, py_s), fill=WOOD_SEAM)
    d.polygon([(314, 196), (337, 178), (360, 196)], fill=(192, 98, 56, 255), outline=OUTLINE)
    d.rectangle((330, 206, 342, 222), fill=(58, 30, 32, 255), outline=OUTLINE)
    d.ellipse((358, 212, 366, 223), fill=WOOD_HI, outline=OUTLINE)
    d.ellipse((366, 210, 374, 221), fill=WOOD_HI, outline=OUTLINE)

    # 6. Radiant Golden Diamond Mine Cave Entrance at Upper Right (Node 6 = 410, 118)
    d.polygon([(368, 72), (412, 42), (479, 54), (479, 134), (432, 134), (376, 118)],
              fill=(216, 94, 58, 255), outline=OUTLINE)
    d.rectangle((390, 74, 430, 120), fill=GOLD_HONEY, outline=OUTLINE)
    d.rectangle((393, 77, 427, 120), fill=GOLD_LEMON)
    d.rectangle((397, 84, 423, 120), fill=(62, 34, 34, 255), outline=OUTLINE)
    diamond_pts = [(410, 56), (422, 65), (417, 76), (403, 76), (398, 65)]
    d.polygon(diamond_pts, fill=(98, 228, 255, 255), outline=OUTLINE)
    d.polygon([(410, 58), (418, 65), (410, 72), (402, 65)], fill=(214, 252, 255, 255))
    for gx, gy, gr in ((384, 108, 6), (436, 110, 6), (396, 68, 4), (425, 68, 4)):
        d.ellipse((gx - gr, gy - gr + 1, gx + gr, gy + gr - 1), fill=GOLD_LEMON, outline=OUTLINE)
        d.ellipse((gx - gr + 2, gy - gr + 2, gx, gy), fill=GOLD_HI)
    _draw_sparkle(im, 382, 92, arm=3)
    _draw_sparkle(im, 438, 92, arm=3)
    _draw_sparkle(im, 410, 50, arm=2)

    # 7. Foreground Cliff Corners + Eucalyptus Trees + Kangaroos
    d.polygon([(0, 212), (48, 224), (72, 270), (0, 270)], fill=(238, 124, 68, 255), outline=OUTLINE)
    d.polygon([(442, 134), (479, 132), (479, 240), (434, 214)], fill=(224, 106, 62, 255), outline=OUTLINE)

    for tx, ty, ts in (
        (16, 166, 1.05), (92, 40, 0.9), (186, 64, 1.0), (402, 174, 1.0),
        (236, 252, 1.05), (308, 256, 0.95), (366, 258, 0.95),
    ):
        _draw_eucalyptus_tree(d, tx, ty, scale=ts)

    _draw_kangaroo(d, 136, 112, flip=False)
    _draw_kangaroo(d, 114, 208, flip=True)
    _draw_kangaroo(d, 148, 248, flip=False)

    im.save(BG_OUT / "map_australia_clean.png", "PNG")
    print(f"Painted 100% textured 32-bit RGBA Australia map -> {BG_OUT / 'map_australia_clean.png'}")

    nodes_json = []
    for idx, (nx_px, ny_px) in enumerate(NODE_COORDS_480):
        nodes_json.append({
            "level": idx + 1,
            "nx": round(nx_px / 480.0, 5),
            "ny": round((270.0 - ny_px) / 270.0, 5),
            "target": NODE_TARGETS[idx],
            "waypointIndex": NODE_WAYPOINT_INDICES[idx],
        })
    wp_json = []
    for wx_px, wy_px in TRAIL_WAYPOINTS_480:
        wp_json.append({
            "nx": round(wx_px / 480.0, 5),
            "ny": round((270.0 - wy_px) / 270.0, 5),
        })

    DATA_UI_OUT.mkdir(parents=True, exist_ok=True)
    with (DATA_UI_OUT / "australia_map_nodes.json").open("w", encoding="utf-8") as f:
        json.dump({"nodes": nodes_json, "trailWaypoints": wp_json}, f, indent=2)
    print(f"Exported matched Australia map node config -> {DATA_UI_OUT / 'australia_map_nodes.json'}")


def _tint_play_button_rgba(im: Image.Image, target_w: int, target_h: int, is_over: bool) -> Image.Image:
    """Process Vegas 187.png/189.png PLAY arrow button into crisp full 32-bit RGBA preserving deep shading & bevels."""
    rgba = im.convert("RGBA")
    arr = np.array(rgba, dtype=np.uint8)
    # Strip soft Photoshop outer drop shadow cleanly
    rgb_sum = arr[:, :, 0].astype(np.int32) + arr[:, :, 1].astype(np.int32) + arr[:, :, 2].astype(np.int32)
    shadow_alpha = (arr[:, :, 3] < 175) & (rgb_sum < 320)
    arr[shadow_alpha | (arr[:, :, 3] < 120), 3] = 0

    clean = Image.fromarray(arr, mode="RGBA").resize((target_w, target_h), Image.Resampling.BOX)
    ca = np.array(clean, dtype=np.uint8)
    alpha_mask = ca[:, :, 3] >= 120

    # Keep authentic deep emerald-green & warm honey-gold shading (subtle +4% lift on hover, NO blowout!)
    if is_over:
        rgb_img = Image.fromarray(ca[:, :, :3], mode="RGB")
        rgb_img = ImageEnhance.Brightness(rgb_img).enhance(1.05)
        rgb_img = ImageEnhance.Color(rgb_img).enhance(1.04)
        ca[:, :, :3] = np.array(rgb_img, dtype=np.uint8)

    ca[:, :, 3] = np.where(alpha_mask, 255, 0).astype(np.uint8)

    # Apply crisp 1px #2B1810 colored outline around the opaque silhouette via NumPy
    padded = np.pad(alpha_mask, 1, mode="constant", constant_values=False)
    interior = (
        padded[1:-1, 1:-1]
        & padded[:-2, 1:-1]
        & padded[2:, 1:-1]
        & padded[1:-1, :-2]
        & padded[1:-1, 2:]
    )
    edge_mask = alpha_mask & (~interior)
    ca[edge_mask] = OUTLINE
    return Image.fromarray(ca, mode="RGBA")


def prepare_play_buttons_vibrant() -> None:
    im_up = Image.open(VEGAS_IMAGES / "187.png").convert("RGBA")
    im_over = Image.open(VEGAS_IMAGES / "189.png").convert("RGBA")

    bb1 = im_up.getbbox()
    bb2 = im_over.getbbox()
    union_box = (min(bb1[0], bb2[0]), min(bb1[1], bb2[1]), max(bb1[2], bb2[2]), max(bb1[3], bb2[3]))
    up_c = im_up.crop(union_box)
    over_c = im_over.crop(union_box)

    play_up = _tint_play_button_rgba(up_c, 148, 124, is_over=False)
    play_over = _tint_play_button_rgba(over_c, 148, 124, is_over=True)
    play_up.save(UI_OUT / "btn_play_up.png", "PNG")
    play_over.save(UI_OUT / "btn_play_over.png", "PNG")

    pad = 16
    gw, gh = 148 + pad * 2, 124 + pad * 2
    base_mask = Image.new("L", (gw, gh), 0)
    base_mask.paste(play_over.getchannel("A"), (pad, pad))

    ring_inner = base_mask.filter(ImageFilter.MaxFilter(5))
    ring_mid = base_mask.filter(ImageFilter.MaxFilter(9))
    ring_outer = base_mask.filter(ImageFilter.MaxFilter(13))

    glow = Image.new("RGBA", (gw, gh), (0, 0, 0, 0))
    ri = ring_inner.load()
    rm = ring_mid.load()
    ro = ring_outer.load()
    gp = glow.load()
    for y in range(gh):
        for x in range(gw):
            if ri[x, y] > 128:
                gp[x, y] = (255, 253, 224, 195)
            elif rm[x, y] > 128:
                gp[x, y] = (255, 227, 56, 130)
            elif ro[x, y] > 128:
                gp[x, y] = (247, 168, 27, 68)
    glow.save(UI_OUT / "btn_play_glow.png", "PNG")
    print("Saved full 32-bit RGBA btn_play_up / btn_play_over / glow")


def prepare_hub_and_mode_cards() -> None:
    hub_ref = Image.open(REF_WORLD_HUB).convert("RGB")
    mine_ref = Image.open(REF_GOLD_MINE).convert("RGB")

    # World Hub 4 artworks in bright natural 32-bit RGBA (ZERO quantize, preserve natural miner skin!)
    hub_specs = {
        "hub_card_mine": ((166, 138, 480, 278), 1.18, 1.10),
        "hub_card_casino": ((543, 138, 857, 278), 1.24, 1.14),
        "hub_card_arcade": ((166, 354, 480, 494), 1.28, 1.24),
        "hub_card_shop": ((543, 354, 857, 494), 1.20, 1.12),
    }
    for key, (box, sat, br) in hub_specs.items():
        rgba = _vibrant_rgba_resize(hub_ref.crop(box), 156, 66, sat=sat, bright=br, contrast=1.05)
        rgba.save(UI_OUT / f"{key}.png", "PNG")

    # Gold Mine 1P & 2P artworks in bright natural 32-bit RGBA (preserve skin tones; add crisp sparkles on gold/gems)
    for key, box in (("mode_card_1p", (166, 156, 480, 424)), ("mode_card_2p", (543, 156, 857, 424))):
        rgba = _vibrant_rgba_resize(mine_ref.crop(box), 156, 124, sat=1.18, bright=1.09, contrast=1.04)
        rgba.save(UI_OUT / f"{key}.png", "PNG")

    # Refresh wooden-brass frames with #2B1810 colored outline & radiant brass highlights
    _draw_wooden_frame(164, 88, footer_h=18, hover=False).save(UI_OUT / "ui_hub_card_frame.png", "PNG")
    _draw_wooden_frame(164, 88, footer_h=18, hover=True).save(UI_OUT / "ui_hub_card_frame_over.png", "PNG")
    _draw_wooden_frame(164, 168, footer_h=38, hover=False).save(UI_OUT / "ui_mode_card_frame.png", "PNG")
    _draw_wooden_frame(164, 168, footer_h=38, hover=True).save(UI_OUT / "ui_mode_card_frame_over.png", "PNG")

    _draw_wooden_panel(76, 24, avatar_slot=False).save(UI_OUT / "ui_btn_back_up.png", "PNG")
    back_over = _draw_wooden_panel(76, 24, avatar_slot=False)
    ImageDraw.Draw(back_over).rectangle((1, 1, 74, 22), outline=BRASS_HI)
    back_over.save(UI_OUT / "ui_btn_back_over.png", "PNG")

    _draw_wooden_panel(184, 28, avatar_slot=False).save(UI_OUT / "ui_title_banner.png", "PNG")
    _draw_wooden_panel(220, 28, avatar_slot=False).save(UI_OUT / "ui_title_banner_wide.png", "PNG")
    _draw_wooden_panel(116, 76, avatar_slot=False).save(UI_OUT / "ui_map_preview.png", "PNG")
    print("Generated vibrant 32-bit RGBA Hub/Mode card artworks & cozy wood frames")


def _draw_wooden_panel(w: int, h: int, avatar_slot: bool = False) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, w - 1, h - 1), fill=OUTLINE)
    d.rectangle((1, 1, w - 2, h - 2), fill=BRASS_MID)
    d.line((1, 1, w - 2, 1), fill=BRASS_HI)
    d.line((1, 1, 1, h - 2), fill=BRASS_HI)
    d.line((2, h - 2, w - 2, h - 2), fill=BRASS_SH)
    d.line((w - 2, 2, w - 2, h - 2), fill=BRASS_SH)

    d.rectangle((3, 3, w - 4, h - 4), fill=OUTLINE)
    d.rectangle((4, 4, w - 5, h - 5), fill=WOOD_MID)
    d.line((4, 4, w - 5, 4), fill=WOOD_INNER_SH)
    d.line((4, 4, 4, h - 5), fill=WOOD_INNER_SH)
    for py in range(16, h - 6, 14):
        d.line((4, py, w - 5, py), fill=WOOD_SEAM)
        d.line((4, py + 1, w - 5, py + 1), fill=WOOD_HI)

    for cx, cy, sx, sy in ((0, 0, 1, 1), (w - 1, 0, -1, 1), (0, h - 1, 1, -1), (w - 1, h - 1, -1, -1)):
        for i in range(9):
            for j in range(4):
                px = cx + sx * i
                py = cy + sy * j
                if 0 <= px < w and 0 <= py < h:
                    im.putpixel((px, py), BRASS_MID)
                px2 = cx + sx * j
                py2 = cy + sy * i
                if 0 <= px2 < w and 0 <= py2 < h:
                    im.putpixel((px2, py2), BRASS_MID)
        for i in range(9):
            im.putpixel((cx + sx * i, cy), OUTLINE)
            im.putpixel((cx, cy + sy * i), OUTLINE)
        im.putpixel((cx + sx * 2, cy + sy * 2), BRASS_HI)

    if avatar_slot:
        d.rectangle((5, 4, 36, 35), fill=OUTLINE)
        d.rectangle((6, 5, 35, 34), fill=WOOD_INNER_SH)
    return im


def _draw_wooden_frame(w: int, h: int, footer_h: int, hover: bool = False) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rim_hi = (255, 250, 178, 255) if hover else BRASS_HI
    rim_mid = (255, 214, 72, 255) if hover else BRASS_MID
    rim_sh = (196, 128, 28, 255) if hover else BRASS_SH

    d.rectangle((0, 0, w - 1, h - 1), outline=OUTLINE)
    d.rectangle((1, 1, w - 2, h - 2), outline=rim_mid)
    d.rectangle((2, 2, w - 3, h - 3), outline=rim_mid)
    d.line((1, 1, w - 2, 1), fill=rim_hi)
    d.line((1, 1, 1, h - 2), fill=rim_hi)
    d.line((2, h - 2, w - 2, h - 2), fill=rim_sh)
    d.line((w - 2, 2, w - 2, h - 2), fill=rim_sh)
    d.rectangle((3, 3, w - 4, h - 4), outline=OUTLINE)

    fy0 = h - footer_h
    d.rectangle((3, fy0 - 1, w - 4, fy0 - 1), fill=OUTLINE)
    d.rectangle((4, fy0, w - 5, h - 5), fill=WOOD_MID)
    d.line((4, fy0, w - 5, fy0), fill=WOOD_HI)
    for py in range(fy0 + 10, h - 6, 12):
        d.line((4, py, w - 5, py), fill=WOOD_SEAM)
        d.line((4, py + 1, w - 5, py + 1), fill=WOOD_HI)

    for cx, cy, sx, sy in ((0, 0, 1, 1), (w - 1, 0, -1, 1), (0, h - 1, 1, -1), (w - 1, h - 1, -1, -1)):
        for i in range(9):
            for j in range(4):
                px = cx + sx * i
                py = cy + sy * j
                if 0 <= px < w and 0 <= py < h:
                    im.putpixel((px, py), rim_mid)
                px2 = cx + sx * j
                py2 = cy + sy * i
                if 0 <= px2 < w and 0 <= py2 < h:
                    im.putpixel((px2, py2), rim_mid)
        for i in range(9):
            im.putpixel((cx + sx * i, cy), OUTLINE)
            im.putpixel((cx, cy + sy * i), OUTLINE)
        im.putpixel((cx + sx * 2, cy + sy * 2), rim_hi)
    return im


def _draw_ticket_button(w: int, h: int, dot_col, hi_col, body_col, sh_col) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((1, 1, w - 2, h - 2), fill=body_col)
    d.line((1, 1, w - 2, 1), fill=hi_col)
    d.line((1, 1, 1, h - 2), fill=hi_col)
    d.line((1, h - 2, w - 2, h - 2), fill=sh_col)
    d.line((w - 2, 1, w - 2, h - 2), fill=sh_col)
    for x in range(0, w, 2):
        im.putpixel((x, 0), dot_col)
        im.putpixel((x, h - 1), dot_col)
    for y in range(0, h, 2):
        im.putpixel((0, y), dot_col)
        im.putpixel((w - 1, y), dot_col)
    return im


def _draw_tab(w: int, h: int, active: bool) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if active:
        fill, hi, sh = GOLD_LEMON, GOLD_HI, GOLD_HONEY
    else:
        fill, hi, sh = (64, 102, 146, 255), (104, 152, 204, 255), (44, 72, 108, 255)
    d.rectangle((1, 1, w - 2, h - 1), fill=fill)
    for cx, cy in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1)):
        im.putpixel((cx, cy), (0, 0, 0, 0))
    d.line((2, 0, w - 3, 0), fill=OUTLINE)
    im.putpixel((1, 1), OUTLINE)
    im.putpixel((w - 2, 1), OUTLINE)
    d.line((0, 2, 0, h - 1), fill=OUTLINE)
    d.line((w - 1, 2, w - 1, h - 1), fill=OUTLINE)
    if not active:
        d.line((0, h - 1, w - 1, h - 1), fill=OUTLINE)
    d.line((2, 1, w - 3, 1), fill=hi)
    d.line((1, 2, 1, h - 2), fill=hi)
    d.line((w - 2, 2, w - 2, h - 2), fill=sh)
    return im


def _draw_input_box(w: int, h: int, focused: bool) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, w - 1, h - 1), fill=OUTLINE)
    d.rectangle((1, 1, w - 2, h - 2), fill=(76, 46, 36, 255) if focused else (64, 38, 30, 255))
    d.line((1, 1, w - 2, 1), fill=(48, 26, 20, 255))
    d.line((1, h - 2, w - 2, h - 2), fill=(112, 72, 54, 255))
    if focused:
        d.rectangle((1, 1, w - 2, h - 2), outline=GOLD_LEMON)
    return im


def _draw_checkbox(checked: bool) -> Image.Image:
    s = 13
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, s - 1, s - 1), fill=OUTLINE)
    d.rectangle((1, 1, s - 2, s - 2), fill=BRASS_MID)
    d.rectangle((2, 2, s - 3, s - 3), fill=(62, 34, 26, 255))
    if checked:
        for px, py in ((3, 6), (4, 7), (5, 8), (6, 7), (7, 6), (8, 5), (9, 4), (4, 6), (5, 7), (6, 6), (7, 5), (8, 4)):
            im.putpixel((px, py), GOLD_HI)
    return im


def _draw_green_action_button(w: int, h: int, hover: bool) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rim_hi = GOLD_HI if hover else BRASS_HI
    rim_sh = GOLD_HONEY if hover else BRASS_SH
    g_hi = (168, 250, 114, 255) if hover else (130, 236, 82, 255)
    g_mid = (66, 206, 62, 255) if hover else (46, 178, 46, 255)
    g_sh = (34, 138, 38, 255) if hover else (26, 118, 32, 255)

    d.rectangle((1, 1, w - 2, h - 2), fill=rim_sh)
    d.line((2, 1, w - 3, 1), fill=rim_hi)
    d.line((1, 2, 1, h - 3), fill=rim_hi)
    d.rectangle((3, 3, w - 4, h - 4), fill=g_mid)
    d.rectangle((3, 3, w - 4, 4), fill=g_hi)
    d.rectangle((3, h - 5, w - 4, h - 4), fill=g_sh)

    for cx, cy in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1),
                   (0, h - 1), (1, h - 1), (0, h - 2), (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        im.putpixel((cx, cy), (0, 0, 0, 0))
    d.line((2, 0, w - 3, 0), fill=OUTLINE)
    d.line((2, h - 1, w - 3, h - 1), fill=OUTLINE)
    d.line((0, 2, 0, h - 3), fill=OUTLINE)
    d.line((w - 1, 2, w - 1, h - 3), fill=OUTLINE)
    for px, py in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        im.putpixel((px, py), OUTLINE)
    return im


def _draw_bevel_button(w: int, h: int, hi_col, mid_col, sh_col) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, w - 1, h - 1), fill=OUTLINE)
    d.rectangle((1, 1, w - 2, h - 2), fill=mid_col)
    d.line((1, 1, w - 2, 1), fill=hi_col)
    d.line((1, 1, 1, h - 2), fill=hi_col)
    d.line((1, h - 2, w - 2, h - 2), fill=sh_col)
    d.line((w - 2, 1, w - 2, h - 2), fill=sh_col)
    return im


def _draw_cozy_mini_miner(step_frame: bool) -> Image.Image:
    """Hand-crafted 24x32 cozy Stardew-style mini-miner sprite (warm brown outline, red shirt, bright face)."""
    im = Image.new("RGBA", (24, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    y_bob = -1 if step_frame else 0

    hat_hi = (164, 102, 54, 255)
    hat_mid = (128, 74, 38, 255)
    hat_band = GOLD_LEMON
    skin = (255, 206, 166, 255)
    cheek = (242, 152, 128, 255)
    beard_hi = (255, 252, 240, 255)
    beard_sh = (212, 218, 232, 255)
    shirt_hi = (244, 82, 60, 255)
    shirt_mid = (214, 54, 42, 255)
    shirt_sh = (158, 34, 36, 255)
    pants = (58, 86, 118, 255)
    boot = (112, 62, 34, 255)

    # Hat crown + wide brim
    d.rectangle((7, 3 + y_bob, 16, 7 + y_bob), fill=hat_mid, outline=OUTLINE)
    d.line((8, 4 + y_bob, 14, 4 + y_bob), fill=hat_hi)
    d.line((8, 6 + y_bob, 15, 6 + y_bob), fill=hat_band)
    d.rectangle((4, 7 + y_bob, 19, 9 + y_bob), fill=hat_mid, outline=OUTLINE)
    d.line((5, 8 + y_bob, 18, 8 + y_bob), fill=hat_hi)

    # Smiling face + winking eye + beard
    d.rectangle((7, 10 + y_bob, 16, 14 + y_bob), fill=skin, outline=OUTLINE)
    d.point((10, 11 + y_bob), fill=OUTLINE)  # winking eye
    d.point((14, 11 + y_bob), fill=(46, 102, 168, 255))  # bright right eye
    d.point((12, 12 + y_bob), fill=cheek)
    d.ellipse((6, 13 + y_bob, 17, 19 + y_bob), fill=beard_hi, outline=OUTLINE)
    d.arc((7, 14 + y_bob, 16, 18 + y_bob), 15, 165, fill=beard_sh)

    # Vibrant red miner shirt + hands
    d.rectangle((6, 18 + y_bob, 17, 25 + y_bob), fill=shirt_mid, outline=OUTLINE)
    d.line((7, 19 + y_bob, 15, 19 + y_bob), fill=shirt_hi)
    d.line((7, 24 + y_bob, 16, 24 + y_bob), fill=shirt_sh)
    d.rectangle((5, 20 + y_bob, 7, 23 + y_bob), fill=shirt_mid, outline=OUTLINE)
    d.rectangle((16, 20 + y_bob, 18, 23 + y_bob), fill=shirt_mid, outline=OUTLINE)
    d.rectangle((5, 23 + y_bob, 7, 24 + y_bob), fill=skin, outline=OUTLINE)
    d.rectangle((16, 23 + y_bob, 18, 24 + y_bob), fill=skin, outline=OUTLINE)

    # Belt + gold buckle + indigo trousers + walking boots
    d.line((7, 25 + y_bob, 16, 25 + y_bob), fill=OUTLINE)
    d.rectangle((11, 24 + y_bob, 12, 25 + y_bob), fill=GOLD_LEMON)
    if step_frame:
        d.rectangle((7, 25, 10, 29), fill=pants, outline=OUTLINE)
        d.rectangle((13, 24, 16, 28), fill=pants, outline=OUTLINE)
        d.rectangle((6, 29, 10, 31), fill=boot, outline=OUTLINE)
        d.rectangle((14, 28, 18, 30), fill=boot, outline=OUTLINE)
    else:
        d.rectangle((7, 25, 11, 29), fill=pants, outline=OUTLINE)
        d.rectangle((12, 25, 16, 29), fill=pants, outline=OUTLINE)
        d.rectangle((6, 29, 10, 31), fill=boot, outline=OUTLINE)
        d.rectangle((13, 29, 17, 31), fill=boot, outline=OUTLINE)
    return im


def _draw_map_disc(w: int, h: int, fill_hi, fill_mid, fill_sh, rim_col) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((1, 4, w - 2, h - 1), fill=OUTLINE)
    d.ellipse((2, 4, w - 3, h - 2), fill=fill_sh)
    d.ellipse((1, 1, w - 2, h - 4), fill=OUTLINE)
    d.ellipse((2, 2, w - 3, h - 5), fill=rim_col)
    d.ellipse((4, 3, w - 5, h - 6), fill=fill_mid)
    d.arc((4, 3, w - 5, h - 7), 195, 345, fill=fill_hi)
    return im


def _draw_padlock(w: int, h: int, is_gold: bool) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    shackle = (218, 230, 246, 255)
    body_mid = GOLD_HONEY if is_gold else (142, 158, 186, 255)
    body_hi = GOLD_HI if is_gold else (198, 212, 236, 255)
    body_sh = GOLD_AMBER if is_gold else (88, 100, 128, 255)

    cx = w // 2
    sw = max(3, w // 4)
    d.arc((cx - sw - 1, 0, cx + sw + 1, h // 2 + 1), 180, 360, fill=OUTLINE, width=3)
    d.arc((cx - sw, 1, cx + sw, h // 2), 180, 360, fill=shackle, width=1)
    by = h // 2 - 2
    d.rectangle((1, by, w - 2, h - 1), fill=OUTLINE)
    d.rectangle((2, by + 1, w - 3, h - 2), fill=body_mid)
    d.line((2, by + 1, w - 3, by + 1), fill=body_hi)
    d.line((2, h - 2, w - 3, h - 2), fill=body_sh)
    d.rectangle((cx - 1, by + 3, cx + 1, by + 5), fill=OUTLINE)
    d.point((cx, by + 6), fill=OUTLINE)
    return im


def prepare_vibrant_ui_and_map_sprites() -> None:
    UI_OUT.mkdir(parents=True, exist_ok=True)

    _draw_wooden_panel(200, 190, avatar_slot=False).save(UI_OUT / "ui_panel_auth.png", "PNG")
    _draw_wooden_panel(136, 40, avatar_slot=True).save(UI_OUT / "ui_account_card.png", "PNG")
    _draw_wooden_panel(136, 88, avatar_slot=False).save(UI_OUT / "ui_account_dropdown.png", "PNG")
    _draw_wooden_panel(312, 182, avatar_slot=False).save(UI_OUT / "ui_modal_panel.png", "PNG")
    _draw_wooden_panel(240, 114, avatar_slot=False).save(UI_OUT / "ui_rename_panel.png", "PNG")

    _draw_ticket_button(
        86, 18, (24, 62, 98, 255), (144, 224, 255, 255), (62, 162, 220, 255), (36, 114, 166, 255)
    ).save(UI_OUT / "ui_btn_ticket_blue_up.png", "PNG")
    _draw_ticket_button(
        86, 18, (18, 46, 76, 255), (192, 242, 255, 255), (82, 186, 242, 255), (48, 136, 192, 255)
    ).save(UI_OUT / "ui_btn_ticket_blue_over.png", "PNG")
    _draw_ticket_button(
        86, 18, (112, 32, 38, 255), (255, 212, 214, 255), (226, 134, 136, 255), (176, 86, 92, 255)
    ).save(UI_OUT / "ui_btn_ticket_red_up.png", "PNG")
    _draw_ticket_button(
        86, 18, (86, 22, 28, 255), (255, 232, 232, 255), (242, 154, 156, 255), (196, 102, 108, 255)
    ).save(UI_OUT / "ui_btn_ticket_red_over.png", "PNG")

    _draw_tab(86, 18, active=True).save(UI_OUT / "ui_tab_active.png", "PNG")
    _draw_tab(86, 18, active=False).save(UI_OUT / "ui_tab_inactive.png", "PNG")

    _draw_input_box(168, 18, focused=False).save(UI_OUT / "ui_input_normal.png", "PNG")
    _draw_input_box(168, 18, focused=True).save(UI_OUT / "ui_input_focus.png", "PNG")

    _draw_checkbox(checked=False).save(UI_OUT / "ui_checkbox_off.png", "PNG")
    _draw_checkbox(checked=True).save(UI_OUT / "ui_checkbox_on.png", "PNG")

    _draw_green_action_button(144, 26, hover=False).save(UI_OUT / "ui_btn_green_up.png", "PNG")
    _draw_green_action_button(144, 26, hover=True).save(UI_OUT / "ui_btn_green_over.png", "PNG")

    _draw_bevel_button(124, 17, (124, 216, 248, 255), (54, 156, 204, 255), (34, 108, 152, 255)).save(UI_OUT / "ui_btn_blue_up.png", "PNG")
    _draw_bevel_button(124, 17, (164, 236, 255, 255), (76, 182, 232, 255), (48, 132, 182, 255)).save(UI_OUT / "ui_btn_blue_over.png", "PNG")
    _draw_bevel_button(124, 17, (242, 134, 124, 255), (212, 86, 78, 255), (154, 48, 46, 255)).save(UI_OUT / "ui_btn_red_up.png", "PNG")
    _draw_bevel_button(124, 17, (255, 162, 150, 255), (234, 108, 98, 255), (178, 62, 56, 255)).save(UI_OUT / "ui_btn_red_over.png", "PNG")

    # Lit marquee bulbs
    lit = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
    lp = lit.load()
    for y in range(10):
        for x in range(10):
            dist = math.hypot(x + 0.5 - 5.0, y + 0.5 - 5.0)
            if dist <= 4.3:
                if dist <= 1.8:
                    lp[x, y] = GOLD_HI
                elif dist <= 3.1:
                    lp[x, y] = (176, 244, 255, 255)
                else:
                    lp[x, y] = (72, 184, 240, 220)
    lit.save(UI_OUT / "menu_bulb_lit.png", "PNG")

    glow = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    gp = glow.load()
    for y in range(12):
        for x in range(12):
            dist = math.hypot(x + 0.5 - 6.0, y + 0.5 - 6.0)
            if dist <= 2.8:
                gp[x, y] = (242, 252, 255, 200)
            elif dist <= 4.3:
                gp[x, y] = (182, 240, 255, 130)
            elif dist <= 5.6:
                gp[x, y] = (118, 214, 252, 65)
    glow.save(UI_OUT / "menu_bulb_glow.png", "PNG")

    # Radiant lemon-gold 12x12 Coin Icon (#FFFDE0 / #FFE338 / #F7A81B / #B85A18)
    coin = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    dc = ImageDraw.Draw(coin)
    dc.ellipse((1, 1, 10, 10), fill=GOLD_HONEY, outline=OUTLINE)
    dc.ellipse((2, 2, 9, 9), fill=GOLD_LEMON)
    dc.point([(3, 3), (4, 3), (3, 4), (4, 4)], fill=GOLD_HI)
    for y in range(4, 8):
        coin.putpixel((5, y), GOLD_HI)
        coin.putpixel((6, y), GOLD_AMBER)
    coin.save(UI_OUT / "menu_coin_icon.png", "PNG")

    # Hand-crafted cozy mini-miner & zero-star map node sprites
    _draw_cozy_mini_miner(step_frame=False).save(UI_OUT / "map_mini_miner_idle.png", "PNG")
    _draw_cozy_mini_miner(step_frame=True).save(UI_OUT / "map_mini_miner_walk.png", "PNG")

    _draw_map_disc(34, 22, GOLD_HI, GOLD_LEMON, GOLD_AMBER, GOLD_HONEY).save(UI_OUT / "map_node_cleared.png", "PNG")
    _draw_map_disc(34, 22, (255, 224, 118, 255), GOLD_HONEY, GOLD_AMBER, GOLD_LEMON).save(UI_OUT / "map_node_unlocked.png", "PNG")
    _draw_map_disc(36, 24, (176, 252, 140, 255), (62, 206, 82, 255), (32, 134, 46, 255), GOLD_LEMON).save(
        UI_OUT / "map_node_selected.png", "PNG"
    )
    _draw_map_disc(34, 22, (158, 174, 206, 255), (104, 118, 152, 255), (66, 74, 104, 255), (186, 198, 224, 255)).save(
        UI_OUT / "map_node_locked.png", "PNG"
    )

    map_glow = Image.new("RGBA", (48, 34), (0, 0, 0, 0))
    dg = ImageDraw.Draw(map_glow)
    dg.ellipse((1, 1, 46, 32), fill=(176, 250, 116, 75))
    dg.ellipse((4, 3, 43, 30), fill=(224, 255, 134, 135))
    dg.ellipse((8, 6, 39, 27), fill=(255, 253, 192, 200))
    map_glow.save(UI_OUT / "map_node_glow.png", "PNG")

    chk = Image.new("RGBA", (14, 14), (0, 0, 0, 0))
    dc = ImageDraw.Draw(chk)
    dc.ellipse((0, 0, 13, 13), fill=OUTLINE_MOSS)
    dc.ellipse((1, 1, 12, 12), fill=(46, 198, 86, 255))
    dc.arc((1, 1, 12, 12), 200, 340, fill=(146, 248, 148, 255))
    for px, py in ((3, 7), (4, 8), (5, 9), (6, 8), (7, 7), (8, 6), (9, 5), (10, 4),
                   (4, 7), (5, 8), (6, 7), (7, 6), (8, 5), (9, 4)):
        chk.putpixel((px, py), GOLD_HI)
    chk.save(UI_OUT / "map_check_badge.png", "PNG")

    _draw_padlock(12, 14, is_gold=False).save(UI_OUT / "map_lock_small.png", "PNG")
    _draw_padlock(16, 18, is_gold=True).save(UI_OUT / "map_lock_big.png", "PNG")
    print("Generated all vibrant 32-bit RGBA UI, coin, mini-miner, and map sprites")


def generate_map_alignment_preview() -> None:
    """Create scratch/map_nodes_preview.png to verify nodes & mini-miner align cleanly on the clean map."""
    SCRATCH_DIR.mkdir(parents=True, exist_ok=True)
    base = Image.open(BG_OUT / "map_australia_clean.png").convert("RGBA")
    cleared_disc = Image.open(UI_OUT / "map_node_cleared.png").convert("RGBA")
    selected_disc = Image.open(UI_OUT / "map_node_selected.png").convert("RGBA")
    locked_disc = Image.open(UI_OUT / "map_node_locked.png").convert("RGBA")
    glow = Image.open(UI_OUT / "map_node_glow.png").convert("RGBA")
    chk = Image.open(UI_OUT / "map_check_badge.png").convert("RGBA")
    lk_s = Image.open(UI_OUT / "map_lock_small.png").convert("RGBA")
    lk_b = Image.open(UI_OUT / "map_lock_big.png").convert("RGBA")
    miner = Image.open(UI_OUT / "map_mini_miner_idle.png").convert("RGBA")

    for i, (cx, cy) in enumerate(NODE_COORDS_480):
        lvl = i + 1
        if lvl in (1, 2):
            base.paste(cleared_disc, (cx - 17, cy - 11), cleared_disc)
            base.paste(chk, (cx + 8, cy - 16), chk)
        elif lvl == 3:
            base.paste(glow, (cx - 24, cy - 17), glow)
            base.paste(selected_disc, (cx - 18, cy - 12), selected_disc)
            base.paste(miner, (cx - 12, cy - 34), miner)
        else:
            base.paste(locked_disc, (cx - 17, cy - 11), locked_disc)
            lk = lk_b if lvl == 6 else lk_s
            base.paste(lk, (cx - lk.width // 2, cy - lk.height - 2), lk)

    base.save(SCRATCH_DIR / "map_nodes_preview.png", "PNG")
    print(f"Saved scratch map alignment check -> {SCRATCH_DIR / 'map_nodes_preview.png'}")


def update_theme_json() -> None:
    theme_path = THEME_DIR / "theme.json"
    with theme_path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    data["filter"] = "NEAREST"

    new_sprite_entries = {
        "bg_menu_16bit": "backgrounds/bg_menu_vibrant.png",
        "bg_hub_canyon": "backgrounds/bg_hub_scenic.png",
        "bg_map_australia": "backgrounds/map_australia_clean.png",
        "menu_avatar": "ui/menu_avatar.png",
        "menu_coin_icon": "ui/menu_coin_icon.png",
        "menu_bulb_lit": "ui/menu_bulb_lit.png",
        "menu_bulb_glow": "ui/menu_bulb_glow.png",
        "btn_play_up": "ui/btn_play_up.png",
        "btn_play_over": "ui/btn_play_over.png",
        "btn_play_glow": "ui/btn_play_glow.png",
        "ui_panel_auth": "ui/ui_panel_auth.png",
        "ui_account_card": "ui/ui_account_card.png",
        "ui_account_dropdown": "ui/ui_account_dropdown.png",
        "ui_modal_panel": "ui/ui_modal_panel.png",
        "ui_rename_panel": "ui/ui_rename_panel.png",
        "ui_btn_ticket_blue_up": "ui/ui_btn_ticket_blue_up.png",
        "ui_btn_ticket_blue_over": "ui/ui_btn_ticket_blue_over.png",
        "ui_btn_ticket_red_up": "ui/ui_btn_ticket_red_up.png",
        "ui_btn_ticket_red_over": "ui/ui_btn_ticket_red_over.png",
        "ui_tab_active": "ui/ui_tab_active.png",
        "ui_tab_inactive": "ui/ui_tab_inactive.png",
        "ui_input_normal": "ui/ui_input_normal.png",
        "ui_input_focus": "ui/ui_input_focus.png",
        "ui_checkbox_off": "ui/ui_checkbox_off.png",
        "ui_checkbox_on": "ui/ui_checkbox_on.png",
        "ui_btn_green_up": "ui/ui_btn_green_up.png",
        "ui_btn_green_over": "ui/ui_btn_green_over.png",
        "ui_btn_blue_up": "ui/ui_btn_blue_up.png",
        "ui_btn_blue_over": "ui/ui_btn_blue_over.png",
        "ui_btn_red_up": "ui/ui_btn_red_up.png",
        "ui_btn_red_over": "ui/ui_btn_red_over.png",
        "hub_card_mine": "ui/hub_card_mine.png",
        "hub_card_casino": "ui/hub_card_casino.png",
        "hub_card_arcade": "ui/hub_card_arcade.png",
        "hub_card_shop": "ui/hub_card_shop.png",
        "mode_card_1p": "ui/mode_card_1p.png",
        "mode_card_2p": "ui/mode_card_2p.png",
        "ui_hub_card_frame": "ui/ui_hub_card_frame.png",
        "ui_hub_card_frame_over": "ui/ui_hub_card_frame_over.png",
        "ui_mode_card_frame": "ui/ui_mode_card_frame.png",
        "ui_mode_card_frame_over": "ui/ui_mode_card_frame_over.png",
        "ui_btn_back_up": "ui/ui_btn_back_up.png",
        "ui_btn_back_over": "ui/ui_btn_back_over.png",
        "ui_title_banner": "ui/ui_title_banner.png",
        "ui_title_banner_wide": "ui/ui_title_banner_wide.png",
        "ui_map_preview": "ui/ui_map_preview.png",
        "map_mini_miner_idle": "ui/map_mini_miner_idle.png",
        "map_mini_miner_walk": "ui/map_mini_miner_walk.png",
        "map_node_cleared": "ui/map_node_cleared.png",
        "map_node_unlocked": "ui/map_node_unlocked.png",
        "map_node_selected": "ui/map_node_selected.png",
        "map_node_locked": "ui/map_node_locked.png",
        "map_node_glow": "ui/map_node_glow.png",
        "map_check_badge": "ui/map_check_badge.png",
        "map_lock_small": "ui/map_lock_small.png",
        "map_lock_big": "ui/map_lock_big.png",
    }

    data["nearestSprites"] = list(new_sprite_entries.keys())
    sprites = data.setdefault("sprites", {})
    sprites.pop("menu_miner", None)
    sprites.pop("menu_sign_base", None)
    sprites.update(new_sprite_entries)

    missing = []
    for k, rel in sprites.items():
        if not (THEME_DIR / rel).is_file():
            missing.append(f"sprite:{k}={rel}")
    for k, anim in data.get("animations", {}).items():
        for fr in anim.get("frames", []):
            if not (THEME_DIR / fr).is_file():
                missing.append(f"anim:{k}={fr}")
    for k, rel in data.get("sounds", {}).items():
        if not (THEME_DIR / rel).is_file():
            missing.append(f"sound:{k}={rel}")
    for k, rel in data.get("music", {}).items():
        if not (THEME_DIR / rel).is_file():
            missing.append(f"music:{k}={rel}")
    if missing:
        raise FileNotFoundError("Missing theme asset files: " + ", ".join(missing))

    with theme_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Updated {theme_path} (filter={data['filter']}, sprites={len(sprites)}, missing=0)")


def copy_pixel_font() -> None:
    FONTS_OUT.mkdir(parents=True, exist_ok=True)
    src = Path(r"C:\Windows\Fonts\tahomabd.ttf")
    if src.exists():
        shutil.copy2(src, FONTS_OUT / "tahomabd.ttf")
        print(f"Copied pixel-hinted Vietnamese font -> {FONTS_OUT / 'tahomabd.ttf'}")


def main() -> None:
    copy_pixel_font()
    prepare_reference_backdrop()
    prepare_hub_scenic_backdrop()
    prepare_australia_clean_map()
    prepare_play_buttons_vibrant()
    prepare_hub_and_mode_cards()
    prepare_vibrant_ui_and_map_sprites()
    generate_map_alignment_preview()
    update_theme_json()


if __name__ == "__main__":
    main()
