package io.github.kenthejr.infinitycastle.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FloorProfileTest {
	@Test
	void entranceFloorIsCalm() {
		FloorProfile entrance = FloorProfile.of(CastleGeometry.ENTRANCE_FLOOR);
		assertEquals(1.0, entrance.gravityScale());
		assertEquals(0.0F, entrance.cameraTilt());
	}

	@Test
	void everyFloorHasSaneValues() {
		boolean anyDrifting = false;
		boolean anyTilted = false;
		for (int floor = 0; floor < CastleGeometry.FLOOR_COUNT; floor++) {
			FloorProfile profile = FloorProfile.of(floor);
			assertTrue(profile.gravityScale() > 0.0 && profile.gravityScale() <= 1.0);
			assertTrue(Math.abs(profile.cameraTilt()) <= 15.0F);
			anyDrifting |= profile.drifting();
			anyTilted |= profile.cameraTilt() != 0.0F;
		}
		assertTrue(anyDrifting);
		assertTrue(anyTilted);
	}

	@Test
	void profilesRepeatWithTheCastle() {
		assertEquals(FloorProfile.of(3), FloorProfile.of(3 + CastleGeometry.FLOOR_COUNT));
		assertEquals(FloorProfile.of(CastleGeometry.FLOOR_COUNT - 1), FloorProfile.of(-1));
	}

	@Test
	void japaneseNames() {
		assertEquals("第九層", FloorProfile.of(8).japaneseName());
		assertEquals("第一層", FloorProfile.of(0).japaneseName());
		assertEquals("第十六層", FloorProfile.of(15).japaneseName());
	}

	@ParameterizedTest
	@CsvSource({
		"1, 一",
		"9, 九",
		"10, 十",
		"11, 十一",
		"20, 二十",
		"99, 九十九",
		"100, 百",
		"101, 百一",
		"110, 百十",
		"1000, 千",
		"2026, 二千二十六",
		"9999, 九千九百九十九"
	})
	void kanjiNumerals(int n, String expected) {
		assertEquals(expected, KanjiNumerals.of(n));
	}

	@Test
	void kanjiNumeralsRejectOutOfRange() {
		assertThrows(IllegalArgumentException.class, () -> KanjiNumerals.of(0));
		assertThrows(IllegalArgumentException.class, () -> KanjiNumerals.of(10000));
	}
}
