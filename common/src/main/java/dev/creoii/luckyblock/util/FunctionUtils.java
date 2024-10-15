package dev.creoii.luckyblock.util;

import com.google.common.collect.ImmutableMap;
import dev.creoii.luckyblock.outcome.Outcome;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionUtils {
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(\\w+)}");
    private static final Pattern MATH_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?([*/+-]\\d+(\\.\\d+)?)+");
    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("graal.js");
    public static final Map<String, Function<Outcome.Context, String>> STRING_PARAMS = new ImmutableMap.Builder<String, Function<Outcome.Context, String>>()
            .put("playerName", context -> context.player() == null ? "" : context.player().getGameProfile().getName())
            .put("playerUUID", context -> context.player() == null ? "" : String.valueOf(context.player().getUuidAsString()))
            .build();
    public static final Map<String, Function<Outcome.Context, Integer>> INT_PARAMS = new ImmutableMap.Builder<String, Function<Outcome.Context, Integer>>()
            .put("playerPosX", context -> context.player() == null ? context.pos().getX() : context.player().getBlockX())
            .put("playerPosY", context -> context.player() == null ? context.pos().getY() : context.player().getBlockY())
            .put("playerPosZ", context -> context.player() == null ? context.pos().getZ() : context.player().getBlockZ())
            .put("blockPosX", context -> context.pos().getX())
            .put("blockPosY", context -> context.pos().getY())
            .put("blockPosZ", context -> context.pos().getZ())
            .put("randomRGBColor", context -> context.world().getRandom().nextInt(16777215))
            .build();
    public static final Map<String, Function<Outcome.Context, Double>> DOUBLE_PARAMS = new ImmutableMap.Builder<String, Function<Outcome.Context, Double>>()
            .put("playerVecX", context -> context.player() == null ? context.pos().toCenterPos().getX() : context.player().getX())
            .put("playerVecY", context -> context.player() == null ? context.pos().toCenterPos().getY() : context.player().getY())
            .put("playerVecZ", context -> context.player() == null ? context.pos().toCenterPos().getZ() : context.player().getZ())
            .put("playerX", context -> context.player() == null ? context.pos().toCenterPos().getX() : context.player().getX())
            .put("playerY", context -> context.player() == null ? context.pos().toCenterPos().getY() : context.player().getY())
            .put("playerZ", context -> context.player() == null ? context.pos().toCenterPos().getZ() : context.player().getZ())
            .put("blockVecX", context -> context.pos().toCenterPos().getX())
            .put("blockVecY", context -> context.pos().toCenterPos().getY())
            .put("blockVecZ", context -> context.pos().toCenterPos().getZ())
            .put("blockX", context -> context.pos().toCenterPos().getX())
            .put("blockY", context -> context.pos().toCenterPos().getY())
            .put("blockZ", context -> context.pos().toCenterPos().getZ())
            .put("playerDistance", context -> context.player() == null ? 0d : context.player().getPos().distanceTo(context.pos().toCenterPos()))
            .put("playerSquaredDistance", context -> context.player() == null ? 0d : context.player().getPos().squaredDistanceTo(context.pos().toCenterPos()))
            .put("playerPitch", context -> context.player() == null ? 0d : context.player().getPitch())
            .put("playerYaw", context -> context.player() == null ? 0d : context.player().getYaw())
            .build();
    public static final Map<String, BiFunction<String[], Outcome.Context, String>> FUNCTIONS = new ImmutableMap.Builder<String, BiFunction<String[], Outcome.Context, String>>()
            .put("random", (args, context) -> String.valueOf(args[context.world().getRandom().nextInt(args.length)]))
            .put("randomBetween", FunctionUtils::getRandomBetween)
            .put("randomVelocity", FunctionUtils::getRandomVelocity)
            .build();
    private static final List<String> COLORS = List.of("brown", "red", "orange", "yellow", "lime", "green", "cyan", "blue", "light_blue", "pink", "magenta", "purple", "black", "gray", "light_gray", "white");
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "dark_oak", "acacia", "mangrove", "cherry");

    private static String getRandomBetween(String[] args, Outcome.Context context) {
        if (args.length != 2)
            throw new IllegalArgumentException("Function 'randomBetween' requires 2 arguments, found " + args.length);

        int from = Integer.parseInt(parseString(args[0], context));
        int to = Integer.parseInt(parseString(args[1], context));

        return String.valueOf(context.world().getRandom().nextBetween(from, to));
    }

    private static String getRandomVelocity(String[] args, Outcome.Context context) {
        if (args.length != 2 && args.length != 0)
            return "0";

        double power;
        double pitch;
        if (args.length == 0) {
            power = .9d;
            pitch = 15d;
        } else {
            power = Double.parseDouble(parseString(args[0], context));
            pitch = Double.parseDouble(parseString(args[1], context));
        }

        float yawRad = (float) Math.toRadians(context.world().getRandom().nextBetween(-180, 180));
        float pitchRad = (float) Math.toRadians(-90d + context.world().getRandom().nextBetween((int) -pitch, (int) pitch));

        Vec3d motion = new Vec3d(-MathHelper.sin(yawRad) * MathHelper.cos(pitchRad) * power, -MathHelper.sin(pitchRad) * power, MathHelper.cos(yawRad) * MathHelper.cos(pitchRad) * power);
        return motion.x + "," + motion.y + "," + motion.z;
    }

    /**
     * @param string a json object in string format
     * @return the string with all parameters and functions replaced with their values, based on the context
     */
    public static String parseString(String string, Outcome.Context context) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = PARAM_PATTERN.matcher(string);

        while (matcher.find()) {
            String param = matcher.group(1);

            if (FunctionUtils.STRING_PARAMS.containsKey(param)) {
                String replacement = FunctionUtils.STRING_PARAMS.get(param).apply(context);
                matcher.appendReplacement(result, replacement);
            } else if (FunctionUtils.DOUBLE_PARAMS.containsKey(param)) {
                Number numberValue = FunctionUtils.DOUBLE_PARAMS.get(param).apply(context);
                matcher.appendReplacement(result, String.valueOf(numberValue));
            } else if (FunctionUtils.INT_PARAMS.containsKey(param)) {
                Number numberValue = FunctionUtils.INT_PARAMS.get(param).apply(context);
                matcher.appendReplacement(result, String.valueOf(numberValue));
            } else throw new IllegalArgumentException("Error parsing param '" + param + "'");
        }

        return evaluateExpressions(matcher.appendTail(result).toString().replaceAll("\"([\\-\\d\\.]+)\"", "$1"));
    }

    private static String evaluateExpressions(String input) {
        Matcher matcher = MATH_PATTERN.matcher(input);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            try {
                matcher.appendReplacement(result, SCRIPT_ENGINE.eval(matcher.group()).toString());
            } catch (Exception e) {
                throw new IllegalArgumentException("Error evaluating math expression: " + matcher.group(), e);
            }
        }
        return matcher.appendTail(result).toString().replaceAll("\"([\\-\\d\\.]+)\"", "$1");
    }
}
