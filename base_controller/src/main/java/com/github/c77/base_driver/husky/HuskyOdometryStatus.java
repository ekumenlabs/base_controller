package com.github.c77.base_driver.husky;

import com.github.c77.base_driver.AbstractOdometryStatus;
import com.github.c77.base_driver.OdometryStatus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author jcerruti@creativa77.com (Julian Cerruti)
 */
public class HuskyOdometryStatus extends AbstractOdometryStatus {

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
        short leftSpeed = buffer.getShort(9);
        // Right encoder speed
        short rightSpeed = buffer.getShort(11);

        // Special case: first time ever we can't calculate differences
        if(!haveLastTravel) {
            lastLeftTravel = leftTravel;
            lastRightTravel = rightTravel;
            haveLastTravel = true;
            return;
        }

        // Calculate deltas
        double dr = ((leftTravel - lastLeftTravel) + (rightTravel - lastRightTravel))/2000.0;
        double da = ((rightTravel - lastRightTravel) - (leftTravel - lastLeftTravel))/(1000.0*WIDTH);
        lastLeftTravel = leftTravel;
        lastRightTravel = rightTravel;

        // Update data
        synchronized (this) {
            this.speedLinearX = (leftSpeed + rightSpeed) / 2000.0;
            speedAngularZ = (rightSpeed - leftSpeed) / (1000.0*WIDTH);
            poseX += dr * Math.cos(poseTheta);
            poseY += dr * Math.sin(poseTheta);
            poseTheta += da;
        }
    }
}
