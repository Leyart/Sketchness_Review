package utils.gamebus;

/**
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public enum GameEventType {

    join, quit, guessed, timeExpired, finalTraces, tag, gameLoading, gameStarted, gameEnded, task, askTag, showImages, nextRound, points, guessedObject, timerChange, leaderboard, talk, skipTask, saveTraces, getGameInfo, gameInfo, taskAcquired, unknown
}
