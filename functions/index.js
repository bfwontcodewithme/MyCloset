const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.notifyStylistOnNewRequest = onDocumentCreated(
  "styling_requests/{requestId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data = snap.data();
    const stylistId = data?.stylistId;

    if (!stylistId) {
      console.log("No stylistId in request");
      return;
    }

    // get stylist user doc
    const userDoc = await admin.firestore().collection("users").doc(stylistId).get();
    if (!userDoc.exists) {
      console.log("Stylist not found:", stylistId);
      return;
    }

    const userData = userDoc.data() || {};
    const fcmToken = userData.fcmToken;

    if (!fcmToken) {
      console.log("No FCM token for stylist:", stylistId);
      return;
    }

    const requestId = event.params.requestId;

    const message = {
      token: fcmToken,
      notification: {
        title: "New Styling Request 👗",
        body: "You received a new styling request",
      },
      data: {
        requestId: String(requestId),
      },
    };

    await admin.messaging().send(message);
    console.log("Notification sent to stylist:", stylistId, "requestId:", requestId);
  }
);
