# The browser version shares the bout rules and has its own HTML UI

The scoreboard gets a browser version next to the Android app, mainly for phones that cannot install the APK. The bout rules and their tests stay in one Kotlin module, compiled for both the JVM and JavaScript, and the browser screen is written separately in Kotlin/JS against plain HTML and CSS. The screen is therefore written twice, and every feature a referee can see lands in two UIs.

## Considered Options

- **One Compose UI for both, on Wasm**: no second screen, but Compose for web is Beta, the first load is several megabytes in a sports hall with poor signal, and it draws to a canvas with only initial screen-reader support.
- **TypeScript rewrite**: the simplest browser toolchain, but two copies of the rules and their tests that can drift apart.

## Consequences

- The browser version gives a signal only where the browser can vibrate. On an iPhone there is no signal, and no sound replaces it.
