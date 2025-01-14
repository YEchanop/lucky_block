package dev.creoii.luckyblock.neoforge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.creoii.luckyblock.LuckyBlockContainer;
import dev.creoii.luckyblock.LuckyBlockManager;
import dev.creoii.luckyblock.LuckyBlockMod;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class NeoForgeLuckyBlockManager extends LuckyBlockManager {
    @Override
    public Map<String, LuckyBlockContainer> init() {
        ImmutableMap.Builder<String, LuckyBlockContainer> builder = ImmutableMap.builder();
        ModList.get().getModFiles().forEach(modFileInfo -> {
            if (!getIgnoredMods().contains(modFileInfo.moduleName())) {
                try {
                    Path root = modFileInfo.getFile().getSecureJar().getPath("data");
                    if (Files.exists(root)) {
                        Files.walk(root).forEach(path -> {
                            if (PATH_PATTERN.matcher(path.toString()).matches()) {
                                try {
                                    String fileContent = Files.readString(path);
                                    JsonElement element = JsonParser.parseString(fileContent);
                                    if (element.isJsonObject()) {
                                        DataResult<LuckyBlockContainer> dataResult = LuckyBlockContainer.CODEC.parse(JsonOps.INSTANCE, element);
                                        dataResult.resultOrPartial(error -> LuckyBlockMod.LOGGER.error("Error parsing lucky block container: {}", error)).ifPresent(container -> {
                                            LuckyBlockMod.LOGGER.info("Loaded lucky block container '{}'", container.getId().getNamespace());
                                            builder.put(container.getId().getNamespace(), container);
                                        });
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load resources from JAR: " + modFileInfo.getFile().getFilePath(), e);
                }
            }
        });
        return builder.build();
    }

    @Override
    public List<String> getIgnoredMods() {
        return new ImmutableList.Builder<String>()
                .add("minecraft").add("neoforge").add("architectury")
                .build();
    }
}
