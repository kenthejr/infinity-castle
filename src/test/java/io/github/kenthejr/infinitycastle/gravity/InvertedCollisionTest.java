package io.github.kenthejr.infinitycastle.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

/** Exercises the mirrored step logic against real Minecraft collision shapes. */
class InvertedCollisionTest {
	private static final float PLAYER_STEP = 0.6F;
	private static final double CEILING = 10.0;

	/** A ceiling slab from x = -2 to 4 with its underside at {@link #CEILING}, plus a ledge hanging below it ahead. */
	private static List<VoxelShape> world(double ledgeDepth) {
		List<VoxelShape> shapes = new ArrayList<>();
		shapes.add(Shapes.create(new AABB(-2, CEILING, -2, 4, CEILING + 1, 2)));
		if (ledgeDepth > 0) {
			shapes.add(Shapes.create(new AABB(0.5, CEILING - ledgeDepth, -2, 4, CEILING, 2)));
		}
		return shapes;
	}

	/** Same axis order as vanilla: Y, then X, then Z. */
	private static Vec3 collide(Vec3 movement, AABB box, List<VoxelShape> shapes) {
		double y = Shapes.collide(Direction.Axis.Y, box, shapes, movement.y);
		box = box.move(0, y, 0);
		double x = Shapes.collide(Direction.Axis.X, box, shapes, movement.x);
		box = box.move(x, 0, 0);
		double z = Shapes.collide(Direction.Axis.Z, box, shapes, movement.z);
		return new Vec3(x, y, z);
	}

	private static Vec3 walk(double ledgeDepth, boolean onGround) {
		AABB player = new AABB(-0.3, CEILING - 1.8, -0.3, 0.3, CEILING, 0.3);
		Vec3 movement = new Vec3(0.3, 0.08, 0.0);
		List<VoxelShape> shapes = world(ledgeDepth);
		return InvertedCollision.collide(movement, player, PLAYER_STEP, onGround, box -> shapes, InvertedCollisionTest::collide);
	}

	@Test
	void stepsDownOntoASlabHangingFromTheCeiling() {
		Vec3 result = walk(0.5, true);
		assertEquals(0.3, result.x, 1e-9);
		assertEquals(-0.5, result.y, 1e-9);
	}

	@Test
	void fullBlockIsTooTallToStepOnto() {
		Vec3 result = walk(1.0, true);
		assertEquals(0.2, result.x, 1e-9);
		assertEquals(0.0, result.y, 1e-9);
	}

	@Test
	void walksFreelyWithoutALedge() {
		Vec3 result = walk(0.0, true);
		assertEquals(0.3, result.x, 1e-9);
		assertEquals(0.0, result.y, 1e-9);
	}

	@Test
	void doesNotStepWhileAirborne() {
		AABB player = new AABB(-0.3, CEILING - 3.0, -0.3, 0.3, CEILING - 1.2, 0.3);
		List<VoxelShape> shapes = world(0.5);
		Vec3 result = InvertedCollision.collide(new Vec3(0.3, 0.0, 0.0), player, PLAYER_STEP, false, box -> shapes, InvertedCollisionTest::collide);
		assertEquals(0.3, result.x, 1e-9);
		assertEquals(0.0, result.y, 1e-9);
	}
}
