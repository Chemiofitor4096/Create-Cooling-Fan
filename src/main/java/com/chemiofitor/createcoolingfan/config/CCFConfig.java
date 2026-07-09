package com.chemiofitor.createcoolingfan.config;

import com.chemiofitor.createcoolingfan.CreateCoolingFan;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCFConfig {

    // --- Forge-managed config (simple values) ---
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.DoubleValue DEFAULT_FACTOR;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("Casting Fan Factors");
        DEFAULT_FACTOR = BUILDER
            .comment("Fallback factor for any FanProcessingType not explicitly listed in createcoolingfan-factors.json.")
            .defineInRange("defaultFactor", 0.5, -5.0, 5.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // --- Auto-generated per-type factors JSON ---
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FACTORS_FILE = FMLPaths.CONFIGDIR.get().resolve("createcoolingfan-factors.json");

    // Built-in defaults — seeded into the JSON on first run
    private static final LinkedHashMap<String, Double> BUILT_IN_DEFAULTS = new LinkedHashMap<>();
    static {
        BUILT_IN_DEFAULTS.put("create:splashing", 1.0);
        BUILT_IN_DEFAULTS.put("create:haunting",  0.5);
        BUILT_IN_DEFAULTS.put("create:smoking",   -0.3);
        BUILT_IN_DEFAULTS.put("create:blasting",  -0.5);
    }

    // --- Runtime cache ---
    private static final Map<FanProcessingType, Double> FACTOR_MAP = new HashMap<>();
    private static double cachedDefaultFactor;

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;

        cachedDefaultFactor = DEFAULT_FACTOR.get();
        FACTOR_MAP.clear();

        // 1. Read existing JSON (or start from built-in defaults)
        Map<String, Double> fileMap = readFactorFile();

        // 2. Scan all registered types — auto-add any not yet in the file
        boolean dirty = false;
        for (FanProcessingType type : FanProcessingTypeRegistry.getSortedTypesView()) {
            ResourceLocation id = FanProcessingTypeRegistry.getId(type);
            if (id == null) continue;

            String key = id.toString();
            if (!fileMap.containsKey(key)) {
                Double builtIn = BUILT_IN_DEFAULTS.get(key);
                fileMap.put(key, builtIn != null ? builtIn : cachedDefaultFactor);
                dirty = true;
            }
        }

        // 3. Write back if new types were discovered
        if (dirty) {
            writeFactorFile(fileMap);
        }

        // 4. Build runtime lookup
        for (FanProcessingType type : FanProcessingTypeRegistry.getSortedTypesView()) {
            ResourceLocation id = FanProcessingTypeRegistry.getId(type);
            Double factor = id != null ? fileMap.getOrDefault(id.toString(), cachedDefaultFactor) : cachedDefaultFactor;
            FACTOR_MAP.put(type, factor);
        }
    }

    public static double getFactor(FanProcessingType type) {
        if (type == null)
            return DEFAULT_FACTOR.get();
        return FACTOR_MAP.getOrDefault(type, cachedDefaultFactor);
    }

    // --- JSON file I/O ---

    private static Map<String, Double> readFactorFile() {
        if (!Files.exists(FACTORS_FILE)) {
            return new LinkedHashMap<>(BUILT_IN_DEFAULTS);
        }
        try (Reader reader = Files.newBufferedReader(FACTORS_FILE)) {
            Map<String, Double> map = GSON.fromJson(reader,
                new TypeToken<LinkedHashMap<String, Double>>() {}.getType());
            return map != null ? map : new LinkedHashMap<>();
        } catch (Exception e) {
            CreateCoolingFan.LOGGER.error("Failed to read {}, using defaults.", FACTORS_FILE, e);
            return new LinkedHashMap<>(BUILT_IN_DEFAULTS);
        }
    }

    private static void writeFactorFile(Map<String, Double> map) {
        try {
            Files.createDirectories(FACTORS_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FACTORS_FILE)) {
                GSON.toJson(map, writer);
            }
        } catch (Exception e) {
            CreateCoolingFan.LOGGER.error("Failed to write {}", FACTORS_FILE, e);
        }
    }
}
