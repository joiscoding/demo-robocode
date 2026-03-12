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
import robocode.WinEvent;

import java.awt.Color;

import static robocode.util.Utils.normalRelativeAngle;

/**
 * ShowdownBot - a demo-focused sample robot that keeps radar lock, strafes, and leads its shots.
 */
public class ShowdownBot extends AdvancedRobot {

	private static final double WALL_MARGIN = 72;
	private static final double PREFERRED_DISTANCE = 220;
	private static final double BASE_MOVE_DISTANCE = 140;

	private double moveDirection = 1;
	private double lastEnemyEnergy = 100;
	private String targetName;

	public void run() {
		setBodyColor(new Color(28, 63, 107));
		setGunColor(new Color(245, 194, 66));
		setRadarColor(new Color(117, 217, 255));
		setBulletColor(new Color(255, 140, 66));
		setScanColor(new Color(255, 255, 255));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

		while (true) {
			if (getDistanceRemaining() == 0) {
				setAhead(BASE_MOVE_DISTANCE * moveDirection);
			}
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent event) {
		if (targetName != null && !targetName.equals(event.getName()) && event.getDistance() > PREFERRED_DISTANCE) {
			return;
		}
		targetName = event.getName();

		double absoluteBearing = getHeadingRadians() + event.getBearingRadians();
		double energyDrop = lastEnemyEnergy - event.getEnergy();
		lastEnemyEnergy = event.getEnergy();

		if (energyDrop > 0.1 && energyDrop <= 3.0) {
			moveDirection *= -1;
		}

		lockRadar(absoluteBearing);
		strafeTarget(absoluteBearing, event.getDistance());
		aimAndFire(event, absoluteBearing);
	}

	public void onHitByBullet(HitByBulletEvent event) {
		moveDirection *= -1;
		setAhead(BASE_MOVE_DISTANCE * moveDirection);
	}

	public void onHitWall(HitWallEvent event) {
		moveDirection *= -1;
		setAhead(BASE_MOVE_DISTANCE * moveDirection);
	}

	public void onHitRobot(HitRobotEvent event) {
		targetName = event.getName();
		moveDirection *= -1;
		setTurnRight(event.getBearing() + 90);
		setBack(90);
	}

	public void onRobotDeath(RobotDeathEvent event) {
		if (event.getName().equals(targetName)) {
			targetName = null;
			lastEnemyEnergy = 100;
		}
	}

	public void onWin(WinEvent event) {
		for (int i = 0; i < 12; i++) {
			turnRight(30);
			turnLeft(30);
		}
	}

	private void lockRadar(double absoluteBearing) {
		double radarTurn = normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarTurn * 2);
	}

	private void strafeTarget(double absoluteBearing, double distance) {
		double distanceFactor = Math.max(-0.45, Math.min(0.45, (distance - PREFERRED_DISTANCE) / PREFERRED_DISTANCE));
		double destinationAngle = absoluteBearing + (Math.PI / 2 - distanceFactor) * moveDirection;
		double turn = normalRelativeAngle(destinationAngle - getHeadingRadians());
		double moveDistance = BASE_MOVE_DISTANCE;

		if (Math.abs(turn) > Math.PI / 2) {
			turn = normalRelativeAngle(turn + Math.PI);
			moveDistance = -moveDistance;
		}
		if (isNearWall()) {
			moveDirection *= -1;
			moveDistance = -moveDistance;
		}

		setTurnRightRadians(turn);
		setAhead(moveDistance);
	}

	private void aimAndFire(ScannedRobotEvent event, double absoluteBearing) {
		double firePower = chooseFirePower(event.getDistance(), event.getEnergy());
		double bulletSpeed = 20 - 3 * firePower;

		double predictedX = getX() + Math.sin(absoluteBearing) * event.getDistance();
		double predictedY = getY() + Math.cos(absoluteBearing) * event.getDistance();
		double enemyHeading = event.getHeadingRadians();
		double enemyVelocity = event.getVelocity();
		double travelTime = 0;

		while ((++travelTime) * bulletSpeed < distanceTo(predictedX, predictedY)) {
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;

			if (!isInsideBattlefield(predictedX, predictedY)) {
				predictedX = clamp(predictedX, WALL_MARGIN, getBattleFieldWidth() - WALL_MARGIN);
				predictedY = clamp(predictedY, WALL_MARGIN, getBattleFieldHeight() - WALL_MARGIN);
				break;
			}
		}

		double aim = Math.atan2(predictedX - getX(), predictedY - getY());
		setTurnGunRightRadians(normalRelativeAngle(aim - getGunHeadingRadians()));

		if (getEnergy() > firePower && getGunHeat() == 0 && Math.abs(getGunTurnRemainingRadians()) < Math.toRadians(5)) {
			setFire(firePower);
		}
	}

	private double chooseFirePower(double distance, double enemyEnergy) {
		double power = Math.max(1.1, Math.min(3.0, 420 / distance));

		if (distance < 140) {
			power = 3.0;
		}
		if (getEnergy() < 20) {
			power = Math.min(power, 1.8);
		}
		return Math.min(power, enemyEnergy / 4 + 0.4);
	}

	private boolean isNearWall() {
		return getX() < WALL_MARGIN
			|| getY() < WALL_MARGIN
			|| getBattleFieldWidth() - getX() < WALL_MARGIN
			|| getBattleFieldHeight() - getY() < WALL_MARGIN;
	}

	private boolean isInsideBattlefield(double x, double y) {
		return x >= WALL_MARGIN
			&& y >= WALL_MARGIN
			&& x <= getBattleFieldWidth() - WALL_MARGIN
			&& y <= getBattleFieldHeight() - WALL_MARGIN;
	}

	private double distanceTo(double x, double y) {
		return Math.hypot(x - getX(), y - getY());
	}

	private double clamp(double value, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
