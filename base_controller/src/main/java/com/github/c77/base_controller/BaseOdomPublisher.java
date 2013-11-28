package com.github.c77.base_controller;

import com.github.c77.base_driver.BaseDevice;
import com.github.c77.base_driver.BaseStatus;
import com.github.c77.base_driver.OdometryStatus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.concurrent.CountDownLatch;

import geometry_msgs.Point;
import geometry_msgs.PoseWithCovariance;
import geometry_msgs.Quaternion;
import geometry_msgs.Twist;
import nav_msgs.Odometry;

/**
 * @author jcerruti@willowgarage.com (Julian Cerruti)
 */
public class BaseOdomPublisher extends AbstractNodeMain {

    private CountDownLatch nodeStartLatch = new CountDownLatch(1);
    Thread basePublisherThread;
    BaseDevice baseDevice;
    private Publisher<Odometry> odometryPublisher;

    private static final Log log = LogFactory.getLog(BaseOdomPublisher.class);

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("mobile_base/odom_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        basePublisherThread = new Thread() {
            @Override
            public void run() {
                OdometryStatus odometryStatus;
                try {
                    while(true){
                        odometryStatus = baseDevice.getOdometryStatus();
                        BaseOdomPublisher.this.publish(odometryStatus);
                        Thread.sleep(10);
                    }
                } catch (Throwable t) {
                    log.error("Exception occurred during state publisher loop.", t);
                }
            }
        };

        try {
            nodeStartLatch.await();
        } catch (InterruptedException e) {
            log.info("Interrupted while waiting for ACM device");
        }

        odometryPublisher = connectedNode.newPublisher("mobile_base/odom", "nav_msgs/Odometry");

        basePublisherThread.start();
    }

    private void publish(OdometryStatus odometryStatus) {
        // Create odomentry message
        Odometry odometryMessage = odometryPublisher.newMessage();
        Point position = odometryMessage.getPose().getPose().getPosition();
        Quaternion orientation = odometryMessage.getPose().getPose().getOrientation();
        Twist twist = odometryMessage.getTwist().getTwist();
        synchronized (odometryStatus) {
            position.setX(odometryStatus.getPoseX());
            position.setY(odometryStatus.getPoseY());
            orientation.setZ(Math.sin(odometryStatus.getPoseTheta()/2.0));
            orientation.setW(Math.cos(odometryStatus.getPoseTheta()/2.0));
            // TODO: Rename to SpeedLinearX and SpeedAngularZ
            twist.getLinear().setX(odometryStatus.getSpeedLinear());
            twist.getAngular().setZ(odometryStatus.getSpeedAngular());
        }

        // TODO: Finish populating message and publish it
    }

    /**
     * Should be called to finish the node initialization. The base driver, already initialize, should
     * be provided to this method. This allows to defer the device creation to the moment Android gives
     * the application the required USB permissions.
     * @param selectedBaseDevice: the base device that wants to be used (Kobuki or create for now)
     */
    public void setBaseDevice(BaseDevice selectedBaseDevice) {
        baseDevice = selectedBaseDevice;
        nodeStartLatch.countDown();
    }
}
