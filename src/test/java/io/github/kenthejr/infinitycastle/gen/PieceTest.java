package io.github.kenthejr.infinitycastle.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PieceTest {
	static List<Piece> allPieces() {
		List<Piece> pieces = new ArrayList<>();
		for (Material material : Material.values()) {
			switch (material.shape()) {
				case STAIRS -> {
					for (Dir dir : Dir.values()) {
						pieces.add(Piece.stairs(material, dir, false));
						pieces.add(Piece.stairs(material, dir, true));
					}
				}
				case SLAB -> {
					pieces.add(Piece.slab(material, false));
					pieces.add(Piece.slab(material, true));
				}
				case AXIS -> {
					for (Axis axis : Axis.values()) {
						pieces.add(Piece.axis(material, axis));
					}
				}
				case HORIZONTAL_AXIS -> {
					pieces.add(Piece.axis(material, Axis.X));
					pieces.add(Piece.axis(material, Axis.Z));
				}
				case LANTERN -> {
					pieces.add(Piece.lantern(false));
					pieces.add(Piece.lantern(true));
				}
				default -> pieces.add(Piece.of(material));
			}
		}
		return pieces;
	}

	@Test
	void flippingTwiceIsIdentity() {
		for (Piece piece : allPieces()) {
			assertEquals(piece, piece.flipVertical().flipVertical());
		}
	}

	@Test
	void fourQuarterTurnsIsIdentity() {
		for (Piece piece : allPieces()) {
			assertEquals(piece, piece.rotateY(1).rotateY(1).rotateY(1).rotateY(1));
			assertEquals(piece.rotateY(3), piece.rotateY(-1));
		}
	}

	@Test
	void flippingSwapsHalvesAndLanterns() {
		assertEquals(Piece.lantern(false), Piece.lantern(true).flipVertical());
		assertEquals(Piece.slab(Material.WOOD_SLAB, true), Piece.slab(Material.WOOD_SLAB, false).flipVertical());
		Piece stairs = Piece.stairs(Material.ROOF_STAIRS, Dir.EAST, false);
		assertEquals(Piece.stairs(Material.ROOF_STAIRS, Dir.EAST, true), stairs.flipVertical());
	}

	@Test
	void rotatingTurnsStairsAndLogs() {
		assertEquals(Dir.SOUTH, Piece.stairs(Material.WOOD_STAIRS, Dir.EAST, false).rotateY(1).facing());
		assertEquals(Axis.Z, Piece.axis(Material.TIMBER, Axis.X).rotateY(1).axis());
		assertEquals(Axis.Y, Piece.axis(Material.TIMBER, Axis.Y).rotateY(1).axis());
	}

	@Test
	void tippedPiecesHaveASidewaysForm() {
		for (Piece piece : allPieces()) {
			Material.Shape shape = piece.tipOverX().material().shape();
			assertFalse(shape == Material.Shape.STAIRS || shape == Material.Shape.SLAB || shape == Material.Shape.LANTERN || shape == Material.Shape.CONNECTING, piece.toString());
		}
		assertEquals(Axis.Z, Piece.axis(Material.TIMBER, Axis.Y).tipOverX().axis());
	}

	@Test
	void tatamiCannotStandOnEnd() {
		assertThrows(IllegalArgumentException.class, () -> Piece.axis(Material.TATAMI, Axis.Y));
	}

	@Test
	void frameRotationMovesDoorsClockwise() {
		Canvas canvas = new Canvas();
		canvas.frame(1).set(7, 0, 0, Material.DECK); // north edge, turned once → east edge
		assertEquals(Material.DECK, canvas.get(15, 0, 7).material());
		assertEquals(Dir.EAST, Dir.NORTH.rotateCw(1));
		assertEquals(Dir.NORTH, Dir.WEST.rotateCw(1));
	}
}
