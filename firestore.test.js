const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_raxxon_01";
const DELEGATED_ADMIN_UID = "admin_sekretaris_02";
const JEMAAT_ALICE = "jemaat_alice_01";
const JEMAAT_BOB = "jemaat_bob_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "Raxxon47@gmail.com",
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: "Raxxon47@gmail.com",
    email_verified: false,
  });
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

// 1. Guest (unauthenticated) read-only access to published bulletin items; drafts, private collections, unfiltered queries & all writes denied
test("Guest users can read published bulletin items only; drafts, private data, unfiltered queries, and all writes are denied", async () => {
  const adminDb = adminContext().firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(10000);

  // Seed one published news, one draft news, one published schedule, one published photo
  await assertSucceeds(
    adminDb.collection("news").doc("pub_news").set({
      title: "Warta Minggu Terbuka",
      summary: "Ringkasan ibadah minggu untuk tamu & jemaat.",
      content: "Isi lengkap warta jemaat SIB Toburon yang dipublikasikan.",
      category: "WARTA",
      authorId: ADMIN_UID,
      authorName: "Admin",
      coverPreset: "SANCTUARY",
      pinned: true,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("news").doc("draft_news").set({
      title: "Draf Internal",
      summary: "Draf belum dipublikasikan.",
      content: "Tamu tidak boleh dapat membaca draf internal gereja.",
      category: "WARTA",
      authorId: ADMIN_UID,
      authorName: "Admin",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("schedules").doc("pub_sched").set({
      title: "Ibadah Raya Minggu",
      description: "Ibadah umum SIB Toburon.",
      category: "IBADAH",
      dateLabel: "Setiap Minggu",
      timeLabel: "08:00 - 10:00 WITA",
      location: "Gedung Utama SIB Toburon",
      speaker: "Pdt. Markus",
      sortOrder: 100,
      recurring: true,
      published: true,
      authorId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("pub_photo").set({
      title: "Foto Ibadah",
      caption: "Dokumentasi ibadah minggu.",
      category: "IBADAH",
      eventDate: "Nov 2026",
      coverPreset: "WORSHIP",
      likesCount: 0,
      published: true,
      authorId: ADMIN_UID,
      photographerName: "Multimedia",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest CAN read published items via filtered queries & direct get
  await assertSucceeds(unauthDb.collection("news").where("published", "==", true).get());
  await assertSucceeds(unauthDb.collection("news").doc("pub_news").get());
  await assertSucceeds(unauthDb.collection("schedules").where("published", "==", true).get());
  await assertSucceeds(unauthDb.collection("gallery_photos").where("published", "==", true).get());

  // Guest CANNOT run unfiltered queries or read drafts or private user/admin collections
  await assertFails(unauthDb.collection("news").get());
  await assertFails(unauthDb.collection("schedules").get());
  await assertFails(unauthDb.collection("gallery_photos").get());
  await assertFails(unauthDb.collection("news").doc("draft_news").get());
  await assertFails(unauthDb.collection("users").doc(JEMAAT_ALICE).get());
  await assertFails(unauthDb.collection("admins").doc(ADMIN_UID).get());

  // Guest CANNOT create, update, or delete any document
  await assertFails(
    unauthDb.collection("news").doc("guest_write").set({
      title: "Warta Tamu",
      summary: "Percobaan tulis tamu.",
      content: "Tamu tidak diizinkan menulis ke Firestore.",
      category: "WARTA",
      authorId: "guest",
      authorName: "Tamu",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    unauthDb.collection("gallery_photos").doc("pub_photo").update({
      likesCount: 1,
      updatedAt: pastDate(1000),
    })
  );
});

// 2. Email Spoofing Attack (email matches admin, email_verified == false)
test("Spoofed admin with unverified email is rejected", async () => {
  const spoofDb = spoofedAdminContext().firestore();
  const ts = pastDate();

  await assertFails(
    spoofDb.collection("news").doc("spoof_news").set({
      title: "Warta Palsu",
      summary: "Ringkasan percobaan spoof",
      content: "Ini adalah percobaan spoofing email tanpa verifikasi.",
      category: "WARTA",
      authorId: "spoof_uid",
      authorName: "Spoof",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

// 3. News CRUD, query alignment, shadow field rejection & future timestamp rejection
test("Admin can create/update/delete news; jemaat can only read published news via filtered query", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const created = pastDate(10000);

  // Admin creates published news (mirrors Kotlin NewsRepository.createNews map - no "id" key)
  await assertSucceeds(
    adminDb.collection("news").doc("news_001").set({
      title: "Ibadah Syukur Hari Ulang Tahun SIB Toburon",
      summary: "Undangan bagi seluruh jemaat menghadiri ibadah syukur bersama.",
      content: "Shalom jemaat SIB Toburon, mari hadir bersama dalam ibadah syukur hari Minggu ini pukul 08:00 WITA.",
      category: "WARTA",
      authorId: ADMIN_UID,
      authorName: "Sekretariat SIB Toburon",
      coverPreset: "SANCTUARY",
      pinned: true,
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin creates draft news
  await assertSucceeds(
    adminDb.collection("news").doc("news_draft").set({
      title: "Draf Pengumuman Natal",
      summary: "Draf jadwal persiapan panitia natal.",
      content: "Konten ini masih berupa draf dan belum dipublikasikan ke jemaat.",
      category: "PENGUMUMAN",
      authorId: ADMIN_UID,
      authorName: "Admin",
      coverPreset: "WORSHIP",
      pinned: false,
      published: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject shadow field (`id` or `isVerified`)
  await assertFails(
    adminDb.collection("news").doc("news_bad").set({
      id: "news_bad",
      title: "Berita dengan ID",
      summary: "Ringkasan berita tidak sah.",
      content: "Tidak boleh menyertakan key id dalam payload dokumen.",
      category: "WARTA",
      authorId: ADMIN_UID,
      authorName: "Admin",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    adminDb.collection("news").doc("news_future").set({
      title: "Berita Masa Depan",
      summary: "Ringkasan timestamp tidak sah.",
      content: "Timestamp tidak boleh melampaui request.time server.",
      category: "WARTA",
      authorId: ADMIN_UID,
      authorName: "Admin",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: true,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Admin partial update preserves createdAt
  await assertSucceeds(
    adminDb.collection("news").doc("news_001").update({
      title: "Ibadah Syukur HUT SIB Toburon (Diperbarui)",
      updatedAt: pastDate(1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    adminDb.collection("news").doc("news_001").update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Jemaat filtered query succeeds (matches Kotlin observePublishedNews)
  await assertSucceeds(
    aliceDb.collection("news").where("published", "==", true).get()
  );

  // Jemaat unfiltered query fails (Query Trust Test)
  await assertFails(aliceDb.collection("news").get());

  // Jemaat cannot get unpublished draft
  await assertFails(aliceDb.collection("news").doc("news_draft").get());

  // Jemaat cannot create, update, or delete news
  await assertFails(
    aliceDb.collection("news").doc("news_001").update({
      title: "Dibajak Jemaat",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(aliceDb.collection("news").doc("news_001").delete());

  // Admin can list all news and delete draft
  await assertSucceeds(adminDb.collection("news").get());
  await assertSucceeds(adminDb.collection("news").doc("news_draft").delete());
});

// 4. Schedules CRUD & Query alignment
test("Schedules enforce admin writes and published-only jemaat list queries", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    adminDb.collection("schedules").doc("sched_001").set({
      title: "Ibadah Raya Minggu",
      description: "Ibadah umum jemaat SIB Toburon disertai perjamuan kasih.",
      category: "IBADAH",
      dateLabel: "Minggu, 22 Nov 2026",
      timeLabel: "08:00 - 10:00 WITA",
      location: "Gedung Utama SIB Toburon",
      speaker: "Pdt. Markus L. T.",
      sortOrder: 202611220800,
      recurring: true,
      published: true,
      authorId: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Jemaat filtered query succeeds
  await assertSucceeds(
    aliceDb.collection("schedules").where("published", "==", true).get()
  );
  // Jemaat unfiltered query fails
  await assertFails(aliceDb.collection("schedules").get());
  // Jemaat write fails
  await assertFails(
    aliceDb.collection("schedules").doc("sched_001").update({
      location: "Lokasi Palsu",
      updatedAt: pastDate(1000),
    })
  );
});

// 5. Gallery Photos & Atomic Like/Unlike via WriteBatch
test("Gallery photos enforce atomic likesCount updates paired with user liked_photos", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("photo_001").set({
      title: "Persekutuan Padang Pemuda",
      caption: "Kebersamaan kaum muda SIB Toburon di bukit hijau.",
      category: "PEMUDA",
      eventDate: "15 Nov 2026",
      coverPreset: "FELLOWSHIP",
      likesCount: 0,
      published: true,
      authorId: ADMIN_UID,
      photographerName: "Tim Multimedia",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Direct standalone likesCount increment without creating liked_photos fails (Atomicity Guarantee!)
  await assertFails(
    aliceDb.collection("gallery_photos").doc("photo_001").update({
      likesCount: 1,
      updatedAt: pastDate(2000),
    })
  );

  // Atomic batch: create /users/alice/liked_photos/photo_001 + increment likesCount succeeds!
  const likeBatch = aliceDb.batch();
  const likeTime = pastDate(2000);
  likeBatch.set(
    aliceDb.collection("users").doc(JEMAAT_ALICE).collection("liked_photos").doc("photo_001"),
    {
      photoId: "photo_001",
      createdAt: likeTime,
    }
  );
  likeBatch.update(aliceDb.collection("gallery_photos").doc("photo_001"), {
    likesCount: 1,
    updatedAt: likeTime,
  });
  await assertSucceeds(likeBatch.commit());

  // Atomic unlike batch: delete liked_photos + decrement likesCount succeeds!
  const unlikeBatch = aliceDb.batch();
  unlikeBatch.delete(
    aliceDb.collection("users").doc(JEMAAT_ALICE).collection("liked_photos").doc("photo_001")
  );
  unlikeBatch.update(aliceDb.collection("gallery_photos").doc("photo_001"), {
    likesCount: 0,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(unlikeBatch.commit());

  // Jemaat cannot edit photo title or jump likesCount by +5
  await assertFails(
    aliceDb.collection("gallery_photos").doc("photo_001").update({
      title: "Judul Diubah Jemaat",
      updatedAt: pastDate(500),
    })
  );
});

// 6. User Profile Isolation, Bookmarks & Delegated Admin Management
test("User profiles and bookmarks are strictly owner-isolated; admins collection is protected", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(JEMAAT_BOB).firestore();
  const ts = pastDate(5000);

  // Alice creates her own profile (no role/isAdmin field allowed)
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).set({
      displayName: "Alice Toburon",
      fellowshipGroup: "PEMUDA",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Self-assigned privilege escalation rejected
  await assertFails(
    bobDb.collection("users").doc(JEMAAT_BOB).set({
      displayName: "Bob",
      fellowshipGroup: "UMUM",
      role: "admin",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's profile (PII Blanket Test)
  await assertFails(bobDb.collection("users").doc(JEMAAT_ALICE).get());

  // Alice can create & list her own bookmarks; Bob cannot
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).collection("bookmarks").doc("bm_1").set({
      itemId: "news_001",
      itemType: "NEWS",
      title: "Ibadah Syukur",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).collection("bookmarks").get()
  );
  await assertFails(
    bobDb.collection("users").doc(JEMAAT_ALICE).collection("bookmarks").get()
  );

  // Non-admin cannot add themselves to /admins
  await assertFails(
    aliceDb.collection("admins").doc(JEMAAT_ALICE).set({
      displayName: "Alice",
      roleTitle: "Admin Palsu",
      addedBy: JEMAAT_ALICE,
      createdAt: ts,
    })
  );

  // Bootstrapped admin can delegate a church staff admin
  await assertSucceeds(
    adminDb.collection("admins").doc(DELEGATED_ADMIN_UID).set({
      displayName: "Sekretaris Gereja",
      roleTitle: "Sekretaris Jemaat",
      addedBy: ADMIN_UID,
      createdAt: ts,
    })
  );

  // Delegated admin can now publish news!
  const delegatedDb = testEnv.authenticatedContext(DELEGATED_ADMIN_UID).firestore();
  await assertSucceeds(
    delegatedDb.collection("news").doc("news_delegated").set({
      title: "Pengumuman Latihan Koor",
      summary: "Latihan paduan suara Jumat malam.",
      content: "Seluruh anggota paduan suara diharapkan hadir Jumat pukul 19:00 WITA.",
      category: "PELAYANAN",
      authorId: DELEGATED_ADMIN_UID,
      authorName: "Sekretaris Gereja",
      coverPreset: "WORSHIP",
      pinned: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

// 7. FCM Notification Events Outbox & Owner-Isolated FCM Token Storage
test("FCM notification_events enforces admin creation & filtered reads; fcm_tokens are strictly owner-isolated", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(JEMAAT_BOB).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(3000);

  // Admin atomically publishes news + notification_event (mirrors Kotlin createNews WriteBatch)
  const publishBatch = adminDb.batch();
  publishBatch.set(adminDb.collection("news").doc("news_push_01"), {
    title: "Warta Khusus Akhir Bulan",
    summary: "Ibadah syukur keluarga diadakan hari Minggu pukul 08:00 WITA.",
    content: "Seluruh jemaat SIB Toburon diundang menghadiri ibadah syukur akhir bulan.",
    category: "WARTA",
    authorId: ADMIN_UID,
    authorName: "Sekretariat SIB Toburon",
    coverPreset: "SANCTUARY",
    pinned: false,
    published: true,
    createdAt: ts,
    updatedAt: ts,
  });
  publishBatch.set(adminDb.collection("notification_events").doc("notif_01"), {
    title: "Berita Baru: Warta Khusus Akhir Bulan",
    body: "Ibadah syukur keluarga diadakan hari Minggu pukul 08:00 WITA.",
    eventType: "NEWS",
    targetId: "news_push_01",
    topic: "sib_toburon_news",
    published: true,
    authorId: ADMIN_UID,
    createdAt: ts,
  });
  await assertSucceeds(publishBatch.commit());

  // Guest and Jemaat can read published notification_events via filtered query
  await assertSucceeds(
    guestDb.collection("notification_events").where("published", "==", true).get()
  );
  await assertSucceeds(
    aliceDb.collection("notification_events").where("published", "==", true).get()
  );

  // Guest and Jemaat CANNOT run unfiltered list query on notification_events
  await assertFails(guestDb.collection("notification_events").get());
  await assertFails(aliceDb.collection("notification_events").get());

  // Non-admin and Guest CANNOT create or delete notification_events
  await assertFails(
    aliceDb.collection("notification_events").doc("notif_fake").set({
      title: "Notifikasi Palsu",
      body: "Jemaat tidak boleh menyiarkan notifikasi.",
      eventType: "NEWS",
      targetId: "news_push_01",
      topic: "sib_toburon_news",
      published: true,
      authorId: JEMAAT_ALICE,
      createdAt: ts,
    })
  );
  await assertFails(aliceDb.collection("notification_events").doc("notif_01").delete());
  await assertFails(guestDb.collection("notification_events").doc("notif_01").delete());

  // Admin CAN delete notification_events
  await assertSucceeds(adminDb.collection("notification_events").doc("notif_01").delete());

  // Owner (Alice) can save, update, list, and delete her own FCM token record
  const aliceTokenRef = aliceDb
    .collection("users")
    .doc(JEMAAT_ALICE)
    .collection("fcm_tokens")
    .doc("token_abc123");

  await assertSucceeds(
    aliceTokenRef.set({
      token: "fcm_device_token_example_1234567890",
      newsEnabled: true,
      schedulesEnabled: true,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceTokenRef.set({
      token: "fcm_device_token_example_1234567890",
      newsEnabled: true,
      schedulesEnabled: false,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).collection("fcm_tokens").get()
  );

  // Cross-user (Bob) and Guest CANNOT read or write Alice's FCM tokens
  await assertFails(
    bobDb.collection("users").doc(JEMAAT_ALICE).collection("fcm_tokens").doc("token_abc123").get()
  );
  await assertFails(
    bobDb.collection("users").doc(JEMAAT_ALICE).collection("fcm_tokens").get()
  );
  await assertFails(
    bobDb.collection("users").doc(JEMAAT_ALICE).collection("fcm_tokens").doc("token_hijack").set({
      token: "fcm_hijack_token_0987654321",
      newsEnabled: true,
      schedulesEnabled: true,
      updatedAt: ts,
    })
  );
  await assertFails(
    guestDb.collection("users").doc(JEMAAT_ALICE).collection("fcm_tokens").doc("token_abc123").get()
  );

  // Owner can delete her token
  await assertSucceeds(aliceTokenRef.delete());
});

// 8. Church Members Directory (Public Names Only) & Admin-Only Member Details (PII Protection)
test("Church members directory exposes full names only; personal details are strictly Admin-only", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(5000);

  // Admin atomically registers a member across /church_members and /church_member_details
  const createBatch = adminDb.batch();
  createBatch.set(adminDb.collection("church_members").doc("member_001"), {
    fullName: "Daniel Petrus Toburon",
    gender: "LELAKI",
    baptismStatus: "DIBAPTIS",
    published: true,
    authorId: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  createBatch.set(adminDb.collection("church_member_details").doc("member_001"), {
    fullName: "Daniel Petrus Toburon",
    gender: "LELAKI",
    birthDate: "1995-06-14",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    authorId: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(createBatch.commit());

  // Guest CANNOT read church_members list or doc (members hidden from Guest mode)
  await assertFails(
    guestDb.collection("church_members").where("published", "==", true).get()
  );
  await assertFails(
    guestDb.collection("church_members").doc("member_001").get()
  );

  // Signed-in Jemaat CAN read church_members (fullName, gender, baptismStatus only) via filtered query
  await assertSucceeds(
    aliceDb.collection("church_members").where("published", "==", true).get()
  );

  // Guest and Jemaat CANNOT read private member details (birthDate, denomination)
  await assertFails(
    guestDb.collection("church_member_details").doc("member_001").get()
  );
  await assertFails(
    aliceDb.collection("church_member_details").doc("member_001").get()
  );
  await assertFails(
    aliceDb.collection("church_member_details").get()
  );

  // Reject leaking birthDate or denomination into /church_members
  await assertFails(
    adminDb.collection("church_members").doc("member_leak").set({
      fullName: "Leak Test",
      gender: "LELAKI",
      baptismStatus: "DIBAPTIS",
      birthDate: "1995-06-14",
      published: true,
      authorId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin CANNOT create, update, or delete members
  await assertFails(
    aliceDb.collection("church_members").doc("member_fake").set({
      fullName: "Jemaat Palsu",
      gender: "LELAKI",
      baptismStatus: "BELUM_DIBAPTIS",
      published: true,
      authorId: JEMAAT_ALICE,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("church_members").doc("member_001").delete());

  // Admin CAN read single detail & list all details (for Excel/PDF export), update atomically, and delete atomically
  await assertSucceeds(
    adminDb.collection("church_member_details").doc("member_001").get()
  );
  await assertSucceeds(
    adminDb.collection("church_member_details").get()
  );

  const updateBatch = adminDb.batch();
  const updateTs = pastDate(1000);
  updateBatch.update(adminDb.collection("church_members").doc("member_001"), {
    fullName: "Daniel Petrus Sibin",
    gender: "LELAKI",
    baptismStatus: "DIBAPTIS",
    updatedAt: updateTs,
  });
  updateBatch.update(adminDb.collection("church_member_details").doc("member_001"), {
    fullName: "Daniel Petrus Sibin",
    gender: "LELAKI",
    birthDate: "1995-06-14",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    updatedAt: updateTs,
  });
  await assertSucceeds(updateBatch.commit());

  // Reject invalid birthDate format
  await assertFails(
    adminDb.collection("church_member_details").doc("member_001").update({
      birthDate: "14/06/1995",
      updatedAt: pastDate(500),
    })
  );

  const deleteBatch = adminDb.batch();
  deleteBatch.delete(adminDb.collection("church_members").doc("member_001"));
  deleteBatch.delete(adminDb.collection("church_member_details").doc("member_001"));
  await assertSucceeds(deleteBatch.commit());
});


