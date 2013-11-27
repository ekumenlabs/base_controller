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

package com.github.c77.base_driver;

/**
 * Created by Sebastian Garcia Marra on 05/08/13.
 */

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HuskyBaseDevice implements BaseDevice {
    long initialTime = System.currentTimeMillis();

    // Husky low level commands
    private final byte SOH = (byte) 0xAA;
    private final byte ProtocolVersion = (byte) 0x1;
    private final byte STX = (byte) 0x55;

    private static final Log log = LogFactory.getLog(HuskyBaseDevice.class);
    private static UsbSerialDriver serialDriver = null;

    public BaseStatus getBaseStatus() {
        BaseStatus baseStatus;
        baseStatus = new BaseStatus();
        return baseStatus;
    }

    // All BaseDevice classes have the same signature. Husky doesn't need velocity
    //conversion since is able to receive direct linear and angular velocities.
    private class BaseSpeedValues {
        private final int linearSpeed;
        private final int angularSpeed;

        private static final double LINEAR_SPEED_LIMIT = 100.0;
        private static final double ANGULAR_SPEED_LIMIT = 100.0;

        private BaseSpeedValues(double linearSpeed, double angularSpeed) {
            this.linearSpeed = (int)Math.round(Math.max(Math.min(linearSpeed * 100.0, LINEAR_SPEED_LIMIT), -1.0*LINEAR_SPEED_LIMIT));
            this.angularSpeed = (int)Math.round(Math.max(Math.min(angularSpeed * 100.0, ANGULAR_SPEED_LIMIT), -1.0*ANGULAR_SPEED_LIMIT));
        }

        public int getLinearSpeed() {
            return linearSpeed;
        }

        public int getAngSpeed() {
            return angularSpeed;
        }
    }

    public HuskyBaseDevice(UsbSerialDriver driver) {
        serialDriver = driver;
        try {
            serialDriver.open();
            serialDriver.setParameters(115200, UsbSerialDriver.DATABITS_8,
                    UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            log.info("Serial device opened correctly");
        } catch (IOException e) {
            log.info("Error setting up device: " + e.getMessage(), e);
            e.printStackTrace();
            try {
                serialDriver.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            serialDriver = null;
        }

        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        SerialInputOutputManager serialInputOutputManager;

        final SerialInputOutputManager.Listener listener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                }

                @Override
                public void onNewData(final byte[] data) {
                    HuskyBaseDevice.this.updateReceivedData(data);
                }
            };

        serialInputOutputManager = new SerialInputOutputManager(serialDriver, listener);
        executorService.submit(serialInputOutputManager);
    }

    private void updateReceivedData(final byte[] bytes) {
        int readBytes = bytes.length;
        //log.info("-- IN -->" + HuskyBaseUtils.byteArrayToString(bytes));
    }

    public void initialize() {
        log.info("Initializing");
        // write(buildPackage(new byte[]{ 0x03, 0x40 }));
    }

    public void move(double linearVelX, double angVelZ) {
        //log.info("trying to move (" + linearVelX + "," + angVelZ + ")");
        BaseSpeedValues speeds = twistToBase(linearVelX, angVelZ);
        sendMovementPackage(speeds);
    }

    // All BaseDevice classes have the same signature. Husky doesn't need velocity
    //conversion since is able to receive direct linear and angular velocities.
    private BaseSpeedValues twistToBase(double linearVelX, double angVelZ) {
        return new BaseSpeedValues(linearVelX, angVelZ);
    }

    private void sendMovementPackage(BaseSpeedValues speeds) {
        int linearSpeed = speeds.getLinearSpeed();
        int angSpeed = speeds.getAngSpeed();
        int linearAccel = 0x00C8;         // Fixed acceleration of 5[m/s²]
        int MSGType = 0x0204;             // Set velocities using kinematic model

        //Little-endian encoding
        byte[] baseControlMsg = new byte[]{
            (byte) MSGType,
            (byte) (MSGType >> 8),
            STX,
            (byte) linearSpeed,
            (byte) (linearSpeed >> 8),
            (byte) angSpeed,
            (byte) (angSpeed >> 8),
            (byte) linearAccel,
            (byte) (linearAccel >> 8)
        };

        write(buildPackage(baseControlMsg));
    }


    byte[] buildPackage(byte[] payload) {
        char checksum = 0;
        int payloadLength = payload.length;
        byte[] pkg = new byte[payloadLength + 11];
        long msgTime = System.currentTimeMillis();

        byte Flags = (byte) 0x01;                        // ACK suppressed
        byte Length0 = (byte) (payloadLength + 8);
        byte Length1 = (byte) ~Length0;                  // It always is Length0's complement
        int TimeStamp = (int)msgTime - (int)initialTime; // Set TimeStamp in milliseconds using four bytes

        pkg[0] = SOH;
        pkg[1] = Length0;
        pkg[2] = Length1;
        pkg[3] = ProtocolVersion;
        pkg[4] = (byte) TimeStamp;
        pkg[5] = (byte) (TimeStamp >> 8);
        pkg[6] = (byte) (TimeStamp >> 16);
        pkg[7] = (byte) (TimeStamp >> 24);
        pkg[8] = Flags;

        for (int i = 9; i < payloadLength + 9; i++) {
            pkg[i] = payload[i - 9];
        }

        checksum = HuskyBaseUtils.checkSum(pkg);
        pkg[pkg.length - 2] = (byte)checksum;
        pkg[pkg.length - 1] = (byte)(checksum >> 8);

        return pkg;
    }

    private void write(byte[] command) {
        try {
            //log.info("Writing a command to USB Device: " + HuskyBaseUtils.byteArrayToString(command));
            serialDriver.write(command, 1000);
        } catch(Throwable t) {
            log.error("Exception writing command: " + HuskyBaseUtils.byteArrayToString(command), t);
        }
    }
}
