package models.game;

import akka.actor.Cancellable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import models.Painter;
import models.factory.GameRoom;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.Play;
import play.db.DB;
import play.i18n.Messages;
import play.libs.Akka;
import play.libs.Json;
import scala.concurrent.duration.Duration;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.CMS.CMS;
import utils.gamebus.GameBus;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages.GameEvent;
import utils.gamebus.GameMessages.Room;
import utils.gamebus.GameMessages.SystemMessage;
import utils.gamemanager.GameManager;

/**
 * A chat room is an Actor.
 */
public class Game extends GameRoom {

	private final Integer remainingTimeOnGuess = Integer.parseInt(Play
			.application().configuration().getString("remainingTimeOnGuess")); // Once
																				// a
																				// player
																				// has
																				// guessed,
																				// change
																				// the
																				// time
																				// for
																				// everyone
																				// to
																				// 20
																				// seconds
	private final Integer remainingTimeOnAllGuess = Integer
			.parseInt(Play.application().configuration()
					.getString("remainingTimeOnAllGuess")); // If all have
															// guessed, reduce
															// the timer to 3
															// seconds
	private final Integer maxSketcherPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("maxSketcherPointsRemaining"));
	private final Integer minSketcherPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("minSketcherPointsRemaining"));
	private final Integer maxGuesserPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("maxGuesserPointsRemaining"));
	private final Integer minGuesserPointsRemaining = Integer.parseInt(Play
			.application().configuration()
			.getString("minGuesserPointsRemaining"));
	private Integer maxRound = Integer.parseInt(Play.application()
			.configuration().getString("maxRounds")); // Maximum number of
														// rounds
	private Integer requiredPlayers = Integer.parseInt(Play.application()
			.configuration().getString("requiredPlayers"));
	private final Boolean fixGroundTruth = Boolean.parseBoolean(Play
			.application().configuration().getString("fixGroundTruth"));
	private final Integer groundTruthId = Integer.parseInt(Play.application()
			.configuration().getString("groundTruthId"));
	// [TODO] Minimum tags that an image should have to avoid asking to the
	// users for new tags
	private final Integer minimumTags = Integer.parseInt(Play.application()
			.configuration().getString("minimumTags"));
	// Variables used to manage the rounds
	private Boolean guessedWord = false; // Has the word been guessed for the
											// current round?
	private Boolean gameStarted = false;
	private Boolean areWeAsking = false;
	private Integer roundNumber = 0; // Starts with round number 1
	private Integer sketcherPointsRemaining = 0; // The number of points
													// available to the
													// sketcher: for the first
													// reply he gets 5, for the
													// second 4 and so on
	private Integer guesserPointsRemaining = maxGuesserPointsRemaining; // The
																		// number
																		// of
																		// points
																		// available
																		// to
																		// the
																		// guessers:the
																		// first
																		// get
																		// 10,
																		// the
																		// second
																		// 9 and
																		// so on
	private Integer numberGuessed = 0; // Number of players that have guessed
										// for a specific round
	private Painter sketcherPainter; // The current sketcher
	// System variables
	private final Integer modules = 2; // Number of modules available in the
										// game
										// (chat/paint)
	private Room roomChannel; // Name of the room
	private Boolean taskAcquired = false;
	// Members of this room.
	private CopyOnWriteArrayList<Painter> playersVect = new CopyOnWriteArrayList<>();
	// Control Variables
	private ObjectNode guessObject;
	private Integer missingPlayers = requiredPlayers;
	private Integer disconnectedPlayers = 0;
	private Boolean shownImages = false;
	private final HashSet<ObjectNode> taskHashSet = new HashSet<>();
	private final HashSet<ObjectNode> priorityTaskHashSet = new HashSet<>();
	// We should not assign the same uTask to the same match, keep a list of the
	// uTasks that has been already used
	private final HashSet<Integer> usedUTasks = new HashSet<>();
	// We should not use more times the same tag
	private final HashSet<String> usedTags = new HashSet<>();
	private Integer uTaskID = null;
	private ObjectNode taskImage;
	private Integer sessionId;
	private final static Integer maxSinglePlayer = Play.application()
			.configuration().getInt("maxSinglePlayer");

	public Game() {
		super(Game.class);
	}

	/*
	 * Handles all the messages sent to this actor
	 * 
	 * @param message An Object representing a generic message sent to our Actor
	 */
	@Override
	public void onReceive(final Object message) throws Exception {

		if (message instanceof Room) {
			this.roomChannel = ((Room) message);
			requiredPlayers = ((Room) message).getRequiredPlayers();
			// In the initial idea of single player, give 50 images to the
			// player that is segmenting
			if (requiredPlayers == 1) {
				maxRound = maxSinglePlayer;
			}
			missingPlayers = requiredPlayers;
			newGameSetup();
			Logger.info("[GAME] " + roomChannel.getRoom() + " created.");
		}
		if (message instanceof GameEvent) {
			final GameEvent event = (GameEvent) message;
			switch (event.getType()) {
			case join:
				playerJoin(event.getMessage());
				publishLobbyEvent(GameEventType.gameInfo);
				break;
			case quit:
				handleQuitter(event.getMessage());
				publishLobbyEvent(GameEventType.gameInfo);
				break;
			case guessed:
				guessed(event.getMessage());
				break;
			case timeExpired:
				playerTimeExpired(event.getMessage());
				break;
			case finalTraces:
				CMS.closeUTask(uTaskID, CMS.segmentation(event.getObject(),
						sketcherPainter.name, sessionId));
				break;
			case tag:
				taskImage.remove("tag");
				taskImage.put("tag", event.getMessage());
				sendTask(false);
				break;
			case skipTask:
				skipTask(event.getMessage());
				break;
			case taskAcquired:
				taskAcquired();
				break;
			case getGameInfo:
				publishLobbyEvent(GameEventType.gameInfo);
				break;
			default:
				break;
			}
		}
	}

	public Integer generateRandomItem(final int i, final int size) {
		Integer item;
		byte trials = 0;
		do {
			try {
				item = new Random().nextInt(size);
			} catch (final IllegalArgumentException ex) {
				item = null;
				Logger.error("[GAME] Failed to retrieve Task Image, retrying.");
				trials++;
				if (trials >= 5) {
					throw new Error(
							"[GAME] Failed to retrieve Task Image, aborting");
				}
			}
		} while ((item == null) || (item == i));

		return item;
	}

	public static Integer generateRandomItem(final int size) {
		Integer item;
		byte trials = 0;
		do {
			try {
				item = new Random().nextInt(size);
			} catch (final IllegalArgumentException ex) {
				item = null;
				Logger.error("[GAME] Failed to retrieve Task Image, retrying.");
				trials++;
				if (trials >= 5) {
					throw new Error(
							"[GAME] Failed to retrieve Task Image, aborting");
				}
			}
		} while (item == null);

		return item;
	}

	/*
	 * Retrieves one of the images that has been stored to be segmented at
	 * random being careful not to retrieve the same image two times for the
	 * same match. <p>
	 * 
	 * @return The object related to the task: image + tag
	 */
	private ObjectNode retrieveTaskImage() {
		guessObject = null;
		uTaskID = null;
		// If we have task prioritized, then use them first
		while (priorityTaskHashSet.size() > 0 && guessObject == null) {
			final int size = priorityTaskHashSet.size();
			final Integer item = generateRandomItem(size);
			int i = 0;
			final Iterator<ObjectNode> it2 = priorityTaskHashSet.iterator();
			while (it2.hasNext()) {
				final ObjectNode obj = it2.next();
				if (i == item) {
					usedTags.add(obj.get("tag").asText());
					guessObject = obj;
					final Integer task = guessObject.get("taskid").asInt();
					if (usedUTasks.contains(task)) {
						priorityTaskHashSet.remove(guessObject);
						guessObject = null;
					} else {
						uTaskID = guessObject.get("utaskid").asInt();
						usedUTasks.add(task);
					}
					break;
				}
				i = i + 1;
			}
			if (guessObject != null) {
				priorityTaskHashSet.remove(guessObject);
			}
		}
		if (guessObject == null) {
			int size;
			Integer item = null;
			byte trials = 0;
			do {
				try {
					size = taskHashSet.size();
					if (size > 0) {
						item = new Random().nextInt(size);
					} else {
						break;
					}
				} catch (final IllegalArgumentException ex) {
					item = null;
					Logger.error("[GAME] Failed to retrieve Task Image, retrying. Exception: "
							+ ex);
					trials++;
					if (trials >= 5) {
						throw new Error(
								"[GAME] Failed to retrieve Task Image, aborting");
					}
				}
			} while (item == null);
			if (item != null) {
				int i = 0;
				Iterator<ObjectNode> it = taskHashSet.iterator();
				while (it.hasNext()) {
					final ObjectNode obj = it.next();
					if (i == item) {
						if (!usedTags.contains(obj.get("tag").asText())
								|| requiredPlayers == 1) {
							guessObject = obj;
							usedTags.add(obj.get("tag").asText());
							break;
						} else {
							taskHashSet.remove(obj);
							size = taskHashSet.size();
							item = generateRandomItem(i, size);
							i = 0;
							it = taskHashSet.iterator();
							continue;
						}
					}
					i = i + 1;
				}
				taskHashSet.remove(guessObject);
			} else {
				guessObject = null;
			}
		}
		return guessObject;
	}

	/*
	 * Retrieves the name of the sketcher for the next round. It assigns the
	 * sketcher role to the players that have never played as such first <p>
	 * 
	 * @return The object related to the task: image + tag
	 */
	private String nextSketcher() {
		sketcherPainter = null;
		final int currentPlayers = requiredPlayers - disconnectedPlayers;
		int count = 0;
		// Publish system messages to inform that a new round is starting and
		// the roles are being chosen
		GameBus.getInstance().publish(
				new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),
						"newround"), roomChannel));
		GameBus.getInstance().publish(
				new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),
						"choosingroles"), roomChannel));

		// Set all the players as GUESSERS at the beginning
		for (int i = 0; i < currentPlayers; i++) {
			playersVect.get(i).role = "GUESSER";
		}

		// Keep searching for a new sketcher
		while (sketcherPainter == null) {
			// If all the players have been sketcher at least one time, reset
			// their status
			if (count > currentPlayers) {
				for (int i = 0; i < currentPlayers; i++) {
					playersVect.get(i).hasBeenSketcher = false;
				}
				count = 0;
			} // Find a sketcher at random among the ones that have never played
				// such a role
			else {
				final int index = (int) (Math.random() * currentPlayers);
				if (!playersVect.get(index).hasBeenSketcher) {
					sketcherPainter = playersVect.get(index);
					sketcherPainter.role = "SKETCHER";
					sketcherPainter.hasBeenSketcher = true;
				}
				count++;
			}
		}
		// Publish a system message to inform the other players on who is the
		// sketcher
		GameBus.getInstance().publish(
				new SystemMessage(Messages.get(LanguagePicker.retrieveLocale(),
						"thesketcheris") + " " + sketcherPainter.name,
						roomChannel));
		return sketcherPainter.name;
	}

	/*
	 * Check if there are enough players connected and that all the modules have
	 * received the login information regarding all these players. If not enough
	 * players are connected, inform the players with a message and wait for new
	 * connections or for all the modules to receive the login of the respective
	 * players <p>
	 */
	private void checkStart() throws Exception {
		if (!triggerStart()) // Send a message to inform about the missing
								// players
		{
			final int nPlayers = playersVect.size();
			if (requiredPlayers - nPlayers > 1) {
				GameBus.getInstance().publish(
						new SystemMessage(Messages.get(
								LanguagePicker.retrieveLocale(), "waitingfor")
								+ " "
								+ (requiredPlayers - nPlayers)
								+ " "
								+ Messages.get(LanguagePicker.retrieveLocale(),
										"playerstostart"), roomChannel));
			} else if (requiredPlayers - nPlayers == 1) {
				GameBus.getInstance().publish(
						new SystemMessage(Messages.get(
								LanguagePicker.retrieveLocale(), "waitingfor")
								+ " "
								+ (requiredPlayers - nPlayers)
								+ " "
								+ Messages.get(LanguagePicker.retrieveLocale(),
										"playertostart"), roomChannel));
			}
		}
	}

	private boolean triggerStart() throws Error {
		boolean canStart = true;
		for (final Painter painter : playersVect) {
			if (painter.getnModulesReceived() < modules) {
				canStart = false;
			}
		}
		// We need to wait for all the modules to receive the player list
		if (canStart && playersVect.size() >= requiredPlayers) {
			GameManager.getInstance().removeInstance(getSelf());
			publishLobbyEvent(GameEventType.gameStarted);
			if (taskAcquired) {
				// Create a new session in which to store the actions of the
				// game
				if (!fixGroundTruth)
					sessionId = CMS.openSession();
				else
					sessionId = groundTruthId;
				disconnectedPlayers = 0;
				roundNumber = 0;
				gameStarted = true;
				nextRound();
				// We start the game
				if (sketcherPainter != null) {
					GameBus.getInstance().publish(
							new GameEvent(roomChannel,
									GameEventType.gameStarted));
				} else {
					throw new Error("[GAME]: Cannot find a suitable Sketcher!");
				}
			} else {
				GameBus.getInstance().publish(
						new SystemMessage(Messages.get(
								LanguagePicker.retrieveLocale(), "acquiring"),
								roomChannel));
				GameBus.getInstance().publish(
						new GameEvent(roomChannel, GameEventType.gameLoading));
			}
			return true;
		}
		return false;
	}

	/*
	 * Start a new round of the game
	 */
	private void nextRound() {
		roundNumber++;
		if (roundNumber <= maxRound) {
			// Reset the points and status counters
			guesserPointsRemaining = maxGuesserPointsRemaining;
			sketcherPointsRemaining = 0;
			numberGuessed = 0;
			guessedWord = false;
			// Nobody has guessed for this round
			for (final Painter reset : playersVect) {
				reset.guessed = false;
			}
			nextSketcher();
			// Check if a tag for the current image as already been provided;if
			// not, ask for a new one
			try {
				taskImage = retrieveTaskImage();
			} // We cannot recover the task to be done, recover the error by
				// closing
				// the game
			catch (final Exception e) {
				gameEnded();
			}
			if (taskImage != null) {
				final String label = taskImage.get("tag").asText();
				if (label.equals("")) // We need to ask for a new tag
				{
					sendTask(true);
				} else // We have already a tag that has been provided, use that
						// one
				{
					sendTask(false);
				}
			} // We have no more things to do
			else {
				gameEnded();
			}
		} // We have played all the rounds for the game, inform the users and
			// the modules
			// that the match has ended
		else {
			gameEnded();
		}
	}

	private void sendTask(final Boolean ask) {
		if (ask) {
			areWeAsking = true;
			final GameEvent task = new GameEvent(sketcherPainter.name,
					roomChannel, GameEventType.askTag);
			task.setObject(taskImage);
			GameBus.getInstance().publish(task);
		} else {
			areWeAsking = false;
			GameBus.getInstance().publish(
					new GameEvent(sketcherPainter.name, roomChannel,
							GameEventType.nextRound));
			final GameEvent task = new GameEvent(sketcherPainter.name,
					roomChannel, GameEventType.task);
			task.setObject(taskImage);
			GameBus.getInstance().publish(task);
		}
	}

	/*
	 * Check if the timer for all the players has expired, show the solution for
	 * the current round and start a new one
	 */
	private void playerTimeExpired(final String name) {
		// If all the players have disconnected during a game, start a new one
		// if it's not a single player game
		if (((requiredPlayers - disconnectedPlayers) <= 1) && gameStarted
				&& requiredPlayers != 1) {
			// Restart the game
			gameEnded();
		} // There are still players in game
		else {
			// We are still missing the end response from some players
			// [TODO] POSSIBLE EXPLOIT
			if (missingPlayers > 0) {
				for (final Iterator<Painter> it = playersVect.iterator(); it
						.hasNext();) {
					final Painter painter = it.next();
					if (painter.name.equals(name)) {
						missingPlayers--;
					}
				}
			}
			// If we have received a response from all the active players in the
			// game, end the round
			if ((missingPlayers - disconnectedPlayers) == 0) {
				// Before calling the new round, show the solution to all the
				// players and end the round
				if (shownImages == false && areWeAsking == false) {
					showImages();
					shownImages = true;
					missingPlayers = requiredPlayers;
					// If at least one player has guessed, it means that the
					// drawn contour is a good one
					if (guessedWord) {
						GameBus.getInstance().publish(
								new GameEvent(roomChannel,
										GameEventType.saveTraces));
					}
				} // If the solution has been given or a tag has not been
					// chosen, start a new round
				else {
					if (areWeAsking) {
						GameBus.getInstance().publish(
								new SystemMessage(
										sketcherPainter.name
												+ " "
												+ Messages.get(LanguagePicker
														.retrieveLocale(),
														"notag"), roomChannel));
					}
					areWeAsking = false;
					// Start a new round
					nextRound();
					shownImages = false;
					missingPlayers = requiredPlayers;
				}
			}
		}
	}

	/*
	 * Initialization of the variables to start a new game
	 */
	public void newGameSetup() {
		disconnectedPlayers = 0;
		roundNumber = 0;
		gameStarted = false;
		playersVect = new CopyOnWriteArrayList<>();
                CMS.cancelThread(roomChannel.getRoom());
                while(CMS.getThread(roomChannel.getRoom())) {
                    try {
                        //Waiting for thread cancellation
                        Thread.sleep(500);
                        Logger.info("Waiting thread termination...");
                    } catch (Exception ex) {
                        Logger.error("Error while waiting thread termination");
                    }
                }
		Cancellable init = Akka.system()
				.scheduler()
				.scheduleOnce(Duration.create(1000, TimeUnit.MILLISECONDS),
						new Runnable() {
							@Override
							public void run() {
								Integer trials = 0;
								Boolean completed = false;
								while (trials < 5 && !completed) {
									try {
										trials++;
										if (!fixGroundTruth)
											CMS.taskSetInitialization(
													priorityTaskHashSet,
													taskHashSet, roomChannel);
										else
											CMS.fixGroundTruth(groundTruthId,
													priorityTaskHashSet,
													taskHashSet, roomChannel);
										completed = true;
									} catch (final Exception ex) {
										LoggerUtils.error("GAME", ex);
									}
								}
								if (trials >= 5) {
									killActor();
									throw new RuntimeException(
											"[GAME]: Impossible to retrieve the set of image relevant for this game, aborting");
								}
							}
						}, Akka.system().dispatcher());
                CMS.addInitializationThread(roomChannel.getRoom(), init);
		Logger.info("[GAME] New game started");
	}

	private void playerJoin(final String username) throws Exception {
		Logger.debug("[GAME]: Player Joined");
		int count = 0;
		for (final Painter painter : playersVect) {
			if (painter.name.equalsIgnoreCase(username)) {
				painter.setnModulesReceived(painter.getnModulesReceived() + 1);
				count = painter.getnModulesReceived();
				Logger.debug("[GAME]: player " + username + " counted " + count);
				break;
			}
		}
		if (count == 0) {
			final Painter painter = new Painter(username, false);
			painter.setnModulesReceived(1);
			// Add the new entered player, it has never been a sketcher in this
			// game (false)
			playersVect.add(painter);
			publishLobbyEvent(GameEventType.join);
			Logger.info("[GAME]: added player "
					+ playersVect.get(playersVect.size() - 1).name);
			// Check if we can start the game and, in such a case, start it
		} // Wait to see if all the modules have received the login information
			// from the players
		else if (modules > 1 ? count > (modules - 1) : count > 1) {
			Logger.debug("[GAME]: Check Start");
			checkStart();
		}
	}

	/**
	 * Function used to update the status of the lobby room
	 * 
	 * @param type
	 *            the type of the event to publish: room created, room
	 *            destroyed, update status
	 */
	private void publishLobbyEvent(final GameEventType type) {
		final GameEvent join = new GameEvent(type.toString(), GameManager
				.getInstance().getLobby(), type);
		final ObjectNode status = new ObjectNode(JsonNodeFactory.instance);
		// Get the hashcode related to this actoref in order to make it unique
		status.put("id", this.getSelf().hashCode());
		status.put("roomName", roomChannel.getRoom());
		status.put("currentPlayers", playersVect.size());
		status.put("maxPlayers", requiredPlayers);
		status.put("visible", playersVect.size() < requiredPlayers);
		join.setObject(status);
		GameBus.getInstance().publish(join);
	}

	private void handleQuitter(final String quitter) {
		for (final Painter painter : playersVect) {
			if (painter.name.equalsIgnoreCase(quitter)) {
				// Wait for all the modules to announce that the player has
				// disconnected
				painter.setnModulesReceived(painter.getnModulesReceived() - 1);
				playersVect.remove(painter);
				disconnectedPlayers++;
				GameBus.getInstance().publish(
						new GameEvent("quit", GameManager.getInstance()
								.getLobby(), GameEventType.quit));
				// End the game if there's just one player or less
				if (((requiredPlayers - disconnectedPlayers) == 1)
						&& gameStarted) // Restart the game
				{
					gameEnded();
				} else if (((requiredPlayers - disconnectedPlayers) <= 0)
						&& gameStarted) {
					publishLobbyEvent(GameEventType.gameEnded);
				}

			}
		}
		if (playersVect.isEmpty()) {
			gameEnded();
		}
	}

	private void guessed(final String guesser) {
		for (final Painter painter : playersVect) {
			// If the current painter is the guesser, has not guessed before and
			// it is not the sketcher, update his points
			if (painter.name.equals(guesser) && painter.guessed == false) {
				numberGuessed++;
				painter.setPoints(painter.getPoints() + guesserPointsRemaining);
				painter.setCorrectGuess();

				// Send the updated information to the other modules
				final ObjectNode guesserJson = Json.newObject();
				guesserJson.put("type", "points");
				guesserJson.put("name", guesser);
				guesserJson.put("points", guesserPointsRemaining);
				GameEvent eventGuesser = new GameEvent(guesser, roomChannel,
						GameEventType.points);
				eventGuesser.setObject(guesserJson);
				GameBus.getInstance().publish(eventGuesser);

				// Send also the image to be shown
				eventGuesser = new GameEvent(guesser, roomChannel,
						GameEventType.guessedObject);
				eventGuesser.setObject(guessObject);
				GameBus.getInstance().publish(eventGuesser);

				if (guesserPointsRemaining > minGuesserPointsRemaining) {
					// Assign the points to the sketcher. Has someone guessed
					// for this round? If not, assign maximum points, if so
					// assign the minimum for each guess
					sketcherPointsRemaining = (guesserPointsRemaining == maxGuesserPointsRemaining) ? maxSketcherPointsRemaining
							: minSketcherPointsRemaining;
					sketcherPainter.setPoints(sketcherPainter.getPoints()
							+ sketcherPointsRemaining);

					// Send the updated information to the other modules
					final ObjectNode sketcherJson = Json.newObject();
					sketcherJson.put("type", "points");
					sketcherJson.put("name", sketcherPainter.name);
					sketcherJson.put("points", sketcherPointsRemaining);
					final GameEvent eventSketcher = new GameEvent(
							sketcherPainter.name, roomChannel,
							GameEventType.points);
					eventSketcher.setObject(sketcherJson);
					GameBus.getInstance().publish(eventSketcher);
				}
				if (guesserPointsRemaining >= minGuesserPointsRemaining) {
					guesserPointsRemaining--;
				}
			}
			if (!guessedWord) {
				// Send the time change once just a player has guessed
				final GameEvent timeEvent = new GameEvent(roomChannel,
						GameEventType.timerChange);
				// If we are in single player mode, don't wait
				if (requiredPlayers == 1) {
					timeEvent.setObject(timerChange(0, CountdownTypes.round));
				} else {
					timeEvent.setObject(timerChange(remainingTimeOnGuess,
							CountdownTypes.round));
				}
				GameBus.getInstance().publish(timeEvent);
			}
		}
		if (numberGuessed == (playersVect.size() - 1)) {
			// Send the message to change the time for everyone to end the round
			final GameEvent timeEvent = new GameEvent(roomChannel,
					GameEventType.timerChange);
			timeEvent.setObject(timerChange(remainingTimeOnAllGuess,
					CountdownTypes.round));
			GameBus.getInstance().publish(timeEvent);
		}
		guessedWord = true;
	}

	private void gameEnded() {
		// Close the gaming session
		if (sessionId != null) {
			CMS.closeSession(sessionId);
		}
		final GameEvent endEvent = new GameEvent(roomChannel,
				GameEventType.leaderboard);
		endEvent.setObject(compileLeaderboard());
		GameBus.getInstance().publish(endEvent);
		GameBus.getInstance().publish(
				new GameEvent(roomChannel, GameEventType.gameEnded));
		publishLobbyEvent(GameEventType.gameEnded);

		final Painter[] sorted = playersVect.toArray(new Painter[0]);
		Connection connection = null;
		PreparedStatement statement = null;
		PreparedStatement statement1 = null;
		ResultSet rs = null;

		try {
			connection = DB.getConnection();

			for (final Painter painter : sorted) {
				try {
					final String query = "SELECT * FROM USERS WHERE NAME=? ";
					final String query1 = "UPDATE USERS SET TOTAL_SCORE = ? WHERE NAME = ? ";

					statement = connection.prepareStatement(query);
					statement1 = connection.prepareStatement(query1);

					statement.setString(1, painter.name);
					rs = statement.executeQuery();

					rs.next();
					statement1.setInt(1,
							rs.getInt("TOTAL_SCORE") + painter.getPoints());
					statement1.setString(2, painter.name);
					statement1.executeUpdate();
				} catch (final SQLException ex) {
					play.Logger.error("Unable to update total score for user: "
							+ painter.name, ex);
				} finally {
					if (rs != null) {
						rs.close();
					}
					if (statement != null)
						statement.close();
					if (statement1 != null)
						statement1.close();
				}

			}
		} catch (final SQLException ex) {

			Logger.error("Unable to get a DB connection.");

		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (final SQLException e) {
				play.Logger.error("Unable to close a SQL connection.");
			}
		}

		killActor();
	}

	// Prepares the leaderboard of the players based on their points
	private ObjectNode compileLeaderboard() {
		final JsonNodeFactory factory = JsonNodeFactory.instance;
		final ObjectNode leaderboard = new ObjectNode(factory);
		leaderboard.put("type", "leaderboard");
		leaderboard.put("playersNumber", playersVect.size());
		final Painter[] sorted = playersVect.toArray(new Painter[0]);
		Arrays.sort(sorted);
		final ArrayNode playersOrder = new ArrayNode(factory);
		for (final Painter painter : sorted) {
			final ObjectNode row = new ObjectNode(factory);
			row.put("name", painter.name);
			row.put("points", painter.getPoints());
			playersOrder.add(row);
		}
		leaderboard.put("playerList", playersOrder);
		return leaderboard;
	}

	private ObjectNode timerChange(final int remainingTime,
			final CountdownTypes timeObject) {
		final ObjectNode timeChange = Json.newObject();
		timeChange.put("type", "timeChange");
		timeChange.put("amount", remainingTime);
		timeChange.put("timeObject", timeObject.name());
		return timeChange;
	}

	private void skipTask(final String kind) {
		CMS.postAction(sessionId, "skiptask", sketcherPainter.name, "");
		GameBus.getInstance().publish(
				new SystemMessage(sketcherPainter.name
						+ " "
						+ Messages.get(LanguagePicker.retrieveLocale(),
								"skiptask"), roomChannel));
		final GameEvent timeEvent = new GameEvent(roomChannel,
				GameEventType.timerChange);
		timeEvent.setObject(timerChange(0, CountdownTypes.valueOf(kind)));
		GameBus.getInstance().publish(timeEvent);
	}

	private void showImages() {
		final GameEvent showImages = new GameEvent(roomChannel,
				GameEventType.showImages);
		final ObjectNode show = Json.newObject();
		show.put("type", "showImages");
		// If we are in single player mode, don't show the images again
		/*
		 * if(requiredPlayers==1) show.put("seconds",0); else
		 */
		show.put("seconds", 5);
		showImages.setObject(show);
		GameBus.getInstance().publish(showImages);
		// Send also the image to be shown
		for (final Painter painter : playersVect) {
			if (!painter.role.equals("SKETCHER") && painter.guessed == false) {
				final GameEvent eventGuesser = new GameEvent(painter.name,
						roomChannel, GameEventType.guessedObject);
				eventGuesser.setObject(guessObject);
				GameBus.getInstance().publish(eventGuesser);
			}
		}
	}

	private void taskAcquired() {
		if (!taskAcquired) {
			taskAcquired = true;
			triggerStart();
		}
	}
}

enum CountdownTypes {

	round, tag
}
