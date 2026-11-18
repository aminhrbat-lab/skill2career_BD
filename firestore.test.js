const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";
const ADMIN_UID = "admin_001";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function tsPast(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any NearMe collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("public_profiles").get());
  await assertFails(unauth.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("notifications").where("userId", "==", ALICE_UID).get());
  await assertFails(
    unauth.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice",
      username: "alice",
      age: 24,
      gender: "Female",
      onboardingCompleted: true,
      verificationStatus: "unverified",
      accountStatus: "active",
      locationSharingEnabled: false,
      invisibleMode: false,
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );
});

test("Public profiles enforce 18+ age, anti-self-verification, no @DocumentId id, and filtered list", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid create payload mirroring PublicProfileRepository
  await assertSucceeds(
    alice.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Rahman",
      username: "alice_r",
      age: 24,
      gender: "Female",
      bio: "Exploring coffee spots and tech.",
      cityLabel: "Dhaka",
      locationSharingEnabled: false,
      invisibleMode: false,
      discoveryRadiusKm: 5.0,
      onboardingCompleted: true,
      verificationStatus: "unverified",
      accountStatus: "active",
      createdAt: tsPast(5000),
      updatedAt: tsPast(5000),
    })
  );

  // Underage (<18) must fail
  await assertFails(
    bob.collection("public_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Young Bob",
      username: "bob_17",
      age: 17,
      gender: "Male",
      onboardingCompleted: true,
      verificationStatus: "unverified",
      accountStatus: "active",
      locationSharingEnabled: false,
      invisibleMode: false,
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );

  // Self-assigning verified status or badges must fail
  await assertFails(
    alice.collection("public_profiles").doc(ALICE_UID).update({
      verificationStatus: "verified",
      updatedAt: tsPast(500),
    })
  );
  await assertFails(
    alice.collection("public_profiles").doc(ALICE_UID).update({
      badges: ["Verified Member"],
      updatedAt: tsPast(500),
    })
  );

  // Allowed partial update succeeds
  await assertSucceeds(
    alice.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Updated bio!",
      locationSharingEnabled: true,
      approxLat: 23.81,
      approxLng: 90.41,
      updatedAt: tsPast(500),
    })
  );

  // Cross-user profile mutation fails
  await assertFails(
    bob.collection("public_profiles").doc(ALICE_UID).update({
      bio: "Hijacked",
      updatedAt: tsPast(500),
    })
  );

  // Client discovery query alignment succeeds; unfiltered query fails
  await assertSucceeds(
    bob
      .collection("public_profiles")
      .where("onboardingCompleted", "==", true)
      .where("accountStatus", "==", "active")
      .get()
  );
  await assertFails(bob.collection("public_profiles").get());
});

test("Private PII info is strictly owner-only", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      mobileNumber: "+8801700000000",
      dateOfBirth: "2000-05-10",
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );
});

test("Connections and private DMs enforce participant isolation (even against admins)", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const admin = testEnv
    .authenticatedContext(ADMIN_UID, { email: "aminhrbat@gmail.com", email_verified: true })
    .firestore();

  const connId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    alice.collection("connections").doc(connId).set({
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      participantUids: [ALICE_UID, BOB_UID],
      senderName: "Alice",
      receiverName: "Bob",
      introMessage: "Hey! Saw we both enjoy design.",
      status: "pending",
      interestScore: 92,
      lifestyleScore: 85,
      educationScore: 78,
      overallScore: 85,
      createdAt: tsPast(5000),
      updatedAt: tsPast(5000),
    })
  );

  // Bob accepts
  await assertSucceeds(
    bob.collection("connections").doc(connId).update({
      status: "accepted",
      updatedAt: tsPast(1000),
    })
  );

  // Charlie cannot read or query Alice's connections
  await assertFails(charlie.collection("connections").doc(connId).get());
  await assertSucceeds(
    alice.collection("connections").where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Conversation & Private DM
  const convId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    alice.collection("conversations").doc(convId).set({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: ["Alice", "Bob"],
      lastMessage: "Hi Bob!",
      lastSenderId: ALICE_UID,
      createdAt: tsPast(4000),
      updatedAt: tsPast(4000),
    })
  );

  await assertSucceeds(
    alice.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participantUids: [ALICE_UID, BOB_UID],
      text: "Hi Bob!",
      messageType: "text",
      unsent: false,
      readByUids: [ALICE_UID],
      createdAt: tsPast(3000),
      updatedAt: tsPast(3000),
    })
  );

  // Bob can read; Admin and Charlie CANNOT read private DM!
  await assertSucceeds(
    bob
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlie.collection("conversations").doc(convId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    admin.collection("conversations").doc(convId).collection("messages").doc("msg_1").get()
  );
});

test("Chatrooms, posts, stories, events, and notifications enforce visibility & ownership", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  // Public & private chatroom
  await assertSucceeds(
    alice.collection("chatrooms").doc("room_pub").set({
      creatorId: ALICE_UID,
      name: "Mymensingh Friends",
      roomType: "public",
      joinOption: "anyone",
      memberUids: [ALICE_UID],
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );
  await assertSucceeds(
    alice.collection("chatrooms").doc("room_priv").set({
      creatorId: ALICE_UID,
      name: "Secret Study Group",
      roomType: "private",
      joinOption: "invite",
      memberUids: [ALICE_UID],
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );

  await assertSucceeds(
    bob.collection("chatrooms").where("roomType", "in", ["public", "interest", "location"]).get()
  );
  await assertFails(bob.collection("chatrooms").doc("room_priv").get());

  // Public post vs private post
  await assertSucceeds(
    alice.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "Anyone up for coffee near Gulshan?",
      mediaType: "location",
      cityLabel: "Dhaka",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      likedByUids: [],
      savedByUids: [],
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );
  await assertSucceeds(bob.collection("posts").where("visibility", "==", "public").get());
  await assertFails(bob.collection("posts").get());

  // Notifications strictly owner-only
  await assertSucceeds(
    alice.collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "Welcome",
      body: "Profile created",
      eventType: "registration",
      read: false,
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );
  await assertFails(
    bob.collection("notifications").where("userId", "==", ALICE_UID).get()
  );
});

test("Admin RBAC rejects spoofed unverified admin email and allows verified admin", async () => {
  const spoofedAdmin = testEnv
    .authenticatedContext("spoof_1", { email: "aminhrbat@gmail.com", email_verified: false })
    .firestore();
  const realAdmin = testEnv
    .authenticatedContext(ADMIN_UID, { email: "aminhrbat@gmail.com", email_verified: true })
    .firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Spoofed email_verified=false cannot update platform_settings
  await assertFails(
    spoofedAdmin.collection("platform_settings").doc("global").set({
      registrationEnabled: true,
      gpsDiscoveryEnabled: true,
      chatroomsEnabled: true,
      photoUploadLimit: 20,
      dailyConnectionLimit: 25,
      premiumEnabled: false,
      adsEnabled: false,
      minRegistrationAge: 18,
      announcement: "Welcome",
      maintenanceMode: false,
      updatedAt: tsPast(),
    })
  );

  // Verified super admin succeeds
  await assertSucceeds(
    realAdmin.collection("platform_settings").doc("global").set({
      registrationEnabled: true,
      gpsDiscoveryEnabled: true,
      chatroomsEnabled: true,
      photoUploadLimit: 20,
      dailyConnectionLimit: 25,
      premiumEnabled: false,
      adsEnabled: false,
      minRegistrationAge: 18,
      announcement: "Welcome to NearMe!",
      maintenanceMode: false,
      updatedAt: tsPast(),
    })
  );

  // Normal user can read platform_settings but cannot write
  await assertSucceeds(alice.collection("platform_settings").doc("global").get());
  await assertFails(
    alice.collection("platform_settings").doc("global").update({
      maintenanceMode: true,
      updatedAt: tsPast(),
    })
  );

  // Immutable admin audit log
  await assertSucceeds(
    realAdmin.collection("admin_audit_logs").doc("log_1").set({
      adminId: ADMIN_UID,
      action: "UPDATE_SETTINGS",
      targetId: "global",
      previousState: "default",
      newState: "configured",
      reason: "Initial setup",
      createdAt: tsPast(),
      updatedAt: tsPast(),
    })
  );
  await assertFails(
    realAdmin.collection("admin_audit_logs").doc("log_1").delete()
  );
});
