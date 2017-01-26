package com.github.c77.base_driver;

/**
 * @author jcerruti@willowgarage.com (Julian Cerruti)
 */
public interface OdometryStatus {
    double getPoseX();

    double getPoseY();

    double getPoseTheta();

    double getSpeedLinearX();

    double getSpeedAngularZ();
}
