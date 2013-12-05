package com.github.c77.base_driver;

/**
 * @author jcerruti@willowgarage.com (Julian Cerruti)
 */
public class AbstractOdometryStatus implements OdometryStatus {
    protected double poseX;
    protected double poseY;
    protected double poseTheta;
    protected double speedLinearX;
    protected double speedAngularZ;

    @Override
    public double getPoseX() {
        return poseX;
    }

    @Override
    public double getPoseY() { return poseY; }

    @Override
    public double getPoseTheta() {
        return poseTheta;
    }

    @Override
    public double getSpeedLinearX() {
        return speedLinearX;
    }

    @Override
    public double getSpeedAngularZ() {
        return speedAngularZ;
    }
}
