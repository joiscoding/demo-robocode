/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;

import static robocode.util.Utils.normalRelativeAngleDegrees;

/**
 * NotCrazyBot — a more deliberate fighter than {@link Crazy}: locks radar, aims the gun,
 * varies bullet power by range, and uses short strafing segments instead of a fixed zigzag.
 * <p>
 * Tunable fields below are the main levers for battle tuning.
 */
public class NotCrazyBot extends AdvancedRobot {

	/** Pixels per movement segment (larger = harder to hit, but more wall collisions). */
	private static final double MOVE_SEGMENT = 168;
	/** Small body turn each tick for unpredictable path vs linear aimers. */
	private static final double BODY_DRIFT = 22;
	/** Fire power when target is far ({@code > FAR_RANGE}). */
	private static final double FIRE_POWER_FAR = 1.45;
	/** Fire power at medium range. */
	private static final double FIRE_POWER_MID = 2.15;
	/** Fire power when target is close. */
	private static final double FIRE_POWER_NEAR = 2.85;
	private static final double FAR_RANGE = 520;
	private static final double NEAR_RANGE = 220;
	/** Maximum |gun error| (degrees) before firing. */
	private static final double GUN_AIM_TOLERANCE = 4.0;

	private int moveDir = 1;

	@Override
	public void run() {
		setBodyColor(new Color(0, 140, 70));
		setGunColor(new Color(0, 120, 90));
		setRadarColor(new Color(0, 90, 120));
		setBulletColor(new Color(255, 240, 120));
		setScanColor(new Color(200, 255, 200));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);

		while (true) {
			setTurnRadarRight(360);
			setAhead(MOVE_SEGMENT * moveDir);
			setTurnRight(BODY_DRIFT * (moveDir > 0 ? 1 : -1));
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		double absBearing = getHeading() + e.getBearing();
		double gunTurn = normalRelativeAngleDegrees(absBearing - getGunHeading());
		setTurnGunRight(gunTurn);
		setTurnRadarLeft(normalRelativeAngleDegrees(absBearing - getRadarHeading()));

		double dist = e.getDistance();
		double power;
		if (dist > FAR_RANGE) {
			power = FIRE_POWER_FAR;
		} else if (dist < NEAR_RANGE) {
			power = FIRE_POWER_NEAR;
		} else {
			power = FIRE_POWER_MID;
		}
		power = Math.min(power, getEnergy() - 0.15);
		power = Math.max(0.1, Math.min(3.0, power));

		if (getGunHeat() == 0 && Math.abs(gunTurn) < GUN_AIM_TOLERANCE && power >= 0.1) {
			setFire(power);
		}
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		moveDir = -moveDir;
		setAhead(MOVE_SEGMENT * moveDir);
		setTurnLeft(55 + e.getBearing());
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		moveDir = -moveDir;
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		if (e.isMyFault()) {
			moveDir = -moveDir;
		}
		double absBearing = getHeading() + e.getBearing();
		double gunTurn = normalRelativeAngleDegrees(absBearing - getGunHeading());
		setTurnGunRight(gunTurn);
		if (getGunHeat() == 0 && Math.abs(gunTurn) < 8) {
			setFire(Math.min(2.5, getEnergy() - 0.1));
		}
	}
}
