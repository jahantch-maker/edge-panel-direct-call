# Edge Panel Direct Call

A simple, sleek one-tap calling app for Samsung phones.

- Up to **30 people** in a smooth, scrollable grid (2 or 3 columns)
- **Samsung Edge panel**: swipe in from the edge and tap a person to call instantly
- **Lock-screen calling**: the *Quick Call* icon (and an optional lock-screen notification) opens over the lock screen and dials without unlocking
- Add people from your contacts (name, number and photo) or type them in
- Tap to call · pencil button to edit, delete and drag to reorder

## Download

Latest APK: **https://github.com/jahantch-maker/edge-panel-direct-call/releases/latest/download/EdgePanelDirectCall.apk**

(Every push to `main` rebuilds the APK automatically and replaces the *latest* release.)

## Setup on the phone

1. Install the APK (allow "install unknown apps" for your browser/file manager when asked).
2. Open **Direct Call**, allow phone access, add your people.
3. Turn on the Edge panel: **Settings → Display → Edge panels → tick "Direct Call"**.
4. Optional, for the lock screen: in the app tap the sliders icon → switch on **Lock-screen shortcut**, or add the **Quick Call** app as a lock-screen shortcut.

## Notes

- Release builds are signed with the key in `app/release.jks` (password `directcall`) so each new build installs over the previous one. It is a personal-use key; don't reuse it for anything important.
- Build locally: `gradle assembleRelease` (needs JDK 17 + Android SDK 35).
