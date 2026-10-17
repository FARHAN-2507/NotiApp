# Payment Alert

Detects incoming UPI payments on a receiver's Android phone by reading payment app
notifications and bank credit SMS, shows them in a local list, and (when Firebase is
configured) pushes instant alerts to the owner's phone.

## Scope of this phase

Phase 0+1 (done): local detection
- `PaymentListenerService` (NotificationListenerService) reads `PhonePe` payment notifications.
- `SmsReceiver` reads bank credit SMS (e.g. HDFC, SBI, ICICI, Kotak-style formats).
- `PaymentParser` extracts amount + sender, ignores debits, failures, OTPs, personal SMS.
- Room stores detected payments locally; duplicate notifications/SMS for the same payment
  landing within a 3-minute window are collapsed.
- Compose UI: permission onboarding (Notification Access + SMS), live payments list, clear history.

Phase 2 (done, needs your Firebase project): owner alerts — works on the **free Spark plan**
- Receiver phone queues each detected payment locally (Room) and uploads it to Firestore
  (`shops/{code}/payments`) with retry via WorkManager when offline.
- Owner phone runs a watcher service (`PaymentWatchService`, foreground service) that listens
  to Firestore in real time and shows a high-priority notification ("₹250 received — from Rahul")
  the moment a new payment document appears. No server/Cloud Function required. The watcher
  also mirrors all synced payments (last 200) into the owner app's Payments list and total.
- The watcher restarts after reboot and keeps working with the app closed (ongoing low-priority
  notification "Watching for payments" is shown; battery-optimization exemption recommended).
- Devices link via a 6-digit shop code (no accounts/OTP yet; that is Phase 3).
- Optional: `functions/src/index.ts` (Cloud Function + FCM push) exists for the Blaze-plan
  variant; it is only needed if you later want true push while the owner app is force-stopped.

Later phases (per the plan): pairing security, QR, voice, heartbeat, release.

## Requirements

- JDK 17+ (built/tested with JDK 21)
- Android SDK with `platforms;android-35` and `build-tools;35.0.0` (path in `local.properties`)
- Android phone (API 26+)

## Build

```bash
./gradlew test assembleDebug        # unit tests + APK
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Phone setup

1. Open the app, tap **Open notification access settings** and enable "Payment Alert listener".
2. Tap **Grant SMS permission** and allow receiving SMS.
3. Keep the phone online and the app installed; PhonePe and the bank SMS will be picked up
   in the background.

## Test checklist (Phase 1 done-when)

1. Send a ₹1 PhonePe payment to the receiver phone → appears with correct amount/sender
   within seconds.
2. Bank credit SMS arrives → appears.
3. Send ₹1 (outgoing) → ignored.
4. Same payment produces both a PhonePe notification and a bank SMS → only one entry.
5. Failed/cancelled/pending payment notifications → ignored; OTPs and personal SMS → ignored.

## Phase 2 setup (one-time, in the Firebase console) — free Spark plan

The app builds and runs without Firebase (local-only mode). To enable owner alerts
(no billing/Blaze needed):

1. Create a project at https://console.firebase.google.com (e.g. `payment-alert`).
2. **Firestore Database** → Create → location `asia-south1` (Mumbai).
3. **Authentication** → Sign-in method → **Anonymous** → Enable.
4. **Project settings → Your apps → Add app → Android**:
   - Package name: `com.mofa.paymentalert`
   - Download `google-services.json` → place it in `app/google-services.json`
     (gitignored; needed at build time).
5. Deploy the security rules (needs Node.js 20+; rules deploy is free):

```bash
npm install -g firebase-tools
firebase login
firebase use --add          # pick your project
firebase deploy --only firestore:rules
```

6. Rebuild and install this APK on **both** phones:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On the **receiver phone**: open the app → Sync card → "This phone receives payments"
→ note the 6-digit code. On the **owner phone**: sync card → "I want alerts (owner)"
→ enter the code → tap "Allow notifications" if prompted → tap "Battery settings" and
allow the app to run without battery optimization (recommended for reliable alerts).

Test: send a ₹1 payment to the receiver. The owner phone should buzz within seconds.
The receiver's Sync card shows "Pending uploads" until a payment reaches Firestore.

Note: the owner watcher runs as a foreground service; alerts work with the app closed and
after reboot, but a "Watching for payments" notification stays visible. If the phone vendor
aggressively kills background apps (Xiaomi/Vivo/Oppo), also enable "Autostart"/"No restrictions"
for Payment Alert in the phone's settings.

The owner phone shows an **Alerts toggle at the top** of every screen: when ON, each new
payment triggers a notification + a spoken alert ("250 rupees received from RAHUL KUMAR")
using the phone's text-to-speech. When OFF, the watcher is stopped completely (no
notifications, no voice, no battery use) until it is switched back on.

### Optional: Blaze-plan push (Cloud Function + FCM)

If you later upgrade to Blaze (pay-as-you-go, generous free quota) you can get true push
delivery that even wakes a force-stopped app:

```bash
cd functions && npm install && cd ..
firebase deploy --only functions,firestore:rules
```

The receiver then uploads as before, the Cloud Function sends FCM to owner tokens, and
`FcmService` shows the same notification. Both mechanisms can coexist.

## Troubleshooting: a payment did not appear

The app keeps a **debug log** (last 50 notifications/SMS from monitored sources) at the
bottom of the main screen. Use it to debug detection:

- `system / Listener — Notification listener connected` means notification access is
  working. If you see no such entry, re-enable Notification Access for the app.
- After a PhonePe payment you should see an entry for `com.phonepe.app`:
  - `PARSED` + "parsed: ₹X from ..." — detection worked; check the payments list above.
  - `SKIPPED` + "rejected: ..." — the notification format is not supported yet. Copy the
    `Title`/`Text` lines from that entry and add them to `SampleMessages.kt`, then fix
    the patterns.
- If a PhonePe payment notification never shows up in the log at all, notification
  access is not bound to the app (re-enable it in settings).

## Extending the parser

Real message formats vary by app version, bank, and language. When a message does not
parse (or parses wrong):

1. Copy the exact notification/SMS text.
2. Add it to `app/src/test/java/com/mofa/paymentalert/parser/SampleMessages.kt`
   (use `expected = null` for messages that must be ignored).
3. Run `./gradlew testDebugUnitTest` — it fails, showing what to fix in
   `ParserRules.kt` / `PaymentParser.kt`.
4. Re-run until green; the corpus keeps every fixed format from regressing.

To support more apps, uncomment/add their package name in
`ParserRules.monitoredNotificationPackages`.

## Project structure

```
app/src/main/java/com/mofa/paymentalert/
├── PaymentAlertApp.kt          # manual DI: database + repository, IO scope
├── MainActivity.kt
├── data/
│   ├── DedupPolicy.kt          # amount + 3-min window + sender compatibility
│   ├── PaymentRepository.kt
│   ├── local/                  # Room: PaymentEntity, PaymentDao, AppDatabase
│   └── model/                  # ParsedPayment
├── parser/
│   ├── ParserRules.kt          # allow-list, credit/debit/ignore patterns, bank senders
│   └── PaymentParser.kt
├── service/
│   ├── PaymentListenerService.kt
│   └── SmsReceiver.kt
├── ui/                         # MainScreen, MainViewModel, theme
└── util/                       # hashing, formatting
app/src/test/...                # parser + dedup tests, SampleMessages corpus
```

## Known limitations

- SMS permission is restricted on Google Play; keep distribution as a private APK
  (as planned) or drop the SMS channel for a Play release.
- Unknown message formats are ignored rather than guessed — fix them via the corpus.
- Own outgoing payments are ignored by design (this app is for the receiving phone).
- Alerts are a convenience, not proof. Verify large amounts in the bank app.

## Privacy policy

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md). In short:

- Payment detection happens on-device; personal SMS, OTPs and other apps'
  notifications are ignored and never stored.
- When sync is enabled, only payment details (amount, sender, time, source,
  hashed message text) are sent to Google Firebase over TLS, readable with the
  shop code.
- No analytics, no ads, no selling or sharing with third parties.

The in-app **About** screen shows the same privacy summary, the installed
version, and the support contact.

## Author & support

Created by **Farhan**.

- Support, questions, bug reports: **farhanakthar99@gmail.com**
- In-app: drawer → **About** (creator, support email with one-tap "Email"
  button, privacy summary, app version).
