package io.github.kenthejr.infinitycastle.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ModulesTest {
	private static final Set<ModuleType> LIT = EnumSet.of(
		ModuleType.ENTRANCE, ModuleType.HALL, ModuleType.ROOM, ModuleType.CORRIDOR, ModuleType.PAVILION, ModuleType.STAIRWELL
	);

	/** Every plan in a chunk of the castle, so all module variants and rotations get exercised. */
	private static List<CellPlan> samplePlans() {
		CastleLayout layout = new CastleLayout(1234L);
		List<CellPlan> plans = new ArrayList<>();
		CastleLayoutTest.forEachCell((floor, half, cx, cz) -> plans.add(layout.plan(floor, half, cx, cz)));
		return plans;
	}

	@Test
	void everyDoorwayHasFloorAndHeadroom() {
		for (CellPlan plan : samplePlans()) {
			if (plan.type() == ModuleType.SIDEWAYS_CHAMBER) {
				continue;
			}
			Canvas canvas = Modules.build(plan);
			for (Dir dir : Dir.values()) {
				if (!plan.openings().has(dir)) {
					continue;
				}
				for (int along = Openings.DOOR_MIN; along <= Openings.DOOR_MAX; along++) {
					for (int depth = 0; depth <= 1; depth++) {
						int[] xz = edgeCell(dir, along, depth);
						assertTrue(canvas.get(xz[0], 0, xz[1]).material().isSolid(), plan + ": no floor in doorway " + dir);
						for (int y = 1; y <= Modules.DOOR_HEIGHT; y++) {
							assertTrue(canvas.get(xz[0], y, xz[1]).isAir(), plan + ": doorway " + dir + " blocked at y=" + y + " by " + canvas.get(xz[0], y, xz[1]));
						}
					}
				}
			}
		}
	}

	@Test
	void closedSidesOfRoomsAreWalled() {
		for (CellPlan plan : samplePlans()) {
			if (plan.type() != ModuleType.HALL && plan.type() != ModuleType.ROOM) {
				continue;
			}
			Canvas canvas = Modules.build(plan);
			for (Dir dir : Dir.values()) {
				if (plan.openings().has(dir)) {
					continue;
				}
				int[] xz = edgeCell(dir, 7, 1);
				assertFalse(canvas.get(xz[0], 2, xz[1]).isAir(), plan + ": missing wall on " + dir);
			}
		}
	}

	@Test
	void lanternsHangFromOrStandOnSomething() {
		for (CellPlan plan : samplePlans()) {
			Canvas canvas = Modules.build(plan);
			canvas.forEach((x, y, z, piece) -> {
				if (piece.material() == Material.PAPER_LANTERN && plan.type() != ModuleType.SIDEWAYS_CHAMBER) {
					Piece support = canvas.get(x, piece.hanging() ? y + 1 : y - 1, z);
					assertTrue(support.material().isSolid(), plan.type() + ": floating lantern at " + x + "," + y + "," + z);
				}
			});
		}
	}

	@Test
	void habitableModulesAreLit() {
		for (CellPlan plan : samplePlans()) {
			if (!LIT.contains(plan.type())) {
				continue;
			}
			boolean[] lit = {false};
			Modules.build(plan).forEach((x, y, z, piece) -> lit[0] |= piece.material().isLightSource());
			assertTrue(lit[0], plan + " has no light");
		}
	}

	@ParameterizedTest
	@EnumSource(value = ModuleType.class, names = {"HALL", "ROOM", "ENTRANCE", "PAVILION"})
	void roofedModulesHaveARoof(ModuleType type) {
		Openings openings = type == ModuleType.ROOM ? new Openings(true, false, false, false) : type == ModuleType.PAVILION ? Openings.NONE : Openings.ALL;
		Canvas canvas = Modules.build(new CellPlan(type, openings, 0L));
		boolean[] roof = {false};
		canvas.forEach((x, y, z, piece) -> roof[0] |= piece.material() == Material.ROOF_STAIRS || piece.material() == Material.ROOF_TILE);
		assertTrue(roof[0]);
	}

	@Test
	void entranceCentreIsClearForArrivingPlayers() {
		Canvas canvas = Modules.build(new CellPlan(ModuleType.ENTRANCE, Openings.ALL, 0L));
		assertTrue(canvas.get(8, 0, 8).material().isSolid());
		assertTrue(canvas.get(8, 1, 8).isAir());
		assertTrue(canvas.get(8, 2, 8).isAir());
	}

	/**
	 * Walks the stairwell from the deck to the landing, only ever stepping to a horizontally adjacent block at most one
	 * higher, the way a player climbs stairs.
	 */
	@Test
	void stairwellIsClimbableFromDeckToLanding() {
		Canvas canvas = Modules.build(new CellPlan(ModuleType.STAIRWELL, Openings.ALL, 0L));
		// Standing heights: the y a player's feet are at when on top of the block at (x, y - 1, z).
		boolean[][][] visited = new boolean[16][Canvas.SIZE_Y + 1][16];
		List<int[]> frontier = new ArrayList<>();
		frontier.add(new int[] {8, 1, 8});
		visited[8][1][8] = true;
		boolean reachedLanding = false;
		while (!frontier.isEmpty()) {
			int[] at = frontier.removeLast();
			if (at[1] == Modules.LANDING_Y + 1) {
				reachedLanding = true;
				break;
			}
			for (Dir dir : Dir.values()) {
				int nx = at[0] + dir.dx();
				int nz = at[2] + dir.dz();
				if (nx < 0 || nx > 15 || nz < 0 || nz > 15) {
					continue;
				}
				for (int ny = at[1] - 1; ny <= at[1] + 1; ny++) {
					if (ny < 1 || ny > Canvas.SIZE_Y - 2 || visited[nx][ny][nz]) {
						continue;
					}
					boolean floor = canvas.get(nx, ny - 1, nz).material().isSolid();
					boolean headroom = !canvas.get(nx, ny, nz).material().isSolid() && !canvas.get(nx, ny + 1, nz).material().isSolid();
					// Stepping up needs headroom above where you start, too.
					boolean clearAbove = ny <= at[1] || !canvas.get(at[0], at[1] + 2, at[2]).material().isSolid();
					if (floor && headroom && clearAbove) {
						visited[nx][ny][nz] = true;
						frontier.add(new int[] {nx, ny, nz});
					}
				}
			}
		}
		assertTrue(reachedLanding, "could not climb to the landing");
	}

	@Test
	void landingSitsJustBelowTheEquator() {
		Canvas canvas = Modules.build(new CellPlan(ModuleType.STAIRWELL, Openings.NONE, 0L));
		assertEquals(Material.DECK, canvas.get(8, Modules.LANDING_Y, 12).material());
		for (int y = Modules.LANDING_Y + 1; y < Canvas.SIZE_Y; y++) {
			assertTrue(canvas.get(8, y, 12).isAir(), "gravity gate blocked at y=" + y);
		}
	}

	private static int[] edgeCell(Dir dir, int along, int depth) {
		return switch (dir) {
			case NORTH -> new int[] {along, depth};
			case SOUTH -> new int[] {along, 15 - depth};
			case WEST -> new int[] {depth, along};
			case EAST -> new int[] {15 - depth, along};
		};
	}
}
