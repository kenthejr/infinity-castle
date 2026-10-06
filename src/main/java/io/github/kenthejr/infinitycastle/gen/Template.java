package io.github.kenthejr.infinitycastle.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A hand-built design, imported from the designs world by {@code tools/DesignImport.java}.
 *
 * <p>Templates are text files under {@code data/infinitycastle/designs}. After comment lines starting with {@code #}
 * they hold:
 *
 * <pre>
 * size X Y Z
 * palette              one line per symbol: a character and a block state such as spruce_stairs[facing=east,half=top]
 * layer 0 … layer Y-1  Z rows of X symbols each, north row first, west column first; '.' is air
 * door north|east|south|west
 *                      the blocks that change when that side's door slides open: "x y z symbol" per line
 * </pre>
 *
 * Designs have odd widths and depths, so they have a centre block and can be turned about it without moving.
 */
public final class Template {
	private static final String RESOURCE_ROOT = "/data/infinitycastle/designs/";

	/** A block that changes when a door opens. */
	public record Patch(int x, int y, int z, Piece piece) {
	}

	private final String name;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final Piece[] pieces;
	private final Map<Dir, List<Patch>> doors;
	private final Template[] rotations = new Template[4];

	private Template(String name, int sizeX, int sizeY, int sizeZ, Piece[] pieces, Map<Dir, List<Patch>> doors) {
		this.name = name;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.pieces = pieces;
		this.doors = doors;
		this.rotations[0] = this;
	}

	/** Loads a design from the mod's resources. */
	public static Template load(String name) {
		InputStream in = Template.class.getResourceAsStream(RESOURCE_ROOT + name + ".txt");
		if (in == null) {
			throw new IllegalArgumentException("No design named " + name);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			return parse(name, reader.lines().toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Template parse(String name, List<String> lines) {
		int sizeX = -1;
		int sizeY = -1;
		int sizeZ = -1;
		Piece[] pieces = null;
		Map<Character, Piece> palette = new HashMap<>();
		Map<Dir, List<Patch>> doors = new EnumMap<>(Dir.class);
		String section = "";
		int layer = -1;
		int row = 0;
		Dir door = null;
		for (String raw : lines) {
			String line = raw.strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			String[] words = line.split("\\s+");
			switch (words[0]) {
				case "size" -> {
					sizeX = Integer.parseInt(words[1]);
					sizeY = Integer.parseInt(words[2]);
					sizeZ = Integer.parseInt(words[3]);
					pieces = new Piece[sizeX * sizeY * sizeZ];
					section = "size";
					continue;
				}
				case "palette" -> {
					section = "palette";
					continue;
				}
				case "layer" -> {
					layer = Integer.parseInt(words[1]);
					row = 0;
					section = "layer";
					continue;
				}
				case "door" -> {
					door = Dir.valueOf(words[1].toUpperCase(Locale.ROOT));
					doors.put(door, new ArrayList<>());
					section = "door";
					continue;
				}
				default -> {
				}
			}
			switch (section) {
				case "palette" -> palette.put(words[0].charAt(0), parsePiece(words[1]));
				case "layer" -> {
					if (pieces == null || row >= sizeZ || line.length() != sizeX) {
						throw new IllegalArgumentException(name + ": bad row in layer " + layer + ": " + line);
					}
					for (int x = 0; x < sizeX; x++) {
						pieces[index(sizeX, sizeZ, x, layer, row)] = symbol(name, palette, line.charAt(x));
					}
					row++;
				}
				case "door" -> doors.get(door).add(new Patch(Integer.parseInt(words[0]), Integer.parseInt(words[1]), Integer.parseInt(words[2]), symbol(name, palette, words[3].charAt(0))));
				default -> throw new IllegalArgumentException(name + ": unexpected line " + line);
			}
		}
		if (pieces == null) {
			throw new IllegalArgumentException(name + " has no size");
		}
		if (sizeX % 2 == 0 || sizeZ % 2 == 0) {
			throw new IllegalArgumentException(name + " must have an odd width and depth so it has a centre block");
		}
		for (Dir dir : Dir.values()) {
			if (!doors.containsKey(dir)) {
				throw new IllegalArgumentException(name + " has no door on its " + dir + " side");
			}
		}
		return new Template(name, sizeX, sizeY, sizeZ, pieces, doors);
	}

	private static Piece symbol(String name, Map<Character, Piece> palette, char c) {
		Piece piece = palette.get(c);
		if (piece == null) {
			throw new IllegalArgumentException(name + ": symbol '" + c + "' is not in the palette");
		}
		return piece;
	}

	/** Parses a block state such as {@code spruce_stairs[facing=east,half=top]} into a piece. */
	public static Piece parsePiece(String state) {
		int bracket = state.indexOf('[');
		Material material = Material.byBlockName(bracket < 0 ? state : state.substring(0, bracket));
		Dir facing = Dir.NORTH;
		boolean top = false;
		Axis axis = Axis.Y;
		if (bracket >= 0) {
			for (String property : state.substring(bracket + 1, state.length() - 1).split(",")) {
				String[] kv = property.split("=");
				switch (kv[0]) {
					case "facing" -> facing = Dir.valueOf(kv[1].toUpperCase(Locale.ROOT));
					case "half", "type" -> top = kv[1].equals("top");
					case "hanging" -> top = Boolean.parseBoolean(kv[1]);
					case "axis" -> axis = Axis.valueOf(kv[1].toUpperCase(Locale.ROOT));
					default -> throw new IllegalArgumentException("Unknown property in " + state);
				}
			}
		}
		return new Piece(material, facing, top, axis);
	}

	private static int index(int sizeX, int sizeZ, int x, int y, int z) {
		return (y * sizeZ + z) * sizeX + x;
	}

	public String name() {
		return this.name;
	}

	public int sizeX() {
		return this.sizeX;
	}

	public int sizeY() {
		return this.sizeY;
	}

	public int sizeZ() {
		return this.sizeZ;
	}

	public Piece get(int x, int y, int z) {
		if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY || z < 0 || z >= this.sizeZ) {
			return Piece.AIR;
		}
		Piece piece = this.pieces[index(this.sizeX, this.sizeZ, x, y, z)];
		return piece == null ? Piece.AIR : piece;
	}

	/** The blocks that change when the door on {@code side} slides open. */
	public List<Patch> door(Dir side) {
		return this.doors.get(side);
	}

	/** This design turned clockwise (seen from above) by {@code quarterTurns} about its centre block. */
	public Template rotated(int quarterTurns) {
		int turns = Math.floorMod(quarterTurns, 4);
		Template rotated = this.rotations[turns];
		if (rotated == null) {
			rotated = this;
			for (int i = 0; i < turns; i++) {
				rotated = rotated.rotatedOnce();
			}
			this.rotations[turns] = rotated;
		}
		return rotated;
	}

	private Template rotatedOnce() {
		// A quarter turn clockwise takes north to east: (x, z) → (sizeZ - 1 - z, x), and swaps the footprint's sides.
		int newSizeX = this.sizeZ;
		int newSizeZ = this.sizeX;
		Piece[] turned = new Piece[this.pieces.length];
		for (int y = 0; y < this.sizeY; y++) {
			for (int z = 0; z < this.sizeZ; z++) {
				for (int x = 0; x < this.sizeX; x++) {
					Piece piece = this.pieces[index(this.sizeX, this.sizeZ, x, y, z)];
					if (piece != null) {
						turned[index(newSizeX, newSizeZ, this.sizeZ - 1 - z, y, x)] = piece.rotateY(1);
					}
				}
			}
		}
		Map<Dir, List<Patch>> turnedDoors = new EnumMap<>(Dir.class);
		for (Dir side : Dir.values()) {
			List<Patch> patches = new ArrayList<>();
			for (Patch patch : this.doors.get(side)) {
				patches.add(new Patch(this.sizeZ - 1 - patch.z(), patch.y(), patch.x(), patch.piece().rotateY(1)));
			}
			turnedDoors.put(side.rotateCw(1), patches);
		}
		return new Template(this.name + "+90", newSizeX, this.sizeY, newSizeZ, turned, turnedDoors);
	}

	/**
	 * Draws the design with its north-west bottom corner at ({@code ox}, {@code oy}, {@code oz}), with the doors on the
	 * sides in {@code open} slid open.
	 */
	public void stamp(Canvas canvas, int ox, int oy, int oz, Openings open) {
		for (int y = 0; y < this.sizeY; y++) {
			for (int z = 0; z < this.sizeZ; z++) {
				for (int x = 0; x < this.sizeX; x++) {
					Piece piece = this.get(x, y, z);
					if (!piece.isAir()) {
						canvas.set(ox + x, oy + y, oz + z, piece);
					}
				}
			}
		}
		for (Dir side : Dir.values()) {
			if (open.has(side)) {
				for (Patch patch : this.door(side)) {
					canvas.set(ox + patch.x(), oy + patch.y(), oz + patch.z(), patch.piece());
				}
			}
		}
	}

	@Override
	public String toString() {
		return this.name;
	}
}
