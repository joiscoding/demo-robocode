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
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.Color;


/**
 * Crazy - a sample robot that demonstrates movement patterns.
 * <p>
 * This tuned version keeps the original erratic spirit, but adds basic radar
 * locking, lead targeting, adaptive fire power, and evasive strafing.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class Crazy extends AdvancedRobot {
	private static final double BASE_MOVE_DISTANCE = 160;
	private static final double CLOSE_MOVE_DISTANCE = 110;
	private static final double WALL_ESCAPE_DISTANCE = 120;
	private static final double ROBOT_ESCAPE_DISTANCE = 150;
	private static final double WALL_MARGIN = 72;
	private static final double CLOSE_RANGE = 140;
	private static final double MID_RANGE = 260;
	private static final double PERPENDICULAR_ANGLE = Math.PI / 2;
	private static final double RADAR_LOCK_SCALE = 2;
	private static final double MAX_VELOCITY = 8;
	private static final double CORNERING_VELOCITY = 5;
	private static final double FIRE_ALIGNMENT_THRESHOLD = Math.toRadians(12);
	private static final long DIRECTION_CHANGE_COOLDOWN = 18;

	private int moveDirection = 1;
	private long lastScanTime = -1;
	private long lastDirectionChangeTime = Long.MIN_VALUE;

	/**
	 * Main robot logic that sets colors, keeps the radar sweeping, and lets the
	 * event handlers drive the combat movement.
	 */
	public void run() {
		setBodyColor(new Color(0, 200, 0));
		setGunColor(new Color(0, 150, 50));
		setRadarColor(new Color(0, 100, 100));
		setBulletColor(new Color(255, 255, 100));
		setScanColor(new Color(255, 200, 200));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setMaxVelocity(MAX_VELOCITY);
		setAhead(BASE_MOVE_DISTANCE);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		while (true) {
			if (getTime() - lastScanTime > 4 && getRadarTurnRemainingRadians() == 0) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			if (getDistanceRemaining() == 0) {
				setAhead(BASE_MOVE_DISTANCE * moveDirection);
			}
			execute();
		}
	}

	/**
	 * Tracks enemies with the radar, uses simple linear lead targeting, and
	 * moves perpendicular to the target to stay harder to hit.
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		lastScanTime = getTime();

		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double firePower = getFirePower(e.getDistance(), e.getEnergy());
		double bulletSpeed = Rules.getBulletSpeed(firePower);
		double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing);
		double lead = Math.asin(limit(-1, lateralVelocity / bulletSpeed, 1));
		double gunTurn = Utils.normalRelativeAngle(absoluteBearing + lead - getGunHeadingRadians());
		double radarTurn = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
		double strafeOffset = e.getDistance() < CLOSE_RANGE ? Math.toRadians(20) : Math.toRadians(10);
		double bodyTurn = Utils.normalRelativeAngle(
				absoluteBearing + moveDirection * (PERPENDICULAR_ANGLE - strafeOffset) - getHeadingRadians());
		double moveDistance = e.getDistance() < CLOSE_RANGE ? CLOSE_MOVE_DISTANCE : BASE_MOVE_DISTANCE;

		if (shouldReverse(e) || isNearWall()) {
			reverseDirection();
			bodyTurn = Utils.normalRelativeAngle(
					absoluteBearing + moveDirection * (PERPENDICULAR_ANGLE - strafeOffset) - getHeadingRadians());
		}

		setTurnRadarRightRadians(radarTurn * RADAR_LOCK_SCALE);
		setTurnGunRightRadians(gunTurn);
		setTurnRightRadians(bodyTurn);
		setMaxVelocity(Math.abs(getTurnRemaining()) > 30 ? CORNERING_VELOCITY : MAX_VELOCITY);
		setAhead(moveDistance * moveDirection);

		if (Math.abs(gunTurn) < FIRE_ALIGNMENT_THRESHOLD && getGunHeat() == 0) {
			setFire(firePower);
		}
	}

	/**
	 * Dodge perpendicular to incoming fire and break linear movement.
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		reverseDirection();
		setTurnRightRadians(Utils.normalRelativeAngle(e.getBearingRadians() + moveDirection * PERPENDICULAR_ANGLE));
		setAhead(BASE_MOVE_DISTANCE * moveDirection);
	}

	/**
	 * Bounce away from walls quickly to reduce corner trapping.
	 */
	public void onHitWall(HitWallEvent e) {
		reverseDirection();
		setTurnRight(-e.getBearing());
		setAhead(WALL_ESCAPE_DISTANCE * moveDirection);
	}

	/**
	 * Punish close collisions, but prioritize escaping from rammers.
	 */
	public void onHitRobot(HitRobotEvent e) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double gunTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians());

		setTurnGunRightRadians(gunTurn);
		if (Math.abs(gunTurn) < Math.toRadians(15) && getGunHeat() == 0) {
			setFire(Math.min(3, Math.max(1.8, getEnergy() / 12)));
		}
		reverseDirection();
		setAhead(ROBOT_ESCAPE_DISTANCE * moveDirection);
	}

	/**
	 * Resume a broad radar sweep when the current target dies.
	 */
	public void onRobotDeath(RobotDeathEvent e) {
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

	private boolean shouldReverse(ScannedRobotEvent e) {
		if (getTime() - lastDirectionChangeTime < DIRECTION_CHANGE_COOLDOWN) {
			return false;
		}
		return e.getDistance() < CLOSE_RANGE
				|| (e.getDistance() < MID_RANGE && Math.abs(e.getBearing()) < 20)
				|| (e.getDistance() < MID_RANGE && getTime() % 24 == 0);
	}

	private boolean isNearWall() {
		return getX() < WALL_MARGIN
				|| getY() < WALL_MARGIN
				|| getX() > getBattleFieldWidth() - WALL_MARGIN
				|| getY() > getBattleFieldHeight() - WALL_MARGIN;
	}

	private void reverseDirection() {
		moveDirection = -moveDirection;
		lastDirectionChangeTime = getTime();
	}

	private double getFirePower(double distance, double enemyEnergy) {
		double firePower;

		if (distance < CLOSE_RANGE) {
			firePower = 2.8;
		} else if (distance < MID_RANGE) {
			firePower = 2.1;
		} else {
			firePower = 1.4;
		}
		if (getEnergy() < 18) {
			firePower = Math.min(firePower, 1.6);
		}
		if (enemyEnergy < 4) {
			firePower = Math.min(firePower, enemyEnergy / 2 + 0.4);
		}
		return limit(Rules.MIN_BULLET_POWER, firePower, Rules.MAX_BULLET_POWER);
	}

	private double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}
}
