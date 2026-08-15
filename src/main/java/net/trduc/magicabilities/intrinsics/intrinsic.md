# Hệ thống Intrinsic — Kế hoạch & Kiến trúc

> File kế hoạch cho hệ thống **Intrinsic** — cùng tinh thần với `Boss/boss_.md` (audit).
> Cập nhật: core engine + 2 intrinsic đầu tiên đã được code — "Cuồng sát" I/II/III (mục 6) và
> "Adrenaline" I-V (mục 7).

---

## 1. Mục tiêu

Intrinsic là hệ thống **nội tại** dùng chung cho cả **boss** và **player**:

- Gồm **2 thành phần**: (a) chỉ số cộng thêm cố định (stat modifier — kháng nguyên tố, tốc độ,
  máu, damage%...) và (b) khả năng bẩm sinh (passive ability — kích hoạt tự động theo sự kiện,
  không cần học/luyện).
- **Boss**: intrinsic gắn cố định theo loại boss ngay từ lúc spawn, không đổi.
- **Player**: không có sẵn — phải giết boss để rớt ra **Intrinsic Book**, sau đó lắp (equip) vào
  **kho Intrinsic** của bản thân để kích hoạt và sử dụng.

---

## 2. Kiến trúc đề xuất

Package: `net.trduc.magicabilitiesfork.intrinsics`, ngang hàng với `powers/`, `Boss/`, `data/`.

```
intrinsics/
├── Intrinsic.java              ✅ đã code — abstract class, owner-bound (như Power)
├── IntrinsicId.java             ✅ đã code — enum: BERSERK_1-3, ADRENALINE_1-5
├── IntrinsicRegistry.java       ✅ đã code — factory IntrinsicId -> Intrinsic instance
├── IntrinsicManager.java        ✅ đã code — runtime: equip/unequip/getActive/tickAll (thay cho IntrinsicHolder)
├── IntrinsicListener.java       ✅ đã code — bridge EntityDamageByEntityEvent/EntityDamageEvent/EntityDeathEvent -> hook
├── IntrinsicStatModifier.java   ✅ đã code — wrap AttributeModifier (chưa có intrinsic nào dùng tới)
├── DamageCategory.java          ✅ đã code — phân loại sát thương vật lí vs phi vật lí (dùng chung)
├── custom/
│   ├── BerserkIntrinsic.java    ✅ đã code — "Cuồng sát" I/II/III (xem mục 6)
│   └── AdrenalineIntrinsic.java ✅ đã code — "Adrenaline" I-V (xem mục 7)
├── item/IntrinsicBook.java       ⏳ chưa code — item rớt từ boss
├── player/PlayerIntrinsicStorage ⏳ chưa code — kho học được, persistence SQLite
└── gui/IntrinsicGui.java         ⏳ chưa code — GUI học/kích hoạt

commands/IntrinsicCommand.java   ✅ đã code — /intrinsic give|clear|list (TẠM THỜI, thay cho Book/GUI)
```

### Chi tiết những gì đã code

**`Intrinsic` (core):** owner-bound (giống `Power` nhận `Player owner` trong constructor), hook:
`onEquip()`, `onUnequip()`, `onKill(victim)`, `onDamageDealt(victim, baseDamage) -> double`,
`onDamaged(EntityDamageEvent)`, `onTick()`, `getStatModifiers()`.

**`IntrinsicManager`:** map `UUID -> List<Intrinsic>` đang active trên từng entity (boss hoặc
player). Đây là seam mà hệ Book/kho/GUI (chưa code) sẽ gọi vào sau này qua `equip()`/`unequip()`.

**`IntrinsicListener`:** hook `EntityDamageByEntityEvent` (sửa damage ra qua `onDamageDealt`,
priority NORMAL), `EntityDamageEvent` (gọi `onDamaged`, priority **HIGH** - không dùng MONITOR vì
Adrenaline cần `setCancelled()` để miễn sát thương, MONITOR chỉ nên dùng để quan sát chứ không
sửa kết quả), và tự theo dõi "ai đánh trúng cuối cùng" theo UUID để suy ra kill trên
`EntityDeathEvent` — **không dùng** `LivingEntity#getKiller()` vì API đó chỉ set cho Player, boss
(custom mob) giết gì đó sẽ không được ghi nhận nếu dùng field đó.

**`IntrinsicCommand` (`/intrinsic`):** cách tạm để test — chưa có Book/GUI thật. Quyền
`magic.admin`. `give <player> <id>`, `clear <player>`, `list <player>`.

---

## 3. Đã chốt

- Intrinsic = chỉ số + khả năng bẩm sinh (cả hai).
- Boss: cố định theo loại, gắn từ lúc spawn.
- Player: không có sẵn, phải giết boss lấy Intrinsic Book, lắp vào kho để kích hoạt.

## 4. Câu hỏi còn mở (chưa chốt — Book/kho/GUI vẫn đang chờ)

1. **Giới hạn slot kích hoạt**: player active bao nhiêu intrinsic cùng lúc?
2. **Tỷ lệ rớt Intrinsic Book**: 100% hay % ngẫu nhiên? Có tính theo `BossMastery` tier không?
3. **1 boss ↔ nhiều intrinsic?**: Demon Lord sẽ gắn với đúng "Cuồng sát" nào (I/II/III), hay rớt
   ngẫu nhiên theo mastery tier?
4. **Trùng lặp**: giết cùng boss nhiều lần thì Book thừa xử lý sao?
5. **Gỡ kích hoạt (unequip)**: được đổi qua lại hay 1 lần là vĩnh viễn? (engine đã hỗ trợ sẵn
   `unequip()`, chỉ thiếu lớp nghiệp vụ quyết định khi nào cho phép gọi)
6. **Cách kích hoạt**: GUI riêng hay lệnh?

## 5. Bước tiếp theo

1. ~~Viết skeleton core (`Intrinsic`, `IntrinsicId`, `IntrinsicManager`, `IntrinsicRegistry`)~~ ✅
2. ~~Wiring runtime engine (listener chuyển event -> hook)~~ ✅
3. Viết `IntrinsicBook` (item) + `IntrinsicDropListener` (hook `BossDeathEvent`).
4. Viết `PlayerIntrinsicStorage` + `IntrinsicStore` (SQLite, tự chứa như `BossMasteryStore`).
5. (Tuỳ) GUI kích hoạt, thay `/intrinsic` command tạm thời.
6. Gắn "Cuồng sát" (I/II/III?) vào Demon Lord một khi câu hỏi #3 được chốt.

---

## 6. Intrinsic đã code: "Cuồng sát" I / II / III

**File:** `intrinsics/custom/BerserkIntrinsic.java`, đăng ký trong `IntrinsicRegistry` với
`IntrinsicId.BERSERK_1` / `BERSERK_2` / `BERSERK_3`.

**Cơ chế:**
- Chỉ kích hoạt (tính stack) khi chủ sở hữu đang **dưới 50% máu** tại thời điểm hạ gục mục tiêu.
- Mỗi lần giết 1 mob hoặc 1 sinh linh (LivingEntity bất kỳ) trong lúc dưới 50% máu → **+1 stack**,
  cộng dồn với các stack hiện có, và **làm mới đồng hồ hết hạn** về "D giây kể từ bây giờ".
- Damage ra (mọi đòn đánh, không riêng gì đòn giết) được cộng thêm `stacks * bonusPerStack`
  điểm sát thương hiệu quả (flat add), miễn là còn ít nhất 1 stack.
- Nếu không giết thêm gì trước khi hết D giây kể từ lần giết gần nhất → **toàn bộ stack mất sạch
  cùng lúc** (không giảm dần từng cái một) — đúng theo mô tả "nếu không giết thêm hiệu ứng nội
  tại hết".

| Tier | Bonus / stack | Thời hạn (reset mỗi kill) |
|------|---------------|----------------------------|
| I    | +1            | 10s                        |
| II   | +2            | 11s                        |
| III  | +3            | 12s                        |

**Test nhanh (chưa có Book/GUI):**
```
/intrinsic give <player> BERSERK_1
```
rồi để máu dưới 50%, đi giết mob — action bar sẽ báo số stack + bonus hiện tại.

**Giả định đã đưa ra khi code (cần bạn xác nhận lại nếu sai):**
- "Điểm sát thương hiệu quả" hiểu là **flat damage add** lên đòn đánh ra (không phải % tăng
  damage, không phải absorption/heal).
- Bonus áp dụng cho **mọi đòn đánh trong lúc còn stack**, không riêng đòn kế tiếp đòn giết.
- Điều kiện "dưới 50% máu" chỉ cần đúng **tại thời điểm giết** để cộng stack; đã có stack rồi thì
  bonus damage vẫn áp dụng kể cả nếu máu hồi lại trên 50% trong lúc timer còn chạy.
- "Sinh linh" = bất kỳ `LivingEntity` nào (mob, player, boss...), không giới hạn passive/hostile.

---

## 7. Intrinsic đã code: "Adrenaline" I / II / III / IV / V

**File:** `intrinsics/custom/AdrenalineIntrinsic.java`, đăng ký trong `IntrinsicRegistry` với
`IntrinsicId.ADRENALINE_1` .. `ADRENALINE_5`.

**Cơ chế:**
- Khi một đòn đánh làm máu chủ sở hữu **rơi vào khoảng nguy cấp** (1-3 tim cho tier I-III, 1-2 tim
  cho tier IV-V) mà lúc đó **chưa đang miễn sát thương** → kích hoạt miễn sát thương vật lí trong
  D giây kể từ lúc đó.
- Nếu chính đòn vừa kích hoạt là **sát thương vật lí**, đòn đó cũng bị vô hiệu hoá ngay (cú "cứu
  mạng" - đòn lẽ ra đẩy máu xuống mức nguy cấp bị miễn luôn). Nếu đòn kích hoạt là **phi vật lí**
  (lửa/độc/phép...) thì đòn đó vẫn gây damage bình thường — chỉ các đòn vật lí *sau đó* trong D
  giây mới được miễn.
- Trong lúc còn hiệu lực: mọi sát thương vật lí tiếp theo bị miễn hoàn toàn, không giảm dần,
  không refresh thêm thời gian khi bị đánh tiếp.
- Đòn đủ mạnh để giết ngay từ trên mức nguy cấp (bỏ qua hẳn khoảng 1-3/1-2 tim, ví dụ đủ máu rồi
  ăn 1 đòn chí mạng chết luôn) thì **không được cứu** — Adrenaline chỉ bắt đúng khoảnh khắc máu
  "rơi vào" khoảng nguy cấp, không phải bùa bất tử.
- **Hồi chiêu 1000s sau mỗi lần kích hoạt** — trong lúc hồi chiêu, máu rơi vào khoảng nguy cấp
  không kích hoạt lại được. (Đã chặn luôn vụ DOT retrigger liên tục nêu ở bản trước.)

| Tier | Khoảng máu kích hoạt | Thời hạn miễn | Hồi chiêu |
|------|----------------------|-----------------|-----------|
| I    | 1-3 tim              | 5s              | 1000s     |
| II   | 1-3 tim              | 6s              | 1000s     |
| III  | 1-3 tim              | 7s              | 1000s     |
| IV   | 1-2 tim              | 8s              | 1000s     |
| V    | 1-2 tim              | 9s              | 1000s     |

**Test nhanh:**
```
/intrinsic give <player> ADRENALINE_1
```
rồi để bị đánh xuống 1-3 tim (physical) — đòn đó sẽ bị vô hiệu, action bar báo miễn sát thương.

**Giả định đã đưa ra khi code (cần bạn xác nhận lại nếu sai):**
- **"Sát thương vật lí"** = melee (`ENTITY_ATTACK`), sweep attack, projectile, thorns, đá/block
  rơi, ngã (`FALL`), nổ (`BLOCK_EXPLOSION`/`ENTITY_EXPLOSION`), va chạm (`CONTACT`/`CRAMMING`).
  **Phi vật lí** = lửa, dung nham, chết đuối, ngạt, đói, độc, wither, sét, phép, hư không... Danh
  sách nằm trong `DamageCategory.PHYSICAL_CAUSES`, sửa trực tiếp ở đó nếu cần đổi.
- "1 tim" = 2 HP; khoảng "1→3 tim" hiểu là máu sau đòn đánh nằm trong `(0, 6]` HP (còn sống, tối
  đa 3 tim); "1→2 tim" là `(0, 4]` HP.
- Miễn sát thương = **hủy toàn bộ đòn đánh** (`event.setCancelled(true)`), không phải giảm % hay
  hồi máu bù lại.
- Trigger chỉ tính khi máu **rơi vào** khoảng nguy cấp lúc CHƯA miễn VÀ không đang hồi chiêu —
  nếu đang miễn hoặc đang hồi chiêu mà bị đánh (kể cả phi vật lí) đẩy máu xuống thấp hơn nữa,
  không có gì được kích hoạt/gia hạn thêm.
- Hồi chiêu 1000s tính từ **lúc kích hoạt**, không phải từ lúc hết miễn — nên tổng thời gian giữa
  2 lần cứu mạng liên tiếp tối thiểu là ~1000s (khoảng D giây miễn nằm trong 1000s đó).

---

## 8. Bug: `getStatModifiers()` được khai báo nhưng chưa từng được `IntrinsicManager` áp dụng

Rà soát phát hiện: `Intrinsic.getStatModifiers()` được doc ở mục 2 mô tả là "áp qua Bukkit
`AttributeInstance` khi equip (player) hoặc spawn (boss)", nhưng `IntrinsicManager.equip()` /
`unequip()` / `unequipAll()` / `clearAll()` trước đây chỉ gọi `onEquip()`/`onUnequip()` - không hề
đọc `getStatModifiers()` để add/remove `AttributeModifier` thật. Vô hại hiện tại vì Berserk/Adrenaline
đều không override (list rỗng mặc định), nhưng là bẫy chờ sẵn cho intrinsic tiếp theo có stat modifier
- cùng dạng lỗi "effect khai báo nhưng không ai đọc" đã gặp ở `Boss/boss_.md` mục 18.

**Đã sửa:** `IntrinsicManager` giờ tự áp/gỡ `AttributeModifier` thật ở `equip()`/`unequip()`/
`unequipAll()`/`clearAll()`, dùng `IntrinsicStatModifier.getModifierId()` (UUID cố định, có sẵn từ
trước) để tái tạo đúng `AttributeModifier` cần gỡ, không cần track riêng danh sách đã áp. `clearAll()`
lấy owner qua `Intrinsic#getOwner()` của chính intrinsic (không cần thêm map UUID -> LivingEntity).
Không đổi hành vi của Berserk/Adrenaline (cả hai vẫn trả `getStatModifiers()` rỗng mặc định).

Parse-verify `IntrinsicManager.java` bằng `javalang`: OK.

