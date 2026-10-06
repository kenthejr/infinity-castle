package io.github.kenthejr.infinitycastle.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PieceTest {
	static List<Piece> allPieces() {
		List<Piece> pieces = new ArrayList<>();
		for (Material material : Material.values()) {
			switch (material.shape()) {
				case STAIRS, TRAPDOOR -> {
					for (Dir dir : Dir.values()) {
						pieces.add(new Piece(material, dir, false, Axis.Y));
						pieces.add(new Piece(material, dir, true, Axis.Y));
					}
				}
				case GATE, PANEL -> {
					for (Dir dir : Dir.values()) {
						pieces.add(new Piece(material, dir, false, Axis.Y));
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
		assertEquals(Piece.slab(Material.OAK_SLAB, true), Piece.slab(Material.OAK_SLAB, false).flipVertical());
		assertEquals(Piece.stairs(Material.SPRUCE_STAIRS, Dir.EAST, true), Piece.stairs(Material.SPRUCE_STAIRS, Dir.EAST, false).flipVertical());
		assertEquals(Piece.trapdoor(Dir.WEST, true), Piece.trapdoor(Dir.WEST, false).flipVertical());
		assertEquals(Piece.fusuma(Dir.SOUTH), Piece.fusuma(Dir.SOUTH).flipVertical());
	}

	@Test
	void rotatingTurnsFacingsAndLogs() {
		assertEquals(Dir.SOUTH, Piece.stairs(Material.SPRUCE_STAIRS, Dir.EAST, false).rotateY(1).facing());
		assertEquals(Dir.WEST, Piece.fusuma(Dir.NORTH).rotateY(3).facing());
		assertEquals(Dir.EAST, Piece.gate(Dir.WEST).rotateY(2).facing());
		assertEquals(Axis.Z, Piece.axis(Material.STRIPPED_BIRCH_LOG, Axis.X).rotateY(1).axis());
		assertEquals(Axis.Y, Piece.axis(Material.STRIPPED_BIRCH_LOG, Axis.Y).rotateY(1).axis());
	}

	@Test
	void tippedPiecesHaveASidewaysForm() {
		Set<Material.Shape> sideways = Set.of(Material.Shape.AIR, Material.Shape.CUBE, Material.Shape.AXIS, Material.Shape.PANEL);
		for (Piece piece : allPieces()) {
			Piece tipped = piece.tipOverX();
			assertTrue(sideways.contains(tipped.material().shape()), piece + " -> " + tipped);
			if (tipped.material().shape() == Material.Shape.PANEL) {
				assertEquals(Axis.X, tipped.facing().axis(), piece + " would lie flat");
			}
		}
		assertEquals(Axis.Z, Piece.axis(Material.STRIPPED_BIRCH_LOG, Axis.Y).tipOverX().axis());
		assertEquals(Material.SPRUCE_PLANKS, Piece.fusuma(Dir.NORTH).tipOverX().material());
	}

	@Test
	void factoriesCheckShapes() {
		assertThrows(IllegalArgumentException.class, () -> Piece.stairs(Material.OAK_SLAB, Dir.NORTH, false));
		assertThrows(IllegalArgumentException.class, () -> Piece.axis(Material.SPRUCE_PLANKS, Axis.X));
	}

	@Test
	void directionsTurnClockwise() {
		assertEquals(Dir.EAST, Dir.NORTH.rotateCw(1));
		assertEquals(Dir.NORTH, Dir.WEST.rotateCw(1));
		assertEquals(Dir.SOUTH, Dir.NORTH.opposite());
		assertEquals(Axis.Z, Dir.NORTH.axis());
		assertEquals(Axis.X, Dir.EAST.axis());
	}

	@Test
	void canvasOnlySetsEmptyVoxelsWhenAsked() {
		Canvas canvas = new Canvas();
		assertTrue(canvas.setIfAir(1, 1, 1, Piece.of(Material.SPRUCE_PLANKS)));
		assertTrue(!canvas.setIfAir(1, 1, 1, Piece.of(Material.OAK_PLANKS)));
		assertTrue(!canvas.setIfAir(-1, 1, 1, Piece.of(Material.OAK_PLANKS)));
		assertEquals(Material.SPRUCE_PLANKS, canvas.get(1, 1, 1).material());
		canvas.set(1, 1, 1, Piece.AIR);
		assertTrue(canvas.isEmpty());
	}
}
