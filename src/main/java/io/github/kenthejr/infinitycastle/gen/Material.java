package io.github.kenthejr.infinitycastle.gen;

import java.util.Locale;

/**
 * The castle's building blocks: the palette the hand-built designs use. Each material is named after the block it is
 * mapped to by {@code io.github.kenthejr.infinitycastle.world.CastlePalette}; fusuma is the mod's own block.
 */
public enum Material {
	AIR(Shape.AIR),
	/** Floors, walls, roofs and bridge walkways. */
	SPRUCE_PLANKS(Shape.CUBE),
	/** Bridge kerbs and stair rails. */
	DARK_OAK_PLANKS(Shape.CUBE),
	OAK_PLANKS(Shape.CUBE),
	/** Room floors: half a block below the surrounding planks, like tatami in a raised frame. */
	OAK_SLAB(Shape.SLAB),
	SPRUCE_SLAB(Shape.SLAB),
	/** Roof eaves and the long staircases. */
	SPRUCE_STAIRS(Shape.STAIRS),
	DARK_OAK_STAIRS(Shape.STAIRS),
	/** Stood open on edge as a lattice panel below the fusuma. */
	DARK_OAK_TRAPDOOR(Shape.TRAPDOOR),
	/** Interior posts and veranda rails. */
	OAK_FENCE(Shape.FENCE),
	/** Bridge and stair rails. */
	SPRUCE_FENCE(Shape.FENCE),
	SPRUCE_FENCE_GATE(Shape.GATE),
	/** The post at the heart of each spiral stair. */
	STRIPPED_BIRCH_LOG(Shape.AXIS),
	LANTERN(Shape.LANTERN),
	/** Sliding paper door panel with a lattice window: the castle's walls. */
	FUSUMA(Shape.PANEL);

	public enum Shape {
		AIR,
		CUBE,
		/** Has an x / y / z axis like a log. */
		AXIS,
		/** Faces a horizontal direction and occupies the top or bottom half. */
		STAIRS,
		/** Occupies the top or bottom half. */
		SLAB,
		/** Faces a horizontal direction, hinged at the top or bottom; always open. */
		TRAPDOOR,
		/** Standing or hanging. */
		LANTERN,
		/** Connects to its neighbours; the connections are resolved after generation. */
		FENCE,
		/** Faces a horizontal direction; always closed. */
		GATE,
		/** A thin panel facing a horizontal direction. */
		PANEL
	}

	private final Shape shape;

	Material(Shape shape) {
		this.shape = shape;
	}

	public Shape shape() {
		return this.shape;
	}

	/** Emits light; used by tests to check rooms are lit. */
	public boolean isLightSource() {
		return this == LANTERN || this == FUSUMA;
	}

	/** Blocks a player can stand on (or against, when inverted). */
	public boolean isSolid() {
		return this.shape != Shape.AIR && this.shape != Shape.LANTERN;
	}

	/** Whether a block of this material faces one of the four horizontal directions. */
	public boolean hasFacing() {
		return this.shape == Shape.STAIRS || this.shape == Shape.TRAPDOOR || this.shape == Shape.GATE || this.shape == Shape.PANEL;
	}

	/** Whether a block of this material has an upper and a lower variant. */
	public boolean hasHalves() {
		return this.shape == Shape.STAIRS || this.shape == Shape.SLAB || this.shape == Shape.TRAPDOOR || this.shape == Shape.LANTERN;
	}

	/** What stands in for this material where it has no form, for example a slab tipped onto its side. */
	public Material standIn() {
		return switch (this) {
			case OAK_SLAB -> OAK_PLANKS;
			case SPRUCE_SLAB, SPRUCE_STAIRS, FUSUMA -> SPRUCE_PLANKS;
			case DARK_OAK_STAIRS, DARK_OAK_TRAPDOOR -> DARK_OAK_PLANKS;
			case OAK_FENCE, SPRUCE_FENCE, SPRUCE_FENCE_GATE, LANTERN -> AIR;
			default -> this;
		};
	}

	/** The block name used in design templates, e.g. {@code spruce_stairs}. */
	public String blockName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public static Material byBlockName(String name) {
		try {
			return valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown material " + name, e);
		}
	}
}
