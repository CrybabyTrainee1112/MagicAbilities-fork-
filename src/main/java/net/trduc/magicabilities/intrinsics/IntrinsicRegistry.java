package net.trduc.magicabilitiesfork.intrinsics;

import net.trduc.magicabilitiesfork.intrinsics.custom.AdrenalineIntrinsic;
import net.trduc.magicabilitiesfork.intrinsics.custom.BerserkIntrinsic;
import org.bukkit.entity.LivingEntity;

public final class IntrinsicRegistry {

    private IntrinsicRegistry() {
    }

    public static Intrinsic create(IntrinsicId id, LivingEntity owner) {
        switch (id) {
            case BERSERK_1:
                return BerserkIntrinsic.tier1(owner);
            case BERSERK_2:
                return BerserkIntrinsic.tier2(owner);
            case BERSERK_3:
                return BerserkIntrinsic.tier3(owner);
            case ADRENALINE_1:
                return AdrenalineIntrinsic.tier1(owner);
            case ADRENALINE_2:
                return AdrenalineIntrinsic.tier2(owner);
            case ADRENALINE_3:
                return AdrenalineIntrinsic.tier3(owner);
            case ADRENALINE_4:
                return AdrenalineIntrinsic.tier4(owner);
            case ADRENALINE_5:
                return AdrenalineIntrinsic.tier5(owner);
            default:
                throw new IllegalArgumentException("Unknown IntrinsicId: " + id);
        }
    }
}
