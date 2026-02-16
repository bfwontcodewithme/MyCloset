const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const OpenAI = require("openai");

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

// ✅ NEW: AI Suggest Outfit (Callable Function)
exports.aiSuggestOutfit = onCall({ secrets: ["OPENAI_API_KEY"] }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new Error("unauthenticated");
  }

  const { season, tag, items, maxItems = 5 } = request.data || {};
  if (!Array.isArray(items) || items.length === 0) {
    return { pickedItemIds: [], reason: "No items provided" };
  }

  const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

  const prompt = {
    season: season || "Any",
    tag: tag || "",
    maxItems,
    items: items.map((i) => ({
      id: i.id,
      type: i.type,
      color: i.color,
      season: i.season,
      tags: i.tags || [],
      wearCount: i.wearCount || 0,
      lastWornAt: i.lastWornAt || 0
    }))
  };

  const resp = await client.responses.create({
    model: "gpt-5.2",
    instructions:
      "You are a stylist assistant. Return STRICT JSON only: " +
      "{\"pickedItemIds\": [\"...\"], \"reason\": \"...\"}. " +
      "Pick items that match season/tag, balance colors, and prefer items not worn recently.",
    input: JSON.stringify(prompt)
  });

  let parsed;
  try {
    parsed = JSON.parse(resp.output_text);
  } catch (e) {
    parsed = { pickedItemIds: [], reason: "AI returned non-JSON" };
  }

  // keep only valid ids
  const validIds = new Set(prompt.items.map((x) => x.id));
  const picked = (parsed.pickedItemIds || [])
    .filter((id) => validIds.has(id))
    .slice(0, maxItems);

  return { pickedItemIds: picked, reason: parsed.reason || "" };
});
