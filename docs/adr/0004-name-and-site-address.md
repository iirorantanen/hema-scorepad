# The scoreboard is named HEMA Scorepad and served at `iirorantanen.github.io/hema-scorepad`

The scoreboard's name is HEMA Scorepad. The browser version is the GitHub Pages project site of the repository `hema-scorepad` under the owner's account, at `https://iirorantanen.github.io/hema-scorepad/`, with no custom domain. GitHub Pages does not redirect after a repository rename, so the repository carries the name before the first release of the browser version, and the address stays once referees have added the scoreboard to their home screens.

"Scoreboard" stays the word for what the referee operates (`GLOSSARY.md`). HEMA Scorepad is its proper name.

## Considered Options

Name:

- **HEMA Scoreboard, the working title**: matches the application ID, but it is close to HEMA Scorecard, a tournament system, and a web search for it returns that.
- **HEMA Sparring Scoreboard**: sets the scoreboard apart from tournament software, but it is long and needs a second, short form under the icon.

Site address:

- **A custom domain**: survives a change of host or repository, but it costs a yearly fee, and a lapsed domain breaks every installed copy and can be registered by someone else.
- **A GitHub organisation named after the project**: not tied to the owner's username, but the repository moves out of the owner's account.

## Consequences

- The application ID `io.github.iirorantanen.hemascoreboard` ([ADR 0003](0003-minimum-android-version-and-application-id.md)), the Kotlin package, the release key alias and the name in the signing certificate ([ADR 0001](0001-dedicated-release-signing-key.md)) keep the working title. A new application ID or a new key makes the next release a different app.
- The address breaks when the repository is renamed or transferred, or when the owner's GitHub username changes.
- Every project site of the owner shares the origin `iirorantanen.github.io`. The browser version shares that origin's local storage with them, so its storage keys carry a prefix of their own.
