package com.voxshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Economia propia: saldo por jugador guardado en balances.json. */
public final class Economy {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, Double>>() {}.getType();
	private static final Map<UUID, Double> BALANCES = new HashMap<>();
	private static Path file;

	private Economy() {}

	public static synchronized void load(Path dir) {
		file = dir.resolve("balances.json");
		BALANCES.clear();
		if (!Files.exists(file)) return;
		try {
			Map<String, Double> raw = GSON.fromJson(Files.readString(file), MAP_TYPE);
			if (raw != null) {
				raw.forEach((k, v) -> {
					try {
						BALANCES.put(UUID.fromString(k), v);
					} catch (IllegalArgumentException ignored) {
					}
				});
			}
		} catch (IOException | RuntimeException e) {
			VoxShop.LOGGER.error("No se pudo leer balances.json", e);
		}
	}

	public static synchronized void save() {
		if (file == null) return;
		try {
			Files.createDirectories(file.getParent());
			Map<String, Double> raw = new HashMap<>();
			BALANCES.forEach((k, v) -> raw.put(k.toString(), v));
			Files.writeString(file, GSON.toJson(raw, MAP_TYPE));
		} catch (IOException e) {
			VoxShop.LOGGER.error("No se pudo guardar balances.json", e);
		}
	}

	private static double round(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

	public static synchronized double get(UUID id) {
		return BALANCES.getOrDefault(id, ShopConfig.get().startingBalance);
	}

	public static synchronized void set(UUID id, double amount) {
		BALANCES.put(id, round(Math.max(0, amount)));
		save();
	}

	public static synchronized void add(UUID id, double amount) {
		set(id, get(id) + amount);
	}

	/** Resta si hay saldo suficiente. */
	public static synchronized boolean take(UUID id, double amount) {
		double cur = get(id);
		if (cur + 0.0001 < amount) return false;
		set(id, cur - amount);
		return true;
	}

	public static String format(double v) {
		return ShopConfig.get().currencySymbol + String.format(Locale.ROOT, "%,.2f", v);
	}
}
