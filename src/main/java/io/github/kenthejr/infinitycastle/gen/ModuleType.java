package io.github.kenthejr.infinitycastle.gen;

public enum ModuleType {
	/** Empty space: the gaps that let you see the castle stretch away forever. */
	VOID,
	/** A small room tipped on its side, floating in a void cell. Purely scenic. */
	SIDEWAYS_CHAMBER,
	/** A lone small room with no bridges. */
	PAVILION,
	/** Dead end: a room with a single bridge. */
	ROOM,
	/** A long room with a bridge at each end. */
	CORRIDOR,
	/** An open bridge across the whole cell. */
	BRIDGE,
	/** A large room at a junction of bridges. */
	HALL,
	/** A tall hall whose roof carries the long staircase up to the equator, where gravity flips. Present in both halves of a floor. */
	STAIRWELL,
	/** The hall players arrive in. */
	ENTRANCE
}
