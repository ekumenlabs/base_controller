/*
 * Copyright (C) 2013 Creativa77.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.c77.base_controller;

import com.github.c77.base_driver.BaseDevice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import java.util.concurrent.CountDownLatch;

import geometry_msgs.Twist;

/**
 * @author jcerruti@creativa77.com.ar (Julian Cerruti)
 */

public class BaseControllerNode extends AbstractNodeMain implements MessageListener<Twist> {
    //  Latch used to synchronize start node start with
    //  base driver reference being provided
    private CountDownLatch nodeStartLatch = new CountDownLatch(1);

    private BaseDevice baseDevice;
    private double linearVelX = 0.0;
    private double angVelZ = 0.0;
    private String CMD_VEL_TOPIC;

    private static final Log log = LogFactory.getLog(BaseControllerNode.class);
    Thread moveBaseThread;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android/move_base");
    }

    /**
     * Creates a new base controller node that listens twists
     * in a given topic. Usually this will be cmd_vel.
     * @param vel_topic: The topic in which to listen for twists.
     */
    public BaseControllerNode(String vel_topic) {
        CMD_VEL_TOPIC = vel_topic;
    }

    /**
     * Should be called to finish the node initialization. The base driver, already initialize should
     * be provided to this method. This allows to defer the device creation to the moment Android gives
     * the application the required USB permissions.
     * @param selectedBaseDevice: the base device that wants to be used (Kobuki or create for now)
     */
    public void setBaseDevice(BaseDevice selectedBaseDevice) {
        baseDevice = selectedBaseDevice;
        nodeStartLatch.countDown();
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        log.info("Base controller starting. Will wait for ACM device");

        moveBaseThread = new Thread() {
            @Override
            public void run() {
                // thread to constantly send commands to the base
                try {
                    while(true){
                        baseDevice.move(linearVelX, angVelZ);
                        Thread.sleep(250);
                    }
                } catch (Throwable t) {
                    log.error("Exception occurred during move loop", t);
                    // Whenever we get interrupted out of the loop, for any reason
                    // we try to stop the base, just in case
                    try {
                        setTwistValues(0.0, 0.0);
                        baseDevice.move(0.0, 0.0);
                    } catch(Throwable t0) {
                    }
                }
            }
        };

        // Wait here until the base driver is provided via setBaseDevice.
        try {
            nodeStartLatch.await();
        } catch (InterruptedException e) {
            log.info("Interrupted while waiting for ACM device");
        }

        // Start base_controller subscriber
        Subscriber<Twist> vel_listener = connectedNode.newSubscriber(CMD_VEL_TOPIC, Twist._TYPE);
        vel_listener.addMessageListener(this);

        // Initialize base.
        baseDevice.initialize();
        moveBaseThread.start();

        log.info("Base controller initialized.");
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
    }

    @Override
    public void onShutdownComplete(Node node) {
        super.onShutdownComplete(node);
    }

    private synchronized void setTwistValues(double linearVelX, double angVelZ) {
        this.linearVelX = linearVelX;
        this.angVelZ = angVelZ;
        log.info("synchronized setting: " + this.linearVelX);
    }

    @Override
    public void onNewMessage(Twist twist) {
        log.info("Current Twist msg: " + twist);
        setTwistValues(twist.getLinear().getX(), twist.getAngular().getZ());
    }
}
