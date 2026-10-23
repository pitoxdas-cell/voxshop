package com.voxshop;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de cofre (6 filas) con dos vistas: categorias y articulos de una categoria.
 * Nunca se puede sacar ni meter items: todos los clics se interceptan.
 *
 * Articulos:  clic izq = comprar 1 | shift+izq = comprar 64
 *             clic der = vender 1  | shift+der = vender todo
 */
public class ShopMenu extends ChestMenu {
	private static final int SIZE = 54;
	private static final int PER_PAGE = 45;
	private static final int[] CATEGORY_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34
	};
	private static final int SLOT_BACK = 45, SLOT_PREV = 48, SLOT_BALANCE = 49, SLOT_NEXT = 50, SLOT_CLOSE = 53;

	private final ServerPlayer player;
	private final Container shop;
	private int category = -1; // -1 = menu principal
	private int page = 0;

	private ShopMenu(int id, Inventory inv, Container shop, ServerPlayer player) {
		super(MenuType.GENERIC_9x6, id, inv, shop, 6);
		this.shop = shop;
		this.player = player;
		render();
	}

	public static void open(ServerPlayer player) {
		player.openMenu(new SimpleMenuProvider(
			(id, inv, p) -> new ShopMenu(id, inv, new SimpleContainer(SIZE), player),
			Component.literal(ShopConfig.get().title)));
	}

	// ------------------------------------------------------------ render

	private static Component text(String s, ChatFormatting color) {
		return Component.literal(s).withStyle(Style.EMPTY.withItalic(false).withColor(color));
	}

	private static ItemStack named(Item item, String name, ChatFormatting color, List<Component> lore) {
		ItemStack st = new ItemStack(item);
		st.set(DataComponents.CUSTOM_NAME, text(name, color));
		if (lore != null && !lore.isEmpty()) st.set(DataComponents.LORE, new ItemLore(lore));
		return st;
	}

	private void render() {
		shop.clearContent();
		ItemStack filler = named(Items.GRAY_STAINED_GLASS_PANE, " ", ChatFormatting.GRAY, null);
		ShopConfig cfg = ShopConfig.get();
		if (category >= cfg.categories.size()) category = -1; // por si se recargo la config

		if (category < 0) {
			for (int i = 0; i < SIZE; i++) shop.setItem(i, filler.copy());
			for (int i = 0; i < CATEGORY_SLOTS.length; i++) shop.setItem(CATEGORY_SLOTS[i], ItemStack.EMPTY);
			for (int i = 0; i < cfg.categories.size() && i < CATEGORY_SLOTS.length; i++) {
				ShopConfig.Category c = cfg.categories.get(i);
				List<Component> lore = new ArrayList<>();
				lore.add(text(c.resolved.size() + " articulos", ChatFormatting.GRAY));
				lore.add(text("", ChatFormatting.GRAY));
				lore.add(text("Clic para abrir", ChatFormatting.YELLOW));
				shop.setItem(CATEGORY_SLOTS[i], named(c.iconItem, c.name, ChatFormatting.GOLD, lore));
			}
			shop.setItem(SLOT_BALANCE, balanceItem());
			shop.setItem(SLOT_CLOSE, named(Items.BARRIER, "Cerrar", ChatFormatting.RED, null));
			return;
		}

		ShopConfig.Category c = cfg.categories.get(category);
		int pages = Math.max(1, (c.resolved.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.max(0, Math.min(page, pages - 1));

		for (int i = 0; i < PER_PAGE; i++) {
			int idx = page * PER_PAGE + i;
			if (idx >= c.resolved.size()) break;
			shop.setItem(i, entryItem(c.resolved.get(idx)));
		}
		for (int i = PER_PAGE; i < SIZE; i++) shop.setItem(i, filler.copy());
		shop.setItem(SLOT_BACK, named(Items.ARROW, "Volver", ChatFormatting.YELLOW, null));
		if (page > 0) shop.setItem(SLOT_PREV, named(Items.PAPER, "Pagina anterior", ChatFormatting.AQUA, null));
		if (page < pages - 1) shop.setItem(SLOT_NEXT, named(Items.PAPER, "Pagina siguiente", ChatFormatting.AQUA, null));
		shop.setItem(SLOT_BALANCE, balanceItem());
		shop.setItem(SLOT_CLOSE, named(Items.BARRIER, "Cerrar", ChatFormatting.RED, null));
	}

	private ItemStack balanceItem() {
		List<Component> lore = List.of(text(Economy.format(Economy.get(player.getUUID())), ChatFormatting.GREEN));
		return named(Items.GOLD_INGOT, "Tu saldo", ChatFormatting.GOLD, lore);
	}

	private ItemStack entryItem(ShopConfig.Resolved r) {
		ItemStack st = new ItemStack(r.item());
		List<Component> lore = new ArrayList<>();
		lore.add(text("", ChatFormatting.GRAY));
		if (r.buy() >= 0) lore.add(text("Compra: " + Economy.format(r.buy()), ChatFormatting.GREEN));
		else lore.add(text("No se puede comprar", ChatFormatting.DARK_GRAY));
		if (r.sell() >= 0) lore.add(text("Venta: " + Economy.format(r.sell()), ChatFormatting.RED));
		else lore.add(text("No se puede vender", ChatFormatting.DARK_GRAY));
		lore.add(text("", ChatFormatting.GRAY));
		if (r.buy() >= 0) {
			lore.add(text("Clic izq: comprar 1", ChatFormatting.YELLOW));
			lore.add(text("Shift + izq: comprar 64", ChatFormatting.YELLOW));
		}
		if (r.sell() >= 0) {
			lore.add(text("Clic der: vender 1", ChatFormatting.YELLOW));
			lore.add(text("Shift + der: vender todo", ChatFormatting.YELLOW));
		}
		st.set(DataComponents.LORE, new ItemLore(lore));
		return st;
	}

	// ------------------------------------------------------------ clics

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player p) {
		// Solo reaccionamos a clics simples / shift sobre la parte de la tienda.
		if (slotId >= 0 && slotId < SIZE && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
			handle(slotId, button, clickType == ClickType.QUICK_MOVE);
		}
		render();
		broadcastFullState();
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		return ItemStack.EMPTY;
	}

	private void handle(int slot, int button, boolean shift) {
		ShopConfig cfg = ShopConfig.get();

		if (category < 0) {
			if (slot == SLOT_CLOSE) {
				player.closeContainer();
				return;
			}
			for (int i = 0; i < CATEGORY_SLOTS.length && i < cfg.categories.size(); i++) {
				if (CATEGORY_SLOTS[i] == slot) {
					category = i;
					page = 0;
					click();
					return;
				}
			}
			return;
		}

		if (slot == SLOT_CLOSE) {
			player.closeContainer();
			return;
		}
		if (slot == SLOT_BACK) {
			category = -1;
			click();
			return;
		}
		if (slot == SLOT_PREV) {
			page--;
			click();
			return;
		}
		if (slot == SLOT_NEXT) {
			page++;
			click();
			return;
		}
		if (slot >= PER_PAGE) return;

		ShopConfig.Category c = cfg.categories.get(category);
		int idx = page * PER_PAGE + slot;
		if (idx >= c.resolved.size()) return;
		ShopConfig.Resolved r = c.resolved.get(idx);

		if (button == 0) buy(r, shift ? 64 : 1);
		else if (button == 1) sell(r, shift ? Integer.MAX_VALUE : 1);
	}

	// ------------------------------------------------------------ compra / venta

	private void buy(ShopConfig.Resolved r, int amount) {
		if (r.buy() < 0) {
			msg("Este articulo no se puede comprar.", ChatFormatting.RED);
			return;
		}
		double total = r.buy() * amount;
		if (!Economy.take(player.getUUID(), total)) {
			msg("No tienes suficiente dinero (" + Economy.format(total) + ").", ChatFormatting.RED);
			return;
		}
		int left = amount;
		int max = new ItemStack(r.item()).getMaxStackSize();
		while (left > 0) {
			int n = Math.min(left, max);
			player.getInventory().placeItemBackInInventory(new ItemStack(r.item(), n));
			left -= n;
		}
		msg("Compraste " + amount + "x " + new ItemStack(r.item()).getHoverName().getString()
			+ " por " + Economy.format(total), ChatFormatting.GREEN);
		sound(true);
	}

	private void sell(ShopConfig.Resolved r, int wanted) {
		if (r.sell() < 0) {
			msg("Este articulo no se puede vender.", ChatFormatting.RED);
			return;
		}
		Inventory inv = player.getInventory();
		int removed = 0;
		for (int i = 0; i < 36 && removed < wanted; i++) {
			ItemStack st = inv.getItem(i);
			if (st.isEmpty() || !st.is(r.item())) continue;
			// No comprar items con nombre/encantados al precio base.
			if (st.isEnchanted() || st.has(DataComponents.CUSTOM_NAME)) continue;
			int n = Math.min(st.getCount(), wanted - removed);
			st.shrink(n);
			if (st.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
			removed += n;
		}
		if (removed == 0) {
			msg("No tienes ese articulo para vender.", ChatFormatting.RED);
			return;
		}
		double total = r.sell() * removed;
		Economy.add(player.getUUID(), total);
		msg("Vendiste " + removed + "x " + new ItemStack(r.item()).getHoverName().getString()
			+ " por " + Economy.format(total), ChatFormatting.GREEN);
		sound(false);
	}

	private void msg(String s, ChatFormatting color) {
		player.sendSystemMessage(Component.literal(s).withStyle(color));
	}

	private void click() {
		player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.0f);
	}

	private void sound(boolean buy) {
		player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, buy ? 1.2f : 0.8f);
	}
}
