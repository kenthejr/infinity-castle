package io.github.kenthejr.infinitycastle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.CastleLayout;
import io.github.kenthejr.infinitycastle.gen.CellPlan;
import io.github.kenthejr.infinitycastle.gen.Half;
import io.github.kenthejr.infinitycastle.gravity.FloorProfile;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.world.CastleChunkGenerator;
import io.github.kenthejr.infinitycastle.world.CastleDimension;
import io.github.kenthejr.infinitycastle.world.CastleTeleporter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /infinitycastle enter|leave [targets]} moves players (operators only);
 * {@code /infinitycastle where} describes the floor and cell you are in;
 * {@code /infinitycastle stairwell} takes you to the nearest gravity gate on your floor (operators only).
 */
public final class CastleCommands {
	private CastleCommands() {
	}

	public static void init() {
		CastleTeleporter.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("infinitycastle")
				.then(Commands.literal("enter")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> move(ctx, List.of(ctx.getSource().getPlayerOrException()), true))
					.then(Commands.argument("targets", EntityArgument.players())
						.executes(ctx -> move(ctx, EntityArgument.getPlayers(ctx, "targets"), true))))
				.then(Commands.literal("leave")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> move(ctx, List.of(ctx.getSource().getPlayerOrException()), false))
					.then(Commands.argument("targets", EntityArgument.players())
						.executes(ctx -> move(ctx, EntityArgument.getPlayers(ctx, "targets"), false))))
				.then(Commands.literal("where").executes(CastleCommands::where))
				.then(Commands.literal("stairwell")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(CastleCommands::stairwell))
		);
	}

	private static int move(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean enter) {
		int moved = 0;
		for (ServerPlayer player : players) {
			boolean inCastle = CastleDimension.is(player.level());
			if (enter ? CastleTeleporter.enter(player) : inCastle && CastleTeleporter.leave(player)) {
				moved++;
			}
		}
		int count = moved;
		ctx.getSource().sendSuccess(() -> Component.translatable(enter ? "commands.infinitycastle.enter" : "commands.infinitycastle.leave", count), true);
		return moved;
	}

	private static int where(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = player.level();
		if (!CastleDimension.is(level) || !(level.getChunkSource().getGenerator() instanceof CastleChunkGenerator)) {
			ctx.getSource().sendFailure(Component.translatable("commands.infinitycastle.where.outside"));
			return 0;
		}
		double y = GravityController.centerY(player);
		int floor = CastleGeometry.floorOf(y);
		Half half = CastleGeometry.halfOf(y);
		CellPlan plan = CastleChunkGenerator.layout(level.getChunkSource().randomState())
			.plan(floor, half, CastleGeometry.cellOf(player.getBlockX()), CastleGeometry.cellOf(player.getBlockZ()));
		FloorProfile profile = FloorProfile.of(floor);
		ctx.getSource().sendSuccess(() -> Component.translatable(
			"commands.infinitycastle.where",
			profile.japaneseName(),
			profile.number(),
			Component.translatable("half.infinitycastle." + half.name().toLowerCase(Locale.ROOT)),
			Component.translatable("module.infinitycastle." + plan.type().name().toLowerCase(Locale.ROOT)),
			GravityController.isInverted(player) ? "↑" : "↓"
		), false);
		return 1;
	}

	private static int stairwell(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = player.level();
		if (!CastleDimension.is(level)) {
			ctx.getSource().sendFailure(Component.translatable("commands.infinitycastle.where.outside"));
			return 0;
		}
		int floor = CastleGeometry.floorOf(GravityController.centerY(player));
		CastleLayout layout = CastleChunkGenerator.layout(level.getChunkSource().randomState());
		Optional<CastleLayout.Cell> cell = layout.nearestStairwell(floor, CastleGeometry.cellOf(player.getBlockX()), CastleGeometry.cellOf(player.getBlockZ()), 32);
		if (cell.isEmpty()) {
			ctx.getSource().sendFailure(Component.translatable("commands.infinitycastle.stairwell.none"));
			return 0;
		}
		Vec3 landing = CastleTeleporter.lowerLanding(floor, cell.get());
		player.teleportTo(level, landing.x, landing.y, landing.z, Set.of(), player.getYRot(), 0.0F, true);
		return 1;
	}
}
