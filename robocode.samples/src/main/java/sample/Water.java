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

import java.awt.Color;

import static robocode.util.Utils.normalRelativeAngleDegrees;


/**
 * Water - a sample robot tuned for mobile strafing and steady targeting.
 * <p>
 * Keeps a medium distance from opponents while orbiting and firing with
 * distance-based power selection.
 */
public class Water extends AdvancedRobot {

	private double preferredDistance = 180;
	private double cruiseDistance = 160;
	private double turnRate = 22;
	private double radarOverscanFactor = 2;
	private double closeFirePower = 3.0;
	private double mediumFirePower = 2.6;
	private double longFirePower = 2.0;
	private double aimTolerance = 2.2;
	private double evasiveTurnOffset = 90;
	private double bulletEvasionDistance = 140;
	private double wallEscapeDistance = 180;
	private double robotEscapeDistance = 120;

	private int moveDirection = 1;

	/**
	 * Main robot loop with continuous movement and scanning.
	 */
	public void run() {
		setBodyColor(new Color(40, 110, 200));
		setGunColor(new Color(30, 70, 130));
		setRadarColor(new Color(120, 210, 255));
		setScanColor(new Color(120, 210, 255));
		setBulletColor(new Color(20, 120, 255));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		while (true) {
			setTurnRadarRight(360);
			setTurnRight(turnRate * moveDirection);
			setAhead(cruiseDistance * moveDirection);
			execute();
		}
	}

	/**
	 * Locks radar/gun on scanned target and keeps lateral movement.
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		double absoluteBearing = getHeading() + e.getBearing();
		double gunTurn = normalRelativeAngleDegrees(absoluteBearing - getGunHeading());
		double radarTurn = normalRelativeAngleDegrees(absoluteBearing - getRadarHeading());

		setTurnRadarRight(radarTurn * radarOverscanFactor);
		setTurnGunRight(gunTurn);

		double distanceError = e.getDistance() - preferredDistance;
		double orbitTurn = e.getBearing() + (distanceError >= 0 ? 82 : 98) * moveDirection;

		setTurnRight(normalRelativeAngleDegrees(orbitTurn));
		if (Math.abs(distanceError) > 25) {
			double correctionDistance = Math.min(220, Math.abs(distanceError) + 80);
			setAhead(correctionDistance * (distanceError > 0 ? 1 : -1));
		} else {
			setAhead(cruiseDistance * moveDirection);
		}

		if (Math.abs(gunTurn) <= aimTolerance && getGunHeat() == 0) {
			setFire(resolveFirePower(e.getDistance()));
		}

		if (e.getDistance() < 120) {
			moveDirection = -moveDirection;
		}
	}

	/**
	 * Perpendicular dodge when hit by bullets.
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		moveDirection = -moveDirection;
		setTurnRight(normalRelativeAngleDegrees(e.getBearing() + evasiveTurnOffset));
		setAhead(moveDirection * bulletEvasionDistance);
	}

	/**
	 * Reverse from walls and re-enter open space.
	 */
	public void onHitWall(HitWallEvent e) {
		moveDirection = -moveDirection;
		setBack(wallEscapeDistance);
	}

	/**
	 * Break collisions and fire at close range.
	 */
	public void onHitRobot(HitRobotEvent e) {
		moveDirection = -moveDirection;
		setTurnRight(normalRelativeAngleDegrees(e.getBearing() + evasiveTurnOffset));
		setBack(robotEscapeDistance);
		if (getGunHeat() == 0) {
			fire(2.8);
		}
	}

	private double resolveFirePower(double distance) {
		double firePower;

		if (distance < 120) {
			firePower = closeFirePower;
		} else if (distance < 280) {
			firePower = mediumFirePower;
		} else {
			firePower = longFirePower;
		}

		if (getEnergy() < 20) {
			firePower = Math.min(firePower, 1.8);
		}
		if (getEnergy() < 10) {
			firePower = Math.min(firePower, 1.2);
		}
		return Math.max(0.1, Math.min(3, firePower));
	}
}
