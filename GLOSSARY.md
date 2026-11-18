# HEMA Sparring Scoreboard

A single-bout scoreboard for plain HEMA sparring, operated by a referee on the sideline. It is not a tournament system.

## Language

**Scoreboard**:
What the referee operates: it keeps the bout, also between app launches, remembers the settings and gives signals. The Android app and the browser version are its two versions. "App" stays the word for the Android app as the thing installed, launched and sent to the background.
_Avoid_: App as a name for what the referee operates, session

**Bout**:
One sparring match between two fencers, with its own score and bout timer. It ends when a fencer reaches the point cap or when the last exchange finishes, whichever comes first.
_Avoid_: Match, fight, round

**Exchange**:
One continuous engagement between the fencers, from the referee's start call to the halt. The scoreboard records the points an exchange produces, not the exchange itself.
_Avoid_: Pass, phrase, rally

**Last exchange**:
The final exchange of a bout, called by the referee when ten seconds remain on the bout timer. It is fought to its finish, whether that comes before or after the timer reaches zero.
_Avoid_: Final pass, overtime, sudden death

**Bout length**:
The time the bout timer counts down from, chosen by the referee before the bout. It is at least 30 seconds. With the bout timer off, there is no countdown and no last exchange is signalled.
_Avoid_: Duration, time limit

**Point cap**:
The score at which a bout ends, chosen by the referee before the bout. No fencer can be awarded a point above it. After one fencer reaches it, the other can still be awarded points up to it, and a bout where both reach it is a tie. It is at least 1. With the point cap switched off, scores are not limited.
_Avoid_: Target score, max points, first-to

**Settings**:
The bout length and the point cap as the referee has chosen them. Each can be switched off on its own and keeps its value while off. They are remembered between bouts and between app launches. A change applies to the current bout while no points stand and the bout timer has not run, and from the next new bout otherwise.
_Avoid_: Preferences, options, ruleset, preset

**Fencer**:
One of the two people sparring in a bout, identified only as Red or Blue.
_Avoid_: Player, fighter, left/right fencer

**Red / Blue**:
The two fencer identities. A fencer keeps their colour for the whole bout, whichever side of the display they appear on.
_Avoid_: Left/right, fencer 1/fencer 2

**Referee**:
The person operating the scoreboard from the sideline while watching the fencers.
_Avoid_: User, judge, scorekeeper

**Point**:
One unit of score the referee awards directly to a fencer. The scoreboard does not know why the point was given.
_Avoid_: Hit, touch

**Point log**:
The list of points awarded in a bout that still stand, each with the fencer, the bout timer reading when it was awarded and the score after it. An undone point is not in it, so it always matches the score.
_Avoid_: History, event log, score history

**Undo**:
Taking back the most recent point, as if it had never been awarded. It can be repeated back to 0–0, cannot be reversed, and does not change the bout timer, except that a settings change waiting for the next new bout applies once undo leaves no points standing in a bout whose bout timer has not run.
_Avoid_: Revert, correction, remove point

**Signal**:
Something the scoreboard gives to tell the referee what happened without them looking at the display. There are four: a Red point, a Blue point, the last exchange, and the end of the bout when a fencer reaches the point cap.
_Avoid_: Alert, notification, haptic

**Bout timer**:
A countdown from the bout length that keeps running while points are awarded, until the referee pauses it or the scoreboard leaves the foreground. A bout brought back at the next app launch has it paused.
_Avoid_: Clock, stopwatch

**Half**:
The part of the display that belongs to one fencer. It shows that fencer's score in their colour, and the referee awards them a point by tapping anywhere on it. A half is a fencer's and a side is a place: the Red half is on the left side or the right side of the display, and swap sides moves it to the other.
_Avoid_: Side as a name for a fencer's half, panel, zone, column

**Side arrangement**:
Which side of the display Red and Blue each appear on.
_Avoid_: Layout, orientation

**Swap sides**:
Exchanging which side of the display Red and Blue appear on, without changing their scores.
_Avoid_: Mirror, flip

**New bout**:
Clearing the score, the point log and the bout timer once the referee confirms, so the next bout can start. The side arrangement and the settings are kept.
_Avoid_: Reset, restart, clear
