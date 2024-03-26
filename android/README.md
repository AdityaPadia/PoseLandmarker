# PhysioFit Demo

### Overview
This is an app that detects pose landmarks on a person from a continuous camera frames seen by your device's front camera and a pre-recorded video of a physiotherapy workout. The pose landmarks are compared in real-time to provide visual and voice feedback to the user performing the exercise.
The pose landmarker model used is developed my MediaPipe by Google. 

This application should be run on a physical Android device to take advantage of the camera.

![Pose Landmarker Demo](pose_landmarker.png?raw=true "Pose Landmarker Demo")
[Public domain video from Lance Foss](https://www.youtube.com/watch?v=KALIKOd1pbA)

## Build the demo using Android Studio

### Prerequisites

*   The **[Android Studio](https://developer.android.com/studio/index.html)** IDE. This sample has been tested on Android Studio Dolphin.

*   A physical Android device with a minimum OS version of SDK 24 (Android 7.0 -
    Nougat) with developer mode enabled. The process of enabling developer mode
    may vary by device.

### Building

*   Open Android Studio. From the Welcome screen, select Open an existing
    Android Studio project.

*   From the Open File or Project window that appears, navigate to and select
    the mediapipe/examples/pose_landmarker/android directory. Click OK. You may
    be asked if you trust the project. Select Trust.

*   If it asks you to do a Gradle Sync, click OK.

*   With your Android device connected to your computer and developer mode
    enabled, click on the green Run arrow in Android Studio.

### Models used

Downloading, extraction, and placing the models into the *assets* folder is
managed automatically by the **download.gradle** file.
