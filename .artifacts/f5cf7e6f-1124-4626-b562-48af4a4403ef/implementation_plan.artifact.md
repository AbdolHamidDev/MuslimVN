# Apply Custom Notification Sound Globally

The goal is to use `muslimvn_notification.ogg` as the default notification sound for all notifications in the MuslimVN app. This involves configuring the notification channel (for Android 8.0+) and the notification builder (for backward compatibility).

## Proposed Changes

### Core Module

#### [MODIFY] [NotificationModule.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/core/di/NotificationModule.kt)
- Update `provideNotificationManager` to set the custom sound URI and audio attributes for the `ADHAN_CHANNEL_ID` channel.
- Note: Since notification channel properties are largely immutable after creation, existing users might need to reinstall or clear app data for the sound change to take effect if the channel ID remains the same. I will update the `ADHAN_CHANNEL_ID` to ensure it takes effect.

#### [MODIFY] [AdhanReceiver.kt](file:///home/hamid/AndroidStudioProjects/MuslimVN/app/src/main/java/com/example/muslimvn/core/utils/AdhanReceiver.kt)
- Update `showNotification` to set the sound URI in the `NotificationCompat.Builder`. This ensures the custom sound is used on older Android versions.

## Verification Plan

### Automated Tests
- I will check if the build passes after these changes.

### Manual Verification
- Deploy the app to an emulator or device.
- Trigger a notification (e.g., an adhan alarm) and verify that the `muslimvn_notification.ogg` sound plays.
