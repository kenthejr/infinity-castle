package io.github.kenthejr.infinitycastle.gen;

public enum ModuleType {
	/** Empty space: the gaps that let you see the castle stretch away forever. */
	VOID,
	/** A room tipped on its side, floating in a void cell. Purely scenic. */
	SIDEWAYS_CHAMBER,
	/** A lone roofed platform with no connections. */
	PAVILION,
	/** Dead end: a tatami room of nested sliding screens. */
	ROOM,
	/** Enclosed shoji corridor. */
	CORRIDOR,
	/** Open vermilion bridge over the void. */
	BRIDGE,
	/** Junction hall with pillars and a hipped roof. */
	HALL,
	/** Spiral stair climbing to the equator, where gravity flips. Present in both halves of a floor. */
	STAIRWELL,
	/** The hall players arrive in. */
	ENTRANCE
}
