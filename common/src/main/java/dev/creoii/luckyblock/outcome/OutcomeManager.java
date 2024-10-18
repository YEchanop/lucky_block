package dev.creoii.luckyblock.outcome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.creoii.luckyblock.LuckyBlockMod;
import dev.creoii.luckyblock.util.FunctionUtils;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class OutcomeManager extends JsonDataLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient().create();
    private Map<Identifier, JsonObject> randomOutcomes;
    private Map<Identifier, JsonObject> nonrandomOutcomes;
    private final Map<Pair<Outcome, Outcome.Context>, MutableInt> delays = Maps.newHashMap();

    public OutcomeManager() {
        super(GSON, "outcomes");
    }

    public Set<Identifier> getIds() {
        return randomOutcomes.keySet();
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
        ImmutableMap.Builder<Identifier, JsonObject> builder = ImmutableMap.builder();
        ImmutableMap.Builder<Identifier, JsonObject> builder1 = ImmutableMap.builder();
        for (Map.Entry<Identifier, JsonObject> entry : prepared.entrySet().stream().map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getAsJsonObject())).collect(Collectors.toSet())) {
            LuckyBlockMod.LOGGER.info("Loading outcome '{}'", entry.getKey());
            if (entry.getKey().getPath().startsWith("nonrandom/")) {
                builder1.put(entry.getKey(), entry.getValue());
            } else builder.put(entry.getKey(), entry.getValue());
        }
        randomOutcomes = builder.build();
        nonrandomOutcomes = builder1.build();
    }

    public void tickDelays(MinecraftServer server) {
        if (server.getTickManager().shouldTick()) {
            if (delays.isEmpty())
                return;

            List<Pair<Outcome, Outcome.Context>> toRemove = new ArrayList<>();

            delays.forEach((pair, integer) -> {
                if (integer.decrementAndGet() <= 0) {
                    pair.getLeft().run(pair.getRight());
                    toRemove.add(pair);
                }
            });

            toRemove.forEach(delays::remove);
        }
    }

    public void addDelay(Outcome outcome, Outcome.Context context, int delay) {
        delays.put(new Pair<>(outcome, context), new MutableInt(delay));
    }

    public boolean isEmpty() {
        return randomOutcomes.isEmpty();
    }

    @Nullable
    public JsonObject getOutcomeById(Identifier id) {
        if (nonrandomOutcomes.isEmpty()) {
            throw new IllegalArgumentException("No nonrandom outcomes found");
        }

        for (Map.Entry<Identifier, JsonObject> outcome : nonrandomOutcomes.entrySet()) {
            if (outcome.getKey().equals(id)) {
                return outcome.getValue();
            }
        }

        if (randomOutcomes.isEmpty()) {
            throw new IllegalArgumentException("No random outcomes found");
        }

        for (Map.Entry<Identifier, JsonObject> outcome : randomOutcomes.entrySet()) {
            if (outcome.getKey().equals(id)) {
                return outcome.getValue();
            }
        }

        throw new IllegalArgumentException("Outcome '" + id.toString() + "' does not exist");
    }

    public Pair<Identifier, JsonObject> getRandomOutcome(Random random, int luck) {
        if (randomOutcomes.isEmpty()) {
            throw new IllegalArgumentException("No outcomes found");
        }

        int lowest = 0, highest = 0;

        for (Map.Entry<Identifier, JsonObject> outcome : randomOutcomes.entrySet()) {
            int outcomeLuck;
            try {
                outcomeLuck = outcome.getValue().getAsJsonPrimitive("luck").getAsInt();
            } catch (Exception e) {
                outcomeLuck = 0;
            }

            if (outcomeLuck < lowest)
                lowest = outcomeLuck;
            if (outcomeLuck > highest)
                highest = outcomeLuck;
        }

        highest += -1 * lowest + 1;

        double totalWeight = 0d;
        List<Double> weights = new ArrayList<>();
        weights.add(0d);

        for (Map.Entry<Identifier, JsonObject> outcome : randomOutcomes.entrySet()) {
            int outcomeLuck;
            try {
                outcomeLuck = outcome.getValue().getAsJsonPrimitive("luck").getAsInt() + (-1 * lowest) + 1;
            } catch (Exception e) {
                outcomeLuck = (-1 * lowest) + 1;
            }

            double outcomeChance;
            try {
                outcomeChance = outcome.getValue().getAsJsonPrimitive("chance").getAsDouble();
            } catch (Exception e) {
                outcomeChance = 1d;
            }

            double adjusted = Math.pow(1d / (1d - Math.abs(luck) * .77d / 100d), luck >= 0 ? outcomeLuck : highest + 1 - outcomeLuck);
            weights.add(totalWeight += (outcomeChance > 0d ? outcomeChance : 1d) * adjusted * 100);
        }

        double rIndex = random.nextDouble() * totalWeight;
        int index = -1;
        for (int i = 0; i < weights.size() - 1; ++i) {
            if (rIndex >= weights.get(i) && rIndex < weights.get(i + 1)) {
                index = i;
            }
        }

        if (index == -1)
            index = weights.size() - 2;

        @SuppressWarnings("unchecked") Map.Entry<Identifier, JsonObject> entry = randomOutcomes.entrySet().toArray(new Map.Entry[]{})[index];
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    @Nullable
    public Outcome parseJsonOutcome(JsonObject object, Outcome.Context context) {
        JsonObject parsedObject = GSON.fromJson(FunctionUtils.parseString(object.toString(), context), JsonObject.class);
        DataResult<Outcome> dataResult = Outcome.CODEC.parse(JsonOps.INSTANCE, parsedObject);
        Optional<Outcome> outcome = dataResult.resultOrPartial(string -> LuckyBlockMod.LOGGER.error("Error parsing outcome: {}", string));
        return outcome.orElse(null);
    }
}
