# My Reading List — Android (Kotlin)

A personal reading-list app with Firebase email/password authentication, ISBN-13 barcode scanning, Open Library search, book covers, cloud-synced books, and reading-status tracking.

## Features
- Firebase Authentication: create an account, sign in, sign out.
- Google Code Scanner using Google Play services; scanner is configured for EAN-13 only.
- ISBN lookup through `https://openlibrary.org/isbn/{isbn}.json`.
- Title search through `https://openlibrary.org/search.json?q={title}`.
- Cover images from `https://covers.openlibrary.org/b/isbn/{isbn}-M.jpg` (search results use Open Library cover IDs where available).
- Per-user Firestore storage; statuses: Want to Read, Reading, Finished.
- Filter the list by reading status; update status or remove a book.

## Requirements
- Android Studio (current stable recommended).
- JDK 17.
- Android SDK Platform 35 and Build Tools installed.
- An Android device or emulator with Google Play services for barcode scanning.
- A Firebase project (free Spark plan is sufficient for basic development).

## Firebase setup (required before sign-in works)
1. Open https://console.firebase.google.com/ and create a Firebase project.
2. Add an Android app with package name exactly: `com.example.readinglist`.
3. Download `google-services.json`.
4. Copy it into this project's `app/` directory (same level as `app/build.gradle.kts`).
5. In Firebase Console → Authentication → Sign-in method, enable **Email/Password**.
6. In Firebase Console → Firestore Database, create a database. For a quick development setup choose a region close to you.
7. Publish these Firestore rules (users can only read/write their own books):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/books/{bookId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

8. In Android Studio, click **Sync Project with Gradle Files**. The Google Services Gradle plugin is automatically applied when `app/google-services.json` exists.
9. Run the app. Create an account with an email and password of at least six characters.

> The project intentionally does not contain `google-services.json`: it contains private project identifiers and must be downloaded from your own Firebase project. Without it, the app builds but shows a setup message instead of attempting authentication.

## Run the app
1. Extract the ZIP.
2. Android Studio → **Open** → select the `ReadingListApp` folder.
3. Allow Gradle sync and install any prompted SDK components.
4. Complete Firebase setup above.
5. Connect an Android phone with USB debugging enabled, or start an emulator image that includes Google Play.
6. Press **Run**.

## Using the app
- **Search**: enter a title, tap Search, and select a result.
- **Scan ISBN**: scan the book's EAN-13 barcode. Only 13-digit ISBNs beginning with 978 or 979 are accepted.
- **Add ISBN**: manually enter an ISBN-13 if scanning is unavailable.
- **Reading status**: choose Want to Read, Reading, or Finished from each book's dropdown.
- **Remove**: tap Remove on a book card.
- Books are stored under the signed-in Firebase user's Firestore path.

## Notes / limitations
- Open Library is a third-party public catalogue. Some ISBNs may have incomplete metadata, author data, or no cover.
- Google Code Scanner requires Google Play services. The first scan may download its scanner module; internet access is required.
- This is a starter coursework project, not a production-hardened release. Add input validation, accessibility polish, pagination/caching, automated tests, and Firestore offline handling before production use.
- Do not commit your real `google-services.json` to a public repository.
