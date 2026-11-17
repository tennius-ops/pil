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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write bloodSugarRecords", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/bloodSugarRecords`).get()
  );
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/bloodSugarRecords`)
      .doc("rec_1")
      .set({
        userId: ALICE_UID,
        valueMgDl: 110,
        timing: "FASTING",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 8:30",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated owner: valid create without note, create with note, query, update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty list query succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .orderBy("createdAt", "desc")
      .get()
  );

  // Create without optional note
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("rec_1")
      .set({
        userId: ALICE_UID,
        valueMgDl: 105,
        timing: "FASTING",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 7:45",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create with optional note
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("rec_2")
      .set({
        userId: ALICE_UID,
        valueMgDl: 142,
        timing: "AFTER_MEAL",
        measuredDate: "2026-11-16",
        measuredTimeText: "오후 1:10",
        note: "점심 식후 2시간",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update succeeds while preserving createdAt
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("rec_1")
      .update({
        valueMgDl: 108,
        note: "재측정",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // List query matching Kotlin BloodSugarRepository succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .orderBy("createdAt", "desc")
      .get()
  );

  // Delete succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("rec_2")
      .delete()
  );
});

test("Cross-user isolation: Alice cannot read, create, update, or delete Bob's records", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("bloodSugarRecords")
      .doc("bob_rec")
      .set({
        userId: BOB_UID,
        valueMgDl: 120,
        timing: "BEFORE_MEAL",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 11:30",
        createdAt: new Date(Date.now() - 10000),
        updatedAt: new Date(Date.now() - 10000),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("bloodSugarRecords")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("bloodSugarRecords")
      .doc("bob_rec")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("bloodSugarRecords")
      .doc("bob_rec")
      .update({ valueMgDl: 99, updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("bloodSugarRecords")
      .doc("bob_rec")
      .delete()
  );
});

test("Validation bounds reject out-of-range values, ghost fields, @DocumentId id field, and tampered timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const validTime = new Date(Date.now() - 2000);

  // Reject valueMgDl < 20 or > 600
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("bad_low")
      .set({
        userId: ALICE_UID,
        valueMgDl: 10,
        timing: "FASTING",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 8:00",
        createdAt: validTime,
        updatedAt: validTime,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("bad_high")
      .set({
        userId: ALICE_UID,
        valueMgDl: 700,
        timing: "FASTING",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 8:00",
        createdAt: validTime,
        updatedAt: validTime,
      })
  );

  // Reject invalid timing enum
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("bad_timing")
      .set({
        userId: ALICE_UID,
        valueMgDl: 115,
        timing: "RANDOM",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 8:00",
        createdAt: validTime,
        updatedAt: validTime,
      })
  );

  // Reject @DocumentId 'id' inside payload or ghost field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("bad_id_field")
      .set({
        id: "bad_id_field",
        userId: ALICE_UID,
        valueMgDl: 115,
        timing: "FASTING",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 8:00",
        createdAt: validTime,
        updatedAt: validTime,
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bloodSugarRecords")
      .doc("bad_future_ts")
      .set({
        userId: ALICE_UID,
        valueMgDl: 115,
        timing: "FASTING",
        measuredDate: "2026-11-16",
        measuredTimeText: "오전 8:00",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );
});
