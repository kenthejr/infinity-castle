package io.github.kenthejr.infinitycastle.item;

import io.github.kenthejr.infinitycastle.world.CastleTeleporter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Nakime's biwa. Strum it to be pulled into the castle; strum it again inside to be released. */
public class BiwaItem extends Item {
	public static final int COOLDOWN_TICKS = 60;

	public BiwaItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer && CastleTeleporter.toggle(serverPlayer)) {
			serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}
}
