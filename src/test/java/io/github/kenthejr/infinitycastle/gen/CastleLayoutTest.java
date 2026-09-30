package io.github.kenthejr.infinitycastle.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CastleLayoutTest {
	private static final int RADIUS = 12;

	@ParameterizedTest
	@ValueSource(longs = {0L, 42L, -7L, 123456789L, Long.MIN_VALUE})
	void neighboursAgreeOnEverySharedEdge(long seed) {
		CastleLayout layout = new CastleLayout(seed);
		for (int floor = 0; floor < CastleGeometry.FLOOR_COUNT; floor++) {
			for (Half half : Half.values()) {
				for (int cx = -RADIUS; cx < RADIUS; cx++) {
					for (int cz = -RADIUS; cz < RADIUS; cz++) {
						Openings here = layout.openings(floor, half, cx, cz);
						assertEquals(here.east(), layout.openings(floor, half, cx + 1, cz).west(), "east/west edge at " + cx + "," + cz);
						assertEquals(here.south(), layout.openings(floor, half, cx, cz + 1).north(), "south/north edge at " + cx + "," + cz);
					}
				}
			}
		}
	}

	@Test
	void voidCellsHaveNoDoorsAndDoorsLeadSomewhere() {
		CastleLayout layout = new CastleLayout(99L);
		forEachCell((floor, half, cx, cz) -> {
			Openings openings = layout.openings(floor, half, cx, cz);
			if (layout.isVoid(floor, half, cx, cz)) {
				assertEquals(Openings.NONE, openings);
				return;
			}
			for (Dir dir : Dir.values()) {
				if (openings.has(dir)) {
					assertFalse(layout.isVoid(floor, half, cx + dir.dx(), cz + dir.dz()), "door into the void at " + cx + "," + cz + " " + dir);
				}
			}
		});
	}

	@Test
	void planIsDeterministic() {
		CastleLayout a = new CastleLayout(2024L);
		CastleLayout b = new CastleLayout(2024L);
		forEachCell((floor, half, cx, cz) -> assertEquals(a.plan(floor, half, cx, cz), b.plan(floor, half, cx, cz)));
	}

	@Test
	void differentSeedsBuildDifferentCastles() {
		CastleLayout a = new CastleLayout(1L);
		CastleLayout b = new CastleLayout(2L);
		int differences = 0;
		for (int cx = -RADIUS; cx < RADIUS; cx++) {
			for (int cz = -RADIUS; cz < RADIUS; cz++) {
				if (!a.plan(3, Half.LOWER, cx, cz).equals(b.plan(3, Half.LOWER, cx, cz))) {
					differences++;
				}
			}
		}
		assertTrue(differences > 100, "only " + differences + " cells differ");
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, 1L, 42L, -99L, 8675309L})
	void entranceIsAlwaysAFourWayHall(long seed) {
		CastleLayout layout = new CastleLayout(seed);
		CellPlan entrance = layout.plan(CastleGeometry.ENTRANCE_FLOOR, Half.LOWER, 0, 0);
		assertEquals(ModuleType.ENTRANCE, entrance.type());
		assertEquals(Openings.ALL, entrance.openings());
		for (Dir dir : Dir.values()) {
			ModuleType neighbour = layout.plan(CastleGeometry.ENTRANCE_FLOOR, Half.LOWER, dir.dx(), dir.dz()).type();
			assertNotEquals(ModuleType.VOID, neighbour);
			assertNotEquals(ModuleType.STAIRWELL, neighbour);
		}
	}

	@Test
	void stairwellsExistInBothHalvesSoTheyMeetAtTheEquator() {
		CastleLayout layout = new CastleLayout(5L);
		int stairwells = 0;
		for (int floor = 0; floor < CastleGeometry.FLOOR_COUNT; floor++) {
			for (int cx = -RADIUS; cx < RADIUS; cx++) {
				for (int cz = -RADIUS; cz < RADIUS; cz++) {
					boolean lower = layout.plan(floor, Half.LOWER, cx, cz).type() == ModuleType.STAIRWELL;
					boolean upper = layout.plan(floor, Half.UPPER, cx, cz).type() == ModuleType.STAIRWELL;
					assertEquals(lower, upper, "stairwell mismatch at floor " + floor + " " + cx + "," + cz);
					if (lower) {
						stairwells++;
					}
				}
			}
		}
		assertTrue(stairwells > 0);
	}

	@Test
	void moduleTypeMatchesConnections() {
		CastleLayout layout = new CastleLayout(77L);
		forEachCell((floor, half, cx, cz) -> {
			CellPlan plan = layout.plan(floor, half, cx, cz);
			Openings o = plan.openings();
			switch (plan.type()) {
				case VOID, SIDEWAYS_CHAMBER -> assertEquals(Openings.NONE, o);
				case PAVILION -> assertEquals(0, o.count());
				case ROOM -> assertEquals(1, o.count());
				case CORRIDOR, BRIDGE -> assertTrue(o.isStraight());
				case HALL -> assertTrue(o.count() >= 3 || o.count() == 2 && !o.isStraight());
				case STAIRWELL, ENTRANCE -> {
				}
			}
		});
	}

	@Test
	void mixOfModulesIsReasonable() {
		CastleLayout layout = new CastleLayout(31337L);
		Map<ModuleType, Integer> counts = new EnumMap<>(ModuleType.class);
		int[] total = {0};
		forEachCell((floor, half, cx, cz) -> {
			counts.merge(layout.plan(floor, half, cx, cz).type(), 1, Integer::sum);
			total[0]++;
		});
		double voidFraction = (counts.getOrDefault(ModuleType.VOID, 0) + counts.getOrDefault(ModuleType.SIDEWAYS_CHAMBER, 0)) / (double) total[0];
		assertTrue(voidFraction > 0.2 && voidFraction < 0.6, "void fraction " + voidFraction);
		for (ModuleType type : ModuleType.values()) {
			if (type != ModuleType.ENTRANCE) {
				assertTrue(counts.getOrDefault(type, 0) > 0, "no " + type + " generated");
			}
		}
	}

	@Test
	void outsideTheCastleIsVoid() {
		CastleLayout layout = new CastleLayout(0L);
		assertEquals(CellPlan.VOID, layout.plan(-1, Half.LOWER, 0, 0));
		assertEquals(CellPlan.VOID, layout.plan(CastleGeometry.FLOOR_COUNT, Half.UPPER, 3, 3));
	}

	@FunctionalInterface
	interface CellVisitor {
		void visit(int floor, Half half, int cx, int cz);
	}

	static void forEachCell(CellVisitor visitor) {
		for (int floor = 0; floor < CastleGeometry.FLOOR_COUNT; floor++) {
			for (Half half : Half.values()) {
				for (int cx = -RADIUS; cx < RADIUS; cx++) {
					for (int cz = -RADIUS; cz < RADIUS; cz++) {
						visitor.visit(floor, half, cx, cz);
					}
				}
			}
		}
	}
}
