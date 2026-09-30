package io.github.kenthejr.infinitycastle.gravity;

import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mirror image of vanilla {@code Entity.collide} for entities whose gravity points up.
 *
 * <p>Vanilla lets a grounded entity step up onto a ledge of at most {@code maxUpStep}. When standing on a ceiling the
 * equivalent ledge hangs <em>below</em> the feet, so the step is taken downward and measured from the top of the
 * bounding box instead of the bottom.
 */
public final class InvertedCollision {
	@FunctionalInterface
	public interface ShapeCollider {
		Vec3 collide(Vec3 movement, AABB box, List<VoxelShape> shapes);
	}

	private InvertedCollision() {
	}

	public static Vec3 collide(
		Vec3 movement,
		AABB box,
		float maxStep,
		boolean onGround,
		Function<AABB, List<VoxelShape>> collidersIn,
		ShapeCollider collider
	) {
		Vec3 resolved = movement.lengthSqr() == 0.0 ? movement : collider.collide(movement, box, collidersIn.apply(box.expandTowards(movement)));
		boolean xCollision = movement.x != resolved.x;
		boolean yCollision = movement.y != resolved.y;
		boolean zCollision = movement.z != resolved.z;
		boolean landedAbove = yCollision && movement.y > 0.0;
		if (maxStep <= 0.0F || !(landedAbove || onGround) || !(xCollision || zCollision)) {
			return resolved;
		}

		AABB grounded = landedAbove ? box.move(0.0, resolved.y, 0.0) : box;
		AABB stepBox = grounded.expandTowards(movement.x, -maxStep, movement.z);
		if (!landedAbove) {
			stepBox = stepBox.expandTowards(0.0, 1.0E-5F, 0.0);
		}
		List<VoxelShape> colliders = collidersIn.apply(stepBox);
		float alreadyMoved = (float) -resolved.y;

		for (float step : candidateSteps(grounded, colliders, maxStep, alreadyMoved)) {
			Vec3 stepped = collider.collide(new Vec3(movement.x, -step, movement.z), grounded, colliders);
			if (stepped.horizontalDistanceSqr() > resolved.horizontalDistanceSqr()) {
				return stepped.add(0.0, grounded.minY - box.minY, 0.0);
			}
		}
		return resolved;
	}

	/** Distances below the top of {@code box} at which a collider has a horizontal face, nearest first. */
	static float[] candidateSteps(AABB box, List<VoxelShape> colliders, float maxStep, float skip) {
		FloatSet candidates = new FloatArraySet(4);
		for (VoxelShape shape : colliders) {
			for (double coord : shape.getCoords(Direction.Axis.Y)) {
				float depth = (float) (box.maxY - coord);
				if (depth >= 0.0F && depth <= maxStep && depth != skip) {
					candidates.add(depth);
				}
			}
		}
		float[] sorted = candidates.toFloatArray();
		FloatArrays.unstableSort(sorted);
		return sorted;
	}
}
