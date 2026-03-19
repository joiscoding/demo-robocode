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
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

import java.awt.Color;

/**
 * Duelist — tuned for one-on-one: linear targeting, radar lock, perpendicular strafing,
 * simple bullet-dodge heuristics, and energy-aware shot power.
 */
public class Duelist extends AdvancedRobot {

	private static final double WALL_PAD = 80;
	private static final double STRAFE_DISTANCE = 175;

	private int strafeSign = 1;
	private double lastEnemyEnergy = 100;

	public void run() {
		setBodyColor(new Color(0x1a, 0x3d, 0x2e));
		setGunColor(new Color(0xc9, 0xa2, 0x27));
		setRadarColor(new Color(0x88, 0xcc, 0xff));
		setBulletColor(new Color(0xff, 0xee, 0x88));
		setScanColor(new Color(0x66, 0xaa, 0xff));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		while (true) {
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double absBearing = getHeadingRadians() + e.getBearingRadians();

		double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarTurn + Math.copySign(0.05, radarTurn == 0 ? 1 : radarTurn));

		double bulletPower = selectBulletPower(e);
		double bulletSpeed = 20 - 3 * bulletPower;

		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();
		double eh = e.getHeadingRadians();
		double v = e.getVelocity();
		double evx = Math.sin(eh) * v;
		double evy = Math.cos(eh) * v;

		double predX = ex;
		double predY = ey;
		for (int i = 0; i < 4; i++) {
			double dx = predX - getX();
			double dy = predY - getY();
			double dist = Math.hypot(dx, dy);
			double flight = dist / bulletSpeed;
			predX = ex + evx * flight;
			predY = ey + evy * flight;
		}

		double aimDx = predX - getX();
		double aimDy = predY - getY();
		double aimAngle = Math.atan2(aimDx, aimDy);
		setTurnGunRightRadians(Utils.normalRelativeAngle(aimAngle - getGunHeadingRadians()));

		double aimErr = Math.abs(Utils.normalRelativeAngle(aimAngle - getGunHeadingRadians()));
		if (getGunHeat() == 0 && aimErr < 0.12 && getEnergy() > bulletPower) {
			setFire(bulletPower);
		}

		if (lastEnemyEnergy - e.getEnergy() > 0.09 && lastEnemyEnergy - e.getEnergy() < 3.2) {
			strafeSign *= -1;
		}
		lastEnemyEnergy = e.getEnergy();

		if (tooCloseToWall()) {
			strafeSign *= -1;
		}

		double moveAngle = Utils.normalRelativeAngle(absBearing + (Math.PI / 2) * strafeSign - getHeadingRadians());
		setTurnRightRadians(moveAngle);
		setAhead(STRAFE_DISTANCE * strafeSign);
		setMaxVelocity(nearWall() ? 6 : 8);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		strafeSign *= -1;
	}

	public void onHitWall(HitWallEvent e) {
		strafeSign *= -1;
	}

	public void onWin(WinEvent e) {
		setMaxVelocity(8);
		setTurnRight(36000);
	}

	private double selectBulletPower(ScannedRobotEvent e) {
		double dist = e.getDistance();
		if (getEnergy() < 12) {
			return Math.max(0.1, Math.min(1.0, getEnergy() - 0.2));
		}
		if (e.getEnergy() < 5) {
			return Math.max(0.1, Math.min(3.0, getEnergy() - 0.1));
		}
		if (dist > 500) {
			return 1.8;
		}
		if (dist > 320) {
			return 2.0;
		}
		return 2.35;
	}

	private boolean nearWall() {
		double w = getBattleFieldWidth();
		double h = getBattleFieldHeight();
		return getX() < WALL_PAD * 1.4 || getX() > w - WALL_PAD * 1.4
				|| getY() < WALL_PAD * 1.4 || getY() > h - WALL_PAD * 1.4;
	}

	private boolean tooCloseToWall() {
		double w = getBattleFieldWidth();
		double h = getBattleFieldHeight();
		return getX() < WALL_PAD || getX() > w - WALL_PAD
				|| getY() < WALL_PAD || getY() > h - WALL_PAD;
	}
}
