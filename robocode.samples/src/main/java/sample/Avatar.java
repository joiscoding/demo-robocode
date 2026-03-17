/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import robocode.AdvancedRobot;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;

import java.awt.Color;

import static robocode.util.Utils.normalRelativeAngle;


/**
 * Avatar — one-on-one focused {@link AdvancedRobot} with iterative circular targeting,
 * orbit movement with wall smoothing, radar slip, and energy-aware firepower.
 */
public class Avatar extends AdvancedRobot {

	private static final double MAX_TURN_RATE = Math.toRadians(10);

	private double lastAbsBearing = Double.NaN;
	private long lastScanTime = -1;
	private double lastEnemyHeading = Double.NaN;
	private long lastEnemyHeadingTime = -1;
	private int orbitDirection = 1;

	@Override
	public void run() {
		setBodyColor(new Color(0x00, 0xC8, 0xFF));
		setGunColor(new Color(0x00, 0x6E, 0xC9));
		setRadarColor(new Color(0x33, 0xFF, 0xFF));
		setBulletColor(Color.WHITE);
		setScanColor(new Color(0x66, 0xFF, 0xFF));

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);
		setAdjustRadarForGunTurn(false);

		while (true) {
			moveOrbit();
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		long now = getTime();
		double absBearing = getHeadingRadians() + e.getBearingRadians();
		lastAbsBearing = absBearing;
		lastScanTime = now;

		double turnRate = 0;
		if (!Double.isNaN(lastEnemyHeading) && lastEnemyHeadingTime >= 0 && now > lastEnemyHeadingTime) {
			double dh = normalRelativeAngle(e.getHeadingRadians() - lastEnemyHeading);
			turnRate = dh / (now - lastEnemyHeadingTime);
			turnRate = Math.max(-MAX_TURN_RATE, Math.min(MAX_TURN_RATE, turnRate));
		}
		lastEnemyHeading = e.getHeadingRadians();
		lastEnemyHeadingTime = now;

		double power = selectFirepower(e);
		double bulletSpeed = 20 - 3 * power;

		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();
		double h0 = e.getHeadingRadians();
		double v = e.getVelocity();

		double t = e.getDistance() / bulletSpeed;
		double dx = 0;
		double dy = 0;
		for (int i = 0; i < 22; i++) {
			double h = h0 + turnRate * t;
			double vx = Math.sin(h) * v;
			double vy = Math.cos(h) * v;
			double px = ex + vx * t;
			double py = ey + vy * t;
			dx = px - getX();
			dy = py - getY();
			double dist = Math.hypot(dx, dy);
			t = dist / bulletSpeed;
		}

		double aim = Math.atan2(dx, dy);
		double gunTurn = normalRelativeAngle(aim - getGunHeadingRadians());
		setTurnGunRightRadians(gunTurn);

		double radarTurn = normalRelativeAngle(absBearing - getRadarHeadingRadians());
		setTurnRadarRightRadians(1.2 * radarTurn);

		if (getGunHeat() == 0 && Math.abs(gunTurn) < Math.toRadians(6)) {
			setFire(power);
		}
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		orbitDirection *= -1;
	}

	@Override
	public void onBulletHit(BulletHitEvent e) {
		if (Math.random() < 0.28) {
			orbitDirection *= -1;
		}
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		orbitDirection *= -1;
		setBack(40);
	}

	private void moveOrbit() {
		if (Double.isNaN(lastAbsBearing) || lastScanTime < 0 || getTime() - lastScanTime > 12) {
			setTurnRight(30);
			setAhead(25);
			return;
		}

		double absBearing = lastAbsBearing;
		double stick = Math.PI / 2 * orbitDirection;
		double goal = normalRelativeAngle(absBearing + stick);

		double margin = 72;
		double w = getBattleFieldWidth();
		double h = getBattleFieldHeight();
		double x = getX();
		double y = getY();
		double cx = w / 2;
		double cy = h / 2;
		double toCenter = Math.atan2(cx - x, cy - y);

		if (x < margin) {
			goal = normalRelativeAngle(toCenter + Math.PI / 5);
		} else if (x > w - margin) {
			goal = normalRelativeAngle(toCenter - Math.PI / 5);
		}
		if (y < margin) {
			goal = normalRelativeAngle(toCenter);
		} else if (y > h - margin) {
			goal = normalRelativeAngle(toCenter);
		}

		double turn = normalRelativeAngle(goal - getHeadingRadians());
		setTurnRightRadians(turn);

		double dist = 38 + (getTime() % 7) * 2;
		setAhead(dist);
	}

	private double selectFirepower(ScannedRobotEvent e) {
		double my = getEnergy();
		if (my < 0.2) {
			return 0.1;
		}

		double d = e.getDistance();
		double en = e.getEnergy();

		double p = 2.35;
		if (d > 520) {
			p = 1.35;
		} else if (d > 360) {
			p = 1.85;
		} else if (d < 160) {
			p = 2.85;
		}

		if (en < 22) {
			p = Math.max(p, 2.65);
		}
		if (en < 10) {
			p = 3;
		}

		p = Math.min(p, my - 0.1);
		p = Math.min(p, 3);
		return Math.max(0.1, p);
	}
}
