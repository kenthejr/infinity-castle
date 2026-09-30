package io.github.kenthejr.infinitycastle.gen;

import java.util.Objects;

/**
 * A material plus the orientation state a block of that material needs.
 *
 * @param material the material
 * @param facing   stairs facing (the side of the tall back half, which is also the climbing direction)
 * @param top      for stairs and slabs: occupies the upper half of the block; for lanterns: hanging
 * @param axis     for timber and tatami: the long axis
 */
public record Piece(Material material, Dir facing, boolean top, Axis axis) {
	public static final Piece AIR = of(Material.AIR);

	public Piece {
		Objects.requireNonNull(material);
		Objects.requireNonNull(facing);
		Objects.requireNonNull(axis);
		if (material.shape() == Material.Shape.HORIZONTAL_AXIS && axis == Axis.Y) {
			throw new IllegalArgumentException(material + " cannot use a vertical axis");
		}
	}

	public static Piece of(Material material) {
		Axis axis = material.shape() == Material.Shape.HORIZONTAL_AXIS ? Axis.X : Axis.Y;
		return new Piece(material, Dir.NORTH, false, axis);
	}

	public static Piece stairs(Material material, Dir facing, boolean top) {
		requireShape(material, Material.Shape.STAIRS);
		return new Piece(material, facing, top, Axis.Y);
	}

	public static Piece slab(Material material, boolean top) {
		requireShape(material, Material.Shape.SLAB);
		return new Piece(material, Dir.NORTH, top, Axis.Y);
	}

	public static Piece axis(Material material, Axis axis) {
		if (material.shape() != Material.Shape.AXIS && material.shape() != Material.Shape.HORIZONTAL_AXIS) {
			throw new IllegalArgumentException(material + " has no axis");
		}
		return new Piece(material, Dir.NORTH, false, axis);
	}

	public static Piece lantern(boolean hanging) {
		return new Piece(Material.PAPER_LANTERN, Dir.NORTH, hanging, Axis.Y);
	}

	private static void requireShape(Material material, Material.Shape shape) {
		if (material.shape() != shape) {
			throw new IllegalArgumentException(material + " is not " + shape);
		}
	}

	public boolean isAir() {
		return this.material == Material.AIR;
	}

	public boolean hanging() {
		return this.material.shape() == Material.Shape.LANTERN && this.top;
	}

	/** The same piece seen upside down: slabs and stairs swap halves and lanterns swap between standing and hanging. */
	public Piece flipVertical() {
		return switch (this.material.shape()) {
			case STAIRS, SLAB, LANTERN -> new Piece(this.material, this.facing, !this.top, this.axis);
			default -> this;
		};
	}

	/** The same piece after the structure it belongs to is turned clockwise around the vertical axis. */
	public Piece rotateY(int quarterTurns) {
		return switch (this.material.shape()) {
			case STAIRS -> new Piece(this.material, this.facing.rotateCw(quarterTurns), this.top, this.axis);
			case AXIS, HORIZONTAL_AXIS -> new Piece(this.material, this.facing, this.top, this.axis.rotateY(quarterTurns));
			default -> this;
		};
	}

	/**
	 * The piece after its structure is tipped onto its side around the X axis (the structure's "up" becomes north).
	 * Blocks that have no sideways form in Minecraft are swapped for a solid stand-in.
	 */
	public Piece tipOverX() {
		return switch (this.material.shape()) {
			case AXIS -> new Piece(this.material, this.facing, this.top, this.axis == Axis.Y ? Axis.Z : this.axis == Axis.Z ? Axis.Y : Axis.X);
			case HORIZONTAL_AXIS -> of(Material.DECK);
			case STAIRS, SLAB -> of(this.material == Material.ROOF_STAIRS || this.material == Material.ROOF_SLAB ? Material.ROOF_TILE : Material.CEILING);
			case LANTERN, CONNECTING -> of(Material.SHOJI_WALL);
			default -> this;
		};
	}
}
