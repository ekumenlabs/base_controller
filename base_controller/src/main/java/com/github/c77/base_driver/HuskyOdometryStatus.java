package com.github.c77.base_driver;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author jcerruti@creativa77.com (Julian Cerruti)
 */
public class HuskyOdometryStatus implements OdometryStatus {
    private int poseX;
    private int poseY;
    private double poseTheta;
    private double speedLinear;
    private double speedAngular;

    private int lastLeftTravel;
    private int lastRightTravel;
    private boolean haveLastTravel = false;

    // TODO: Allow setting (and load from ROS param in node)
    private static final double WIDTH = 0.55;

    public void update(byte[] encoderData) {
        if(encoderData.length != 13) {
            throw new RuntimeException("wrong size encoder data = " + encoderData.length);
        }

        // --------------------------------
        // Parse buffer
        // --------------------------------
        ByteBuffer buffer = ByteBuffer.wrap(encoderData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Number of encoders
        // TODO: Verify it's two
        byte nEncoders = buffer.get();
        // Left encoder travel
        int leftTravel = buffer.getInt(1);
        // Right encoder travel
        int rightTravel = buffer.getInt(5);
        // Left encoder speed
        char leftSpeed = buffer.getChar(9);
        // Right encoder speed
        char rightSpeed = buffer.getChar(11);

        // Special case: first time ever we can't calculate differences
        if(!haveLastTravel) {
            lastLeftTravel = leftTravel;
            lastRightTravel = rightTravel;
            haveLastTravel = true;
            return;
        }

        // Calculate deltas
        double dr = ((leftTravel - lastLeftTravel) + (rightTravel - lastRightTravel))/2.0;
        double da = ((rightTravel - lastRightTravel) - (leftTravel - lastLeftTravel))/WIDTH;
        lastLeftTravel = leftTravel;
        lastRightTravel = rightTravel;

        // Update data
        synchronized (this) {
            speedLinear = (leftSpeed + rightSpeed) / 2.0;
            speedAngular = (rightSpeed - leftSpeed) / WIDTH;
            poseX += dr * Math.cos(poseTheta);
            poseY += dr * Math.sin(poseTheta);
            poseTheta += da;
        }
    }

    @Override
    public int getPoseX() {
        return poseX;
    }

    @Override
    public int getPoseY() {
        return poseY;
    }

    @Override
    public double getPoseTheta() {
        return poseTheta;
    }

    @Override
    public double getSpeedLinear() {
        return speedLinear;
    }

    @Override
    public double getSpeedAngular() {
        return speedAngular;
    }
}
