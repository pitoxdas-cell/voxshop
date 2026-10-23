package com.voxshop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

public final class ShopCommands {
	private ShopCommands() {}

	private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

	public static void register(CommandDispatcher<CommandSourceStack> d) {
		// /shop  y  /shop reload
		d.register(Commands.literal("shop")
			.executes(ctx -> {
				ShopMenu.open(ctx.getSource().getPlayerOrException());
				return 1;
			})
			.then(Commands.literal("reload").requires(ADMIN).executes(ctx -> {
				ShopConfig.reload();
				ok(ctx, "shop.json recargado (" + ShopConfig.get().categories.size() + " categorias).");
				return 1;
			})));

		// /balance y /bal
		d.register(balance("balance"));
		d.register(balance("bal"));

		// /pay <jugador> <cantidad>
		d.register(Commands.literal("pay")
			.then(Commands.argument("jugador", EntityArgument.player())
				.then(Commands.argument("cantidad", DoubleArgumentType.doubleArg(0.01))
					.executes(ctx -> {
						ServerPlayer from = ctx.getSource().getPlayerOrException();
						ServerPlayer to = EntityArgument.getPlayer(ctx, "jugador");
						double amount = DoubleArgumentType.getDouble(ctx, "cantidad");
						if (from.getUUID().equals(to.getUUID())) {
							fail(ctx, "No puedes pagarte a ti mismo.");
							return 0;
						}
						if (!Economy.take(from.getUUID(), amount)) {
							fail(ctx, "No tienes suficiente dinero.");
							return 0;
						}
						Economy.add(to.getUUID(), amount);
						ok(ctx, "Pagaste " + Economy.format(amount) + " a " + to.getName().getString() + ".");
						to.sendSystemMessage(Component.literal(
							from.getName().getString() + " te pago " + Economy.format(amount) + ".")
							.withStyle(ChatFormatting.GREEN));
						return 1;
					}))));

		// /eco give|take|set <jugador> <cantidad>   (admin)
		d.register(Commands.literal("eco").requires(ADMIN)
			.then(eco("give"))
			.then(eco("take"))
			.then(eco("set")));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> balance(String name) {
		return Commands.literal(name).executes(ctx -> {
			ServerPlayer p = ctx.getSource().getPlayerOrException();
			ok(ctx, "Saldo: " + Economy.format(Economy.get(p.getUUID())));
			return 1;
		});
	}

	private static LiteralArgumentBuilder<CommandSourceStack> eco(String op) {
		return Commands.literal(op)
			.then(Commands.argument("jugador", EntityArgument.player())
				.then(Commands.argument("cantidad", DoubleArgumentType.doubleArg(0))
					.executes(ctx -> {
						ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
						double amount = DoubleArgumentType.getDouble(ctx, "cantidad");
						switch (op) {
							case "give" -> Economy.add(target.getUUID(), amount);
							case "take" -> Economy.set(target.getUUID(), Economy.get(target.getUUID()) - amount);
							default -> Economy.set(target.getUUID(), amount);
						}
						ok(ctx, target.getName().getString() + " ahora tiene "
							+ Economy.format(Economy.get(target.getUUID())));
						return 1;
					})));
	}

	private static void ok(CommandContext<CommandSourceStack> ctx, String msg) {
		ctx.getSource().sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.GREEN), false);
	}

	private static void fail(CommandContext<CommandSourceStack> ctx, String msg) {
		ctx.getSource().sendFailure(Component.literal(msg).withStyle(ChatFormatting.RED));
	}
}
