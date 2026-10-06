package io.github.kenthejr.infinitycastle.gen;

import static io.github.kenthejr.infinitycastle.gen.CastleGeometry.DECK_Y;
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
	private static final Set<ModuleType> BUILDINGS = EnumSet.of(
		ModuleType.ENTRANCE, ModuleType.HALL, ModuleType.ROOM, ModuleType.CORRIDOR, ModuleType.PAVILION, ModuleType.STAIRWELL
	);
	private static final Set<ModuleType> LIT = EnumSet.of(
		ModuleType.ENTRANCE, ModuleType.HALL, ModuleType.ROOM, ModuleType.CORRIDOR, ModuleType.PAVILION, ModuleType.STAIRWELL, ModuleType.BRIDGE
	);

	/** Every plan in a patch of the castle, so all designs, variants and rotations get exercised. */
	private static List<CellPlan> samplePlans() {
		CastleLayout layout = new CastleLayout(1234L);
		List<CellPlan> plans = new ArrayList<>();
		CastleLayoutTest.forEachCell((floor, half, cx, cz) -> plans.add(layout.plan(floor, half, cx, cz)));
		return plans;
	}

	/**
	 * From every open edge you can walk along the centre line, over the bridge and through the door, to just short of
	 * the spiral stair at the cell's centre: floor underfoot the whole way and headroom above.
	 */
	@Test
	void everyDoorwayLeadsOverABridgeIntoTheRoom() {
		for (CellPlan plan : samplePlans()) {
			if (plan.type() == ModuleType.SIDEWAYS_CHAMBER) {
				continue;
			}
			Canvas canvas = Modules.build(plan);
			for (Dir dir : Dir.values()) {
				if (!plan.openings().has(dir)) {
					continue;
				}
				// Stop just short of the spiral stair in the middle of the room, which is one block nearer the south
				// and east edges of the even-sized cell.
				boolean nearSide = dir == Dir.SOUTH || dir == Dir.EAST;
				int lastDepth = plan.type() == ModuleType.BRIDGE ? Canvas.SIZE_Z - 1 : Modules.CENTER - (nearSide ? 3 : 2);
				for (int depth = 0; depth <= lastDepth; depth++) {
					for (int along = Openings.DOOR_MIN; along <= Openings.DOOR_MAX; along++) {
						if (depth > 1 && along != Modules.CENTER) {
							continue; // doorways are checked across their full width only at the edge
						}
						int[] xz = edgeCell(dir, along, depth);
						assertTrue(canvas.get(xz[0], DECK_Y, xz[1]).material().isSolid(), plan + ": no floor at depth " + depth + " from " + dir);
						for (int y = DECK_Y + 1; y <= DECK_Y + Modules.DOOR_HEIGHT; y++) {
							Piece blocker = canvas.get(xz[0], y, xz[1]);
							assertTrue(blocker.isAir(), plan + ": way in from " + dir + " blocked at depth " + depth + ", y=" + y + " by " + blocker);
						}
					}
				}
			}
		}
	}

	@Test
	void closedSidesAreWalledAndHaveNoBridge() {
		for (CellPlan plan : samplePlans()) {
			if (!BUILDINGS.contains(plan.type())) {
				continue;
			}
			Canvas canvas = Modules.build(plan);
			for (Dir dir : Dir.values()) {
				if (plan.openings().has(dir)) {
					continue;
				}
				int[] edge = edgeCell(dir, Modules.CENTER, 0);
				assertTrue(canvas.get(edge[0], DECK_Y, edge[1]).isAir(), plan + ": bridge leaves a closed side " + dir);
				boolean wall = false;
				for (int depth = 0; depth < Modules.CENTER - 2 && !wall; depth++) {
					int[] xz = edgeCell(dir, Modules.CENTER, depth);
					wall = !canvas.get(xz[0], DECK_Y + 1, xz[1]).isAir();
				}
				assertTrue(wall, plan + ": missing wall on " + dir);
			}
		}
	}

	@Test
	void lanternsHangFromOrStandOnSomething() {
		for (CellPlan plan : samplePlans()) {
			Canvas canvas = Modules.build(plan);
			canvas.forEach((x, y, z, piece) -> {
				if (piece.material() == Material.LANTERN && plan.type() != ModuleType.SIDEWAYS_CHAMBER) {
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
	@EnumSource(value = ModuleType.class, names = {"HALL", "ROOM", "ENTRANCE", "PAVILION", "CORRIDOR", "STAIRWELL"})
	void buildingsHaveARoofOverTheirFloor(ModuleType type) {
		Openings openings = type == ModuleType.ROOM ? new Openings(true, false, false, false)
			: type == ModuleType.PAVILION ? Openings.NONE
			: type == ModuleType.CORRIDOR ? new Openings(false, true, false, true)
			: Openings.ALL;
		Canvas canvas = Modules.build(new CellPlan(type, openings, 0L));
		int x = Modules.CENTER - 3;
		int z = Modules.CENTER - 3;
		assertTrue(canvas.get(x, DECK_Y, z).material().isSolid(), type + ": no floor");
		boolean roof = false;
		for (int y = DECK_Y + 1; y < Canvas.SIZE_Y; y++) {
			roof |= canvas.get(x, y, z).material() == Material.SPRUCE_PLANKS;
		}
		assertTrue(roof, type + ": no roof");
	}

	@Test
	void bridgeRailsAlternateFencesAndGatesWithLanternsOnEveryFourthPost() {
		for (Axis axis : new Axis[] {Axis.Z, Axis.X}) {
			Canvas canvas = new Canvas();
			Modules.bridgeAcross(canvas, axis);
			boolean alongZ = axis == Axis.Z;
			for (int i = 0; i < Canvas.SIZE_Z; i++) {
				for (int rail : new int[] {Modules.BRIDGE_MIN, Modules.BRIDGE_MAX}) {
					int x = alongZ ? rail : i;
					int z = alongZ ? i : rail;
					Material expected = i % 2 == 0 ? Material.SPRUCE_FENCE : Material.SPRUCE_FENCE_GATE;
					assertEquals(expected, canvas.get(x, DECK_Y + 1, z).material(), "rail above at " + i + " along " + axis);
					assertEquals(expected, canvas.get(x, DECK_Y - 1, z).material(), "rail below at " + i + " along " + axis);
					assertEquals(i % 4 == 2, canvas.get(x, DECK_Y + 2, z).material() == Material.LANTERN, "lantern at " + i);
					assertEquals(i % 4 == 2, canvas.get(x, DECK_Y - 2, z).material() == Material.LANTERN, "lantern under at " + i);
					assertEquals(Material.DARK_OAK_PLANKS, canvas.get(x, DECK_Y, z).material(), "kerb at " + i);
				}
				assertEquals(Material.SPRUCE_PLANKS, canvas.get(alongZ ? Modules.CENTER : i, DECK_Y, alongZ ? i : Modules.CENTER).material());
			}
		}
		// Bridges from opposite edges of neighbouring cells meet with the pattern unbroken: both are phased on the coordinate.
		Canvas halves = new Canvas();
		Modules.bridge(halves, Dir.NORTH, 10);
		Modules.bridge(halves, Dir.SOUTH, 9);
		assertEquals(Material.SPRUCE_FENCE, halves.get(Modules.BRIDGE_MIN, DECK_Y + 1, 0).material());
		assertEquals(Material.SPRUCE_FENCE_GATE, halves.get(Modules.BRIDGE_MIN, DECK_Y + 1, Canvas.SIZE_Z - 1).material());
		assertTrue(halves.get(Modules.CENTER, DECK_Y, 9).material().isSolid());
		assertTrue(halves.get(Modules.CENTER, DECK_Y, 10).isAir());
		assertTrue(halves.get(Modules.CENTER, DECK_Y, 23).material().isSolid());
		assertTrue(halves.get(Modules.CENTER, DECK_Y, 22).isAir());
	}

	@Test
	void entranceArrivalPointIsClear() {
		Canvas canvas = Modules.build(new CellPlan(ModuleType.ENTRANCE, Openings.ALL, 0L));
		assertTrue(canvas.get(Modules.ARRIVAL_X, DECK_Y, Modules.ARRIVAL_Z).material().isSolid());
		assertTrue(canvas.get(Modules.ARRIVAL_X, DECK_Y + 1, Modules.ARRIVAL_Z).isAir());
		assertTrue(canvas.get(Modules.ARRIVAL_X, DECK_Y + 2, Modules.ARRIVAL_Z).isAir());
	}

	/**
	 * Walks from the floor of the stairwell hall up its spiral stair, out of the roof hatch and up the long staircase
	 * to the landing, only ever stepping to a horizontally adjacent block at most one higher, the way a player climbs.
	 */
	@Test
	void stairwellIsClimbableFromFloorToLanding() {
		for (int variant = 0; variant < 8; variant++) {
			Canvas canvas = Modules.build(new CellPlan(ModuleType.STAIRWELL, Openings.ALL, variant));
			// Standing heights: the y a player's feet are at when on top of the block at (x, y - 1, z).
			boolean[][][] visited = new boolean[Canvas.SIZE_X][Canvas.SIZE_Y + 1][Canvas.SIZE_Z];
			List<int[]> frontier = new ArrayList<>();
			frontier.add(new int[] {Modules.ARRIVAL_X, DECK_Y + 1, Modules.ARRIVAL_Z});
			visited[Modules.ARRIVAL_X][DECK_Y + 1][Modules.ARRIVAL_Z] = true;
			boolean reachedLanding = false;
			while (!frontier.isEmpty()) {
				int[] at = frontier.removeLast();
				if (at[1] == Modules.LANDING_Y + 1 && at[0] == Modules.LANDING_CENTER_X && at[2] == Modules.LANDING_CENTER_Z) {
					reachedLanding = true;
					break;
				}
				for (Dir dir : Dir.values()) {
					int nx = at[0] + dir.dx();
					int nz = at[2] + dir.dz();
					if (nx < 0 || nx >= Canvas.SIZE_X || nz < 0 || nz >= Canvas.SIZE_Z) {
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
			assertTrue(reachedLanding, "variant " + variant + ": could not climb to the landing");
		}
	}

	@Test
	void landingSitsJustBelowTheEquatorWithAClearGateAbove() {
		Canvas canvas = Modules.build(new CellPlan(ModuleType.STAIRWELL, Openings.NONE, 0L));
		assertEquals(Material.SPRUCE_PLANKS, canvas.get(Modules.LANDING_CENTER_X, Modules.LANDING_Y, Modules.LANDING_CENTER_Z).material());
		for (int along = Openings.DOOR_MIN; along <= Openings.DOOR_MAX; along++) {
			for (int y = Modules.LANDING_Y + 1; y < Canvas.SIZE_Y; y++) {
				assertTrue(canvas.get(along, y, Modules.LANDING_CENTER_Z).isAir(), "gravity gate blocked at " + along + "," + y);
			}
		}
		assertFalse(canvas.get(Modules.BRIDGE_MIN, Modules.LANDING_Y + 1, Modules.LANDING_CENTER_Z).isAir(), "no rail beside the landing");
	}

	@Test
	void stairwellBridgesPassUnderTheStaircase() {
		Canvas canvas = Modules.build(new CellPlan(ModuleType.STAIRWELL, Openings.ALL, 5L));
		// The south bridge runs under the landing: lanterns on the bridge and the staircase above must not meet.
		for (int z = Modules.CENTER; z < Canvas.SIZE_Z; z++) {
			for (int x = Modules.BRIDGE_MIN; x <= Modules.BRIDGE_MAX; x++) {
				int lowest = Canvas.SIZE_Y;
				for (int y = DECK_Y + 3; y < Canvas.SIZE_Y; y++) {
					if (!canvas.get(x, y, z).isAir()) {
						lowest = Math.min(lowest, y);
					}
				}
				assertTrue(lowest > DECK_Y + 2, "staircase reaches down to the bridge at " + x + "," + z);
			}
		}
	}

	private static int[] edgeCell(Dir dir, int along, int depth) {
		int last = Canvas.SIZE_X - 1;
		return switch (dir) {
			case NORTH -> new int[] {along, depth};
			case SOUTH -> new int[] {along, last - depth};
			case WEST -> new int[] {depth, along};
			case EAST -> new int[] {last - depth, along};
		};
	}
}
