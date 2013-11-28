package com.github.c77.base_driver;

/**
 * @author jcerruti@willowgarage.com (Julian Cerruti)
 */
public interface OdometryStatus {
    int getPoseX();

    int getPoseY();

    double getPoseTheta();

    double getSpeedLinear();

    double getSpeedAngular();
}
