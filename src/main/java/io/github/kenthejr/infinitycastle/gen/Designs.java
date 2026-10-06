package io.github.kenthejr.infinitycastle.gen;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * The catalogue of hand-built designs and which modules use them.
 *
 * <p>Twelve buildings were designed in-game: three footprints (a square, a larger square and a long room) in four
 * styles. "Plain" rooms have fusuma walls; "banded" rooms are taller, with a dark-oak lattice band below the fusuma;
 * "veranda" rooms are wrapped in a lantern-lit veranda under spruce eaves. Every building has a spiral stair at its
 * centre climbing to a hatch in its flat roof, and a doorway that can slide open in the middle of each side.
 */
public final class Designs {
	public static final String ENTRANCE = "veranda_banded_27";

	/** The small squares: lone pavilions, dead ends and the chambers that float on their sides. */
	public static final List<String> SMALL = List.of("plain_13", "banded_13", "veranda_19", "veranda_banded_19");
	/** The large squares: halls at junctions. */
	public static final List<String> LARGE = List.of("plain_21", "banded_21", "veranda_27", "veranda_banded_27");
	/** The long rooms, with their long axis running east–west: corridors. */
	public static final List<String> LONG = List.of("plain_21x13", "banded_21x13", "veranda_27x19", "veranda_banded_27x19");
	/** Tall designs whose roof is high enough for the stair up to the gravity gate to fit in a half. */
	public static final List<String> TALL = List.of("banded_13", "banded_21", "veranda_banded_19", "veranda_banded_27");
	/** Designs small enough to lie on their side inside a half. */
	public static final List<String> SIDEWAYS = List.of("plain_13", "banded_13");

	public static final List<String> ALL = Stream.of(SMALL, LARGE, LONG).flatMap(List::stream).toList();

	private static final ConcurrentMap<String, Template> LOADED = new ConcurrentHashMap<>();

	private Designs() {
	}

	public static Template get(String name) {
		return LOADED.computeIfAbsent(name, Template::load);
	}

	/** One of {@code names}, chosen by a hash. */
	public static Template pick(List<String> names, long hash) {
		return get(names.get(CastleRandom.below(hash, names.size())));
	}
}
