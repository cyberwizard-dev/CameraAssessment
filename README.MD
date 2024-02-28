# Camera Assessment App

The **Camera Assessment** app is designed to provide users with control over dual cameras (front and back) on their device. As the developer, I aimed to create a user-friendly interface that allows users to switch between front and back cameras, capture images, and save them to the device's gallery.

## Features

### Camera Control
- The app supports dual camera control, allowing users to switch between the front and back cameras with a single tap.
- Each camera preview is displayed within a box, providing a clear indication of the active camera.

### Image Capture
- Users can capture images using the active camera by tapping the "Save Image" button.
- Images are saved with a timestamp and camera identifier (front or back) to the device's gallery.

### Clock Display
- The app features a digital clock display that updates in real-time, providing users with the current time.

## Implementation Details

### Camera Preview
- The `CameraPreview` composable is responsible for rendering the camera preview within the app UI.
- It utilizes the CameraX library to manage camera functionality, including camera selection and image capture.

### Image Capture
- Image capture functionality is implemented using the `ImageCapture` class from the CameraX library.
- Upon capturing an image, it is saved to the device's gallery using the appropriate metadata (timestamp and camera identifier).

### Permissions Handling
- The app requests necessary permissions (camera and storage) from the user to ensure proper functionality.
- Permission requests are handled gracefully, with the app guiding users through the process if permissions are denied.

## Usage

1. Upon launching the app, users are prompted to grant camera and storage permissions.
2. Once permissions are granted, users can switch between the front and back cameras by tapping the respective camera previews.
3. To capture an image, users tap the "Save Image" button, which triggers the image capture process.
4. The captured image is then saved to the device's gallery with relevant metadata.

## Permissions

The **Camera Assessment** app requires the following permissions:

- **Camera**: Allows the app to access the device's camera for capturing images.
- **Storage**: Allows the app to save captured images to the device's gallery.

## Compatibility

The app is compatible with Android devices running Android API level 21 (Android 5.0 Lollipop) and above.

