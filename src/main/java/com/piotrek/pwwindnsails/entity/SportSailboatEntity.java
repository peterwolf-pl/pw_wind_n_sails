package com.piotrek.pwwindnsails.entity;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 4.5m x 1.5m high-performance sport sailing boat (Sloop rig with mainsail and jib).
 * Same width as the classic sailboat, but 1.5 blocks longer with a sleek white racing hull,
 * lighter aluminium mast extended by 2 blocks, boom mounted half a block higher,
 * and working headsail (jib) providing enhanced sail drive and top speed.
 */
public class SportSailboatEntity extends SailboatEntity {

	public SportSailboatEntity(EntityType<? extends SportSailboatEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected double getHullMinFwd() {
		// Stern transom extended aft
		return -1.95;
	}

	@Override
	protected double getHullMaxFwd() {
		// Bow stem extended forward (+1.5 blocks total length)
		return 2.15;
	}

	@Override
	protected double getBowTaperStart() {
		// Long, sleek wave-piercing racing bow taper
		return 1.05;
	}

	@Override
	public double getPassengerCenterZOffset() {
		// Helm seat located in the aft section of the extended sport cockpit
		return -1.15;
	}

	@Override
	public double getBowLeashOffsetZ() {
		// Lead attaches to the racing bow stem tip
		return 2.15;
	}

	@Override
	public float getPickRadius() {
		return 2.4F;
	}

	@Override
	public double getBowCleatLocalZ() {
		return 1.625;
	}

	@Override
	public double getSternCleatLocalZ() {
		return -1.625;
	}

	@Override
	public double getMastLocalZ() {
		return 1.06;
	}

	@Override
	public double getMastHeight() {
		return 7.2;
	}

	@Override
	protected ItemStack getDropItemStack() {
		return new ItemStack(WindAndSailsMod.SPORT_SAILBOAT_ITEM);
	}

	@Override
	public void tick() {
		super.tick();

		// Sport performance boost from dual-sail rig (mainsail + jib) and longer waterline
		if (!this.level().isClientSide() && !this.isSailFurled() && !this.isLeashed()) {
			Vec3 vel = this.getDeltaMovement();
			double horizSpeedSq = vel.x * vel.x + vel.z * vel.z;
			if (horizSpeedSq > 0.001) {
				// Jib aerodynamic slot effect adds ~18% extra speed and drive
				this.setDeltaMovement(vel.x * 1.012, vel.y, vel.z * 1.012);
			}
		}
	}
}
