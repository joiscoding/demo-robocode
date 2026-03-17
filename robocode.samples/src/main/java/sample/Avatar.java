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
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

import java.awt.Color;

/**
 * Avatar - a one-on-one duelist with evasive movement, radar lock, and predictive fire.
 */
public class Avatar extends AdvancedRobot {

	private static final double BOT_HALF_SIZE = 18;
	private static final double WALL_MARGIN = BOT_HALF_SIZE + 18;
	private static final double WALL_STICK = 180;
	private static final double PREFERRED_DISTANCE = 270;
	private static final double MIN_DISTANCE = 150;
	private static final double MAX_DISTANCE = 420;
	private static final double MAX_APPROACH_ANGLE = Math.PI / 6;
	private static final double RADAR_OVERSCAN = Math.toRadians(10);
	private static final double GUN_TOLERANCE = Math.toRadians(4);

	private int moveDirection = 1;
	private double enemyEnergy = 100;
	private double enemyHeading;
	private double enemyVelocity;
	private double enemyDistance = Double.POSITIVE_INFINITY;
	private long lastDirectionChangeTime;
	private long lastScanTime;

	@Override
	public void run() {
		setBodyColor(new Color(44, 56, 92));
		setGunColor(new Color(110, 172, 218));
		setRadarColor(new Color(148, 210, 189));
		setBulletColor(new Color(255, 190, 92));
		setScanColor(new Color(255, 255, 255));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		while (true) {
			if (getTime() - lastScanTime > 4) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			if (getDistanceRemaining() == 0) {
				setAhead(moveDirection * WALL_STICK);
			}
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		lastScanTime = getTime();

		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double headingChange = Utils.normalRelativeAngle(e.getHeadingRadians() - enemyHeading);
		double enemyX = getX() + Math.sin(absoluteBearing) * e.getDistance();
		double enemyY = getY() + Math.cos(absoluteBearing) * e.getDistance();
		double energyDrop = enemyEnergy - e.getEnergy();

		enemyHeading = e.getHeadingRadians();
		enemyVelocity = e.getVelocity();
		enemyDistance = e.getDistance();
		enemyEnergy = e.getEnergy();

		lockRadar(absoluteBearing);
		updateMovement(absoluteBearing, energyDrop);

		double firePower = chooseFirePower(e.getDistance(), e.getEnergy());
		double[] targetPoint = predictPosition(enemyX, enemyY, enemyHeading, enemyVelocity, headingChange, firePower);
		double gunTurn = Utils.normalRelativeAngle(
				Math.atan2(targetPoint[0] - getX(), targetPoint[1] - getY()) - getGunHeadingRadians());

		setTurnGunRightRadians(gunTurn);
		if (getGunHeat() == 0 && Math.abs(gunTurn) < GUN_TOLERANCE && getEnergy() > firePower) {
			setFire(firePower);
		}
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		if (getTime() - lastDirectionChangeTime > 8) {
			reverseDirection();
		}
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		reverseDirection();
		setAhead(moveDirection * WALL_STICK);
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double gunTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians());

		setTurnGunRightRadians(gunTurn);
		if (getGunHeat() == 0) {
			setFire(Math.min(3, Math.max(1.6, getEnergy() - 0.1)));
		}
		reverseDirection();
	}

	@Override
	public void onWin(WinEvent e) {
		for (int i = 0; i < 16; i++) {
			turnRight(22.5);
			turnLeft(22.5);
		}
	}

	private void lockRadar(double absoluteBearing) {
		double radarTurn = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
		double extraTurn = Math.copySign(RADAR_OVERSCAN, radarTurn == 0 ? 1 : radarTurn);

		setTurnRadarRightRadians(radarTurn + extraTurn);
	}

	private void updateMovement(double absoluteBearing, double energyDrop) {
		if (shouldReverse(energyDrop)) {
			reverseDirection();
		}
		double distanceRatio = limit((enemyDistance - PREFERRED_DISTANCE) / PREFERRED_DISTANCE, -1, 1);
		double approachAngle = distanceRatio * MAX_APPROACH_ANGLE;
		double orbitAngle = absoluteBearing + moveDirection * ((Math.PI / 2) - approachAngle);
		double smoothedAngle = wallSmooth(getX(), getY(), orbitAngle, moveDirection);

		setMaxVelocity(Math.abs(getTurnRemaining()) > 35 ? 4 : 8 - (getTime() % 2));
		setBackAsFront(smoothedAngle);
	}

	private boolean shouldReverse(double energyDrop) {
		long turnsSinceChange = getTime() - lastDirectionChangeTime;
		long cadence = 18 + Math.min(24L, (long) (enemyDistance / 14)) + ((getTime() / 11) % 9);

		if (energyDrop > 0.09 && energyDrop <= 3 && turnsSinceChange > 6) {
			return true;
		}
		if (enemyDistance < MIN_DISTANCE && turnsSinceChange > 4) {
			return true;
		}
		if (enemyDistance > MAX_DISTANCE && turnsSinceChange > 10) {
			return true;
		}
		return turnsSinceChange > cadence && enemyDistance < 280;
	}

	private void reverseDirection() {
		moveDirection = -moveDirection;
		lastDirectionChangeTime = getTime();
		setAhead(moveDirection * WALL_STICK);
	}

	private double wallSmooth(double x, double y, double angle, int orientation) {
		double smoothedAngle = angle;
		int attempts = 0;

		while (!isInsideField(projectX(x, smoothedAngle, WALL_STICK), projectY(y, smoothedAngle, WALL_STICK))
				&& attempts++ < 60) {
			smoothedAngle += orientation * 0.05;
		}
		return smoothedAngle;
	}

	private boolean isInsideField(double x, double y) {
		return x > WALL_MARGIN
				&& y > WALL_MARGIN
				&& x < getBattleFieldWidth() - WALL_MARGIN
				&& y < getBattleFieldHeight() - WALL_MARGIN;
	}

	private double[] predictPosition(double enemyX, double enemyY, double enemyHeadingRadians, double velocity,
			double headingChange, double firePower) {
		double predictedX = enemyX;
		double predictedY = enemyY;
		double predictedHeading = enemyHeadingRadians;
		double bulletSpeed = Rules.getBulletSpeed(firePower);
		int ticks = 0;

		while ((++ticks) * bulletSpeed < Math.hypot(predictedX - getX(), predictedY - getY())) {
			predictedX += Math.sin(predictedHeading) * velocity;
			predictedY += Math.cos(predictedHeading) * velocity;
			predictedHeading += headingChange;

			if (!isInsideField(predictedX, predictedY)) {
				predictedX = limit(predictedX, WALL_MARGIN, getBattleFieldWidth() - WALL_MARGIN);
				predictedY = limit(predictedY, WALL_MARGIN, getBattleFieldHeight() - WALL_MARGIN);
				break;
			}
		}
		return new double[] { predictedX, predictedY };
	}

	private void setBackAsFront(double goAngle) {
		double angle = Utils.normalRelativeAngle(goAngle - getHeadingRadians());

		if (Math.abs(angle) > Math.PI / 2) {
			if (angle < 0) {
				setTurnRightRadians(Math.PI + angle);
			} else {
				setTurnLeftRadians(Math.PI - angle);
			}
			setBack(WALL_STICK);
		} else {
			if (angle < 0) {
				setTurnLeftRadians(-angle);
			} else {
				setTurnRightRadians(angle);
			}
			setAhead(WALL_STICK);
		}
	}

	private double chooseFirePower(double distance, double targetEnergy) {
		double firePower = 3.1 - distance / 320;

		if (distance > 450) {
			firePower = 1.5;
		}
		if (distance < 220) {
			firePower = Math.max(firePower, 2.6);
		}
		if (Math.abs(enemyVelocity) < 1.2) {
			firePower = Math.max(firePower, 2.75);
		}
		if (getEnergy() < 18) {
			firePower = Math.min(firePower, 2.1);
		}
		if (getEnergy() < 9) {
			firePower = Math.min(firePower, 1.2);
		}

		firePower = Math.min(firePower, targetEnergy / 4 + 0.5);
		firePower = Math.min(firePower, getEnergy() - 0.1);

		return limit(firePower, Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);
	}

	private double projectX(double x, double angle, double length) {
		return x + Math.sin(angle) * length;
	}

	private double projectY(double y, double angle, double length) {
		return y + Math.cos(angle) * length;
	}

	private double limit(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
