base_controller
===============

This base controller for ROS on Android allows the user to drive the
Kobuki or Create bases from an Android device directly connected to the
desired base via USB.

Building
--------

To build this package you can just use `gradlew` from the command line, or
you can put the package inside a catkin workspace and built it with
`catkin_make`.

Usage
-----

These code fragments show how to use this package to create a
Kobuki driver in an ROS Activity. For the Create base you should simple
replace Kobuki with Create.


Import the [USB library](https://github.com/mik3y/usb-serial-for-android):

```java
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
```

Import the ROS nodes and the base drivers:

```java
import com.github.c77.base_controller.BaseControllerNode;
import com.github.c77.base_controller.BaseStatusPublisher;
import com.github.c77.base_driver.KobukiBaseDevice;
```

You can instantiate the nodes when creating the Main ROS Activity:

```java
public MainActivity() {
    super("MainActivity", "MainActivity");
    baseControllerNode = new BaseControllerNode("/cmd_vel");
    baseStatusPublisher = new BaseStatusPublisher();
}
```

In the init method of the Node, we use the USB library to find the device and create the base driver.
After creating the base driver, we can set the device to the two nodes.

```java
// Get UsbManager from Android.
UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
// Find the first available driver.
UsbSerialDriver driver = UsbSerialProber.findFirstDevice(manager);
// Create the low level base device.
KobukiBaseDevice kobukiBaseDevice = new KobukiBaseDevice(driver);
baseControllerNode.setBaseDevice(kobukiBaseDevice);
baseStatusPublisher.setBaseDevice(kobukiBaseDevice);
```

After this point use the nodeMainExecutor to launch the nodes.
