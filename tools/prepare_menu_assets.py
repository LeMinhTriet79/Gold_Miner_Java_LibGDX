import json
import math
import shutil
from collections import deque
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

PROJECT_ROOT = Path(__file__).resolve().parent.parent
VEGAS_IMAGES = Path(r"C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source\images")
UPLOADED_DIR = Path(
    r"C:\Users\Minh Triet\.gemini\antigravity\brain\757e0f6c-3160-4996-a906-b84c565ce796\.user_uploaded"
)

REF_16_9_PLAY = UPLOADED_DIR / "media_1795758188784_794c3d3f.jpg"
REF_4_3_CLEAN = UPLOADED_DIR / "media_1795759675734_05ac2f55.jpg"

THEME_DIR = PROJECT_ROOT / "assets" / "themes" / "vegas"
UI_OUT = THEME_DIR / "ui"
BG_OUT = THEME_DIR / "backgrounds"
DATA_UI_OUT = PROJECT_ROOT / "assets" / "data" / "ui"
FONTS_OUT = PROJECT_ROOT / "assets" / "fonts"

OUTLINE = (34, 16, 6, 255)
BRASS_HI = (244, 200, 74, 255)
BRASS_MID = (212, 148, 38, 255)
BRASS_SH = (156, 94, 18, 255)
WOOD_HI = (142, 86, 44, 255)
WOOD_MID = (120, 68, 32, 255)
WOOD_SEAM = (90, 48, 20, 255)
WOOD_INNER_SH = (66, 34, 14, 255)


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


def _detect_and_dim_sign_bulbs(scene_1024: Image.Image) -> list[dict]:
    """Detect marquee bulbs on GOLD MINER sign, export 480x270 coords, and dim lit bulbs to unlit deep blue."""
    w, h = scene_1024.size  # 1024 x 514
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

    # Dim bright cyan/white bulbs into uniform unlit deep-blue marquee bulbs
    for mx, my, pts in bulbs:
        for bx, by in pts:
            r, g, b = px[bx, by]
            # Keep dark outer edge darker, tone bright center to deep slate-blue
            lum = (r + g + b) / 3.0
            if lum > 175:
                px[bx, by] = (28, 108, 158)
            elif lum > 130:
                px[bx, by] = (20, 82, 126)

    # Convert 1024x514 scene coords -> 480x270 libGDX Y-up world coords (scene occupies y=24..270)
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


def prepare_reference_backdrop() -> None:
    ref_play = Image.open(REF_16_9_PLAY).convert("RGB")  # 1024 x 571
    ref_clean = Image.open(REF_4_3_CLEAN).convert("RGB")  # 1024 x 764

    scene = ref_play.crop((0, 0, 1024, 514)).copy()
    clean_scene = ref_clean.crop((0, 0, 1024, 686))

    # Align rail track Y between ref_clean (y=499) and scene (y=401)
    rail_clean = _find_rail_y(clean_scene, 440, 560, 250, 600)
    rail_play = _find_rail_y(scene, 350, 460, 250, 520)

    # 1. Clean Top-Right Sky (replace entire account card x=760..1024, y=0..124)
    # clean_scene x=655..985, y=0..165 has zero pine trees and matching stepped sunset bands
    sky_patch = clean_scene.crop((655, 0, 985, 165)).resize((264, 124), Image.Resampling.LANCZOS)
    _blend_patch_feathered(scene, sky_patch, 760, 0, feather_left=10, feather_right=0, feather_top=0, feather_bottom=10)

    # 2. Clean Right-Center Hopper, Rails & Foreground (replace entire PLAY button x=554..878, y=214..514)
    # clean_scene x=642..866 contains ONLY clean bush/hill, wooden hopper, rails, dirt & grass (no crates, no minecart!)
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

    # 3. Remove bottom-right Gemini sparkle (x=886..966, y=446..514) with clean foreground rocks (x=780..864 in clean_scene)
    rock_patch = clean_scene.crop((780, 578, 864, 686)).resize((80, 68), Image.Resampling.LANCZOS)
    _blend_patch_feathered(scene, rock_patch, 886, 446, feather_left=12, feather_right=12, feather_top=12, feather_bottom=0)

    # Detect & dim sign bulbs for live chasing light animation
    _detect_and_dim_sign_bulbs(scene)

    # Downsample to true 480x246 pixel grid, preserve hard edges, and quantize to crisp 16-bit palette (no dither)
    scene_480 = (
        scene.resize((480, 246), Image.Resampling.BOX)
        .filter(ImageFilter.UnsharpMask(radius=1.0, percent=150, threshold=2))
        .quantize(colors=64, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        .convert("RGBA")
    )
    canvas = Image.new("RGBA", (480, 270), (0, 0, 0, 255))
    canvas.paste(scene_480, (0, 0))

    draw = ImageDraw.Draw(canvas)
    # 16-bit golden bottom bar (y=246..269 image coords => y=0..24 libGDX)
    draw.rectangle((0, 246, 479, 246), fill=(68, 36, 14, 255))
    draw.rectangle((0, 247, 479, 247), fill=(246, 200, 108, 255))
    draw.rectangle((0, 248, 479, 248), fill=(230, 168, 48, 255))
    draw.rectangle((0, 249, 479, 268), fill=(210, 140, 14, 255))
    draw.rectangle((0, 269, 479, 269), fill=(170, 108, 12, 255))

    BG_OUT.mkdir(parents=True, exist_ok=True)
    canvas.save(BG_OUT / "bg_menu_16bit.png", "PNG")
    print(f"Saved quantized 480x270 pixel-grid backdrop -> {BG_OUT / 'bg_menu_16bit.png'}")


def prepare_avatar_from_reference() -> None:
    ref_clean = Image.open(REF_4_3_CLEAN).convert("RGB")
    # Crop miner hat + winking smiling face + upper beard cleanly below the GOLD MINER sign
    face = ref_clean.crop((133, 224, 377, 468))
    avatar = (
        face.resize((30, 30), Image.Resampling.BOX)
        .filter(ImageFilter.UnsharpMask(radius=0.9, percent=140, threshold=2))
        .quantize(colors=32, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        .convert("RGBA")
    )
    avatar.save(UI_OUT / "menu_avatar.png", "PNG")
    print(f"Saved quantized 30x30 pixel-art avatar -> {UI_OUT / 'menu_avatar.png'}")


def _quantize_16bit_sprite(im: Image.Image, target_w: int, target_h: int, brighten: float = 1.0) -> Image.Image:
    # Strip soft Photoshop drop shadow (low alpha & dark gray)
    rgba = im.convert("RGBA")
    px = rgba.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            r, g, b, a = px[x, y]
            if a < 175 and (r + g + b) < 320:
                px[x, y] = (0, 0, 0, 0)
            elif a < 120:
                px[x, y] = (0, 0, 0, 0)

    small = rgba.resize((target_w, target_h), Image.Resampling.BOX)
    sp = small.load()

    # Quantize opaque RGB to 24 crisp stepped colors without dither
    opaque_mask = [[False] * target_w for _ in range(target_h)]
    rgb_for_q = Image.new("RGB", (target_w, target_h), (0, 0, 0))
    rp = rgb_for_q.load()
    for y in range(target_h):
        for x in range(target_w):
            r, g, b, a = sp[x, y]
            if a >= 128:
                opaque_mask[y][x] = True
                if brighten != 1.0:
                    r = min(255, int(r * brighten))
                    g = min(255, int(g * brighten))
                    b = min(255, int(b * brighten))
                rp[x, y] = (r, g, b)

    quantized = rgb_for_q.quantize(colors=24, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")
    qp = quantized.load()

    out = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))
    op = out.load()
    for y in range(target_h):
        for x in range(target_w):
            if not opaque_mask[y][x]:
                continue
            # Check if silhouette boundary -> draw crisp 1px 16-bit dark pixel outline
            is_edge = (
                x == 0 or y == 0 or x == target_w - 1 or y == target_h - 1
                or not opaque_mask[y - 1][x]
                or not opaque_mask[y + 1][x]
                or not opaque_mask[y][x - 1]
                or not opaque_mask[y][x + 1]
            )
            if is_edge:
                op[x, y] = (28, 16, 8, 255)
            else:
                r, g, b = qp[x, y]
                op[x, y] = (r, g, b, 255)
    return out


def prepare_play_buttons_16bit() -> None:
    im_up = Image.open(VEGAS_IMAGES / "187.png").convert("RGBA")
    im_over = Image.open(VEGAS_IMAGES / "189.png").convert("RGBA")

    bb1 = im_up.getbbox()
    bb2 = im_over.getbbox()
    union_box = (min(bb1[0], bb2[0]), min(bb1[1], bb2[1]), max(bb1[2], bb2[2]), max(bb1[3], bb2[3]))
    up_c = im_up.crop(union_box)
    over_c = im_over.crop(union_box)

    play_up = _quantize_16bit_sprite(up_c, 148, 124, brighten=1.0)
    play_over = _quantize_16bit_sprite(over_c, 148, 124, brighten=1.10)
    play_up.save(UI_OUT / "btn_play_up.png", "PNG")
    play_over.save(UI_OUT / "btn_play_over.png", "PNG")

    pad = 16
    gw, gh = 148 + pad * 2, 124 + pad * 2
    base_mask = Image.new("L", (gw, gh), 0)
    base_mask.paste(play_over.getchannel("A"), (pad, pad))

    # Stepped 16-bit pixel aura rings (no smooth Gaussian blur) for crisp Nearest rendering
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
                gp[x, y] = (255, 232, 112, 185)
            elif rm[x, y] > 128:
                gp[x, y] = (255, 206, 68, 120)
            elif ro[x, y] > 128:
                gp[x, y] = (255, 174, 36, 62)
    glow.save(UI_OUT / "btn_play_glow.png", "PNG")
    print("Saved 16-bit quantized btn_play_up / btn_play_over / stepped btn_play_glow")


def _draw_wooden_panel(w: int, h: int, avatar_slot: bool = False) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    # Outer 1px outline
    d.rectangle((0, 0, w - 1, h - 1), fill=OUTLINE)
    # Brass frame
    d.rectangle((1, 1, w - 2, h - 2), fill=BRASS_MID)
    d.line((1, 1, w - 2, 1), fill=BRASS_HI)
    d.line((1, 1, 1, h - 2), fill=BRASS_HI)
    d.line((2, h - 2, w - 2, h - 2), fill=BRASS_SH)
    d.line((w - 2, 2, w - 2, h - 2), fill=BRASS_SH)

    # Inner dark border + wood planks
    d.rectangle((3, 3, w - 4, h - 4), fill=OUTLINE)
    d.rectangle((4, 4, w - 5, h - 5), fill=WOOD_MID)
    # Recessed top/left inner shadow
    d.line((4, 4, w - 5, 4), fill=WOOD_INNER_SH)
    d.line((4, 4, 4, h - 5), fill=WOOD_INNER_SH)
    # Horizontal wood plank seams
    for py in range(16, h - 6, 14):
        d.line((4, py, w - 5, py), fill=WOOD_SEAM)
        d.line((4, py + 1, w - 5, py + 1), fill=WOOD_HI)

    # L-shaped brass corner brackets + rivets
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
        # Bracket outer outline & corner highlight
        for i in range(9):
            im.putpixel((cx + sx * i, cy), OUTLINE)
            im.putpixel((cx, cy + sy * i), OUTLINE)
        im.putpixel((cx + sx * 2, cy + sy * 2), BRASS_HI)

    if avatar_slot:
        # Recessed 32x32 frame for 30x30 avatar on left side of account card (x=5..36, y=4..35)
        d.rectangle((5, 4, 36, 35), fill=OUTLINE)
        d.rectangle((6, 5, 35, 34), fill=WOOD_INNER_SH)

    return im


def _draw_ticket_button(w: int, h: int, dot_col, hi_col, body_col, sh_col) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((1, 1, w - 2, h - 2), fill=body_col)
    d.line((1, 1, w - 2, 1), fill=hi_col)
    d.line((1, 1, 1, h - 2), fill=hi_col)
    d.line((1, h - 2, w - 2, h - 2), fill=sh_col)
    d.line((w - 2, 1, w - 2, h - 2), fill=sh_col)

    # Crisp 16-bit perforated marquee ticket border (alternating 1px dots & 1px gap)
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
        fill, hi, sh = (246, 192, 38, 255), (255, 232, 120, 255), (204, 142, 18, 255)
    else:
        fill, hi, sh = (50, 78, 114, 255), (84, 120, 166, 255), (34, 54, 82, 255)

    d.rectangle((1, 1, w - 2, h - 1), fill=fill)
    # Round top-left and top-right 2px corners cleanly
    for cx, cy in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1)):
        im.putpixel((cx, cy), (0, 0, 0, 0))
    # Dark outline
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
    d.rectangle((1, 1, w - 2, h - 2), fill=(62, 38, 28, 255) if focused else (54, 32, 24, 255))
    d.line((1, 1, w - 2, 1), fill=(38, 20, 14, 255))
    d.line((1, h - 2, w - 2, h - 2), fill=(92, 60, 44, 255))
    if focused:
        d.rectangle((1, 1, w - 2, h - 2), outline=(248, 202, 56, 255))
    return im


def _draw_checkbox(checked: bool) -> Image.Image:
    s = 13
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, s - 1, s - 1), fill=OUTLINE)
    d.rectangle((1, 1, s - 2, s - 2), fill=BRASS_MID)
    d.rectangle((2, 2, s - 3, s - 3), fill=(52, 30, 20, 255))
    if checked:
        ck = (255, 232, 120, 255)
        for px, py in ((3, 6), (4, 7), (5, 8), (6, 7), (7, 6), (8, 5), (9, 4), (4, 6), (5, 7), (6, 6), (7, 5), (8, 4)):
            im.putpixel((px, py), ck)
    return im


def _draw_green_action_button(w: int, h: int, hover: bool) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rim_hi = (255, 224, 96, 255)
    rim_sh = (196, 136, 20, 255)
    g_hi = (154, 242, 98, 255) if hover else (118, 222, 66, 255)
    g_mid = (62, 192, 54, 255) if hover else (44, 164, 40, 255)
    g_sh = (34, 128, 30, 255) if hover else (24, 108, 24, 255)

    d.rectangle((1, 1, w - 2, h - 2), fill=rim_sh)
    d.line((2, 1, w - 3, 1), fill=rim_hi)
    d.line((1, 2, 1, h - 3), fill=rim_hi)
    d.rectangle((3, 3, w - 4, h - 4), fill=g_mid)
    d.rectangle((3, 3, w - 4, 4), fill=g_hi)
    d.rectangle((3, h - 5, w - 4, h - 4), fill=g_sh)

    # Pixel rounded corners + crisp dark 1px border
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


def prepare_16bit_ui_sprites() -> None:
    UI_OUT.mkdir(parents=True, exist_ok=True)

    _draw_wooden_panel(200, 190, avatar_slot=False).save(UI_OUT / "ui_panel_auth.png", "PNG")
    _draw_wooden_panel(136, 40, avatar_slot=True).save(UI_OUT / "ui_account_card.png", "PNG")
    _draw_wooden_panel(136, 88, avatar_slot=False).save(UI_OUT / "ui_account_dropdown.png", "PNG")
    _draw_wooden_panel(312, 182, avatar_slot=False).save(UI_OUT / "ui_modal_panel.png", "PNG")
    _draw_wooden_panel(240, 114, avatar_slot=False).save(UI_OUT / "ui_rename_panel.png", "PNG")

    _draw_ticket_button(
        86, 18, (16, 48, 76, 255), (130, 212, 246, 255), (56, 146, 200, 255), (32, 102, 148, 255)
    ).save(UI_OUT / "ui_btn_ticket_blue_up.png", "PNG")
    _draw_ticket_button(
        86, 18, (10, 34, 58, 255), (176, 236, 255, 255), (72, 170, 224, 255), (42, 124, 174, 255)
    ).save(UI_OUT / "ui_btn_ticket_blue_over.png", "PNG")
    _draw_ticket_button(
        86, 18, (88, 20, 26, 255), (244, 202, 206, 255), (206, 136, 140, 255), (158, 88, 94, 255)
    ).save(UI_OUT / "ui_btn_ticket_red_up.png", "PNG")
    _draw_ticket_button(
        86, 18, (64, 12, 16, 255), (255, 224, 226, 255), (222, 152, 156, 255), (176, 104, 110, 255)
    ).save(UI_OUT / "ui_btn_ticket_red_over.png", "PNG")

    _draw_tab(86, 18, active=True).save(UI_OUT / "ui_tab_active.png", "PNG")
    _draw_tab(86, 18, active=False).save(UI_OUT / "ui_tab_inactive.png", "PNG")

    _draw_input_box(168, 18, focused=False).save(UI_OUT / "ui_input_normal.png", "PNG")
    _draw_input_box(168, 18, focused=True).save(UI_OUT / "ui_input_focus.png", "PNG")

    _draw_checkbox(checked=False).save(UI_OUT / "ui_checkbox_off.png", "PNG")
    _draw_checkbox(checked=True).save(UI_OUT / "ui_checkbox_on.png", "PNG")

    _draw_green_action_button(144, 26, hover=False).save(UI_OUT / "ui_btn_green_up.png", "PNG")
    _draw_green_action_button(144, 26, hover=True).save(UI_OUT / "ui_btn_green_over.png", "PNG")

    _draw_bevel_button(124, 17, (112, 204, 238, 255), (46, 142, 184, 255), (28, 98, 134, 255)).save(UI_OUT / "ui_btn_blue_up.png", "PNG")
    _draw_bevel_button(124, 17, (148, 226, 252, 255), (66, 168, 212, 255), (42, 122, 164, 255)).save(UI_OUT / "ui_btn_blue_over.png", "PNG")
    _draw_bevel_button(124, 17, (230, 126, 118, 255), (196, 82, 76, 255), (138, 44, 40, 255)).save(UI_OUT / "ui_btn_red_up.png", "PNG")
    _draw_bevel_button(124, 17, (248, 152, 144, 255), (220, 104, 96, 255), (164, 58, 52, 255)).save(UI_OUT / "ui_btn_red_over.png", "PNG")

    # Crisp 10x10 16-bit lit bulb
    lit = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
    lp = lit.load()
    for y in range(10):
        for x in range(10):
            dist = math.hypot(x + 0.5 - 5.0, y + 0.5 - 5.0)
            if dist <= 4.3:
                if dist <= 1.8:
                    lp[x, y] = (255, 253, 232, 255)
                elif dist <= 3.1:
                    lp[x, y] = (176, 236, 255, 255)
                else:
                    lp[x, y] = (64, 168, 224, 220)
    lit.save(UI_OUT / "menu_bulb_lit.png", "PNG")

    # Stepped 12x12 pixel bulb glow halo (crisp Nearest rendering)
    glow = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    gp = glow.load()
    for y in range(12):
        for x in range(12):
            dist = math.hypot(x + 0.5 - 6.0, y + 0.5 - 6.0)
            if dist <= 2.8:
                gp[x, y] = (230, 248, 255, 200)
            elif dist <= 4.3:
                gp[x, y] = (176, 234, 255, 130)
            elif dist <= 5.6:
                gp[x, y] = (110, 204, 246, 65)
    glow.save(UI_OUT / "menu_bulb_glow.png", "PNG")

    # Crisp 12x12 16-bit gold coin
    coin = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    dc = ImageDraw.Draw(coin)
    dc.ellipse((1, 1, 10, 10), fill=(204, 132, 16, 255), outline=(92, 46, 8, 255))
    dc.ellipse((2, 2, 9, 9), fill=(248, 196, 36, 255))
    dc.point([(3, 3), (4, 3), (3, 4), (4, 4)], fill=(255, 240, 128, 255))
    for y in range(4, 8):
        coin.putpixel((5, y), (255, 240, 128, 255))
        coin.putpixel((6, y), (204, 132, 16, 255))
    coin.save(UI_OUT / "menu_coin_icon.png", "PNG")
    print("Generated all 16-bit pixel-art UI sprites")


def update_theme_json() -> None:
    theme_path = THEME_DIR / "theme.json"
    with theme_path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    data["nearestSprites"] = [
        "bg_menu_16bit",
        "menu_avatar",
        "menu_coin_icon",
        "menu_bulb_lit",
        "menu_bulb_glow",
        "btn_play_up",
        "btn_play_over",
        "btn_play_glow",
        "ui_panel_auth",
        "ui_account_card",
        "ui_account_dropdown",
        "ui_modal_panel",
        "ui_rename_panel",
        "ui_btn_ticket_blue_up",
        "ui_btn_ticket_blue_over",
        "ui_btn_ticket_red_up",
        "ui_btn_ticket_red_over",
        "ui_tab_active",
        "ui_tab_inactive",
        "ui_input_normal",
        "ui_input_focus",
        "ui_checkbox_off",
        "ui_checkbox_on",
        "ui_btn_green_up",
        "ui_btn_green_over",
        "ui_btn_blue_up",
        "ui_btn_blue_over",
        "ui_btn_red_up",
        "ui_btn_red_over",
    ]
    sprites = data.setdefault("sprites", {})
    sprites.pop("menu_miner", None)
    sprites.pop("menu_sign_base", None)
    sprites.update(
        {
            "bg_menu_16bit": "backgrounds/bg_menu_16bit.png",
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
        }
    )

    with theme_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Updated {theme_path}")


def copy_pixel_font() -> None:
    FONTS_OUT.mkdir(parents=True, exist_ok=True)
    src = Path(r"C:\Windows\Fonts\tahomabd.ttf")
    if src.exists():
        shutil.copy2(src, FONTS_OUT / "tahomabd.ttf")
        print(f"Copied pixel-hinted Vietnamese font -> {FONTS_OUT / 'tahomabd.ttf'}")


def main() -> None:
    copy_pixel_font()
    prepare_reference_backdrop()
    prepare_avatar_from_reference()
    prepare_play_buttons_16bit()
    prepare_16bit_ui_sprites()
    update_theme_json()


if __name__ == "__main__":
    main()
