/*
 * Copyright 2017 Ekumen, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ekumen.base_driver;

import android.hardware.usb.UsbDeviceConnection;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Encapsulates common functionality for all base devices, providing a common method to
 * establish a connection using a given {@link UsbSerialPort} and a {@link UsbDeviceConnection}.
 * Connection parameters have to be defined in each subclass, as they are device-dependent.
 */
public abstract class AbstractBaseDevice implements BaseDevice {

    protected UsbSerialPort port;
    private static final Log log = LogFactory.getLog(AbstractBaseDevice.class);

    public AbstractBaseDevice(UsbSerialPort port, UsbDeviceConnection connection) throws Exception {
        this.port = port;

        if (port == null || connection == null) {
            throw new Exception("null USB port/ connection provided");
        }

        try {
            port.open(connection);
            setConnectionParameters(port);
        } catch (IOException e) {
            log.info("Error setting up device: " + e.getMessage(), e);
            try {
                port.close();
            } catch (IOException e1) {
                log.error("Error closing device");
                e1.printStackTrace();
            }
        }
    }

    protected abstract void setConnectionParameters(UsbSerialPort port) throws Exception;

    @Override
    public abstract void initialize();

    @Override
    public abstract void move(double linearVelX, double angVelZ);

    @Override
    public abstract BaseStatus getBaseStatus();

    @Override
    public abstract OdometryStatus getOdometryStatus();
}
