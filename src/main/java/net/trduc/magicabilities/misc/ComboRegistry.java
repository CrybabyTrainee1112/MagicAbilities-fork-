package net.trduc.magicabilitiesfork.misc;

import net.trduc.magicabilitiesfork.powers.PowerType;

import java.util.*;

public final class ComboRegistry {
    private static final Map<PowerType, List<String>> combos = new EnumMap<>(PowerType.class);

    static {
        combos.put(PowerType.FIRE, Arrays.asList(
                "Fire + Wind — Area denial and sustained burn.",
                "Recommended triggers: Fire (Right Click), Wind (Sneak+Right Click)",
                "Sequence: Use Wind to group or push enemies into position → Cast Fire to ignite and maintain damage.",
                "Tuning: Reduce fire spread or increase cooldown to avoid excessive overlap."));

        combos.put(PowerType.WIND, Arrays.asList(
                "Wind + Fire — Force enemies into burning zones for maximum AoE.",
                "Recommended triggers: Wind (Right Click), Fire (Right Click)",
                "Sequence: Push with Wind → Ignite with Fire → Reapply Wind to keep them inside.",
                "Tuning: Limit friendly knockback and adjust cooldowns."));

        combos.put(PowerType.ICE, Arrays.asList(
                "Ice + Lightning — Crowd control into high single-target burst.",
                "Recommended triggers: Ice (Sneak / Left Click) then Lightning (Right Click)",
                "Sequence: Slow or freeze with Ice → Follow up with Lightning for heavy damage.",
                "Tuning: Keep freeze duration short enough for counterplay."));

        combos.put(PowerType.LIGHTNING, Arrays.asList(
                "Lightning + Ice — Use Lightning as finish after Ice control.",
                "Recommended triggers: Lightning (Right Click)",
                "Sequence: Start with control (Ice) → Finish with Lightning.",
                "Tuning: Limit Lightning chain length in grouped fights."));

        combos.put(PowerType.WATER, Arrays.asList(
                "Water + Earth — Trap and control groups.",
                "Recommended triggers: Water (Right Click), Earth (Right Click)",
                "Sequence: Use Water to slow or pull → Raise Earth barriers to trap enemies.",
                "Tuning: Limit barrier lifetime or size."));

        combos.put(PowerType.METEOR_LORD, Arrays.asList(
                "Meteor Lord + Shockwave + Fire — Siege and area clear.",
                "Recommended triggers: Meteor (Active), Shockwave (Close-range), Fire (AoE)",
                "Sequence: Call Meteor → Immediately use Shockwave to scatter survivors → Seal with Fire.",
                "Tuning: Add cast delay to Meteor to allow counterplay."));

        combos.put(PowerType.PHOENIX, Arrays.asList(
                "Phoenix + Fire — High DPS with sustain.",
                "Recommended triggers: Phoenix (Ultimate), Fire (AoE)",
                "Sequence: Engage with Phoenix to gain healing → Lay down Fire zones to maintain pressure.",
                "Tuning: Cap self-heal percentage and overlap damage."));

        combos.put(PowerType.DEMON, Arrays.asList(
                "Demon + Vampire/Blood — Sustain in prolonged fights.",
                "Recommended triggers: Demon (Debuff) + Vampire/Blood (Life-steal)",
                "Sequence: Apply Demon debuffs → Trade hits while using Vampire/Blood to recover health.",
                "Tuning: Cap life-steal and add stacking limits."));

        combos.put(PowerType.WARP, Arrays.asList(
                "Warp + Portal (+ Superior Warp) — Hit-and-run and flanking.",
                "Recommended triggers: Portal place (Passive) / Warp (Active)",
                "Sequence: Place Portal behind enemy → Warp in for attack → Warp out or Superior Warp for long escape.",
                "Tuning: Limit number of active portals and add cooldown on placement."));

        combos.put(PowerType.GRAVITY, Arrays.asList(
                "Gravity + Cloud — Vertical denial and aerial control.",
                "Recommended triggers: Cloud (Passive) / Gravity (Active)",
                "Sequence: Gain aerial advantage with Cloud → Use Gravity to force enemies to ground or trap them mid-air.",
                "Tuning: Reduce radius or duration to avoid total aerial lockdown."));

        combos.put(PowerType.NATURE, Arrays.asList(
                "Nature + Crystal/Magnetic — Support and zone control.",
                "Recommended triggers: Nature (Passive) / Crystal (Place) / Magnetic (Active)",
                "Sequence: Use Nature for roots/heals → Place Crystal cover → Use Magnetic to reposition enemies or items.",
                "Tuning: Limit passive heal range and duration."));

        combos.put(PowerType.CRYSTAL, Arrays.asList(
                "Crystal + Spike + Earth — Chokepoint traps and area denial.",
                "Recommended triggers: Crystal (Place) / Spike (Trap) / Earth (Barrier)",
                "Sequence: Shape the ground with Earth → Place Crystal barriers → Lay Spike traps at chokepoints.",
                "Tuning: Make Spike triggers conditional to avoid accidental kills."));

        combos.put(PowerType.CHRONARCH, Arrays.asList(
                "Chronarch + any burst power — guarantee a hit before it can be dodged.",
                "Recommended triggers: Stasis Lock (Left Click, slot3) then a hard-hitting Left/Right Click",
                "Sequence: Freeze the target with Stasis Lock → land your heaviest combo before it breaks free → Haste Surge to chase down the rest.",
                "Tuning: Stasis Lock only removes mobility, not incoming-damage reduction — watch for burst caps when pairing with execute-style powers."));

        combos.put(PowerType.PUPPETEER, Arrays.asList(
                "Puppeteer + any AoE/execute power — guarantee a puppet is standing exactly where you need it.",
                "Recommended triggers: String Shot (Left Click, slot0) to tag, Yank (slot1) or Puppet March (slot3) to reposition, then your burst.",
                "Sequence: Tag a priority target with String Shot → Yank it into your kill zone (or March it through allies for collision damage) → finish with Cut String for a distance-scaled burst.",
                "Tuning: String Ward only mitigates damage from currently-strung entities — pairing with self-damage ults (e.g. Zero Hour reflect-style effects) needs a separate cap."));

        combos.put(PowerType.KAGEMUSHA, Arrays.asList(
                "Kagemusha + any AoE/execute power — hit a foe that's already missing half its swings.",
                "Recommended triggers: move around to keep doubles topped up, Dance Away (Right Click, slot1) to tag+Dizzy, Clone Ambush (Left Click, slot0) or your burst to finish.",
                "Sequence: Keep at least 1 double alive (just walk) → Dance Away the target for Dizzy + a confirmed miss-chance window → Clone Ambush or your hardest hit while they're disoriented → Converge (Jump) to top off HP once the fight settles.",
                "Tuning: the passive miss-chance only checks while >=1 double is alive or the Dance Away target's Dizzy is active — a player who kills every double first removes the whole defensive layer."));

    }

    private ComboRegistry() {}

    public static List<String> getCombosFor(PowerType type){
        return combos.getOrDefault(type, Collections.singletonList("No optimized combos registered for " + type + ". Please consult combo.md or contribute one."));
    }
}

