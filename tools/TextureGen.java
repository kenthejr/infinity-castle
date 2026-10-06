import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Generates the mod's textures procedurally so they're reproducible and easy to tweak.
 *
 * <p>Run from the repository root with {@code java tools/TextureGen.java}. Output overwrites the PNGs under
 * {@code src/main/resources/assets/infinitycastle}.
 */
public final class TextureGen {
	private static final File ROOT = new File("src/main/resources/assets/infinitycastle");

	public static void main(String[] args) throws IOException {
		write("textures/block/tatami_top.png", tatamiTop());
		write("textures/block/tatami_side.png", tatamiSide());
		write("textures/block/shoji.png", shoji(5, 0xF4E4BC));
		write("textures/block/shoji_screen.png", shoji(4, 0xEEDDB2));
		write("textures/block/shoji_screen_top.png", solidWood());
		write("textures/block/lacquered_planks.png", lacqueredPlanks());
		write("textures/block/paper_lantern.png", paperLantern());
		write("textures/block/fusuma.png", fusuma());
		write("textures/item/biwa.png", biwa());
		write("icon.png", icon(128));
		System.out.println("Textures written to " + ROOT.getAbsolutePath());
	}

	private static void write(String path, BufferedImage image) throws IOException {
		File file = new File(ROOT, path);
		file.getParentFile().mkdirs();
		ImageIO.write(image, "png", file);
	}

	// ---------------------------------------------------------------- helpers

	private static BufferedImage image(int size) {
		return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	}

	private static int rgb(int rgb) {
		return 0xFF000000 | rgb;
	}

	/** Scales a colour's brightness by {@code f}. */
	private static int shade(int rgb, double f) {
		int r = clamp((int) Math.round(((rgb >> 16) & 0xFF) * f));
		int g = clamp((int) Math.round(((rgb >> 8) & 0xFF) * f));
		int b = clamp((int) Math.round((rgb & 0xFF) * f));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int mix(int a, int b, double t) {
		int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(bl);
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	// ---------------------------------------------------------------- blocks

	/** Woven rush running along the texture's x axis. */
	private static BufferedImage tatamiTop() {
		BufferedImage img = image(16);
		Random random = new Random(1);
		int base = 0xB7B06A;
		for (int y = 0; y < 16; y++) {
			double row = y % 2 == 0 ? 1.04 : 0.92;
			for (int x = 0; x < 16; x++) {
				double weave = (x + (y / 2) * 3) % 4 == 0 ? 0.9 : 1.0;
				img.setRGB(x, y, shade(base, row * weave * (0.94 + random.nextDouble() * 0.1)));
			}
		}
		return img;
	}

	/** Mat side: rush on top, a dark patterned binding (heri) below. */
	private static BufferedImage tatamiSide() {
		BufferedImage img = image(16);
		Random random = new Random(2);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int color;
				if (y < 3) {
					color = shade(0xB7B06A, (y % 2 == 0 ? 1.0 : 0.9) * (0.94 + random.nextDouble() * 0.1));
				} else if (y < 13) {
					boolean motif = (x + y) % 6 == 0 || (x - y + 16) % 6 == 0;
					color = motif ? rgb(0x4A3A22) : shade(0x1E2230, 0.92 + random.nextDouble() * 0.12);
				} else {
					color = shade(0x8C8648, 0.9 + random.nextDouble() * 0.1);
				}
				img.setRGB(x, y, color);
			}
		}
		return img;
	}

	/** Back-lit paper stretched over a dark wooden lattice (kumiko). */
	private static BufferedImage shoji(int spacing, int paper) {
		BufferedImage img = image(16);
		Random random = new Random(spacing);
		int wood = 0x3A2616;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				boolean frame = x == 0 || y == 0 || x == 15 || y == 15;
				boolean lattice = x % spacing == 0 || y % spacing == 0;
				int color;
				if (frame) {
					color = shade(wood, 0.85);
				} else if (lattice) {
					color = shade(wood, 1.1);
				} else {
					double glow = 1.0 - 0.06 * Math.hypot(x - 7.5, y - 7.5) / 7.5;
					color = shade(paper, glow * (0.97 + random.nextDouble() * 0.05));
				}
				img.setRGB(x, y, color);
			}
		}
		return img;
	}

	private static BufferedImage solidWood() {
		BufferedImage img = image(16);
		Random random = new Random(3);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				img.setRGB(x, y, shade(0x3A2616, 0.9 + random.nextDouble() * 0.2));
			}
		}
		return img;
	}

	/** Vermilion (shu-iro) lacquer: long glossy boards with a highlight along each edge and almost no grain. */
	private static BufferedImage lacqueredPlanks() {
		BufferedImage img = image(16);
		Random random = new Random(4);
		int lacquer = 0xB02A1A;
		for (int y = 0; y < 16; y++) {
			int row = y % 8;
			double sheen = switch (row) {
				case 0 -> 1.28;
				case 1 -> 1.12;
				case 7 -> 0.62;
				default -> 1.0 - row * 0.025;
			};
			for (int x = 0; x < 16; x++) {
				double grain = 0.98 + random.nextDouble() * 0.04;
				img.setRGB(x, y, shade(lacquer, sheen * grain));
			}
		}
		return img;
	}

	/**
	 * Lantern atlas. The model reads: body sides from (0,0)-(8,12), caps from (8,0)-(14,6), body top/bottom from
	 * (8,8)-(16,16).
	 */
	private static BufferedImage paperLantern() {
		BufferedImage img = image(16);
		Random random = new Random(5);
		int paper = 0xC8321E;
		int black = 0x1A1210;
		for (int y = 0; y < 12; y++) {
			for (int x = 0; x < 8; x++) {
				int color;
				if (y == 0 || y == 11) {
					color = rgb(black);
				} else {
					double bulge = 1.0 - 0.25 * Math.abs(x - 3.5) / 3.5;
					double rib = y % 2 == 0 ? 0.82 : 1.0;
					color = shade(paper, (0.8 + 0.35 * bulge) * rib * (0.96 + random.nextDouble() * 0.06));
					// A pale band across the middle, like the painted crest on a chōchin.
					if (y >= 5 && y <= 6 && x >= 2 && x <= 5) {
						color = mix(color, 0xFFE8C8, 0.55);
					}
				}
				img.setRGB(x, y, color);
			}
		}
		for (int y = 0; y < 6; y++) {
			for (int x = 8; x < 14; x++) {
				img.setRGB(x, y, shade(black, y == 0 ? 1.8 : 1.0 + random.nextDouble() * 0.2));
			}
		}
		for (int y = 8; y < 16; y++) {
			for (int x = 8; x < 16; x++) {
				img.setRGB(x, y, shade(paper, 0.9 + random.nextDouble() * 0.08));
			}
		}
		return img;
	}

	/**
	 * A fusuma panel: cream paper in a dark frame, a back-lit lattice window across the upper half and a round pull
	 * (hikite) lower right.
	 */
	private static BufferedImage fusuma() {
		BufferedImage img = image(16);
		Random random = new Random(6);
		int wood = 0x3A2616;
		int paper = 0xEADBBE;
		int glow = 0xFFE9BE;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				boolean frame = x == 0 || y == 0 || x == 15 || y == 15;
				boolean inWindow = x >= 3 && x <= 12 && y >= 2 && y <= 8;
				boolean lattice = inWindow && ((x - 3) % 3 == 0 || (y - 2) % 3 == 0);
				int color;
				if (frame) {
					color = shade(wood, 0.85 + random.nextDouble() * 0.1);
				} else if (lattice) {
					color = shade(wood, 1.15);
				} else if (inWindow) {
					double light = 1.0 - 0.05 * Math.abs(x - 7.5) / 4.5;
					color = shade(glow, light * (0.97 + random.nextDouble() * 0.05));
				} else {
					color = shade(paper, 0.96 + random.nextDouble() * 0.06);
				}
				img.setRGB(x, y, color);
			}
		}
		// Hikite: a sunken oval pull.
		img.setRGB(11, 11, shade(wood, 0.7));
		img.setRGB(12, 11, shade(wood, 0.7));
		img.setRGB(11, 12, shade(wood, 0.9));
		img.setRGB(12, 12, shade(wood, 1.1));
		return img;
	}

	// ---------------------------------------------------------------- items

	/** A four-stringed biwa held diagonally: pear-shaped body lower left, bent-back pegbox upper right. */
	private static BufferedImage biwa() {
		BufferedImage img = image(16);
		int body = 0x6B3A1E;
		int face = 0x9A5A2C;
		int dark = 0x2A160A;
		int ivory = 0xE8DCC0;
		// Body: an ellipse leaning along the diagonal.
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double u = (x - 5.5 + (y - 10.5)) / Math.sqrt(2);
				double v = (x - 5.5 - (y - 10.5)) / Math.sqrt(2);
				double e = (u * u) / (5.2 * 5.2) + (v * v) / (3.6 * 3.6);
				if (e <= 1.0) {
					int color = e > 0.72 ? body : face;
					if (Math.abs(v) < 0.5 && u > -3.5 && u < 1.5) {
						color = dark; // strings across the face
					}
					img.setRGB(x, y, rgb(color));
				}
			}
		}
		// Neck.
		for (int i = 0; i < 6; i++) {
			img.setRGB(9 + i, 6 - i, rgb(body));
			img.setRGB(8 + i, 6 - i, rgb(dark));
		}
		// Pegbox bent back and pegs.
		img.setRGB(15, 0, rgb(dark));
		img.setRGB(14, 0, rgb(dark));
		img.setRGB(13, 1, rgb(ivory));
		img.setRGB(15, 2, rgb(ivory));
		// Bridge.
		img.setRGB(4, 12, rgb(ivory));
		img.setRGB(5, 11, rgb(ivory));
		return img;
	}

	/** Mod icon: shoji frames receding endlessly toward a glowing vanishing point. */
	private static BufferedImage icon(int size) {
		BufferedImage img = image(size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = (x - c) / c;
				double dy = (y - c) / c;
				double r = Math.max(Math.abs(dx), Math.abs(dy));
				// Each frame is half the size of the previous one: log scale gives evenly spaced rings.
				double depth = -Math.log(Math.max(r, 1e-4)) / Math.log(1.6);
				double frac = depth - Math.floor(depth);
				double glow = Math.pow(1.0 - r, 2.2);
				int color = mix(0x2B0C07, 0xFFB35C, Math.min(1.0, glow * 1.6));
				if (frac < 0.12) {
					color = mix(0x1A0A05, 0xC8321E, Math.min(1.0, glow * 2.2 + 0.25));
				} else if (frac > 0.2 && frac < 0.9) {
					double along = Math.abs(dx) > Math.abs(dy) ? dy / Math.max(r, 1e-4) : dx / Math.max(r, 1e-4);
					boolean lattice = Math.abs(((along + 1) * 3) % 1.0 - 0.5) > 0.46;
					if (lattice) {
						color = mix(color, 0x3A2616, 0.6);
					} else {
						color = mix(color, 0xF4E4BC, 0.35 * Math.min(1.0, glow * 3));
					}
				}
				img.setRGB(x, y, color);
			}
		}
		return img;
	}
}
