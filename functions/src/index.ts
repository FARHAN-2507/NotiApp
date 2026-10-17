import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";

initializeApp();

const db = getFirestore();

export const onPaymentCreated = onDocumentCreated(
  {
    document: "shops/{code}/payments/{paymentId}",
    region: "asia-south1",
  },
  async (event) => {
    const payment = event.data?.data();
    if (!payment) return;

    const code = event.params.code;
    const tokensSnap = await db.collection(`shops/${code}/tokens`).get();
    const entries = tokensSnap.docs
      .map((doc) => ({ id: doc.id, token: doc.get("token") as string | undefined }))
      .filter((entry): entry is { id: string; token: string } => Boolean(entry.token));

    if (entries.length === 0) {
      logger.info(`No owner tokens registered for shop ${code}`);
      return;
    }

    const amount = Number(payment.amount ?? 0);
    const sender = typeof payment.sender === "string" ? payment.sender : "";
    const body = sender ? `from ${sender}` : "Payment received";

    const response = await getMessaging().sendEachForMulticast({
      tokens: entries.map((entry) => entry.token),
      data: {
        amount: String(amount),
        sender,
      },
      notification: {
        title: `₹${amount} received`,
        body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "payments",
        },
      },
    });

    const cleanups: Promise<unknown>[] = [];
    response.responses.forEach((result, index) => {
      if (result.success) return;
      const errorCode = result.error?.code ?? "";
      const invalid =
        errorCode === "messaging/registration-token-not-registered" ||
        errorCode === "messaging/invalid-registration-token" ||
        errorCode === "messaging/invalid-argument";
      if (invalid) {
        cleanups.push(db.doc(`shops/${code}/tokens/${entries[index].id}`).delete());
      } else {
        logger.warn(`Push failed for token ${entries[index].id}: ${errorCode}`);
      }
    });
    await Promise.all(cleanups);

    logger.info(`Sent ${response.successCount}/${entries.length} alerts for shop ${code}`);
  },
);
