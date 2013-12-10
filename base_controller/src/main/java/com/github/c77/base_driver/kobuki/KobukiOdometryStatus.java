package com.github.c77.base_driver.kobuki;

import com.github.c77.base_driver.AbstractOdometryStatus;
import com.github.c77.base_driver.BaseStatus;
import com.github.c77.base_driver.OdometryStatus;

/**
 * @author jcerruti@willowgarage.com (Julian Cerruti)
 */
public class KobukiOdometryStatus extends AbstractOdometryStatus {
    private boolean init_l;
    private boolean init_r;

    private double last_tick_left;
    private double last_tick_right;
    private double last_rad_left;
    private double last_rad_right;
    private short last_timestamp;
    private double last_diff_time;

    private static final double WIDTH = 280.0; // in mm
    private static final double tick_to_rad = 0.002436916871363930187454;

    private double Theta;
    private double x0;
    private double y0;


    void update(BaseStatus baseStatus) {

        double left_diff_ticks;
        double right_diff_ticks;
        double curr_tick_left;
        double curr_tick_right;
        short curr_timestamp;
        curr_timestamp = baseStatus.getTimeStamp();

        if (curr_timestamp == last_timestamp) {
            return;
        }
        curr_tick_left = baseStatus.getLeftDistance();
        if (!init_l)
        {
            last_tick_left = curr_tick_left;
            init_l = true;
        }
        left_diff_ticks = (curr_tick_left - last_tick_left);
        last_tick_left = curr_tick_left;
        last_rad_left += tick_to_rad * left_diff_ticks;

        curr_tick_right = baseStatus.getRightDistance();
        if (!init_r)
        {
            last_tick_right = curr_tick_right;
            init_r = true;
        }
        right_diff_ticks = (curr_tick_right - last_tick_right);
        last_tick_right = curr_tick_right;
        last_rad_right += tick_to_rad * right_diff_ticks;

        // TODO this line and the last statements are really ugly; refactor, put in another place
        double SL = last_rad_left;
        double SR = last_rad_right;

        double S = (SR+SL)/2000.0;
        Theta = (SR-SL)/(1000.0*WIDTH) + Theta;
        x0 = S * Math.cos(Theta) + x0;
        y0 = S * Math.sin(Theta) + y0;

        if (curr_timestamp != last_timestamp)
        {
            last_diff_time = ((double)(short)((curr_timestamp - last_timestamp) & 0xffff)) / 1000.0f;
            last_timestamp = curr_timestamp;
        }

        // Update data
        synchronized (this) {
            // TODO: Calculate speed based on updates
            this.speedLinearX = x0/last_diff_time;
            this.speedAngularZ = Theta/last_diff_time;
            poseTheta = Theta;
            poseX = x0;
            poseY = y0;
        }
    }
}
