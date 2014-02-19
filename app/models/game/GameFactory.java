package models.game;

import akka.actor.ActorRef;
import models.factory.Factory;
import utils.gamebus.GameBus;
import utils.gamemanager.GameManager;

public class GameFactory extends Factory {

    public static synchronized void createGame(final String room, final Integer maxPlayers) throws Exception {
        ActorRef obtained = create(room, maxPlayers, Game.class);
        GameManager.getInstance().addInstance(maxPlayers, room, obtained);
        //Subscribe to lobby messages
        GameBus.getInstance().subscribe(obtained, GameManager.getInstance().getLobby());
    }
}
