package io.github.kenthejr.infinitycastle.test;

import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.CastleLayout;
import io.github.kenthejr.infinitycastle.gen.CellPlacer;
import io.github.kenthejr.infinitycastle.gen.Half;
import io.github.kenthejr.infinitycastle.gen.Material;
import io.github.kenthejr.infinitycastle.gen.ModuleType;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.registry.ModBlocks;
import io.github.kenthejr.infinitycastle.world.CastleChunkGenerator;
import io.github.kenthejr.infinitycastle.world.CastleDimension;
import io.github.kenthejr.infinitycastle.world.CastlePalette;
import io.github.kenthejr.infinitycastle.world.CastleTeleporter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server-side game tests: these run inside a real dedicated-server world with the mod loaded. */
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only way to get a server player in a game test
public class CastleGameTest {
	private static ServerLevel castle(GameTestHelper helper) {
		ServerLevel castle = helper.getLevel().getServer().getLevel(CastleDimension.LEVEL);
		helper.assertTrue(castle != null, "The castle dimension is not loaded");
		helper.assertTrue(castle.getChunkSource().getGenerator() instanceof CastleChunkGenerator, "The castle uses the wrong generator");
		return castle;
	}

	/** Every block the pure layout says belongs in a chunk is really there, in the right state. */
	@GameTest
	public void generatedChunksMatchTheLayout(GameTestHelper helper) {
		ServerLevel castle = castle(helper);
		CastleLayout layout = CastleChunkGenerator.layout(castle.getChunkSource().randomState());
		for (int cx = -1; cx <= 1; cx++) {
			for (int cz = -1; cz <= 1; cz++) {
				castle.getChunk(cx, cz);
				int ox = cx * 16;
				int oz = cz * 16;
				int[] checked = {0};
				CellPlacer.placeColumn(layout, cx, cz, (x, y, z, piece) -> {
					BlockState actual = castle.getBlockState(new BlockPos(ox + x, y, oz + z));
					boolean match = piece.material().shape() == Material.Shape.CONNECTING
						? actual.is(CastlePalette.block(piece.material()))
						: actual.equals(CastlePalette.state(piece));
					if (!match) {
						throw helper.assertionException("At " + (ox + x) + "," + y + "," + (oz + z) + " expected " + CastlePalette.state(piece) + " but found " + actual);
					}
					checked[0]++;
				});
				helper.assertTrue(checked[0] > 0 || layout.plan(CastleGeometry.ENTRANCE_FLOOR, Half.LOWER, cx, cz).type() == ModuleType.VOID, "chunk " + cx + "," + cz + " is empty");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void entranceHallIsSafeToArriveIn(GameTestHelper helper) {
		ServerLevel castle = castle(helper);
		BlockPos arrival = BlockPos.containing(CastleTeleporter.ENTRANCE);
		helper.assertTrue(castle.getBlockState(arrival.below()).is(ModBlocks.TATAMI), "no tatami under the arrival point");
		helper.assertTrue(castle.getBlockState(arrival).isAir(), "arrival point is blocked");
		helper.assertTrue(castle.getBlockState(arrival.above()).isAir(), "no headroom at the arrival point");
		helper.succeed();
	}

	@GameTest
	public void playersEnterAndLeave(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ServerLevel origin = player.level();
		BlockPos start = player.blockPosition();

		helper.assertTrue(CastleTeleporter.enter(player), "enter failed");
		helper.assertTrue(CastleDimension.is(player.level()), "player is not in the castle");
		helper.assertTrue(player.position().distanceTo(CastleTeleporter.ENTRANCE) < 0.01, "player did not arrive at the entrance: " + player.position());
		GlobalPos returnPoint = player.getAttached(CastleTeleporter.RETURN_POINT);
		helper.assertTrue(returnPoint != null && returnPoint.dimension() == origin.dimension() && returnPoint.pos().equals(start), "return point not recorded");

		helper.assertTrue(CastleTeleporter.leave(player), "leave failed");
		helper.assertTrue(player.level().dimension() == origin.dimension(), "player did not return home");
		helper.assertTrue(player.blockPosition().equals(start), "player returned to the wrong place: " + player.blockPosition());
		helper.assertTrue(!player.hasAttached(CastleTeleporter.RETURN_POINT), "return point not cleared");
		helper.succeed();
	}

	@GameTest
	public void gravityFlipsAtTheEquator(GameTestHelper helper) {
		ServerLevel castle = castle(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CastleTeleporter.enter(player);
		int floor = CastleGeometry.ENTRANCE_FLOOR;
		double equator = CastleGeometry.equatorY(floor);

		place(player, castle, equator - 6);
		GravityController.tick(player);
		helper.assertTrue(!GravityController.isInverted(player), "gravity inverted below the equator");
		helper.assertValueEqual(player.getAttributeValue(Attributes.GRAVITY), 0.08, "gravity below the equator");
		float uprightEye = player.getEyeHeight();

		place(player, castle, equator + 1);
		GravityController.tick(player);
		helper.assertTrue(GravityController.isInverted(player), "gravity did not flip above the equator");
		helper.assertTrue(player.getAttributeValue(Attributes.GRAVITY) < 0, "gravity attribute is not negative");
		helper.assertTrue(Math.abs(player.getEyeHeight() - (player.getBbHeight() - uprightEye)) < 1.0E-4, "eye height not mirrored: " + player.getEyeHeight());

		place(player, castle, equator - 3);
		GravityController.tick(player);
		helper.assertTrue(!GravityController.isInverted(player), "gravity did not flip back");
		helper.assertTrue(Math.abs(player.getEyeHeight() - uprightEye) < 1.0E-4, "eye height not restored");

		CastleTeleporter.leave(player);
		GravityController.tick(player);
		helper.assertValueEqual(player.getAttributeValue(Attributes.GRAVITY), 0.08, "gravity outside the castle");
		helper.succeed();
	}

	/** An inverted player moving up into a ceiling lands on it, and jumping pushes them down, away from it. */
	@GameTest
	public void invertedPlayersStandOnCeilings(GameTestHelper helper) {
		ServerLevel castle = castle(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		CastleTeleporter.enter(player);
		// A ceiling test pad in an empty cell of the entrance floor's upper half, where gravity is inverted.
		int floor = CastleGeometry.ENTRANCE_FLOOR;
		CastleLayout layout = CastleChunkGenerator.layout(castle.getChunkSource().randomState());
		int cx = 1;
		while (layout.plan(floor, Half.UPPER, cx, 0).type() != ModuleType.VOID) {
			cx++;
		}
		int x = cx * 16 + 8;
		BlockPos ceiling = new BlockPos(x, (int) CastleGeometry.equatorY(floor) + 10, 8);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				castle.setBlockAndUpdate(ceiling.offset(dx, 0, dz), ModBlocks.LACQUERED_PLANKS.defaultBlockState());
			}
		}
		player.teleportTo(castle, x + 0.5, ceiling.getY() - 2.0, 8.5, java.util.Set.of(), 0.0F, 0.0F, false);
		GravityController.tick(player);
		helper.assertTrue(GravityController.isInverted(player), "not inverted in the upper half");

		player.move(MoverType.PLAYER, new Vec3(0.0, 1.0, 0.0));
		helper.assertTrue(player.onGround(), "touching the ceiling does not count as ground");
		helper.assertTrue(Math.abs(player.getBoundingBox().maxY - ceiling.getY()) < 1.0E-6, "did not stop at the ceiling: " + player.getBoundingBox().maxY);

		player.setDeltaMovement(Vec3.ZERO);
		player.jumpFromGround();
		helper.assertTrue(player.getDeltaMovement().y < 0.0, "jumped toward the ceiling instead of away: " + player.getDeltaMovement());
		helper.succeed();
	}

	private static void place(ServerPlayer player, ServerLevel castle, double centerY) {
		player.teleportTo(castle, 8.5, centerY - player.getBbHeight() / 2.0, 8.5, java.util.Set.of(), 0.0F, 0.0F, false);
	}
}
