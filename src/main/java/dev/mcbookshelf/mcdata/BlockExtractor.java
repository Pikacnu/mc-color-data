package dev.mcbookshelf.mcdata;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class BlockExtractor {
    public static void generateBlockData(Path output) throws IOException {
        System.out.println("Generating block data...");
        Files.createDirectories(output);

        JsonObject data = new JsonObject();
        JsonArray dataArray = extractBlocks();
        data.add("blocks", dataArray);
        JsonUtils.writeJsonToFile(output.resolve("data.json"), data, true);
        JsonUtils.writeJsonToFile(output.resolve("data.min.json"), data, false);
    }

    private static JsonArray extractBlocks() {
        JsonArray data = new JsonArray();
        Registry<Block> blockRegistry = BuiltInRegistries.BLOCK;

        for (var entry : blockRegistry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();
            extractBlockData(block).forEach(stateData ->{
                stateData.addProperty("blockId", id.toString());
                data.add(stateData);
            });

        }
        return data;
    }

    private static ArrayList<JsonObject> extractBlockData(Block block) {
        ArrayList<JsonObject> stateDataList = new ArrayList<>();

        JsonObject data = new JsonObject();
        JsonArray brightnessArray = new JsonArray();

        BlockState state = block.defaultBlockState();
        MapColor color = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

        for (Brightness brightness : Brightness.values()) {
            int baseColor = color.col;
            int modifiedColor = applyBrightness(baseColor, brightness.modifier);
            brightnessArray.add(modifiedColor);
        }

        data.add("brightness", brightnessArray);
        stateDataList.add(data);

        block.getStateDefinition().getPossibleStates().forEach(
            s -> {
                if (s != state) {
                    MapColor stateColor = s.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                    if (stateColor != color) {
                        JsonObject stateData = new JsonObject();
                        JsonObject properties = new JsonObject();
                        JsonArray subBrightnessArray = new JsonArray();
                        s.getValues().forEach((key, value) -> properties.addProperty(key.getName(), value.toString()));
                        stateData.add("properties", properties);
                        for(Brightness brightness : Brightness.values()) {
                            int modifiedColor = applyBrightness(stateColor.col, brightness.modifier);
                            subBrightnessArray.add(modifiedColor);
                        }
                        stateData.add("brightness", subBrightnessArray);
                        stateDataList.add(stateData);
                    }
                }
            }
        );
        return stateDataList;
    }

    private static int applyBrightness(int baseColor, int modifier) {
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        r = (r * modifier) / 255;
        g = (g * modifier) / 255;
        b = (b * modifier) / 255;

        return (r << 16) | (g << 8) | b;
    }
}
