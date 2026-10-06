package io.github.kenthejr.infinitycastle.test;

import io.github.kenthejr.infinitycastle.client.CastleCamera;
import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.CastleLayout;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.world.CastleChunkGenerator;
import io.github.kenthejr.infinitycastle.world.CastleDimension;
import io.github.kenthejr.infinitycastle.world.CastleTeleporter;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * End-to-end test on a real client: enters the castle, jumps through a gravity gate and back, and saves screenshots
 * of each step to {@code build/run/clientGameTest/screenshots}.
 */
@SuppressWarnings("UnstableApiUsage")
public class CastleClientGameTest implements FabricClientGameTest {
	/** Commands from the server console run in the overworld unless told otherwise. */
	private static final String IN_CASTLE = "execute in infinitycastle:infinity_castle run ";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Clean screenshots: no hotbar or command feedback.
			context.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			singleplayer.getServer().runCommand("gamemode creative @a");
			singleplayer.getServer().runCommand("infinitycastle enter @a");
			context.waitFor(mc -> mc.player != null && CastleDimension.is(mc.player.level()));
			singleplayer.getConnection().waitForChunksRender();
			context.getInput().lookAt(180.0F, 5.0F);
			context.waitTicks(20);
			context.takeScreenshot("castle-entrance");

			// A wide view of the void, looking across and down the castle.
			singleplayer.getServer().runCommand("gamemode spectator @a");
			singleplayer.getServer().runCommand(IN_CASTLE + "tp @a 40 12 -24");
			context.getInput().lookAt(135.0F, 25.0F);
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("castle-vista");
			// Survival, because in creative a held jump key can toggle flight.
			singleplayer.getServer().runCommand("gamemode survival @a");

			// Stand on the nearest stairwell landing facing north, back down the staircase, then jump through the gravity gate.
			int floor = CastleGeometry.ENTRANCE_FLOOR;
			CastleLayout layout = singleplayer.getServer().computeOnServer(server ->
				CastleChunkGenerator.layout(server.getLevel(CastleDimension.LEVEL).getChunkSource().randomState()));
			CastleLayout.Cell cell = layout.nearestStairwell(floor, 0, 0, 32).orElseThrow(() -> new AssertionError("no stairwell near spawn"));
			Vec3 landing = CastleTeleporter.lowerLanding(floor, cell);
			singleplayer.getServer().runCommand(IN_CASTLE + String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f 180 -10", landing.x, landing.y, landing.z));
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(30);
			assertInverted(context, false, "standing on the lower landing");
			context.takeScreenshot("gate-below");

			context.getInput().holdKeyFor(options -> options.keyJump, 3);
			context.waitTicks(60);
			assertInverted(context, true, "after jumping through the gate");
			double underside = CastleTeleporter.upperLandingUnderside(floor);
			double top = context.computeOnClient(mc -> mc.player.getBoundingBox().maxY);
			if (Math.abs(top - underside) > 0.01) {
				throw new AssertionError("expected to stand on the upper landing at " + underside + " but head is at " + top);
			}
			if (!context.computeOnClient(mc -> CastleCamera.isViewUpsideDown())) {
				throw new AssertionError("camera did not roll over");
			}
			context.takeScreenshot("gate-above-inverted");

			context.getInput().holdKeyFor(options -> options.keyJump, 3);
			context.waitTicks(60);
			assertInverted(context, false, "after jumping back down");
			context.takeScreenshot("gate-back-below");
		}
	}

	private static void assertInverted(ClientGameTestContext context, boolean expected, String when) {
		boolean inverted = context.computeOnClient(mc -> {
			LocalPlayer player = mc.player;
			return player != null && GravityController.isInverted(player);
		});
		if (inverted != expected) {
			Vec3 pos = context.computeOnClient(mc -> mc.player.position());
			throw new AssertionError("gravity " + (inverted ? "inverted" : "normal") + " " + when + " at " + pos);
		}
	}
}
