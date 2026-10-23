package com.voxshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuracion de la tienda (shop.json). Si no existe, se genera una por defecto.
 * buy < 0  -> no se puede comprar.   sell < 0 -> no se puede vender.
 */
public class ShopConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ShopConfig instance = new ShopConfig();
	private static Path file;

	public String title = "Tienda";
	public String currencySymbol = "$";
	public double startingBalance = 1000.0;
	public List<Category> categories = new ArrayList<>();

	public static class Category {
		public String name;
		public String icon;
		public List<Entry> items = new ArrayList<>();

		public transient Item iconItem = Items.CHEST;
		public transient List<Resolved> resolved = new ArrayList<>();
	}

	public static class Entry {
		public String item;
		public double buy;
		public double sell;

		public Entry() {}

		public Entry(String item, double buy, double sell) {
			this.item = item;
			this.buy = buy;
			this.sell = sell;
		}
	}

	public record Resolved(Item item, double buy, double sell) {}

	public static ShopConfig get() {
		return instance;
	}

	public static void load(Path dir) {
		file = dir.resolve("shop.json");
		ShopConfig cfg = null;
		try {
			Files.createDirectories(dir);
			if (Files.exists(file)) {
				cfg = GSON.fromJson(Files.readString(file), ShopConfig.class);
			}
		} catch (IOException | RuntimeException e) {
			VoxShop.LOGGER.error("shop.json invalido, usando valores por defecto", e);
		}
		if (cfg == null || cfg.categories == null) {
			cfg = defaults();
			try {
				Files.writeString(file, GSON.toJson(cfg));
			} catch (IOException e) {
				VoxShop.LOGGER.error("No se pudo escribir shop.json", e);
			}
		}
		cfg.resolveAll();
		instance = cfg;
	}

	public static void reload() {
		load(file.getParent());
	}

	private void resolveAll() {
		for (Category c : categories) {
			c.iconItem = lookup(c.icon);
			if (c.iconItem == null) c.iconItem = Items.CHEST;
			c.resolved = new ArrayList<>();
			if (c.items == null) continue;
			for (Entry e : c.items) {
				Item it = lookup(e.item);
				if (it == null) {
					VoxShop.LOGGER.warn("Item desconocido en shop.json: {}", e.item);
					continue;
				}
				c.resolved.add(new Resolved(it, e.buy, e.sell));
			}
		}
	}

	private static Item lookup(String id) {
		if (id == null) return null;
		Identifier ident = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
		if (ident == null) return null;
		return BuiltInRegistries.ITEM.getOptional(ident).orElse(null);
	}

	// ---------- valores por defecto (estilo skyblock) ----------

	private static Category cat(String name, String icon, Entry... entries) {
		Category c = new Category();
		c.name = name;
		c.icon = icon;
		c.items = new ArrayList<>(List.of(entries));
		return c;
	}

	private static Entry e(String id, double buy, double sell) {
		return new Entry(id, buy, sell);
	}

	private static ShopConfig defaults() {
		ShopConfig c = new ShopConfig();
		c.categories.add(cat("Bloques", "grass_block",
			e("stone", 8, 1), e("cobblestone", 4, 0.5), e("dirt", 2, 0.2), e("sand", 4, 0.5),
			e("gravel", 4, 0.5), e("oak_log", 12, 2), e("spruce_log", 12, 2), e("birch_log", 12, 2),
			e("jungle_log", 14, 2), e("acacia_log", 12, 2), e("dark_oak_log", 12, 2),
			e("oak_planks", 4, 0.5), e("glass", 10, 1), e("bricks", 20, 3), e("sandstone", 8, 1),
			e("netherrack", 4, 0.5), e("obsidian", 200, 20), e("ice", 20, 2), e("clay", 12, 2),
			e("snow_block", 8, 1), e("end_stone", 30, 4)));
		c.categories.add(cat("Minerales", "diamond",
			e("coal", 20, 3), e("copper_ingot", 25, 3), e("iron_ingot", 60, 8), e("gold_ingot", 90, 12),
			e("redstone", 15, 2), e("lapis_lazuli", 15, 2), e("quartz", 20, 3), e("diamond", 400, 60),
			e("emerald", 250, 40), e("netherite_ingot", 5000, -1)));
		c.categories.add(cat("Granja", "wheat",
			e("wheat", 8, 1), e("wheat_seeds", 4, 0.2), e("carrot", 8, 1), e("potato", 8, 1),
			e("beetroot", 8, 1), e("melon_slice", 4, 0.5), e("pumpkin", 10, 1.5), e("sugar_cane", 6, 1),
			e("cactus", 6, 1), e("bamboo", 4, 0.5), e("cocoa_beans", 10, 1.5), e("nether_wart", 20, 3),
			e("kelp", 4, 0.5), e("sweet_berries", 6, 1), e("bone_meal", 10, 1)));
		c.categories.add(cat("Mobs", "bone",
			e("rotten_flesh", -1, 1), e("bone", 12, 2), e("string", 12, 2), e("spider_eye", 15, 2),
			e("gunpowder", 25, 4), e("ender_pearl", 150, 20), e("blaze_rod", 80, 10),
			e("slime_ball", 30, 4), e("ghast_tear", 200, 30), e("leather", 15, 2), e("feather", 8, 1),
			e("ink_sac", 10, 1.5), e("phantom_membrane", 60, 8), e("magma_cream", 40, 5)));
		c.categories.add(cat("Comida", "cooked_beef",
			e("bread", 10, 1.5), e("cooked_beef", 20, 3), e("cooked_porkchop", 20, 3),
			e("cooked_chicken", 15, 2), e("cooked_mutton", 15, 2), e("baked_potato", 10, 1.5),
			e("apple", 8, 1), e("golden_carrot", 40, 6), e("golden_apple", 300, 40),
			e("cake", 60, 8), e("pumpkin_pie", 25, 3), e("cookie", 4, 0.5)));
		c.categories.add(cat("Utilidades", "crafting_table",
			e("torch", 4, 0.5), e("bucket", 40, 5), e("chest", 30, 4), e("hopper", 150, 20),
			e("furnace", 20, 3), e("crafting_table", 12, 2), e("anvil", 400, 50),
			e("bookshelf", 80, 10), e("enchanting_table", 1500, 200), e("name_tag", 200, 20),
			e("saddle", 250, 30), e("lead", 30, 4), e("experience_bottle", 120, 15)));
		return c;
	}
}
