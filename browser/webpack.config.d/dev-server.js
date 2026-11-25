// The Kotlin plugin's dev server also serves the whole repository, to every machine on the network, and opens a browser window.
// The serve command (README, Development) serves the scoreboard only and prints its address.
if (config.devServer) {
    config.devServer.open = false;
    config.devServer.static = [require("path").resolve(__dirname, "kotlin")];
}
