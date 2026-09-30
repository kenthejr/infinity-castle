package io.github.kenthejr.infinitycastle.gravity;

import io.github.kenthejr.infinitycastle.InfinityCastle;
import io.github.kenthejr.infinitycastle.world.CastleDimension;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Applies the castle's gravity to players.
 *
 * <p>Gravity is expressed as a transient modifier on the vanilla {@code minecraft:gravity} attribute, which vanilla
 * already honours for movement, syncs to clients and respects in its anti-flight checks. A negative value makes the
 * player fall upward; the mixins in {@code io.github.kenthejr.infinitycastle.mixin} teach collision, jumping, stepping
 * and fall damage to treat "up" as the ground in that case.
 *
 * <p>{@link #tick(Player)} runs on both logical sides for the player being simulated there: the server player and the
 * client's own local player. Both run the same deterministic rules on the same positions, so the client predicts flips
 * immediately instead of waiting a round trip.
 */
public final class GravityController {
	public static final Identifier MODIFIER_ID = InfinityCastle.id("castle_gravity");

	private GravityController() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(GravityController::tickLevel);
	}

	/** Whether gravity pulls this entity upward. */
	public static boolean isInverted(Entity entity) {
		return entity.getGravity() < 0.0;
	}

	public static void tick(Player player) {
		GravityState state = (GravityState) player;
		if (!CastleDimension.is(player.level()) || player.isSpectator()) {
			if (state.infinitycastle$isTracking()) {
				state.infinitycastle$reset();
				applyModifier(player, 0.0);
			}
			syncDimensions(player, state);
			return;
		}

		double centerY = centerY(player);
		boolean wasInverted = state.infinitycastle$isInverted();
		boolean inverted = state.infinitycastle$isTracking()
			? GravityRules.nextInverted(wasInverted, state.infinitycastle$lastCenterY(), centerY)
			: GravityRules.initialInverted(centerY);
		FloorProfile profile = FloorProfile.at(centerY);
		int previousFloor = state.infinitycastle$isTracking() ? state.infinitycastle$lastFloor() : Integer.MIN_VALUE;
		state.infinitycastle$update(inverted, centerY, profile.floor());

		double scale = profile.gravityScale() * (inverted ? -1.0 : 1.0);
		applyModifier(player, scale - 1.0);
		syncDimensions(player, state);

		if (player instanceof ServerPlayer serverPlayer) {
			if (previousFloor != Integer.MIN_VALUE && inverted != wasInverted) {
				serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.PLAYERS, 0.8F, inverted ? 0.5F : 0.667F);
			}
			if (profile.floor() != previousFloor) {
				announceFloor(serverPlayer, profile);
			}
		}
	}

	public static double centerY(Entity entity) {
		return entity.getY() + entity.getBbHeight() / 2.0;
	}

	private static void applyModifier(Player player, double amount) {
		AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
		if (gravity == null) {
			return;
		}
		AttributeModifier existing = gravity.getModifier(MODIFIER_ID);
		if (Math.abs(amount) < 1.0E-9) {
			if (existing != null) {
				gravity.removeModifier(MODIFIER_ID);
			}
		} else if (existing == null || existing.amount() != amount) {
			gravity.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	/** Eye height depends on gravity (see {@code EntityMixin}), so refresh dimensions whenever gravity flips. */
	private static void syncDimensions(Player player, GravityState state) {
		boolean inverted = isInverted(player);
		if (inverted != state.infinitycastle$dimensionsInverted()) {
			state.infinitycastle$setDimensionsInverted(inverted);
			player.refreshDimensions();
		}
	}

	private static void announceFloor(ServerPlayer player, FloorProfile profile) {
		Component name = Component.literal(profile.japaneseName()).withStyle(ChatFormatting.GOLD);
		Component message = Component.translatable("message.infinitycastle.floor", name, profile.number());
		if (profile.drifting()) {
			message = Component.empty().append(message).append(Component.translatable("message.infinitycastle.floor.drifting").withStyle(ChatFormatting.GRAY));
		}
		player.sendOverlayMessage(message);
	}

	private static void tickLevel(ServerLevel level) {
		if (!CastleDimension.is(level)) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			double offset = GravityRules.wrapOffset(player.getY());
			if (offset != 0.0) {
				// The floors repeat, so falling out of one end drops you back in the other, keeping your momentum.
				player.teleportTo(level, 0.0, offset, 0.0, Relative.ALL, 0.0F, 0.0F, false);
			}
		}
	}
}
