package com.chemiofitor.createcoolingfan;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateCoolingFan.MOD_ID)
public class CreateCoolingFan {
    public static final String MOD_ID = "createcoolingfan";
    public static final String NAME = "Create Cooling Fan";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public CreateCoolingFan() {
        LOGGER.info("Create Cooling Fan has loaded!.");
    }
}
