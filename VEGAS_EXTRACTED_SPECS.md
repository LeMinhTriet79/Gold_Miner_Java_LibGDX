# VEGAS EXTRACTED SPECS — TÀI LIỆU KỸ THUẬT TRÍCH XUẤT TỪ GOLD MINER VEGAS

> **Nguồn gốc:** Khai thác trực tiếp từ mã nguồn ActionScript (`.as`) và tài nguyên (`.png`, `.jpg`, `.mp3`, `.ttf`, `.svg`) tại `C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source`.
> **Phiên bản gốc:** `© Intermix Network, LLC 2005. All Rights Reserved. GMV 1.20` (`scripts/frame_1/DoAction.as`), giải biên dịch bằng JPEXS FFDec từ SWF.
> **Mục đích:** Cung cấp toàn bộ thông số toán học quy đổi theo `dt` (delta time), bảng vật thể, luật chơi, cửa hàng, Vegas Bonus và bản đồ tài nguyên để chuyển thể sang `Gold_Miner_Java_LibGDX` (`FitViewport` `480×270`) mà **không chép code timeline rối của Flash**.

---

## TỔNG QUAN SOURCE & CẤU TRÚC THƯ MỤC GỐC

### Thống kê 12 thư mục trong `Gold_Miner_Vegas_Source`

| Thư mục | Số lượng file | Định dạng thực tế | Ghi chú |
| :--- | :---: | :--- | :--- |
| `scripts/` | 3.012 | `.as` (ActionScript 1/2) | Logic timeline chính, hook, vật thể, shop, timer, bonus, 2P |
| `sprites/` | 18.834 (645 thư mục) | `.png` (nền trong suốt) | Các MovieClip đã xuất sẵn từng frame PNG |
| `images/` | 279 | 246 `.png`, 33 `.jpg` | Ảnh nền `800×600`, biển hiệu, UI bitmap, chi tiết đồ họa |
| `buttons/` | 544 | `.png` | Trạng thái Up/Over/Down của các nút Menu, Shop, HUD |
| `frames/` | 512 | `.png` (`800×600`) | Ảnh chụp toàn màn hình từng frame của timeline chính |
| `sounds/` | 77 | 76 `.mp3`, 1 `.wav` (`-1.wav` rỗng 44 B) | 4 bản nhạc nền BGM (`132–135.mp3`) + 72 hiệu ứng SFX |
| `fonts/` | 14 | `.ttf` | Font bitmap/vector tiếng Anh (`CAC Norm Heavy`, `Futura Md BT`, `CCZoinks`...) |
| `texts/` | 307 | `.txt` | Chuỗi hướng dẫn, nhãn HUD, hội thoại, popup |
| `shapes/` | 1.885 | `.svg` | Vector gốc của SWF (không cần render lại vì `sprites/` đã có PNG) |
| `morphshapes/` | 8 | `.svg` | Hình biến dạng vector |
| `symbolClass/` | 1 | `.csv` | Bảng ánh xạ Symbol Class của Flash UI Components |
| `movies/` | 0 | — | Rỗng (các đoạn phim intro `animation1..4.swf` là file ngoài) |

### 3 Chế độ chơi gốc (`_root.gameMode`)

| `gameMode` | Tên chế độ | Cấu trúc |
| :---: | :--- | :--- |
| `1` | **Story Mode** | 3 Thế giới (`world = 1..3`): **Australia** (`1–18`), **Hawaii** (`0` màn cứu thuyền + `1–18`), **Las Vegas** (`1–18`). Lưu tiến độ mở khóa vào `unLockArea`. |
| `2` | **Gold Rush Mode** | Chơi nối tiếp tính điểm cao, lặp vòng khi hết World 3 Level 18 với độ khó `goalChangerMax` và `goalOverPersent` tăng thêm. |
| `3` | **2 Players Mode** | Đối kháng chung máy 6 màn (Lv 1–2 ở W1, Lv 3–4 ở W2, Lv 5–6 ở W3), thời lượng `60s`/màn, so điểm `score` vs `score2` (hòa thì Player 1 `+1`). |

---

## HẠNG MỤC 1. NHỊP KHUNG HÌNH & THỜI GIAN (FPS & TIMING)

### 1.1. Tốc độ khung hình chuẩn (`FPS_gốc`)
* **FPS gốc xác chứng:** **`18 FPS`** (`~55.56 ms/frame`).
* **Bằng chứng mã nguồn:** Trong `DefineSprite_1826` (`timerMov` cho 1P, 22 frame) và `DefineSprite_3314` (`timerMov` cho 2P, 22 frame), đồng hồ chạy vòng lặp từ `frame_2` đến `frame_19` (tổng cộng **18 frame**) rồi mới thực hiện `time -= 1` tại `frame_19/DoAction.as`.
* **Công thức quy đổi vận tốc sang Delta Time (`dt`, đơn vị giây):**
  $$\text{Speed}_{\text{per sec}} = \text{Speed}_{\text{per frame}} \times 18$$

### 1.2. Thời lượng màn chơi & Cơ chế kết thúc giờ
* **Thời lượng chuẩn mỗi màn:** **60 giây** (`_root.timerMov.time = 60 + extraTime` trong `startLevel()` và `startLevelTwoPlayer()`).
  * *(Lưu ý kỹ thuật: hàm `startGameTwoPlayer()` có dòng khởi tạo `time = 30`, nhưng ngay khi vào màn `startLevelTwoPlayer()` luôn gán lại `_root.timerMov.time = 60`.)*
* **Cộng thêm giờ từ Shop (Pocket Watch):** `extraTime = 15` → tổng thời lượng **`75 giây`**.
* **Cảnh báo đếm ngược cuối giờ (`DefineSprite_1826/frame_19` & `DefineSprite_3314/frame_19`):**
  * Khi `time <= 5`, đồng hồ nhảy tới nhãn `"beep"` (`frame_20..22`), phát âm thanh `1825.mp3` mỗi giây và quay lại `frame_2`.
  * *(Ghi chú thiết kế libGDX: Bản Vegas gốc kêu beep từ **5 giây cuối**; khi đưa vào `hook_config.json` nên cấu hình tham số `countdownWarningSeconds` để dễ chỉnh giữa `5` chuẩn Vegas hoặc `10` theo thói quen bản Classic.)*
* **Gia hạn khi hết giờ (`Overtime` lúc `time == 0`):**
  * Ở **1 Player (`DefineSprite_1826/frame_19`)**: Khi `time <= 0`, gán `time = 0`. Nếu `busy == 1` (tức người chơi đang trong animation kéo/cộng tiền thưởng `bonus` ở `DefineSprite_3075`), game **tiếp tục chờ** ở `gotoAndPlay(2)` cho đến khi `busy == 0` rồi mới gọi `_root.endLevel()`.
  * Ở **2 Players (`DefineSprite_3314/frame_19`)**: Khi `time <= 0`, gọi thẳng `_root.endLevelTwoPlayer()`.

---

## HẠNG MỤC 2. CƠ CHẾ NGOÀM CÀO & XE ĐẨY (HOOK PHYSICS & GAME FEEL)

### 2.1. Hệ tọa độ gốc (`800 × 600`) và Quy đổi sang `FitViewport(480, 270)`

| Thành phần màn hình | Hệ gốc `800 × 600` (Y hướng xuống) | Hệ `480 × 270` (Y hướng lên, `k = 0.45`) | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Thanh HUD & Bầu trời** | `y ∈ [0, 83]` | `y ∈ [232.65, 270]` | Logo trái trên; `TARGET`, `TOTAL`, `TIMER` phải trên |
| **Vùng Đường ray & Xe mỏ** | `y ∈ (83, 195]`, trục tâm ngoàm `y ≈ 180` | `y ∈ [182.25, 232.65)`, tâm ngoàm `y ≈ 189` (hoặc `205` nếu thu gọn HUD đáy) | Click chuột vào vùng này điều khiển xe chạy ngang |
| **Vùng Lòng đất (Thả ngoàm)** | `y ∈ (195, 550)` (cao `355 px`) | `y ∈ [22.5, 182.25)` (cao `159.75 u`) | Click chuột vào vùng này (hoặc phím `Down`/`S`) để thả ngoàm |
| **Thanh công cụ đáy** | `y ∈ [550, 600]` (cao `50 px`) | `y ∈ [0, 22.5]` | Nút `MAIN MENU`, Window/Full, Sound, `PAUSE`, `HELP` |

* **Hệ số quy đổi khuyến nghị cho `480 × 270` (16:9):**
  * Tỉ lệ ngang `sx = 480 / 800 = 0.60`; tỉ lệ dọc `sy = 270 / 600 = 0.45`.
  * **Chốt dùng hệ số đồng nhất theo trục dọc $k = 0.45$** cho **tầm vươn dây, tốc độ phóng/thu và bán kính vật thể** để chiều sâu mỏ, góc quét và độ khó khớp 100% cảm giác bản Vegas (phần bề ngang 16:9 rộng hơn 4:3 hai bên giúp bố cục thoáng, không bóp méo hình tròn va chạm).
  * Riêng vị trí chạy xe ngang `cartX` có thể ánh xạ theo chiều ngang $x_{\text{world}} = x_{\text{orig}} \times 0.60$ (hoặc giữ biên cách mép $12\text{–}468\text{ u}$ tương ứng `20..780 px`).

### 2.2. Di chuyển Xe mỏ (`cartMov` — đặc sản của bản Vegas)
* **Nguồn code:** `DefineSprite_2863` (1P), `DefineSprite_3341` & `DefineSprite_3335` (2P), `DefineSprite_2816` (`cartMoveBut`), `DefineSprite_1816` (9 mẫu biên đường ray).
* **Phạm vi di chuyển mặc định:** `cartMin = 20`, `cartMax = 780` (trên màn `800 px`). Một số màn dùng đường ray hẹp hơn trong `DefineSprite_1816`: `[258, 618]`, `[382, 780]`, `[20, 487]`, `[205, 780]`, `[315, 506]`, `[285, 780]`, `[405, 780]`, hoặc `[400, 780]` (`frame_385`).
* **Điều khiển:** Giữ phím mũi tên Trái/Phải (`Key 37 / 39`) hoặc click chuột vào vùng đường ray (`83 < _ymouse <= 195`) để xe chạy tới điểm click `mouseSpot` (click lại gần xe để dừng).
* **Tốc độ di chuyển xe:**

| Trạng thái xe | Tốc độ gốc (`px/frame`) | Tốc độ theo giây (`18 FPS`) | Quy đổi `480×270` (`k=0.45` / `sx=0.60`) |
| :--- | :--- | :--- | :--- |
| **Cơ bản (`cartSpeed = 3`)** | `3.0 px/f` | **`54.0 px/s`** | `24.3 u/s` (`k=0.45`) / `32.4 u/s` (`sx=0.60`) |
| **Sau khi mua Oil Can / Monkey Grease (`× 2.5`)** | `7.5 px/f` | **`135.0 px/s`** | `60.75 u/s` (`k=0.45`) / `81.0 u/s` (`sx=0.60`) |
| **Thuyền tự trôi (`cart == 2`, Hawaii Lv0)** | `1.2 px/f` (`cartSpeed × 0.4`) | **`21.6 px/s`** | `9.72 u/s` (`k=0.45`) — không điều khiển được |

* **Ràng buộc 2 Players:** Hai xe giữ khoảng cách tối thiểu **`60 px`** (`cartMov._x < cartMov2._x - 60`, tương đương `36 u` theo `sx=0.60`).

### 2.3. Góc lắc ngoàm (`SWINGING` — `DefineSprite_2864`)
Đo trực tiếp từ 103 frame ảnh của `DefineSprite_2864` quanh tâm xoay `(745, 266)` (chu kỳ lặp `swing` = `100 frame`, từ `frame_3` tới `frame_102`, với pha khởi đầu ngẫu nhiên `random(92) + 2` tại `frame_1`):
* **Biên độ góc cực đại:** **`-80°` đến `+80°`** (`0°` = thẳng đứng hướng xuống dưới).
  * Đo thực tế: `-79.78°` tại `frame_28`, `0°` quanh `frame_53`, `+80.00°` tại `frame_79`.
* **Quy luật xoay phi tuyến (Sinusoidal / Ease-In-Out — KHÔNG xoay đều tuyến tính):**
  * Tốc độ góc lớn nhất khi đi qua tâm thẳng đứng (`0°`): **`~6.0°/frame`** ≈ **`108°/s`**.
  * Giảm tốc mượt dần về **`< 0.2°/frame`** ở hai biên `±80°` (tạo cảm giác "nghỉ nhẹ" ở hai đầu giúp căn góc dễ hơn).
* **Chu kỳ toàn phần:** `100 frame / 18 FPS` = **`5.556 giây`** (nửa chu kỳ từ `-80°` sang `+80°` = **`2.778 giây`**).
* **Mô hình toán học chuẩn cho Java (`dt`):**
  $$\theta(t) = 80^\circ \cdot \sin(\omega \cdot t + \phi_0), \quad \omega = \frac{2\pi}{5.556} \approx 1.131\text{ rad/s}$$
  $$\dot{\theta}(0^\circ) = 80^\circ \cdot \omega \approx 90.5^\circ/\text{s} \text{ (thuần sin) hoặc dùng } \sin\text{ gia tốc nhẹ ở giữa khớp } 108^\circ/\text{s}.$$

### 2.4. Tốc độ phóng dây (`EXTENDING` — `DefineSprite_2863`, `frames 2..47`)
* Đo trực tiếp tọa độ đầu ngoàm trong `DefineSprite_2863`:
  * Từ `frame_2` (`y_bot = 189 px`) đến `frame_47` (`y_bot = 789 px`): tổng tầm vươn tối đa **`600 px`** trong **`45.6` frame** (~`2.53 giây`), tăng tuyến tính đều **`+13.16 px/frame`**.
* **Quy đổi sang giây (`18 FPS`):** **`236.8 px/s`**.
* **Quy đổi sang `480 × 270` (`k = 0.45`):**
  * Tốc độ phóng dây: **`106.56 u/s`** (`~107 u/s`).
  * Chiều dài dây tối đa: **`270 u`** (hoặc tự động thu dây khi đầu ngoàm vượt khỏi biên màn hình `x < 0 || x > 480 || y < 0`).

### 2.5. Tốc độ thu dây & Công thức kéo theo độ nặng (`RETRACTING`)
* **Nguồn code:** `DefineSprite_2863/frame_49/PlaceObject2_2862_46/CLIPACTIONRECORD onClipEvent(enterFrame).as` (và `CLIPACTIONRECORD onClipEvent(load).as`).
* Toàn bộ hành trình thu dây trên timeline dài `321` frame (`frame_49` đến `frame_370`) tương ứng quãng đường `600 px` → **`1.869 px` cho mỗi 1 bước frame** (`33.65 px/s` tại 18 FPS; **`15.14 u/s`** trên `480×270` với `k = 0.45`).
* **Lực kéo mặc định:** `minerStrength = 10` (cộng thêm `+2` mỗi lần uống **Strength Drink** ở Shop hoặc kéo được **Đĩa sườn `"rib"`** trong màn).

#### Toàn bộ nhánh công thức kéo dây (khớp 100% code gốc):
1. **Thu dây rỗng (`weight == -1`) hoặc sau khi ném Thuốc nổ (`stickBonus == 1`):**
   * `stickBonus` bật `= 1`, nhảy `+16 frame/tick` (`_currentframe + 15` + playback 1 frame) → **`29.9 px/frame`** = **`538.3 px/s`** (**`~242.2 u/s`** ở `k = 0.45`).
2. **Kéo Siêu Sức Mạnh (`strength == 1`, từ hộp lực `"power"` hoặc Túi Bí Ẩn):**
   * Đầu ngoàm chuyển sang **Super Claw** (`claw = 4`).
   * Mọi vật thông thường (`objectN != 33`) đều nhảy `+16 frame/tick` (**`538.3 px/s`** / **`242.2 u/s`**).
   * Ngoại lệ duy nhất: **Thuyền chở vàng (`objectN == 33`, `weight = 100`)** bị ghìm về `weight = 8` → kéo ở tốc độ `10 - 8 = 2 step/frame` (`67.3 px/s`).
3. **Quá tải & Đứt dây (`minerStrength - weight <= 0`):**
   * Nếu KHÔNG có `cableBonus == 1`, KHÔNG có `strength == 1`, và KHÔNG có `stickBonus == 1`: gọi `miner.gotoAndStop("brake")` → **ĐỨT DÂY**, mất vật đang kéo và ngoàm thu về rỗng!
   * Nếu có **Cáp Titan (`cableBonus == 1`)**: tự động gán lại `weight = 9` → không bao giờ đứt dây; với `minerStrength = 10` sẽ kéo lên ở tốc độ tối thiểu `1 step/frame` (**`33.65 px/s`** / **`15.14 u/s`**).
4. **Kéo vật thông thường (`minerStrength - weight > 0`):**
   $$\text{stepPerFrame} = \text{minerStrength} - \text{weight}$$
   $$v_{\text{retract\_px}} = 33.65 \times (\text{minerStrength} - \text{weight}) \quad [\text{px/s}]$$
   $$v_{\text{retract\_world}} = 15.14 \times (\text{minerStrength} - \text{weight}) \quad [\text{u/s tại } k=0.45]$$

#### Bảng tốc độ kéo mẫu theo độ nặng (`weight`)

| `weight` | Vật thể tiêu biểu | `minerStrength = 10` (mặc định) | `minerStrength = 12` (+Strength Drink) | Có `cableBonus = 1` (`mS = 10`) | `480×270` (`mS=10`, `k=0.45`) |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **`-1` (rỗng)** | Không kẹp gì / Ném TNT | `538.3 px/s` (`stickBonus`) | `538.3 px/s` | `538.3 px/s` | **`242.2 u/s`** |
| **`0`** | Gold Pan (`7`), Dây điện tắt | `336.5 px/s` (10 step) | `403.8 px/s` (12 step) | `336.5 px/s` | **`151.4 u/s`** |
| **`1`** | Ngọc trai (`42`), Túi nhẹ (Clover) | `302.9 px/s` (9 step) | `370.2 px/s` (11 step) | `302.9 px/s` | **`136.3 u/s`** |
| **`2`** | Kim cương (`13`), TNT (`12`), Ma (`45/46`) | `269.2 px/s` (8 step) | `336.5 px/s` (10 step) | `269.2 px/s` | **`121.1 u/s`** |
| **`3`** | Vàng nhỏ (`2`), Gem/Chip, Chuột Bilby | `235.6 px/s` (7 step) | `302.9 px/s` (9 step) | `235.6 px/s` | **`106.0 u/s`** |
| **`4`** | Đá nhỏ (`10`), Nhện (`50`), Đĩa sườn (`63`) | `201.9 px/s` (6 step) | `269.2 px/s` (8 step) | `201.9 px/s` | **`90.8 u/s`** |
| **`5`** | Bilby Kim cương (`22`), Cá (`19`), Xương dài | `168.3 px/s` (5 step) | `235.6 px/s` (7 step) | `168.3 px/s` | **`75.7 u/s`** |
| **`7`** | Vàng vừa-nhỏ (`3`), Idol (`52–55`), Crook (`77`) | `101.0 px/s` (3 step) | `168.3 px/s` (5 step) | `101.0 px/s` | **`45.4 u/s`** |
| **`8`** | Vàng vừa (`4`), Đá vừa (`9`) | `67.3 px/s` (2 step) | `134.6 px/s` (4 step) | `67.3 px/s` | **`30.3 u/s`** |
| **`9`** | Vàng to (`5`), Đá lớn (`8`), Bilby xe (`29`) | `33.7 px/s` (1 step) | `101.0 px/s` (3 step) | `33.7 px/s` | **`15.1 u/s`** |
| **`10`** | Két sắt (`79`), Máy nước (`64`), Rác nặng | **Đứt dây (`brake`)** | `67.3 px/s` (2 step) | `33.7 px/s` (`w→9`) | **Đứt** / `15.1 u/s` |
| **`11`** | Vàng khổng lồ (`6`) | **Đứt dây (`brake`)** | `33.7 px/s` (1 step) | `33.7 px/s` (`w→9`) | **Đứt** / `15.1 u/s` |
| **`12`** | Tảng đá lớn (`31`), Tường sập (`60–62`) | **Đứt dây (`brake`)** | **Đứt dây (`brake`)** | `33.7 px/s` (`w→9`) | **Đứt** / `15.1 u/s` |
| **`100`** | Thuyền vàng (`33`, Hawaii Lv0) | **Đứt dây (`brake`)** | **Đứt dây (`brake`)** | `33.7 px/s` (`w→9`) | **Đứt** / `15.1 u/s` |

### 2.6. Các loại đầu ngoàm (`_root.claw`) & Cơ chế ném Thuốc nổ
* **`claw = 1`:** Ngoàm chuẩn mặc định.
* **`claw = 2` (Spike Claw):** Khi chạm Đá/Xương/Tường (`objectN ∈ {8..10, 14..18, 31, 35, 36, 60..62}`), lập tức phá hủy vật (`gotoAndStop(3)`) mà **không cần kéo lên**; khi chạm Quỷ Tasmania / Bilby (`21, 27, 28`), đẩy lùi vật đi `-15 px` theo trục X.
* **`claw = 3` (Laser Claw):** Gắn tia ngắm laser chỉ hướng thả ngoàm.
* **`claw = 4` (Super Claw):** Đi kèm `strength = 1` khi nhặt `"power"`.
* **Ném Thuốc nổ khi đang kéo (`DefineSprite_2825` & `DefineButton2_2828` / `2694`):**
  * Phím `Up` (`W`/`Up`) hoặc bấm nút Thuốc nổ khi đang thu dây (`busy == 1` và `stick >= 1`): trừ `stick -= 1`, phóng thanh thuốc nổ bám theo ngoàm theo công thức nội suy `pos += (clawPos - pos) * 0.5` mỗi frame (`~0.5` quãng đường mỗi tick 18 FPS).
  * Khi thuốc nổ chạm vật (`hitTest`):
    * Nếu đang kẹp **Két sắt nguyên (`objectN == 79` / `claw._currentframe == 79`)**: nổ bung vỏ két chuyển sang **`objectN = 80`**, giữ nguyên giá **`$800`** và bật **`stickBonus = 1`** (kéo lên với tốc độ tối đa `538 px/s`).
    * Mọi vật khác: hủy vật (`price = 0`, `claw.gotoAndStop("off")`) và bật `stickBonus = 1` để thu dây không về ngay lập tức.

---

## HẠNG MỤC 3. BẢNG VẬT THỂ & CHƯỚNG NGẠI VẬT ĐẦY ĐỦ (`objectN 1..88`)

> Kích thước hiển thị đo từ các frame `1..88` của `DefineSprite_2685` trên hệ `800×600`. Bán kính va chạm gợi ý `r` trên hệ `480×270` tính theo `r ≈ 0.45 × ((w + h) / 4)`.

### 3.1. Vàng, Đá, Kim cương, Xương & Bộ sưu tập

| `objectN` | Sprite ID | Tên vật thể | Giá gốc (`$`) | Giá khi có Bonus (`$`) | `weight` | Kích thước gốc (`px`) | `r` gợi ý (`480×270`) | Hành vi đặc biệt |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| `1` | `2657, 2674, 2680` | Rỗng / Công tắc / Đã mất | `0` | `0` | `0 / 1 / -1` | — | — | Không kéo vật (`weight=-1` bật thu dây nhanh) |
| `2` | `DefineSprite_1844` | Vàng nhỏ (Small Gold) | `50` | — | `3` | `16 × 15` | `3.5` | Vật phẩm cơ bản |
| `3` | `DefineSprite_1846` | Vàng vừa-nhỏ | `100` | — | `7` | `30 × 28` | `6.5` | Vật phẩm cơ bản |
| `4` | `DefineSprite_1849` | Vàng vừa (Medium Gold) | `250` | — | `8` | `53 × 42` | `10.7` | Vật phẩm cơ bản |
| `5` | `DefineSprite_1851` | Vàng to (Large Gold) | `500` | — | `9` | `86 × 58` | `16.2` | Nặng, kéo chậm |
| `6` | `DefineSprite_1861` | Vàng khổng lồ (Mega Gold) | `1000` | — | `11` | `92 × 80` | `19.4` | **Đứt dây** nếu `minerStrength=10` & không có `cableBonus`/`strength` |
| `7` | `DefineSprite_1865` | Khay đãi vàng (Gold Pan) | `25` | — | `0` | `32 × 17` | `5.5` | Siêu nhẹ |
| `8` | `DefineSprite_1873` | Đá lớn (Large Rock) | `20` | `60` (`rockBonus`) | `9` | `86 × 45` | `14.7` | Spike Claw đập vỡ |
| `9` | `DefineSprite_1878` | Đá vừa (Medium Rock) | `11` | `33` (`rockBonus`) | `8` | `39 × 40` | `8.9` | Spike Claw đập vỡ |
| `10` | `DefineSprite_1881` | Đá nhỏ (Small Rock) | `5` | `15` (`rockBonus`) | `4` | `22 × 20` | `4.7` | Spike Claw đập vỡ |
| `31` | `DefineSprite_2059` | Tảng đá siêu nặng | `120` | `160` (`rockBonus`) | `12` | `111 × 78` | `21.3` | **Đứt dây** kể cả khi có +1 Strength Drink (`mS=12`); Spike Claw đập vỡ |
| `60, 61, 62` | `2533, 2537, 2538` | Tường sập Vegas (Broken Walls) | `140` | `240` (`rockBonus`) | `12` | `77×110` / `126×76` | `21.0` | Rất nặng (`w=12`); Spike Claw đập vỡ |
| `13` | `DefineSprite_1943` | Kim cương (Diamond) | `600` | `900` (`diamondBonus`) | `2` | `19 × 18` | `4.2` | Giá trị cao, rất nhẹ |
| `14` | `DefineSprite_1951` | Sọ nhỏ (Small Skull) | `20` | `400` (`boneBonus`) | `2` | `22 × 18` | `4.5` | Spooky Doll tăng 20×; Spike Claw đập vỡ |
| `15` | `DefineSprite_1955` | Xương nhỏ (Small Bone) | `5` | `100` (`boneBonus`) | `2` | `28 × 23` | `5.7` | Spooky Doll tăng 20×; Spike Claw đập vỡ |
| `16, 17` | `1958, 1959` | Xương dài dọc / ngang | `10` | `200` (`boneBonus`) | `5` | `19×55` / `55×18` | `8.3` | Spooky Doll tăng 20×; Spike Claw đập vỡ |
| `18` | `DefineSprite_1963` | Bộ xương lớn | `20` | `400` (`boneBonus`) | `7` | `35 × 30` | `7.3` | Spooky Doll tăng 20×; Spike Claw đập vỡ |
| `23..26` | `1989..1992` | 4 Đá quý (Gems — W1) | `100` | `+$2000` (đủ 4 món) | `3` | `~16 × 20` | `4.1` | Thu thập đủ 4 món trong 1 màn kích hoạt thưởng `"gem"` |
| `52..55` | `2448, 2452, 2456, 2460` | 4 Tượng cổ (Idols — W2) | `100` | `+$2000` (đủ 4 món) | `7` | `~24 × 32` | `6.3` | Thu thập đủ 4 món trong 1 màn kích hoạt thưởng `"gem"` |
| `68..71` | `2596, 2599, 2602, 2605` | 4 Phỉnh bài (Chips — W3) | `100` | `+$2000` (đủ 4 món) | `3` | `20 × 15` | `4.0` | Thu thập đủ 4 món trong 1 màn kích hoạt thưởng `"gem"` |

### 3.2. Túi Bí Ẩn, Thuốc nổ, Bẫy & Vật phẩm đặc biệt

| `objectN` | Sprite ID | Tên vật thể | Giá gốc (`$`) | `weight` | Kích thước (`px`) | `r` (`480×270`) | Hành vi đặc biệt |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `11, 51, 87` | `1884, 2444, 2684` | Túi Bí Ẩn / Rương (W1/W2/W3) | Ngẫu nhiên / Buff | `6..9` (Clover: `1..8`) | `~42 × 40` | `9.2` | Xem **Hạng mục 4** |
| `47` | `2360` / `2651` | Túi tiền ngẫu nhiên phụ | `1..500` / `1..300` | `7` / `6..9` (Clover `1..8`) | `63 × 67` | `14.6` | Clover (`2651`) khóa giá `$100..$299` |
| `12` | `1940` + `1938` | **Thùng TNT / Thủy lôi** | `1` | `2` | `54 × 56` | `12.4` | Chạm vào nổ ngay! Vùng nổ `189×178 px` (**bán kính nổ `~90 px`**, tức **`~40.5 u`** ở `480×270`), phá hủy mọi vật trong tầm & **nổ dây chuyền** TNT khác |
| `33` | `DefineSprite_2066` | Thuyền vàng (Hawaii Lv0) | `"boat"` (`boatValue`) | `100` | `374 × 272` | `72.7` | Chỉ kéo được khi có `strength=1` (bị ép `w=8`), thưởng chia 5 đợt `int((boatValue+2)/5)` |
| `34` | `DefineSprite_2072` | Bình Siêu Sức Mạnh rơi | `"power"` | `7` | `81 × 56` | `15.4` | Bật `strength = 1`, `claw = 4` (Super Claw) hết màn |
| `35, 36` | `2078, 2082` | Rác / Cản nặng | `1..25` / `1..70` | `9` / `10` | `60×35` / `58×59` | `11` / `13` | Spike Claw đập vỡ |
| `48` | `DefineSprite_2363` | Thuốc nổ rơi | `"dynamite"` | `2` | `40 × 34` | `8.3` | `+1 stick` |
| `57` | `DefineSprite_2463` | Công tắc TNT (Switch) | `0` (`objectN=1`) | `-1` | `54 × 50` | `11.7` | Kích hoạt nổ toàn bộ TNT trên màn |
| `58` | `2503` + `2480` | **Quái vật Dung nham (Lava)** | `0` (nguội) / Nổ (giận) | `-1` | `49 × 31` | `9.0` | Luân phiên trạng thái nguội ↔ giận (`explode=1`). Kẹp lúc giận sẽ nổ lan như TNT (`189×178 px`) |
| `63` | `DefineSprite_2542` | Đĩa sườn (Dinner Plate) | `"rib"` | `4` | `46 × 25` | `8.0` | Tăng `minerStrength += 2` trong màn |
| `64` | `DefineSprite_2546` | Máy nước ngọt (Soda Machine) | `100` | `10` | `54 × 91` | `16.3` | Rất nặng (`w=10`), dễ làm đứt dây |
| `66` | `DefineSprite_2586` | Ngoàm gai rơi (Spike Claw) | `"spike"` | `5` | `52 × 38` | `10.1` | Bật `claw = 2` hết màn |
| `72..74` | `2611, 2613, 2615` | **3 Máy Slot Vegas** | `0` (`objectN=1`) | `-1` | `49 × 49` | `11.0` | Xem **Hạng mục 7** |
| `75, 76, 78` | `2620, 2624, 2645` | Ghế / TV vỡ (Chairs / TV) | `20` / `20` / `50` | `4` / `5` / `9` | `38×57` / `41×37` / `52×62` | `9–13` | Chướng ngại vật ít tiền ở Vegas |
| `79 / 80` | `DefineSprite_2648` | **Két sắt Vegas (Safe)** | `800` | `10` (`79`) / `-1` (`80`) | `52 × 52` | `11.7` | Bình thường nặng `w=10` (đứt dây nếu thiếu Strength/Cable). **Ném TNT trúng khi đang kéo** sẽ mở tung sang `80` giữ `$800` và kéo siêu tốc (`stickBonus=1`)! |
| `81` | `2657` + `2656` | **Dây điện hở (Live Wire)** | `0` / `"zap"` | `0` / `3` | `56 × 43` | `11.1` | Nhấp nháy: kẹp lúc tắt không mất gì; kẹp lúc phóng điện làm bác thợ mỏ bị giật (`"zap"`) mất nhịp |
| `82, 88` | `2661, 2664` | Thuốc nổ rơi (1 hoặc 2 thanh) | `"dynamite"` / `"dynamite2"` | `3` / `2` | — | `8.0` | `+1` hoặc `+2 stick` |

### 3.3. Sinh vật di chuyển & Nhân vật đặc biệt (tốc độ đã quy đổi sang `18 FPS`)

| `objectN` | Sprite ID | Tên sinh vật / nhân vật | Giá (`$`) | `weight` | Tốc độ gốc (`px/f`) | Quy đổi (`px/s` @ 18 FPS) | `480×270` (`k=0.45`) | Hành vi đặc biệt |
| :---: | :--- | :--- | :---: | :---: | :--- | :--- | :--- | :--- |
| `19` | `1968/1967` | Cá nhỏ (Fish) | `20` | `5` | Ngang `0.05..1.25`, Dọc `0.05..2.5` | `0.9..22.5` / `0.9..45.0` | `0.4..10.1` / `0.4..20.3 u/s` | Bơi tuần tra ngẫu nhiên quanh vị trí gốc (`±34` X, `±100` Y) |
| `20` | `1973/1972`, `2084/2083` | Cá vàng giá cao | `150` | `2` | Ngang `0.15..3.75`, Dọc `0.15..7.5` | `2.7..67.5` / `2.7..135.0` | `1.2..30.4` / `1.2..60.8 u/s` | Bơi nhanh gấp 3× cá thường |
| `21` | `1986/1985` | Chuột túi Bilby | `10` | `3` | Nhảy `8→6→5→4→3` (tb `~5.2`) | `~93.6 px/s` | `~42.1 u/s` | Nhảy từng đợt rồi dừng `5..14` frame; Spike Claw đẩy lùi `-15 px` |
| `22` | `1988/1987` | **Bilby ôm Kim cương** | `610` (`910` Polish) | `5` | `4.1..6.0 px/f` | **`73.8..108.0 px/s`** | **`33.2..48.6 u/s`** | Tương đương Chuột kim cương bản Classic! Tỉ lệ 20% đổi hướng mỗi 33 frame |
| `27, 28` | `2025/2024`, `2028/2027` | Quỷ Tasmania | `30` | `7` | Đi `2.0`, Lao `4.0` | `36.0` / `72.0 px/s` | `16.2` / `32.4 u/s` | Gặp vật cản phía trước sẽ lao phá hủy (`gotoAndStop(3)`); Spike Claw đẩy lùi `-15 px` |
| `29, 30` | `2055/2054`, `2057/2056` | Bilby đẩy xe cút kít | `35` (+vật chở) | `9` (+vật chở) | `2.5..4.9 px/f` | `45.0..88.2 px/s` | `20.3..39.7 u/s` | Có thể nhặt trộm vật trên màn vào xe; kẹp vùng xe (`box3`) lấy thẳng món đồ đó, hoặc kẹp cả xe để nhả món lại |
| `39, 40` | `2089/2088`, `2094/2093` | Sứa / Sinh vật biển | `50` | `4` / `9` | `0.15..3.75` X, `0.15..7.5` Y | `2.7..67.5` / `2.7..135` | `1.2..30.4 u/s` | Bơi dọc biên độ rộng ở Hawaii |
| `41 / 42` | `2103/2102` | Sò đóng / Ngọc trai | `50` (`41`) / `700` (`850` Pirate) (`42`) | `9` (`41`) / `1` (`42`) | Đứng yên | `0` | `0` | Đóng/mở theo chu kỳ timeline (`frame_21` lộ Ngọc trai `w=1`!) |
| `45 / 46` | `2261/2262/2263` | **Hồn ma Đỏ / Xanh** | `"ghost"` (`200` Jar) / `200→1600` | `2` | Bơi lượn `0.1..2.5` X, `0.1..5.0` Y | `1.8..45` / `1.8..90` | `0.8..20.3 u/s` | Ma đỏ hù mất thời gian (trừ khi có Spirit Jar); Ma xanh bắt nối chuỗi `T39→T42` thưởng `$200→$400→$800→$1600` |
| `50` | `2440/2439` | Nhện (Spider) | `10` | `4` | `0.75..3.6 px/f` | `13.5..64.8 px/s` | `6.1..29.2 u/s` | Chạy ngang qua lại |
| `65` | `2582/2564` | Kiến Vegas (Ant) | `"ant"` (`0` nếu có Ant Spray) | `3` | `4.0 px/f` | `72.0 px/s` | `32.4 u/s` | Cắn làm choáng; nếu đã mua Ant Spray sẽ tiêu diệt ngay khi chạm ngoàm |
| `43, 49, 67` | `2256/2196`, `2430/2429`, `2593/2592` | Hansel (`43,49`) & Julie (`67`) | `300`, `1000`, `"jewl2"` (`+1000`) | `5`, `7`, `5` | `5..50` (thuyền) | `90..900 px/s` | — | Nhân vật cốt truyện (Hansel đâm thuyền trừ `$10..$59`; Julie tặng `$1000`) |
| `77` | `2641/2640`, `2668/2667` | **Tên trộm Joey Joe Joe** | `500` (chỉ bắt được khi `crookBonus=1`) | `7` | Đi `3.0`, Chạy `12.0` | `54.0` / `216.0 px/s` | `24.3` / `97.2 u/s` | Đi bộ ngang mặt đất `y=185`, chạm xe thợ mỏ cướp **`$50..$99`** rồi phóng biến mất; mua Wanted Poster sẽ tóm cổ lấy `$500` |

---

## HẠNG MỤC 4. CƠ CHẾ TÚI BÍ ẨN (MYSTERY BAG)

Nguồn code: `DefineSprite_1884` (`objectN=11`, Australia), `DefineSprite_2444` (`objectN=51`, Hawaii), `DefineSprite_2684` (`objectN=87`, Vegas) và túi tiền phụ `DefineSprite_2651` (`objectN=47`).

### 4.1. Bảng xác suất Túi Bí Ẩn chuẩn (`11`, `51`, `87`)
Trong ActionScript, `random(N)` trả về số nguyên đều trong $[0, N-1]$, nên `random(8) + 1` cho kết quả đều $1..8$ (mỗi bậc `12.5%`).

| Điều kiện | Phân phối độ nặng (`weight`) | Kết quả (`w2 = random(8) + 1`) | Xác suất | Phần thưởng & Hiệu ứng |
| :--- | :--- | :--- | :---: | :--- |
| **`cloverBonus == 0`** (Không có Cỏ 4 lá / Vòng hoa / Chân thỏ) | `random(4) + 6` → đều trên `{6, 7, 8, 9}` *(TB = `7.5`)* | `w2 ∈ {1, 2, 3}` | **37.5%** | Tiền ngẫu nhiên `random(600) + 1` (**`$1 – $600`**, TB `$300.5`) |
| | | `w2 == 4` | **12.5%** | **`"power"`**: Bật `strength = 1` + `claw = 4` (Super Claw) hết màn |
| | | `w2 == 5` | **12.5%** | **`"dynamite"`**: Cộng `+1` thanh Thuốc nổ (`stick += 1`) |
| | | `w2 == 6` | **12.5%** | **`"lazer"`**: Bật `claw = 3` (Laser Claw) hết màn |
| | | `w2 == 7` | **12.5%** | **`"spike"`**: Bật `claw = 2` (Spike Claw) hết màn |
| | | `w2 == 8` | **12.5%** | Tiền cố định **`$800`** |
| **`cloverBonus == 1`** (Có Cỏ 4 lá / Lucky Lei / Rabbit's Foot) | `w = random(3)+1`: `1/3` ra `1..7`, `1/3` ra `1`, `1/3` ra `8` *(TB giảm còn `4.33`)* | `w2 ∈ {1, 2}` | **25.0%** | **`"power"`**: Siêu Sức Mạnh + Super Claw (`strength=1, claw=4`) |
| | | `w2 ∈ {3, 4}` | **25.0%** | **`"dynamite"`**: Cộng `+1` thanh Thuốc nổ (`stick += 1`) |
| | | `w2 ∈ {5, 6}` | **25.0%** | **`"spike"`**: Bật `claw = 2` (Spike Claw) |
| | | `w2 ∈ {7, 8}` | **25.0%** | Tiền cố định **`$700`** |

> **Nhận xét thiết kế:** Khi có **Lucky Clover (`cloverBonus == 1`)**, túi không chỉ loại bỏ hoàn toàn tiền ít (`$1..$600`) và tia laser để nhân đôi xác suất ra Siêu Sức Mạnh / Thuốc nổ / Ngoàm gai (`75%`) + tiền lớn `$700` (`25%`), mà còn **làm nhẹ trọng lượng trung bình của túi từ `7.5` xuống `4.33`** giúp kéo nhanh hơn hẳn!

### 4.2. Túi phụ (`DefineSprite_2651`, `objectN = 47`)
* **`cloverBonus == 0`:** `weight = random(4) + 6` (`6..9`), tiền `random(300) + 1` (**`$1 – $300`**).
* **`cloverBonus == 1`:** `weight` theo phân phối nhẹ (`TB 4.33`), tiền `random(200) + 100` (**`$100 – $299`**).

---

## HẠNG MỤC 5. HỆ THỐNG CỬA HÀNG GIỮA MÀN (VEGAS SHOP LOGIC)

* **Nguồn code:** `DefineSprite_1568` (W1 Australia), `DefineSprite_1636` (W2 Hawaii), `DefineSprite_1714` (W3 Las Vegas), các khung slot `DefineSprite_1514..1705` và nút `DefineButton2_1509..1703`.
* **Quy tắc chung:**
  * Shop dùng chung **Tiền Mỏ (`score`, `$`)** đang có để mua; chỉ hiện sau khi qua màn (trừ các màn chuyển chặng `level == 6, 12, 18` đi qua bản đồ `map`).
  * Mỗi ô hàng chỉ mua tối đa 1 lần mỗi lượt ghé shop.
  * **Toàn bộ buff đều bị `resetBonuses()` xóa sau khi kết thúc 1 màn**, NGOẠI TRỪ **Thuốc nổ (`stick`)** được tích trữ cộng dồn (`stick += 1`) qua các màn!

### 5.1. Bảng vật phẩm, tỉ lệ xuất hiện & công thức định giá theo World/Level

| World | Slot | Vật phẩm & Tỉ lệ xuất hiện | Công thức giá Easy/Medium (`dificulty < 3`) | Công thức giá Hard (`dificulty == 3`) | Tác dụng biến hệ thống |
| :---: | :---: | :--- | :--- | :--- | :--- |
| **W1** | 1 (`1514`) | `33.3%` Strength Drink<br>`33.3%` Oil Can<br>`33.3%` Trống | `random(300) + 50` (`$50–$349`)<br>`random(300) + 25` (`$25–$324`) | `random(600) + 1` (`$1–$600`)<br>`random(300) + 25` (`$25–$324`) | `minerStrength += 2`<br>`cartSpeed *= 2.5` |
| **W1** | 2 (`1519`) | `60%` Lucky Clover<br>`40%` Trống | `random(100) + 19` (`$19–$118`) | `random(100) + 19` (`$19–$118`) | `cloverBonus = 1` |
| **W1** | 3 (`1524`) | **`100%`** Dynamite Stick | `random(300) + 1 + level * 2` | `random(600) + 1` (`$1–$600`) | `stick += 1` *(tích lũy)* |
| **W1** | 4 (`1529`) | `60%` Pocket Watch<br>`40%` Trống | `random(500) + 150` (`$150–$649`) | `random(1000) + 150` (`$150–$1149`) | `extraTime = 15` (`+15s`) |
| **W1** | 5 (`1539`) | `30%` Gem Polish<br>`40%` Titanium Cable<br>`30%` Trống | `random(600) + 300` (`$300–$899`)<br>`random(800) + 10` (`$10–$809`) | `random(600) + 300`<br>`random(800) + 10` | `diamondBonus = 1`<br>`cableBonus = 1` |
| **W2** | 1 (`1593`) | `33.3%` Tropical Strength Drink<br>`33.3%` *(khi `level>=7`)* Monkey Grease<br>Còn lại Trống | `random(400) + 10` (`$10–$409`)<br>`random(300) + 25` (`$25–$324`) | `random(800) + 1` (`$1–$800`)<br>`random(300) + 25` (`$25–$324`) | `minerStrength += 2`<br>`cartSpeed *= 2.5` |
| **W2** | 2 (`1606`) | `40%` Lucky Lei<br>`60%` Pirate Hat (`level <= 7`) / Spirit Jar (`level >= 8`) | `random(level*50) + 1 + level*2`<br>Hat: `random(100)+1`; Jar: `random(300)+1` | Như trên | `cloverBonus = 1`<br>`pirateBonus = 1` / `spiritBonus = 1` |
| **W2** | 3 (`1611`) | **`100%`** Coconut Bomb | `random(200) + 1 + level * 2` | `random(600) + 2` (`$2–$601`) | `stick += 1` *(tích lũy)* |
| **W2** | 4 (`1617`) | `30%` Pocket Watch<br>`30%` Spooky Doll<br>`40%` Trống | `random(500) + 150` (`$150–$649`)<br>`random(500) + 100` (`$100–$599`) | `random(1000) + 150` (`$150–$1149`)<br>`random(1000) + 1` (`$1–$1000`) | `extraTime = 15`<br>`boneBonus = 1` |
| **W2** | 5 (`1621`) | `30%` Gem Polish<br>`40%` Titanium Cable<br>`30%` Trống | `random(level*100) + 1 + 200`<br>`random(level*20) + 1 + 200` | Như trên | `diamondBonus = 1`<br>`cableBonus = 1` |
| **W3** | 1 (`1675`) | `50%` Oil Can<br>`50%` Trống *(Ghi chú bug gốc)* | `random(300) + 25` (`$25–$324`) | `random(300) + 25` (`$25–$324`) | `cartSpeed *= 2.5` *(Ghi chú: `frame_2` có Strength Drink giá `random(300)+1+level*2` / Hard `random(600)+1` nhưng `frame_1` quên nhánh nhảy tới `frame_2`)* |
| **W3** | 2 (`1689`) | `40%` Lucky Rabbit's Foot<br>`60%` Wanted Poster (`level >= 7`), else Trống | `random(100) + 19` (`$19–$118`)<br>`random(500) + 1` (`$1–$500`) | Như trên | `cloverBonus = 1`<br>`crookBonus = 1` |
| **W3** | 3 (`1692`) | `level >= 9`: `100%` Dynamite Stick; `< 9`: Trống | `dificulty == 1`: `random(600) + 300`<br>`dificulty >= 2`: `random(1000) + 1` | `random(1000) + 1` (`$1–$1000`) | `stick += 1` *(tích lũy)* |
| **W3** | 4 (`1695`) | **`100%`** Pocket Watch | `random(600) + 150` (`$150–$749`) | `random(1000) + 150` (`$150–$1149`) | `extraTime = 15` |
| **W3** | 5 (`1705`) | `60%` Ant Spray<br>`40%` Trống | `random(500) + 1` (`$1–$500`) | `random(1000) + 1` (`$1–$1000`) | `antBonus = 1` |

* **Ghi chú mở rộng cho `shop.json`:** Trong code vật thể có biến `rockBonus` (tăng giá Đá 3× và Tường +$100) nhưng Shop của bản Vegas đã thay bằng Titanium Cable / Ant Spray / Wanted Poster. Dự án `Gold_Miner_Java_LibGDX` có thể giữ cấu trúc data-driven ở `shop.json` để hỗ trợ cả các món chuẩn Vegas lẫn **Rock Collectors Book (`rockBonus`)** nếu muốn bổ sung.

---

## HẠNG MỤC 6. ĐƯỜNG CONG ĐỘ KHÓ & BỐ CỤC MÀN CHƠI (LEVEL CURVE & LAYOUTS)

### 6.1. Công thức tăng `Goal` động (`setGoal()` trong `scripts/frame_1/DoAction_2.as`)
Tại đầu mỗi màn, mục tiêu `goal` được tính cộng dồn dựa trên **tiền dư màn trước** và **tổng giá trị tài nguyên của màn hiện tại (`levelValue`)**:

```text
if (lastScore >= goal):
    goal = int(goal + (lastScore - goal) * goalOverPersent)

if (goalChanger < goalChangerMax):
    goalChanger = goalChanger + goalChangerAddOn

goal = int(goal + levelValue * goalChanger)
```

*(Với trường hợp Story Mode lưu/nạp giữa chừng `continueVar == 2`, giữ nguyên `goal` đã lưu; riêng World 2 Lv0 là màn cứu thuyền nên `goal = score` trước đó và reset `score = 0`.)*

### 6.2. Bảng hằng số cân bằng độ khó gốc

| Chế độ | Độ khó (`dificulty`) | Thuốc nổ đầu (`stick`) | `goalChanger` khởi điểm | `goalChangerAddOn` / màn | `goalChangerMax` | `goalOverPersent` | Biến thể màn (`sublevel r`) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Story (`1`)** | `1` (Easy) | `3` | `0.38` | `+0.01` | `0.42` | `0.38` | `r ∈ {1, 2, 3}` |
| **Story (`1`)** | `2` (Medium) | `1` | `0.42` | `+0.01` | `0.44` | `0.46` | `r ∈ {1, 2, 3, 4, 5}` |
| **Story (`1`)** | `3` (Hard) | `0` | `0.44` | `+0.01` | `0.45` | `0.50` | `r ∈ {3, 4, 5}` |
| **Gold Rush (`2`)** | `1` (Easy) | `3` | `0.40` | `+0.01` | `0.44` | `0.25` | Như trên |
| **Gold Rush (`2`)** | `2` (Medium) | `1` | `0.40` | `+0.005` | `0.47` | `0.27` | Như trên |
| **Gold Rush (`2`)** | `3` (Hard) | `0` | `0.35` | `+0.005` | `0.50` | `0.30` | Như trên |
| **2 Players (`3`)** | `2` (Cố định) | `5` (cả 2 P) | `0.43` | `+0.01` | `0.44` | `0.25` | 6 màn đối kháng |

* **Vòng lặp Gold Rush (`frame_270/DoAction.as`):** Khi vượt qua World 3 Level 18, quay lại World 1 Level 1 nhưng tăng thêm `goalChangerMax += 0.5` và `goalOverPersent += 0.1`!

### 6.3. Cơ chế lưu/nạp dữ liệu màn & Lưu ý quan trọng
* Trong source SWF, `loadLevel()` gọi `loadVariables("GoldMinerVegas\\Levels\\MGVegas_{world}_{level}.txt")` (file text ngoài nằm cạnh file `.exe` cài đặt, **không nhúng trong SWF** nên không có sẵn các file `MGVegas_*.txt` trong `Gold_Miner_Vegas_Source`).
* Tuy nhiên, schema đầy đủ của file màn đã được trích xuất nguyên vẹn từ `convertVars()` trong `frame_1/DoAction_2.as`:
  * `levelCodeN_{L}_{r}`, `dirtN_{L}_{r}`, `trackN_{L}_{r}` (chọn nền mỏ, mặt đất, đường ray), `things_{L}_{r}` (số lượng vật thể), `levelValue_{L}_{r}` (tổng giá trị lý thuyết trên màn).
  * Với `x = 1..things`: `T{x}N` (`objectN` `1..88`), `T{x}X` (`40..760`), `T{x}Y` (`220..530`).
* **Số liệu tham chiếu Màn 1 chuẩn từ `DefineButton2_760`:**
  * Khởi tạo save mới: `world=1, level=1, score=0, goal=555, sublevel=3, stick=3, dificulty=1, cart=1, goalChanger=0.39`.
  * Suy ngược giá trị màn 1: `levelValue ≈ 555 / 0.39 ≈ $1,423` (tỷ lệ `levelValue / goal ≈ 2.56×`, hoàn toàn thỏa mãn ràng buộc `LevelValidator >= 1.6×` của dự án).

### 6.4. Mẫu dữ liệu JSON 4 màn đầu chuẩn Vegas (`480 × 270`, sẵn sàng cho `assets/data/levels/`)

#### `level_1.json` — Màn 1: Khởi động Australia (`goal: 555`, `levelValue: 1425`, `track: [12, 468]`)
```json
{
  "level": 1,
  "world": 1,
  "themeHint": "australia",
  "durationSeconds": 60,
  "goal": 555,
  "levelValue": 1425,
  "cartRange": { "minX": 12, "maxX": 468 },
  "items": [
    { "objectN": 5, "type": "gold_large", "x": 110, "y": 55, "value": 500, "weight": 9, "radius": 16.2 },
    { "objectN": 4, "type": "gold_medium", "x": 365, "y": 70, "value": 250, "weight": 8, "radius": 10.7 },
    { "objectN": 4, "type": "gold_medium", "x": 235, "y": 48, "value": 250, "weight": 8, "radius": 10.7 },
    { "objectN": 3, "type": "gold_small_med", "x": 175, "y": 115, "value": 100, "weight": 7, "radius": 6.5 },
    { "objectN": 3, "type": "gold_small_med", "x": 305, "y": 112, "value": 100, "weight": 7, "radius": 6.5 },
    { "objectN": 2, "type": "gold_small", "x": 80, "y": 132, "value": 50, "weight": 3, "radius": 3.5 },
    { "objectN": 2, "type": "gold_small", "x": 245, "y": 138, "value": 50, "weight": 3, "radius": 3.5 },
    { "objectN": 2, "type": "gold_small", "x": 405, "y": 128, "value": 50, "weight": 3, "radius": 3.5 },
    { "objectN": 11, "type": "mystery_bag", "x": 395, "y": 50, "value": 0, "weight": 7, "radius": 9.2 },
    { "objectN": 8, "type": "rock_large", "x": 160, "y": 78, "value": 20, "weight": 9, "radius": 14.7 },
    { "objectN": 8, "type": "rock_large", "x": 325, "y": 65, "value": 20, "weight": 9, "radius": 14.7 },
    { "objectN": 9, "type": "rock_medium", "x": 215, "y": 95, "value": 11, "weight": 8, "radius": 8.9 },
    { "objectN": 10, "type": "rock_small", "x": 135, "y": 135, "value": 5, "weight": 4, "radius": 4.7 },
    { "objectN": 10, "type": "rock_small", "x": 355, "y": 132, "value": 5, "weight": 4, "radius": 4.7 }
  ]
}
```

#### `level_2.json` — Màn 2: Kim cương & Thùng TNT (`goal: 1150`, `levelValue: 2635`, nối tiếp công thức `setGoal`)
```json
{
  "level": 2,
  "world": 1,
  "themeHint": "australia",
  "durationSeconds": 60,
  "goal": 1150,
  "levelValue": 2635,
  "cartRange": { "minX": 12, "maxX": 468 },
  "items": [
    { "objectN": 13, "type": "diamond", "x": 85, "y": 42, "value": 600, "weight": 2, "radius": 4.2 },
    { "objectN": 13, "type": "diamond", "x": 410, "y": 48, "value": 600, "weight": 2, "radius": 4.2 },
    { "objectN": 5, "type": "gold_large", "x": 245, "y": 50, "value": 500, "weight": 9, "radius": 16.2 },
    { "objectN": 4, "type": "gold_medium", "x": 150, "y": 88, "value": 250, "weight": 8, "radius": 10.7 },
    { "objectN": 4, "type": "gold_medium", "x": 340, "y": 88, "value": 250, "weight": 8, "radius": 10.7 },
    { "objectN": 3, "type": "gold_small_med", "x": 195, "y": 125, "value": 100, "weight": 7, "radius": 6.5 },
    { "objectN": 3, "type": "gold_small_med", "x": 295, "y": 122, "value": 100, "weight": 7, "radius": 6.5 },
    { "objectN": 2, "type": "gold_small", "x": 125, "y": 138, "value": 50, "weight": 3, "radius": 3.5 },
    { "objectN": 2, "type": "gold_small", "x": 360, "y": 136, "value": 50, "weight": 3, "radius": 3.5 },
    { "objectN": 11, "type": "mystery_bag", "x": 240, "y": 105, "value": 0, "weight": 7, "radius": 9.2 },
    { "objectN": 12, "type": "tnt_barrel", "x": 180, "y": 60, "value": 1, "weight": 2, "radius": 12.4, "blastRadius": 40.5 },
    { "objectN": 12, "type": "tnt_barrel", "x": 385, "y": 72, "value": 1, "weight": 2, "radius": 12.4, "blastRadius": 40.5 },
    { "objectN": 8, "type": "rock_large", "x": 165, "y": 48, "value": 20, "weight": 9, "radius": 14.7 },
    { "objectN": 8, "type": "rock_large", "x": 310, "y": 48, "value": 20, "weight": 9, "radius": 14.7 },
    { "objectN": 9, "type": "rock_medium", "x": 245, "y": 136, "value": 11, "weight": 8, "radius": 8.9 },
    { "objectN": 14, "type": "skull", "x": 105, "y": 105, "value": 20, "weight": 2, "radius": 4.5 }
  ]
}
```

#### `level_3.json` — Màn 3: Chuột túi Bilby & Bilby Kim cương (`goal: 1900`, `levelValue: 3420`)
```json
{
  "level": 3,
  "world": 1,
  "themeHint": "australia",
  "durationSeconds": 60,
  "goal": 1900,
  "levelValue": 3420,
  "cartRange": { "minX": 12, "maxX": 468 },
  "items": [
    { "objectN": 22, "type": "mouse_diamond", "x": 140, "y": 82, "value": 610, "weight": 5, "radius": 12.0, "speedMin": 33.2, "speedMax": 48.6 },
    { "objectN": 22, "type": "mouse_diamond", "x": 340, "y": 54, "value": 610, "weight": 5, "radius": 12.0, "speedMin": 33.2, "speedMax": 48.6 },
    { "objectN": 21, "type": "mouse", "x": 240, "y": 115, "value": 10, "weight": 3, "radius": 11.0, "speed": 42.1 },
    { "objectN": 5, "type": "gold_large", "x": 95, "y": 52, "value": 500, "weight": 9, "radius": 16.2 },
    { "objectN": 5, "type": "gold_large", "x": 395, "y": 48, "value": 500, "weight": 9, "radius": 16.2 },
    { "objectN": 6, "type": "gold_mega", "x": 240, "y": 42, "value": 1000, "weight": 11, "radius": 19.4 },
    { "objectN": 4, "type": "gold_medium", "x": 185, "y": 95, "value": 250, "weight": 8, "radius": 10.7 },
    { "objectN": 11, "type": "mystery_bag", "x": 300, "y": 98, "value": 0, "weight": 7, "radius": 9.2 },
    { "objectN": 8, "type": "rock_large", "x": 145, "y": 45, "value": 20, "weight": 9, "radius": 14.7 },
    { "objectN": 8, "type": "rock_large", "x": 335, "y": 110, "value": 20, "weight": 9, "radius": 14.7 }
  ]
}
```

#### `level_4.json` — Màn 4 (Mẫu đặc trưng Las Vegas): 3 Máy Slot, Két sắt & Phỉnh bài (`goal: 2800`, `levelValue: 4160`)
```json
{
  "level": 4,
  "world": 3,
  "themeHint": "vegas",
  "durationSeconds": 60,
  "goal": 2800,
  "levelValue": 4160,
  "cartRange": { "minX": 12, "maxX": 468 },
  "items": [
    { "objectN": 72, "type": "slot_machine", "slotIndex": 1, "x": 150, "y": 125, "value": 0, "weight": -1, "radius": 11.0 },
    { "objectN": 73, "type": "slot_machine", "slotIndex": 2, "x": 240, "y": 125, "value": 0, "weight": -1, "radius": 11.0 },
    { "objectN": 74, "type": "slot_machine", "slotIndex": 3, "x": 330, "y": 125, "value": 0, "weight": -1, "radius": 11.0 },
    { "objectN": 79, "type": "safe_locked", "x": 240, "y": 48, "value": 800, "weight": 10, "radius": 11.7 },
    { "objectN": 13, "type": "diamond", "x": 85, "y": 65, "value": 600, "weight": 2, "radius": 4.2 },
    { "objectN": 13, "type": "diamond", "x": 395, "y": 65, "value": 600, "weight": 2, "radius": 4.2 },
    { "objectN": 5, "type": "gold_large", "x": 130, "y": 45, "value": 500, "weight": 9, "radius": 16.2 },
    { "objectN": 5, "type": "gold_large", "x": 350, "y": 45, "value": 500, "weight": 9, "radius": 16.2 },
    { "objectN": 68, "type": "poker_chip_1", "x": 70, "y": 110, "value": 100, "weight": 3, "radius": 4.0 },
    { "objectN": 69, "type": "poker_chip_2", "x": 195, "y": 85, "value": 100, "weight": 3, "radius": 4.0 },
    { "objectN": 70, "type": "poker_chip_3", "x": 285, "y": 85, "value": 100, "weight": 3, "radius": 4.0 },
    { "objectN": 71, "type": "poker_chip_4", "x": 410, "y": 110, "value": 100, "weight": 3, "radius": 4.0 },
    { "objectN": 87, "type": "mystery_bag_vegas", "x": 240, "y": 90, "value": 0, "weight": 7, "radius": 9.2 },
    { "objectN": 12, "type": "tnt_barrel", "x": 200, "y": 52, "value": 1, "weight": 2, "radius": 12.4, "blastRadius": 40.5 },
    { "objectN": 60, "type": "broken_wall", "x": 300, "y": 50, "value": 140, "weight": 12, "radius": 21.0 }
  ]
}
```

---

## HẠNG MỤC 7. TÍNH NĂNG ĐẶC TRƯNG CỦA BẢN VEGAS (VEGAS BONUS / CASINO)

### 7.1. Mini-game Máy Slot trong mỏ (`T39, T40, T41` — `DefineSprite_2611, 2613, 2615` + `DefineSprite_3015`)
* **Thiết lập:** Trên các màn Slot ở Las Vegas có **3 máy Slot** (`T39`, `T40`, `T41`), khởi tạo ban đầu với 3 biểu tượng khác nhau (`1`, `2`, `3`).
* **Cơ chế kích hoạt:** Khi ngoàm chạm vào 1 máy bất kỳ (`weight = -1` nên ngoàm bật thu về ngay lập tức), máy đó quay ~35 frame (`~1.94 giây`) rồi chốt kết quả mới tại `frame_50`:
  * **Xác suất "trợ lực" (Rigged Luck — `frame_50/DoAction.as`):** Quay `luck = random(10) + 1`:
    * **`40%` (`luck >= 7`):** Tự động khớp biểu tượng với 2 máy còn lại (nếu 2 máy kia đang trùng nhau thì **chắc chắn nổ Jackpot 3 hình giống nhau**; nếu 2 máy kia khác nhau thì tự ghép đôi 2 hình giống nhau để tạo cơ hội ăn ngay lượt sau!).
    * **`60%` (`luck <= 6`):** Quay ngẫu nhiên đều `random(3) + 1` (1/3 mỗi biểu tượng).
* **Bảng trả thưởng 3 cấp (`DefineSprite_3015` `comboMov`):**
  * **`"slot1"` (Giải Đặc biệt):** Cả 3 máy trùng biểu tượng `1` → từng máy nảy rung và bay chữ `+$1,000` về ví → **Tổng `+$3,000`**!
  * **`"slot2"` (Giải Nhì):** Cả 3 máy trùng biểu tượng `2` → 3 đợt `+$500` → **Tổng `+$1,500`**!
  * **`"slot3"` (Giải Ba):** Cả 3 máy trùng biểu tượng `3` → 3 đợt `+$150` → **Tổng `+$450`**!
* **Quay lại sau trúng (`frame_99..148`):** Ngay sau khi ăn thưởng, cả 3 máy tự quay lại ngẫu nhiên đều (`random(3) + 1`) — có xác suất $3 \times (1/3)^3 = 1/9 \approx 11.1\%$ trúng tiếp miễn phí!

### 7.2. Combo sưu tập 4 món (`gemCount >= 4` → `+$2,000`)
* Áp dụng cho **4 Gems** (`23..26`), **4 Tượng cổ** (`52..55`) hoặc **4 Poker Chips** (`68..71`).
* Mỗi món kéo lên bình thường được `$100` và đánh dấu vào bảng `gemMov`. Khi kéo trúng món thứ 4 trong cùng 1 màn (`gemCount >= 4`), giá của món thứ 4 biến thành `"gem"`, kích hoạt chuỗi cộng tiền liên hoàn ở `DefineSprite_3075/frame_263..284`:
  $$12 \times \$50 + 9 \times \$100 + \$500 = \mathbf{+\$2,000}$$

### 7.3. Gợi ý áp dụng cho Khu Casino & Arcade của `Gold_Miner_Java_LibGDX`
* Có thể tận dụng chính bộ 3 biểu tượng Máy Slot của Vegas làm **trò Casino thứ 2** (sau Bầu Cua Tôm Cá ở Chặng 3): thay vì dùng logic trợ lực 40% trong mỏ, ta đặt xác suất công bằng $1/N$ cho từng guồng quay và cân chỉnh bảng trả thưởng để đảm bảo `House Edge` chuẩn Casino (~5–8%) trong `domain/casino/`!

---

## HẠNG MỤC 8. SỰ KIỆN ÂM THANH & PHẢN HỒI HÌNH ẢNH (AUDIO / VISUAL CUES)

### 8.1. Bảng ánh xạ Sự kiện → Âm thanh & Hiệu ứng hình ảnh

| Sự kiện Gameplay | File Âm thanh gốc (`sounds/`) | Thời lượng | Phản hồi Hình ảnh / Animation |
| :--- | :--- | :---: | :--- |
| **Nhạc nền Menu / W1 / W2 / W3** | `132.mp3`, `133.mp3`, `134.mp3`, `135.mp3` | `49.5–56.8s` | `musicMov.gotoAndPlay("main" / "W1" / "W2" / "W3")` |
| **Thả ngoàm (`EXTENDING`)** | `Tha_Cau.mp3` | `1.59s` | Bác thợ mỏ chuyển state `"down"` |
| **Thu ngoàm / Quay tời (`RETRACTING`)** | `Keo_Cau.mp3` | `0.76s` | Bác thợ mỏ chuyển state `"up"`, xoay tay quay |
| **Đứt dây (`brake`)** | `2771.mp3` / nhóm `2756–2789.mp3` | `~3.11s` | Bác thợ mỏ chuyển state `"brake"` lắc đầu ngã ngửa |
| **Nổ TNT / Thuốc nổ / Lava** | `1939.mp3` (và `2466 / 2494.mp3`) | `~1.3s` | Hoạt ảnh nổ 16 frame `DefineSprite_1938` (`85×80 px`) + khói `DefineSprite_2824` |
| **Cộng tiền & Chữ bay lên (`bonus`)** | `NhanTien.mp3` / `TinhTien.mp3` | `0.55s` / `0.84s` | `DefineSprite_3075`: Hiện `+$xxx` trên đầu xe, dịch ngang 6 frame, nội suy 10 bước (`0.1 → 1.0`, `~0.55s`) bay thẳng về ô `TOTAL` (`moneyBlackMov`). Khi `score >= goal`, đổi màu ô `TOTAL` và bật nút qua màn sớm |
| **Đếm ngược cuối giờ (`time <= 5s`)** | `1825.mp3` | `0.13s` | Nhảy sang nhãn `"beep"` (`frame_20..22`), chữ số timer phồng nhẹ báo hiệu |
| **Xe mỏ chạy / dừng** | `2817.mp3`, `2818.mp3`, `2819.mp3` | `0.5–1.7s` | `cartMoveSound` + hiệu ứng vòng click `clickSpotMov` trên đường ray |
| **Trúng Slot / Đủ 4 món Combo** | `2608–2610.mp3`, `3005.mp3`, `3010.mp3` | `0.2–1.5s` | Rung lắc máy Slot (`-5°, -10°, -5°, +5°, +7°, -3°, 0°`) + chữ bay `comboMov` |
| **Tương tác Cửa hàng (Lex nói / mua)** | `1553, 1559, 1565, 1631, 1662, 1664.mp3` | `0.2–0.8s` | `lexMov` chuyển `"talk"` / `"buy"`, cập nhật bong bóng thoại |
| **Qua màn (`pass`) / Thua màn (`nopass`)** | `1479.mp3` / `1730.mp3` | `0.3s` / `0.5s` | Chuyển màn hình tổng kết / cửa hàng |

* **Toàn bộ trạng thái hoạt ảnh Bác thợ mỏ (`miner`):** `1` (idle), `"down"` (thả dây), `"up"` (kéo dây), `"throw"` (ném thuốc nổ), `"brake"` (đứt dây), `"strength"` (uống bình sức mạnh), `"ghost"` (bị ma hù), `"ant"` (bị kiến cắn), `"jewl2"` (nhận quà từ Julie), `"rib"` (ăn đĩa sườn), `"zap"` (bị giật điện).

---

## HẠNG MỤC 9. KIỂM KÊ & BÓC TÁCH TÀI NGUYÊN ĐỒ HỌA + ÂM THANH (ASSETS INVENTORY)

### 9.1. Danh mục tài nguyên thực tế & Ánh xạ Semantic Keys

| Nhóm tài nguyên | Vị trí trong `Gold_Miner_Vegas_Source` | Định dạng & Kích thước mẫu | Ánh xạ Semantic Key đề xuất (`assets/themes/vegas/`) |
| :--- | :--- | :--- | :--- |
| **Bác thợ mỏ (Miner)** | `sprites/DefineSprite_2741..2811` (P1) & `2870..2997` (P2) | `.png` trong suốt (`~45–178 px`, idle/pull/throw/strength/zap...) | `miner_idle`, `miner_down`, `miner_pull_*`, `miner_throw_*`, `miner_brake_*`, `miner_strength_*`, `miner_zap_*` |
| **Xe đẩy & Thuyền** | `sprites/DefineSprite_2998` (3 frame), `2810/2811` | `.png` trong suốt | `cart_standard`, `cart_boat`, `cart_p2` |
| **Đầu ngoàm cào** | `sprites/DefineSprite_2826` (4 frame), `2861` (126 frame ngoàm kẹp từng vật) | `.png` trong suốt (`381×295` khung, cắt viền còn `~30–100 px`) | `hook_claw_normal`, `hook_claw_spike`, `hook_claw_laser`, `hook_claw_super` |
| **Vật thể trong mỏ (`1..88`)** | `sprites/DefineSprite_2685` (`2.png..87.png` tĩnh) & `1844..2684` (có animation) | `.png` trong suốt (`16×15 px` đến `374×272 px`) | `gold_small`, `gold_medium`, `gold_large`, `gold_mega`, `rock_small/medium/large`, `diamond`, `mystery_bag`, `tnt_barrel`, `mouse_walk_*`, `mouse_diamond_*`, `slot_machine_*`, `safe_locked`, `safe_open`, `gem_1..4`, `chip_1..4` |
| **Nền mỏ, Hang & Đường ray** | `sprites/DefineSprite_3106, 3107, 3132, 3133, 3162, 3163, 3177, 3201, 3219` + `1816` (12 ray) | `.png` trong suốt (`883×364` đến `1292×368`) | `bg_mine_w1`, `bg_mine_w2`, `bg_mine_w3`, `bg_cave_w1..3`, `track_01..12` |
| **Màn hình Menu / Shop / Map** | `images/1320, 1331, 1346, 1376, 1461, 1463, 1465, 1501, 1583, 1637, 1716, 1732.jpg/.png` + `sprites/DefineSprite_1568, 1636, 1714` | `.jpg` & `.png` (`800 × 600`) | `bg_menu`, `bg_shop_w1..3`, `bg_map`, `bg_pass`, `bg_nopass` |
| **Hiệu ứng nổ & Khói** | `sprites/DefineSprite_1938` (16f), `DefineSprite_2824` (10f) | `.png` trong suốt (`85×80`, `87×80`) | `fx_explosion_01..16`, `fx_smoke_01..10` |
| **Nút bấm UI & Vật phẩm Shop** | `buttons/DefineButton2_*` (544 `.png`, ví dụ `1509..1703` kích thước `35×64` đến `178×152`) | `.png` trong suốt | `btn_play`, `btn_pause`, `btn_next`, `shop_strength`, `shop_oil`, `shop_clover`, `shop_dynamite`, `shop_watch`, `shop_polish`, `shop_cable`, `shop_poster`, `shop_spray` |
| **Âm thanh BGM & SFX** | `sounds/` (76 `.mp3`; bỏ qua `-1.wav` 44B) | `.mp3` (BGM ~`50–57s`, SFX `0.13–3.1s`) | `bgm_menu`, `bgm_w1..3`, `sfx_hook_extend`, `sfx_hook_retract`, `sfx_explosion`, `sfx_coin`, `sfx_countdown_beep`, `sfx_pass`, `sfx_nopass`, `sfx_slot_spin`, `sfx_slot_win` |
| **Phông chữ** | `fonts/` (14 `.ttf`) | `.ttf` (`CAC Norm Heavy`, `Futura Md BT`, `CCZoinks`...) | Giữ font số/logo Vegas nếu muốn, nhưng **bắt buộc bổ sung font `.ttf` hỗ trợ đầy đủ dấu Tiếng Việt** (nạp qua `gdx-freetype`) cho UI |

### 9.2. Phương án chuyển đổi sạch sang `assets/themes/vegas/` (cho Bước 0.3)

> **Tin rất thuận lợi:** Toàn bộ hình ảnh động và vật thể trong `sprites/`, `buttons/`, `images/` đã được xuất sẵn sang **`.png` nền trong suốt** sắc nét — **KHÔNG cần viết pipeline render lại 1.885 file `.svg` trong `shapes/`!**

Quy trình đề xuất thực hiện ở **Bước 0.3**:
1. **Auto-crop & Chuẩn hóa tên Semantic Key:** Viết 1 script Python nhỏ dùng `Pillow` (`Image.getbbox()`) để cắt bỏ phần viền trong suốt thừa (ví dụ `DefineSprite_2685` đang đặt vật trên khung `1352×814`) và copy có chọn lọc các sprite cốt lõi sang thư mục tạm với tên Semantic Key chuẩn (`gold_small.png`, `tnt_barrel.png`, `fx_explosion_01.png`...).
2. **Đóng gói `TextureAtlas`:** Dùng `TexturePacker` của libGDX đóng gói toàn bộ sprite đã crop thành `assets/themes/vegas/atlas/vegas.atlas` + `vegas.png` (giảm số lần bind texture xuống mức tối thiểu, đảm bảo 60 FPS mượt). Riêng các ảnh nền lớn `800×600` (Menu, Shop) có thể giữ riêng trong `assets/themes/vegas/backgrounds/` để tránh tràn kích thước trang atlas `2048×2048`.
3. **Cấu hình `assets/themes/vegas/theme.json`:** Khai báo `"filter": "LINEAR"` (dùng `TextureFilter.Linear` cho đồ họa Flash mịn mượt) và bảng ánh xạ `semanticKey -> regionName / filePath`.
4. **Chuẩn hóa Âm thanh:**
   * Copy các file `.mp3` đã lọc sang `assets/themes/vegas/audio/bgm/` và `sfx/` theo Semantic Key.
   * *(Khuyến nghị tối ưu):* libGDX đọc `.mp3` trực tiếp được trên Desktop, nhưng nếu máy có sẵn `ffmpeg`, chuyển BGM & SFX sang `.ogg` (Vorbis) sẽ giúp nhạc nền loop không bị khựng 1 khoảng im lặng ở đầu/cuối file MP3 và giảm độ trễ giải mã OpenAL.
