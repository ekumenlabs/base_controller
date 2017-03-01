base_controller
===============

This base controller for ROS on Android allows the user to drive the
Kobuki or Create bases from an Android device directly connected to the
desired base via USB.

Building
--------

To build this package you can just use `gradlew` from the command line, or
you can put the package inside a catkin workspace and build it with
`catkin_make`.

Requirements
------------

In order to use this code to drive a Create or a Kobuki base,
an Android device with USB On The Go (OTG) is needed. A
[usb-otg](http://www.ebay.com/bhp/micro-usb-otg-cable-nexus-7) cable
is also needed.

We tested this code in:

* Nexus 7
* Nexus 7 2013
* Galaxy S4 Google Edition


Usage
-----

These code fragments show how to use this package to create a
Kobuki driver in a ROS Activity. For the Create base you should simply
replace Kobuki with Create.


First, import the [USB library](https://github.com/mik3y/usb-serial-for-android). You can use the available Maven Artifact if you don't want to build it from source. 
To do so, add the following dependency in your module's `build.gradle`:

```
dependencies {
  ...
  compile 'com.hoho.android:usb-serial-for-android:[0.2, 0.3)'
  ...
}
```

Then, import the new objects in your Java source file:

```java
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
```

Import the ROS nodes and the base drivers:

```java
import com.ekumen.base_controller.BaseControllerNode;
import com.ekumen.base_controller.BaseStatusPublisher;
import com.ekumen.base_driver.kobuki.KobukiBaseDevice;
```

In the `init` method of your `RosActivity`, use the USB library to find a driver, get a port and a connection.
Then, use them to create the Base Devices required to instantiate the nodes:

```java
// Get UsbManager from Android.
UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

// Get a driver list
List<UsbSerialDriver> driverList = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
// Continue only if we have a single driver in the list
if (driverList.isEmpty()) {
    throw new Exception("No drivers found for the supplied USB device: " + device);
}

// Use first available driver to get Port and Connection
UsbSerialDriver driver = driverList.get(0);
UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
UsbSerialPort port = driver.getPorts().get(0);

// Create the low level base device using the created Port and Connection
BaseDevice kobukiBaseDevice = new KobukiBaseDevice(port, connection);

// Create the nodes using the Base Device
BaseControllerNode baseControllerNode = new BaseControllerNode(kobukiBaseDevice, "/cmd_vel");
BaseStatusPublisher baseStatusPublisher = new BaseStatusPublisher(kobukiBaseDevice);
```

After this point, use the `nodeMainExecutor` to launch the nodes in the standard Rosjava way.

Maven Artifact
--------------
You can use base_controller in your project using the available Maven Artifact instead of building it from source. 
To do so, add the following dependency to your modules's `build.gradle`:

```
dependencies {
  ...
  compile 'com.ekumen.base_controller:base_controller_lib:[0.2, 0.3)'
  ...
}
```

Note: to do this, the [rosjava_mvn_repo](https://github.com/rosjava/rosjava_mvn_repo) should be available in your listed repositories.
The easiest way to ensure it is using the kinetic standard top-level `build.gradle` in your project (you can use base_controller's [build.gradle](https://github.com/ekumenlabs/base_controller/blob/kinetic/build.gradle) as an example).

