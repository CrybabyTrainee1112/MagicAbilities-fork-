# Boss System — Kiểm kê tổng thể (Audit)

> File này là bản kiểm kê đầy đủ trạng thái hệ thống Boss (GOAP-based AI) tính đến thời điểm hiện tại.
> Bổ sung cho `README.md` (tài liệu giới thiệu/quick-start) — file này tập trung vào **cái gì đã xong, cái gì đã sửa, cái gì còn thiếu**.

---

## 1. Kiến trúc hiện tại (34 → 42 file Java)

```
Boss/
├── ai/
│   ├── worldstate/   WorldKey, TargetKey, WorldState, Condition, Effect, SkillContext, WorldStateKeys
│   ├── skill/        Skill, SkillExecutor, AbstractSkill
│   ├── goal/         Goal, AbstractGoal                      (tầng chiến thuật - GOAP)
│   ├── planner/      ActionGraph, ActionGraphNode, GraphBuilder, PlanNode, Planner (A*)
│   ├── htn/          Task, CompoundTask, Method, PrimitiveTask (SkillTask/GoalTask),
│   │                 HTNPlanner, Strategy, AbstractStrategy    (tầng chiến lược - HTN, MỚI)
│   ├── sensor/       Sensor, SensorScope
│   ├── memory/       Memory
│   ├── decision/     Decision                                 (Behavior cũ đã bị xoá, xem mục 9)
│   └── executor/     Brain (Sensor→Decision→(Strategy/HTN | Goal/GOAP)→Executor)
├── core/             Boss, BossType, BossManager, BossFactory, BossRegistry
├── phase/            PhaseDefinition (nay mang cả Goal VÀ Strategy riêng theo từng phase)
├── threat/           ThreatTable
├── damage/           DamageAPI
├── event/            BossSpawnEvent, BossDeathEvent, BossPhaseChangeEvent (nay ĐÃ được fire thật)
├── README.md         Quick-start / kiến trúc HTN+GOAP hybrid
└── boss_.md          File này
```

**Trạng thái build:** toàn bộ 42 file parse sạch bằng `javalang` (không lỗi cú pháp) sau phiên làm
việc hiện tại (thêm 9 file `ai/htn/`, xoá `ai/decision/Behavior.java`, sửa 6 file hiện có). Không có
`pom.xml`/`build.gradle` trong gói được cung cấp nên chưa build thử bằng Maven thật — các thay đổi
được kiểm tra bằng đọc code + đối chiếu type signature ở từng call site (`grep -rn "new Brain("`,
`"new Boss("`, `"new BossType("`... chỉ có đúng 1 call site thật ở `BossFactory`, còn lại là ví dụ
trong `README.md` đã cập nhật theo).

---

## 2. Bug nghiêm trọng — ĐÃ SỬA ✅

| # | File | Vấn đề | Cách sửa |
|---|---|---|---|
| 1 | `ai/executor/Brain.java` | `requestPlan()` gọi `planner.findPlan(goal, Location)` trong khi `Planner.findPlan(Goal, WorldState)` — **sai kiểu dữ liệu, không compile được**. | Chụp snapshot `WorldState` (dùng copy-constructor có sẵn) **trên thread chính trước khi vào async**, truyền snapshot vào `findPlan`. Tránh luôn race-condition tiềm ẩn khi async task đọc `Memory` sống. |
| 2 | `ai/skill/SkillExecutor.java` | Cooldown hardcode `50ms` (= 1 tick) cho **mọi** skill, bỏ qua hoàn toàn `skill.getCooldownTicks()`. Builder có `.cooldownTicks(40)` cũng vô nghĩa. | Đổi map lưu trữ từ `skillId -> lastCastTime` sang `skillId -> thời điểm hết cooldown`, tính đúng bằng `cooldownTicks * 50ms`. `cleanup()` từ no-op → dọn entry hết hạn thật. |
| 3 | `ai/planner/GraphBuilder.java` + `ai/planner/Planner.java` | `canConnect()` luôn `return true` → `ActionGraph`/edges được build (O(n²)) nhưng `Planner.findPlan()` **không hề dùng**, chỉ lặp brute-force `graph.getSkills()`. Toàn bộ cấu trúc đồ thị là dead code. | **Hướng B (đã chọn):** `Condition`/`Effect` thêm `getRelevantKeys()`/`getModifiedKeys()` (default rỗng, không phá interface cũ) — các factory method (`equals`, `greaterThan`, `setValue`, `modifyValue`...) tự khai báo `WorldKey`. `GraphBuilder.canConnect()` so khớp thật: A nối B nếu `effect.getModifiedKeys()` của A giao `precondition.getRelevantKeys()` của B; nếu 1 bên không khai báo (skill viết tay bằng lambda thô) → giữ hành vi cũ (luôn nối) để không breaking. `Planner.findPlan()` giờ mở rộng theo `outgoingEdges` của skill cuối trong plan, fallback về full list ở node đầu tiên hoặc khi không có edge nào xác định được — không bao giờ bị kẹt tìm kiếm. |

**Kết quả kiểm tra:** cả 6 file bị đổi (`Brain`, `SkillExecutor`, `Condition`, `Effect`, `GraphBuilder`, `Planner`) đã parse sạch bằng `javalang`, và toàn bộ 34 file trong `Boss/` vẫn parse sạch sau khi áp fix (không phá code khác).

---

## 3. Chưa tích hợp vào plugin — 🔴 chưa làm

Grep toàn bộ codebase ngoài `Boss/` (`commands`, `events`, `guis`, `powers`, `data`, `misc`...) cho từ khóa `Boss.` → **0 kết quả**. Hệ thống Boss hiện là một khối hoàn toàn cô lập.

- [ ] `MagicAbilitiesfork.onEnable()` chưa khởi tạo `BossManager` / `BossFactory`.
- [ ] Chưa có `BukkitRunnable` gọi `bossManager.tickAll()` mỗi tick.
- [ ] Chưa có listener `EntityDamageByEntityEvent` → `bossManager.notifyBossDamaged()` (ThreatTable sẽ luôn rỗng trong thực tế).
- [ ] Chưa có listener bắt mob chết → gọi `Boss.die(killer)` + `unregisterBoss()`. (`Boss.die()` giờ tự fire `BossDeathEvent` bên trong nó — xem mục 9 — nhưng vẫn cần 1 listener `EntityDeathEvent` ở plugin gọi nó, hiện chưa có.)
- [x] ✅ ĐÃ SỬA — `BossSpawnEvent` được fire trong `BossFactory.spawn()`; `BossPhaseChangeEvent` được fire trong `Boss.transitionTo()` (tự động theo %máu); `BossDeathEvent` được fire trong `Boss.die()`. Cả 3 đều gọi `Bukkit.getPluginManager().callEvent(...)` thật — chỉ còn thiếu listener phía plugin core lắng nghe chúng (không phải lỗi của Boss module).
- [ ] Chưa có command nào (`/boss debug|list|threat`) — README tự nhận "cần implement".
- [ ] Chưa có `bosses.yml` / loader đọc boss type từ config — README tự nhận "sẽ được tạo sau". Khác hẳn pattern config-driven đang dùng cho `AssassinPower`, `CultivatorPower`.

---

## 4. Chưa có implementation cụ thể — 🔴 chưa làm

- [ ] **0 Skill cụ thể** — chỉ có `Skill`/`AbstractSkill` (interface + abstract). `ThunderStrikeSkill` trong README chỉ là ví dụ minh họa.
- [ ] **0 Goal cụ thể** — chỉ có `Goal`/`AbstractGoal`.
- [ ] **0 Sensor cụ thể** — `ThreatSensor`, `HealthSensor` chỉ tồn tại trong README, không có file thật.
- [x] ✅ ĐÃ CÓ — `Boss/bosses/demonlord/` — boss cụ thể đầu tiên, xem mục 10.
- [x] ✅ ĐÃ CÓ — `demon_lord` đăng ký được qua `DemonLordBossType.register()` (chưa có ai GỌI nó — vẫn cần plugin `onEnable`, xem mục 3/8).
- [ ] Vẫn chưa test được trên server thật (chưa có Maven/Gradle project để build, chưa có plugin lifecycle wiring) — chỉ mới verify bằng `javalang` (syntax) + đọc code đối chiếu type signature từng call site, KHÔNG phải build thật.

---

## 5. Lỗ hổng logic khác — 🟡 chưa sửa, cần bạn xác nhận hướng đi

| Vấn đề | Vị trí | Ghi chú |
|---|---|---|
| Không có navigation/pathfinding | `SkillExecutor.tryExecute()` | Javadoc `Skill.getTargetKey()` ghi *"Executor will navigate the boss to that location before casting"* nhưng không có code di chuyển nào — cast thẳng bất kể khoảng cách. **Vẫn chưa sửa** (ngoài phạm vi phiên này). |
| ~~Phase system không liên kết thật~~ ✅ ĐÃ SỬA | `Brain`/`PhaseDefinition` | `PhaseDefinition` giờ mang cả `availableGoals` VÀ `availableStrategies` riêng; `Brain.tick()` resolve theo `currentPhase` mỗi tick (`resolveGoalsForPhase`/`resolveStrategiesForPhase`), fallback về catalog đầy đủ của `BossType` nếu phase number không khớp `PhaseDefinition` nào. |
| ~~Không tự chuyển phase~~ ✅ ĐÃ SỬA | `Boss.checkPhaseTransition()` | Gọi sau mỗi `brain.tick()`, so `entity.getHealth()/Attribute.MAX_HEALTH` với ngưỡng các `PhaseDefinition` (sort ascending, chọn ngưỡng lớn nhất ≤ %máu hiện tại) → tự `transitionTo()`, fire `BossPhaseChangeEvent`, gọi `brain.cancelPlan()` để buộc re-plan theo goal/strategy pool mới, và broadcast `phaseTransitionMessage` cho player trong bán kính 40 block. |
| `DamageAPI` là dead code | Vẫn còn — không có `Skill` nào gọi `DamageAPI.dealDamage()`/`calculateDamage()` vì chưa có Skill cụ thể (xem mục 4, ngoài phạm vi phiên này). |
| `Effect.setLocation(...)` là no-op | Vẫn còn, chưa cần dùng tới. |
| `SensorScope.GLOBAL` chưa dùng | Vẫn còn — chưa có cơ chế share world state giữa nhiều boss cho sensor `GLOBAL`. |
| `Memory.hasGoalRelevantFactChanged()` là stub | Vẫn còn, trả thẳng `dirtyFlag`. |
| Tham số hệ thống hardcode | `MAX_PLAN_DEPTH=20`, `MAX_NODES_EXPLORED=1000`, `DECAY_RATE=0.95f`, `DECAY_INTERVAL_MS=5000`, `THREAT_TIMEOUT_MS=60000`, và giờ thêm `HTNPlanner.MAX_DECOMPOSITION_DEPTH=12`/`MAX_TASKS_EXPANDED=500` — vẫn hardcode, chưa đọc từ config. |
| `Effect.apply(null, state)` trong Planner/HTNPlanner simulation | Trade-off có chủ đích, không phải bug (xem thêm mục 9.2 — `GoalTask.getEffect()` là no-op tương tự, có lý do riêng). |
| ~~`Decision.shouldSwitchGoal()` là dead code~~ ✅ ĐÃ SỬA | `Brain.runGoap()` giờ gọi `shouldSwitchGoal()` thật trước khi đổi goal đang theo đuổi — có hysteresis 20% chống flip-flop, thay vì luôn chọn lại `bestGoal` mỗi lần dirty. |
| ~~Sensor throttle chia 0~~ ✅ ĐÃ SỬA | `Brain.updateSensors()` clamp `Math.max(1, sensor.getThrottleTicks())` trước khi `%`, tránh `ArithmeticException` nếu 1 Sensor lỡ khai báo `getThrottleTicks() == 0`. |
| ~~`BossFactory.configureMob()` NPE risk~~ ✅ ĐÃ SỬA | Null-check `mob.getAttribute(Attribute.MAX_HEALTH)`/`KNOCKBACK_RESISTANCE` trước khi gọi `setBaseValue()` — một số `EntityType` hiếm không có các attribute này. |

---

## 6. Tham khảo bên ngoài — VortexMobs (Sauron05/VortexMobs)

Kiến trúc khác hẳn (không phải GOAP thuần) — hệ thống "adaptive combat AI" học lối chơi của server:

- `ServerGenome`: mỗi server có seed riêng + "combat brain" bền vững qua thời gian.
- Học từ hành vi người chơi: kiting bằng cung, turtle bằng khiên, high-ground abuse, focus-fire, burst damage → mob/boss thích nghi ngược lại.
- Boss có các stage/unlock mở dần: `DASH` (phạt kiting), `ROAR` (phạt gom cụm), `INTERCEPTORS` (triệu hồi support khi meta lạm dụng ranged).
- Lệnh gọn: `/vortexmobs brain|spawnboss|resetbrain confirm|reload`.

→ Ý tưởng đáng tham khảo cho `Personality`/`Learning` ở mục 7, và format lệnh `/boss <sub>` gọn thay vì rải nhiều lệnh riêng.

---

## 7. Nghiên cứu mở rộng — `AdaptiveCombatEngine`

Kiến trúc đề xuất, kết hợp nhiều kỹ thuật AI thay vì chỉ GOAP:

| Khối | Trạng thái | Ghi chú |
|---|---|---|
| **Sensors** | 🟡 Có interface, 0 implementation | `ai/sensor/` |
| **Combat Analytics** | 🔴 Chưa có | Tổng hợp Sensor → số liệu combat (DPS, tần suất né, loại damage). |
| **Knowledge Base** | 🔴 Chưa có | Facts dài hạn, cần persistent storage (SQLite kiểu `DbManager.java`), sống qua nhiều lần spawn — khác `Memory` hiện tại (chỉ sống trong 1 fight). |
| **Strategy Generator** | 🔴 Chưa có | Sinh chiến lược cấp cao từ Knowledge Base + Analytics, thay `PhaseDefinition` hardcode theo %máu. |
| **Utility AI** | 🟡 Có bản thô, hysteresis đã nối thật | `Decision.chooseGoal()` so `assessPriority()` tuyến tính; `shouldSwitchGoal()` (ngưỡng 20%) nay ĐÃ được `Brain.runGoap()` gọi thật (mục 9), không còn dead code. Utility AI đầy đủ vẫn cần response curve phức tạp hơn ngưỡng tuyến tính này. |
| **HTN (chiến lược)** | ✅ Có, MỚI (mục 9) | `ai/htn/` — `CompoundTask`/`Method` (branching thật theo precondition, backtrack), thay hẳn `Behavior` flat-sequence cũ. `GoalTask` là điểm khớp nối tường minh với GOAP. |
| **GOAP Planner** | ✅ Có, đã nâng cấp (mục 2.3) + giờ dùng làm tầng chiến thuật bên trong HTN qua `GoalTask` | `ai/planner/` |
| **Skill Synergy** | 🟡 Có nền móng | `ActionGraph` giờ biết quan hệ effect→precondition thật, nhưng chưa có khái niệm "giá trị cộng hưởng" combo. |
| **Memory** | 🟡 Có, nhưng ngắn hạn | `ai/memory/Memory.java` — world state + 20 cast gần nhất, reset khi boss chết. |
| **Personality** | 🔴 Chưa có | Tham số hoá "tính cách" boss (hung hăng/phòng thủ/cơ hội), lấy ý tưởng `ServerGenome` từ VortexMobs. |
| **Learning** | 🔴 Chưa có | Vòng phản hồi kết quả fight → cập nhật Knowledge Base/Personality. Khó nhất — cần định nghĩa thắng/thua, tần suất update, tránh overfit. |

**Nhận xét:** 3 khối (Sensor, GOAP, Memory) đã có sơ khai; 8 khối còn lại là mở rộng lớn. Utility AI / Behavior Tree / GOAP không thay thế nhau mà dùng cho 3 loại quyết định khác nhau trong cùng 1 boss (phản xạ tức thời / lựa chọn mờ / lập kế hoạch dài hơi).

---

## 8. Đề xuất thứ tự xử lý tiếp theo

1. ~~Sửa 3 bug nghiêm trọng~~ ✅ Đã xong (mục 2).
2. ~~Nối phase system thật + tự chuyển phase + fire events~~ ✅ Đã xong (mục 5, mục 9).
3. ~~Thêm tầng HTN, sửa 4 bug nhỏ (sensor throttle/0, shouldSwitchGoal dead code, BossFactory NPE, Condition/Effect không phải functional interface)~~ ✅ Đã xong (mục 9).
4. ~~Viết ít nhất 1 boss type hoàn chỉnh~~ ✅ Đã xong — Demon Lord (mục 10).
5. **Nối Boss vào lifecycle plugin** (mục 3) — VẪN CHƯA LÀM, giờ là việc quan trọng nhất còn lại: `onEnable` gọi `DemonLordBossType.register()`, `BukkitRunnable` gọi `bossManager.tickAll()`, listener `EntityDamageEvent`/`EntityDeathEvent` gọi `notifyBossDamaged()`/`boss.die(killer)`. Không có bước này thì Demon Lord ở mục 10 chỉ là code nằm im, spawn xong không tick (đã xác nhận bằng grep, xem hội thoại có liên quan).
6. `bosses.yml` config-driven (bao gồm cả tham số HTN: `MAX_DECOMPOSITION_DEPTH`/`MAX_TASKS_EXPANDED`) + debug commands (`/boss debug` nên in cả Strategy đang active + decomposition hiện tại, không chỉ GOAP plan).
7. Bắt đầu `AdaptiveCombatEngine` — ưu tiên `Combat Analytics` + `Personality` trước vì khác biệt nhất so với hệ thống hiện tại và không phụ thuộc các khối còn lại quá nhiều.

---

## 9. Cập nhật phiên làm việc hiện tại — Tầng HTN + sửa lỗi nhỏ

### 9.1 Bug nhỏ đã sửa (phát hiện ở phiên review trước, sửa ở phiên này)

| # | File | Vấn đề | Cách sửa |
|---|---|---|---|
| 1 | `ai/executor/Brain.updateSensors()` | `sensorUpdateCounter % sensor.getThrottleTicks()` — nếu 1 Sensor lỡ trả `getThrottleTicks() == 0` (Javadoc chỉ ghi "must be >= 1", không có validate) → `ArithmeticException`, crash cả Brain tick. | Clamp `Math.max(1, sensor.getThrottleTicks())` trước khi `%`. |
| 2 | `ai/decision/Decision.shouldSwitchGoal()` | Có logic hysteresis (ngưỡng 20%) nhưng không nơi nào gọi — `Brain` cũ chỉ gọi thẳng `chooseGoal()` mỗi khi dirty, khiến goal có thể đổi liên tục nếu 2 Goal có priority sát nhau dao động quanh nhau mỗi tick. | `Brain.runGoap()` giờ gọi `shouldSwitchGoal()` thật: chỉ đổi goal đang theo đuổi nếu goal mới thắng rõ ràng (>20%), nếu không tiếp tục theo goal cũ (vẫn replan nếu cần, chỉ không đổi *goal*). |
| 3 | `core/BossFactory.configureMob()` | `mob.getAttribute(Attribute.MAX_HEALTH)`/`KNOCKBACK_RESISTANCE` không null-check — 1 số `EntityType` hiếm không có attribute này → NPE ngay lúc spawn. | Null-check trước khi gọi `setBaseValue()`/`setHealth()`; nếu null thì bỏ qua, giữ health mặc định Bukkit gán. |
| 4 | `ai/worldstate/Condition.java`, `Effect.java` | `getDescription()` là abstract method thứ 2 (bên cạnh `isSatisfied()`/`apply()`) → cả 2 interface KHÔNG phải functional interface → `AbstractSkill` dòng 22-23 dùng lambda (`ctx -> true`, `(ctx, state) -> {}`) làm fallback mặc định **không compile được**. Bug có sẵn từ code gốc, phát hiện khi review lại theo yêu cầu người dùng — javadoc của `getRelevantKeys()`/`getModifiedKeys()` vốn đã ghi rõ ý định "inline lambdas in a custom Skill" nên đây là lỗi lệch giữa ý định thiết kế và interface thật. | Đưa `getDescription()` thành `default` (trả `"condition"`/`"effect"`) ở cả 2 interface — chỉ còn `isSatisfied`/`apply` abstract → thành functional interface thật, lambda compile được, các factory method (`equals`, `greaterThan`...) vẫn override `getDescription()` bình thường để có mô tả có ý nghĩa. |

### 9.2 Tầng HTN mới — `ai/htn/`

**Vì sao thêm HTN thay vì chỉ nâng cấp GOAP:** GOAP thuần không diễn đạt tốt combo/cutscene có thứ
tự cố định có chủ đích (`Behavior` cũ giải quyết tạm bằng 1 flat sequence, không branching). HTN xử
lý đúng bài toán này bằng "Method" (mỗi Method = 1 cách phân rã, có precondition riêng, thử theo
thứ tự khai báo, backtrack nếu thất bại giữa chừng) — đây là mô hình HTN kinh điển kiểu SHOP, không
phải Behavior Tree (không có Selector/Sequence node lồng nhau) và không phải Utility AI (không so
điểm số liên tục) — chọn đúng công cụ cho đúng loại quyết định như mục 7 đã nhận xét.

**File mới (9 file):**
- `Task.java` — marker interface gốc (`CompoundTask` hoặc `PrimitiveTask`).
- `PrimitiveTask.java` — leaf task, có `getPrecondition()`/`getEffect()` để `HTNPlanner` mô phỏng tiến (forward-simulate) qua các subtask anh em trong cùng 1 Method.
- `SkillTask.java` — bọc 1 `Skill`, cast thẳng không qua GOAP (thay thế vai trò cũ của `Behavior`).
- `GoalTask.java` — **điểm khớp nối HTN↔GOAP**: giao hẳn cho Decision+Planner tự tìm skill sequence tới khi `Goal.isComplete()`. `getEffect()` cố tình là no-op (không khai báo `getModifiedKeys()`) — đã ghi rõ trong Javadoc: HTNPlanner không thể biết trước GOAP sẽ chọn skill nào lúc runtime nên không mô phỏng được state sau `GoalTask`; hệ quả: subtask đứng sau 1 `GoalTask` trong cùng Method được kiểm tra precondition dựa trên state TRƯỚC `GoalTask`, không phải state giả định sau khi goal hoàn thành. Đây là trade-off có ghi chú rõ, không phải bug ẩn — giống tinh thần `Effect.apply(null, state)` đã chấp nhận ở GOAP Planner (mục 5).
- `Method.java` — id + precondition + `List<Task>` subtasks (bắt buộc ≥ 1 subtask).
- `CompoundTask.java` — id + `List<Method>` (bắt buộc ≥ 1 method), thử theo thứ tự khai báo.
- `HTNPlanner.java` — decompose đệ quy có backtrack thật (method thất bại giữa chừng → không để lọt kết quả 1 phần, quay lại thử method tiếp theo), giới hạn `MAX_DECOMPOSITION_DEPTH=12`/`MAX_TASKS_EXPANDED=500` giống tinh thần `Planner` GOAP (`MAX_PLAN_DEPTH`/`MAX_NODES_EXPLORED`). Stateless, an toàn dùng chung nhiều boss (không cần build-once như `ActionGraph` vì Method order đã tường minh sẵn lúc khai báo).
- `Strategy.java` / `AbstractStrategy.java` — thay thế `Behavior`/`ai/decision/Behavior.java` (đã xoá file). Vẫn giữ nguyên contract `shouldActivate()`/`getPriority()`/`onComplete()` để `Brain` chọn Strategy ưu tiên cao nhất y hệt logic cũ, chỉ khác `getRootTask()` trả về `Task` (có thể branching) thay vì `List<Skill>` phẳng.

**File sửa để nối tầng HTN vào:**
- `ai/executor/Brain.java` — viết lại: `tick()` giờ resolve Goal/Strategy theo phase (9.3) → nếu có
  Strategy thắng thế, `HTNPlanner.decompose()` MỘT LẦN lúc activate (không mỗi tick, giữ đúng
  nguyên tắc "build/plan không chạy mỗi tick" đã có ở GOAP) → `runStrategy()` pop từng
  `PrimitiveTask`: `SkillTask` cast thẳng, `GoalTask` giao cho `runGoap`-style sub-loop (dùng lại
  `requestPlan`/`currentPlan`/`decision` y hệt luồng GOAP thuần) tới khi goal đó complete. Không có
  Strategy nào active → rơi về `runGoap()` (logic cũ, cộng thêm fix 9.1.2).
- `core/BossType.java` — `List<Behavior> behaviors` → `List<Strategy> strategies` (đổi tên field/getter, vị trí tham số không đổi trong constructor).
- `core/BossFactory.java` — truyền `bossType.getPhases()` + `bossType.getGoals()`/`getStrategies()` (làm fallback) vào `Brain` thay vì 1 flat `availableGoals`/`behaviors`; fire `BossSpawnEvent` sau khi đăng ký boss.
- `phase/PhaseDefinition.java` — thêm field `List<Strategy> availableStrategies` (constructor đổi từ 5 xuống... thực ra thêm 1 tham số, 5→6) — đây là chỗ **ràng buộc Phase↔AI thật sự**: 1 Strategy ultimate chỉ nên khả dụng ở phase cuối, giờ khai báo được thẳng qua `PhaseDefinition`, không cần hack trong `shouldActivate()`.
- `core/Boss.java` — thêm `checkPhaseTransition()` gọi sau mỗi `brain.tick()`: tự tính %máu qua `Attribute.MAX_HEALTH`, resolve phase đúng (ngưỡng lớn nhất ≤ %máu hiện tại, xem code để rõ thuật toán chọn), gọi `brain.cancelPlan()` khi đổi phase (bỏ plan/decomposition cũ xây theo goal/strategy pool của phase cũ), fire `BossPhaseChangeEvent`, broadcast `phaseTransitionMessage` cho player trong bán kính 40 block bằng Bukkit API thuần (không phụ thuộc core). `die()` giờ nhận thêm overload `die(LivingEntity killer)` và tự fire `BossDeathEvent` (idempotent — gọi 2 lần không fire 2 event).

### 9.3 Vẫn còn hạn chế / chưa làm (từ HTN, ghi nhận trung thực)

- HTNPlanner mô phỏng forward qua `SkillTask` chính xác (dùng `Effect.getModifiedKeys()` y hệt GOAP), nhưng qua `GoalTask` thì KHÔNG (xem 9.2 — trade-off có chủ đích, không phải thiếu sót ẩn).
- Chưa có debug command in ra Strategy/decomposition đang chạy (mục 3/8) — khó trace "vì sao boss chọn Method này" ngoài đọc log `System.err` khi lỗi.
- `HTNPlanner` cũng hardcode `MAX_DECOMPOSITION_DEPTH`/`MAX_TASKS_EXPANDED` giống các tham số GOAP khác — chưa đọc từ config (mục 5).
- Vẫn 0 `Strategy`/`CompoundTask`/`Method` cụ thể — toàn bộ ví dụ trong README là minh họa, chưa test end-to-end được (mục 4, mục 8 bước 5).

---

## 10. Boss cụ thể đầu tiên — Demon Lord (`Boss/bosses/demonlord/`, 11 file)

Boss máu-quỷ (blood + demon lord theme), `EntityType.WITHER_SKELETON`, 400 HP, dùng đúng
pipeline HTN+GOAP đã build ở mục 9 — không phải ví dụ README nữa, là code thật.

| File | Vai trò |
|---|---|
| `BloodSiphonSkill` | Rút máu mục tiêu (melee ≤8 block), hồi máu boss + cộng `DEMON_BLOOD_CHARGE`. |
| `CrimsonNovaSkill` | AoE nổ máu quanh boss, chỉ dùng được khi `PLAYERS_CLUSTERED`, cộng charge nhanh hơn. |
| `SanguineChainsSkill` | Trói mục tiêu (Slowness nặng) qua `TARGET_ROOTED_TICKS` (đếm ngược theo tick, không phải boolean). |
| `HellfireBrandSkill` | Sát thương đơn mục tiêu, bonus "execute" khi `TARGET_HEALTH_PERCENT <= 0.3`. |
| `DemonicAscensionSkill` | Ultimate phase 3: tiêu `DEMON_BLOOD_CHARGE`, hồi 25% máu + buff Strength/Speed. |
| `PressureTargetGoal` | Goal nền tảng "hạ máu mục tiêu về 0", priority tăng khi mục tiêu càng gần. |
| `DrainLifeGoal` | Priority tăng dần mượt theo %máu boss càng thấp (utility curve thật, không phải ngưỡng bật/tắt). |
| `ControlClusterGoal` | Priority 0.9 khi có cụm người chơi, 0 khi không — phản ứng đúng lúc `PLAYERS_CLUSTERED` bật. |
| `DemonLordCoreSensor` | Sensor DUY NHẤT ghi mọi world fact các Skill/Goal ở trên đọc: %máu boss/target, khoảng cách, số người chơi gần, cụm (heuristic đơn giản, ghi chú rõ là simplification), giảm dần `TARGET_ROOTED_TICKS`. |
| `BloodAscensionStrategy` | HTN Strategy phase 3 — 2 Method: đủ charge thì Ascension ngay + `GoalTask(pressureTargetGoal)`; chưa đủ thì Siphon trước + `GoalTask(drainLifeGoal)`. Đây là ví dụ THẬT của điểm khớp nối `GoalTask` (mục 9.2), không phải minh họa. |
| `DemonLordBossType` | Lắp ráp: 3 `PhaseDefinition` (100-60% GOAP thuần → 60-30% thêm `ControlClusterGoal` → dưới 30% HTN `BloodAscensionStrategy` tiếp quản), build `ActionGraph` 1 lần, `register()` gọi `BossFactory.registerBossType()`. |

**Quyết định thiết kế đáng chú ý:**
- 3 Goal dùng chung 1 instance (`pressureTargetGoal`, `drainLifeGoal`, `controlClusterGoal`) xuyên suốt cả 3 phase VÀ bên trong `BloodAscensionStrategy`'s `GoalTask` — vì `Brain` so goal bằng reference (`activeGoapGoal != goal`), dùng chung instance tránh replan thừa không cần thiết.
- `DemonicAscensionSkill` về mặt kỹ thuật KHÔNG cấm GOAP thuần chọn nó (precondition chỉ check `DEMON_BLOOD_CHARGE`) — nó chỉ "hầu như" HTN-only vì `DemonLordBossType` không đưa nó vào goal pool nào của phase 1/2, và phase 3 vẫn có nó trong `allGoals`/`skills` chung nhưng không Goal nào trong pool nhắm tới cast riêng lẻ nó qua A* trừ khi 1 Goal khác vô tình cần đúng effect này — đây là giới hạn của thiết kế hiện tại (không có cơ chế "skill chỉ HTN mới gọi được"), ghi nhận trung thực chứ không giấu.

**Chưa verify được (giới hạn thật, không phải bug ẩn):**
- **Không có Maven/Gradle project** trong gói được cung cấp → chưa build thật được, chỉ verify bằng `javalang` (cú pháp) + đọc code đối chiếu chữ ký từng method/constructor tại mọi call site. Các API Bukkit dùng (`Particle.DUST`, `Attribute.MAX_HEALTH`, `LivingEntity.damage(double, Entity)`, `PotionEffect` 5-arg constructor...) đúng theo tài liệu Spigot 1.21 nhưng chưa compile thật để chắc chắn 100%.
- Chưa test in-game (phụ thuộc mục 3 - lifecycle wiring vẫn chưa xong).
- Cụm người chơi (`PLAYERS_CLUSTERED`) dùng heuristic đơn giản (mục sensor ở trên) — có thể cho kết quả "clustered" sai trong địa hình phức tạp (nhiều tầng, vật cản).

---

## 11. Bản upload tiếp theo — lifecycle wiring, Boss Mastery, Shield Defense + EnhancedCombatStrategy (không phải do phiên này viết) + các lỗi thiết kế phát hiện khi đánh giá

Từ đây trở đi ghi nhận 1 đợt phát triển tiếp theo, phần lớn **không phải do phiên làm việc viết
HTN ban đầu tạo ra** — có vẻ tự làm hoặc từ công cụ khác — cộng với các lỗi phát hiện + đã sửa khi
đánh giá lại.

### 11.1 Phần mới (ghi nhận để đồng bộ tài liệu)

- **Lifecycle wiring** — `MagicAbilitiesfork.setupBossSystem()` gọi `DemonLordBossType.register()`,
  đăng ký `BossEventListener` (bridge `EntityDamageByEntityEvent`/`EntityDeathEvent` →
  `notifyBossDamaged()`/`die()`), chạy tick task 5Hz. Coi như đã giải quyết mục 3/8.
- **`event/BossEventListener.java`**, **`commands/PowerBossCommand.java`** (`/powerboss
  summon|list|kill|info|mastery`) — combat event + lệnh admin thật.
- **`mastery/BossMastery(Store).java`** — tiến trình dài hạn: thắng nhanh (≤60s) → tier lên, thắng
  chậm (≥240s) → tier xuống, scale HP (+15%/tier)/damage (+10%/tier). Dùng file `boss.db` RIÊNG
  (tách khỏi `data.db` của `DbManager`) — cải tiến tốt, tránh đụng độ với dữ liệu người chơi.
- **BossBar, debug logging, chống lặp skill trong GOAP** (`Memory.snapshotRecentCastCounts()` +
  `Planner.findPlan(...)` overload phạt g-cost skill vừa cast gần đây) — như đã ghi ở lần trước.
- **Trang bị vũ khí/giáp** — `BossFactory.equipBoss()` (mới): Demon Lord được gắn full giáp
  Netherite + kiếm + khiên, drop chance 0%. **Ghi nhận 1 code smell nhỏ:** hàm này nằm trong
  `BossFactory` (code khung sườn dùng chung) nhưng lại `if ("demon_lord".equals(bossTypeId))` — hardcode
  theo ID của 1 boss cụ thể ngay trong lớp framework. Chưa sửa (không phải bug chức năng, chỉ là
  smell kiến trúc) — về lâu dài nên chuyển thành dữ liệu cấu hình theo từng `BossType` thay vì
  if/else theo ID trong `BossFactory`.
- **`ShieldDefenseSkill` + `DefendWithShieldGoal`** (mới) — thêm 1 nhánh phòng thủ: khi máu thấp,
  boss có thể "giơ khiên" (buff Resistance + hồi máu nhẹ).
- **`EnhancedCombatStrategy`** (mới) — 1 HTN Strategy thứ 2, CompoundTask 5 Method (ascension /
  punish cluster / **chain_root_execute — SanguineChains rồi HellfireBrand, đúng combo được nhắc ở
  mục 10 nhưng chưa ai làm** / drain-to-heal / general pressure fallback). Thiết kế tốt hơn
  `BloodAscensionStrategy` cũ về độ đa dạng hành vi.

### 11.2 Lỗi phát hiện + đã sửa khi đánh giá lại

**(a) `DemonicAscensionSkill` vẫn KHÔNG bị giới hạn phase như tưởng — lỗi mục 10 tái xuất hiện:**
Bản upload này build trên nền TRƯỚC lượt sửa 2 lỗi ở phiên trước (bản sửa đó chưa bao giờ được đóng
gói gửi đi — lỗi quy trình của phiên trước, xin lỗi vì việc đó), nên `DemonicAscensionSkill` lại nằm
chung `skills` dùng build 1 `ActionGraph` duy nhất, và `DrainLifeGoal` (goal mà effect của Ascension
khớp thẳng) có mặt ở mọi phase → GOAP thuần có thể tự cast ultimate từ Phase 1.
**Đã sửa lại:** tách `goapSkills` (không có Ascension) khỏi `allSkills` (catalog đầy đủ) — `ActionGraph`
chỉ build từ `goapSkills`. Ascension giờ CHỈ reach được qua `SkillTask` trong Strategy.

**(b) `SanguineChainsSkill` — đã có lời giải nhưng bị vô hiệu hoá bởi lỗi (c) bên dưới:**
`EnhancedCombatStrategy`'s Method `chain_root_execute` đã đúng hướng (Chains rồi Brand, kịch bản
HTN thật, không phải graph hack) — nhưng vì lỗi ưu tiên Strategy bên dưới, Method này chưa bao giờ
chạy được ở Phase 3. Cũng đã cập nhật `HellfireBrandSkill`'s execute bonus để trigger thật theo
`TARGET_ROOTED_TICKS > 0` (không chỉ máu thấp như trước) — giờ combo có tác dụng sát thương thật.

**(c) Strategy bị đụng độ ưu tiên, làm chết Method hay nhất của `EnhancedCombatStrategy`:**
Bản gốc đăng ký CẢ `BloodAscensionStrategy` (priority 0.9) LẪN `EnhancedCombatStrategy` (priority
0.85) cho Phase 3. `findTopStrategy()` luôn chọn priority cao hơn khi cả hai cùng thoả
`shouldActivate()` (cả hai đều chỉ check `hasTarget()`) → `BloodAscensionStrategy` LUÔN thắng ở Phase
3, khiến 4/5 Method của `EnhancedCombatStrategy` (bao gồm `chain_root_execute`, `punish_cluster_combo`)
không bao giờ chạy được đúng lúc cần nhất.
**Đã sửa:** chỉ đăng ký `EnhancedCombatStrategy` (Phase 2 VÀ Phase 3) — nó là tập cha thật sự của
`BloodAscensionStrategy` (Method `demonic_ascension_combo` phủ đúng use-case ascend-khi-đủ-charge,
cộng thêm 4 method khác). `BloodAscensionStrategy.java` vẫn giữ trong code (không xoá, đánh dấu rõ
trong javadoc là "không đăng ký, không phải rác mồ côi") — có thể tái dùng cho 1 boss đơn giản hơn
sau này.

**(d) `DefendWithShieldGoal` — target condition bị đảo ngược, khiến `ShieldDefenseSkill` không bao
giờ thực sự được lên kế hoạch:** Target condition gốc là `BOSS_HEALTH_PERCENT <= 0.6` — đúng ra target
condition phải là trạng thái ĐÃ ĐẠT ĐƯỢC (goal complete), không phải điều kiện kích hoạt. Viết như cũ
nghĩa là `isComplete()` trả `true` NGAY LÚC Goal này vừa trở nên liên quan (máu vừa xuống 60%) → A*
trả về plan rỗng ngay từ đầu, `ShieldDefenseSkill` không bao giờ thực sự được chọn qua Goal này.
**Đã sửa:** đổi thành `Condition.greaterThan(BOSS_HEALTH_PERCENT, DEFEND_THRESHOLD)` — goal chỉ
"complete" khi máu đã hồi lên trên ngưỡng, đúng pattern đã dùng ở `DrainLifeGoal`.

**(e) Dọn phụ:** khôi phục `.cost(...)` bị rớt mất ở 4 skill (`BloodSiphon` 1.5, `CrimsonNova` 2.0,
`SanguineChains` 1.2, `DemonicAscension` 0.5) — không phải bug nghiêm trọng (Builder có default 1.0)
nhưng làm mất sự khác biệt "độ đắt" giữa các skill mà A* dùng để tie-break. Xoá lại file rác
`ai/decision/Behavior.java` (xuất hiện lại lần 2, vẫn không nơi nào dùng).

### 11.3 Vài điểm cân bằng số liệu (chưa sửa, cần test thật)
- Blood Siphon sustain ~10 HP/s liên tục nếu trong tầm 8 block (500 HP base + Ascension hồi 25% mỗi
  ~15-30s + Shield Defense hồi 5% mỗi 5s khi máu thấp) — cộng dồn 3 nguồn hồi máu, nên theo dõi kỹ
  khi test solo, có thể quá bền.
- Hellfire Brand execute: 7 dmg thường → 16 dmg khi trigger bonus (giờ có 2 điều kiện: máu thấp HOẶC
  bị trói) — dễ trigger hơn trước, cần test lại độ khó.
- Cluster heuristic vẫn là heuristic đơn giản, chưa test địa hình phức tạp.
- `EnhancedCombatStrategy`'s Method `general_pressure` dùng `Condition.and(Condition.greaterOrEqual(NEAREST_THREAT_DISTANCE, 0.0))`
  — về bản chất luôn đúng (khoảng cách không bao giờ âm), đóng vai trò fallback cuối cùng. Hoạt động
  đúng nhưng cách viết hơi vòng vo, có thể thay bằng 1 helper `Condition.always()` cho rõ ý hơn (chưa
  có method này trong `Condition`, có thể thêm sau nếu muốn).

---

## 12. Thiết kế lại toàn bộ bộ skill theo đúng power `demon_lord` thật

Người dùng chỉ ra bộ skill cũ của Boss (Blood Siphon/Crimson Nova/Sanguine Chains/Hellfire Brand/
Demonic Ascension/Shield Defense) là **tự đặt ra**, không liên quan tới `DemonLordPower.java`
(`powers/custom/`) — power thật của người chơi có 6 khả năng + 1 passive:
Hell's Tongue, Soul Warrior Summoning, Hell's Prison, Hellfire Eruption, Shadow Step, Judgment, Fury.

### 12.1 Ánh xạ Power → Boss Skill (điều chỉnh cho AI, không copy y nguyên)

| Power ability (player) | Boss Skill mới | Điều chỉnh chính |
|---|---|---|
| Hell's Tongue (3 lưỡi kiếm lửa bắn theo hướng nhìn) | `HellsTongueSkill` | Player bắn projectile theo hướng chuột; boss không có aim nên đổi thành 3 nhát chém cung ~50° hướng target, staggered 0/3/6 tick. |
| Soul Warrior Summoning | `SummonSoulWarriorsSkill` | Bỏ cơ chế re-target theo "last damaged by player" (không có khái niệm tương đương ở boss) — set target 1 lần lúc triệu hồi. |
| Hell's Loose Confinement | `HellPrisonSkill` | Giữ nguyên cơ chế vòng lửa kéo+dame+ignite+slow rồi nổ cuối — mạnh hơn hẳn `SanguineChainsSkill` cũ (chỉ slow). |
| Hellfire Eruption | `HellfireEruptionSkill` | Giữ telegraph 1s trước khi nổ (cho người chơi cơ hội né vì boss không có mechanic đối kháng nào khác). |
| Shadow Step | `ShadowStepSkill` | **Quan trọng nhất về mặt AI** — dạy teleport ra sau lưng target + burst khi hạ cánh, đây là câu trả lời thật đầu tiên cho lỗ hổng "không có navigation" nhắc đi nhắc lại nhiều lần trong file này. Effect khai báo `NEAREST_THREAT_DISTANCE = 1.5` sau khi dùng — khiến `GraphBuilder` nối thật edge ShadowStep → các skill tầm gần khác, GOAP có thể tự xâu chuỗi "target xa → Shadow Step → Hell's Tongue" bằng A*, không cần HTN ép buộc. |
| Judgment (kênh 3s rồi nổ diện rộng %máu tối đa) | `JudgmentSkill` | Giữ nguyên kênh 3s (glowing telegraph + Resistance) rồi giải phóng 20 + 30%maxHP mỗi mục tiêu trong 15 block. Cùng bug loại đã gặp ở `DemonicAscensionSkill` (mục 11) nên áp dụng lại kỹ thuật loại khỏi `ActionGraph` — lý do khác (Judgment sát thương gần như "giết ngay" nên `PressureTargetGoal` sẽ chọn nó mọi lúc nếu không loại). |
| Fury (passive, trigger ≤30% máu) | `FurySkill` + `FuryGoal` | Thêm 1 chút hồi máu nhỏ (5%) so với bản gốc (bản gốc chỉ buff, không hồi) — lý do: cần 1 trạng thái "đã đạt được" để `FuryGoal.isComplete()` có ý nghĩa (học từ lỗi `DefendWithShieldGoal` ở mục 11). |
| *(không có trong Power)* | `BloodSiphonSkill`, `CrimsonNovaSkill` | Giữ lại — không trùng lặp với 6 khả năng thật, vẫn có vai trò riêng (sustain, cluster-punish rẻ hơn Eruption). |

### 12.2 `EnhancedCombatStrategy` viết lại — 8 Method, ưu tiên theo tình huống thật

Method thử theo thứ tự khai báo (method đầu tiên thoả precondition thắng): `fury_combo` (máu <35%) →
`judgment_combo` (đủ charge) → `close_gap_combo` (target xa 6-14 block → Shadow Step) →
`prison_execute_combo` (target gần, chưa bị trói → Prison rồi Tongue) → `punish_cluster_combo`
(cụm người chơi → Eruption) → `summon_reinforcements_combo` (≥3 người chơi gần → triệu hồi) →
`drain_to_heal` (máu ≤50% → Siphon) → `general_pressure` (fallback, dùng `Condition.always()` mới
thêm thay vì "and(greaterOrEqual(distance, 0))" — cách viết cũ tuy đúng nhưng vòng vo, đã nhắc ở
mục 11.3).

**Vì sao đây là "tự nhiên, không bị ràng buộc" như người dùng muốn:** method nào thắng phụ thuộc
hoàn toàn vào world state sống (máu, khoảng cách, số người chơi, charge) tại đúng thời điểm quyết
định — không phải rotation cố định. `goapSkills` (dùng cho `ActionGraph`) giờ chứa **8/9 skill**
(chỉ loại `JudgmentSkill`) — nghĩa là kể cả khi không có Strategy nào active (Phase 1), GOAP thuần
vẫn có thể tự chọn `ShadowStepSkill`/`HellPrisonSkill`/`HellfireEruptionSkill`/`SummonSoulWarriorsSkill`
nếu effect của chúng tình cờ giúp ích cho Goal đang theo đuổi — không bị giới hạn "chỉ dùng được nếu
HTN xếp lịch".

### 12.3 Bug tự phát hiện + tự sửa trong lúc viết

- `HellsTongueSkill.fireBlade()` hardcode `currentPhase = 0` khi gọi `DamageAPI.dealDamage()` — vì
  hàm private helper không nhận `SkillContext`. `getPhaseMultiplier(0)` rơi vào nhánh `default` cho
  ra **0.7x** (THẤP hơn cả Phase 1's 1.0x) — nghĩa là nếu không sửa, đòn đánh thường xuyên nhất của
  boss sẽ luôn yếu hơn dự kiến bất kể đang ở phase nào. Đã sửa: trích `context.getCurrentPhase()`
  MỘT LẦN trước khi schedule 3 `BukkitRunnable`, truyền xuống `fireBlade()` như tham số.
- `JudgmentSkill.release()` gọi thẳng `target.damage(damage, boss)` thay vì qua `DamageAPI` — bỏ lỡ
  hệ số nhân mastery-tier (đã hoạt động đúng cho các skill khác từ bản upload trước, mục 11) và cả
  phase multiplier. Đã sửa: route qua `DamageAPI.dealDamage(boss, target, damage, currentPhase)`.

### 12.4 Tiện ích mới cho toàn bộ Boss module (không riêng Demon Lord)

- **`Condition.always()`** (`ai/worldstate/Condition.java`) — factory mới, trả condition luôn đúng.
  Dùng cho Method fallback cuối cùng trong HTN, rõ ý hơn hẳn cách "and(greaterOrEqual(key, 0))" gượng ép.
- **`ai/skill/SkillPlugins.java`** (mới) — helper `SkillPlugins.get(Entity)` tra `Plugin` theo tên
  qua `Bukkit.getPluginManager()` thay vì import thẳng `net.trduc.magicabilitiesfork.MagicAbilitiesfork`
  — giữ đúng nguyên tắc "Core → Boss một chiều, không ngược lại". 5 skill mới (Hell's Tongue, Summon,
  Prison, Eruption, Shadow Step, Judgment) đều cần `Plugin` để schedule `BukkitRunnable` (kênh, combo
  nhiều bước, hiệu ứng trễ) nên tách sẵn cho boss khác dùng lại sau này.

### 12.5 Vẫn cần test thật (chưa verify được, ghi nhận trung thực)
- Số liệu sát thương/thời gian hồi chiêu là ước lượng dựa trên con số gốc trong `DemonLordPower.java`,
  đã giảm nhẹ so với bản player (vì boss cast lặp lại qua cooldown, không phải 1 lần bấm nút như
  player) — CẦN chỉnh lại sau khi test thật, đặc biệt `JudgmentSkill` (20 + 30%maxHP mỗi mục tiêu
  trong 15 block, cực mạnh nếu nhiều người đứng gần nhau).
- `ShadowStepSkill` dùng `boss.teleport()` trực tiếp — không kiểm tra block đích có an toàn không
  (có thể teleport vào tường/lỗ nếu địa hình phức tạp quanh target). Chưa xử lý.
- Vẫn chưa có Maven/Gradle project trong gói được cung cấp để build thật — chỉ verify bằng `javalang`
  (74 file, sạch) + đối chiếu chữ ký method/constructor thủ công tại từng call site.

---

## 13. Lỗi kiến trúc nghiêm trọng phát hiện khi người dùng báo "boss không dùng hết skill, nhất là summon"

Đây KHÔNG phải lỗi thứ tự Method hay số liệu cân bằng như mục 12 nghĩ ban đầu — mà là 1 lỗi sâu hơn
nằm ở chính `Brain.tick()`, ảnh hưởng tới MỌI boss dùng HTN Strategy, không riêng Demon Lord.

### 13.1 Nguyên nhân gốc: Strategy chỉ decompose lại **một lần** cho mỗi target

`Brain.tick()` (bản trước khi sửa) chỉ gọi `activateStrategy()` (decompose lại HTN, đánh giá lại
toàn bộ Method) khi:
```java
topStrategy.getPriority(context) > activeStrategy.getPriority(context)
```
Vì `EnhancedCombatStrategy` là Strategy DUY NHẤT với priority CỐ ĐỊNH (0.85f), điều kiện này là
`0.85 > 0.85` = **luôn luôn false** sau lần đầu tiên. Cộng thêm: 4/8 Method
(`judgment_combo`, `close_gap_combo`, `prison_execute_combo`, `summon_reinforcements_combo`) đều kết
thúc bằng `GoalTask(pressureTargetGoal)` — Goal này chỉ `isComplete()` khi **target chết hẳn**
(`TARGET_HEALTH_PERCENT <= 0`). Kết quả: Method nào thắng ở lần decompose ĐẦU TIÊN (gần như chắc
chắn là `prison_execute_combo`, vì điều kiện "target ≤5 block, chưa bị trói" gần như luôn đúng khi
đang melee) sẽ khoá Strategy vào GOAP thuần đuổi theo `pressureTargetGoal` cho **tới khi target chết**
— không bao giờ đánh giá lại `judgment_combo`, `summon_reinforcements_combo`, `punish_cluster_combo`
nữa trong suốt phần còn lại của trận. Đây là lý do thật sự khiến Summon (và cả Judgment!) gần như
không bao giờ được thấy, không phải vì thứ tự Method hay điều kiện quá hẹp.

### 13.2 Lỗi phụ: SkillTask bị khoá cooldown làm đứng hình cả combo

Nếu Method thắng có bước đầu là 1 skill đang cooldown (ví dụ Summon 25s, Judgment 45s),
`runStrategy()` cũ chỉ "retry cùng bước mỗi tick" — khiến boss **đứng im hoàn toàn** chờ tới khi
skill đó hết cooldown, có thể tới 45 giây, thay vì làm gì khác.

### 13.3 Đã sửa (`ai/executor/Brain.java`)

1. **Reconsider định kỳ**: thêm `STRATEGY_RECONSIDER_INTERVAL_TICKS = 20` (1s). Khi Strategy đang
   "coast" ở đúng bước `GoalTask` CUỐI CÙNG (`currentDecomposition.size() == 1`), cứ mỗi 1s
   `Brain.tick()` chủ động gọi lại `activateStrategy()` để đánh giá lại cả 8 Method theo world state
   mới nhất — không cần chờ 1 Strategy "thắng" Strategy khác nữa.
2. **Bỏ qua bước bị khoá cooldown**: `runStrategy()` giờ phân biệt rõ "precondition chưa đúng" (retry
   tiếp, có thể tự hết trong vài tick) với "đang cooldown" (`!skillExecutor.isCooldownExpired(...)`)
   — nếu là cooldown, **bỏ qua bước đó ngay**, không chờ, để phần còn lại của decomposition (hoặc lần
   reconsider tiếp theo) có cơ hội chạy.
3. **Sắp lại thứ tự 8 Method trong `EnhancedCombatStrategy`**: đưa `summon_reinforcements_combo` và
   `punish_cluster_combo` (tình huống cụ thể, hiếm hơn) lên TRƯỚC `prison_execute_combo` (gần như luôn
   đúng khi melee bình thường) — nếu không, kể cả với fix #1, `prison_execute_combo` vẫn thắng hầu hết
   các lần reconsider vì điều kiện của nó quá dễ đúng, tiếp tục "che khuất" summon/cluster.

### 13.4 Vẫn cần lưu ý (trade-off còn lại)
- Chu kỳ reconsider 1s nghĩa là nếu 1 combo scripted (VD `prison_execute_combo`: Prison→Tongue) đang
  chạy dở (chưa tới bước GoalTask cuối), nó KHÔNG bị ngắt giữa chừng — chỉ khi đã chạy xong hết các
  SkillTask và đang "coast" ở GoalTask mới bị đánh giá lại. Đây là chủ ý (không muốn ngắt combo đang
  diễn ra nửa chừng), không phải giới hạn kỹ thuật.
- Đây là bug ở tầng `Brain`/framework (không riêng Demon Lord) — bất kỳ boss tương lai nào dùng HTN
  Strategy với Method kết thúc bằng `GoalTask` hướng tới 1 Goal khó hoàn thành (kiểu "giết mục tiêu")
  đều sẽ dính lỗi này nếu không có fix #1. Đáng lẽ nên phát hiện sớm hơn từ mục 9 khi mới viết tầng
  HTN — ghi nhận thẳng thắn đây là thiếu sót của thiết kế ban đầu, không phải lỗi mới phát sinh.

---

## 14. GOAP giờ nhận biết cooldown — "chiêu này đang hồi thì tự chọn chiêu khác"

Sau mục 13, `Brain` không còn khoá cứng vào 1 Method/skill nữa, nhưng cả `Planner` (GOAP) lẫn
`HTNPlanner` vẫn **hoàn toàn không biết skill nào đang cooldown** khi lập kế hoạch — tách biệt có
chủ đích giữa "cái gì khả thi về nguyên tắc" (Planner) và "cái gì sẵn sàng ngay bây giờ"
(SkillExecutor). Điều này khiến A* có thể liên tục chọn lại đúng 1 skill đang khoá cooldown mỗi lần
replan, thay vì tự động thử skill khác cũng giúp ích cho Goal.

### 14.1 Đã sửa — `Planner.findPlan()` nhận thêm `Set<String> unavailableSkillIds`

```java
public Optional<List<Skill>> findPlan(Goal goal, WorldState currentState,
                                      Map<String, Integer> recentCastCounts,
                                      Set<String> unavailableSkillIds)
```
Trong vòng lặp A*, skill có ID nằm trong `unavailableSkillIds` bị loại khỏi candidate **giống hệt**
như precondition không thoả — nghĩa là A* tự nhiên tìm skill KHÁC cũng dẫn tới goal thay vì cứ dựng
kế hoạch quanh 1 skill đang khoá. `Planner.getAllSkills()` (mới, delegate `ActionGraph.getSkills()`)
cho `Brain` biết cần check cooldown của những skill ID nào.

`Brain.requestPlan()` build snapshot NGAY TRÊN MAIN THREAD (giống hệt cách làm với `stateSnapshot`/
`castCountsSnapshot` đã có) trước khi giao việc tìm plan cho thread async — tránh động vào
`SkillExecutor` từ thread khác.

2 overload cũ (`findPlan(goal, state)`, `findPlan(goal, state, castCounts)`) vẫn giữ nguyên, tự động
dùng `Collections.emptySet()` — không phá code gọi cũ.

### 14.2 HTN vẫn CHƯA cooldown-aware ở bước chọn Method (giới hạn còn lại, ghi nhận trung thực)

`HTNPlanner.decompose()` chỉ check `Condition` (world state), không check cooldown — nếu Method
thắng có `SkillTask` đầu đang khoá, `Brain.runStrategy()` (mục 13.3) sẽ BỎ QUA bước đó (không đứng
hình nữa) rồi rơi xuống `GoalTask` cuối, và từ đó GOAP (giờ đã cooldown-aware) tiếp quản đúng cách.
Nghĩa là: HTN vẫn có thể "chọn nhầm" Method có bước đầu đang khoá, nhưng hệ quả giờ chỉ là bỏ qua 1
bước chứ không còn đứng hình hay chặn GOAP — chấp nhận được, chưa cần dạy `HTNPlanner` biết cooldown
(sẽ cần truyền `SkillExecutor` vào tận tầng decompose, thay đổi lớn hơn, để dành nếu thực tế vẫn thấy
cần).

---

*Cập nhật lần cuối: theo phiên làm việc hiện tại (thêm cooldown-awareness cho GOAP Planner - `unavailableSkillIds` trong `findPlan()`, snapshot xây trên main thread giống pattern castCounts đã có). File này nên được cập nhật mỗi khi có thay đổi lớn trong `Boss/`.*

---

## 15. Rà soát lại theo yêu cầu "boss chưa dùng hết chiêu của power demon_lord"

Đối chiếu lại toàn bộ `DemonLordBossType`/`EnhancedCombatStrategy`/`Brain`/`Planner` với bảng ánh xạ ở
mục 12: **cả 6 khả năng + Fury của `DemonLordPower` đều đã có mặt và có đường tới được**:
- 5/6 (Hell's Tongue, Summon, Prison, Eruption, Shadow Step) nằm trong `goapSkills` → GOAP thuần
  (Phase 1) có thể tự chọn bất cứ lúc nào effect của chúng giúp Goal đang theo đuổi.
- Judgment cố tình bị loại khỏi `ActionGraph` (lý do ở mục 12.1/mục 13) — chỉ tới được qua
  `judgment_combo` của `EnhancedCombatStrategy` khi `DEMON_BLOOD_CHARGE >= 1.0`. Nhờ fix mục 13
  (reconsider mỗi 1s thay vì khoá cứng Method đầu tiên) + fix mục 14 (GOAP bỏ qua skill đang cooldown),
  Judgment/Summon giờ thực sự được cast trong trận, không bị "che khuất" nữa.
- Fury có `fury_combo` riêng (máu <35%, ưu tiên cao nhất trong 8 Method).
- `BloodSiphonSkill`/`CrimsonNovaSkill` (không thuộc Power thật) vẫn giữ làm công cụ sustain/cluster
  rẻ hơn — không lấn át 6 khả năng thật vì priority/Method order đã xếp chúng ở cuối (`drain_to_heal`
  là Method áp chót, trước mỗi `general_pressure`).

Phần việc thật sự còn thiếu khi rà lại: **`ShadowStepSkill.execute()` gọi `boss.teleport()` thẳng vào
toạ độ tính sẵn, không kiểm tra khối đích có an toàn không** — đúng như đã ghi nhận nhưng chưa xử lý ở
mục 12.5. Đây là vấn đề thật (không phải aesthetic): nếu địa hình quanh target gồ ghề, boss có thể
teleport vào tường (suffocate) hoặc rơi vào lava/nước ngay khi thực hiện "câu trả lời duy nhất cho bài
toán no-navigation" — nghĩa là đúng lúc cần nó nhất (target ở xa, địa hình phức tạp) lại là lúc dễ gãy
nhất.

**Đã sửa (phiên này):** thêm `findSafeLanding()` — thử 8 góc quanh target (sau lưng trước, rồi các góc
lệch dần tới đối diện) × 3 mức lệch Y (0/1/2, chịu được bậc thang/gồ nhỏ), chọn toạ độ đầu tiên có cả
khối chân và khối đầu đều passable và không phải liquid (`isSafeLanding()`). Nếu không tìm được điểm
nào an toàn trong 24 ứng viên, `execute()` bỏ qua lượt cast đó thay vì teleport liều — Planner/HTN sẽ
tự thử lại hoặc chọn skill khác ở lượt sau vì `NEAREST_THREAT_DISTANCE` không đổi. Không kiểm tra có
mặt đất bên dưới (rơi 1-2 khối chấp nhận được, không đáng để giới hạn thêm ứng viên).

**Chưa sửa lúc đó (đã xử lý lại ở mục 16 khi người dùng hỏi riêng về tính năng này):** 5 file skill/goal
cũ không thuộc Power thật (`DemonicAscensionSkill`, `HellfireBrandSkill`, `SanguineChainsSkill`,
`ShieldDefenseSkill`, `DefendWithShieldGoal`) cùng `BloodAscensionStrategy` vẫn nằm trong source nhưng
KHÔNG được `DemonLordBossType.create()` đăng ký (đã xác nhận lại bằng grep) — đúng như mục 11 mô tả, đây
là chủ ý giữ lại làm code tái dùng cho boss khác, không phải rác quên xoá, nên không đụng vào trong lần
rà soát này. Số liệu sát thương/cooldown của 8 skill đang dùng vẫn là ước lượng chưa test trên server
sống (mục 12.5) — cần chỉnh lại sau khi có dữ liệu thật, đặc biệt Judgment.

---

## 16. Bật lại tính năng đỡ khiên (`ShieldDefenseSkill`/`DefendWithShieldGoal`) theo yêu cầu

Người dùng hỏi lại riêng về tính năng đỡ khiên. Rà soát phát hiện 2 việc:

1. **Chưa hề được đăng ký** — xác nhận bằng grep, `ShieldDefenseSkill`/`DefendWithShieldGoal` không
   xuất hiện trong `DemonLordBossType.java`, đúng như mục 11/15 mô tả (code mồ côi có chủ đích).
2. **`DefendWithShieldGoal` vẫn dính đúng bug đã ghi là "đã sửa" ở mục 11(d) nhưng thực ra chưa sửa
   trong file** — target condition vẫn là `Condition.lessOrEqual(BOSS_HEALTH_PERCENT, 0.6)` (điều kiện
   KÍCH HOẠT), không phải `greaterThan` (điều kiện ĐÃ ĐẠT ĐƯỢC) như mục 11(d) tuyên bố. Nghĩa là tài
   liệu ghi nhận 1 fix chưa từng thực sự được áp dụng vào code — đã sửa lại đúng lần này (đổi thành
   `Condition.greaterThan(BOSS_HEALTH_PERCENT, DEFEND_THRESHOLD)`).

Người dùng chọn bật lại làm cơ chế phòng thủ phụ (không thuộc power thật, giống vai trò của BloodSiphon/
CrimsonNova). Đã wire vào `DemonLordBossType`:
- Thêm `ShieldDefenseSkill` vào `goapSkills` → GOAP thuần (Phase 1) có thể tự chọn.
- Thêm `DefendWithShieldGoal` vào `allGoals` và vào goal pool của Phase 1.
- Thêm Method mới `shield_defense_combo` vào `EnhancedCombatStrategy` (Phase 2+), đặt sau
  `prisonExecuteCombo` và trước `drainToHeal`. Điều kiện cố tình thu hẹp thành dải `(0.5, 0.6]`
  (khác với precondition gốc `<=0.6` của chính Skill) để KHÔNG chồng lấn lên `drain_to_heal` (`<=0.5`)
  — tránh việc shield_defense luôn thắng và làm chết hẳn nhánh Blood Siphon vốn đã cân bằng từ trước.
  Kết quả: máu 50-60% → đỡ khiên (nhẹ, hồi 5% + Resistance 4s); máu ≤50% → rút máu (nặng hơn, hồi 6% +
  tích blood charge); máu <35% → Fury (khẩn cấp nhất).
- Bổ sung `.cost(0.7)` cho `ShieldDefenseSkill` (trước đó không set, rơi về default 1.0 của Builder —
  cùng loại thiếu sót nhỏ đã liệt kê ở mục 11(e) cho 4 skill khác, giờ dọn nốt cho skill thứ 5 này).

**Chưa test thật:** dải 0.5-0.6 hẹp (10% máu boss = 50 HP trên nền 500 HP) nên cửa sổ để
`shield_defense_combo` thực sự thắng trong 1 trận có thể khá ngắn nếu boss mất máu nhanh — cần chơi thử
để xem có cần nới dải ra không (VD 0.45-0.6) nếu thấy chiêu này hiếm khi được thấy trong thực tế.

---

## 17. Bật lại `DemonicAscensionSkill` làm chiêu "lật kèo" theo yêu cầu

Người dùng muốn boss dùng được `DemonicAscensionSkill` (hồi 25% máu tối đa + buff Strength/Speed 10s)
làm công cụ lật ngược thế trận khi đang thua. Đây cũng là 1 trong 4 skill cũ không thuộc power thật,
trước giờ chỉ tồn tại trong source (chỉ được dùng bởi `BloodAscensionStrategy` - 1 Strategy độc lập
CHƯA TỪNG được đăng ký vào `DemonLordBossType`).

**Vì sao không dùng thẳng `BloodAscensionStrategy` có sẵn:** nó là 1 `Strategy` độc lập, priority cố
định 0.9 - cao hơn `enhanced_combat` (0.85). `Brain.findTopStrategy()` chọn Strategy theo priority cao
nhất trong số các Strategy `shouldActivate()` trả `true`; `BloodAscensionStrategy.shouldActivate()` chỉ
check `hasTarget()` (luôn đúng khi có mục tiêu) - nghĩa là nếu đăng ký thêm nó vào phase 2/3, nó sẽ
LUÔN thắng và thay thế hẳn `enhanced_combat`, khiến boss mất quyền truy cập fury/judgment/prison/summon/
shield/... trong suốt phase đó, không phải chỉ thêm 1 lựa chọn nữa. Thay vào đó, gộp thẳng
`DemonicAscensionSkill` thành 1 Method mới trong `EnhancedCombatStrategy` (giống cách làm với
`shield_defense_combo` ở mục 16) - giữ nguyên toàn bộ 9 Method cũ, chỉ thêm 1 lựa chọn nữa vào cùng
1 Strategy. `BloodAscensionStrategy` vẫn giữ nguyên trong source, không đụng vào, tiếp tục là code tái
dùng cho boss khác sau này (đúng tinh thần mục 11).

**Thiết kế Method mới `demonic_ascension_combo`:** điều kiện `BOSS_HEALTH_PERCENT <= 0.4 AND
DEMON_BLOOD_CHARGE >= 1.0`, đặt Ở ĐẦU danh sách Method (trước cả `fury_combo`) vì đây là công cụ lật
kèo mạnh nhất (hồi 25% + buff, so với Fury chỉ hồi 5%) nên nếu có đủ điều kiện thì nên thắng Fury. Nếu
máu thấp nhưng CHƯA đủ charge, tự động rơi xuống `fury_combo` (panic button dự phòng không cần tài
nguyên). Chung tài nguyên `DEMON_BLOOD_CHARGE` với `judgment_combo` (cả 2 đều tốn 1.0 charge) nhưng nhờ
gate theo máu, 2 Method không tranh chấp tuỳ tiện: máu ≤40% có charge → ưu tiên sống sót (Ascension);
máu >40% có charge → dùng để kết liễu mục tiêu (Judgment).

`DemonicAscensionSkill` được thêm vào `allSkills` (catalog) nhưng CỐ TÌNH không thêm vào `goapSkills` -
đúng lý do đã từng ghi nhận ở mục 11: skill này từng "rò rỉ" vào GOAP thuần Phase 1 qua `DrainLifeGoal`
(hiệu ứng hồi máu của nó tình cờ giúp goal đó) khi còn nằm trong graph, khiến 1 "ultimate lật kèo" bị
dùng bừa bãi ngay từ đầu trận thay vì chỉ đúng lúc đang thua. Giữ ngoài `ActionGraph`, chỉ tới được qua
Method này, là cách duy nhất kiểm soát đúng pacing.

**Chưa test thật:** ngưỡng 40% máu + cần đủ charge là 2 điều kiện cùng lúc, có thể hiếm khi trùng nhau
trong thực tế nếu tốc độ tích charge (từ BloodSiphon/CrimsonNova/HellPrison/HellfireEruption/Summon,
mỗi cast +0.2-0.3) chậm hơn tốc độ mất máu. Nếu chơi thử thấy chiêu này gần như không bao giờ được
thấy, có thể cần nới ngưỡng máu lên (VD 0.45) hoặc giảm `CHARGE_COST` của riêng skill này xuống dưới
1.0 (tách khỏi ngưỡng chung với Judgment) thay vì chia sẻ y hệt.

---

## 18. Boss "dùng hết bộ kỹ năng như người chơi thật" - HTN cooldown-aware + 2 skill từng chết được cứu

Người dùng yêu cầu: boss phải có khả năng dùng hết các skill mình có, hễ skill nào đang hồi chiêu thì
tự chuyển sang skill khác, hết cooldown thì quay lại dùng - đúng như một người chơi thật đang luân
chiêu. Rà soát cho thấy 3 lỗ hổng cộng dồn khiến hành vi thật khác xa mục tiêu này:

### 18.1 `HTNPlanner` chọn Method xong là khoá cứng, không biết skill đầu có đang cooldown
`HTNPlanner.decompose()` (trước bản này) chỉ check `Condition` (world state) khi chọn Method, hoàn
toàn không biết `SkillExecutor` đang khoá skill nào. Hệ quả cụ thể với Demon Lord: `judgment_combo`
chỉ cần `DEMON_BLOOD_CHARGE >= 1.0` (điều kiện rất dễ giữ đúng liên tục, charge không tự giảm) và
đứng TRƯỚC 5 Method tình huống khác trong danh sách. Khi Judgment đã cast 1 lần và đang cooldown 45s,
Method này vẫn thắng ở MỌI lần reconsider (mỗi 1s) vì điều kiện của nó không đổi; `Brain.runStrategy()`
phát hiện SkillTask(Judgment) đang cooldown, bỏ qua bước đó, rơi xuống `GoalTask(pressureTargetGoal)`
- nhưng vì goal này chỉ có `HellsTongueSkill` là skill DUY NHẤT trong `goapSkills` khai báo effect thật
sự chạm `TARGET_HEALTH_PERCENT` (xem mục 18.2), nên trong suốt ~45s đó boss chỉ spam Hell's Tongue,
không có cơ hội thử `summon_reinforcements_combo`/`punish_cluster_combo`/`close_gap_combo`/
`prison_execute_combo` dù điều kiện riêng của chúng có đúng hay không.

**Đã sửa:** `HTNPlanner.decompose()` có thêm overload nhận `Predicate<String> skillUnavailable`. Khi
gặp 1 `SkillTask` mà skill của nó đang cooldown, xử lý y hệt "precondition không thoả" - Method chứa
nó bị bỏ qua, `CompoundTask` tự động thử Method kế tiếp trong danh sách (backtracking sẵn có của
thuật toán, không cần logic mới). `Brain.activateStrategy()` gọi overload mới, truyền thẳng
`skillId -> !skillExecutor.isCooldownExpired(skillId)` - an toàn gọi trực tiếp (không phải snapshot)
vì `activateStrategy()` chạy đồng bộ trên main thread, khác với `Planner.findPlan()` chạy async (đã
cần snapshot ở mục 14). Overload 2 tham số cũ vẫn giữ nguyên (mặc định `id -> false`, tương đương
hành vi cũ) - không phá code gọi khác.

**Đánh đổi còn lại (chủ ý, không phải thiếu sót):** với Method nhiều bước như `prison_execute_combo`
(Prison → Tongue), nếu bước ĐẦU (Prison) đang cooldown thì HTN bỏ qua NGUYÊN method đó tại thời điểm
chọn, kể cả khi bước sau (Tongue) đang rảnh - không cố "chọn Method rồi bỏ bước 1, chạy bước 2". Không
sao vì `general_pressure` (fallback, `Condition.always()`) cũng cast chính Tongue, nên kỹ năng đó vẫn
được dùng qua đường khác trong cùng tick reconsider; giữ đơn giản hơn là dạy `HTNPlanner` bỏ từng bước
lẻ trong 1 Method (thay đổi lớn hơn, chưa thấy cần thiết thực tế).

### 18.2 GOAP chỉ "nhìn thấy" Hell's Tongue là cách duy nhất giết mục tiêu
Kiểm tra lại effect khai báo (dùng cho A*/heuristic, khác với damage thật lúc `execute()`) của từng
skill trong `goapSkills`: chỉ `HellsTongueSkill` khai `Effect.modifyValue(TARGET_HEALTH_PERCENT, -0.15)`.
`HellPrisonSkill` và `HellfireEruptionSkill` gây sát thương thật rất nặng lúc `execute()` nhưng effect
khai báo của chúng TRƯỚC ĐÂY chỉ đụng `DEMON_BLOOD_CHARGE`/`TARGET_ROOTED_TICKS` - với `Planner`, nghĩa
là 2 skill này "vô hình" với `PressureTargetGoal`, không bao giờ được A* chọn để giết mục tiêu, dù ở
Phase 1 (GOAP thuần) hay bất cứ lúc nào Strategy coast vào `GoalTask(pressureTargetGoal)`.

**Đã sửa:** thêm `Effect.modifyValue(TARGET_HEALTH_PERCENT, -0.30)` cho `HellPrisonSkill` và `-0.35`
cho `HellfireEruptionSkill` - số ước lượng tương đối theo mức độ sát thương thật của mỗi skill, cùng
quy ước "effect khai báo là gần đúng, Sensor tick sau ghi đè bằng giá trị thật" đã dùng cho
`HellsTongueSkill`/`HellfireBrandSkill` từ trước.

### 18.3 `CrimsonNovaSkill` chết hẳn về mặt planning dù có mặt trong `goapSkills`
Effect gốc của `CrimsonNovaSkill` chỉ ghi `DEMON_BLOOD_CHARGE` - không Goal nào nhắm tới key này, và 2
skill duy nhất tiêu charge (`Judgment`, `DemonicAscension`) bị loại khỏi `goapSkills` có chủ đích (mục
11/17). Kết quả: skill này không giúp hoàn thành bất kỳ Goal nào ở bất kỳ phase nào, cũng không nằm
trong Method HTN nào - "sống" trong code nhưng chưa từng thực sự được cast trong 1 trận đấu thật, khác
hẳn vai trò "phương án rẻ hơn thay Hellfire Eruption" mà comment gốc mô tả.

**Đã sửa:**
- Thêm `Effect.modifyValue(TARGET_HEALTH_PERCENT, -0.2)` (nhẹ hơn Hellfire Eruption, đúng vai "bản rẻ
  hơn") và `Effect.setValue(PLAYERS_CLUSTERED, 0)` (xem mục 18.4) vào effect của nó.
- Thêm Method mới `punish_cluster_combo_alt` trong `EnhancedCombatStrategy`, cùng precondition với
  `punish_cluster_combo` (`PLAYERS_CLUSTERED == 1`), đặt NGAY SAU nó trong danh sách - nhờ fix 18.1,
  Method này chỉ thực sự được chọn khi `HellfireEruptionSkill` (20s cooldown) đang khoá, cho đúng hành
  vi "dùng skill khác khi skill chính đang hồi chiêu" mà không cần thêm logic riêng.

### 18.4 `ControlClusterGoal` là ngõ cụt cho GOAP
Không skill nào (kể cả trước khi sửa) khai báo effect ghi `PLAYERS_CLUSTERED = 0` - key này trước giờ
chỉ được `DemonLordCoreSensor` ghi theo vị trí người chơi thật. Nghĩa là `Planner.findPlan()` cho
`ControlClusterGoal` luôn thất bại (không tìm được skill nào "hoàn thành" goal theo state mô phỏng),
nên mỗi khi 1 Method của `EnhancedCombatStrategy` hand-off sang `GoalTask(controlClusterGoal)`, GOAP
replan liên tục và luôn rỗng - boss coast vào ngõ cụt khoảng 1s (tới lần reconsider kế tiếp) mà không
làm gì thêm.

**Đã sửa:** thêm `Effect.setValue(PLAYERS_CLUSTERED, 0)` vào cả `HellfireEruptionSkill` lẫn
`CrimsonNovaSkill` (mục 18.2/18.3) - cùng quy ước "effect khai báo gần đúng cho mục đích lập kế hoạch,
Sensor tick sau ghi đè bằng giá trị thật" đã dùng khắp module, không phải cách sửa đặc thù riêng.

### 18.5 Chưa test thật / hạn chế còn lại
- Các con số effect mới (`-0.30`, `-0.35`, `-0.2`) là ước lượng tương đối theo độ mạnh skill, chưa đối
  chiếu với sát thương thật đo trên server sống - có thể cần tinh chỉnh cùng đợt với các con số damage
  gốc đã ghi ở mục 12.5/15.
- `PhaseDefinition.damageMultiplier` (1.0/1.15/1.35 khai trong `DemonLordBossType`) và độ lệch với bảng
  hardcode riêng của `DamageAPI.getPhaseMultiplier()` (1.0/1.2/1.5) - đã phát hiện ở buổi rà soát trước
  nhưng CHƯA sửa trong lần cập nhật này (nằm ngoài phạm vi yêu cầu "dùng hết skill" của phiên này) - cần
  1 lượt riêng để thống nhất 1 trong 2 hệ số, tránh 2 nguồn số liệu cạnh tranh nhau.

---

*Cập nhật lần cuối: theo phiên làm việc hiện tại (HTN cooldown-aware Method selection qua
`HTNPlanner.decompose(root, state, skillUnavailable)` + `Brain.activateStrategy()`; hồi sinh
`CrimsonNovaSkill` bằng Method dự phòng thật; vá effect khai báo còn thiếu của `HellPrisonSkill`/
`HellfireEruptionSkill`/`CrimsonNovaSkill` để GOAP nhìn đúng năng lực gây sát thương và giải toả cụm
của chúng). File này nên được cập nhật mỗi khi có thay đổi lớn trong `Boss/`.*

---

## 19. Thống nhất 2 hệ số nhân theo phase (`PhaseDefinition.damageMultiplier` vs `DamageAPI.getPhaseMultiplier()`)

Tiếp nối mục 18.5: `PhaseDefinition.damageMultiplier` (1.0/1.15/1.35 mà `DemonLordBossType` khai báo
cho 3 phase) trước đây là cấu hình CHẾT - `DamageAPI.dealDamage(Mob, LivingEntity, double, int)`, nơi
DUY NHẤT tính sát thương thật cho mọi skill, chỉ gọi `getPhaseMultiplier(currentPhase)` - 1 bảng
hardcode riêng (`1.0/1.2/1.5`) hoàn toàn không liên quan tới `PhaseDefinition`. `PhaseDefinition.
getDamageMultiplier()` chỉ được gọi ở đúng getter của chính nó trước khi sửa (xác nhận bằng grep).

**Cách sửa:** áp dụng đúng pattern đã có sẵn cho mastery-tier multiplier (`MASTERY_DAMAGE_KEY`) - lưu
giá trị THẬT vào `PersistentDataContainer` của entity thay vì tính lại từ 1 bảng tách biệt:

- `DamageAPI.java`: thêm `PHASE_DAMAGE_KEY` (`NamespacedKey`). `dealDamage(Mob, LivingEntity, double,
  int)` giờ đọc `boss.getPersistentDataContainer().getOrDefault(PHASE_DAMAGE_KEY, DOUBLE,
  getPhaseMultiplier(currentPhase))` - ưu tiên giá trị thật do BossType khai báo, chỉ rơi về bảng
  hardcode cũ nếu entity chưa từng được set (phòng hờ, không nên xảy ra trong luồng bình thường).
- `Boss.java`: thêm `writePhaseDamageMultiplier(PhaseDefinition)` - ghi `phase.getDamageMultiplier()`
  vào `PHASE_DAMAGE_KEY` trên entity. Gọi 1 lần trong constructor (theo phase ban đầu tính từ 100%
  máu) và mỗi lần `transitionTo()` (chuyển phase tự động theo % máu).

Không cần sửa bất kỳ skill nào (`DamageAPI.dealDamage(boss, target, dmg, context.getCurrentPhase())`
vẫn giữ nguyên signature) - tất cả 9 skill của Demon Lord tự động dùng đúng 1.0/1.15/1.35 đã khai báo
từ giờ trở đi, không còn 2 nguồn số liệu cạnh tranh nhau. Thay đổi này áp dụng cho MỌI BossType tương
lai dùng `Boss`/`DamageAPI`, không riêng Demon Lord.

**Chưa test thật:** sát thương phase 2/3 giờ sẽ NHẸ HƠN trước (1.15/1.35 thay vì 1.2/1.5 vốn đang áp
dụng nhầm) - nếu chơi thử thấy boss yếu đi rõ rệt ở phase cuối so với trước, đây là do multiplier đúng
đang thấp hơn multiplier sai trước đó, không phải bug mới; cần cân nhắc có muốn nới 1.35 lên cao hơn
hay không sau khi thấy độ khó thực tế.

---

*Cập nhật lần cuối: theo phiên làm việc hiện tại (thống nhất `PhaseDefinition.damageMultiplier` và
`DamageAPI` qua `PHASE_DAMAGE_KEY` trên PersistentDataContainer, cùng pattern với
`MASTERY_DAMAGE_KEY`). File này nên được cập nhật mỗi khi có thay đổi lớn trong `Boss/`.*

---

## 20. `REINFORCEMENTS_COOLDOWN_TICKS` không tồn tại - lỗi biên dịch trong `SummonReinforcementsGoal`

`SummonReinforcementsGoal` (mục 20 cũ, xem javadoc trong file) tham chiếu
`WorldStateKeys.REINFORCEMENTS_COOLDOWN_TICKS` nhưng key này chưa từng được khai báo trong
`WorldStateKeys.java` - lỗi biên dịch, không phải lỗi logic. Ngoài ra dù có khai báo key, chưa có nơi
nào ghi giá trị vào nó: `SummonSoulWarriorsSkill` (skill duy nhất Goal này nhắm tới) chỉ có effect trên
`DEMON_BLOOD_CHARGE`, và không có Sensor nào đếm ngược key này - nên kể cả sau khi thêm khai báo, Goal
sẽ không bao giờ complete được (giống lỗi "thiếu effect" đã gặp ở mục 18 với `HellPrisonSkill`/
`HellfireEruptionSkill`/`CrimsonNovaSkill`).

**Đã sửa (3 chỗ, theo đúng pattern của `TARGET_ROOTED_TICKS`):**
1. `WorldStateKeys.java`: thêm `REINFORCEMENTS_COOLDOWN_TICKS` (countdown, không phải boolean).
2. `SummonSoulWarriorsSkill.java`: effect giờ là `Effect.composite(...)` gồm `modifyValue(DEMON_BLOOD_CHARGE, ...)`
   cũ và `setValue(REINFORCEMENTS_COOLDOWN_TICKS, COOLDOWN_TICKS)` mới - đặt lại thành 500 ticks (25s),
   dùng chung hằng số với `cooldownTicks(500)` của chính skill thay vì 1 con số ma thuật thứ hai.
3. `DemonLordCoreSensor.java`: thêm đoạn giảm dần `REINFORCEMENTS_COOLDOWN_TICKS` mỗi tick sensor chạy,
   copy y hệt khối giảm `TARGET_ROOTED_TICKS` đã có sẵn.

Không đổi `Builder.effect()` (vẫn chỉ nhận 1 `Effect`, gọi lần 2 sẽ ghi đè lần 1) - lý do bắt buộc dùng
`Effect.composite()` thay vì gọi `.effect()` hai lần.

Parse-verify toàn bộ `src/main/java` bằng `javalang` sau khi sửa: 179 file Java, 0 lỗi liên quan tới
thay đổi này (`Metrics.java` báo lỗi parse nhưng là hạn chế có sẵn của `javalang` với file đó, không
liên quan tới 3 file vừa sửa).

---

## 21. Vì sao Demon Lord không bao giờ summon được: `SummonReinforcementsGoal` chưa từng bị `new`

Sau khi sửa mục 20 (biên dịch được), Goal vẫn **không hoạt động** vì lý do khác hẳn: đọc lại
`DemonLordBossType.create()` thì `SummonReinforcementsGoal` **chưa từng được khởi tạo** ở đâu cả -
không nằm trong danh sách Goal của Phase 1, Phase 2/3, hay bookkeeping `allGoals` truyền vào
`BossType`. Class Goal này tồn tại nhưng không một dòng code nào từng gọi `new SummonReinforcementsGoal()`
- javadoc của chính nó tự nhận "chỉ thêm vào Phase 1's pool (xem boss_.md mục 20)", nhưng mục 20 đó
chưa từng được viết ở phiên trước (bị mục 20 khác - lỗi biên dịch - chiếm số), tức là bước "thêm vào
code" đã bị bỏ sót hoàn toàn khi viết javadoc trước khi code xong.

Hệ quả: Phase 1 (100%-60% máu) không bao giờ summon được (Goal không tồn tại để GOAP cân nhắc). Phase
2/3 vẫn summon được bình thường qua `EnhancedCombatStrategy`'s `summon_reinforcements_combo` Method
(HTN, không liên quan Goal này) - nên bug chỉ ảnh hưởng Phase 1.

**Đã sửa:** tạo instance `summonReinforcementsGoal`, thêm vào danh sách Goal của Phase 1 (cùng
`pressureTargetGoal`/`drainLifeGoal`/`furyGoal`/`defendWithShieldGoal`), **không** thêm vào `allGoals`
dùng chung cho Phase 2/3 (đúng ý đồ ban đầu - Phase 2+ đã có đường riêng qua HTN, không cần Goal này
chen vào tranh chấp với GOAP ở đó). Để bookkeeping/toString của `BossType` vẫn liệt kê đủ, tách thêm
`fullGoalCatalog` (= `allGoals` + `summonReinforcementsGoal`) và truyền list này vào constructor
`BossType` thay vì `allGoals` trần trụi - cùng tinh thần với cách `allSkills` đã xử lý 2 skill
HTN-only (`JudgmentSkill`/`DemonicAscensionSkill`).

Parse-verify lại toàn bộ 179 file: 0 lỗi mới.

---

*Cập nhật lần cuối: theo phiên làm việc hiện tại (đăng ký `SummonReinforcementsGoal` vào Phase 1's
pool - trước đó Goal này chưa từng được `new`, nên Demon Lord không bao giờ summon được ở Phase 1
dù code đã biên dịch được từ mục 20). File này nên được cập nhật mỗi khi có thay đổi lớn trong `Boss/`.*
