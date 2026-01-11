package v02_myVersion.States;

public enum Code {
    CANT(),        // Action can't be played
    OK(),          // Everything is ok
    LOCK(),        // We need to stay in this state (action may required multiple turn)
    END_OF_TURN(), // We need to stop going on next states with lower priority (action may take another turn to complete)
    WARN(),        // Suspicious or undesirable behavior (turn overflow)
    ERR();         // An error occurs
}