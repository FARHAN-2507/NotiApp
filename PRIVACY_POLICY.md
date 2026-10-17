# Privacy Policy — Payment Alert

**Last updated: 17 October 2026**
**Developer: Farhan**
**Contact: farhanakthar99@gmail.com**

Payment Alert ("the app") detects incoming UPI payments on an Android phone
("receiver phone") and sends instant alerts to another phone ("owner phone")
chosen by the user. This policy explains exactly what the app accesses, what
is stored, and what leaves the device. In short: the app only processes
payment information, it stays under your control, and it is never sold or
shared with third parties.

## 1. What the app accesses

- **Notifications (Notification Access permission).** The app reads
  notifications posted by payment apps it monitors (currently PhonePe). It
  does not read notifications from any other app. The purpose is solely to
  detect incoming payment messages.
- **SMS (Receive SMS permission).** The app reads incoming SMS messages only
  to detect bank credit alerts (messages from bank sender IDs such as
  `AD-HDFCBK`). Non-bank SMS, personal messages, and OTPs are ignored and are
  not saved or transmitted.
- **Internet.** Used only to send and receive payment records through Google
  Firebase (see section 3) when device syncing is enabled.

The app does **not** access contacts, location, camera, microphone, photos,
files, call logs, or browsing history.

## 2. What is stored on the device

- Detected payments: amount, sender name, source (notification or SMS),
  timestamp, and the raw message text. These are stored in a private local
  database on the receiver phone.
- A short debug log (last 50 monitored messages, parsed/rejected) to help
  troubleshoot missed payments. It can be cleared anytime from the Logs
  screen.
- App settings: device role, the 6-digit shop code, and alert preferences.

All local data is removed when you clear history in the app or uninstall it.

## 3. What is transmitted

When syncing is enabled (owner-alerts feature):

- Detected payment records — **amount, sender name, source, timestamp, and a
  one-way hash of the message text** — are sent to Google Firebase
  (Cloud Firestore), encrypted in transit (TLS/HTTPS).
- On the owner phone, the app stores a Firebase Cloud Messaging device token
  that identifies the phone for push notifications.
- Authentication is anonymous — no name, email, phone number, or password is
  required or collected.

Data in Firebase is stored under the shop's 6-digit code and can be read only
with that code. Keep the code private; anyone who knows it can view that
shop's payments. Data is retained until the developer deletes it or the code
is regenerated.

## 4. What is never done

- No selling, renting, or sharing of data with advertisers or other third
  parties.
- No analytics, tracking, or advertising SDKs.
- No collection of message content beyond what is needed to detect a payment.
- No use of data for any purpose other than delivering payment alerts.

## 5. Security

- Data is encrypted in transit using industry-standard TLS.
- Local data is protected by Android app sandboxing (other apps cannot read
  the app's private database).
- Access to synced data requires the private 6-digit shop code.
- WARNING: Alerts are a convenience, not proof of payment. Always verify
  important amounts in your bank or payment app.

## 6. Your control

- You can clear all locally stored payments and the debug log at any time
  from within the app.
- You can disable alerts (which stops the owner watcher completely) or turn
  off syncing by changing the device role.
- Revoking Notification Access or SMS permission stops the corresponding
  detection immediately.
- Uninstalling the app removes all local app data from that phone.

## 7. Children

The app is not directed at children under 13 and does not knowingly collect
data from them.

## 8. Changes to this policy

If this policy changes, the updated version will be included with the app
release, with a new "last updated" date.

## 9. Contact

For any questions, support, or data-deletion requests:

**Farhan** — farhanakthar99@gmail.com
