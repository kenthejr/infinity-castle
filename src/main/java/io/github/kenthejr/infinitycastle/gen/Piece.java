package io.github.kenthejr.infinitycastle.gen;

import java.util.Objects;

/**
 * A material plus the orientation state a block of that material needs.
 *
 * @param material the material
 * @param facing   for stairs, trapdoors, gates and panels: the direction the block faces (for stairs, the side of the
 *                 tall back half, which is also the climbing direction)
 * @param top      for stairs, slabs and trapdoors: occupies or hinges on the upper half of the block; for lanterns:
 *                 hanging
 * @param axis     for logs: the long axis
 */
public record Piece(Material material, Dir facing, boolean top, Axis axis) {
	public static final Piece AIR = of(Material.AIR);

	public Piece {
		Objects.requireNonNull(material);
		Objects.requireNonNull(facing);
		Objects.requireNonNull(axis);
	}

	public static Piece of(Material material) {
		return new Piece(material, Dir.NORTH, false, Axis.Y);
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
		requireShape(material, Material.Shape.AXIS);
		return new Piece(material, Dir.NORTH, false, axis);
	}

	public static Piece lantern(boolean hanging) {
		return new Piece(Material.LANTERN, Dir.NORTH, hanging, Axis.Y);
	}

	/** An open trapdoor standing on edge against the {@code facing} side of its block, hinged at the top or bottom. */
	public static Piece trapdoor(Dir facing, boolean top) {
		return new Piece(Material.DARK_OAK_TRAPDOOR, facing, top, Axis.Y);
	}

	public static Piece gate(Dir facing) {
		return new Piece(Material.SPRUCE_FENCE_GATE, facing, false, Axis.Y);
	}

	public static Piece fusuma(Dir facing) {
		return new Piece(Material.FUSUMA, facing, false, Axis.Y);
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

	/** The same piece seen upside down: halves swap and lanterns swap between standing and hanging. */
	public Piece flipVertical() {
		return this.material.hasHalves() ? new Piece(this.material, this.facing, !this.top, this.axis) : this;
	}

	/** The same piece after the structure it belongs to is turned clockwise around the vertical axis. */
	public Piece rotateY(int quarterTurns) {
		if (this.material.hasFacing()) {
			return new Piece(this.material, this.facing.rotateCw(quarterTurns), this.top, this.axis);
		}
		if (this.material.shape() == Material.Shape.AXIS) {
			return new Piece(this.material, this.facing, this.top, this.axis.rotateY(quarterTurns));
		}
		return this;
	}

	/**
	 * The piece after its structure is tipped onto its side around the X axis (the structure's "up" becomes north).
	 * Blocks that have no sideways form in Minecraft are swapped for a solid stand-in, or dropped.
	 */
	public Piece tipOverX() {
		return switch (this.material.shape()) {
			case AXIS -> new Piece(this.material, this.facing, this.top, this.axis == Axis.Y ? Axis.Z : this.axis == Axis.Z ? Axis.Y : Axis.X);
			// A panel facing east or west turns within its own plane and stays upright; one facing north or south lies flat.
			case PANEL -> this.facing.axis() == Axis.X ? this : of(this.material.standIn());
			case AIR, CUBE -> this;
			default -> of(this.material.standIn());
		};
	}
}
