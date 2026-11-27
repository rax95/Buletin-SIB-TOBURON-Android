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

  // Admin CAN toggle featuredInNews to highlight important schedule on Berita main menu
  await assertSucceeds(
    adminDb.collection("schedules").doc("sched_001").update({
      featuredInNews: true,
      updatedAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    adminDb.collection("schedules").doc("sched_001").update({
      featuredInNews: false,
      updatedAt: pastDate(1200),
    })
  );

  // Jemaat filtered query succeeds
  await assertSucceeds(
    aliceDb.collection("schedules").where("published", "==", true).get()
  );
  // Jemaat unfiltered query fails
  await assertFails(aliceDb.collection("schedules").get());
  // Jemaat write fails (including trying to toggle featuredInNews)
  await assertFails(
    aliceDb.collection("schedules").doc("sched_001").update({
      featuredInNews: true,
      updatedAt: pastDate(1000),
    })
  );
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

  // Admin can upload a multi-photo album (1 primary + up to 7 extraImagesBase64)
  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("photo_multi_01").set({
      title: "Album Retret Belia",
      caption: "Kumpulan gambar kebersamaan belia SIB Toburon.",
      category: "PEMUDA",
      eventDate: "20 Nov 2026",
      coverPreset: "CUSTOM",
      imageBase64: "dGVzdF9pbWFnZV8x",
      extraImagesBase64: ["dGVzdF9pbWFnZV8y", "dGVzdF9pbWFnZV8z", "dGVzdF9pbWFnZV80"],
      likesCount: 0,
      published: true,
      authorId: ADMIN_UID,
      photographerName: "Multimedia",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin can update extraImagesBase64 on an existing gallery album
  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("photo_multi_01").update({
      extraImagesBase64: ["dGVzdF9pbWFnZV8y"],
      updatedAt: pastDate(400),
    })
  );

  // Reject exceeding 7 extraImagesBase64 items (> 8 total photos per upload)
  await assertFails(
    adminDb.collection("gallery_photos").doc("photo_multi_too_many").set({
      title: "Album Terlalu Banyak",
      caption: "Melebihi had 8 gambar.",
      category: "UMUM",
      eventDate: "20 Nov 2026",
      coverPreset: "CUSTOM",
      imageBase64: "img0",
      extraImagesBase64: ["1", "2", "3", "4", "5", "6", "7", "8"],
      likesCount: 0,
      published: true,
      authorId: ADMIN_UID,
      photographerName: "Multimedia",
      createdAt: created,
      updatedAt: created,
    })
  );
});

// 6. User Profile Isolation, Bookmarks & Delegated Admin Management
test("User profiles and bookmarks are strictly owner-isolated; admins collection is protected", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(JEMAAT_BOB).firestore();
  const ts = pastDate(5000);

  // Alice creates her own profile (with Google email & lastLoginAt; no role/isAdmin field allowed)
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).set({
      displayName: "Alice Toburon",
      email: "alice@example.com",
      fellowshipGroup: "PEMUDA",
      createdAt: ts,
      updatedAt: ts,
      lastLoginAt: ts,
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

  // Bob and Guest cannot read Alice's profile or list /users (PII Blanket Test)
  const guestDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(bobDb.collection("users").doc(JEMAAT_ALICE).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(guestDb.collection("users").get());

  // Alice can update and clear her own profile photoBase64; oversized photoBase64 and cross-user writes fail
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).update({
      photoBase64: "dGVzdF9hdmF0YXJfYmFzZTY0",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(JEMAAT_ALICE).update({
      photoBase64: null,
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(JEMAAT_ALICE).update({
      photoBase64: "a".repeat(160001),
      updatedAt: pastDate(1200),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(JEMAAT_ALICE).update({
      photoBase64: "aGlqYWNr",
      updatedAt: pastDate(1000),
    })
  );

  // Admin CAN list signed-in Google users and inspect user profiles
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("users").doc(JEMAAT_ALICE).get());

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

  // Admin atomically registers a member (with optional passportPhotoBase64) across /church_members and /church_member_details
  const createBatch = adminDb.batch();
  createBatch.set(adminDb.collection("church_members").doc("member_001"), {
    fullName: "Daniel Petrus Toburon",
    gender: "LELAKI",
    baptismStatus: "DIBAPTIS",
    passportPhotoBase64: "dGVzdF9wYXNzcG9ydF9waG90bw==",
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
    maritalStatus: "BUJANG",
    passportPhotoBase64: "dGVzdF9wYXNzcG9ydF9waG90bw==",
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

  // Signed-in Jemaat CAN read church_members (fullName, gender, baptismStatus, optional passportPhotoBase64) via filtered query
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

  // Admin CAN read single detail & list all details (for Excel/PDF export), update/clear passport photo atomically, and delete atomically
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
    passportPhotoBase64: null,
    updatedAt: updateTs,
  });
  updateBatch.update(adminDb.collection("church_member_details").doc("member_001"), {
    fullName: "Daniel Petrus Sibin",
    gender: "LELAKI",
    birthDate: "1995-06-14",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    maritalStatus: "BERKAHWIN",
    passportPhotoBase64: null,
    updatedAt: updateTs,
  });
  await assertSucceeds(updateBatch.commit());

  // Reject oversized passportPhotoBase64 (> 160000 chars) on both collections
  await assertFails(
    adminDb.collection("church_members").doc("member_001").update({
      passportPhotoBase64: "p".repeat(160001),
      updatedAt: pastDate(600),
    })
  );
  await assertFails(
    adminDb.collection("church_member_details").doc("member_001").update({
      passportPhotoBase64: "p".repeat(160001),
      updatedAt: pastDate(600),
    })
  );

  // Reject invalid birthDate format and invalid maritalStatus enum
  await assertFails(
    adminDb.collection("church_member_details").doc("member_001").update({
      birthDate: "14/06/1995",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    adminDb.collection("church_member_details").doc("member_001").update({
      maritalStatus: "INVALID_STATUS",
      updatedAt: pastDate(400),
    })
  );

  // Admin links member_001 to Alice's verified Google email with MyKad, phoneNumber, spouseName, and SIB membership number
  await assertSucceeds(
    adminDb.collection("church_member_details").doc("member_001").update({
      memberEmail: "alice@example.com",
      myKadNumber: "950614-12-5431",
      phoneNumber: "012-3456789",
      spouseName: "Maria Toburon",
      sibMembershipNumber: "SIB-TBR-0042",
      updatedAt: pastDate(300),
    })
  );
  await assertFails(
    adminDb.collection("church_member_details").doc("member_001").update({
      phoneNumber: "0".repeat(26),
      updatedAt: pastDate(290),
    })
  );

  // Admin can register an unbaptized member with empty denomination and no SIB membership number
  await assertSucceeds(
    adminDb.collection("church_member_details").doc("member_002").set({
      fullName: "Hannah Lukas",
      gender: "PEREMPUAN",
      birthDate: "2004-03-11",
      baptismStatus: "BELUM_DIBAPTIS",
      denomination: "",
      maritalStatus: "BUJANG",
      myKadNumber: "040311-12-0912",
      authorId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject baptized member with empty denomination
  await assertFails(
    adminDb.collection("church_member_details").doc("member_002").update({
      baptismStatus: "DIBAPTIS",
      denomination: "",
      updatedAt: pastDate(250),
    })
  );

  const aliceVerifiedDb = testEnv.authenticatedContext(JEMAAT_ALICE, {
    email: "Alice@Example.com",
    email_verified: true,
  }).firestore();
  const bobVerifiedDb = testEnv.authenticatedContext(JEMAAT_BOB, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  // Verified Alice CAN query and read ONLY her own linked member detail record
  await assertSucceeds(
    aliceVerifiedDb
      .collection("church_member_details")
      .where("memberEmail", "==", "alice@example.com")
      .get()
  );
  await assertSucceeds(
    aliceVerifiedDb.collection("church_member_details").doc("member_001").get()
  );

  // Verified Bob CANNOT query Alice's record, read member_001, or run an unfiltered list query
  await assertFails(
    bobVerifiedDb
      .collection("church_member_details")
      .where("memberEmail", "==", "alice@example.com")
      .get()
  );
  await assertFails(
    bobVerifiedDb.collection("church_member_details").doc("member_001").get()
  );
  await assertFails(bobVerifiedDb.collection("church_member_details").get());

  // Unverified Alice token CANNOT read member_001
  const aliceUnverifiedDb = testEnv.authenticatedContext(JEMAAT_ALICE, {
    email: "alice@example.com",
    email_verified: false,
  }).firestore();
  await assertFails(
    aliceUnverifiedDb.collection("church_member_details").doc("member_001").get()
  );

  // Verified Alice CAN update her own Admin-linked record (member_001) across both collections,
  // but CANNOT change memberEmail to hijack someone else's email
  const aliceSelfEditBatch = aliceVerifiedDb.batch();
  aliceSelfEditBatch.update(aliceVerifiedDb.collection("church_members").doc("member_001"), {
    fullName: "Alice Petrus Sibin",
    gender: "PEREMPUAN",
    baptismStatus: "DIBAPTIS",
    updatedAt: pastDate(200),
  });
  aliceSelfEditBatch.update(aliceVerifiedDb.collection("church_member_details").doc("member_001"), {
    fullName: "Alice Petrus Sibin",
    gender: "PEREMPUAN",
    myKadNumber: "950614-12-5432",
    updatedAt: pastDate(200),
  });
  await assertSucceeds(aliceSelfEditBatch.commit());

  await assertFails(
    aliceVerifiedDb.collection("church_member_details").doc("member_001").update({
      memberEmail: "bob@example.com",
      updatedAt: pastDate(150),
    })
  );

  // Verified Bob CAN self-register atomically with doc ID `member_${JEMAAT_BOB}` + admin-only MEMBER_REGISTERED notification
  const bobSelfId = `member_${JEMAAT_BOB}`;
  const bobRegTs = pastDate(180);
  const bobRegBatch = bobVerifiedDb.batch();
  bobRegBatch.set(bobVerifiedDb.collection("church_members").doc(bobSelfId), {
    fullName: "Bob Lukas Toburon",
    gender: "LELAKI",
    baptismStatus: "BELUM_DIBAPTIS",
    published: true,
    authorId: JEMAAT_BOB,
    createdAt: bobRegTs,
    updatedAt: bobRegTs,
  });
  bobRegBatch.set(bobVerifiedDb.collection("church_member_details").doc(bobSelfId), {
    fullName: "Bob Lukas Toburon",
    myKadNumber: "980210-12-1121",
    gender: "LELAKI",
    birthDate: "1998-02-10",
    baptismStatus: "BELUM_DIBAPTIS",
    denomination: "",
    maritalStatus: "BUJANG",
    memberEmail: "bob@example.com",
    authorId: JEMAAT_BOB,
    createdAt: bobRegTs,
    updatedAt: bobRegTs,
  });
  bobRegBatch.set(bobVerifiedDb.collection("notification_events").doc("notif_reg_bob"), {
    title: "Ahli Baharu Berdaftar",
    body: "Bob Lukas Toburon telah mengisi maklumat ahli SIB Toburon.",
    eventType: "MEMBER_REGISTERED",
    targetId: bobSelfId,
    topic: "sib_toburon_admin_members",
    published: false,
    authorId: JEMAAT_BOB,
    createdAt: bobRegTs,
  });
  await assertSucceeds(bobRegBatch.commit());

  // Bob CANNOT self-register with arbitrary doc ID, CANNOT publish public notification, and CANNOT delete his record
  await assertFails(
    bobVerifiedDb.collection("church_members").doc("member_arbitrary_id").set({
      fullName: "Bob Palsu",
      gender: "LELAKI",
      baptismStatus: "BELUM_DIBAPTIS",
      published: true,
      authorId: JEMAAT_BOB,
      createdAt: bobRegTs,
      updatedAt: bobRegTs,
    })
  );
  await assertFails(
    bobVerifiedDb.collection("notification_events").doc("notif_reg_bob_pub").set({
      title: "Broadcast Palsu",
      body: "Tidak boleh published true.",
      eventType: "MEMBER_REGISTERED",
      targetId: bobSelfId,
      topic: "sib_toburon_admin_members",
      published: true,
      authorId: JEMAAT_BOB,
      createdAt: bobRegTs,
    })
  );
  await assertFails(bobVerifiedDb.collection("church_member_details").doc(bobSelfId).delete());

  // Other Jemaat (Alice) & Guest CANNOT read Bob's private MEMBER_REGISTERED admin notification; Admin CAN
  await assertFails(aliceVerifiedDb.collection("notification_events").doc("notif_reg_bob").get());
  await assertFails(guestDb.collection("notification_events").doc("notif_reg_bob").get());
  await assertSucceeds(adminDb.collection("notification_events").doc("notif_reg_bob").get());
  await assertSucceeds(adminDb.collection("notification_events").get());

  const deleteBatch = adminDb.batch();
  deleteBatch.delete(adminDb.collection("church_members").doc("member_001"));
  deleteBatch.delete(adminDb.collection("church_member_details").doc("member_001"));
  deleteBatch.delete(adminDb.collection("church_member_details").doc("member_002"));
  deleteBatch.delete(adminDb.collection("church_members").doc(bobSelfId));
  deleteBatch.delete(adminDb.collection("church_member_details").doc(bobSelfId));
  deleteBatch.delete(adminDb.collection("notification_events").doc("notif_reg_bob"));
  await assertSucceeds(deleteBatch.commit());
});

// 9. Gallery Comments & Threaded Replies (/gallery_photos/{photoId}/comments/{commentId})
test("Gallery comments support signed-in comments and replies, guest read on published posts, and owner/admin deletion", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(JEMAAT_BOB).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(5000);

  // Seed published gallery photo and draft gallery photo
  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("photo_comment_pub").set({
      title: "Ibadah Pujian",
      caption: "Malam pujian dan penyembahan SIB Toburon.",
      category: "IBADAH",
      eventDate: "21 Nov 2026",
      coverPreset: "WORSHIP",
      likesCount: 0,
      published: true,
      authorId: ADMIN_UID,
      photographerName: "Multimedia",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("photo_comment_draft").set({
      title: "Draf Galeri",
      caption: "Belum diterbitkan.",
      category: "UMUM",
      eventDate: "21 Nov 2026",
      coverPreset: "FELLOWSHIP",
      likesCount: 0,
      published: false,
      authorId: ADMIN_UID,
      photographerName: "Multimedia",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Signed-in Alice posts a top-level comment
  await assertSucceeds(
    aliceDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("comment_1")
      .set({
        photoId: "photo_comment_pub",
        authorId: JEMAAT_ALICE,
        authorName: "Alice Toburon",
        text: "Puji Tuhan, gambar yang sangat indah!",
        createdAt: pastDate(4000),
      })
  );

  // Signed-in Bob posts a reply referencing parentCommentId = comment_1
  await assertSucceeds(
    bobDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("reply_1")
      .set({
        photoId: "photo_comment_pub",
        authorId: JEMAAT_BOB,
        authorName: "Bob Toburon",
        text: "Amin! Terima kasih pasukan multimedia.",
        parentCommentId: "comment_1",
        replyToAuthorName: "Alice Toburon",
        createdAt: pastDate(3000),
      })
  );

  // Guest & signed-in users CAN read comments on published photo
  await assertSucceeds(
    guestDb.collection("gallery_photos").doc("photo_comment_pub").collection("comments").get()
  );
  await assertSucceeds(
    aliceDb.collection("gallery_photos").doc("photo_comment_pub").collection("comments").get()
  );

  // Guest & non-admin CANNOT read or comment on unpublished draft photo
  await assertFails(
    guestDb.collection("gallery_photos").doc("photo_comment_draft").collection("comments").get()
  );
  await assertFails(
    aliceDb
      .collection("gallery_photos")
      .doc("photo_comment_draft")
      .collection("comments")
      .doc("draft_comment")
      .set({
        photoId: "photo_comment_draft",
        authorId: JEMAAT_ALICE,
        authorName: "Alice",
        text: "Tidak dibenarkan pada draf.",
        createdAt: pastDate(2000),
      })
  );

  // Guest CANNOT post a comment
  await assertFails(
    guestDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("guest_comment")
      .set({
        photoId: "photo_comment_pub",
        authorId: "guest",
        authorName: "Guest",
        text: "Komen tetamu",
        createdAt: pastDate(2000),
      })
  );

  // Reject spoofed authorId, non-existent parentCommentId, and oversized text (> 500 chars)
  await assertFails(
    bobDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("spoof_comment")
      .set({
        photoId: "photo_comment_pub",
        authorId: JEMAAT_ALICE,
        authorName: "Alice",
        text: "Spoofing authorId",
        createdAt: pastDate(2000),
      })
  );
  await assertFails(
    bobDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("bad_parent_reply")
      .set({
        photoId: "photo_comment_pub",
        authorId: JEMAAT_BOB,
        authorName: "Bob",
        text: "Balasan kepada komen tiada",
        parentCommentId: "missing_parent_id",
        createdAt: pastDate(2000),
      })
  );
  await assertFails(
    bobDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("too_long_comment")
      .set({
        photoId: "photo_comment_pub",
        authorId: JEMAAT_BOB,
        authorName: "Bob",
        text: "x".repeat(501),
        createdAt: pastDate(2000),
      })
  );

  // Bob CANNOT delete Alice's comment; Alice CAN delete her own comment; Admin CAN delete Bob's reply
  await assertFails(
    bobDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("comment_1")
      .delete()
  );
  await assertSucceeds(
    aliceDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("comment_1")
      .delete()
  );
  await assertSucceeds(
    adminDb
      .collection("gallery_photos")
      .doc("photo_comment_pub")
      .collection("comments")
      .doc("reply_1")
      .delete()
  );
});

// 10. News Comments & Threaded Replies (/news/{newsId}/comments/{commentId})
test("News comments support signed-in comments and replies, guest read on published news, and owner/admin deletion", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(JEMAAT_BOB).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(5000);

  // Seed published news article and unpublished draft news article
  await assertSucceeds(
    adminDb.collection("news").doc("news_comment_pub").set({
      title: "Warta Minggu Kebangkitan",
      summary: "Jadual ibadah dan kegiatan pelayanan minggu ini.",
      content: "Shalom jemaat SIB Toburon, marilah kita bersama menghadiri ibadah minggu ini.",
      category: "WARTA",
      authorId: ADMIN_UID,
      authorName: "Sekretariat",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("news").doc("news_comment_draft").set({
      title: "Draf Warta Dalaman",
      summary: "Masih dalam semakan admin.",
      content: "Draf ini belum diterbitkan untuk tatapan umum.",
      category: "PENGUMUMAN",
      authorId: ADMIN_UID,
      authorName: "Sekretariat",
      coverPreset: "WORSHIP",
      pinned: false,
      published: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Signed-in Alice posts a top-level comment on published news
  await assertSucceeds(
    aliceDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("news_c1")
      .set({
        newsId: "news_comment_pub",
        authorId: JEMAAT_ALICE,
        authorName: "Alice Toburon",
        text: "Terima kasih atas makluman ini, Tuhan memberkati!",
        createdAt: pastDate(4000),
      })
  );

  // Signed-in Bob posts a reply referencing parentCommentId = news_c1
  await assertSucceeds(
    bobDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("news_r1")
      .set({
        newsId: "news_comment_pub",
        authorId: JEMAAT_BOB,
        authorName: "Bob Toburon",
        text: "Amin, jumpa di gereja hari Ahad ini!",
        parentCommentId: "news_c1",
        replyToAuthorName: "Alice Toburon",
        createdAt: pastDate(3000),
      })
  );

  // Guest & signed-in users CAN read comments on published news
  await assertSucceeds(
    guestDb.collection("news").doc("news_comment_pub").collection("comments").get()
  );
  await assertSucceeds(
    aliceDb.collection("news").doc("news_comment_pub").collection("comments").get()
  );

  // Guest & non-admin CANNOT read or comment on unpublished draft news
  await assertFails(
    guestDb.collection("news").doc("news_comment_draft").collection("comments").get()
  );
  await assertFails(
    aliceDb
      .collection("news")
      .doc("news_comment_draft")
      .collection("comments")
      .doc("draft_c1")
      .set({
        newsId: "news_comment_draft",
        authorId: JEMAAT_ALICE,
        authorName: "Alice",
        text: "Tidak dibenarkan pada draf berita.",
        createdAt: pastDate(2000),
      })
  );

  // Guest CANNOT post a comment
  await assertFails(
    guestDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("guest_c1")
      .set({
        newsId: "news_comment_pub",
        authorId: "guest",
        authorName: "Guest",
        text: "Komen tetamu",
        createdAt: pastDate(2000),
      })
  );

  // Reject spoofed authorId, non-existent parentCommentId, and oversized text (> 500 chars)
  await assertFails(
    bobDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("spoof_c1")
      .set({
        newsId: "news_comment_pub",
        authorId: JEMAAT_ALICE,
        authorName: "Alice",
        text: "Spoofing authorId",
        createdAt: pastDate(2000),
      })
  );
  await assertFails(
    bobDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("bad_parent_r1")
      .set({
        newsId: "news_comment_pub",
        authorId: JEMAAT_BOB,
        authorName: "Bob",
        text: "Balasan kepada komen tiada",
        parentCommentId: "missing_parent_id",
        createdAt: pastDate(2000),
      })
  );
  await assertFails(
    bobDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("too_long_c1")
      .set({
        newsId: "news_comment_pub",
        authorId: JEMAAT_BOB,
        authorName: "Bob",
        text: "x".repeat(501),
        createdAt: pastDate(2000),
      })
  );

  // Bob CANNOT delete Alice's comment; Alice CAN delete her own comment; Admin CAN delete Bob's reply
  await assertFails(
    bobDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("news_c1")
      .delete()
  );
  await assertSucceeds(
    aliceDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("news_c1")
      .delete()
  );
  await assertSucceeds(
    adminDb
      .collection("news")
      .doc("news_comment_pub")
      .collection("comments")
      .doc("news_r1")
      .delete()
  );
});

// 11. App Version Config (/app_config/version): public get, Admin-only write, no list/delete, strict HTTPS URL validation
test("App version config allows public get on version doc and Admin-only validated writes", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(3000);

  // Admin CAN create and update /app_config/version
  await assertSucceeds(
    adminDb.collection("app_config").doc("version").set({
      latestVersionCode: 2,
      latestVersionName: "1.1.0",
      releaseNotes: "Pendaftaran ahli kendiri di tab Akaun, logo rasmi SIB, dan penambahbaikan galeri.",
      updateUrl: "https://play.google.com/store/apps/details?id=com.aistudio.buletinsib.kxmrtv",
      forceUpdate: false,
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );

  // Guest and Jemaat CAN get /app_config/version
  await assertSucceeds(guestDb.collection("app_config").doc("version").get());
  await assertSucceeds(aliceDb.collection("app_config").doc("version").get());

  // Nobody can list /app_config or read arbitrary config docs
  await assertFails(guestDb.collection("app_config").get());
  await assertFails(aliceDb.collection("app_config").get());
  await assertFails(adminDb.collection("app_config").get());
  await assertFails(aliceDb.collection("app_config").doc("other_secret").get());

  // Non-admin and Guest CANNOT write or delete /app_config/version
  await assertFails(
    aliceDb.collection("app_config").doc("version").set({
      latestVersionCode: 99,
      latestVersionName: "9.9.9",
      releaseNotes: "Fake update",
      updateUrl: "https://evil.example.com/app.apk",
      forceUpdate: true,
      updatedBy: JEMAAT_ALICE,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(guestDb.collection("app_config").doc("version").delete());
  await assertFails(adminDb.collection("app_config").doc("version").delete());

  // Reject non-HTTPS updateUrl, invalid versionCode, and non-version doc IDs even for Admin
  await assertFails(
    adminDb.collection("app_config").doc("version").set({
      latestVersionCode: 3,
      latestVersionName: "1.2.0",
      releaseNotes: "Insecure link test",
      updateUrl: "http://insecure.example.com/app.apk",
      forceUpdate: false,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    adminDb.collection("app_config").doc("version").set({
      latestVersionCode: 0,
      latestVersionName: "0.0.0",
      releaseNotes: "Invalid code",
      updateUrl: "",
      forceUpdate: false,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    adminDb.collection("app_config").doc("other_doc").set({
      latestVersionCode: 2,
      latestVersionName: "1.1.0",
      releaseNotes: "Wrong doc ID",
      updateUrl: "",
      forceUpdate: false,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Admin CAN update /app_config/version with empty updateUrl (announcement-only notice)
  await assertSucceeds(
    adminDb.collection("app_config").doc("version").set({
      latestVersionCode: 3,
      latestVersionName: "1.2.0",
      releaseNotes: "Kemas kini baharu tersedia.",
      updateUrl: "",
      forceUpdate: true,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(500),
    })
  );
});

// 12. Automatic Member Card Google Account Linking (User-first auto-add & Admin-first email auto-link)
test("User self-registration auto-adds to Admin members list and Admin-first registration auto-links when matching Google user signs in", async () => {
  const adminDb = adminContext().firestore();
  const delegatedAdminDb = testEnv.authenticatedContext(DELEGATED_ADMIN_UID, {
    email: "sekretaris@sibtoburon.org",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE, {
    email: "Alice.Member@gmail.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(JEMAAT_BOB, {
    email: "bob.other@gmail.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate(5000);

  // Register delegated admin
  await assertSucceeds(
    adminDb.collection("admins").doc(DELEGATED_ADMIN_UID).set({
      displayName: "Sekretaris Gereja",
      roleTitle: "Setiausaha",
      addedBy: ADMIN_UID,
      createdAt: ts,
    })
  );

  // Scenario A: User checks own non-existent member_$uid doc without permission error, then self-registers
  const aliceDocId = `member_${JEMAAT_ALICE}`;
  await assertSucceeds(
    aliceDb.collection("church_member_details").doc(aliceDocId).get()
  );

  const selfBatch = aliceDb.batch();
  selfBatch.set(aliceDb.collection("church_members").doc(aliceDocId), {
    fullName: "Alice Toburon",
    gender: "PEREMPUAN",
    baptismStatus: "DIBAPTIS",
    published: true,
    authorId: JEMAAT_ALICE,
    createdAt: ts,
    updatedAt: ts,
  });
  selfBatch.set(aliceDb.collection("church_member_details").doc(aliceDocId), {
    fullName: "Alice Toburon",
    myKadNumber: "950101-12-5678",
    gender: "PEREMPUAN",
    birthDate: "1995-01-01",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    maritalStatus: "BUJANG",
    memberEmail: "alice.member@gmail.com",
    authorId: JEMAAT_ALICE,
    createdAt: ts,
    updatedAt: ts,
  });
  selfBatch.set(aliceDb.collection("notification_events").doc("evt_alice_01"), {
    title: "Ahli Baharu Berdaftar",
    body: "Alice Toburon telah mengisi maklumat ahli SIB Toburon.",
    eventType: "MEMBER_REGISTERED",
    targetId: aliceDocId,
    topic: "sib_toburon_admin_members",
    published: false,
    authorId: JEMAAT_ALICE,
    createdAt: ts,
  });
  await assertSucceeds(selfBatch.commit());

  // Admin & Delegated Admin immediately see Alice in church_members and church_member_details
  await assertSucceeds(
    delegatedAdminDb.collection("church_members").where("published", "==", true).get()
  );
  await assertSucceeds(
    delegatedAdminDb.collection("church_member_details").where("memberEmail", "==", "alice.member@gmail.com").get()
  );

  // Alice can update her own Member Card details and it syncs to both collections
  const selfUpdateBatch = aliceDb.batch();
  selfUpdateBatch.update(aliceDb.collection("church_members").doc(aliceDocId), {
    fullName: "Alice Toburon Updated",
    gender: "PEREMPUAN",
    baptismStatus: "DIBAPTIS",
    passportPhotoBase64: null,
    updatedAt: pastDate(1000),
  });
  selfUpdateBatch.update(aliceDb.collection("church_member_details").doc(aliceDocId), {
    fullName: "Alice Toburon Updated",
    myKadNumber: "950101-12-5678",
    gender: "PEREMPUAN",
    birthDate: "1995-01-01",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    maritalStatus: "BUJANG",
    spouseName: null,
    sibMembershipNumber: "SIB-0099",
    passportPhotoBase64: null,
    memberEmail: "alice.member@gmail.com",
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(selfUpdateBatch.commit());

  // Scenario B: Admin registers Bob FIRST with his Google email before Bob ever signs up
  const adminCreatedId = "admin_pre_bob_01";
  const adminBatch = adminDb.batch();
  adminBatch.set(adminDb.collection("church_members").doc(adminCreatedId), {
    fullName: "Bob Pre-Registered",
    gender: "LELAKI",
    baptismStatus: "BELUM_DIBAPTIS",
    published: true,
    authorId: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  adminBatch.set(adminDb.collection("church_member_details").doc(adminCreatedId), {
    fullName: "Bob Pre-Registered",
    gender: "LELAKI",
    birthDate: "1990-05-12",
    baptismStatus: "BELUM_DIBAPTIS",
    denomination: "",
    maritalStatus: "BUJANG",
    memberEmail: "bob.other@gmail.com",
    authorId: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(adminBatch.commit());

  // Bob signs in with Google (matching email) -> auto-links via where("memberEmail", "==", email)
  const bobQuerySnap = await assertSucceeds(
    bobDb.collection("church_member_details").where("memberEmail", "==", "bob.other@gmail.com").get()
  );
  if (bobQuerySnap.size !== 1 || bobQuerySnap.docs[0].id !== adminCreatedId) {
    throw new Error("Expected Bob to auto-link to admin-registered member record");
  }

  // Bob can update his auto-linked admin-registered Member Card directly
  const bobUpdateBatch = bobDb.batch();
  bobUpdateBatch.update(bobDb.collection("church_members").doc(adminCreatedId), {
    fullName: "Bob Updated Himself",
    gender: "LELAKI",
    baptismStatus: "DIBAPTIS",
    passportPhotoBase64: null,
    updatedAt: pastDate(500),
  });
  bobUpdateBatch.update(bobDb.collection("church_member_details").doc(adminCreatedId), {
    fullName: "Bob Updated Himself",
    myKadNumber: "900512-12-1111",
    gender: "LELAKI",
    birthDate: "1990-05-12",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    maritalStatus: "BUJANG",
    spouseName: null,
    sibMembershipNumber: null,
    passportPhotoBase64: null,
    memberEmail: "bob.other@gmail.com",
    updatedAt: pastDate(500),
  });
  await assertSucceeds(bobUpdateBatch.commit());

  // Cross-user isolation: Alice CANNOT query or read Bob's private member details, and Bob CANNOT edit Alice's
  await assertFails(
    aliceDb.collection("church_member_details").where("memberEmail", "==", "bob.other@gmail.com").get()
  );
  await assertFails(
    aliceDb.collection("church_member_details").doc(adminCreatedId).get()
  );
  await assertFails(
    bobDb.collection("church_member_details").doc(aliceDocId).update({
      fullName: "Hacked",
      updatedAt: pastDate(100),
    })
  );
});

// 13. Dynamic News & Schedule Categories (/news_categories/{catId}, /schedule_categories/{catId})
test("Dynamic news and schedule categories allow public read, admin-only CRUD, and custom category codes in news/schedules", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(3000);

  // Admin CAN create categories in both collections
  await assertSucceeds(
    adminDb.collection("news_categories").doc("MISI_GLOBAL").set({
      code: "MISI_GLOBAL",
      labelMs: "Misi Global",
      labelEn: "Global Mission",
      sortOrder: 5,
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("schedule_categories").doc("LATIHAN_MUZIK").set({
      code: "LATIHAN_MUZIK",
      labelMs: "Latihan Muzik",
      labelEn: "Music Practice",
      sortOrder: 6,
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );

  // Guest and Jemaat CAN read & list both category collections
  await assertSucceeds(guestDb.collection("news_categories").get());
  await assertSucceeds(guestDb.collection("news_categories").doc("MISI_GLOBAL").get());
  await assertSucceeds(aliceDb.collection("schedule_categories").get());
  await assertSucceeds(aliceDb.collection("schedule_categories").doc("LATIHAN_MUZIK").get());

  // Non-admin & Guest CANNOT create, update, or delete categories
  await assertFails(
    aliceDb.collection("news_categories").doc("FAKE_CAT").set({
      code: "FAKE_CAT",
      labelMs: "Palsu",
      labelEn: "Fake",
      sortOrder: 0,
      updatedBy: JEMAAT_ALICE,
      updatedAt: ts,
    })
  );
  await assertFails(
    guestDb.collection("schedule_categories").doc("LATIHAN_MUZIK").delete()
  );
  await assertFails(
    aliceDb.collection("news_categories").doc("MISI_GLOBAL").update({
      labelMs: "Ubah",
      labelEn: "Edit",
      updatedBy: JEMAAT_ALICE,
      updatedAt: pastDate(1000),
    })
  );

  // Admin CAN update labels & delete categories, but CANNOT mutate immutable `code`
  await assertSucceeds(
    adminDb.collection("news_categories").doc("MISI_GLOBAL").update({
      labelMs: "Misi & Penginjilan",
      labelEn: "Mission & Evangelism",
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    adminDb.collection("news_categories").doc("MISI_GLOBAL").update({
      code: "CHANGED_CODE",
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(500),
    })
  );

  // Custom dynamic category codes are accepted in /news and /schedules
  await assertSucceeds(
    adminDb.collection("news").doc("news_custom_cat").set({
      title: "Laporan Misi 2026",
      summary: "Perkembangan pelayanan misi SIB Toburon.",
      content: "Laporan penuh pelayanan misi luar dan tempatan untuk makluman jemaat.",
      category: "MISI_GLOBAL",
      authorId: ADMIN_UID,
      authorName: "Admin",
      coverPreset: "SANCTUARY",
      pinned: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("schedules").doc("sched_custom_cat").set({
      title: "Latihan Pujian & Muzik",
      description: "Persiapan pasukan pujian untuk ibadah Ahad.",
      category: "LATIHAN_MUZIK",
      dateLabel: "Setiap Sabtu",
      timeLabel: "19:00 - 21:00",
      location: "Dewan Utama",
      speaker: "Pasukan Muzik",
      sortOrder: 200,
      recurring: true,
      published: true,
      authorId: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin CAN delete a category
  await assertSucceeds(
    adminDb.collection("schedule_categories").doc("LATIHAN_MUZIK").delete()
  );
});

// 14. Web QR Member Registration (/app_config/member_registration + token-gated webreg_* creation)
test("Admin manages /app_config/member_registration privately and valid QR token allows browser member registration", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(JEMAAT_ALICE, {
    email: "webuser@gmail.com",
    email_verified: true,
  }).firestore();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(3000);
  const validToken = "SibRegToken2026AbCdEf99";

  // Admin creates /app_config/member_registration
  await assertSucceeds(
    adminDb.collection("app_config").doc("member_registration").set({
      portalUrl: "https://gen-lang-client-0216024023.web.app",
      accessToken: validToken,
      enabled: true,
      updatedBy: ADMIN_UID,
      updatedAt: ts,
    })
  );

  // Admin can read /app_config/member_registration; non-admin & guest CANNOT read secret token
  await assertSucceeds(adminDb.collection("app_config").doc("member_registration").get());
  await assertFails(aliceDb.collection("app_config").doc("member_registration").get());
  await assertFails(guestDb.collection("app_config").doc("member_registration").get());

  // Browser user with wrong token or invalid doc prefix CANNOT register
  await assertFails(
    guestDb.collection("church_members").doc("webreg_bad_01").set({
      fullName: "Invalid Token User",
      gender: "LELAKI",
      baptismStatus: "BELUM_DIBAPTIS",
      published: true,
      authorId: "web_qr_reg",
      registrationToken: "wrong_token_12345678",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    guestDb.collection("church_members").doc("member_spoof_01").set({
      fullName: "Wrong Prefix User",
      gender: "LELAKI",
      baptismStatus: "BELUM_DIBAPTIS",
      published: true,
      authorId: "web_qr_reg",
      registrationToken: validToken,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Browser/iOS user with valid registration token CAN register atomically into church_members + church_member_details + notification_events
  const webId = "webreg_ios_test_01";
  const webBatch = guestDb.batch();
  webBatch.set(guestDb.collection("church_members").doc(webId), {
    fullName: "Daniel iOS Safari",
    gender: "LELAKI",
    baptismStatus: "DIBAPTIS",
    published: true,
    authorId: "web_qr_reg",
    registrationToken: validToken,
    createdAt: ts,
    updatedAt: ts,
  });
  webBatch.set(guestDb.collection("church_member_details").doc(webId), {
    fullName: "Daniel iOS Safari",
    myKadNumber: "920815-12-3456",
    phoneNumber: "+6019-8881234",
    gender: "LELAKI",
    birthDate: "1992-08-15",
    baptismStatus: "DIBAPTIS",
    denomination: "SIB",
    maritalStatus: "BERKAHWIN",
    spouseName: "Grace",
    memberEmail: "webuser@gmail.com",
    authorId: "web_qr_reg",
    registrationToken: validToken,
    createdAt: ts,
    updatedAt: ts,
  });
  webBatch.set(guestDb.collection("notification_events").doc("evt_web_01"), {
    title: "Ahli Baharu (Pautan Web)",
    body: "Daniel iOS Safari telah mendaftar melalui pautan QR.",
    eventType: "MEMBER_REGISTERED",
    targetId: webId,
    topic: "sib_toburon_admin_members",
    published: false,
    authorId: "web_qr_reg",
    registrationToken: validToken,
    createdAt: ts,
  });
  await assertSucceeds(webBatch.commit());

  // Admin can read, edit, and delete web-registered member; matching Google email auto-links
  await assertSucceeds(adminDb.collection("church_member_details").doc(webId).get());
  await assertSucceeds(
    aliceDb.collection("church_member_details").where("memberEmail", "==", "webuser@gmail.com").get()
  );
  await assertSucceeds(
    adminDb.collection("church_member_details").doc(webId).update({
      sibMembershipNumber: "SIB-2026-088",
      updatedAt: pastDate(1000),
    })
  );

  // When Admin pauses the registration link, new browser submissions are rejected
  await assertSucceeds(
    adminDb.collection("app_config").doc("member_registration").set({
      portalUrl: "https://gen-lang-client-0216024023.web.app",
      accessToken: validToken,
      enabled: false,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    guestDb.collection("church_members").doc("webreg_paused_02").set({
      fullName: "Paused Link User",
      gender: "PEREMPUAN",
      baptismStatus: "BELUM_DIBAPTIS",
      published: true,
      authorId: "web_qr_reg",
      registrationToken: validToken,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});







