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
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.Color;


/**
 * Phantom - a robot that uses wall-smoothed perpendicular strafing,
 * single-target focus, and linear targeting for consistent damage.
 *
 * @author Cursor Agent (original)
 */
public class Phantom extends AdvancedRobot {

	private static final double WALL_MARGIN = 50;
	private static final double MOVE_AMOUNT = 140;

	private int moveDirection = 1;
	private int strafeCounter = 0;
	private String targetName = "";
	private long targetScanTime = 0;

	public void run() {
		setBodyColor(new Color(90, 0, 160));
		setGunColor(new Color(140, 0, 255));
		setRadarColor(new Color(200, 100, 255));
		setScanColor(new Color(200, 100, 255));
		setBulletColor(new Color(180, 50, 255));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		targetName = "";
		targetScanTime = 0;

		while (true) {
			setTurnRadarRight(360);
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		boolean isCurrentTarget = e.getName().equals(targetName);
		boolean targetStale = getTime() - targetScanTime > 15;
		boolean acceptScan = targetName.isEmpty() || isCurrentTarget || targetStale;

		if (!acceptScan) {
			return;
		}

		boolean justSwitched = !isCurrentTarget && !targetName.isEmpty();
		targetName = e.getName();
		targetScanTime = getTime();

		double absBearing = getHeadingRadians() + e.getBearingRadians();
		double distance = e.getDistance();

		double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarTurn * 1.9);

		double bulletPower = chooseBulletPower(distance);
		double bulletSpeed = 20.0 - 3.0 * bulletPower;
		double flightTime = distance / bulletSpeed;

		double targetX = getX() + Math.sin(absBearing) * distance
				+ Math.sin(e.getHeadingRadians()) * e.getVelocity() * flightTime;
		double targetY = getY() + Math.cos(absBearing) * distance
				+ Math.cos(e.getHeadingRadians()) * e.getVelocity() * flightTime;

		targetX = clamp(targetX, 18, getBattleFieldWidth() - 18);
		targetY = clamp(targetY, 18, getBattleFieldHeight() - 18);

		double aimAngle = Math.atan2(targetX - getX(), targetY - getY());
		double gunTurn = Utils.normalRelativeAngle(aimAngle - getGunHeadingRadians());
		setTurnGunRightRadians(gunTurn);

		if (!justSwitched && getEnergy() > 0.2 && getGunHeat() == 0 && Math.abs(gunTurn) < 0.15) {
			setFire(bulletPower);
		}

		doMovement(e);
	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(targetName)) {
			targetName = "";
		}
	}

	private double chooseBulletPower(double distance) {
		double myEnergy = getEnergy();

		double power;
		if (distance < 150) {
			power = 3.0;
		} else if (distance > 350) {
			power = 1.5;
		} else {
			power = 2.0;
		}

		power = Math.min(power, myEnergy / 4.0);
		return clamp(power, 0.1, 3.0);
	}

	private void doMovement(ScannedRobotEvent e) {
		strafeCounter++;
		if (strafeCounter > 20 + (int) (Math.random() * 25)) {
			moveDirection *= -1;
			strafeCounter = 0;
		}

		double absBearing = getHeadingRadians() + e.getBearingRadians();
		double distance = e.getDistance();

		double lateralAngle;
		if (distance > 300) {
			lateralAngle = Math.PI / 3;
		} else if (distance < 120) {
			lateralAngle = 2 * Math.PI / 3;
		} else {
			lateralAngle = Math.PI / 2;
		}

		double desiredAngle = absBearing + lateralAngle * moveDirection;
		desiredAngle = wallSmooth(desiredAngle);

		double turn = Utils.normalRelativeAngle(desiredAngle - getHeadingRadians());

		if (Math.abs(turn) > Math.PI / 2) {
			setTurnRightRadians(Utils.normalRelativeAngle(turn + Math.PI));
			setAhead(-MOVE_AMOUNT);
		} else {
			setTurnRightRadians(turn);
			setAhead(MOVE_AMOUNT);
		}
	}

	private double wallSmooth(double angle) {
		double x = getX();
		double y = getY();
		double bfW = getBattleFieldWidth();
		double bfH = getBattleFieldHeight();

		if (isSafe(x, y, angle, bfW, bfH)) {
			return angle;
		}

		for (int i = 1; i <= 40; i++) {
			double step = i * 0.1;
			if (isSafe(x, y, angle + step, bfW, bfH)) {
				return angle + step;
			}
			if (isSafe(x, y, angle - step, bfW, bfH)) {
				return angle - step;
			}
		}
		return angle;
	}

	private boolean isSafe(double x, double y, double angle, double bfW, double bfH) {
		double projX = x + Math.sin(angle) * MOVE_AMOUNT;
		double projY = y + Math.cos(angle) * MOVE_AMOUNT;
		return projX >= WALL_MARGIN && projX <= bfW - WALL_MARGIN
				&& projY >= WALL_MARGIN && projY <= bfH - WALL_MARGIN;
	}

	public void onHitByBullet(HitByBulletEvent e) {
		moveDirection *= -1;
		strafeCounter = 0;
	}

	public void onHitWall(HitWallEvent e) {
		moveDirection *= -1;
		strafeCounter = 0;
	}

	public void onHitRobot(HitRobotEvent e) {
		double bearingRad = Math.toRadians(e.getBearing());
		double gunTurn = Utils.normalRelativeAngle(
				getHeadingRadians() + bearingRad - getGunHeadingRadians());
		setTurnGunRightRadians(gunTurn);
		if (getEnergy() > 1) {
			setFire(3);
		}
		setBack(100);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
