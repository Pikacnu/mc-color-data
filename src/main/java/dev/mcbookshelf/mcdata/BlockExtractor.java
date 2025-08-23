package dev.mcbookshelf.mcdata;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlockExtractor {
    public static void generateBlockData(Path output) throws IOException {
        System.out.println("Generating block data...");
        Files.createDirectories(output);

        JsonObject data = extractBlocks();
        JsonUtils.writeJsonToFile(output.resolve("data.json"), data, true);
        JsonUtils.writeJsonToFile(output.resolve("data.min.json"), data, false);
    }

    private static JsonObject extractBlocks() {
        JsonObject data = new JsonObject();
        Registry<Block> blockRegistry = BuiltInRegistries.BLOCK;

        for (var entry : blockRegistry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();
            data.add(id.toString(), extractBlockData(block));
        }
        return data;
    }

    private static JsonObject extractBlockData(Block block) {
        JsonObject data = new JsonObject();
        JsonArray brightnessArray = new JsonArray();
        BlockState state = block.defaultBlockState();
        MapColor color = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

        if(block.getClass() == RotatedPillarBlock.class ) {
            JsonObject pillarData = new JsonObject();
            String[] AXIS = {"X", "Y", "Z"};
            for (String axis : AXIS) {
                BlockState axisState = state.setValue(RotatedPillarBlock.AXIS, Axis.valueOf(axis));
                MapColor axisColor = axisState.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                JsonObject axisData = new JsonObject();
                JsonArray axisBrightnessArray = new JsonArray();
                for (Brightness brightness : Brightness.values()) {
                    int baseColor = axisColor.col;
                    int modifiedColor = applyBrightness(baseColor, brightness.modifier);
                    axisBrightnessArray.add(modifiedColor);
                }
                axisData.add("brightness", axisBrightnessArray);
                pillarData.add(axis, axisData);
            }
            data.add("properties", pillarData);
        }

        for (Brightness brightness : Brightness.values()) {
            int baseColor = color.col;
            int modifiedColor = applyBrightness(baseColor, brightness.modifier);
            brightnessArray.add(modifiedColor);
        }

        data.add("brightness", brightnessArray);
        return data;
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
