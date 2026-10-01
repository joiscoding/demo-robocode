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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Avatar - a one-on-one duelist.
 * <p>
 * Movement: orbits the enemy at a preferred distance with wall smoothing, and
 * reverses direction when the enemy fires (energy drop) or at an irregular cadence.
 * <p>
 * Radar: narrow lock with a small overscan so the target is scanned every turn.
 * <p>
 * Gun: a "virtual guns" array. A circular-prediction gun and a wave-based
 * GuessFactor gun are both evaluated on every wave; the gun with the better
 * rolling hit rating fires. GuessFactor statistics are kept in static fields
 * so they persist across rounds of a battle.
 */
public class Avatar extends AdvancedRobot {

	private static final double BOT_HALF_SIZE = 18;
	private static final double WALL_MARGIN = BOT_HALF_SIZE + 18;
	private static final double WALL_STICK = 180;
	private static final double PREFERRED_DISTANCE = 270;
	private static final double MIN_DISTANCE = 150;
	private static final double MAX_DISTANCE = 420;
	private static final double RETREAT_ANGLE = Math.PI / 6;
	private static final double APPROACH_ANGLE = Math.PI / 4;
	private static final double RADAR_OVERSCAN = Math.toRadians(10);

	private static final int GF_BINS = 31;
	private static final int MIDDLE_BIN = GF_BINS / 2;
	private static final int DISTANCE_SEGMENTS = 5;
	private static final int VELOCITY_SEGMENTS = 3;
	private static final double DISTANCE_SEGMENT_SIZE = 150;
	private static final double GUN_RATING_DECAY = 0.95;

	// Learned data persists across rounds of a battle (static fields are kept per class loader).
	private static final double[][][] GF_STATS = new double[DISTANCE_SEGMENTS][VELOCITY_SEGMENTS][GF_BINS];
	private static double circularGunRating;
	private static double guessFactorGunRating;

	private final List<Wave> waves = new ArrayList<Wave>();

	private int moveDirection = 1;
	private int lateralDirection = 1;
	private double enemyEnergy = 100;
	private double enemyHeading;
	private double enemyVelocity;
	private double enemyDistance = Double.POSITIVE_INFINITY;
	private long lastDirectionChangeTime;
	private long lastScanTime;

	/** A bullet wave used for GuessFactor learning and virtual gun scoring. */
	private static final class Wave {
		double sourceX;
		double sourceY;
		double fireBearing;
		double bulletSpeed;
		long fireTime;
		int lateralDirection;
		double[] bins;
		double circularOffset;
		double guessFactorOffset;
	}

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
		long now = getTime();
		boolean firstScan = lastScanTime == 0;
		long ticksSinceScan = Math.max(1, now - lastScanTime);
		lastScanTime = now;

		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + Math.sin(absoluteBearing) * e.getDistance();
		double enemyY = getY() + Math.cos(absoluteBearing) * e.getDistance();
		double headingChange = firstScan
				? 0
				: Utils.normalRelativeAngle(e.getHeadingRadians() - enemyHeading) / ticksSinceScan;
		double energyDrop = enemyEnergy - e.getEnergy();
		double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing);

		enemyHeading = e.getHeadingRadians();
		enemyVelocity = e.getVelocity();
		enemyDistance = e.getDistance();
		enemyEnergy = e.getEnergy();
		if (Math.abs(lateralVelocity) > 0.5) {
			lateralDirection = lateralVelocity > 0 ? 1 : -1;
		}

		updateWaves(enemyX, enemyY);
		lockRadar(absoluteBearing);
		updateMovement(absoluteBearing, energyDrop);
		aimAndFire(e, absoluteBearing, enemyX, enemyY, headingChange, lateralVelocity);
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
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double gunTurn = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians());

		setTurnGunRightRadians(gunTurn);
		if (getGunHeat() == 0 && getEnergy() > 1.7) {
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

	// ---------------------------------------------------------------- radar

	private void lockRadar(double absoluteBearing) {
		double radarTurn = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());
		double extraTurn = Math.copySign(RADAR_OVERSCAN, radarTurn == 0 ? 1 : radarTurn);

		setTurnRadarRightRadians(radarTurn + extraTurn);
	}

	// ------------------------------------------------------------- movement

	private void updateMovement(double absoluteBearing, double energyDrop) {
		if (shouldReverse(energyDrop)) {
			reverseDirection();
		}
		// Positive ratio = too far away (approach), negative = too close (retreat).
		double distanceRatio = limit((enemyDistance - PREFERRED_DISTANCE) / PREFERRED_DISTANCE, -1, 1);
		double approachAngle = distanceRatio * (distanceRatio > 0 ? APPROACH_ANGLE : RETREAT_ANGLE);
		double orbitAngle = absoluteBearing + moveDirection * ((Math.PI / 2) - approachAngle);
		double smoothedAngle = wallSmooth(getX(), getY(), orbitAngle, moveDirection);

		setMaxVelocity(Math.abs(getTurnRemaining()) > 35 ? 4 : 8 - (getTime() % 2));
		setBackAsFront(smoothedAngle);
	}

	private boolean shouldReverse(double energyDrop) {
		long turnsSinceChange = getTime() - lastDirectionChangeTime;
		// Irregular cadence so the reversal rhythm is hard to learn.
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

	// ------------------------------------------------------------------ gun

	private void aimAndFire(ScannedRobotEvent e, double absoluteBearing, double enemyX, double enemyY,
			double headingChange, double lateralVelocity) {
		double firePower = chooseFirePower(e.getDistance(), e.getEnergy());
		double bulletSpeed = Rules.getBulletSpeed(firePower);
		double maxEscapeAngle = Math.asin(Rules.MAX_VELOCITY / bulletSpeed);
		double[] bins = GF_STATS[distanceSegment(e.getDistance())][velocitySegment(lateralVelocity)];

		double[] target = predictPosition(enemyX, enemyY, enemyHeading, enemyVelocity, headingChange, bulletSpeed);
		double circularOffset = Utils.normalRelativeAngle(
				Math.atan2(target[0] - getX(), target[1] - getY()) - absoluteBearing);
		double guessFactorOffset = guessFactorOffset(bins, maxEscapeAngle, circularOffset);

		// Launch a wave every scan so both guns are scored and GF stats fill quickly.
		Wave wave = new Wave();
		wave.sourceX = getX();
		wave.sourceY = getY();
		wave.fireBearing = absoluteBearing;
		wave.bulletSpeed = bulletSpeed;
		wave.fireTime = getTime();
		wave.lateralDirection = lateralDirection;
		wave.bins = bins;
		wave.circularOffset = circularOffset;
		wave.guessFactorOffset = guessFactorOffset;
		waves.add(wave);

		double chosenOffset = guessFactorGunRating > circularGunRating ? guessFactorOffset : circularOffset;
		double gunTurn = Utils.normalRelativeAngle(absoluteBearing + chosenOffset - getGunHeadingRadians());
		double tolerance = Math.max(Math.toRadians(1.5), Math.atan(BOT_HALF_SIZE / e.getDistance()) * 0.6);

		setTurnGunRightRadians(gunTurn);
		if (getGunHeat() == 0 && Math.abs(gunTurn) < tolerance && getEnergy() > firePower) {
			setFire(firePower);
		}
	}

	private void updateWaves(double enemyX, double enemyY) {
		Iterator<Wave> iterator = waves.iterator();

		while (iterator.hasNext()) {
			Wave wave = iterator.next();
			double distanceToEnemy = Math.hypot(enemyX - wave.sourceX, enemyY - wave.sourceY);
			double traveled = (getTime() - wave.fireTime) * wave.bulletSpeed;

			if (traveled < distanceToEnemy - BOT_HALF_SIZE) {
				continue;
			}
			double actualOffset = Utils.normalRelativeAngle(
					Math.atan2(enemyX - wave.sourceX, enemyY - wave.sourceY) - wave.fireBearing);
			double maxEscapeAngle = Math.asin(Rules.MAX_VELOCITY / wave.bulletSpeed);
			double guessFactor = limit(actualOffset * wave.lateralDirection / maxEscapeAngle, -1, 1);
			int hitBin = (int) Math.round(guessFactor * MIDDLE_BIN) + MIDDLE_BIN;

			for (int i = 0; i < GF_BINS; i++) {
				wave.bins[i] = wave.bins[i] * 0.995 + 1.0 / (1 + (i - hitBin) * (i - hitBin));
			}

			// Virtual gun scoring: did each gun's predicted angle fall on the robot?
			double hitTolerance = Math.atan(BOT_HALF_SIZE / distanceToEnemy);

			circularGunRating = circularGunRating * GUN_RATING_DECAY
					+ (Math.abs(actualOffset - wave.circularOffset) < hitTolerance ? 1 : 0);
			guessFactorGunRating = guessFactorGunRating * GUN_RATING_DECAY
					+ (Math.abs(actualOffset - wave.guessFactorOffset) < hitTolerance ? 1 : 0);
			iterator.remove();
		}
	}

	private double guessFactorOffset(double[] bins, double maxEscapeAngle, double fallbackOffset) {
		int bestBin = MIDDLE_BIN;
		double total = 0;

		for (int i = 0; i < GF_BINS; i++) {
			total += bins[i];
			if (bins[i] > bins[bestBin]) {
				bestBin = i;
			}
		}
		if (total < 1) {
			return fallbackOffset;
		}
		double guessFactor = (double) (bestBin - MIDDLE_BIN) / MIDDLE_BIN;

		return lateralDirection * guessFactor * maxEscapeAngle;
	}

	private int distanceSegment(double distance) {
		return (int) Math.min(DISTANCE_SEGMENTS - 1, distance / DISTANCE_SEGMENT_SIZE);
	}

	private int velocitySegment(double lateralVelocity) {
		double speed = Math.abs(lateralVelocity);

		return speed < 2 ? 0 : (speed < 5 ? 1 : 2);
	}

	private double[] predictPosition(double enemyX, double enemyY, double enemyHeadingRadians, double velocity,
			double headingChange, double bulletSpeed) {
		double predictedX = enemyX;
		double predictedY = enemyY;
		double predictedHeading = enemyHeadingRadians;
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

	// ---------------------------------------------------------------- utils

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
