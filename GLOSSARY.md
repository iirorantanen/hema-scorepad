# HEMA Sparring Scoreboard

A single-bout scoreboard for plain HEMA sparring, operated by a referee on the sideline. It is not a tournament system.

## Language

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
The time the bout timer counts down from, chosen by the referee before the bout.
_Avoid_: Duration, time limit

**Point cap**:
The score at which a bout ends, chosen by the referee before the bout. No fencer can be awarded a point above it. After one fencer reaches it, the other can still be awarded points up to it, and a bout where both reach it is a tie.
_Avoid_: Target score, max points, first-to

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
Taking back the most recent point, as if it had never been awarded. It can be repeated back to 0–0, cannot be reversed, and does not change the bout timer.
_Avoid_: Revert, correction, remove point

**Signal**:
Something the scoreboard tells the referee without them looking at the display, such as which fencer was just awarded a point.
_Avoid_: Alert, notification, haptic

**Bout timer**:
A countdown from the bout length that keeps running while points are awarded, until the referee pauses it.
_Avoid_: Clock, stopwatch

**Swap sides**:
Exchanging which side of the display Red and Blue appear on, without changing their scores.
_Avoid_: Mirror, flip
