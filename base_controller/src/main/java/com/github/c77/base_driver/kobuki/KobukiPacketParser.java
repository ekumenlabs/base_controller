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

import android.util.Log;

import com.github.c77.base_driver.AbstractOdometryStatus;
import com.github.c77.base_driver.BaseStatus;
import com.github.c77.base_driver.InertialInformation;

/**
 * Created by Lucas Chiesa on 10/10/13.
 */

public class KobukiPacketParser {

    private final int TIMESTAMP = 0;
    private final int BUMPER = 2;
    private final int WHEEL_DROP = 3;
    private final int CLIFF = 4;
    private final int BUTTON = 11;
    private final int CHARGER = 12;
    private final int BATTERY = 13;

    private final int ANGLE = 0;
    private final int ANGLE_RATE = 2;

    private final int LEFT_ENC = 5;
    private final int RIGHT_ENC = 7;

    private final double TICKS_TO_MM = 11.7;

    private int prevLeftEncoder;
    private int prevRightEncoder;
    private short leftLoops = 0;
    private short rightLoops = 0;


    private void updateOdometry(byte[] sensorPacket, BaseStatus baseStatus) {
        final short MAX_ODOM = (short) 65535;
        final short lowMark = MAX_ODOM / 4;
        final short highMark = MAX_ODOM - lowMark;
        short leftEncoder;
        short rightEncoder;
        int currLeftEncoder;
        int currRightEncoder;
        int acumLeftEncoder;
        int acumRightEncoder;

        leftEncoder = ((short) ((sensorPacket[LEFT_ENC+1] << 8) | (sensorPacket[LEFT_ENC] & 0xFF)));
        rightEncoder = ((short) ((sensorPacket[RIGHT_ENC+1] << 8) | (sensorPacket[RIGHT_ENC] & 0xFF)));
        currLeftEncoder = (int) (leftEncoder & 0x0000ffffL);
        currRightEncoder = (int) (rightEncoder & 0x0000ffffL);

        if (prevLeftEncoder > highMark && currLeftEncoder < lowMark) {
            Log.e("PARSER", "OVER. Left loops: " + Integer.toString(leftLoops));
            leftLoops++;
        }
        if (prevLeftEncoder < lowMark && currLeftEncoder > highMark) {
            Log.e("PARSER", "UNDER Left loops: " + Integer.toString(leftLoops));
            leftLoops--;
        }

        if (prevRightEncoder > highMark && currRightEncoder < lowMark) {
            Log.e("PARSER", "OVER. Right loops: " + Integer.toString(rightLoops));
            rightLoops++;
        }
        if (prevRightEncoder < lowMark && currRightEncoder > highMark) {
            Log.e("PARSER", "UNDER. Right loops: " + Integer.toString(rightLoops));
            rightLoops--;
        }

        acumLeftEncoder = (MAX_ODOM * leftLoops) + currLeftEncoder;
        acumRightEncoder = (MAX_ODOM * rightEncoder) + currLeftEncoder;
        // TODO: Deal with wrapping or "circulation"
        // according to spec: this goes from 0 to 65535 and circles
        baseStatus.setLeftDistance((int)Math.round(acumLeftEncoder / TICKS_TO_MM));
        baseStatus.setRightDistance((int)Math.round(acumRightEncoder / TICKS_TO_MM));
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
