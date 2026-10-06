package io.github.kenthejr.infinitycastle.registry;

import io.github.kenthejr.infinitycastle.InfinityCastle;
import io.github.kenthejr.infinitycastle.item.BiwaItem;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	public static final Item BIWA = register("biwa", BiwaItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	public static final Item TATAMI = registerBlock(ModBlocks.TATAMI);
	public static final Item SHOJI = registerBlock(ModBlocks.SHOJI);
	public static final Item SHOJI_SCREEN = registerBlock(ModBlocks.SHOJI_SCREEN);
	public static final Item LACQUERED_PLANKS = registerBlock(ModBlocks.LACQUERED_PLANKS);
	public static final Item PAPER_LANTERN = registerBlock(ModBlocks.PAPER_LANTERN);
	public static final Item FUSUMA = registerBlock(ModBlocks.FUSUMA);

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		InfinityCastle.id("infinity_castle"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.infinitycastle.infinity_castle"))
			.icon(() -> new ItemStack(ModItems.PAPER_LANTERN))
			.displayItems((parameters, output) -> {
				for (Item item : List.of(BIWA, FUSUMA, TATAMI, SHOJI, SHOJI_SCREEN, LACQUERED_PLANKS, PAPER_LANTERN)) {
					output.accept(item);
				}
			})
			.build()
	);

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, InfinityCastle.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static Item registerBlock(Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
		BlockItem item = new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
		item.registerBlocks(Item.BY_BLOCK, item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		// Classloading registers the items.
	}
}
