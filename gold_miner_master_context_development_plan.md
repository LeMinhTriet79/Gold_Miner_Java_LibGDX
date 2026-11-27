# GOLD MINER (JAVA + LIBGDX) — MASTER CONTEXT & KẾ HOẠCH PHÁT TRIỂN TOÀN DIỆN

> **Mục đích file này:** Đây là tài liệu gốc duy nhất (Single Source of Truth) chứa trọn vẹn **tư duy kiến trúc, quy tắc kỹ thuật, chỉ thị khai thác trọn bộ mã nguồn + tài nguyên Gold Miner Vegas, mô tả nội dung game và kế hoạch phát triển chi tiết từng bước** của dự án `Gold_Miner_Java_LibGDX`.
>
> **Tinh thần dự án:** **Dự án cá nhân làm để chơi cho vui, thỏa mãn hoài niệm tuổi thơ và rèn tư duy hệ thống.** KHÔNG đặt nặng mục tiêu thương mại hóa, KHÔNG cần lo thủ tục phát hành Steam/App Store hay rào cản bản quyền bán hàng. Ưu tiên cao nhất là **tận dụng trọn vẹn cảm giác chơi + đồ họa/âm thanh huyền thoại của bản Gold Miner Vegas**, kiến trúc sạch để dễ thay đổi đồ họa (Theme Pack) và tự do mở rộng thêm chế độ chơi về sau.
>
> **Yêu cầu với AI Agent (Antigravity):** Đọc kỹ toàn bộ file này trước khi đề xuất kế hoạch hoặc viết bất kỳ dòng code nào. Phải thực hiện **Bước 0.0** (truy cập và khai thác thư mục source gốc ở **Phần 3** để xuất `VEGAS_EXTRACTED_SPECS.md`) và chờ người dùng duyệt xong trước khi chỉnh sửa code Java.

---

## PHẦN 1. VAI TRÒ & KỶ LUẬT LÀM VIỆC CỦA AI (PERSONA)

Bạn đóng vai **Kiến trúc sư phần mềm kiêm Senior Game Developer có 20 năm kinh nghiệm** (đồng thời là người thầy hướng dẫn tận tâm). Người dùng là sinh viên/lập trình viên Java (quen Spring Boot, SOLID, Clean Architecture, TDD) đang làm game 2D bằng **Java + libGDX** để giải trí và thỏa đam mê.

### Quy tắc đồng hành bắt buộc

1. **Dẫn đường rõ ràng, làm tới đâu hiểu tới đó:** Trước mỗi bước nhỏ, giải thích ngắn gọn *bước này làm gì, đọc/tạo/sửa file nào, chạy lên sẽ thấy kết quả gì*, chờ người dùng xác nhận rồi mới sửa code.
2. **Làm bản chơi được trước, sáng tạo mở rộng sau:** Dù là dự án chơi cho vui và hoàn toàn tự do thêm các trò quái chiêu (nhiều trò Casino, Arcade, thậm chí chế độ kinh dị Backrooms về sau), vẫn phải giữ kỷ luật làm xong **lõi Đào Vàng chạy mượt (Bản 1.0)** trước rồi mới đắp từng mô-đun vui vẻ vào sau để tránh nản giữa chừng.
3. **Giữ sạch kiến trúc dù làm để chơi:** Không vì làm cho vui mà viết code rối mù kiểu "mì gói" (spaghetti). Tuân thủ nghiêm ngặt việc tách lõi logic Java thuần (`domain/`) khỏi phần vẽ (`libGDX`) và tách kích thước va chạm khỏi kích thước ảnh để sau này đổi Theme cực nhàn.
4. **Ngôn ngữ & trình bày:** Tiếng Việt tự nhiên, gần gũi, trực diện. Giữ nguyên thuật ngữ kỹ thuật tiếng Anh (`Viewport`, `Delta Time`, `TextureAtlas`, `House Edge`, `Theme Pack`, `Greyboxing`, `Scene2D`). Khi có nhiều cách làm, chốt 1 cách gọn nhất và giải thích lý do.
5. **Chốt tiến độ minh bạch:** Xong bước nào phải chạy kiểm chứng (chạy game hoặc pass JUnit test) và đánh dấu `[x]` vào **Phần 7** trước khi đi tiếp.

---

## PHẦN 2. THÔNG TIN WORKSPACE & TECH STACK THỰC TẾ

* **Thư mục dự án chính:** `Gold_Miner_Java_LibGDX`
* **Thư mục source gốc tham khảo (Gold Miner Vegas):**
  `C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source`
* **Package gốc (Root Package):** `com.minhtriet79.goldminer`
* **Cấu trúc module Gradle hiện tại:**
  * `core/` — Toàn bộ mã nguồn game (tách lõi `domain/` thuần Java và tầng hiển thị libGDX).
  * `lwjgl3/` — Điểm khởi chạy bản Desktop PC (`com.minhtriet79.goldminer.lwjgl3.Lwjgl3Launcher`).
  * `assets/` — Chứa file dữ liệu JSON (`data/`), map Tiled `.tmx` (`maps/`), các gói giao diện theo chủ đề (`themes/vegas/`, `themes/pixel16/`), font tiếng Việt.
* **Tech Stack chốt cứng:**
  * Java 17+ (LTS), Gradle.
  * libGDX cốt lõi (`SpriteBatch`, `Scene2D`, `AssetManager`, `Json`, `TmxMapLoader`) + có thể bổ sung `gdx-freetype` (hiển thị phông chữ có dấu tiếng Việt sắc nét) và `ShapeDrawer` (vẽ dây cáp mượt ngay trong `SpriteBatch`).
  * **KHÔNG dùng Box2D:** Dùng va chạm hình học cơ bản (`Circle`, `Rectangle`, `Intersector`) cho nhẹ và khớp chính xác cảm giác kéo của bản Vegas gốc.
  * **JUnit 5** trong `core/build.gradle` để test logic thuần ở `domain/`.

---

## PHẦN 3. CHỈ THỊ ĐẶC BIỆT — KHAI THÁC ("BÀO SẠCH") SOURCE & TÀI NGUYÊN GOLD MINER VEGAS

Người dùng đã có sẵn toàn bộ mã nguồn và tài nguyên bản **Gold Miner Vegas** cổ điển tại đường dẫn:
> **`C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source`**

### 3.1. Nhiệm vụ bắt buộc của AI Agent ở Bước 0.0

AI Agent **PHẢI** chủ động truy cập, đọc sâu và phân tích toàn bộ mã nguồn, script, file cấu hình, hằng số, hình ảnh và âm thanh trong thư mục trên để "bào sạch" toàn bộ bí kíp của bản gốc, sau đó tổng hợp thành một file tài liệu kỹ thuật đầy đủ tên là **`VEGAS_EXTRACTED_SPECS.md`** lưu ngay tại thư mục gốc `Gold_Miner_Java_LibGDX`.

*(Người dùng sẽ gửi file `VEGAS_EXTRACTED_SPECS.md` này đi kiểm duyệt chéo trước khi bật đèn xanh cho các bước code Java tiếp theo.)*

### 3.2. Danh sách 9 hạng mục bắt buộc phải trích xuất từ `Gold_Miner_Vegas_Source`

1. **Nhịp khung hình & Thời gian (FPS & Timing):**
   * Xác định FPS chuẩn của bản Flash/Vegas gốc (ví dụ 24, 30 hay 60 FPS) và thời lượng mỗi màn (giây).
   * Quy đổi **tất cả** tốc độ theo khung hình (frame-based) sang tốc độ theo giây (`dt` - delta time) theo công thức:
     $$\text{Speed}_{\text{per sec}} = \text{Speed}_{\text{per frame}} \times \text{FPS}_{\text{gốc}}$$
2. **Cơ chế ngoàm cào (Hook Physics & Game Feel):**
   * Góc lắc cực đại trái/phải (ví dụ $-80^\circ$ đến $+80^\circ$), tốc độ xoay góc (xoay đều tuyến tính hay có hàm `sin`/`cos` giảm tốc ở hai biên?).
   * Tốc độ phóng dây (`extendSpeed`), tốc độ thu dây rỗng (`emptyRetractSpeed`).
   * **Công thức tính tốc độ kéo theo độ nặng (`weight`)** và hệ số tăng tốc khi uống **Bình sức mạnh (Strength Drink)** hoặc nâng cấp tời (nếu có).
   * Tọa độ tâm tời, kích thước màn hình gốc của bản Vegas (ví dụ `640x480` hay `800x600`) để tính hệ số quy đổi chính xác sang hệ tọa độ ảo của game mới.
3. **Bảng vật thể & Chướng ngại vật đầy đủ:**
   * Liệt kê toàn bộ vật thể trong bản Vegas: Vàng các cỡ, Đá các cỡ, Kim cương, Túi bí ẩn, Thùng TNT, Chuột / Chuột có kim cương, Xương, Tượng, hoặc vật đặc trưng của bản Vegas.
   * Với mỗi vật thể: giá trị tiền (`$`), độ nặng (`weight`), kích thước hitbox tương đối, hành vi di chuyển (tốc độ chuột, bán kính nổ TNT, phản ứng dây chuyền TNT).
4. **Cơ chế Túi Bí Ẩn (Mystery Bag):**
   * Trích xuất chính xác bảng xác suất (probability table): tỉ lệ ra tiền (khoảng tiền cụ thể), ra thuốc nổ, ra sức mạnh, ảnh hưởng của **Cỏ 4 lá (Lucky Clover)** lên tỉ lệ này.
5. **Hệ thống Cửa hàng giữa màn (Vegas Shop Logic):**
   * Danh sách toàn bộ vật phẩm bán trong shop (Thuốc nổ, Bình sức mạnh, Cỏ 4 lá, Sách sưu tầm đá, Nước đánh bóng kim cương, Đồng hồ... tùy bản gốc có gì).
   * **Công thức định giá động theo màn** và tỉ lệ xuất hiện món đồ ở từng màn.
6. **Đường cong độ khó & Bố cục màn chơi (Level Curve & Layouts):**
   * Công thức tăng `Goal` (mục tiêu tiền) từ màn 1 trở đi.
   * Cách bản Vegas lưu/tạo các màn chơi cố định hoặc lặp vòng; trích xuất mẫu dữ liệu 3–5 màn đầu tiên để chuyển sang JSON.
7. **Tính năng đặc trưng của bản Vegas (Vegas Bonus / Casino):**
   * Nếu trong source Vegas có mini-game, vòng quay, cào bài, máy slot hay cơ chế thưởng kiểu Las Vegas, trích xuất toàn bộ luật chơi và tỉ lệ để tham khảo cho **Khu Casino** và **Khu Game Thùng**.
8. **Sự kiện âm thanh & Phản hồi hình ảnh (Audio/Visual Cues):**
   * Liệt kê thời điểm phát âm thanh (đếm ngược 10 giây cuối, kéo vật nặng, kẹp kim cương, nổ TNT, mua đồ, hết giờ) và các hiệu ứng chữ bay lên (`+$500`, `STRENGTH UP!`).
9. **Kiểm kê & Bóc tách Tài nguyên Đồ họa + Âm thanh (Assets Inventory — RẤT QUAN TRỌNG):**
   * Liệt kê toàn bộ thư mục chứa hình ảnh và âm thanh trong `Gold_Miner_Vegas_Source` (nhân vật ông già, các khung hình quay tời/kéo dây/ném bom, vàng, đá, kim cương, túi, chuột, TNT, nền mỏ, cửa hàng, nút bấm, hiệu ứng nổ, các file âm thanh/nhạc nền).
   * Ghi rõ **định dạng file thực tế** đang có (`.png`, `.jpg`, `.svg`, `.swf`, `.wav`, `.mp3`, `.ogg`...) và kích thước ảnh mẫu.
   * Đề xuất phương án chuyển đổi sạch sang thư mục `assets/themes/vegas/` của dự án libGDX (ví dụ: nếu là `.png`/`.wav`/`.ogg` thì copy đổi tên chuẩn hóa sang semantic keys; nếu còn ở dạng vector `.svg` thì đề xuất script chuyển đổi hàng loạt sang `.png` nền trong suốt).

### 3.3. Nguyên tắc kỹ thuật khi chuyển đổi từ Source cũ sang Java libGDX

* **KHÔNG chép nguyên cấu trúc code rối của ActionScript/Timeline sang Java.**
* Chỉ **trích xuất toán học, bảng hằng số, luật chơi, bố cục màn** đưa vào file JSON (`assets/data/*.json`) và **tận dụng tài nguyên ảnh/âm thanh** đưa vào `assets/themes/vegas/`, cài đặt lại toàn bộ bằng kiến trúc Java sạch + `dt` (delta time) của libGDX để game mượt trên mọi màn hình và dễ chạy JUnit test.

---

## PHẦN 4. MÔ TẢ TOÀN BỘ NỘI DUNG GAME

### 4.1. Ý tưởng cốt lõi & Ba trụ cột

* **Cảm hứng:** Lối chơi thả ngoàm cào vàng kinh điển chuẩn vị *Gold Miner Vegas* (dùng luôn hình ảnh + âm thanh + số liệu bản Vegas cho bản đầu để chơi sướng ngay) kết hợp khu giải trí **Casino & Game Thùng** kiểu *Tropix*.
* **KHÔNG dùng hệ thống chấm sao (0–3 sao):** Màn cố định mở khóa tuần tự đơn giản (`LOCKED / UNLOCKED / CLEARED`).
* **Ba trụ cột cốt lõi:**
  1. **Cào vàng chuẩn "game feel" bản Vegas:** Tốc độ ngoàm, lực kéo, độ nặng, túi bí ẩn và tiếng "keng" bám sát số liệu bào từ `Gold_Miner_Vegas_Source`.
  2. **Kinh tế 2 tầng tách bạch:** **Tiền Mỏ (`$`)** chạy trong lượt đào vàng (để vượt `Goal` và mua đồ hỗ trợ giữa màn) tách biệt hoàn toàn với **Tiền Chính Thức (`Xu`)** được tổng kết khi kết thúc hoặc thoát lượt chơi để mang đi chơi **Casino** và tiêu ở khu ngoài.
  3. **Kiến trúc Theme Pack & Mô-đun mở rộng:** Mặc định chạy đồ họa `vegas` bóc từ source gốc, nhưng thiết kế sạch để sau này chỉ cần thả thư mục `pixel16` (đồ họa 16-bit kiểu *Bomberman DOS / SNES*) vào là đổi giao diện toàn game trong 1 nốt nhạc; đồng thời dễ dàng thêm trò Casino, Arcade hay chế độ kinh dị về sau.

### 4.2. Luồng điều hướng tổng thể (Flow Bản 1.0)

```text
[Khởi động game]
      │
      ▼
[MenuScreen] (Logo + Số dư TIỀN CHÍNH THỨC góc trên phải + 4 nút chế độ lớn + nút đổi Theme sau này)
      ├── 1. 1 PLAYER ──► [Chọn nhánh: Màn Cố Định / Màn Ngẫu Nhiên]
      │                      ├── Màn Cố Định ─► [LevelSelectScreen (tuần tự, KHÔNG sao)] ─► [PlayScreen]
      │                      └── Màn Ngẫu Nhiên (Seed: Vô tận / Chuỗi 3 màn)  ───────────► [PlayScreen]
      │                                                                                          │
      │                               ┌──────────────────────────────────────────────────────────┤
      │                               ▼ (Qua màn)                                                ▼ (Thua / Bấm THOÁT lượt)
      │                        [ShopScreen]                                              [SettlementScreen]
      │                 (Dùng TIỀN MỎ '$' mua đồ hỗ trợ)                          (Tổng kết lượt -> Cộng TIỀN CHÍNH THỨC
      │                   ├── [NEXT] ──► Màn kế tiếp                                   vào Ví Tổng WalletService)
      │                   └── [THOÁT] ─► [SettlementScreen]                                      │
      │                                                                                          ▼
      │                                                                                    [MenuScreen]
      │
      ├── 2. 2 PLAYERS ─► [Play2POfflineScreen] (Chung máy, 2 bộ phím, KHÔNG cộng Tiền Chính Thức)
      │
      ├── 3. CASINO ────► [CasinoScreen] ─► Bầu Cua Tôm Cá (trước) -> mở rộng Tài Xỉu, Slot... (Cược bằng TIỀN CHÍNH THỨC)
      │
      └── 4. ARCADE ────► [ArcadeScreen] ─► Máy Gắp Thú (trước) -> mở rộng Đập chuột, Pinball... (Nhận Vé đổi skin/đồ vui)
```

*(Toàn bộ nút chế độ nạp qua `ModeRegistry` để sau này đăng ký thêm chế độ mới mà không phải sửa code Menu.)*

### 4.3. Chi tiết các chế độ & Kinh tế 2 tầng

#### A. Chế độ 1 Người (Trọng tâm số 1)
* **Ngoàm cào (`Hook`) 3 trạng thái:** `SWINGING`, `EXTENDING`, `RETRACTING`. Toàn bộ thông số góc lắc, tốc độ thả, công thức kéo theo độ nặng, thuốc nổ phá vật và vật phẩm hỗ trợ bám theo kết quả trích xuất từ `VEGAS_EXTRACTED_SPECS.md` và nạp từ `assets/data/items.json`.
* **Màn cố định:** Mở khóa tuần tự (KHÔNG chấm sao). Ban đầu nạp 3–5 màn JSON chuyển từ `Gold_Miner_Vegas_Source`, giai đoạn sau dùng **Tiled** kéo thả `.tmx` để tự chế thêm màn theo ý thích.
* **Màn ngẫu nhiên (Seeded Procedural):** Sinh theo `seed` (`SeededRandom`) kết hợp `LevelValidator` đảm bảo không chồng vật và tổng giá trị luôn đủ vượt `Goal` ($\ge 1.6 \times \text{Goal}$). Hỗ trợ kiểu **Vô tận (Endless)** và **Nhiệm vụ 3 màn**.
* **Cửa hàng giữa màn (`ShopScreen`):** Mua bằng **Tiền Mỏ (`$`)** của chính lượt đào vàng đang chạy. Các vật phẩm bám theo bản Vegas (Thuốc nổ, Bình sức mạnh, Cỏ 4 lá, Sách đá quý, Nước đánh bóng kim cương, Đồng hồ cộng giờ...). Có nút `[NEXT]` đi tiếp và `[THOÁT & CHỐT SỔ]` dừng lượt.
* **Màn kết toán (`SettlementScreen`):** Khi **thua màn**, **hoàn thành chuỗi**, hoặc **chủ động bấm Thoát màn**, hệ thống chạy `SettlementCalculator` để quy đổi thành quả lượt đào vàng thành **Tiền Chính Thức (`Xu`)** cộng vào ví tổng `WalletService` (chỉ kết toán đúng 1 lần mỗi lượt; nếu chưa kiếm được gì mà thoát ngay thì nhận $0$).

#### B. Chế độ 2 Người (2 Players Offline trước)
* Hai người chung bàn phím (`S`/`W` và `Down`/`Up`) cùng cào chung 1 khu mỏ trong 60 giây, tranh vật trực tiếp. **KHÔNG cộng Tiền Chính Thức** để tránh tự cày tiền.

#### C. Khu Casino (Bản 1.0 làm Bầu Cua trước, dễ thêm trò sau)
* Dùng **Tiền Chính Thức (`Xu`)** từ `WalletService`.
* **Trò đầu tiên:** Bầu Cua Tôm Cá (6 ô linh vật, 3 xúc xắc, xuất hiện $k$ lần với $k \in \{1, 2, 3\}$ thì nhận lại tiền cược cộng $k$ lần tiền cược; house edge tự nhiên $\approx 7.87\%$).
* Có hạn mức cược `minBet`/`maxBet` (cấu hình trong JSON) và hết tiền thì quay về đào vàng kiếm lại vốn.

#### D. Khu Game Thùng / Arcade (Bản 1.0 làm Máy Gắp Thú trước)
* Tận dụng lại cơ chế dây cáp & ngoàm của game chính, điều khiển cần gắp sang trái/phải rồi thả thẳng xuống gắp đồ chơi.
* Trả ra **Vé (Tickets)** để đổi đồ trang trí/skin bác thợ mỏ, màu dây/ngoàm cho vui mắt.

#### E. Bảng tóm tắt Kinh tế 2 tầng (Chốt cứng)

| Loại tiền | Phạm vi tồn tại | Nguồn vào | Nơi tiêu | Lưu vĩnh viễn? |
| :--- | :--- | :--- | :--- | :--- |
| **Tiền Mỏ (`$`)** (`RunSession`) | Chỉ trong 1 lượt Đào Vàng | Kéo vàng, đá, kim cương, túi bí ẩn | Vượt mốc `Goal` qua màn & mua đồ ở **Shop giữa màn** | Không (kết toán xong sẽ reset) |
| **Tiền Chính Thức (`Xu`)** (`WalletService`) | Ví tổng xuyên suốt game | **Kết toán khi hết/thoát lượt Đào Vàng**, thắng Casino | **Cược ở Khu Casino**, phí lượt Arcade, mua đồ khu ngoài | **Có** (lưu trong `SaveData`) |
| **Vé (`Tickets`)** | Khu Arcade & Quầy quà | Chơi Máy Gắp Thú (Arcade) | Đổi skin & đồ thẩm mỹ | **Có** (lưu trong `SaveData`) |

### 4.4. Kho ý tưởng mở rộng tự do (Làm sau Bản 1.0 khi nào thích)

* Thêm các trò Casino khác: Tài Xỉu, Máy xèng (Slot kiểu Vegas), Vòng quay may mắn, Đua thú, Lật bài.
* Thêm các máy Arcade khác: Đập chuột chũi, Bắn máy bay, Đua xe 2D, Pinball, Ghép 3.
* **Đổi Theme sang 16-bit Pixel Art (`themes/pixel16/`):** Tự vẽ hoặc mua/tải bộ pixel art kiểu *Bomberman DOS / SNES* bỏ vào thư mục `pixel16`, bật nút chuyển Theme trong Cài đặt.
* **Chế độ Bí ẩn (Backrooms / Dreamcore):** Gắp trúng vật phẩm lạ hiếm trong mỏ sẽ mở khóa nhánh kinh dị 2D kiểu Bomberman (bom + cúp + thanh tỉnh táo) hoặc thử nghiệm nhánh 3D kiểu Doom bằng libGDX.
* **Khu phố Hub đèn neon** hoặc nghịch làm **Server Spring Boot + WebSocket** chơi 2 người mạng LAN / Online với bạn bè.

---

## PHẦN 5. KIẾN TRÚC ĐỒ HỌA THEME PACK, ÂM THANH & PHÔNG CHỮ

Để **dùng ngay bộ hình ảnh & âm thanh của bản *Gold Miner Vegas* từ đầu**, nhưng về sau muốn tự thay sang **16-bit Pixel Art** (hoặc bất kỳ bộ đồ họa nào khác) mà **KHÔNG phải sửa một dòng code gameplay nào**, bắt buộc tuân thủ 3 quy tắc kiến trúc sau:

### 5.1. Tách kích thước logic khỏi kích thước ảnh (Decoupled Bounds)
* Hệ tọa độ ảo của game cố định theo chuẩn `FitViewport` tỷ lệ 16:9 (`480 x 270` đơn vị thế giới — hoặc chỉnh theo tỉ lệ gốc Vegas sau khi kiểm tra Bước 0.0, nhưng khuyến nghị giữ `480 x 270` đơn vị thế giới kết hợp cửa sổ PC `960 x 540` để tương thích tuyệt đối với cả bộ ảnh Vegas nét mượt lẫn bộ 16-bit sau này).
* **TUYỆT ĐỐI KHÔNG dùng `texture.getWidth()` / `texture.getHeight()` làm kích thước va chạm hay kích thước hiển thị.**
* Mọi vật thể trong `MineWorld` định nghĩa bằng tọa độ tâm `(x, y)` và bán kính va chạm `radius` theo đơn vị thế giới. Khi vẽ, `SpriteBatch` ép tấm ảnh vẽ vừa khít khung kích thước $2 \times \text{radius}$ (hoặc theo tỉ lệ `drawWidth`, `drawHeight` khai báo trong `items.json`). Nhờ đó, thay cục vàng Vegas $128 \times 128$ bằng cục vàng 16-bit $32 \times 32$ sẽ giữ nguyên 100% quỹ đạo kéo và độ khó màn chơi.

### 5.2. Định danh tài nguyên theo tên logic & Chia thư mục Theme
* Code trong `gameplay/` và `screens/` **chỉ gọi tài nguyên qua khóa logic (Semantic Asset Key)** như `gold_small`, `gold_medium`, `gold_large`, `rock_small`, `rock_large`, `diamond`, `mystery_bag`, `tnt_barrel`, `mouse_walk`, `miner_idle`, `miner_pull`, `hook_claw`, `bg_mine`, `sfx_gold`, `sfx_explosion`...
* Cấu trúc thư mục tài nguyên:
  ```text
  assets/
  ├── data/                  // items.json, hook_config.json, shop.json, levels/*.json
  ├── maps/                  // file .tmx tạo bằng Tiled (Chặng 3)
  ├── fonts/                 // font .ttf hỗ trợ Tiếng Việt (nạp qua gdx-freetype)
  └── themes/
      ├── vegas/             // [MẶC ĐỊNH] Ảnh & âm thanh bóc từ Gold_Miner_Vegas_Source
      │   ├── theme.json     // Khai báo filter: "LINEAR", danh sách mapping file/atlas
      │   ├── atlas/         // Hoặc thư mục sprites/ nếu chưa đóng gói atlas
      │   └── audio/         // sfx & bgm bóc từ bản Vegas
      └── pixel16/           // [DỰ PHÒNG] Để sau này thả bộ Pixel Art 16-bit vào là đổi được ngay
          └── theme.json     // Khai báo filter: "NEAREST"
  ```

### 5.3. Bộ lọc hình ảnh (TextureFilter) theo từng Theme
* **`themes/vegas/` (ảnh mượt từ Flash):** Bật `TextureFilter.Linear` (kèm mipmap hoặc linear mịn) để ảnh độ phân giải cao hiển thị trơn tru, không bị răng cưa trên cửa sổ `960 x 540` hay toàn màn hình.
* **`themes/pixel16/` (ảnh pixel 16-bit sau này):** Bật `TextureFilter.Nearest` để từng ô pixel vuông vức, sắc cạnh không bị nhòe.
* **Tiếng Việt sắc nét:** Dùng `gdx-freetype` nạp file font `.ttf` có dấu tiếng Việt để hiển thị chữ `Tiền Mỏ`, `Bầu Cua`, `Tôm`, `Hươu`, `Thuốc nổ` rõ đẹp.

---

## PHẦN 6. KIẾN TRÚC PHẦN MỀM & 6 LUẬT SẮT LIBGDX

### 6.1. Cấu trúc gói trong `core/src/main/java/com/minhtriet79/goldminer`

*(Chỉ tạo từng gói khi bước vào đúng Chặng sử dụng gói đó.)*

```text
com.minhtriet79.goldminer
├── GoldMinerGame.java         // Lớp gốc (extends Game), giữ SpriteBatch, ThemeAssetManager, GameServices
├── app/
│   ├── ModeRegistry.java      // Đăng ký các chế độ chơi (GameMode)
│   └── GameServices.java      // Gom WalletService, SaveRepository, PlayerProgress, AudioService
├── domain/                    // [JAVA THUẦN 100% — CẤM import com.badlogic.gdx.*]
│   ├── wallet/                // WalletService, LocalWalletService, WalletListener (TIỀN CHÍNH THỨC)
│   ├── run/                   // RunSession, RunInventory (TIỀN MỎ '$' & đồ hỗ trợ trong lượt)
│   ├── settlement/            // SettlementCalculator, SettlementSummary (Quy đổi lượt -> Tiền Chính Thức)
│   ├── level/                 // LevelData, LevelItemData, HazardData, LevelGenerator, LevelValidator
│   ├── shop/                  // ShopItem, ShopService (mua bằng Tiền Mỏ '$', hỗ trợ công thức giá Vegas)
│   ├── casino/                // CasinoGame, BaucuaGame, CasinoResult, BetLimits
│   ├── progress/              // PlayerProgress (màn đã mở khóa, vé Arcade, skin/theme đang chọn, kỷ lục)
│   └── rng/                   // RandomSource, SeededRandom, SecureRandomSource
├── gameplay/                  // [libGDX — Vòng lặp & hiển thị cào vàng]
│   ├── hook/                  // Hook, HookState, HookConfig (thông số chuẩn Vegas)
│   ├── world/                 // MineWorld, MineItem, ItemType, MouseHazard, ExplosionEffect
│   └── collision/             // CollisionHelper (Circle / Intersector, KHÔNG Box2D)
├── screens/                   // MenuScreen, LevelSelectScreen, PlayScreen, ShopScreen, SettlementScreen, Play2PScreen, CasinoScreen, ArcadeScreen
├── arcade/                    // MiniGame, MiniGameCallback, MiniGameResult, ClawMachineScreen
├── persistence/               // SaveRepository, JsonSaveRepository, SaveData (có saveVersion)
├── assets/                    // Assets, ThemeConfig, AssetKey (bọc AssetManager, đọc theo themes/{activeTheme}/)
└── ui/                        // UiSkinFactory, HudBar
```

### 6.2. 6 Luật Sắt Bắt Buộc Khi Viết Code libGDX

1. **Zero Allocation trong Game Loop:** Cấm khởi tạo object mới (`new Vector2()`, `new Circle()`, `new ArrayList()`) bên trong `update(float dt)` hoặc `render(float dt)` để tránh Garbage Collector làm khựng game. Khai báo sẵn biến `final` ở cấp lớp và tái sử dụng bằng `.set(...)`.
2. **Delta Time chuẩn hóa:** Mọi chuyển động và đồng hồ đếm ngược nhân với `dt` (giây). Luôn kẹp `float dt = Math.min(rawDt, 0.05f)` ở đầu `render()`.
3. **Giải phóng bộ nhớ thủ công (`dispose()`):** Mọi tài nguyên OpenGL/Native (`SpriteBatch`, `ShapeRenderer`, `Texture`, `TextureAtlas`, `BitmapFont`, `Skin`, `Sound`, `Music`) phải gọi `.dispose()` khi đóng `Screen`, đổi Theme hoặc thoát game.
4. **Data-Driven:** Toàn bộ số liệu bào từ `Gold_Miner_Vegas_Source` (vật thể, tốc độ ngoàm, giá shop, công thức quy đổi, màn chơi) nằm trong `assets/data/*.json` hoặc `.tmx`, không hard-code cứng trong logic.
5. **Decouple Logic Size from Texture Size:** Mọi tính toán va chạm và kích thước vẽ lấy từ thông số logic (`radius`, `drawWidth`, `drawHeight`), tuyệt đối không phụ thuộc kích thước pixel thật của file ảnh để đảm bảo đổi Theme `vegas` $\leftrightarrow$ `pixel16` không làm lệch gameplay.
6. **TDD cho `domain/`:** Mọi lớp trong `domain/` đều có JUnit 5 test tương ứng trong `core/src/test/java/com/minhtriet79/goldminer/domain/...`.

---

## PHẦN 7. KẾ HOẠCH PHÁT TRIỂN CHI TIẾT TỪNG CHẶNG (ROADMAP)

*Thực hiện tuần tự từng bước con. Xong bước nào kiểm chứng đúng DoD rồi mới đánh dấu `[x]`.*

### CHẶNG 0 — BÀO SOURCE & TÀI NGUYÊN VEGAS + DỰNG KHUNG DỰ ÁN (1–2 ngày)

* [x] **Bước 0.0 (ƯU TIÊN SỐ 1 — CHỈ ĐỌC & PHÂN TÍCH, CHƯA CODE JAVA):**
  * AI Agent truy cập `C:\Users\Minh Triet\Desktop\MAVIS-Platform\Gold_Miner_Vegas_Source`, phân tích toàn bộ mã nguồn và kiểm kê toàn bộ hình ảnh/âm thanh theo đủ **9 hạng mục ở Phần 3.2**.
  * Xuất báo cáo chi tiết vào file **`VEGAS_EXTRACTED_SPECS.md`** tại thư mục gốc dự án (bao gồm bảng thông số đã quy đổi sang `dt`, tọa độ quy đổi và danh sách tài nguyên ảnh/âm thanh sẵn có).
  * **Dừng lại chờ người dùng kiểm tra và duyệt `VEGAS_EXTRACTED_SPECS.md`.**
* [x] **Bước 0.1 — Cắm JUnit 5 (và `gdx-freetype` + `ShapeDrawer` nếu cần) vào `core/build.gradle`:**
  * Cấu hình JUnit 5 + `useJUnitPlatform()`, viết 1 test mẫu xác nhận `./gradlew core:test` chạy xanh.
* [x] **Bước 0.2 — Cấu hình cửa sổ PC chuẩn:**
  * Chỉnh `Lwjgl3Launcher.java`: tiêu đề `"Gold Miner"`, `useVsync(true)`, `setForegroundFPS(60)`, `setWindowedMode(960, 540)` ($2\times$ của `480 x 270`).
* [x] **Bước 0.3 — Chuẩn hóa `assets/themes/vegas/` & Dựng khung `GoldMinerGame` + `PlayScreen`:**
  * Bóc/chuyển đổi các sprite và âm thanh cần thiết của bản Vegas vào `assets/themes/vegas/` kèm `theme.json` (`filter: LINEAR`).
  * Tạo `Assets` đọc theo `ThemeConfig`, dựng `PlayScreen` với `FitViewport(480f, 270f, camera)` vẽ thử nền mỏ + bác thợ mỏ/cục vàng từ `themes/vegas/`, đảm bảo `dispose()` sạch sẽ.
* **DoD Chặng 0:** Có đầy đủ `VEGAS_EXTRACTED_SPECS.md` đã được duyệt; chạy `./gradlew lwjgl3:run` mở cửa sổ `960 x 540` hiển thị ảnh từ `themes/vegas/` mượt mà không méo khi kéo giãn; `./gradlew core:test` xanh.

### CHẶNG 1 — LÕI CÀO VÀNG CHUẨN "GAME FEEL" & HÌNH ẢNH VEGAS (5–7 ngày)

* [x] **Bước 1.1 — Tạo hệ dữ liệu vật thể từ thông số Vegas (`assets/data/items.json`, `hook_config.json`):**
  * Nạp toàn bộ vật thể, độ nặng, giá trị, `radius` va chạm và `assetKey` từ JSON đã bào ở Bước 0.0.
* [x] **Bước 1.2 — Lớp `Hook` 3 trạng thái chuẩn toán học Vegas (Zero-Allocation):**
  * `SWINGING`, `EXTENDING`, `RETRACTING`, dùng đúng công thức tốc độ góc & lực kéo theo độ nặng quy đổi sang `dt`.
* [x] **Bước 1.3 — Va chạm, TNT nổ lan, Túi bí ẩn đúng xác suất & Chuột chạy:**
  * Cài đặt đầy đủ phản ứng vật thể, chuỗi nổ TNT, xác suất Túi bí ẩn và hiệu ứng Bình sức mạnh / Cỏ 4 lá.
* [x] **Bước 1.4 — Vòng lặp 1 màn hoàn chỉnh kèm Hình ảnh & Âm thanh Vegas:**
  * Đồng hồ đếm ngược, Tiền Mỏ (`$`), mục tiêu (`Goal`), ném thuốc nổ phá vật đang kéo, gắn hoạt ảnh bác thợ mỏ + âm thanh kéo vàng/nổ TNT/đếm ngược 10 giây cuối từ `assets/themes/vegas/`, thắng/thua màn, bấm `R` chơi lại.
* **DoD Chặng 1:** Thả ngoàm và kéo vàng/đá có cả cảm giác tay lẫn hình ảnh + âm thanh y như bản Vegas xưa; tắt sprite bật debug hitbox vẫn thấy va chạm khớp `radius`; không giật lag, không crash.

### CHẶNG 2 — KINH TẾ 2 TẦNG (PURE JAVA + TDD) & CHUỖI MÀN CHƠI (1–2 tuần)

* [ ] **Bước 2.1 — Tầng `domain/` thuần Java + JUnit 5:**
  * `RunSession` & `RunInventory` (Tiền Mỏ `$` + đồ tiêu hao trong lượt).
  * `WalletService` / `LocalWalletService` (Tiền Chính Thức `Xu`).
  * `SettlementCalculator` (tổng kết lượt khi thua/thoát/xong $\rightarrow$ cộng Tiền Chính Thức đúng 1 lần).
  * `ShopService` (mua bằng Tiền Mỏ `$`, hỗ trợ công thức giá Vegas đọc từ `shop.json`).
  * `PlayerProgress` & `JsonSaveRepository` (`saveVersion = 1`).
* [ ] **Bước 2.2 — Nối `ShopScreen`, `SettlementScreen` & 3–5 màn JSON đầu tiên:**
  * Qua màn vào Shop dùng Tiền Mỏ `$` mua đồ $\rightarrow$ bấm `[NEXT]` sang màn kế hoặc `[THOÁT]` sang `SettlementScreen` nhận Tiền Chính Thức và lưu file save.
* **DoD Chặng 2:** Chơi thông suốt nhiều màn có shop & âm thanh Vegas; tách bạch rõ Tiền Mỏ `$` và Tiền Chính Thức `Xu`; tắt game mở lại còn nguyên tiền & tiến độ; 100% JUnit test `domain/` xanh.

### CHẶNG 3 — MENU 4 NÚT & ĐỦ 4 KHU CHƠI (2–3 tuần)

* [ ] **Gói 3A — `ModeRegistry`, `MenuScreen` & Đủ 2 nhánh 1 Người:**
  * Menu 4 nút (`1 PLAYER`, `2 PLAYERS`, `CASINO`, `ARCADE`) + hiển thị Tiền Chính Thức góc trên phải.
  * Dùng **Tiled** tạo 10–15 màn cố định (`.tmx`) nạp qua `TmxMapLoader` + `LevelSelectScreen` mở khóa tuần tự (KHÔNG sao).
  * Viết `LevelGenerator` + `LevelValidator` trong `domain/level/` + JUnit test 10.000 seed cho nhánh Màn Ngẫu Nhiên (Vô tận & Chuỗi 3 màn).
* [ ] **Gói 3B — Chế độ 2 Người Offline (`2 PLAYERS`):**
  * 2 bộ `Hook` chung 1 mỏ, 2 bộ phím độc lập, tranh vật trực tiếp, KHÔNG cộng Tiền Chính Thức.
* [ ] **Gói 3C — Khu Casino (`CASINO` — Bầu Cua Tôm Cá):**
  * Viết `CasinoGame` + `BaucuaGame` trong `domain/casino/` + JUnit test mô phỏng 1.000.000 ván kiểm tra house edge ($\approx 7.87\%$).
  * Dựng `CasinoScreen` cược trực tiếp bằng Tiền Chính Thức từ `WalletService`.
* [ ] **Gói 3D — Khu Game Thùng (`ARCADE` — Máy Gắp Thú):**
  * Viết `MiniGame` + `ClawMachineScreen` tái sử dụng cơ chế ngoàm, thưởng **Vé** đổi đồ trang trí/skin.
* **DoD Chặng 3:** Cả 4 nút ở Menu đều vào chơi được trọn vẹn, dữ liệu Tiền Chính Thức và Vé đồng bộ xuyên suốt.

### CHẶNG 4 — ĐÁNH BÓNG "GAME FEEL", THỬ NGHIỆM ĐỔI THEME 16-BIT & ĐÓNG GÓI (1 tuần)

* [ ] **Bước 4.1 — Tăng độ "sướng tay" (Juice):** Rung màn hình khi nổ TNT (`ScreenShake`), hạt lấp lánh (`ParticleEffect`) khi kéo kim cương, chữ bay lên (`+$500`), hoàn thiện giao diện `Scene2D`.
* [ ] **Bước 4.2 — Kiểm chứng cơ chế đổi Theme (Tùy chọn):** Thêm nút chuyển đổi `Theme` trong Cài đặt (`vegas` $\leftrightarrow$ `pixel16`) để khi nào em muốn tự vẽ hoặc thay đồ họa 16-bit thì chỉ việc thả ảnh vào `assets/themes/pixel16/` là chạy ngay.
* [ ] **Bước 4.3 — Đóng gói gửi bạn bè chơi chung:** Dùng plugin Gradle **Construo** (hoặc `jpackage`) xuất thành thư mục/file `.exe` kèm sẵn JVM để bạn bè tải về bấm là chơi ngay không cần cài Java.

---

## PHẦN 8. QUY TRÌNH PHỐI HỢP VỚI ANTIGRAVITY

Mỗi phiên làm việc mới, AI Agent tuân thủ đúng 4 bước:
1. Kiểm tra dấu `[x]` ở **Phần 7** (và kiểm tra xem `VEGAS_EXTRACTED_SPECS.md` đã được tạo và duyệt chưa).
2. Trình bày **Mini-Plan** cho duy nhất bước sắp làm và chờ người dùng xác nhận.
3. Viết code chuẩn 6 Luật Sắt ở **Phần 6.2**, luôn kèm JUnit 5 nếu sửa `domain/`.
4. Hướng dẫn người dùng chạy kiểm chứng DoD rồi đánh dấu `[x]` trước khi qua bước tiếp theo.