package com.voxshop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class VoxShop implements ModInitializer {
	public static final String MOD_ID = "voxshop";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Path dir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
		ShopConfig.load(dir);
		Economy.load(dir);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			ShopCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> Economy.save());
		LOGGER.info("VoxShop cargado: {} categorias", ShopConfig.get().categories.size());
	}
}
