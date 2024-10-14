package dev.creoii.luckyblock.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.creoii.luckyblock.util.LuckyBlockCodecs;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

public class SoundOutcome extends Outcome {
    public static final MapCodec<SoundOutcome> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(createGlobalLuckField(Outcome::getLuck),
                createGlobalChanceField(Outcome::getChance),
                createGlobalDelayField(Outcome::getDelay),
                createGlobalPosField(Outcome::getPos),
                SoundEvent.CODEC.fieldOf("sound_event").forGetter(outcome -> outcome.soundEvent),
                LuckyBlockCodecs.DOUBLE.fieldOf("volume").orElse("1").forGetter(outcome -> outcome.volume),
                LuckyBlockCodecs.DOUBLE.fieldOf("pitch").orElse("1").forGetter(outcome -> outcome.pitch)
        ).apply(instance, SoundOutcome::new);
    });
    private final SoundEvent soundEvent;
    private final String volume;
    private final String pitch;

    public SoundOutcome(int luck, float chance, Optional<Integer> delay, Optional<String> pos, SoundEvent soundEvent, String volume, String pitch) {
        super(OutcomeType.SOUND, luck, chance, delay, pos, false);
        this.soundEvent = soundEvent;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void run(OutcomeContext context) {
        Vec3d pos = getPos().isPresent() ? context.parseVec3d(getPos().get()) : context.pos().toCenterPos();

        double volume = context.parseDouble(this.volume);
        double pitch = context.parseDouble(this.pitch);
        double d = MathHelper.square(soundEvent.getDistanceToTravel((float) volume));

        List<ServerPlayerEntity> players = context.world().getServer().getPlayerManager().getPlayerList().stream().filter(serverPlayer -> {
            return pos.squaredDistanceTo(serverPlayer.getPos()) <= d;
        }).toList();

        long l = context.world().getRandom().nextLong();

        for (ServerPlayerEntity serverPlayer : players) {
            Vec3d vec3d;
            float j;
            while(true) {
                double e = pos.x - serverPlayer.getX();
                double f = pos.y - serverPlayer.getY();
                double g = pos.z - serverPlayer.getZ();
                double h = e * e + f * f + g * g;
                vec3d = pos;
                j = (float) volume;
                if (!(h > d)) {
                    break;
                }
            }

            serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(soundEvent), SoundCategory.NEUTRAL, vec3d.getX(), vec3d.getY(), vec3d.getZ(), j, (float) pitch, l));
        }
    }
}