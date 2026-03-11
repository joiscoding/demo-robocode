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
 * Phantom - a sample robot that combines strafing movement with linear targeting.
 * <p>
 * Oscillates perpendicular to enemies to dodge incoming fire while leading
 * shots using the target's velocity vector.
 *
 * @author Cursor Agent (original)
 */
public class Phantom extends AdvancedRobot {

	private int moveDirection = 1;
	private int strafeCounter = 0;

	public void run() {
		setBodyColor(new Color(90, 0, 160));
		setGunColor(new Color(140, 0, 255));
		setRadarColor(new Color(200, 100, 255));
		setScanColor(new Color(200, 100, 255));
		setBulletColor(new Color(180, 50, 255));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		while (true) {
			setTurnRadarRight(360);
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double distance = e.getDistance();

		// Radar lock: narrow oscillation around the target
		double radarTurn = normalRelativeAngleDegrees(
				Math.toDegrees(absoluteBearing) - getRadarHeading());
		setTurnRadarRight(radarTurn * 1.9);

		// Linear targeting: lead the shot based on target velocity
		double bulletPower = Math.min(3.0, Math.min(getEnergy() / 5.0, 400.0 / distance));
		if (bulletPower < 0.1) {
			bulletPower = 0.1;
		}
		double bulletSpeed = 20.0 - 3.0 * bulletPower;
		double flightTime = distance / bulletSpeed;

		double targetX = getX() + Math.sin(absoluteBearing) * distance
				+ Math.sin(e.getHeadingRadians()) * e.getVelocity() * flightTime;
		double targetY = getY() + Math.cos(absoluteBearing) * distance
				+ Math.cos(e.getHeadingRadians()) * e.getVelocity() * flightTime;

		double aimAngle = Math.toDegrees(Math.atan2(targetX - getX(), targetY - getY()));
		double gunTurn = normalRelativeAngleDegrees(aimAngle - getGunHeading());
		setTurnGunRight(gunTurn);

		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
			setFire(bulletPower);
		}

		// Strafing movement: oscillate perpendicular to the enemy
		strafeCounter++;
		if (strafeCounter > 20 + (int) (Math.random() * 30)) {
			moveDirection *= -1;
			strafeCounter = 0;
		}

		double bearingDeg = e.getBearing();
		double desiredHeading;
		if (distance > 200) {
			desiredHeading = bearingDeg + 60 * moveDirection;
		} else if (distance > 100) {
			desiredHeading = bearingDeg + 80 * moveDirection;
		} else {
			desiredHeading = bearingDeg + 100 * moveDirection;
		}
		setTurnRight(normalRelativeAngleDegrees(desiredHeading));
		setAhead(150 * moveDirection);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		moveDirection *= -1;
		strafeCounter = 0;
		setTurnRight(normalRelativeAngleDegrees(90 - (getHeading() - e.getHeading())));
		setAhead(100 * moveDirection);
	}

	public void onHitWall(HitWallEvent e) {
		moveDirection *= -1;
		strafeCounter = 0;
		setAhead(150 * moveDirection);
	}

	public void onHitRobot(HitRobotEvent e) {
		double gunTurn = normalRelativeAngleDegrees(
				e.getBearing() + getHeading() - getGunHeading());
		setTurnGunRight(gunTurn);
		setFire(3);
		setBack(80);
	}
}
