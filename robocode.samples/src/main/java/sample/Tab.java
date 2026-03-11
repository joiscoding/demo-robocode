/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import robocode.JuniorRobot;


/**
 * Tab - a sample robot based on MyFirstJuniorRobot with extracted variables for easy tweaking.
 * <p>
 * Moves in a seesaw motion and spins the gun around at each end
 * when it cannot see any enemy robot. When the robot sees an enemy
 * robot, it will immediately turn the gun and fire at it.
 */
public class Tab extends JuniorRobot {

	private int moveDistance = 100;
	private int gunSweepAngle = 360;
	private int firePower = 1;
	private int hitByBulletTurn = 100;
	private int hitByBulletAngle = 90;
	private int bodyColor = red;
	private int gunColor = black;
	private int radarColor = blue;

	/**
	 * The main run method - Implements a seesaw movement pattern
	 */
	public void run() {
		// Set robot colors (body, gun, radar)
		setColors(bodyColor, gunColor, radarColor);

		// Seesaw movement pattern - repeats forever
		while (true) {
			ahead(moveDistance);
			turnGunRight(gunSweepAngle);
			back(moveDistance);
			turnGunRight(gunSweepAngle);
		}
	}

	/**
	 * Called when our robot's radar detects another robot
	 * Responds by aiming the gun at the detected robot and firing
	 */
	public void onScannedRobot() {
		turnGunTo(scannedAngle);
		fire(firePower);
	}

	/**
	 * Called when our robot is hit by a bullet
	 * Responds by moving perpendicular to the bullet's path
	 * to potentially avoid future shots from the same direction
	 */
	public void onHitByBullet() {
		turnAheadLeft(hitByBulletTurn, hitByBulletAngle - hitByBulletBearing);
	}
}
