package com.piotrek.pwwindnsails.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * One look-ray target: the cleat wire box or the fence post column actually under the crosshair.
 * Connecting rails and neighboring posts/cleats are not targets.
 */
public final class MooringAim {
	public static final double REACH = 7.0;
	/** Matches the black wire drawn around a cleat. */
	public static final double CLEAT_HALF_X = 0.08;
	public static final double CLEAT_HALF_Z = 0.20;
	public static final double CLEAT_MIN_Y = 0.625;
	public static final double CLEAT_MAX_Y = 0.76;
	/** Vanilla fence post is pixels 6–10. Rails to the next post sit outside this column. */
	public static final double POST_MIN = 0.375;
	public static final double POST_MAX = 0.625;
	private static final double CLICK_PAD = 0.04;

	private MooringAim() {}

	public record Target(boolean cleat, int boatId, int cleatIndex, BlockPos fence, double distance) {}

	public static Target find(Player player, Level level, float partialTick) {
		if (!com.piotrek.pwwindnsails.world.ChunkReady.is(level, player.blockPosition())) {
			return null;
		}
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 look = player.getViewVector(partialTick);
		if (look.lengthSqr() < 1.0E-8) {
			return null;
		}
		look = look.normalize();
		Vec3 end = eye.add(look.scale(REACH));

		BlockHitResult blockHit = level.clip(new ClipContext(
			eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		double occlude = REACH;
		BlockPos occludePos = null;
		if (blockHit.getType() == HitResult.Type.BLOCK) {
			occlude = eye.distanceTo(blockHit.getLocation());
			occludePos = blockHit.getBlockPos();
		}

		Target best = null;
		AABB boats = player.getBoundingBox().inflate(REACH + 3.0);
		for (SailboatEntity boat : level.getEntitiesOfClass(SailboatEntity.class, boats)) {
			Vec3 boatPos = boat.getPosition(partialTick);
			float yaw = Mth.rotLerp(partialTick, boat.yRotO, boat.getYRot());
			for (int i = 0; i < SailboatEntity.CLEAT_COUNT; i++) {
				double dist = cleatDistance(eye, look, boatPos, yaw, boat.getCleatLocalOffset(i), CLICK_PAD);
				if (dist > REACH || dist > occlude + 0.05) {
					continue;
				}
				if (best == null || dist < best.distance) {
					best = new Target(true, boat.getId(), i, BlockPos.ZERO, dist);
				}
			}
		}

		int minX = Mth.floor(Math.min(eye.x, end.x));
		int minY = Mth.floor(Math.min(eye.y, end.y));
		int minZ = Mth.floor(Math.min(eye.z, end.z));
		int maxX = Mth.floor(Math.max(eye.x, end.x));
		int maxY = Mth.floor(Math.max(eye.y, end.y));
		int maxZ = Mth.floor(Math.max(eye.z, end.z));
		for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			if (!level.getBlockState(pos).is(BlockTags.FENCES)) {
				continue;
			}
			BlockPos fixed = pos.immutable();
			double dist = boxDistance(eye, end, postColumn(fixed));
			if (!accept(dist, occlude, occludePos, fixed)) {
				continue;
			}
			if (best == null || dist < best.distance) {
				best = new Target(false, -1, -1, fixed, dist);
			}
		}

		AABB knotArea = new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
		for (LeashFenceKnotEntity knot : level.getEntitiesOfClass(LeashFenceKnotEntity.class, knotArea)) {
			BlockPos pos = knot.getPos();
			if (pos == null || !level.getBlockState(pos).is(BlockTags.FENCES)) {
				continue;
			}
			double dist = boxDistance(eye, end, knot.getBoundingBox().inflate(CLICK_PAD));
			if (!accept(dist, occlude, occludePos, pos)) {
				continue;
			}
			if (best == null || dist < best.distance) {
				best = new Target(false, -1, -1, pos, dist);
			}
		}
		return best;
	}

	/**
	 * Accepts only the reported cleat or post when the look ray actually enters that box.
	 * Never snaps to a neighbor.
	 */
	public static Target confirm(Player player, boolean cleat, int boatId, int cleatIndex, BlockPos fence) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		if (look.lengthSqr() < 1.0E-8) {
			return null;
		}
		look = look.normalize();
		if (cleat) {
			if (!(player.level().getEntity(boatId) instanceof SailboatEntity boat)
				|| cleatIndex < 0
				|| cleatIndex >= SailboatEntity.CLEAT_COUNT
				|| player.distanceTo(boat) > REACH + 4.0) {
				return null;
			}
			double dist = cleatDistance(eye, look, boat.position(), boat.getYRot(), boat.getCleatLocalOffset(cleatIndex), 0.12);
			return dist <= REACH ? new Target(true, boat.getId(), cleatIndex, BlockPos.ZERO, dist) : null;
		}
		if (fence == null || !player.level().getBlockState(fence).is(BlockTags.FENCES)) {
			return null;
		}
		if (player.distanceToSqr(Vec3.atCenterOf(fence)) > (REACH + 2.0) * (REACH + 2.0)) {
			return null;
		}
		Vec3 end = eye.add(look.scale(REACH));
		double dist = boxDistance(eye, end, postColumn(fence).inflate(0.08));
		if (dist > REACH) {
			Optional<LeashFenceKnotEntity> knot = LeashFenceKnotEntity.getKnot(player.level(), fence);
			if (knot.isPresent()) {
				dist = boxDistance(eye, end, knot.get().getBoundingBox().inflate(0.08));
			}
		}
		return dist <= REACH ? new Target(false, -1, -1, fence, dist) : null;
	}

	public static AABB postColumn(BlockPos pos) {
		return new AABB(
			pos.getX() + POST_MIN - CLICK_PAD,
			pos.getY(),
			pos.getZ() + POST_MIN - CLICK_PAD,
			pos.getX() + POST_MAX + CLICK_PAD,
			pos.getY() + 1.0,
			pos.getZ() + POST_MAX + CLICK_PAD
		);
	}

	public static AABB cleatLocalBox(Vec3 localOffset, double pad) {
		return new AABB(
			localOffset.x - CLEAT_HALF_X - pad,
			CLEAT_MIN_Y - pad,
			localOffset.z - CLEAT_HALF_Z - pad,
			localOffset.x + CLEAT_HALF_X + pad,
			CLEAT_MAX_Y + pad,
			localOffset.z + CLEAT_HALF_Z + pad
		);
	}

	public static double cleatDistance(Vec3 eye, Vec3 look, Vec3 boatPos, float yaw, Vec3 localOffset, double pad) {
		float yawRad = yaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		Vec3 end = eye.add(look.scale(REACH));
		return boxDistance(
			toLocal(eye, boatPos, fwdX, fwdZ, rightX, rightZ),
			toLocal(end, boatPos, fwdX, fwdZ, rightX, rightZ),
			cleatLocalBox(localOffset, pad)
		);
	}

	public static double boxDistance(Vec3 from, Vec3 to, AABB box) {
		Optional<Vec3> hit = box.clip(from, to);
		return hit.map(from::distanceTo).orElse(Double.MAX_VALUE);
	}

	private static boolean accept(double dist, double occlude, BlockPos occludePos, BlockPos pos) {
		if (dist > REACH) {
			return false;
		}
		return dist <= occlude + 0.2 || (occludePos != null && occludePos.equals(pos));
	}

	private static Vec3 toLocal(Vec3 world, Vec3 origin, double fwdX, double fwdZ, double rightX, double rightZ) {
		double dx = world.x - origin.x;
		double dy = world.y - origin.y;
		double dz = world.z - origin.z;
		return new Vec3(dx * rightX + dz * rightZ, dy, dx * fwdX + dz * fwdZ);
	}
}
