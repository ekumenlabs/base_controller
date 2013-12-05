package com.github.c77.base_driver.kobuki;

import com.github.c77.base_driver.AbstractOdometryStatus;
import com.github.c77.base_driver.BaseStatus;
import com.github.c77.base_driver.OdometryStatus;

/**
 * @author jcerruti@willowgarage.com (Julian Cerruti)
 */
public class KobukiOdometryStatus extends AbstractOdometryStatus {
    private double lastLeftTravel;
    private double lastRightTravel;
    private boolean haveLastTravel = false;

    private static final double WIDTH = 230.0; // in mm

    void update(BaseStatus baseStatus) {
        double leftTravel = baseStatus.getLeftDistance();
        double rightTravel = baseStatus.getRightDistance();

        // Special case: first time ever we can't calculate differences
        if(!haveLastTravel) {
            lastLeftTravel = leftTravel;
            lastRightTravel = rightTravel;
            haveLastTravel = true;
            return;
        } else if(lastLeftTravel == leftTravel && lastRightTravel == rightTravel) {
            // Another special case: no change since last time
            // (useful for the particular implementation of package parsing for the Kobuki)
            return;
        }

        // Calculate deltas
        double dr = ((leftTravel - lastLeftTravel) + (rightTravel - lastRightTravel))/2000.0;
        double da = ((rightTravel - lastRightTravel) - (leftTravel - lastLeftTravel))/(1000.0*WIDTH);
        lastLeftTravel = leftTravel;
        lastRightTravel = rightTravel;

        // Update data
        synchronized (this) {
            // TODO: Calculate speed based on updates
            /* JAC: temporarily hardcoding speed
            this.speedLinearX = (leftSpeed + rightSpeed) / 2000.0;
            speedAngularZ = (rightSpeed - leftSpeed) / (1000.0*WIDTH); */
            this.speedLinearX = 1.0;
            this.speedAngularZ = 0.0;
            poseX += dr * Math.cos(poseTheta);
            poseY += dr * Math.sin(poseTheta);
            poseTheta += da;
        }
    }
}
