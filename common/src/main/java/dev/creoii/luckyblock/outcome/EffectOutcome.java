package dev.creoii.luckyblock.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.creoii.luckyblock.util.position.VecProvider;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Optional;

public class EffectOutcome extends Outcome {
    public static final MapCodec<EffectOutcome> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(createGlobalLuckField(Outcome::getLuck),
                createGlobalChanceField(Outcome::getChance),
                createGlobalDelayField(Outcome::getDelay),
                createGlobalPosField(Outcome::getPos),
                StatusEffectInstance.CODEC.fieldOf("status_effect").forGetter(outcome -> outcome.statusEffectInstance)
        ).apply(instance, EffectOutcome::new);
    });
    private final StatusEffectInstance statusEffectInstance;

    public EffectOutcome(int luck, float chance, Optional<Integer> delay, Optional<VecProvider> pos, StatusEffectInstance statusEffectInstance) {
        super(OutcomeType.EFFECT, luck, chance, delay, pos, false);
        this.statusEffectInstance = statusEffectInstance;
    }

    @Override
    public void run(Context context) {
        context.player().addStatusEffect(statusEffectInstance);
    }
}
