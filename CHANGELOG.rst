^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Changelog for package base_controller
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

0.2.0 (2017-02-22)
------------------
* Using the latest version of UsbSerialLibrary from public repository.
* Refactored com.github.c77 package name to com.ekumen.
* Corrected package name in manifest.
* Updated rosjava dependency to [0.3, 0.4)
* Dev: Higher odom publish frequency & tf child frame renamed
* Odom publish rate changed to 100 Hz using ROS functions.
  base_link changed to base_footprint (odometry should be published against a frame at ground level)
* Feature: safe stop
* Added safe stop in case cmd_vel are lost for a period of time (1s).
* Upgrade: new buildscripts and Kinetic dependencies
* Update rosjava bootsrap required version to current version
* Depend on catkinized hoho USB library.
* Update the usb library to work with the laser.
  Added a TODO note to remind us of changing the Laser link tf
  broadcasting.
* General clean-up and commenting, particularly of Husky base
* Simplify base nodes by requiring BaseDevice in the constructor
* Kobuki odometry fix
* Properly handle odometry count overflow.
  Kobuki odometry works currently now.
* Be a little bit more strict and Java-ish with driver for BaseDevice
* Publish odometry in odom
* Change the odometry topic to /odom
  Adds odometry publishing to Husky base controller
* Added tf publisher.
* Working Husky package parser
* Modified accel value which should have two bytes of length.
* Added HuskyBaseDevice initial implementation
* Update deps to use the new android_10 name.
* Add a short documentation to README file.
* Initial code release for the Base Controller
* Initial commit
* Contributors: Chad Rockey, Juan Ignacio Ubeira, Julian Cerruti, Lucas Chiesa, Sebastian Garcia Marra, tulku
