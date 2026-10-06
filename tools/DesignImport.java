import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Imports the hand-built designs from the "Infinity castle : Designs" world save into the text templates the generator
 * stamps into the castle ({@code src/main/resources/data/infinitycastle/designs}).
 *
 * <p>The world is a flat Forge 1.20.1 world. Each building was saved twice: once at ground level with every wall
 * closed, and once thirty blocks higher with a doorway slid open in the middle of each side. Both copies are marked
 * with a structure block one block below their north-west corner, and the list below names them. The template keeps
 * the closed building and records, for every side, which blocks change when that side's door is opened, so the
 * generator can open just the doors a cell needs.
 *
 * <p>Run from the repository root:
 *
 * <pre>java tools/DesignImport.java "Infinity castle _ Designs.zip"</pre>
 *
 * The argument may be the zip or an extracted world folder. Nothing but the standard library is needed.
 */
public final class DesignImport {
	private static final File OUT = new File("src/main/resources/data/infinitycastle/designs");

	/** The height each design was built at, and how far above it the copy with open doors sits. */
	private static final int GROUND_Y = -60;
	private static final int OPEN_COPY_LIFT = 30;

	/** Each design's structure-block region: north-west corner and size. The structure block itself sits one block lower. */
	private static final List<Design> DESIGNS = List.of(
		new Design("plain_13", "fusuma walls, single storey", -78, -16, 13, 5, 13),
		new Design("plain_21", "fusuma walls, single storey", -78, 6, 21, 5, 21),
		new Design("plain_21x13", "fusuma walls, single storey, long", -78, 38, 21, 5, 13),
		new Design("banded_13", "dark-oak lattice band below the fusuma, tall", -43, -15, 13, 9, 13),
		new Design("banded_21", "dark-oak lattice band below the fusuma, tall", -43, 7, 21, 9, 21),
		new Design("banded_21x13", "dark-oak lattice band below the fusuma, tall, long", -43, 39, 21, 9, 13),
		new Design("veranda_19", "fusuma walls inside a lantern-lit veranda with eaves", -10, -19, 19, 5, 19),
		new Design("veranda_27", "fusuma walls inside a lantern-lit veranda with eaves", -10, 3, 27, 5, 27),
		new Design("veranda_27x19", "fusuma walls inside a lantern-lit veranda with eaves, long", -10, 35, 27, 5, 19),
		new Design("veranda_banded_19", "banded walls inside a veranda with eaves, tall", 25, -18, 19, 9, 19),
		new Design("veranda_banded_27", "banded walls inside a veranda with eaves, tall", 25, 4, 27, 9, 27),
		new Design("veranda_banded_27x19", "banded walls inside a veranda with eaves, tall, long", 25, 36, 27, 9, 19)
	);

	/** Block-state properties that describe orientation and so belong in the template. Everything else is derived. */
	private static final List<String> KEPT_PROPERTIES = List.of("facing", "half", "type", "axis", "hanging");

	private static final String SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#$%&*+-/:;<=>?@[]^_{|}~";

	record Design(String name, String description, int x, int z, int sizeX, int sizeY, int sizeZ) {
	}

	record State(String block, Map<String, String> properties) {
		/** The template's name for this state, or null for air. */
		String vocabulary() {
			String name = this.block.substring(this.block.indexOf(':') + 1);
			switch (this.block) {
				case "minecraft:air", "minecraft:cave_air", "minecraft:white_wool" -> {
					return null; // white wool marked structure corners while building
				}
				case "kimetsunoyaiba:fusuma_window" -> name = "fusuma";
				case "minecraft:oak_slab" -> {
					if ("double".equals(this.properties.get("type"))) {
						return "oak_planks";
					}
				}
				default -> {
				}
			}
			TreeMap<String, String> kept = new TreeMap<>();
			for (String key : KEPT_PROPERTIES) {
				if (this.properties.containsKey(key)) {
					kept.put(key, this.properties.get(key));
				}
			}
			if (kept.isEmpty()) {
				return name;
			}
			StringBuilder sb = new StringBuilder(name).append('[');
			kept.forEach((k, v) -> sb.append(sb.charAt(sb.length() - 1) == '[' ? "" : ",").append(k).append('=').append(v));
			return sb.append(']').toString();
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("usage: java tools/DesignImport.java <world folder or zip>");
			System.exit(2);
		}
		World world = World.open(new File(args[0]));
		OUT.mkdirs();
		for (Design design : DESIGNS) {
			String[][][] closed = world.read(design.x, GROUND_Y, design.z, design.sizeX, design.sizeY, design.sizeZ);
			String[][][] open = world.read(design.x, GROUND_Y + OPEN_COPY_LIFT, design.z, design.sizeX, design.sizeY, design.sizeZ);
			completeFloor(closed);
			completeFloor(open);
			write(design, closed, open);
		}
	}

	/**
	 * The designs were built on grass, so the ground under the foot of each spiral stair was never part of the saved
	 * structure. In the castle there is only void below a room, so fill those gaps with the room's floor.
	 */
	private static void completeFloor(String[][][] blocks) {
		Map<String, Integer> counts = new HashMap<>();
		for (String[] row : blocks[0]) {
			for (String state : row) {
				if (state != null) {
					counts.merge(state, 1, Integer::sum);
				}
			}
		}
		String floor = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
		for (String[] row : blocks[0]) {
			for (int x = 0; x < row.length; x++) {
				if (row[x] == null) {
					row[x] = floor;
				}
			}
		}
	}

	private static void write(Design design, String[][][] closed, String[][][] open) throws IOException {
		// Palette by frequency, so the most common blocks get the first letters.
		Map<String, Integer> counts = new HashMap<>();
		for (String[][] layer : closed) {
			for (String[] row : layer) {
				for (String state : row) {
					if (state != null) {
						counts.merge(state, 1, Integer::sum);
					}
				}
			}
		}
		for (String[][] layer : open) {
			for (String[] row : layer) {
				for (String state : row) {
					if (state != null) {
						counts.putIfAbsent(state, 0);
					}
				}
			}
		}
		List<String> ordered = new ArrayList<>(counts.keySet());
		ordered.sort((a, b) -> !counts.get(b).equals(counts.get(a)) ? counts.get(b) - counts.get(a) : a.compareTo(b));
		Map<String, Character> palette = new LinkedHashMap<>();
		for (String state : ordered) {
			if (palette.size() >= SYMBOLS.length()) {
				throw new IllegalStateException(design.name + " uses more block states than the template alphabet has symbols");
			}
			palette.put(state, SYMBOLS.charAt(palette.size()));
		}
		Function<String, Character> symbol = state -> state == null ? '.' : palette.get(state);

		File file = new File(OUT, design.name + ".txt");
		try (PrintWriter out = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
			out.printf("# Infinity Castle design \"%s\": %s.%n", design.name, design.description);
			out.printf("# Imported by tools/DesignImport.java from the structure-block regions at (%d, %d, %d) (doors closed)%n", design.x, GROUND_Y, design.z);
			out.printf("# and (%d, %d, %d) (doors open) of the \"Infinity castle : Designs\" world. Edit the world and re-import rather than editing this file.%n", design.x, GROUND_Y + OPEN_COPY_LIFT, design.z);
			out.println("# Layers are listed bottom up. Within a layer each row runs west to east and the rows run north to south.");
			out.printf("size %d %d %d%n", design.sizeX, design.sizeY, design.sizeZ);
			out.println("palette");
			out.println(". air");
			palette.forEach((state, c) -> out.println(c + " " + state));
			for (int y = 0; y < design.sizeY; y++) {
				out.println("layer " + y);
				for (int z = 0; z < design.sizeZ; z++) {
					StringBuilder row = new StringBuilder();
					for (int x = 0; x < design.sizeX; x++) {
						row.append(symbol.apply(closed[y][z][x]));
					}
					out.println(row);
				}
			}
			// Door patches: what changes on each side when its door is slid open.
			Map<String, List<String>> doors = new TreeMap<>();
			int unclassified = 0;
			for (int y = 0; y < design.sizeY; y++) {
				for (int z = 0; z < design.sizeZ; z++) {
					for (int x = 0; x < design.sizeX; x++) {
						String a = closed[y][z][x];
						String b = open[y][z][x];
						if (a == null ? b == null : a.equals(b)) {
							continue;
						}
						String side = side(x, z, design.sizeX, design.sizeZ);
						if (side == null) {
							unclassified++;
							continue;
						}
						doors.computeIfAbsent(side, k -> new ArrayList<>()).add(x + " " + y + " " + z + " " + symbol.apply(b));
					}
				}
			}
			if (unclassified > 0) {
				throw new IllegalStateException(design.name + ": " + unclassified + " changed blocks are not near one side");
			}
			for (String side : List.of("north", "east", "south", "west")) {
				List<String> patch = doors.get(side);
				if (patch == null) {
					throw new IllegalStateException(design.name + " has no door on its " + side + " side");
				}
				out.println("door " + side);
				patch.forEach(out::println);
			}
			System.out.printf(Locale.ROOT, "%-22s %2dx%dx%-2d  %4d blocks, %2d states, door patches %s%n", design.name, design.sizeX, design.sizeY, design.sizeZ,
				counts.values().stream().mapToInt(Integer::intValue).sum(), palette.size(),
				doors.values().stream().map(List::size).toList());
		}
	}

	/** The side a changed block belongs to: the nearest edge, which is unambiguous for doors centred on each side. */
	private static String side(int x, int z, int sizeX, int sizeZ) {
		int west = x;
		int east = sizeX - 1 - x;
		int north = z;
		int south = sizeZ - 1 - z;
		int min = Math.min(Math.min(west, east), Math.min(north, south));
		int ties = (west == min ? 1 : 0) + (east == min ? 1 : 0) + (north == min ? 1 : 0) + (south == min ? 1 : 0);
		if (ties != 1) {
			return null;
		}
		return min == west ? "west" : min == east ? "east" : min == north ? "north" : "south";
	}

	// ---------------------------------------------------------------- world access

	/** Region files of a world, read from a folder or straight out of a zip. */
	private static final class World {
		private final Map<String, byte[]> regions = new HashMap<>();
		private final Map<Long, String[][][]> chunkCache = new HashMap<>();

		static World open(File source) throws IOException {
			World world = new World();
			Pattern pattern = Pattern.compile("(?:^|/)region/(r\\.-?\\d+\\.-?\\d+\\.mca)$");
			if (source.isDirectory()) {
				File[] files = new File(source, "region").listFiles();
				if (files == null) {
					throw new IOException("No region folder in " + source);
				}
				for (File file : files) {
					world.regions.put(file.getName(), Files.readAllBytes(file.toPath()));
				}
			} else {
				try (ZipFile zip = new ZipFile(source)) {
					for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
						ZipEntry entry = e.nextElement();
						String name = entry.getName().replace('\\', '/');
						Matcher m = pattern.matcher(name);
						// Only the overworld's region folder, not the ones under dimensions/.
						if (m.find() && !name.contains("dimensions/") && !name.contains("DIM")) {
							try (InputStream in = zip.getInputStream(entry)) {
								world.regions.put(m.group(1), in.readAllBytes());
							}
						}
					}
				}
			}
			if (world.regions.isEmpty()) {
				throw new IOException("No region files found in " + source);
			}
			return world;
		}

		/** Block states in a box, indexed [y][z][x], as template vocabulary strings (null for air). */
		String[][][] read(int x0, int y0, int z0, int sizeX, int sizeY, int sizeZ) {
			String[][][] out = new String[sizeY][sizeZ][sizeX];
			for (int y = 0; y < sizeY; y++) {
				for (int z = 0; z < sizeZ; z++) {
					for (int x = 0; x < sizeX; x++) {
						out[y][z][x] = block(x0 + x, y0 + y, z0 + z);
					}
				}
			}
			return out;
		}

		private String block(int x, int y, int z) {
			int cx = Math.floorDiv(x, 16);
			int cz = Math.floorDiv(z, 16);
			String[][][] chunk = this.chunkCache.computeIfAbsent(((long) cx << 32) | (cz & 0xFFFFFFFFL), key -> this.loadChunk(cx, cz));
			int sy = y + 64;
			if (chunk == null || sy < 0 || sy >= chunk.length) {
				return null;
			}
			return chunk[sy][z & 15][x & 15];
		}

		/** Decodes one chunk into [y + 64][z][x] vocabulary strings. */
		private String[][][] loadChunk(int cx, int cz) {
			byte[] region = this.regions.get("r." + Math.floorDiv(cx, 32) + "." + Math.floorDiv(cz, 32) + ".mca");
			if (region == null) {
				return null;
			}
			int index = (cx & 31) + (cz & 31) * 32;
			int offset = ((region[index * 4] & 0xFF) << 16 | (region[index * 4 + 1] & 0xFF) << 8 | (region[index * 4 + 2] & 0xFF)) * 4096;
			if (offset == 0) {
				return null;
			}
			int length = (region[offset] & 0xFF) << 24 | (region[offset + 1] & 0xFF) << 16 | (region[offset + 2] & 0xFF) << 8 | (region[offset + 3] & 0xFF);
			int compression = region[offset + 4];
			if (compression != 2) {
				throw new IllegalStateException("Unsupported chunk compression " + compression);
			}
			try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(region, offset + 5, length - 1)))) {
				Map<?, ?> root = (Map<?, ?>) Nbt.read(in);
				String[][][] blocks = new String[384][16][16];
				for (Object sectionObj : (List<?>) root.get("sections")) {
					Map<?, ?> section = (Map<?, ?>) sectionObj;
					Map<?, ?> states = (Map<?, ?>) section.get("block_states");
					if (states == null) {
						continue;
					}
					List<?> paletteNbt = (List<?>) states.get("palette");
					String[] palette = new String[paletteNbt.size()];
					for (int i = 0; i < palette.length; i++) {
						Map<?, ?> entry = (Map<?, ?>) paletteNbt.get(i);
						Map<String, String> properties = new HashMap<>();
						Map<?, ?> props = (Map<?, ?>) entry.get("Properties");
						if (props != null) {
							props.forEach((k, v) -> properties.put((String) k, (String) v));
						}
						palette[i] = new State((String) entry.get("Name"), properties).vocabulary();
					}
					int baseY = ((Number) section.get("Y")).intValue() * 16 + 64;
					if (baseY < 0 || baseY >= 384) {
						continue;
					}
					long[] data = (long[]) states.get("data");
					if (data == null) {
						for (int i = 0; i < 4096; i++) {
							blocks[baseY + (i >> 8)][(i >> 4) & 15][i & 15] = palette[0];
						}
						continue;
					}
					int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.length - 1));
					int perLong = 64 / bits;
					long mask = (1L << bits) - 1;
					for (int i = 0; i < 4096; i++) {
						long word = data[i / perLong];
						int value = (int) ((word >>> ((i % perLong) * bits)) & mask);
						blocks[baseY + (i >> 8)][(i >> 4) & 15][i & 15] = palette[value];
					}
				}
				return blocks;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	/** Just enough of the NBT format to read chunk data: compounds, lists, numbers, strings and arrays. */
	private static final class Nbt {
		static Object read(DataInputStream in) throws IOException {
			int type = in.readByte();
			in.readUTF(); // root name
			return readPayload(in, type);
		}

		private static Object readPayload(DataInputStream in, int type) throws IOException {
			return switch (type) {
				case 1 -> in.readByte();
				case 2 -> in.readShort();
				case 3 -> in.readInt();
				case 4 -> in.readLong();
				case 5 -> in.readFloat();
				case 6 -> in.readDouble();
				case 7 -> {
					byte[] bytes = new byte[in.readInt()];
					in.readFully(bytes);
					yield bytes;
				}
				case 8 -> in.readUTF();
				case 9 -> {
					int elementType = in.readByte();
					int size = in.readInt();
					List<Object> list = new ArrayList<>(size);
					for (int i = 0; i < size; i++) {
						list.add(readPayload(in, elementType));
					}
					yield list;
				}
				case 10 -> {
					Map<String, Object> map = new HashMap<>();
					int t;
					while ((t = in.readByte()) != 0) {
						String name = in.readUTF();
						map.put(name, readPayload(in, t));
					}
					yield map;
				}
				case 11 -> {
					int[] ints = new int[in.readInt()];
					for (int i = 0; i < ints.length; i++) {
						ints[i] = in.readInt();
					}
					yield ints;
				}
				case 12 -> {
					long[] longs = new long[in.readInt()];
					for (int i = 0; i < longs.length; i++) {
						longs[i] = in.readLong();
					}
					yield longs;
				}
				default -> throw new IOException("Unknown NBT tag " + type);
			};
		}
	}

	private DesignImport() {
	}
}
