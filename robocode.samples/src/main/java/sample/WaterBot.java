/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package sample;


import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

import java.awt.Color;

import static robocode.util.Utils.normalRelativeAngleDegrees;


/**
 * WaterBot - a sample robot that demonstrates fluid movement and predictive targeting.
 * <p>
 * Moves in a flowing, wave-like pattern to be hard to hit, while using linear aim
 * projection to predict enemy positions. Designed as a thematic counter to Fire.
 *
 * <ul>
 *   <li>Oscillates laterally like water to avoid incoming bullets</li>
 *   <li>Uses linear extrapolation to lead the target with each shot</li>
 *   <li>Adjusts bullet power by distance and own energy level</li>
 *   <li>Reverses direction on wall contact or bullet hits to keep flowing</li>
 * </ul>
 *
 * @author Robocode contributors
 */
public class WaterBot extends Robot {

    /** Lateral movement direction: +1 = right relative to heading, -1 = left. */
    private int moveDirection = 1;

    /** How far to strafe sideways each wave cycle (pixels). */
    private double strafeDistance = 120;

    /** How many ticks before reversing strafe direction. */
    private int strafeTimer = 0;

    /** Maximum strafe ticks before direction flip. */
    private static final int STRAFE_TICKS = 20;

    /** Bullet speed constant used for linear aim prediction (pixels per tick per power). */
    private static final double BULLET_SPEED_BASE = 20.0;

    /** Distance threshold for switching to high bullet power. */
    private static final double CLOSE_RANGE = 150.0;

    /** Distance threshold for medium bullet power. */
    private static final double MID_RANGE = 300.0;

    /** Energy reserve; don't fire powerful shots when energy drops below this. */
    private static final double LOW_ENERGY = 20.0;

    /**
     * Main robot loop: sets water-themed colors and enters the flowing movement pattern.
     */
    public void run() {
        setBodyColor(new Color(0, 100, 200));
        setGunColor(Color.CYAN);
        setRadarColor(Color.WHITE);
        setBulletColor(Color.CYAN);
        setScanColor(new Color(100, 200, 255));

        while (true) {
            // Keep scanning while strafing sideways
            turnGunRight(360);
        }
    }

    /**
     * Handles robot detection. Performs linear aim projection and fires, then
     * strafes perpendicular to maintain fluid motion.
     *
     * @param e the scanned robot event
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        double distance = e.getDistance();
        double bulletPower = chooseBulletPower(distance);

        // Linear aim: estimate where the enemy will be when the bullet arrives
        double bulletSpeed = BULLET_SPEED_BASE - 3.0 * bulletPower;
        long travelTicks = (long) (distance / bulletSpeed);

        // Predicted absolute bearing to enemy's future position
        double absoluteBearing = getHeading() + e.getBearing();
        double predictedBearing = absoluteBearing + Math.toDegrees(e.getVelocity() * travelTicks
                * Math.sin(Math.toRadians(e.getHeading() - absoluteBearing)) / distance);

        double gunTurn = normalRelativeAngleDegrees(predictedBearing - getGunHeading());
        turnGunRight(gunTurn);

        if (getGunHeat() == 0) {
            fire(bulletPower);
        }

        // Strafe laterally so we're not a stationary target
        strafe();

        scan();
    }

    /**
     * Reverses strafe direction when hit by a bullet.
     *
     * @param e the hit-by-bullet event
     */
    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection *= -1;
        strafeTimer = 0;
        ahead(strafeDistance * moveDirection);
    }

    /**
     * Fires hard at a colliding robot and pushes through if energy allows.
     *
     * @param e the hit-robot event
     */
    public void onHitRobot(HitRobotEvent e) {
        double gunTurn = normalRelativeAngleDegrees(e.getBearing() + getHeading() - getGunHeading());
        turnGunRight(gunTurn);
        fire(3);
    }

    /**
     * Reverses direction on wall contact to keep flowing.
     *
     * @param e the hit-wall event
     */
    public void onHitWall(HitWallEvent e) {
        moveDirection *= -1;
        strafeTimer = 0;
        ahead(strafeDistance * moveDirection);
    }

    /**
     * Performs a victory spin on winning.
     *
     * @param e the win event
     */
    public void onWin(WinEvent e) {
        turnRight(36000);
    }

    /**
     * Moves sideways in a wave-like pattern, reversing periodically.
     */
    private void strafe() {
        strafeTimer++;
        if (strafeTimer >= STRAFE_TICKS) {
            moveDirection *= -1;
            strafeTimer = 0;
        }
        turnRight(90 * moveDirection);
        ahead(strafeDistance);
        turnLeft(90 * moveDirection);
    }

    /**
     * Selects bullet power based on distance and current energy level.
     *
     * @param distance pixels to target
     * @return bullet power in the range [1, 3]
     */
    private double chooseBulletPower(double distance) {
        if (getEnergy() < LOW_ENERGY) {
            return 1;
        }
        if (distance < CLOSE_RANGE) {
            return 3;
        }
        if (distance < MID_RANGE) {
            return 2;
        }
        return 1;
    }
}
