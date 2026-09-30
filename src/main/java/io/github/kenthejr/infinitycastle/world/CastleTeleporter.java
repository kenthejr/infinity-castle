package io.github.kenthejr.infinitycastle.world;

import io.github.kenthejr.infinitycastle.InfinityCastle;
import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.CastleLayout;
import io.github.kenthejr.infinitycastle.gen.Half;
import io.github.kenthejr.infinitycastle.gen.Modules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Moves players into and out of the castle, remembering where they came from. */
public final class CastleTeleporter {
	/** Where a player was standing before entering the castle. */
	public static final AttachmentType<GlobalPos> RETURN_POINT = AttachmentRegistry.create(
		InfinityCastle.id("return_point"),
		builder -> builder.persistent(GlobalPos.CODEC).copyOnDeath()
	);

	/** Arrival point: the centre of the entrance hall. */
	public static final Vec3 ENTRANCE = new Vec3(8.5, CastleGeometry.entranceStandY(), 8.5);

	private CastleTeleporter() {
	}

	public static void init() {
		// Classloading registers the attachment.
	}

	public static boolean enter(ServerPlayer player) {
		ServerLevel castle = player.level().getServer().getLevel(CastleDimension.LEVEL);
		if (castle == null) {
			player.sendSystemMessage(Component.translatable("message.infinitycastle.missing_dimension"));
			return false;
		}
		if (!CastleDimension.is(player.level())) {
			player.setAttached(RETURN_POINT, GlobalPos.of(player.level().dimension(), player.blockPosition()));
		}
		strum(player, 0.5F);
		player.teleport(new TeleportTransition(castle, ENTRANCE, Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.DO_NOTHING));
		strum(player, 0.5F);
		return true;
	}

	public static boolean leave(ServerPlayer player) {
		GlobalPos target = player.getAttached(RETURN_POINT);
		ServerLevel level = target == null ? null : player.level().getServer().getLevel(target.dimension());
		TeleportTransition transition;
		if (level != null && !CastleDimension.is(level)) {
			BlockPos pos = target.pos();
			transition = new TeleportTransition(level, Vec3.atBottomCenterOf(pos), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING);
		} else {
			transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
		}
		player.removeAttached(RETURN_POINT);
		strum(player, 0.667F);
		player.teleport(transition);
		strum(player, 0.667F);
		return true;
	}

	/** Where a player stands on the lower landing of a stairwell, facing the gravity gate. */
	public static Vec3 lowerLanding(int floor, CastleLayout.Cell cell) {
		return new Vec3(
			cell.x() * 16 + Modules.LANDING_CENTER_X + 0.5,
			CastleGeometry.worldY(floor, Half.LOWER, Modules.LANDING_Y) + 1,
			cell.z() * 16 + Modules.LANDING_CENTER_Z + 0.5
		);
	}

	/** The underside of the mirrored landing above, which an inverted player stands against. */
	public static double upperLandingUnderside(int floor) {
		return CastleGeometry.worldY(floor, Half.UPPER, Modules.LANDING_Y);
	}

	public static boolean toggle(ServerPlayer player) {
		return CastleDimension.is(player.level()) ? leave(player) : enter(player);
	}

	/** The biwa note Nakime plays whenever the castle moves someone. */
	public static void strum(ServerPlayer player, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.PLAYERS, 1.0F, pitch);
	}
}
