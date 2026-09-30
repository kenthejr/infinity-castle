package io.github.kenthejr.infinitycastle.gen;

/**
 * Stateless, coordinate-based hashing.
 *
 * <p>Every decision the generator makes is a pure function of the world seed and coordinates, so chunks can be generated
 * in any order (and on any thread) and neighbouring cells always agree about the edge they share.
 */
public final class CastleRandom {
	private CastleRandom() {
	}

	/** SplitMix64 finaliser. */
	public static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	public static long hash(long seed, long salt, int... parts) {
		long h = mix(seed ^ mix(salt + 0x9E3779B97F4A7C15L));
		for (int part : parts) {
			h = mix(h + 0x9E3779B97F4A7C15L + part);
		}
		return h;
	}

	/** Maps a hash to {@code [0, 1)}. */
	public static double unit(long hash) {
		return (hash >>> 11) * 0x1.0p-53;
	}

	/** Maps a hash to {@code [0, bound)}. */
	public static int below(long hash, int bound) {
		return (int) Math.floorMod(hash, (long) bound);
	}

	/** Picks an independent sub-hash for the n-th decorative choice derived from one variant hash. */
	public static long fork(long hash, int n) {
		return mix(hash + 0x632BE59BD9B4E019L * (n + 1));
	}
}
