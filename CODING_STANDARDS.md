# Coding standards

Read at review. Every rule here is a judgement call; a rule a script can decide belongs in an automated check instead. The rules come from the PRD in issue #1 (Modules, Testing Decisions).

## The bout module owns the rules

Scoring and timing rules live in the `bout` module. The Screen draws the bout state and turns taps into referee actions. Device effects turn signals into vibration.

## Tests state referee-visible behaviour

A test gives referee actions and elapsed time, then asserts on what a referee could observe: the score, the point log, the bout timer reading, whether the bout timer is running, and the signals given. Assertions go through the public bout state. Each rule that reads the score, the point log or the bout timer has a test where undo follows the action that triggers it.

## Domain concepts carry their glossary name

Code, tests, docs and commit messages name a domain concept with its term from `GLOSSARY.md`. A concept the glossary lacks is raised as a glossary gap. A glossary entry and the code it names agree when the entry lands: the entry takes the name the code uses, or the code is renamed in the same change.

## The README describes what the app does now

A change that adds a feature or alters behaviour a referee can see updates `README.md` in the same change. The "What it does" list covers every shipped feature, and the Install and Development sections match the current steps.
