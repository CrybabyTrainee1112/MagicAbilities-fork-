# Báo cáo thiết kế: Hệ thống Boss (Boss API + GOAP AI)
**Project:** MagicAbilitiesfork
**Phạm vi:** Module `magicabilitiesfork-boss` (tách riêng khỏi core)
**Trạng thái:** Thiết kế đã chốt, chưa bắt đầu code phần AIAPI/PhaseAPI/ThreatAPI/DamageAPI/EventAPI. Đã có prototype `SkillAPI` đời đầu (xem mục 8) cần refactor lại theo thiết kế GOAP.

---

## 1. Mục tiêu

Xây dựng hệ thống boss cho plugin, tách biệt hoàn toàn khỏi hệ thống Power (vốn thiết kế cho PvP 1v1, gắn chặt vào `Player`). Boss cần:
- AI ra quyết định "thông minh" — tự tìm chuỗi skill hợp lý theo tình huống thay vì chỉ random có trọng số hoặc combo script cứng.
- Chạy được trên server sống (ngân sách 1 tick = 50ms cho **toàn server**, không riêng boss) — mọi quyết định thiết kế đều phải cân nhắc chi phí này trước.
- Tái dùng được VFX/skill logic đã có (VD `ThunderGodPower`) mà không đụng/rủi ro code Power hiện tại.
- Config (bosses.yml...) và code đều tách riêng khỏi core, đúng yêu cầu "chỉ làm nội dung liên quan boss".

---

## 2. Phạm vi tách project

**Quyết định:** Module Maven riêng trong cùng repo (`magicabilitiesfork-boss`), build chung ra 1 file `.jar` duy nhất — không tách deploy thành 2 plugin.

### 2.1 Cấu trúc Maven (dựa trên `pom.xml` hiện tại: groupId `net.trduc`, artifactId `MagicAbilities_fork`, packaging `jar`, Java 1.8, spigot-api `1.21.10-R0.1-SNAPSHOT`, đã có `maven-shade-plugin`)

```
MagicAbilities_fork/                  (parent pom, packaging = pom)
├── pom.xml                           (modules: core, boss; properties/dependencyManagement dùng chung)
├── magicabilitiesfork-core/          (project hiện tại, ~98 file Java, plugin.yml, config.yml...)
│   └── pom.xml                       (giữ nguyên deps cũ + maven-shade-plugin + thêm 1 dependency compile-scope tới magicabilitiesfork-boss)
└── magicabilitiesfork-boss/          (module mới, toàn bộ code trong báo cáo này)
    └── pom.xml                       (chỉ phụ thuộc spigot-api, provided)
```

### 2.2 Chiều phụ thuộc — CHỈ 1 CHIỀU: `core → boss`

Đây là ràng buộc kỹ thuật quan trọng nhất, bắt buộc phải giữ nghiêm suốt quá trình code:

- **Boss module KHÔNG được import bất cứ gì từ package `net.trduc.magicabilitiesfork.*` của core** (không dùng `MagicAbilitiesfork.particleApi`, không dùng `ParticleApi`, `DisplayApi`, `CooldownApi`, `DbManager`...). Nếu boss phụ thuộc ngược vào core để dùng các API đó, trong khi core lại phải phụ thuộc boss để shade class vào jar cuối → **circular dependency, Maven không build được.**
- Mọi VFX trong boss module dùng thẳng Bukkit API gốc (`Particle.DUST` + `Particle.DustOptions` thay cho `ParticleApi.spawnColoredParticles`, `world.spawnParticle` thay `particleApi.spawnParticles`...).
- Cooldown trong boss module tự quản lý nội bộ (không dùng `CooldownApi` của core — API đó keyed theo `Player`, boss không phải Player).
- Core module, khi cần gọi vào boss (VD lệnh `/spawnboss`, hoặc khi 1 Power nào đó cần trigger boss), gọi qua các entrypoint public của boss module (`BossManager`, `BossFactory`) như 1 thư viện bình thường.

### 2.3 Config riêng

Boss module có `src/main/resources/bosses.yml` của riêng nó (không đụng `config.yml`/`cooldowns.yml` của core). File này sẽ chứa:
- Cấu hình từng loại boss (stat, skill pool, phase threshold — theo đúng pattern `cfg.getDouble(path, default)` đã dùng cho Power, load trong constructor).
- Cấu hình throttle của AI (`sensor-interval-ticks`, `replan-cooldown-ms` — xem mục 6).

---

## 3. Nền tảng thực thể Boss

**Quyết định:** Boss dựa trên **Mob thật** (VD `Zombie`/`Evoker` custom, resize qua `Attribute.SCALE` — đúng API 1.21+ đã ghi nhận trong codebase), **không** dùng Display Entity tự dựng như VFX của Power.

Lý do: dùng Mob thật cho phép tái dùng **Paper Mob Goal API** (`Mob#goals`, `Mob#getPathfinder()`) có sẵn cho việc di chuyển/pathfinding, khỏi phải tự viết. Đánh đổi: ít control hình ảnh hơn Display Entity, nhưng VFX phụ trợ (particle, ánh sáng...) vẫn overlay được bình thường qua Bukkit Particle API.

**Đã cân nhắc và loại:**
- **NMS/Mojang-mapped custom entity** — cho phép override AI ở tầng nội bộ Minecraft (`customServerAiStep`, `registerGoals` gốc), nhưng dù đi hướng này thì `Boss`/`Skill`/`Goal`/`Planner` vẫn chỉ cầm 1 `org.bukkit.entity.Mob` như bình thường (Bukkit luôn bọc lại thành `CraftXxx` bất kể subclass NMS nào bên trong) — nghĩa là NMS không mở khoá khả năng kiến trúc mới nào cho thiết kế hiện tại, chỉ tốn thêm effort + rủi ro vỡ theo từng version Minecraft (mapping đổi mỗi bản, cần build setup Paperweight riêng). Không có nhu cầu nào trong thiết kế đòi hỏi tới mức này → loại.
- **Fake NPC qua packet (ProtocolLib)** — cho phép hiển thị đúng 100% skin/model player thật (VD Steve full-body), nhưng đánh đổi mất hẳn `Mob` thật (không còn free pathfinding từ Paper Goal API, phải tự viết di chuyển bằng packet), cần thêm dependency mới (phá nguyên tắc "boss module chỉ phụ thuộc spigot-api" ở mục 2.2), và đổi luôn kiểu dữ liệu `Skill`/`Goal`/`Planner` đang nhận (`Mob`/`LivingEntity`) → ảnh hưởng lan ra toàn bộ thiết kế. Chỉ nên cân nhắc lại nếu sau này có yêu cầu cụ thể "boss phải giống hệt player thật kể cả animation tay chân".

**Cách boss "giống nhân vật cụ thể" (VD Steve) mà không đổi kiến trúc:** gắn **player head có texture tùy ý** (dùng `skullcreator`, đã có sẵn trong `pom.xml` của core) làm helmet cho Mob, kèm leather armor nhuộm màu gợi outfit — chỉ là 1 bước equipment trong `BossFactory.spawn()`, không đụng gì tới `Skill`/`Goal`/`Planner`/`Executor`. Giới hạn: model thân/tay/chân vẫn theo hình Mob nền (to hơn, animation khác player thật), chỉ riêng mặt/đầu đúng skin 100%.

---

## 4. Kiến trúc tổng thể

```
BossAPI
│
├── Boss                  — instance 1 con boss đang sống (state: hp, phase, skill set, Brain)
├── BossManager            — theo dõi các Boss instance đang sống trong world (spawn/despawn/tick/lookup UUID)
├── BossFactory             — nhận BossType từ Registry, dựng 1 Boss instance thật, build Graph (1 lần/loại), giao cho Manager
├── BossRegistry            — catalog tĩnh các LOẠI boss đã đăng ký (map BossType → config/class), giống Power.getPowerFromPowerType
│
├── SkillAPI
│      ├── Skill            — 1 action GOAP: cost, precondition, effect, optional TargetKey, execute()
│      ├── SkillContext     — gom boss/target/phase/worldState thành 1 object truyền qua các layer
│      └── SkillExecutor    — cooldown bookkeeping + cast thật (kế thừa tinh thần BossSkillApi cũ)
│
├── AIAPI                   — chi tiết ở mục 5
│      ├── Sensor
│      ├── Memory
│      ├── Decision
│      ├── Planner
│      ├── Behavior
│      ├── Goal
│      └── Executor
│      (+ Brain — vòng lặp chính, 1 Brain/Boss instance)
│
├── PhaseAPI                — ngưỡng máu → đổi phase → đổi Goal pool / Skill pool khả dụng
├── ThreatAPI                — bảng threat (player→aggro), có cơ chế decay/dọn rác (mục 5.9)
├── DamageAPI                — damage boss gây ra/nhận, resistance theo phase, nối vào pattern DealDamageExecute/DamagedExecute của core (nhưng đứng độc lập trong boss module)
└── EventAPI                — Bukkit custom event (BossSpawnEvent, BossPhaseChangeEvent, BossDeathEvent...), core lắng nghe qua listener bình thường
```

---

## 5. Chi tiết AIAPI — thuật toán GOAP

### 5.1 WorldState / WorldKey / TargetKey

Không dùng `Map<String,Boolean>` phẳng. Thay bằng:
- **WorldKey**: key tham chiếu 1 giá trị **số** (int/double) trong thế giới, có phép so sánh (`==`, `>=`, `<`...). VD: `targetDistance < 3`, `nearbyPlayerCount >= 2`.
- **TargetKey**: key tham chiếu 1 **vị trí** (Location/Vector3), không phải giá trị số. VD: `TargetKey.NEAREST_THREAT`. Nếu 1 Skill khai báo cần `TargetKey`, Executor **tự động điều hướng Mob tới đó** (qua Paper Mob Goal API) trước khi cast — khỏi phải viết action "di chuyển tới X" riêng cho từng skill.
- `WorldState` bọc 2 loại key trên thành 1 class riêng (không để `Skill`/`Goal`/`Planner` thao tác trực tiếp trên `Map` thô) — để sau này đổi cách lưu trữ (VD bitmask nếu cần tối ưu thêm) mà không phải sửa lại các layer trên.

### 5.2 Goal

Mỗi `Goal` có:
- Danh sách điều kiện đích trên `WorldKey` (VD `targetControlled == true`, `areaCleared == true`).
- Method **`assessPriority(WorldState) → float (0.0–1.0)`** — Goal tự chấm điểm ưu tiên của chính nó dựa theo world-state hiện tại. VD Goal "Kiểm soát mục tiêu" tự trả priority cao khi phát hiện `targetClustered=true`; Goal "Bảo toàn" tự trả priority cao khi `bossHpLow=true`.
- Goal pool khả dụng thay đổi theo Phase (PhaseAPI quyết định Goal nào được xét tới ở phase nào), nhưng **priority cụ thể do chính Goal tự tính**, không hardcode if-else theo % máu.

### 5.3 Skill (Action)

Mở rộng so với `BossSkill` cũ (xem mục 8):
- `cost` (double) — dùng cho Planner, tách biệt khỏi `baseWeight` cũ (nếu giữ song song cho fallback random).
- `preconditions` / `effects` trên `WorldKey` — có thể là hàm động theo state hiện tại (lấy cảm hứng từ `Action.Simulate(current State)` của kelindar/goap) thay vì map tĩnh cố định, để 1 skill có outcome khác nhau tùy tình huống (VD `chainJudgment` gây damage khác nhau tùy số player đứng gần).
- `targetKey` optional — nếu có, Executor tự lo di chuyển trước khi cast (xem 5.1).

### 5.4 GraphBuilder

Build **graph action 1 lần duy nhất khi 1 loại boss được đăng ký** ở `BossFactory`/`BossRegistry` — nối các Skill với nhau qua precondition/effect trùng khớp. **Không rebuild graph mỗi lần cần plan** — đây là điểm tối ưu quan trọng nhất so với cách làm "build graph mỗi lần" của ví dụ đơn giản (jeffreypopek.dev), theo đúng cách package `crashkonijn/GOAP` (Unity) làm.

### 5.5 Planner

- Search **backward-chaining** từ Goal (không forward từ state hiện tại) trên graph đã build sẵn.
- Dùng **A\* có heuristic** (đếm số điều kiện goal chưa thỏa — lấy từ kelindar/goap), không phải Dijkstra thuần, để cắt bớt nhánh cần duyệt.
- **Chạy async** (Bukkit scheduler async task) — vì A* thuần là tính toán trên dữ liệu (Map facts + list action), không đụng Bukkit API, tách khỏi main thread hoàn toàn an toàn. Chỉ khi có plan xong mới nhảy về main thread để Executor cast.
- **Chỉ replan khi cần** (xem 5.6), không chạy mỗi tick.

### 5.6 Sensor + Memory (khi nào replan)

- `Sensor` có 2 scope: `GLOBAL` (dùng chung mọi boss, VD thời gian ngày/đêm) và `LOCAL` (riêng 1 boss instance, VD player gần nhất/threat cao nhất của boss đó).
- Throttle Sensor riêng theo loại (không chạy 20Hz) — mỗi 4–10 tick (200–500ms) là đủ.
- `Memory` giữ facts hiện tại + lịch sử cast — là nơi Sensor ghi vào, Planner/Goal đọc ra.
- **Dirty-flag**: khi Memory phát hiện 1 fact liên quan tới Goal hiện tại đổi (VD target thoát range, player rời cụm), bật flag → Decision hủy plan hiện tại (`cancelPlan`, theo pattern excaliburjs.com) → gọi lại Planner ngay, không đợi hết queue action hiện tại.

### 5.7 Decision + Behavior

- `Decision` mỗi tick (throttled): duyệt Goal khả dụng theo Phase hiện tại → gọi `assessPriority` từng Goal → chọn Goal điểm cao nhất → nếu Goal đổi hoặc plan hiện tại invalid → yêu cầu Planner replan.
- `Behavior` — routine viết tay cho khoảnh khắc cố định (cutscene mở màn, ultimate enrage) — **không qua Planner**. Decision có thể gán priority cực cao tạm thời để Behavior "chen ngang" GOAP khi cần (hybrid, không phải full-GOAP thuần).

### 5.8 Executor + Brain

- `Executor`: pop skill kế tiếp trong plan (Queue<Skill>) → nếu có `TargetKey`, điều hướng Mob trước → gọi `SkillExecutor` cast thật → cooldown → cập nhật `WorldState` theo effect.
- `Brain`: vòng lặp chính 1 boss instance = Sensor → Memory → Decision → (Planner|Behavior) → Executor. 1 Brain/Boss.

### 5.9 ThreatAPI — lưu ý riêng

Threat table (player→aggro) phải **tự decay** theo chu kỳ riêng (không phải mỗi tick) và **tự xoá** player rời range/logout — tránh memory leak khi server chạy nhiều ngày qua nhiều lượt boss spawn/despawn.

---

## 6. Nguyên tắc tối ưu hiệu năng (bắt buộc áp dụng khi code)

1. **Sensor/Planner không chạy mỗi tick.** Sensor throttle 4–10 tick; Planner chỉ chạy khi dirty-flag bật (goal đổi/phase đổi/plan fail), không phải vòng lặp liên tục.
2. **Planner async, Executor luôn sync.** A* không đụng Bukkit API → chạy async task, xong mới quay lại main thread để cast skill thật.
3. **WorldState dùng `Map` trước, đừng tối ưu sớm.** Với quy mô hiện tại (~10-20 fact, ~5-10 skill/boss) là đủ nhanh; bitmask (kiểu F.E.A.R./MythicMobs) là over-engineering ở giai đoạn này — nhưng bọc `WorldState` thành class riêng để đổi implementation sau này không phải sửa Skill/Goal/Planner.
4. **Tránh GC churn ở phần chạy đều đặn.** VD `Sensor` không được `new Random()` mỗi lần quét — dùng 1 instance dùng chung (khác với các Power hiện tại, chấp nhận được vì Power gọi theo cooldown, tần suất thấp hơn nhiều).
5. **ThreatAPI cần dọn rác** — xem 5.9.
6. **Mọi số throttle nằm trong `bosses.yml`**, không hardcode — để tinh chỉnh sau khi test thật trên server sống.

---

## 7. Nguồn tham khảo đã dùng và điểm rút ra

| Nguồn | Đóng góp chính vào thiết kế cuối |
|---|---|
| jeffreypopek.dev/goap | Xác nhận mô hình GOAP kinh điển (FEAR): world state, action (precondition/effect/cost/duration), planner build-graph-rồi-backtrack chọn cost thấp nhất. Gợi ý bổ sung field `cost` cho Skill. |
| goap.crashkonijn.com (Unity package) | Tách `GraphBuilder` (build 1 lần/AgentType) khỏi `Resolver` (search trên graph có sẵn) — tối ưu quan trọng nhất. Backward-chaining từ Goal. Tách `WorldKey` (giá trị số + so sánh) và `TargetKey` (vị trí) — giải quyết bài toán "di chuyển tới mục tiêu" mà không cần action riêng. Sensor có scope Global/Local. |
| excaliburjs.com/blog/goal-oriented-action-planning | Goal tự đánh giá priority (`assessPriority`) thay vì hardcode theo phase. Agent phải hỗ trợ `cancelPlan()` khi môi trường đổi giữa chừng. |
| github.com/kelindar/goap | A* có heuristic (đếm điều kiện goal chưa thỏa) thay vì Dijkstra thuần — nhanh hơn khi action nhiều. Precondition/effect có thể tính động theo state hiện tại (`Simulate`) thay vì map tĩnh cố định. |

Bốn nguồn hội tụ về cùng bộ nguyên lý, không mâu thuẫn nhau — thiết kế ở mục 5 là hợp nhất của cả 4.

---

## 8. Prototype đã có sẵn — cần refactor

Trước khi chốt hướng GOAP, đã viết 1 bản `SkillAPI` đời đầu (weighted-random + combo script cứng) nằm ở:
```
net.trduc.magicabilitiesfork.bosses.skills.BossSkill
net.trduc.magicabilitiesfork.bosses.skills.ComboChain
net.trduc.magicabilitiesfork.bosses.skills.BossSkillApi
net.trduc.magicabilitiesfork.bosses.skills.custom.StormbringerSkills   (demo: tái dùng skill ThunderGod + skill mới "Chain Judgment")
```
**Trạng thái:** Đã parse OK (javalang), đã tách đúng module `magicabilitiesfork-boss` (không phụ thuộc core). **Chưa refactor theo GOAP** — cần:
- Đổi `BossSkill` → `Skill`, bổ sung `cost`/`preconditions`/`effects`/`targetKey` như mục 5.3.
- `BossSkillApi` (weighted-random + comboBias) có thể giữ lại làm **fallback đơn giản** hoặc chuyển hẳn logic combo-bias thành 1 dạng `Behavior` script cứng (combo "Storm Judgment" cũ hợp làm ví dụ Behavior hơn là Goal, vì nó là chuỗi cố định có chủ đích, không cần GOAP tự suy luận).
- `StormbringerSkills` là ví dụ dùng thật (Triple Strike/Storm Step/Thunder Cage tái dùng từ `ThunderGodPower`, đổi chữ ký `Player`→`LivingEntity`/`Mob`) — giữ nguyên tinh thần, nối lại vào `Skill` mới khi có.

---

## 9. Thứ tự triển khai đề xuất

1. `WorldKey` / `TargetKey` / `WorldState` (nền tảng mọi thứ khác phụ thuộc vào)
2. `Skill` (Action) — dùng WorldState mới, refactor từ `BossSkill` cũ
3. `Goal` — có `assessPriority`
4. `GraphBuilder`
5. `Planner` (A* + heuristic, async)
6. `Sensor` / `Memory` (Global/Local, throttle)
7. `Decision` / `Behavior` / `Executor` / `Brain` — ráp vòng lặp hoàn chỉnh
8. `Boss` / `BossManager` / `BossFactory` / `BossRegistry` — khung sống, spawn/despawn
9. `PhaseAPI` — nối Goal pool theo ngưỡng máu
10. `ThreatAPI` — kèm cơ chế decay/dọn rác (mục 5.9)
11. `DamageAPI` — resistance/phase-multiplier
12. `EventAPI` — Bukkit custom event cho core lắng nghe

---

## 10. Vấn đề / rủi ro chưa quyết định — cần bàn tiếp

- **Debug/observability:** GOAP khó debug hơn weighted-random ("vì sao boss chọn skill này" phải trace qua planner). Cần cân nhắc thêm command debug (VD `/boss debug <uuid>` in ra plan hiện tại, Goal đang active, world-state snapshot) — chưa thiết kế.
- **Số lượng boss chạy đồng thời:** thiết kế hiện tại giả định vài boss cùng lúc là tối đa. Nếu về sau cần hàng chục boss cùng lúc (world PvE lớn), `WorldState` dạng `Map` và Sensor per-instance có thể cần tối ưu lại (mục 6.3 đã dự phòng bằng cách bọc `WorldState` thành class riêng).
- **DamageAPI kết nối với core:** core có `DealDamageExecute`/`DamagedExecute` cho Power; boss module không được phụ thuộc core (mục 2.2) nên `DamageAPI` của boss phải độc lập hoàn toàn — cần rà lại xem có logic nào ở core (VD PowerTeam, damage-team-check) cần biết boss tồn tại không, và nếu có thì core lắng nghe qua `EventAPI` (một chiều core→boss vẫn giữ nguyên, core chỉ là listener).
- **Cấu hình Goal/Skill theo boss cụ thể:** hiện chưa quyết định Goal/Skill có thể định nghĩa hoàn toàn qua `bosses.yml` (data-driven, không cần sửa code Java để thêm boss mới) hay bắt buộc viết class Java cho mỗi Goal/Skill mới (như Power hiện tại). Ảnh hưởng lớn tới việc mở rộng about lâu dài — cần chốt trước khi viết `GraphBuilder`.
- **Test/tuning:** GOAP cost/heuristic cần chạy thử thật trên server để tinh chỉnh — không thể chỉ đọc code đoán được combo/hành vi có "cảm giác" đúng hay không.

---

## 11. Quy ước code cần tuân theo (đồng bộ với codebase hiện tại)

- Config: `cfg.getDouble("path", default)` / `cfg.getInt(...)` inline tại chỗ dùng, load trong `loadConfig()` gọi từ constructor (không dùng static field) — pattern `CultivatorPower` đang dùng.
- `BukkitRunnable`: gán biến trước, gọi `.runTaskTimer()`/`.runTaskLater()` trên dòng riêng (trả về `BukkitTask`, không phải `BukkitRunnable`).
- API 1.21+: dùng `Attribute.SCALE`/`Attribute.MAX_HEALTH` (không dùng `GENERIC_*` cũ).
- AoE (`nearbyLiving`/`getNearbyEntities`) cần loại trừ chính caster (`exclude` parameter) để tránh tự gây damage lên chính boss.
- `switch` có khai báo biến local trong `case` cần bọc `{ }` tránh scope collision.
- Import Projectile: `org.bukkit.entity.Projectile`, không phải `org.bukkit.projectiles.Projectile`.
- Mọi task chạy dài (Sensor loop, Brain tick loop...) phải được track và cancel khi boss bị despawn/remove — tương đương nguyên tắc `remove()` của Power.
- Validate cú pháp bằng `javalang` (Python) sau mỗi lần sửa nhiều file — `Metrics.java` là false-positive đã biết, bỏ qua.

---

*Báo cáo này là bản chốt thiết kế tính tới thời điểm hiện tại. Mục 10 là các câu hỏi mở cần trả lời trước/trong lúc code, không phải thiếu sót — cố ý để lại vì cần thêm ngữ cảnh thực tế (test trên server, quy mô nội dung) mới quyết được.*
