package com.dndweapons;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DndWeaponsMod implements ModInitializer {
    public static final String MOD_ID = "dndweapons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("DnD Weapons initializing...");
    }
}
