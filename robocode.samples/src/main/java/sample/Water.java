/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies the distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;

import java.awt.*;

import static robocode.util.Utils.normalAbsoluteAngleDegrees;
import static robocode.util.Utils.normalRelativeAngleDegrees;


/**
 * Water — movement-heavy bot aimed at countering &quot;fire&quot; style opponents
 * (stationary gunners and rammers). Keeps a mid-range band, strafes laterally,
 * and fires when the gun lines up.
 *
 * @author Robocode sample (generated)
 */
public class Water extends Robot {

	// --- Tunable parameters (see plan: Water vs Fire robot-a472f2f4) ---
	private final double idealMinDistance = 140;
	private final double idealMaxDistance = 380;
	private final double strafeStep = 28;
	private final double gunSweepDegrees = 12;
	private final double gunAlignThreshold = 3.5;
	private final double firePowerClose = 2.4;
	private final double firePowerMid = 1.8;
	private final double firePowerFar = 1.1;
	private final double ramEscapeDistance = 55;
	private final double closeThreatDistance = 85;
	private final double hitByBulletDodge = 40;
	private final Color bodyColor = new Color(30, 144, 255);
	private final Color accentColor = new Color(0, 105, 148);
	private final Color bulletColor = new Color(173, 216, 230);

	private int strafeSign = 1;

	@Override
	public void run() {
		setBodyColor(bodyColor);
		setGunColor(accentColor);
		setRadarColor(accentColor);
		setScanColor(new Color(135, 206, 250));
		setBulletColor(bulletColor);

		setAdjustGunForRobotTurn(true);

		while (true) {
			turnGunRight(gunSweepDegrees);
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = normalAbsoluteAngleDegrees(getHeading() + e.getBearing());
		double dist = e.getDistance();

		if (dist < closeThreatDistance) {
			turnRight(normalRelativeAngleDegrees(e.getBearing() + 90));
			ahead(ramEscapeDistance);
		} else if (dist > idealMaxDistance) {
			turnRight(e.getBearing());
			ahead(Math.min(dist - idealMaxDistance + 40, 120));
		} else if (dist < idealMinDistance) {
			if (e.getBearing() > -90 && e.getBearing() <= 90) {
				back(50);
			} else {
				ahead(50);
			}
		} else {
			turnRight(normalRelativeAngleDegrees(e.getBearing() + 90 * strafeSign));
			ahead(strafeStep);
			strafeSign *= -1;
		}

		double bearingFromGun = normalRelativeAngleDegrees(enemyAbsoluteBearing - getGunHeading());

		if (Math.abs(bearingFromGun) <= gunAlignThreshold) {
			turnGunRight(bearingFromGun);
			if (getGunHeat() == 0) {
				double power = chooseFirePower(dist);
				fire(Math.min(power, getEnergy() - 0.1));
			}
		} else {
			turnGunRight(bearingFromGun);
		}

		if (Math.abs(bearingFromGun) < 0.01) {
			scan();
		}
	}

	private double chooseFirePower(double distance) {
		if (getEnergy() < 15) {
			return firePowerFar;
		}
		if (distance < 200) {
			return firePowerClose;
		}
		if (distance < 320) {
			return firePowerMid;
		}
		return firePowerFar;
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		turnRight(normalRelativeAngleDegrees(90 - (getHeading() - e.getHeading())));
		ahead(hitByBulletDodge);
		scan();
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		double turnGunAmt = normalRelativeAngleDegrees(e.getBearing() + getHeading() - getGunHeading());
		turnGunRight(turnGunAmt);
		if (getGunHeat() == 0 && getEnergy() > 0.2) {
			fire(Math.min(3, getEnergy() - 0.1));
		}
		back(45);
	}
}
