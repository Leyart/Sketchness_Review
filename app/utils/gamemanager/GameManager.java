package utils.gamemanager;

import akka.actor.ActorRef;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import java.util.HashMap;

import java.util.Map;
import play.Logger;
import play.libs.Akka;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Room;

/**
 * Class used to manage the list of all the possible game instances that are
 * running. A game instance is identified by the name of the match and the
 * number of current players that have joined the match
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class GameManager implements GameManagerInterface {

    private static GameManagerInterface instance = null;
    private static Long matchId = 1L;
    private static Room lobby = new GameMessages.Room("lobby");
    private HashMap<String, ActorRef> gameInstances = new HashMap<>();

    private GameManager() {
    }

    public static GameManagerInterface getInstance() {
        if (instance == null) {
            synchronized (GameManager.class) {
                    instance = TypedActor.get(Akka.system()).typedActorOf(new TypedProps<>(GameManagerInterface.class, GameManager.class));
            }
        }
        return instance;
    }

    @Override
    public void onReceive(Object message, ActorRef ref) {
        Logger.debug((String) message);
    }

    @Override
    public String addInstance(Integer maxPlayers, String roomName, ActorRef current) {
        Long id = matchId++;
        String instanceId = roomName + id;
        if (!gameInstances.containsValue(current)) {
            gameInstances.put(instanceId, current);
        }
        return instanceId;
    }

    @Override
    public void getCurrentGames() {
        for (Map.Entry pairs : gameInstances.entrySet()) {
            ((ActorRef) pairs.getValue()).tell(new GameEvent(GameEventType.getGameInfo), (ActorRef) pairs.getValue());
        }
    }

    @Override
    public Room getLobby() {
        return lobby;
    }

    @Override
    public void removeInstance(ActorRef toRemove) {
        gameInstances.values().remove(toRemove);
        getCurrentGames();
        //toRemove.tell(Kill.getInstance(), null);
    }
}
