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

package com.github.c77.base_driver.kobuki;

import com.github.c77.base_driver.AbstractOdometryStatus;
import com.github.c77.base_driver.BaseStatus;
import com.github.c77.base_driver.InertialInformation;

/**
 * Created by Lucas Chiesa on 10/10/13.
 */

public class KobukiPacketParser extends AbstractOdometryStatus {

    private int TIMESTAMP = 0;
    private int BUMPER = 2;
    private int WHEEL_DROP = 3;
    private int CLIFF = 4;
    private int BUTTON = 11;
    private int CHARGER = 12;
    private int BATTERY = 13;

    private int ANGLE = 0;
    private int ANGLE_RATE = 2;

    private int LEFT_ENC = 5;
    private int RIGHT_ENC = 7;

    private void updateOdometry(byte[] sensorPacket, BaseStatus baseStatus) {
        short leftEncoder;
        short rightEncoder;
        int thisValueL;
        int thisValueR;

        leftEncoder = ((short) ((sensorPacket[LEFT_ENC+1] << 8) | (sensorPacket[LEFT_ENC] & 0xFF)));
        rightEncoder = ((short) ((sensorPacket[RIGHT_ENC+1] << 8) | (sensorPacket[RIGHT_ENC] & 0xFF)));
        thisValueL = (int) (leftEncoder & 0x0000ffffL);
        thisValueR = (int) (rightEncoder & 0x0000ffffL);

        baseStatus.setLeftDistance(thisValueL);
        baseStatus.setRightDistance(thisValueR);
    }

    public BaseStatus parseBaseStatus(byte[] sensorPacket) {
        BaseStatus baseStatus = new BaseStatus();
        baseStatus.setBumper(sensorPacket[BUMPER]);

        baseStatus.setTimeStamp((short) (sensorPacket[TIMESTAMP + 1] << 8 | sensorPacket[TIMESTAMP]));
        baseStatus.setWheelDrop(sensorPacket[WHEEL_DROP]);
        baseStatus.setCliff(sensorPacket[CLIFF]);
        baseStatus.setButton(sensorPacket[BUTTON]);
        baseStatus.setCharger(sensorPacket[CHARGER]);
        baseStatus.setBattery(sensorPacket[BATTERY]);

        updateOdometry(sensorPacket, baseStatus);
        return baseStatus;
    }

    public InertialInformation getInertialInformation (byte[] imuPacket) {
        InertialInformation inertialInformation = new InertialInformation();
        inertialInformation.setAngle((short) ((imuPacket[ANGLE+1] << 8)|(imuPacket[ANGLE] & 0xff)));
        inertialInformation.setAngleRate((short) ((imuPacket[ANGLE_RATE + 1] << 8) | (imuPacket[ANGLE_RATE] & 0xff)));
        return inertialInformation;
    }

}
