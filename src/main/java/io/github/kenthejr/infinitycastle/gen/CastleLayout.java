package io.github.kenthejr.infinitycastle.gen;

import java.util.Optional;

/**
 * Decides what occupies every cell of the castle.
 *
 * <p>The layout is a grid of {@link CastleGeometry#CELL_SIZE}-block cells per floor half. Each cell is either void or occupied; occupied cells
 * connect to occupied neighbours through edges chosen by a hash of the edge itself, so both cells sharing an edge
 * always agree. The module type then follows from the cell's connections: dead ends become rooms, straight runs become
 * corridors or bridges, junctions become halls.
 */
public final class CastleLayout {
	public static final double STAIRWELL_CHANCE = 0.08;
	public static final double MIN_VOID_CHANCE = 0.30;
	public static final double MAX_VOID_CHANCE = 0.55;
	public static final double EDGE_CHANCE = 0.62;
	public static final double BRIDGE_CHANCE = 0.45;
	public static final double SIDEWAYS_CHANCE = 0.07;

	private static final long SALT_STAIR = 0x5354414952L;
	private static final long SALT_DENSITY = 0x44454E53L;
	private static final long SALT_VOID = 0x564F4944L;
	private static final long SALT_EDGE_X = 0x45444745_58L;
	private static final long SALT_EDGE_Z = 0x45444745_5AL;
	private static final long SALT_VARIANT = 0x56415249L;

	private final long seed;

	public CastleLayout(long seed) {
		this.seed = seed;
	}

	public long seed() {
		return this.seed;
	}

	public static boolean isEntranceCell(int floor, Half half, int cx, int cz) {
		return floor == CastleGeometry.ENTRANCE_FLOOR && half == Half.LOWER && cx == 0 && cz == 0;
	}

	/** The entrance hall always opens onto four occupied neighbours so new arrivals never start in a dead end. */
	private static boolean isEntranceNeighbour(int floor, Half half, int cx, int cz) {
		return floor == CastleGeometry.ENTRANCE_FLOOR && half == Half.LOWER && Math.abs(cx) + Math.abs(cz) == 1;
	}

	/** Stairwells are chosen per floor, not per half, so the spiral in each half meets its mirror image at the equator. */
	public boolean isStairwell(int floor, int cx, int cz) {
		if (floor == CastleGeometry.ENTRANCE_FLOOR && Math.abs(cx) + Math.abs(cz) <= 1) {
			return false;
		}
		return CastleRandom.unit(CastleRandom.hash(this.seed, SALT_STAIR, floor, cx, cz)) < STAIRWELL_CHANCE;
	}

	public double voidChance(int floor, Half half) {
		double t = CastleRandom.unit(CastleRandom.hash(this.seed, SALT_DENSITY, floor, half.ordinal()));
		return MIN_VOID_CHANCE + (MAX_VOID_CHANCE - MIN_VOID_CHANCE) * t;
	}

	public boolean isVoid(int floor, Half half, int cx, int cz) {
		if (!CastleGeometry.isValidFloor(floor)) {
			return true;
		}
		if (isEntranceCell(floor, half, cx, cz) || isEntranceNeighbour(floor, half, cx, cz) || this.isStairwell(floor, cx, cz)) {
			return false;
		}
		return CastleRandom.unit(CastleRandom.hash(this.seed, SALT_VOID, floor, half.ordinal(), cx, cz)) < this.voidChance(floor, half);
	}

	/** Whether the edge between cell (cx, cz) and (cx + 1, cz) has a doorway. */
	public boolean edgeEast(int floor, Half half, int cx, int cz) {
		if (this.isVoid(floor, half, cx, cz) || this.isVoid(floor, half, cx + 1, cz)) {
			return false;
		}
		if (isEntranceCell(floor, half, cx, cz) || isEntranceCell(floor, half, cx + 1, cz)) {
			return true;
		}
		return CastleRandom.unit(CastleRandom.hash(this.seed, SALT_EDGE_X, floor, half.ordinal(), cx, cz)) < EDGE_CHANCE;
	}

	/** Whether the edge between cell (cx, cz) and (cx, cz + 1) has a doorway. */
	public boolean edgeSouth(int floor, Half half, int cx, int cz) {
		if (this.isVoid(floor, half, cx, cz) || this.isVoid(floor, half, cx, cz + 1)) {
			return false;
		}
		if (isEntranceCell(floor, half, cx, cz) || isEntranceCell(floor, half, cx, cz + 1)) {
			return true;
		}
		return CastleRandom.unit(CastleRandom.hash(this.seed, SALT_EDGE_Z, floor, half.ordinal(), cx, cz)) < EDGE_CHANCE;
	}

	public Openings openings(int floor, Half half, int cx, int cz) {
		return new Openings(
			this.edgeSouth(floor, half, cx, cz - 1),
			this.edgeEast(floor, half, cx, cz),
			this.edgeSouth(floor, half, cx, cz),
			this.edgeEast(floor, half, cx - 1, cz)
		);
	}

	/** A cell position within a floor. */
	public record Cell(int x, int z) {
	}

	/** The stairwell on {@code floor} closest to cell (cx, cz), searching outward ring by ring. */
	public Optional<Cell> nearestStairwell(int floor, int cx, int cz, int radius) {
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) == r && this.isStairwell(floor, cx + dx, cz + dz)) {
						return Optional.of(new Cell(cx + dx, cz + dz));
					}
				}
			}
		}
		return Optional.empty();
	}

	public CellPlan plan(int floor, Half half, int cx, int cz) {
		if (!CastleGeometry.isValidFloor(floor)) {
			return CellPlan.VOID;
		}

		long variant = CastleRandom.hash(this.seed, SALT_VARIANT, floor, half.ordinal(), cx, cz);
		if (isEntranceCell(floor, half, cx, cz)) {
			return new CellPlan(ModuleType.ENTRANCE, this.openings(floor, half, cx, cz), variant);
		}
		if (this.isStairwell(floor, cx, cz)) {
			return new CellPlan(ModuleType.STAIRWELL, this.openings(floor, half, cx, cz), variant);
		}
		if (this.isVoid(floor, half, cx, cz)) {
			return CastleRandom.unit(variant) < SIDEWAYS_CHANCE
				? new CellPlan(ModuleType.SIDEWAYS_CHAMBER, Openings.NONE, variant)
				: CellPlan.VOID;
		}

		Openings openings = this.openings(floor, half, cx, cz);
		ModuleType type = switch (openings.count()) {
			case 0 -> ModuleType.PAVILION;
			case 1 -> ModuleType.ROOM;
			case 2 -> openings.isStraight()
				? CastleRandom.unit(CastleRandom.fork(variant, 0)) < BRIDGE_CHANCE ? ModuleType.BRIDGE : ModuleType.CORRIDOR
				: ModuleType.HALL;
			default -> ModuleType.HALL;
		};
		return new CellPlan(type, openings, variant);
	}
}
