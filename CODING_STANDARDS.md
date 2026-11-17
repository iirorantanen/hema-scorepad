# Coding standards

Read at review. Every rule here is a judgement call; a rule a script can decide belongs in an automated check instead. The rules come from the PRD in issue #1 (Modules, Testing Decisions).

## The bout module owns the rules

Rules for scoring, timing, saving the bout and giving signals live in the `bout` module, behind its Scoreboard. Reading stored text, the defaults and the lowest values are rules too. Code outside the module decides nothing about the bout: the Screen draws the bout the Scoreboard holds and turns taps into referee actions, the activity forwards them with elapsed time and leaving the foreground, and an adapter only moves text to storage or a signal to vibration.

## Tests state referee-visible behaviour

A test gives referee actions and elapsed time, plus leaving the foreground or a next launch where the rule is about one, then asserts on what a referee could observe: the score, the point log, the bout timer reading, whether the bout timer is running, and the signals given. Assertions go through the public bout state. Each rule that reads the score, the point log or the bout timer has a test where undo follows the action that triggers it.

## Stored text is read only when a release wrote it

Code that reads text an earlier build stored names the release tag that wrote that text, in a comment or in the commit message. `git tag --contains <commit>`, run on the commit that introduced the earlier form, decides whether a release wrote it. Text only an unreleased build wrote is not read.

## Domain concepts carry their glossary name

Code, tests, docs and commit messages name a domain concept with its term from `GLOSSARY.md`. A concept the glossary lacks is raised as a glossary gap. A glossary entry and the code it names agree when the entry lands: the entry takes the name the code uses, or the code is renamed in the same change.

## The README describes what the app does now

A change that adds a feature or alters behaviour a referee can see updates `README.md` in the same change. The "What it does" list covers every shipped feature, and the Install and Development sections match the current steps.
