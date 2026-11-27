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
REF_WORLD_HUB = UPLOADED_DIR / "media_1795763723501_7ce43bc7.jpg"
REF_GOLD_MINE = UPLOADED_DIR / "media_1795763729201_c57c79ea.jpg"
REF_MAP_AUS = UPLOADED_DIR / "media_1795763735630_a1a7c00c.jpg"

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


def _pixelize_photo(img_rgb: Image.Image, target_w: int, target_h: int, colors: int = 64) -> Image.Image:
    return (
        img_rgb.resize((target_w, target_h), Image.Resampling.BOX)
        .filter(ImageFilter.UnsharpMask(radius=1.0, percent=145, threshold=2))
        .quantize(colors=colors, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        .convert("RGBA")
    )


def prepare_hub_backdrop() -> None:
    """Create a darkened 480x270 16-bit canyon sunset backdrop for World Hub & Mode Select (zero watermark, no reflection)."""
    menu_bg = Image.open(BG_OUT / "bg_menu_16bit.png").convert("RGB")
    top_scene = menu_bg.crop((0, 0, 480, 246)).resize((480, 270), Image.Resampling.BOX)

    # Darken smoothly (58%) so golden-framed wooden cards stand out crisply
    px = top_scene.load()
    for y in range(270):
        for x in range(480):
            r, g, b = px[x, y]
            px[x, y] = (int(r * 0.58), int(g * 0.56), int(b * 0.56))

    hub_bg = _pixelize_photo(top_scene, 480, 270, colors=56)
    hub_bg.save(BG_OUT / "bg_hub_canyon.png", "PNG")
    print(f"Saved quantized 480x270 hub backdrop -> {BG_OUT / 'bg_hub_canyon.png'}")


def _draw_wooden_frame(
    w: int,
    h: int,
    footer_h: int,
    hover: bool = False,
) -> Image.Image:
    """Draw a 16-bit brass-framed wooden card with a transparent inner art window above footer_h."""
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rim_hi = (255, 226, 112, 255) if hover else BRASS_HI
    rim_mid = (242, 180, 56, 255) if hover else BRASS_MID
    rim_sh = (182, 118, 28, 255) if hover else BRASS_SH

    # Outer 1px outline & 3px brass bevel border
    d.rectangle((0, 0, w - 1, h - 1), outline=OUTLINE)
    d.rectangle((1, 1, w - 2, h - 2), outline=rim_mid)
    d.rectangle((2, 2, w - 3, h - 3), outline=rim_mid)
    d.line((1, 1, w - 2, 1), fill=rim_hi)
    d.line((1, 1, 1, h - 2), fill=rim_hi)
    d.line((2, h - 2, w - 2, h - 2), fill=rim_sh)
    d.line((w - 2, 2, w - 2, h - 2), fill=rim_sh)
    d.rectangle((3, 3, w - 4, h - 4), outline=OUTLINE)

    # Bottom wooden label band
    fy0 = h - footer_h
    d.rectangle((3, fy0 - 1, w - 4, fy0 - 1), fill=OUTLINE)
    d.rectangle((4, fy0, w - 5, h - 5), fill=WOOD_MID)
    d.line((4, fy0, w - 5, fy0), fill=WOOD_HI)
    for py in range(fy0 + 10, h - 6, 12):
        d.line((4, py, w - 5, py), fill=WOOD_SEAM)
        d.line((4, py + 1, w - 5, py + 1), fill=WOOD_HI)

    # Brass corner brackets & rivets
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


def prepare_hub_and_mode_cards() -> None:
    hub_ref = Image.open(REF_WORLD_HUB).convert("RGB")
    mine_ref = Image.open(REF_GOLD_MINE).convert("RGB")

    # World Hub 4 inner artworks (strictly inside frames and cleanly above label banners)
    hub_crops = {
        "hub_card_mine": (166, 138, 480, 278),
        "hub_card_casino": (543, 138, 857, 278),
        "hub_card_arcade": (166, 354, 480, 494),
        "hub_card_shop": (543, 354, 857, 494),
    }
    for key, box in hub_crops.items():
        art = _pixelize_photo(hub_ref.crop(box), 156, 66, colors=48)
        art.save(UI_OUT / f"{key}.png", "PNG")

    # Gold Mine 1P & 2P inner artworks (above wooden title footers)
    _pixelize_photo(mine_ref.crop((166, 156, 480, 424)), 156, 124, colors=48).save(UI_OUT / "mode_card_1p.png", "PNG")
    _pixelize_photo(mine_ref.crop((543, 156, 857, 424)), 156, 124, colors=48).save(UI_OUT / "mode_card_2p.png", "PNG")

    # 16-bit wooden-brass card frames (outer 164x88 with 18px label footer; 164x168 with 38px footer)
    _draw_wooden_frame(164, 88, footer_h=18, hover=False).save(UI_OUT / "ui_hub_card_frame.png", "PNG")
    _draw_wooden_frame(164, 88, footer_h=18, hover=True).save(UI_OUT / "ui_hub_card_frame_over.png", "PNG")
    _draw_wooden_frame(164, 168, footer_h=38, hover=False).save(UI_OUT / "ui_mode_card_frame.png", "PNG")
    _draw_wooden_frame(164, 168, footer_h=38, hover=True).save(UI_OUT / "ui_mode_card_frame_over.png", "PNG")

    # Shared top header plaques & preview card
    _draw_wooden_panel(76, 24, avatar_slot=False).save(UI_OUT / "ui_btn_back_up.png", "PNG")
    back_over = _draw_wooden_panel(76, 24, avatar_slot=False)
    d_bo = ImageDraw.Draw(back_over)
    d_bo.rectangle((1, 1, 74, 22), outline=BRASS_HI)
    back_over.save(UI_OUT / "ui_btn_back_over.png", "PNG")

    _draw_wooden_panel(184, 28, avatar_slot=False).save(UI_OUT / "ui_title_banner.png", "PNG")
    _draw_wooden_panel(220, 28, avatar_slot=False).save(UI_OUT / "ui_title_banner_wide.png", "PNG")
    _draw_wooden_panel(116, 76, avatar_slot=False).save(UI_OUT / "ui_map_preview.png", "PNG")
    print("Generated World Hub & Gold Mine card artworks and wooden-brass frames")


def _extract_mini_miner(ref_map: Image.Image) -> None:
    """Cut out the mini-miner standing on Node 3 cleanly (with shirt & boots!) into 24x32 idle + walk sprites."""
    crop = ref_map.crop((370, 268, 422, 353)).convert("RGBA")
    w, h = crop.size
    px = crop.load()

    def is_bg_pixel(r: int, g: int, b: int) -> bool:
        is_bright_orange_or_tan = (g >= 92 and r >= 150) or (r >= 200 and g >= 100)
        is_green_disc = g > r + 10 and g > 105
        is_river = b > r + 12 and b > 115
        is_mid_earth = (105 <= r <= 165 and 82 <= g <= 125 and 60 <= b <= 98 and (r - g) < 55)
        return is_bright_orange_or_tan or is_green_disc or is_river or is_mid_earth

    visited = [[False] * w for _ in range(h)]
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        for y in (0, h - 1):
            r, g, b, _ = px[x, y]
            if is_bg_pixel(r, g, b) and not visited[y][x]:
                visited[y][x] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            r, g, b, _ = px[x, y]
            if is_bg_pixel(r, g, b) and not visited[y][x]:
                visited[y][x] = True
                q.append((x, y))

    while q:
        cx, cy = q.popleft()
        px[cx, cy] = (0, 0, 0, 0)
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = cx + dx, cy + dy
            if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                r, g, b, _ = px[nx, ny]
                if is_bg_pixel(r, g, b):
                    visited[ny][nx] = True
                    q.append((nx, ny))

    # Strip bottom green disc glow carefully while preserving dark brown boots
    for y in range(h - 10, h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 0 and (g > r + 4 or (r > 180 and g > 145)):
                px[x, y] = (0, 0, 0, 0)

    # Keep ONLY largest connected opaque component (drops any stray cliff line on side)
    comp_vis = [[False] * w for _ in range(h)]
    best_comp: list[tuple[int, int]] = []
    all_comps: list[list[tuple[int, int]]] = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0 and not comp_vis[y][x]:
                cq = deque([(x, y)])
                comp_vis[y][x] = True
                comp = []
                while cq:
                    cx, cy = cq.popleft()
                    comp.append((cx, cy))
                    for dx in (-1, 0, 1):
                        for dy in (-1, 0, 1):
                            nx, ny = cx + dx, cy + dy
                            if 0 <= nx < w and 0 <= ny < h and not comp_vis[ny][nx] and px[nx, ny][3] > 0:
                                comp_vis[ny][nx] = True
                                cq.append((nx, ny))
                all_comps.append(comp)
                if len(comp) > len(best_comp):
                    best_comp = comp
    best_set = set(best_comp)
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0 and (x, y) not in best_set:
                px[x, y] = (0, 0, 0, 0)

    bb = crop.getbbox()
    trimmed = crop.crop(bb) if bb else crop
    mw = 20
    mh = max(22, min(30, int(round(trimmed.height * (mw / float(trimmed.width))))))
    quant = _quantize_16bit_sprite(trimmed, mw, mh, brighten=1.04)

    idle = Image.new("RGBA", (24, 32), (0, 0, 0, 0))
    ox = (24 - mw) // 2
    oy = 32 - mh
    idle.paste(quant, (ox, oy), quant)
    idle.save(UI_OUT / "map_mini_miner_idle.png", "PNG")

    walk = Image.new("RGBA", (24, 32), (0, 0, 0, 0))
    walk.paste(quant, (ox, max(0, oy - 1)), quant)
    walk.save(UI_OUT / "map_mini_miner_walk.png", "PNG")
    print("Extracted 24x32 mini-miner idle & walk sprites cleanly")


def _draw_map_disc(w: int, h: int, fill_hi, fill_mid, fill_sh, rim_col) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # Bottom 3px coin thickness
    d.ellipse((1, 4, w - 2, h - 1), fill=OUTLINE)
    d.ellipse((2, 4, w - 3, h - 2), fill=fill_sh)
    # Top oval face
    d.ellipse((1, 1, w - 2, h - 4), fill=OUTLINE)
    d.ellipse((2, 2, w - 3, h - 5), fill=rim_col)
    d.ellipse((4, 3, w - 5, h - 6), fill=fill_mid)
    d.arc((4, 3, w - 5, h - 7), 195, 345, fill=fill_hi)
    return im


def _draw_padlock(w: int, h: int, is_gold: bool) -> Image.Image:
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    shackle = (206, 214, 224, 255)
    body_mid = (232, 178, 44, 255) if is_gold else (146, 158, 174, 255)
    body_hi = (255, 224, 108, 255) if is_gold else (196, 208, 222, 255)
    body_sh = (164, 108, 18, 255) if is_gold else (92, 102, 118, 255)

    cx = w // 2
    sw = max(3, w // 4)
    # Shackle arch
    d.arc((cx - sw - 1, 0, cx + sw + 1, h // 2 + 1), 180, 360, fill=OUTLINE, width=3)
    d.arc((cx - sw, 1, cx + sw, h // 2), 180, 360, fill=shackle, width=1)
    # Lock body
    by = h // 2 - 2
    d.rectangle((1, by, w - 2, h - 1), fill=OUTLINE)
    d.rectangle((2, by + 1, w - 3, h - 2), fill=body_mid)
    d.line((2, by + 1, w - 3, by + 1), fill=body_hi)
    d.line((2, h - 2, w - 3, h - 2), fill=body_sh)
    # Keyhole
    d.rectangle((cx - 1, by + 3, cx + 1, by + 5), fill=OUTLINE)
    d.point((cx, by + 6), fill=OUTLINE)
    return im


def prepare_australia_map_assets() -> None:
    """Clean REF_MAP_AUS (removing ALL stars, baked overlays, and sparkles), quantize to 480x270, and build map sprites."""
    ref_map = Image.open(REF_MAP_AUS).convert("RGB")
    _extract_mini_miner(ref_map)

    scene = ref_map.copy()

    def fill_clean_sand(dx: int, dy: int, dw: int, dh: int, base_rgb: tuple[int, int, int],
                        pebble_rgb: tuple[int, int, int], trail_strip: tuple[int, int, int, int] | None = None) -> None:
        patch = Image.new("RGB", (dw, dh), base_rgb)
        dp = ImageDraw.Draw(patch)
        # Subtle deterministic 16-bit Outback texture speckles so patched regions match flat sand naturally
        for py in range(4, dh - 4, 14):
            for px_i in range(6, dw - 6, 22):
                if ((px_i * 7 + py * 13) % 5) == 0:
                    dp.rectangle((px_i, py, px_i + 3, py + 1), fill=pebble_rgb)
        if trail_strip is not None:
            tx0, ty0, tx1, ty1 = trail_strip
            dp.rectangle((tx0, ty0, tx1, ty1), fill=(238, 174, 108))
        _blend_patch_feathered(scene, patch, dx, dy, 10, 10, 10, 10)

    # Pure Outback palette colors measured directly from landmark-free tiles in REF_MAP_AUS
    ORANGE_SAND = (242, 126, 74)
    ORANGE_PEBBLE = (212, 104, 58)
    TAN_SAND = (246, 178, 110)
    TAN_PEBBLE = (224, 152, 88)
    MESA_RED = (218, 96, 62)
    MESA_SH = (182, 74, 48)

    # 1. Remove Top-Left BACK button (0..180, 0..104) -> clean orange/tan cliff sand
    fill_clean_sand(0, 0, 180, 106, ORANGE_SAND, ORANGE_PEBBLE)

    # 2. Remove Top-Center "WORLD 1: AUSTRALIA" banner (280..735, 0..119) cleanly
    fill_clean_sand(280, 0, 456, 120, TAN_SAND, TAN_PEBBLE)

    # 3. Remove Top-Right Profile Card (768..1024, 0..118) cleanly
    fill_clean_sand(768, 0, 256, 120, TAN_SAND, TAN_PEBBLE)

    # 4. Remove Bottom-Right Preview Box & Gemini Sparkle (804..1024, 380..571) cleanly
    fill_clean_sand(802, 380, 222, 191, TAN_SAND, TAN_PEBBLE)

    # 5. CRITICAL: Remove ALL 3 yellow stars above Node 1 & Node 2 (ZERO STARS)
    fill_clean_sand(56, 336, 94, 44, ORANGE_SAND, ORANGE_PEBBLE)
    fill_clean_sand(160, 224, 92, 46, ORANGE_SAND, ORANGE_PEBBLE)

    # 6. Clean baked disc surfaces, baked mini-miner, and baked padlocks on nodes 1..6 organically
    fill_clean_sand(62, 368, 82, 58, ORANGE_SAND, ORANGE_PEBBLE, None)
    fill_clean_sand(164, 254, 82, 56, ORANGE_SAND, ORANGE_PEBBLE, None)
    fill_clean_sand(348, 262, 102, 152, ORANGE_SAND, ORANGE_PEBBLE, None)
    fill_clean_sand(498, 252, 84, 92, MESA_RED, MESA_SH, None)
    fill_clean_sand(646, 142, 82, 92, MESA_RED, MESA_SH, None)
    # Paint rounded smooth sand trail ovals where nodes 1..5 sit so dirt paths flow continuously
    d_sc = ImageDraw.Draw(scene)
    for cx, cy in ((103, 394), (205, 280), (399, 372), (542, 313), (687, 201)):
        d_sc.ellipse((cx - 26, cy - 12, cx + 26, cy + 12), fill=TAN_SAND)
    # Node 6: fill inner dark mine cave doorway cleanly
    d_sc.rectangle((854, 188, 894, 266), fill=(48, 36, 28))
    d_sc.ellipse((846, 214, 902, 272), fill=(52, 40, 32))

    bg_map = _pixelize_photo(scene, 480, 270, colors=64)
    bg_map.save(BG_OUT / "bg_map_australia.png", "PNG")
    print(f"Saved quantized zero-star 480x270 map -> {BG_OUT / 'bg_map_australia.png'}")

    # Crisp 16-bit map discs, glow aura, checkmark badge (NO STARS!), padlocks
    _draw_map_disc(34, 22, (255, 236, 132, 255), (242, 192, 46, 255), (174, 114, 18, 255), (252, 224, 108, 255)).save(
        UI_OUT / "map_node_cleared.png", "PNG"
    )
    _draw_map_disc(34, 22, (238, 186, 86, 255), (194, 126, 40, 255), (124, 72, 20, 255), (232, 174, 74, 255)).save(
        UI_OUT / "map_node_unlocked.png", "PNG"
    )
    _draw_map_disc(36, 24, (146, 238, 112, 255), (68, 182, 56, 255), (34, 118, 30, 255), (248, 216, 82, 255)).save(
        UI_OUT / "map_node_selected.png", "PNG"
    )
    _draw_map_disc(34, 22, (124, 132, 144, 255), (86, 92, 104, 255), (52, 56, 66, 255), (144, 152, 166, 255)).save(
        UI_OUT / "map_node_locked.png", "PNG"
    )

    glow = Image.new("RGBA", (48, 34), (0, 0, 0, 0))
    dg = ImageDraw.Draw(glow)
    dg.ellipse((1, 1, 46, 32), fill=(180, 238, 86, 75))
    dg.ellipse((4, 3, 43, 30), fill=(224, 248, 118, 135))
    dg.ellipse((8, 6, 39, 27), fill=(255, 250, 156, 195))
    glow.save(UI_OUT / "map_node_glow.png", "PNG")

    # Crisp 14x14 green/gold Checkmark badge (replaces stars completely)
    chk = Image.new("RGBA", (14, 14), (0, 0, 0, 0))
    dc = ImageDraw.Draw(chk)
    dc.ellipse((0, 0, 13, 13), fill=OUTLINE)
    dc.ellipse((1, 1, 12, 12), fill=(48, 174, 52, 255))
    dc.arc((1, 1, 12, 12), 200, 340, fill=(136, 236, 122, 255))
    for px, py in ((3, 7), (4, 8), (5, 9), (6, 8), (7, 7), (8, 6), (9, 5), (10, 4),
                   (4, 7), (5, 8), (6, 7), (7, 6), (8, 5), (9, 4)):
        chk.putpixel((px, py), (255, 246, 168, 255))
    chk.save(UI_OUT / "map_check_badge.png", "PNG")

    _draw_padlock(12, 14, is_gold=False).save(UI_OUT / "map_lock_small.png", "PNG")
    _draw_padlock(16, 18, is_gold=True).save(UI_OUT / "map_lock_big.png", "PNG")

    # Export node coords + winding trail waypoints in normalized [0..1] libGDX Y-up space
    map_data = {
        "nodes": [
            {"level": 1, "nx": 0.100, "ny": 0.310, "target": 555, "waypointIndex": 0},
            {"level": 2, "nx": 0.201, "ny": 0.510, "target": 1150, "waypointIndex": 2},
            {"level": 3, "nx": 0.390, "ny": 0.348, "target": 1900, "waypointIndex": 5},
            {"level": 4, "nx": 0.529, "ny": 0.452, "target": 2800, "waypointIndex": 7},
            {"level": 5, "nx": 0.671, "ny": 0.648, "target": 3800, "waypointIndex": 9},
            {"level": 6, "nx": 0.854, "ny": 0.567, "target": 5000, "waypointIndex": 12},
        ],
        "trailWaypoints": [
            {"nx": 0.100, "ny": 0.310},  # 0: Node 1
            {"nx": 0.145, "ny": 0.425},  # 1: trail curve
            {"nx": 0.201, "ny": 0.510},  # 2: Node 2
            {"nx": 0.275, "ny": 0.460},  # 3: before bridge
            {"nx": 0.328, "ny": 0.415},  # 4: wooden bridge
            {"nx": 0.390, "ny": 0.348},  # 5: Node 3
            {"nx": 0.465, "ny": 0.395},  # 6: trail up toward mesa
            {"nx": 0.529, "ny": 0.452},  # 7: Node 4
            {"nx": 0.605, "ny": 0.560},  # 8: plateau bend
            {"nx": 0.671, "ny": 0.648},  # 9: Node 5
            {"nx": 0.610, "ny": 0.310},  # 10: down to railway curve
            {"nx": 0.745, "ny": 0.428},  # 11: along railway track
            {"nx": 0.854, "ny": 0.567},  # 12: Node 6 cave entrance
        ],
    }
    DATA_UI_OUT.mkdir(parents=True, exist_ok=True)
    with (DATA_UI_OUT / "australia_map_nodes.json").open("w", encoding="utf-8") as f:
        json.dump(map_data, f, indent=2)
    print(f"Saved map node & trail waypoint config -> {DATA_UI_OUT / 'australia_map_nodes.json'}")


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

    data["filter"] = "NEAREST"

    new_sprite_entries = {
        "bg_menu_16bit": "backgrounds/bg_menu_16bit.png",
        "bg_hub_canyon": "backgrounds/bg_hub_canyon.png",
        "bg_map_australia": "backgrounds/bg_map_australia.png",
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
    prepare_hub_backdrop()
    prepare_avatar_from_reference()
    prepare_play_buttons_16bit()
    prepare_16bit_ui_sprites()
    prepare_hub_and_mode_cards()
    prepare_australia_map_assets()
    update_theme_json()


if __name__ == "__main__":
    main()
