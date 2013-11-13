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
import org.ros.exception.RosRuntimeException;

import java.io.IOException;
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
        return null;
    }

    // All BaseDevice classes have the same signature. Husky doesn't need velocity
    //conversion since is able to receive direct linear and angular velocities.
    private class BaseSpeedValues {
        private final int linearSpeed;
        private final int angularSpeed ;

        private BaseSpeedValues(int linearSpeed, int angularSpeed) {
            this.linearSpeed = linearSpeed;
            this.angularSpeed = angularSpeed;
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
                public void onNewData(final byte[] data) {}
            };

        serialInputOutputManager = new SerialInputOutputManager(serialDriver, listener);
        executorService.submit(serialInputOutputManager);
    }

    public void initialize() {}

    public void move(double linearVelX, double angVelZ) {
        BaseSpeedValues speeds = twistToBase(linearVelX, angVelZ);
        sendMovementPackage(speeds);
    }

    // All BaseDevice classes have the same signature. Husky doesn't need velocity
    //conversion since is able to receive direct linear and angular velocities.
    private BaseSpeedValues twistToBase(double linearVelX, double angVelZ) {
        return new BaseSpeedValues((int) linearVelX, (int) angVelZ);
    }

    private void sendMovementPackage(BaseSpeedValues speeds) {
        int linearSpeed = speeds.getLinearSpeed();
        int angSpeed = speeds.getAngSpeed();
        int linearAccel = 0x05;         // Fixed acceleration of 5[m/s²]
        int MSGType = 0x0204;           // Set velocities using kinematic model

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
            (byte) (linearAccel >> 8),
        };

        write(buildPackage(baseControlMsg));
    }

    int checkSum(byte[] cmdPackage) {
        // See Appendix A of "clearpath control protocol" doc,to understand how to implement it
        //Polynomial: x16+x12+x5+1 (0x1021)
        //Initial value: 0xFFFF
        //Check constant: 0x1D0F

        //CRC lookup table for polynomial 0x1021
        int table[] = {0, 4129, 8258, 12387, 16516, 20645, 24774, 28903, 33032, 37161, 41290,
                45419, 49548, 53677, 57806, 61935, 4657, 528, 12915, 8786, 21173, 17044, 29431,
                25302, 37689, 33560, 45947, 41818, 54205, 50076, 62463, 58334, 9314, 13379, 1056,
                5121, 25830, 29895, 17572, 21637, 42346, 46411, 34088, 38153, 58862, 62927, 50604,
                54669, 13907, 9842, 5649, 1584, 30423, 26358, 22165, 18100, 46939, 42874, 38681,
                34616, 63455, 59390, 55197, 51132, 18628, 22757, 26758, 30887, 2112, 6241, 10242,
                14371, 51660, 55789, 59790, 63919, 35144, 39273, 43274, 47403, 23285, 19156, 31415,
                27286, 6769, 2640, 14899, 10770, 56317, 52188, 64447, 60318, 39801, 35672, 47931,
                43802, 27814, 31879, 19684, 23749, 11298, 15363, 3168, 7233, 60846, 64911, 52716,
                56781, 44330, 48395, 36200, 40265, 32407, 28342, 24277, 20212, 15891, 11826, 7761,
                3696, 65439, 61374, 57309, 53244, 48923, 44858, 40793, 36728, 37256, 33193, 45514,
                41451, 53516, 49453, 61774, 57711,4224, 161, 12482, 8419, 20484, 16421, 28742,
                24679, 33721, 37784, 41979, 46042, 49981, 54044, 58239, 62302, 689, 4752, 8947,
                13010, 16949, 21012, 25207, 29270, 46570, 42443, 38312, 34185, 62830, 58703, 54572,
                50445, 13538, 9411, 5280, 1153, 29798, 25671, 21540, 17413, 42971, 47098, 34713,
                38840, 59231, 63358, 50973, 55100, 9939, 14066, 1681, 5808, 26199, 30326, 17941,
                22068, 55628, 51565, 63758, 59695, 39368, 35305, 47498, 43435, 22596, 18533, 30726,
                26663, 6336, 2273, 14466, 10403, 52093, 56156, 60223, 64286, 35833, 39896,
                43963, 48026, 19061, 23124, 27191, 31254, 2801, 6864, 10931, 14994, 64814, 60687,
                56684, 52557, 48554, 44427, 40424, 36297, 31782, 27655, 23652, 19525, 15522, 11395,
                7392, 3265, 61215, 65342, 53085, 57212, 44955, 49082, 36825, 40952, 28183, 32310,
                20053, 24180, 11923, 16050, 3793, 7920};

        int initialValue = 0xFFFF;
        int size = cmdPackage.length - 2;
        int checksum = initialValue;
        int counter = 0;
        while(counter < size) {
            checksum = (checksum << 8) ^ table[((checksum >> 8)^cmdPackage[counter]) & 0xFFFF];
            counter++;
        }
        return checksum;
    }

    byte[] buildPackage(byte[] payload) {
        int checksum = 0;
        int payloadLength = payload.length;
        byte[] pkg = new byte[payloadLength + 10];
        long msgTime = System.currentTimeMillis();

        byte Flags = (byte) 0x00;                        // ACK suppressed
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

        checksum = checkSum(pkg);
        pkg[pkg.length - 2] = (byte)checksum;
        pkg[pkg.length - 1] = (byte)(checksum >> 8);

        return pkg;
    }

    private void write(byte[] command) {
        try {
            log.info("Writing a command to USB Device.");
            serialDriver.write(command, 1000);
        } catch (IOException e) {
            throw new RosRuntimeException(e);
        }
    }
}