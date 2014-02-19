package models.lobby;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.i18n.Messages;
import play.mvc.WebSocket;
import utils.LanguagePicker;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Join;
import utils.gamebus.GameMessages.Room;
import utils.gamemanager.GameManager;
import akka.actor.UntypedActor;

public class Lobby extends UntypedActor {

	Room roomChannel;
	String currentGuess;
	Boolean gameStarted = false;
	Boolean askTag = false;
	String askTagSketcher;
	// Members of this room.
	ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();

	@Override
	public void onReceive(final Object message) throws Exception {
		if (message instanceof Join) {
			handleJoin((Join) message);
		}
		if (message instanceof GameEvent) {
			final GameEvent event = (GameEvent) message;
			switch (event.getType()) {
			case gameInfo:
				updateList(event.getObject());
				break;
			case gameStarted:
				handleStartEnd();
				break;
			case gameEnded:
				handleStartEnd();
				break;
			case quit:
				handleQuitter(event.getMessage());
				break;
			default:
				break;
			}
		}
	}

	private void updateList(final ObjectNode object) {
		if (object != null) {
			Logger.debug(object.toString());
			object.put("type", "updateList");
			for (final WebSocket.Out<JsonNode> channel : playersMap.values()) {
				channel.write(object);
			}
		}
	}

	private void handleJoin(final Join message) {
		// Check if this username is free.
		if (playersMap.containsKey(message.getUsername())) {
			getSender().tell(
					Messages.get(LanguagePicker.retrieveLocale(),
							"usernameused"), this.getSelf());
		} else {
			playersMap.put(message.getUsername(), message.getChannel());
			getSender().tell("OK", this.getSelf());
			Logger.debug("[LOBBY] added player " + message.getUsername());
			GameManager.getInstance().getCurrentGames();
		}
	}

	private void handleStartEnd() {
		GameManager.getInstance().getCurrentGames();
	}

	private void handleQuitter(final String quitter)
			throws InterruptedException {
		for (final Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap
				.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(quitter)) {
				// Close the websocket
				entry.getValue().close();
				playersMap.remove(quitter);
				Logger.debug("[LOBBY] " + quitter + " has disconnected.");
			}
		}
	}
}
