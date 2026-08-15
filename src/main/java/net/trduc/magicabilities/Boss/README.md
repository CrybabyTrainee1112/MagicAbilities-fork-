# Boss System - Implementation Summary

Hệ thống Boss AI: **HTN (chiến lược) + GOAP (chiến thuật)** lai (hybrid), theo báo cáo thiết kế.

## 📋 Cấu Trúc Code

```
Boss/
├── ai/
│   ├── worldstate/         # Foundation: WorldKey, TargetKey, WorldState, Condition, Effect
│   ├── skill/              # Action Layer: Skill, SkillExecutor, AbstractSkill
│   ├── goal/                # Goal Layer (GOAP): Goal, AbstractGoal
│   ├── planner/             # GOAP Planning: ActionGraph, GraphBuilder, PlanNode, Planner (A*)
│   ├── htn/                 # HTN Planning: Task, CompoundTask, Method, PrimitiveTask
│   │                        #   (SkillTask/GoalTask), HTNPlanner, Strategy/AbstractStrategy
│   ├── sensor/               # Sensor: Sensor interface, SensorScope enum
│   ├── memory/               # Memory: stores world facts and cast history
│   ├── decision/             # Decision: GOAP goal selection with switch-hysteresis
│   └── executor/             # Brain: main AI loop, coordinates HTN + GOAP
├── core/                    # Framework: Boss, BossType, BossManager, BossFactory, BossRegistry
├── phase/                   # Phase System: PhaseDefinition (per-phase Goals AND Strategies)
├── threat/                  # Threat Management: ThreatTable with decay/cleanup
├── damage/                  # Damage: DamageAPI with phase multipliers
└── event/                   # Events: BossSpawnEvent, BossDeathEvent, BossPhaseChangeEvent
```

## 🎯 Vì sao HTN + GOAP (thay vì chỉ GOAP)

GOAP thuần (A* trên toàn bộ skill pool) rất linh hoạt nhưng **không thể diễn đạt thứ tự cố định
có chủ đích** dễ dàng (combo mở màn, cutscene ultimate) - phải giả lập bằng cost/precondition rất
gượng ép. HTN thuần thì ngược lại: rất tốt cho combo/kịch bản có cấu trúc, nhưng cứng nhắc khi cần
"đạt được mục tiêu X, không quan tâm cách nào" trong tình huống biến động.

**Giải pháp:** hai tầng, khớp nối tại một điểm rõ ràng:

- **Tầng chiến lược (HTN)** - `Strategy` tự đánh giá có nên "chen ngang" hay không (giống `Behavior`
  cũ), nhưng thay vì 1 chuỗi skill cứng, root của nó là 1 `CompoundTask` có thể phân rã khác nhau
  tùy world state (nhiều `Method`, mỗi `Method` có precondition riêng, thử theo thứ tự khai báo,
  backtrack nếu method thất bại giữa chừng).
- **Tầng chiến thuật (GOAP)** - không đổi, vẫn A* tìm skill sequence cho 1 Goal.
- **Điểm khớp nối: `GoalTask`** - một subtask trong `Method` có thể là `GoalTask(goal)`, nghĩa là
  "tới bước này, giao hẳn cho GOAP tự tìm cách đạt goal, không hardcode skill". Method vẫn có thể
  trộn `SkillTask` (bước cố định) và `GoalTask` (bước để GOAP tự lo) trong cùng 1 chuỗi.

## 🚀 Quick Start

### 1. Tạo Skill (Action - dùng chung cho cả GOAP và HTN SkillTask)

```java
public class ThunderStrikeSkill extends AbstractSkill {
    public ThunderStrikeSkill() {
        super(new Builder("thunder_strike")
            .cost(2.0)
            .precondition(Condition.lessThan(WorldStateKeys.NEAREST_THREAT_DISTANCE, 10))
            .effect(Effect.modifyValue(WorldStateKeys.BOSS_HEALTH_PERCENT, -0.15))
            .targetKey(WorldStateKeys.NEAREST_THREAT)
            .cooldownTicks(40)
        );
    }
    
    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        // Spawn particles, deal damage, etc.
    }
}
```

### 2. Tạo Goal (GOAP - mục tiêu tầng chiến thuật)

```java
public class KillTargetGoal extends AbstractGoal {
    public KillTargetGoal() {
        super("kill_target", "Tiêu diệt mục tiêu",
            Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 1)
        );
    }
    
    @Override
    public float assessPriority(WorldState worldState) {
        double dist = worldState.getValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, 100);
        return dist < 20 ? 0.8f : 0.3f;
    }
}
```

### 3. Tạo Strategy (HTN - tầng chiến lược, thay thế Behavior cũ)

```java
// Method A: nếu người chơi tụ cụm -> AoE burst rồi để GOAP dọn dẹp
Method aoeMethod = new Method("aoe_cluster",
    Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1),
    Arrays.asList(new SkillTask(new AoeBurstSkill()), new GoalTask(new KillTargetGoal()))
);

// Method B (fallback): không tụ cụm -> giao thẳng cho GOAP
Method defaultMethod = new Method("default_pressure",
    Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 0), // luôn đúng
    Collections.singletonList(new GoalTask(new KillTargetGoal()))
);

CompoundTask ultimateRoot = new CompoundTask("ultimate_root", Arrays.asList(aoeMethod, defaultMethod));

Strategy ultimateStrategy = new AbstractStrategy("ultimate", ultimateRoot, 0.95f) {
    @Override
    public boolean shouldActivate(SkillContext context) {
        return context.getBoss().getHealth() < context.getBoss().getMaxHealth() * 0.25;
    }
};
```

### 4. Tạo Sensor (Cảm biến)

```java
public class ThreatSensor implements Sensor {
    @Override
    public String getId() { return "threat_sensor"; }
    
    @Override
    public SensorScope getScope() { return SensorScope.LOCAL; }
    
    @Override
    public int getThrottleTicks() { return 5; }  // Update every ~250ms
    
    @Override
    public void sense(SkillContext context, WorldState worldState) {
        LivingEntity threat = context.getTarget();
        if (threat != null) {
            double dist = context.getBoss().getLocation().distance(threat.getLocation());
            worldState.setValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, dist);
        }
    }
}
```

### 5. Đăng ký Boss Type (Goals/Strategies giờ được gán theo từng Phase)

```java
List<Skill> skills = Arrays.asList(new ThunderStrikeSkill(), new AoeBurstSkill());
List<Goal> goals = Arrays.asList(new KillTargetGoal());
List<Strategy> strategies = Arrays.asList(ultimateStrategy);
List<Sensor> sensors = Arrays.asList(new ThreatSensor());

List<PhaseDefinition> phases = Arrays.asList(
    new PhaseDefinition(1, 1.0, goals, Collections.emptyList(), 1.0, "§c§lPhase 1!"),
    new PhaseDefinition(2, 0.5, goals, Collections.emptyList(), 1.2, "§c§lPhase 2 - Boss enraged!"),
    new PhaseDefinition(3, 0.0, goals, strategies, 1.5, "§4§lPhase 3 - ULTIMATE!")
    // ultimateStrategy chỉ khả dụng ở Phase 3 - đây là sự ràng buộc Phase<->AI thật,
    // không phải danh sách phẳng dùng chung cho cả trận như bản cũ.
);

ActionGraph graph = new GraphBuilder(skills).build(); // build 1 lần duy nhất

BossType stormBringer = new BossType(
    "storm_bringer", EntityType.EVOKER, "§6Storm Bringer", 500,
    graph, skills, goals, strategies, sensors, phases
);

BossFactory.registerBossType(stormBringer);
```

### 6. Spawn Boss

```java
BossFactory factory = new BossFactory(bossManager);
Boss boss = factory.spawn("storm_bringer", targetLocation); // tự fire BossSpawnEvent
```

### 7. Tick Loop (trong plugin)

```java
BukkitTask task = new BukkitRunnable() {
    @Override
    public void run() {
        bossManager.tickAll();
    }
}.runTaskTimer(plugin, 0, 1);
```

## 🎯 Luồng mỗi tick (Brain.tick)

```
1. Sensor (throttled 4-10 ticks, clamp >=1 an toàn)
   ↓
2. Resolve Goals/Strategies khả dụng theo Phase hiện tại (fallback = full catalog của BossType)
   ↓
3. Strategy nào ưu tiên cao nhất & shouldActivate() = true, có đang cao hơn Strategy đang chạy?
   -> HTNPlanner.decompose(rootTask, worldState) MỘT LẦN khi activate (không mỗi tick)
   ↓
4a. Có Strategy đang chạy: pop PrimitiveTask kế tiếp
       - SkillTask -> cast thẳng qua SkillExecutor
       - GoalTask  -> giao cho Decision+Planner (GOAP) tới khi Goal.isComplete()
4b. Không có Strategy: Decision chọn Goal ưu tiên cao nhất (có hysteresis 20% chống
    flip-flop, Decision#shouldSwitchGoal - đã nối vào Brain, trước đây là dead code)
    -> Planner (async A*) -> Executor cast skill kế tiếp trong plan
```

## 💡 Nguyên tắc thiết kế chính

✅ **One-directional dependency**: Core → Boss (never reverse)
✅ **WorldState encapsulation**: bọc riêng, tối ưu sau (bitmask...) không phá Skill/Goal/Planner
✅ **Build graph/network once**: ActionGraph (GraphBuilder) build 1 lần lúc đăng ký boss type;
   CompoundTask/Method là cấu trúc bất biến định nghĩa sẵn, không cần builder riêng
✅ **Async GOAP planning**: Planner chạy async, Executor luôn cast sync
✅ **Throttled sensors**: không mỗi tick, 4-10 tick/lần, clamp an toàn nếu sensor khai báo sai
✅ **Dirty flag replanning**: chỉ replan GOAP khi world state thay đổi đáng kể
✅ **Hybrid HTN+GOAP thật**: `GoalTask` là điểm khớp nối tường minh, không phải "Behavior chen
   ngang" mù mờ như trước - HTN quyết định chiến lược, GOAP quyết định chiến thuật
✅ **Phase ràng buộc thật**: PhaseDefinition mang Goal/Strategy riêng theo từng phase, Boss tự
   chuyển phase theo %máu mỗi tick (không cần gọi tay `updatePhase()`), fire BossPhaseChangeEvent

## 📊 Performance

- **Sensor update**: 5-10 ticks (~250-500ms)
- **GOAP Planner search**: Async, max 20 depth, max 1000 nodes
- **HTN decomposition**: chạy 1 lần mỗi khi Strategy activate (không mỗi tick), max depth 12,
  max 500 task expanded
- **Memory footprint**: ~1KB per boss (worldstate + cast history)
- **Threat decay**: Automatic cleanup mỗi 5 seconds

## 🐛 Debug Commands (cần implement - chưa làm, xem boss_.md mục 3)

```
/boss debug <uuid>    # In ra Strategy/decomposition hoặc Goal/plan hiện tại
/boss list            # Liệt kê tất cả boss
/boss threat <uuid>   # Xem threat table
```

## 📝 Cấu hình (bosses.yml)

Vẫn chưa data-driven (xem boss_.md) - throttle numbers, MAX_PLAN_DEPTH, MAX_DECOMPOSITION_DEPTH...
vẫn hardcode, cần đọc từ `bosses.yml` sau khi có bộ khung config-loader giống Power.

---

**Status**: ✅ HTN+GOAP hybrid core hoàn chỉnh, 4 bug đã sửa, Phase thật sự ràng buộc AI, có 1 boss cụ thể (Demon Lord, xem `boss_.md` mục 10)
**Next**: xem `boss_.md` mục 3/8 (chưa nối plugin lifecycle - đây là việc còn lại quan trọng nhất)
