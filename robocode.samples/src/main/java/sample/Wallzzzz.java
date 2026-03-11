/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.Color;


/**
 * Wallzzzz - An anti-wall-crawler duelist tuned to beat Walls.
 * <p>
 * Uses predictive targeting, radar lock, and lateral movement to punish robots
 * that move in long straight segments near the battlefield borders.
 *
 * @author Mathew A. Nelson (original Walls)
 * @author Flemming N. Larsen (contributor)
 */
public class Wallzzzz extends AdvancedRobot {

	private static final double ROBOT_HALF_SIZE = 18;
	private static final double DESIRED_DISTANCE = 220;
	private static final double WALL_DANGER_MARGIN = 60;
	private static final double MOVE_AMOUNT = 140;
	private static final double MAX_APPROACH_ANGLE = Math.PI / 6;

	private int moveDirection = 1;
	private double lastEnemyEnergy = 100;

	@Override
	public void run() {
		setBodyColor(Color.black);
		setGunColor(Color.black);
		setRadarColor(Color.orange);
		setBulletColor(Color.cyan);
		setScanColor(Color.cyan);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);

		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		while (true) {
			if (getDistanceRemaining() == 0) {
				setAhead(MOVE_AMOUNT * moveDirection);
			}
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + Math.sin(absoluteBearing) * e.getDistance();
		double enemyY = getY() + Math.cos(absoluteBearing) * e.getDistance();

		double energyDrop = lastEnemyEnergy - e.getEnergy();
		if (energyDrop > 0 && energyDrop <= 3) {
			moveDirection = -moveDirection;
		}
		lastEnemyEnergy = e.getEnergy();

		if (isNearWall()) {
			moveDirection = -moveDirection;
		}

		double radarTurn = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarTurn * 2);

		double distanceRatio = limit((e.getDistance() - DESIRED_DISTANCE) / DESIRED_DISTANCE, -1, 1);
		double approachAngle = distanceRatio * MAX_APPROACH_ANGLE;
		double goAngle = absoluteBearing + moveDirection * ((Math.PI / 2) - approachAngle);

		setBackAsFront(goAngle);

		double firePower = chooseFirePower(e.getDistance(), e.getEnergy());
		double[] targetPoint = predictPosition(enemyX, enemyY, e.getHeadingRadians(), e.getVelocity(), firePower);
		double gunTurn = Utils.normalRelativeAngle(
				Math.atan2(targetPoint[0] - getX(), targetPoint[1] - getY()) - getGunHeadingRadians());

		setTurnGunRightRadians(gunTurn);

		if (getGunHeat() == 0 && Math.abs(gunTurn) < Math.toRadians(5)) {
			setFire(firePower);
		}
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
		if (getGunHeat() == 0) {
			setFire(Math.min(3, Math.max(0.1, getEnergy() - 0.1)));
		}
		moveDirection = -moveDirection;
		setBack(120);
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		moveDirection = -moveDirection;
		setBack(MOVE_AMOUNT);
	}

	private double[] predictPosition(double enemyX, double enemyY, double enemyHeading, double enemyVelocity,
			double firePower) {
		double bulletSpeed = Rules.getBulletSpeed(firePower);
		double predictedX = enemyX;
		double predictedY = enemyY;
		double distance = Math.hypot(predictedX - getX(), predictedY - getY());
		int ticks = 0;

		while ((++ticks) * bulletSpeed < distance) {
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			predictedX = limit(predictedX, ROBOT_HALF_SIZE, getBattleFieldWidth() - ROBOT_HALF_SIZE);
			predictedY = limit(predictedY, ROBOT_HALF_SIZE, getBattleFieldHeight() - ROBOT_HALF_SIZE);
			distance = Math.hypot(predictedX - getX(), predictedY - getY());
		}
		return new double[] { predictedX, predictedY };
	}

	private double chooseFirePower(double distance, double enemyEnergy) {
		double firePower;

		if (distance < 150) {
			firePower = 3;
		} else if (distance < 300) {
			firePower = 2.4;
		} else {
			firePower = 1.8;
		}

		firePower = Math.min(firePower, enemyEnergy / 4 + 0.5);
		firePower = Math.min(firePower, getEnergy() - 0.1);

		return Math.max(0.1, firePower);
	}

	private boolean isNearWall() {
		return getX() < WALL_DANGER_MARGIN
				|| getY() < WALL_DANGER_MARGIN
				|| getX() > getBattleFieldWidth() - WALL_DANGER_MARGIN
				|| getY() > getBattleFieldHeight() - WALL_DANGER_MARGIN;
	}

	private void setBackAsFront(double goAngle) {
		double angle = Utils.normalRelativeAngle(goAngle - getHeadingRadians());

		if (Math.abs(angle) > Math.PI / 2) {
			if (angle < 0) {
				setTurnRightRadians(Math.PI + angle);
			} else {
				setTurnLeftRadians(Math.PI - angle);
			}
			setBack(MOVE_AMOUNT);
		} else {
			if (angle < 0) {
				setTurnLeftRadians(-angle);
			} else {
				setTurnRightRadians(angle);
			}
			setAhead(MOVE_AMOUNT);
		}
	}

	private double limit(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
