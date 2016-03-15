# android-fingerprint-example
Example implementation using Android 6 Fingerprint Authentication

# Usage
Build, install & run
```bash
./gradlew assembleDebug installDebug
adb shell am start -n com.urucas.fingerprintexample/.MainActivity
```
Wait for activity to show the Fingerprint dialog, then emulate the fingerprint
```bash
adb -e emu finger touch 1111
```
