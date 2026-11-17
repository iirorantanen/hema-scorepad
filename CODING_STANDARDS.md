# Coding standards

Read at review. Every rule here is a judgement call; a rule a script can decide belongs in an automated check instead. The rules come from the PRD in issue #1 (Modules, Testing Decisions).

## The bout module owns the rules

Scoring and timing rules live in the `bout` module. Its Scoreboard keeps the current bout, saves it with its settings to a bout store as it changes, brings it back paused at the next launch, pauses the bout timer when the scoreboard leaves the foreground, and passes each signal on as the bout gives it. The bout store holds text only: reading it, the defaults and the lowest values are rules. The Screen draws the bout state the Scoreboard holds and turns taps into referee actions. The activity forwards those referee actions and the time between frames to the Scoreboard, tells it when the scoreboard leaves the foreground, and keeps the screen awake. Device effects turn signals into vibration.

## Tests state referee-visible behaviour

A test gives referee actions and elapsed time, plus leaving the foreground or a next launch where the rule is about one, then asserts on what a referee could observe: the score, the point log, the bout timer reading, whether the bout timer is running, and the signals given. Assertions go through the public bout state. Each rule that reads the score, the point log or the bout timer has a test where undo follows the action that triggers it.

## Domain concepts carry their glossary name

Code, tests, docs and commit messages name a domain concept with its term from `GLOSSARY.md`. A concept the glossary lacks is raised as a glossary gap. A glossary entry and the code it names agree when the entry lands: the entry takes the name the code uses, or the code is renamed in the same change.

## The README describes what the app does now

A change that adds a feature or alters behaviour a referee can see updates `README.md` in the same change. The "What it does" list covers every shipped feature, and the Install and Development sections match the current steps.
