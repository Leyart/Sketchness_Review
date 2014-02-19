package utils.gamemanager;

import akka.actor.ActorRef;
import akka.actor.TypedActor;
import utils.gamebus.GameMessages.Room;

/**
 * Class used to manage the list of all the possible game instances that are
 * running. A game instance is identified by the name of the match and the
 * number of current players that have joined the match
 *
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public interface GameManagerInterface extends TypedActor.Receiver {

    public String addInstance(Integer maxPlayers, String roomName, ActorRef current);

    public void removeInstance(ActorRef toRemove);

    @Override
    public void onReceive(Object message, ActorRef ref);

    public void getCurrentGames();

    public Room getLobby();
}
