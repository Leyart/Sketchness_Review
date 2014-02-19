package models.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import models.factory.GameRoom;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.WebSocket;
import utils.LanguagePicker;
import utils.levenshteinDistance;
import utils.gamebus.GameBus;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Join;
import utils.gamebus.GameMessages.Room;
import utils.gamebus.GameMessages.SystemMessage;

public class Chat extends GameRoom {

	Room roomChannel;
	String currentGuess;
	Boolean gameStarted = false;
	Boolean askTag = false;
	String askTagSketcher;
	// Members of this room.
	ConcurrentHashMap<String, WebSocket.Out<JsonNode>> playersMap = new ConcurrentHashMap<>();

	public Chat() {
		super(Chat.class);
	}

	@Override
	public void onReceive(final Object message) throws Exception {

		if (message instanceof Room) {
			this.roomChannel = ((Room) message);
			Logger.info("[CHAT] " + roomChannel.getRoom() + " created.");
		}
		if (message instanceof SystemMessage) {
			notifyAll(ChatKind.system, "Sketchness",
					((SystemMessage) message).getMessage());
		}
		if (message instanceof Join) {
			handleJoin((Join) message);
		} else if (message instanceof GameEvent) {
			final GameEvent event = (GameEvent) message;
			// [TODO] E' una porcata, bisogna sistemarlo ma per ora è un fix
			// veloce
			if (self().path().toString().endsWith("lobby")) {
				switch (event.getType()) {
				case talk:
					handleTalk(event.getUsername(), event.getMessage());
					break;
				case quit:
					handleQuitter(event.getMessage());
					break;
				default:
					break;
				}
			} else {
				switch (event.getType()) {
				case gameStarted:
					gameStarted = true;
					break;
				case gameEnded:
					killActor();
					gameStarted = false;
					break;
				case talk:
					handleTalk(event.getUsername(), event.getMessage());
					break;
				case task:
					retrieveTask(event.getObject());
					break;
				case askTag:
					handleAskTag(event);
					break;
				case skipTask:
					askTag = false;
					break; // Reset to the initial status, even if we don't know
							// if it was a normal task or a question
				case quit:
					handleQuitter(event.getMessage());
					break;
				default:
					Logger.debug("Ignored action...");
					break;
				}
			}
		} else {
			unhandled(message);
		}
	}

	private void handleAskTag(final GameEvent message) {
		askTagSketcher = message.getMessage();
		notifyAll(ChatKind.system, "Sketchness",
				Messages.get(LanguagePicker.retrieveLocale(), "asktag"));
		askTag = true;
	}

	private void handleJoin(final Join message) {
		// Check if this username is free.
		if (playersMap.containsKey(message.getUsername())) {
			getSender().tell(
					Messages.get(LanguagePicker.retrieveLocale(),
							"usernameused"), this.getSelf());
		} else if (!gameStarted) {
			playersMap.put(message.getUsername(), message.getChannel());
			notifyAll(ChatKind.join, message.getUsername(),
					Messages.get(LanguagePicker.retrieveLocale(), "join"));
			notifyMemberChange();
			GameBus.getInstance().publish(
					new GameEvent(message.getUsername(), roomChannel,
							GameEventType.join));
			getSender().tell("OK", this.getSelf());
			Logger.debug("[CHAT] added player " + message.getUsername());
		} else {
			getSender().tell(
					Messages.get(LanguagePicker.retrieveLocale(),
							"matchstarted"), this.getSelf());
		}
	}

	private void handleTalk(final String username, final String text) {
		// Received a Talk message
		// If we are asking the sketcher for a tag, then save the tag
		if (askTag && username.equals(askTagSketcher)) {
			askTag = false;
			GameBus.getInstance().publish(
					new GameEvent(text, username, roomChannel,
							GameEventType.tag));
		} else if (gameStarted) {
			// Compare the message sent with the tag in order to establish if we
			// have a right guess
			final levenshteinDistance distanza = new levenshteinDistance();
			if (text != null) {
				switch (distanza.computeLevenshteinDistance(text, currentGuess)) {
				case 0:
					GameBus.getInstance().publish(
							new GameEvent(username, roomChannel,
									GameEventType.guessed));
					break;
				case 1:
					notifyAll(ChatKind.talkNear, username, text);
					break;
				case 2:
					notifyAll(ChatKind.talkWarning, username, text);
					break;
				default:
					notifyAll(ChatKind.talkError, username, text);
					break;
				}
			}
		} else // The players are just chatting, not playing
		{
			notifyAll(ChatKind.talk, username, text);
		}
	}

	private void handleQuitter(final String quitter)
			throws InterruptedException {
		for (final Map.Entry<String, WebSocket.Out<JsonNode>> entry : playersMap
				.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(quitter)) {
				// Close the websocket
				entry.getValue().close();
				playersMap.remove(quitter);
				notifyAll(ChatKind.quit, quitter,
						Messages.get(LanguagePicker.retrieveLocale(), "quit"));
				notifyMemberChange();
				Logger.debug("[CHAT] " + quitter + " has disconnected.");
				GameBus.getInstance()
						.publish(
								new GameEvent(quitter, roomChannel,
										GameEventType.quit));
			}
		}
	}

	private void retrieveTask(final ObjectNode task) {
		currentGuess = task.get("tag").asText();
	}

	// Send a Json event to all members
	private void notifyAll(final ChatKind kind, final String user,
			final String text) {
		for (final WebSocket.Out<JsonNode> channel : playersMap.values()) {

			final ObjectNode event = Json.newObject();
			event.put("type", kind.name());
			event.put("user", user);
			event.put("message", text);

			final ArrayNode m = event.putArray("members");
			for (final String u : playersMap.keySet()) {
				m.add(u);
			}

			channel.write(event);
		}
	}

	// Send the updated list of members to the users
	private void notifyMemberChange() {
		for (final WebSocket.Out<JsonNode> channel : playersMap.values()) {

			final ObjectNode event = Json.newObject();
			event.put("type", ChatKind.membersChange.name());

			final ArrayNode m = event.putArray("members");
			for (final String u : playersMap.keySet()) {
				m.add(u);
			}

			channel.write(event);
		}
	}
}

enum ChatKind {

	system, join, quit, talk, talkNear, talkWarning, talkError, membersChange
}
