package io.github.kenthejr.infinitycastle.gen;

/**
 * The castle's architectural vocabulary. Each material is mapped to a concrete block by
 * {@code io.github.kenthejr.infinitycastle.world.CastlePalette}.
 */
public enum Material {
	AIR(Shape.AIR),
	/** Polished walkway planks. */
	DECK(Shape.CUBE),
	/** Darker planks bordering walkways. */
	FLOOR_TRIM(Shape.CUBE),
	/** Ceiling boards. */
	CEILING(Shape.CUBE),
	TATAMI(Shape.HORIZONTAL_AXIS),
	/** Dark timber columns and beams. */
	TIMBER(Shape.AXIS),
	/** Vermilion lacquered wood used for gates, bridges and the entrance hall. */
	LACQUER(Shape.CUBE),
	/** Solid, back-lit paper wall. */
	SHOJI_WALL(Shape.CUBE),
	/** Thin paper screen that connects like a glass pane. */
	SHOJI_SCREEN(Shape.CONNECTING),
	/** Bridge and balcony railing. */
	RAIL(Shape.CONNECTING),
	ROOF_TILE(Shape.CUBE),
	ROOF_STAIRS(Shape.STAIRS),
	ROOF_SLAB(Shape.SLAB),
	WOOD_STAIRS(Shape.STAIRS),
	WOOD_SLAB(Shape.SLAB),
	PAPER_LANTERN(Shape.LANTERN);

	public enum Shape {
		AIR,
		CUBE,
		/** Has an x / y / z axis like a log. */
		AXIS,
		/** Has an x / z axis only. */
		HORIZONTAL_AXIS,
		STAIRS,
		SLAB,
		/** Standing or hanging. */
		LANTERN,
		/** Fences and panes whose connections are resolved after generation. */
		CONNECTING
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
		return this == PAPER_LANTERN || this == SHOJI_WALL;
	}

	/** Blocks a player can stand on (or against, when inverted). */
	public boolean isSolid() {
		return this.shape != Shape.AIR && this.shape != Shape.LANTERN;
	}
}
