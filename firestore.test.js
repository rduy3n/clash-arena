const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";
const ADMIN_UID = "admin_yaniss";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function baseRoom(roomCode = "K9X4M2") {
  return {
    roomCode,
    hostId: ALICE_UID,
    hostName: "Alice",
    guestId: "",
    guestName: "",
    participantIds: [ALICE_UID],
    status: "waiting",
    stageId: "cyber_street",
    hostFighterId: "glitcherman",
    guestFighterId: "jardin",
    hostReady: false,
    guestReady: false,
    roundNumber: 1,
    p1RoundsWon: 0,
    p2RoundsWon: 0,
    winnerSide: "none",
    p1X: 0.25,
    p1Y: 0.0,
    p1Facing: 1,
    p1Action: "idle",
    p1ActionSeq: 0,
    p1Blocking: false,
    p1Ulti: 0,
    p1DealtDmg: 0,
    p2X: 0.75,
    p2Y: 0.0,
    p2Facing: -1,
    p2Action: "idle",
    p2ActionSeq: 0,
    p2Blocking: false,
    p2Ulti: 0,
    p2DealtDmg: 0,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
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

test("Unauthenticated user cannot read or write users, match_history, or match_rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("match_history").get());
  await assertFails(unauthDb.collection("match_rooms").doc("K9X4M2").get());
  await assertFails(unauthDb.collection("match_rooms").where("participantIds", "array-contains", ALICE_UID).get());
});

test("Authenticated user can manage own profile and match_history, isolated from other users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      favoriteFighterId: "glitcherman",
      wins: 0,
      losses: 0,
      winStreak: 0,
      bestStreak: 0,
      totalDamageDealt: 0,
      roundsWonTotal: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update test: extra ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wins: 5,
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );

  // Match history create + list + immutability + cross-user isolation
  const matchPayload = {
    mode: "VS_BOT",
    myFighterId: "glitcherman",
    opponentFighterId: "le_chinois",
    opponentName: "Maître Chen (Normal)",
    stageId: "orbital_station",
    myRoundsWon: 2,
    opponentRoundsWon: 1,
    damageDealt: 3450,
    won: true,
    createdAt: nowTs(),
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("match_history").doc("m_1001").set(matchPayload)
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("match_history").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("match_history").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("match_history").doc("m_1001").update({ won: false })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("match_history").doc("m_1001").delete()
  );
});

test("Host & guest full online room lifecycle including round reset, finish, rematch to character selection, and guest leave", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const roomRefAlice = aliceDb.collection("match_rooms").doc("K9X4M2");
  const roomRefBob = bobDb.collection("match_rooms").doc("K9X4M2");
  const roomRefCharlie = charlieDb.collection("match_rooms").doc("K9X4M2");

  // Create room
  await assertSucceeds(roomRefAlice.set(baseRoom("K9X4M2")));

  // Invalid room code rejected
  await assertFails(aliceDb.collection("match_rooms").doc("bad").set(baseRoom("bad")));

  // Bob joins waiting room
  await assertSucceeds(
    roomRefBob.update({
      guestId: BOB_UID,
      guestName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      status: "selecting",
      guestReady: false,
      updatedAt: nowTs(),
    })
  );

  // Non-participant Charlie cannot read active selecting room
  await assertFails(roomRefCharlie.get());

  // Guest readies up
  await assertSucceeds(
    roomRefBob.update({
      guestFighterId: "le_chinois",
      guestReady: true,
      updatedAt: nowTs(),
    })
  );

  // Host starts fight resetting all combat fields
  await assertSucceeds(
    roomRefAlice.update({
      hostFighterId: "daron_bresilien",
      hostReady: true,
      stageId: "orbital_station",
      status: "fighting",
      roundNumber: 1,
      p1RoundsWon: 0,
      p2RoundsWon: 0,
      winnerSide: "none",
      p1X: 0.25, p1Y: 0.0, p1Facing: 1, p1Action: "idle", p1ActionSeq: 0, p1Blocking: false, p1Ulti: 0, p1DealtDmg: 0,
      p2X: 0.75, p2Y: 0.0, p2Facing: -1, p2Action: "idle", p2ActionSeq: 0, p2Blocking: false, p2Ulti: 0, p2DealtDmg: 0,
      updatedAt: nowTs(),
    })
  );

  // P2 cannot modify P1 fields
  await assertFails(
    roomRefBob.update({
      p1DealtDmg: 999,
      updatedAt: nowTs(),
    })
  );

  // P2 updates P2 combat state
  await assertSucceeds(
    roomRefBob.update({
      p2X: 0.6,
      p2Action: "classic",
      p2ActionSeq: 1,
      p2DealtDmg: 45,
      updatedAt: nowTs(),
    })
  );

  // Host advances round cleanly resetting BOTH P1 and P2 states
  await assertSucceeds(
    roomRefAlice.update({
      roundNumber: 2,
      p1RoundsWon: 1,
      p2RoundsWon: 0,
      p1X: 0.25, p1Y: 0.0, p1Facing: 1, p1Action: "idle", p1ActionSeq: 0, p1Blocking: false, p1Ulti: 0, p1DealtDmg: 0,
      p2X: 0.75, p2Y: 0.0, p2Facing: -1, p2Action: "idle", p2ActionSeq: 0, p2Blocking: false, p2Ulti: 0, p2DealtDmg: 0,
      updatedAt: nowTs(),
    })
  );

  // Host finishes match
  await assertSucceeds(
    roomRefAlice.update({
      p1RoundsWon: 2,
      status: "finished",
      winnerSide: "p1",
      updatedAt: nowTs(),
    })
  );

  // Regular overwrite on finished room blocked (e.g., changing winnerSide directly)
  await assertFails(
    roomRefAlice.update({
      winnerSide: "p2",
      updatedAt: nowTs(),
    })
  );

  // Rematch! Either participant can reset finished room back to selecting inside the same room
  await assertSucceeds(
    roomRefBob.update({
      status: "selecting",
      hostReady: false,
      guestReady: false,
      roundNumber: 1,
      p1RoundsWon: 0,
      p2RoundsWon: 0,
      winnerSide: "none",
      p1X: 0.25, p1Y: 0.0, p1Facing: 1, p1Action: "idle", p1ActionSeq: 0, p1Blocking: false, p1Ulti: 0, p1DealtDmg: 0,
      p2X: 0.75, p2Y: 0.0, p2Facing: -1, p2Action: "idle", p2ActionSeq: 0, p2Blocking: false, p2Ulti: 0, p2DealtDmg: 0,
      updatedAt: nowTs(),
    })
  );

  // Guest leaves room cleanly back to waiting
  await assertSucceeds(
    roomRefBob.update({
      guestId: "",
      guestName: "",
      participantIds: [ALICE_UID],
      status: "waiting",
      hostReady: false,
      guestReady: false,
      updatedAt: nowTs(),
    })
  );
});

test("Admin email spoofing (unverified) is rejected; verified admin can moderate", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: "yanisscaboulin@gmail.com", email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "yanisscaboulin@gmail.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    bobDb.collection("playtest_feedbacks").doc("fb_bob").set({
      authorId: BOB_UID,
      authorName: "Bob",
      modeTested: "En ligne",
      category: "Bug",
      fighterId: "all",
      rating: 4,
      comment: "Test de modération admin.",
      createdAt: nowTs(),
    })
  );

  // Spoofed unverified admin cannot delete Bob's feedback
  await assertFails(spoofedAdminDb.collection("playtest_feedbacks").doc("fb_bob").delete());
  // Real verified admin can delete it
  await assertSucceeds(realAdminDb.collection("playtest_feedbacks").doc("fb_bob").delete());

  // Admin can also intervene on an active match room, spoofed admin cannot
  await assertSucceeds(bobDb.collection("match_rooms").doc("ADM999").set({
    ...baseRoom("ADM999"),
    hostId: BOB_UID,
    participantIds: [BOB_UID],
  }));
  await assertFails(spoofedAdminDb.collection("match_rooms").doc("ADM999").update({
    status: "finished",
    winnerSide: "p1",
    updatedAt: nowTs(),
  }));
  await assertSucceeds(realAdminDb.collection("match_rooms").doc("ADM999").update({
    status: "finished",
    winnerSide: "p1",
    updatedAt: nowTs(),
  }));
});
