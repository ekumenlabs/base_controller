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

package com.ekumen.base_driver.create;

/**
 * Created by Sebastian Garcia Marra on 22/07/13.
 */

import android.hardware.usb.UsbDeviceConnection;

import com.ekumen.base_driver.AbstractBaseDevice;
import com.ekumen.base_driver.BaseStatus;
import com.ekumen.base_driver.OdometryStatus;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

public class CreateBaseDevice extends AbstractBaseDevice {

    private Double createBaseDiameter = 0.33; //In meters
    private Double createWheelRadius = 0.04;  //In meters

    private final byte SetBaudrate57600 = (byte) 5;
    // iRobot Create low level commands.
    private final byte Start = (byte) 128;
    private final byte FullMode = (byte) 132;
    private final byte DirectDrive = (byte) 145;

    private class BaseSpeedValues {
        private final int rightWheelVel;
        private final int leftWheelVel;

        private BaseSpeedValues(int rightWheelVel, int leftWheelVel) {
            this.rightWheelVel = rightWheelVel;
            this.leftWheelVel = leftWheelVel;
        }

        public int getLeftWheelVel() {
            return leftWheelVel;
        }

        public int getRightWheelVel() {
            return rightWheelVel;
        }
    }

    private static final Log log = LogFactory.getLog(CreateBaseDevice.class);

    public CreateBaseDevice(UsbSerialPort usbSerialPort, UsbDeviceConnection usbDeviceConnection) throws Exception {
        super(usbSerialPort, usbDeviceConnection);
    }

    public void initialize() {
        byte[] baud = new byte[] {SetBaudrate57600};
        write(baud); // configure base baudrate
        byte[] setup = new byte[] {Start, FullMode};
        write(setup); // configure create base.
    }

    public void move(double linearVelX, double angVelZ) {
        BaseSpeedValues speeds = twistToBase(linearVelX, angVelZ);
        sendMovementPackage(speeds);
    }

    private BaseSpeedValues twistToBase(double linearVelX, double angVelZ) {
        int rightWheelVel;
        int leftWheelVel;

        if (linearVelX < 0) {
            angVelZ = -angVelZ;
        }

        double normalizer = (angVelZ * createBaseDiameter) / createWheelRadius;
        rightWheelVel = (int) (0.5 * (((2 * linearVelX) / createWheelRadius) - normalizer));
        leftWheelVel = (int) (0.5 * (((2 * linearVelX) / createWheelRadius) + normalizer));

        if ((leftWheelVel * rightWheelVel) < 0) {
            leftWheelVel = leftWheelVel * 2;
            rightWheelVel = rightWheelVel * 2;
        }
        return new BaseSpeedValues(leftWheelVel, rightWheelVel);
    }

    private void sendMovementPackage(BaseSpeedValues speeds) {
        int leftWheelVel = speeds.getLeftWheelVel();
        int rightWheelVel = speeds.getRightWheelVel();

        byte[] velocitiesArray= new byte[] {
                DirectDrive,
                (byte) leftWheelVel,
                (byte) (leftWheelVel >> 8),
                (byte) rightWheelVel,
                (byte) (rightWheelVel >> 8)
        };

        write(velocitiesArray);
    }

    private void write(byte[] command) {
        log.info("Writing a command to Device.");
        try {
            port.write(command, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Not implemented for the create base.
     * @return: Always 0 for all variables.
     */
    public BaseStatus getBaseStatus() {
        return new BaseStatus();
    }

    @Override
    public OdometryStatus getOdometryStatus() {
        return null;
    }

    @Override
    protected void setConnectionParameters(UsbSerialPort port) throws Exception {
        port.setParameters(9600, UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
    }
}
