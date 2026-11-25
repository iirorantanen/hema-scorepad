// The service worker of the browser version. It keeps every file of the site, so the scoreboard opens with no network connection.
// The build puts one line above this one (browser/build.gradle.kts): FILES, the files of the site, and VERSION, a hash of their contents.
//
// A change to any file gives another VERSION and so another sw.js. The browser fetches sw.js when the scoreboard is opened,
// and a changed one fills a cache of its own in the background and then waits. It takes over once no page of the version
// before it is left open, so a new version shows the next time the scoreboard is opened.
// skipWaiting and clients.claim are left out: they would swap the files under an open page, in the middle of a bout.

// Every project site of the owner shares the origin, and so its caches: the name carries a prefix of its own (ADR 0004).
const PREFIX = "hema-scorepad.";
const CACHE = PREFIX + VERSION;

self.addEventListener("install", (event) => {
  // "reload" goes past the browser's HTTP cache, which could put a file of the version before into the new cache.
  const files = FILES.map((file) => new Request(file, { cache: "reload" }));
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(files)));
});

self.addEventListener("activate", (event) => {
  // No page of an earlier version is open any more, so the caches of the earlier versions go.
  event.waitUntil(
    caches.keys().then((names) =>
      Promise.all(names.filter((name) => name.startsWith(PREFIX) && name !== CACHE).map((name) => caches.delete(name))),
    ),
  );
});

self.addEventListener("fetch", (event) => {
  // The cache of this version answers, whatever query string the address carries.
  // The network answers for anything else, and for every file once the browser has emptied the cache.
  event.respondWith(
    caches.match(event.request, { cacheName: CACHE, ignoreSearch: true }).then((cached) => cached || fetch(event.request)),
  );
});
