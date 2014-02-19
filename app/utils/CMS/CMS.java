package utils.CMS;

import akka.actor.Cancellable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import scala.concurrent.duration.Duration;
import utils.JsonReader;
import utils.LanguagePicker;
import utils.LoggerUtils;
import utils.gamebus.GameBus;
import utils.gamebus.GameEventType;
import utils.gamebus.GameMessages;
import utils.gamebus.GameMessages.Room;

/**
 * Wrapper for the CMS API
 * 
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class CMS {

	// Url of the CMS system
	private final static String rootUrl = Play.application().configuration()
			.getString("cmsUrl");
	private final static String oauthConsumerKey = Play.application()
			.configuration().getString("oauthConsumerKey");
	private final static Set<String> ilioCollection = new HashSet<String>(
			Arrays.asList("6210", "6224", "6225", "6226", "6234", "6237",
					"6240", "6243", "6254", "6257", "6263", "6269", "6274",
					"6276", "6282", "6283", "6287", "6292", "7619", "7620",
					"7621", "7622", "7623", "7624", "7625", "7626", "7627",
					"7628", "7629", "7630", "7631", "7634", "7635", "7637",
					"7640", "7641", "7642", "7643", "7644", "7646", "7648",
					"7649", "7650", "7651", "7652", "7653", "7654", "7656",
					"7657", "7658", "7659", "7660", "7662", "7663", "7664",
					"7665", "7666", "7667", "7668", "7669", "7670", "7672",
					"7673", "7674", "7675", "7677", "7678", "7679", "7680",
					"7681", "7683", "7684", "7685", "7686", "7687", "7688",
					"7689", "7690", "7691", "7692", "7693", "7694", "7695",
					"7696", "7697", "7698", "7699", "7700", "7701", "7702",
					"7703", "7704", "7705", "7706", "7708", "7709", "7711",
					"7712", "7713", "7714"));
        private static HashMap<String,Cancellable> runningThreads = new HashMap<String, Cancellable>();

	public static void closeUTask(final Integer uTaskID, final Integer actionId) {
		if (uTaskID != null) {
			final String request = rootUrl + "/wsmc/utask/" + uTaskID + "/"
					+ actionId + "/close";
			WS.url(request).setContentType("application/x-www-form-urlencoded")
					.put("");
			Logger.debug("[CMS] Closing uTask " + uTaskID);
		}
	}

	public static void closeTask(final Integer taskID) {
		if (taskID != null) {
			final String request = rootUrl + "/wsmc/task/" + taskID + "/close";
			WS.url(request).setContentType("application/x-www-form-urlencoded")
					.put("");
			Logger.debug("[CMS] Closing Task " + taskID);
		}
	}

	public static Integer segmentation(final ObjectNode finalTraces,
			final String username, final Integer session)
			throws MalformedURLException, IOException, JSONException {
		final String id = finalTraces.get("id").getTextValue();
		final String label = finalTraces.get("label").getTextValue();
		textAnnotation(finalTraces, username, session);
		final String traces = finalTraces.get("traces").toString();
		final String history = finalTraces.get("history").toString();

		final String urlParameters = "ta_name=tag&ta_val=" + label
				+ "&content_type=segmentation&&user_id=" + username
				+ "&language=" + LanguagePicker.retrieveIsoCode()
				+ "&session_id=" + session + "&polyline_r=" + traces
				+ "&polyline_h=" + history + "&oauth_consumer_key="
				+ oauthConsumerKey;
		final String request = rootUrl + "/wsmc/image/" + id
				+ "/segmentation.json";
		final F.Promise<WS.Response> returned = WS.url(request)
				.setContentType("application/x-www-form-urlencoded")
				.post(urlParameters);
		final JSONObject actionInfo = new JSONObject(returned.get().getBody());
		final Integer actionId = Integer.parseInt(actionInfo.get("vid")
				.toString());
		Logger.debug("[CMS] Storing segmentation with action " + actionId
				+ " for image with id " + id + " and tag " + label);
		return actionId;
	}

	public static Integer textAnnotation(final ObjectNode finalTraces,
			final String username, final Integer session)
			throws MalformedURLException, IOException, JSONException {
		final JsonReader jsonReader = new JsonReader();
		final String label = finalTraces.get("label").getTextValue();
		final String id = finalTraces.get("id").getTextValue();
		final JsonNode image = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image/" + id + ".json");
		final JsonNode imageSegments = image.get("descriptions");
		final HashSet<String> available = retrieveTags(imageSegments);
		// If the tag is not present in the list of the available tags, add it
		// to
		// the list
		if (!available.contains(label)) {
			// Just the list of the single, current tags is saved under a
			// content descriptor called availableTags
			final String urlParameters = "ta_name=tag&ta_val=" + label
					+ "&content_type=availableTags&&user_id=" + username
					+ "&language=" + LanguagePicker.retrieveIsoCode()
					+ "&session_id=" + session + "&oauth_consumer_key="
					+ oauthConsumerKey;
			final String request = rootUrl + "/wsmc/image/" + id
					+ "/textAnnotation.json";
			WS.url(request).setContentType("application/x-www-form-urlencoded")
					.post(urlParameters);
			Logger.debug("[CMS] Adding new tag: " + label
					+ " for image with id " + id);
		}
		// In any case, record that the player has tagged the image with this
		// tag
		final String urlParameters = "ta_name=tag&ta_val=" + label
				+ "&content_type=tagging&&user_id=" + username + "&language="
				+ LanguagePicker.retrieveIsoCode() + "&session_id=" + session
				+ "&oauth_consumer_key=" + oauthConsumerKey;
		final String request = rootUrl + "/wsmc/image/" + id
				+ "/textAnnotation.json";
		final F.Promise<WS.Response> returned = WS.url(request)
				.setContentType("application/x-www-form-urlencoded")
				.post(urlParameters);
		final JSONObject actionInfo = new JSONObject(returned.get().getBody());
		final Integer actionId = Integer.parseInt(actionInfo.get("vid")
				.toString());
		Logger.debug("[CMS] Storing textAnnotation with action " + actionId
				+ " for image with id " + id + " and tag " + label);
		return actionId;
	}

	public static Integer openSession() throws Error {
		final String request = rootUrl + "/wsmc/session.json";
		Logger.debug("[CMS] Opening a new session...");
		final F.Promise<WS.Response> returned = WS.url(request)
				.setContentType("application/x-www-form-urlencoded")
				.post("oauth_consumer_key=" + oauthConsumerKey);

		String sessionId = returned.get().getBody();
		sessionId = sessionId.replace("[\"", "");
		sessionId = sessionId.replace("\"]", "");
		Logger.debug("[CMS] Retrieved session " + sessionId);
		return Integer.valueOf(sessionId);
	}

	public static void closeSession(final Integer sessionId) throws Error {
		final String request = rootUrl + "/wsmc/session/" + sessionId;
		WS.url(request).setContentType("application/x-www-form-urlencoded")
				.put("state=0&oauth_consumer_key=" + oauthConsumerKey);
		Logger.debug("[CMS] Closing session " + sessionId);
	}

	public static void postAction(final Integer sessionId,
			final String actionType, final String username, String log)
			throws Error {
		final String request = rootUrl + "/wsmc/action";
		if (log.equals("")) {
			log = "{}";
		}
		final String parameters = "session_id=" + sessionId + "&action_type="
				+ actionType + "&user_id=" + username + "&oauth_consumer_key="
				+ oauthConsumerKey + "&action_log=" + log;
		WS.url(request).setContentType("application/x-www-form-urlencoded")
				.post(parameters);
		Logger.debug("[CMS] Action " + actionType + " for session " + sessionId
				+ ": " + log);
	}

	public static void fixGroundTruth(final Integer sessionId,
			final HashSet<ObjectNode> priorityTaskHashSet,
			final HashSet<ObjectNode> taskHashSet, final Room roomChannel) {
		final JsonReader jsonReader = new JsonReader();
		JsonNode retrievedImages;
		final HashMap<String, ObjectNode> temporary = new HashMap<>();
		retrievedImages = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/image.json");
                boolean taskSent=false;
		if (retrievedImages != null) {
			// For each image
			for (final JsonNode item : retrievedImages) {
				if (item.getElements().hasNext()) {
					// Save information related to the image
					final String id = item.get("id").asText();
					final String url = rootUrl
							+ item.get("mediaLocator").asText();
					final Integer width = item.get("width").asInt();
					final Integer height = item.get("height").asInt();

					// Get all the segments that have been stored for the image
					final JsonNode imageSegments = item.get("descriptions");
					HashSet<String> tags = new HashSet<>();
					final ObjectNode guessWord = Json.newObject();
					guessWord.put("type", "task");
					guessWord.put("id", id);

					// Find the valid tags for this task.
					if (imageSegments != null) {
						tags = retrieveTags(imageSegments);
					}
					// Add one tag among the ones that have been retrieved
					// following a particular policy
					guessWord.put("tag", chooseTag(tags));
					guessWord.put("lang", LanguagePicker.retrieveIsoCode());
					guessWord.put("image", url);
					guessWord.put("width", width);
					guessWord.put("height", height);
					temporary.put(id, guessWord);
				}
			}
			JsonNode processedSession = jsonReader.readJsonArrayFromUrl(rootUrl
					+ "/wsmc/session/" + sessionId + ".json");
			if (processedSession != null) {
				processedSession = processedSession.get("actions");
				for (final JsonNode item : processedSession) {
					if (item.get("type").asText().equals("segmentation")) {
						final JsonNode segmentation = jsonReader
								.readJsonArrayFromUrl(rootUrl + "/wsmc/action/"
										+ item.get("id").asText() + ".json");
						if (temporary.containsKey(segmentation.get("image")
								.asText()))
							temporary
									.remove(segmentation.get("image").asText());
					}
				}
			}
			for (final Map.Entry pairs : temporary.entrySet()) {
				taskHashSet.add((ObjectNode) pairs.getValue());
			}
			if(!taskSent) {
                             taskSent=true;
                             sendTaskAcquired(roomChannel);
                        }
		}
	}
        
        
        public static void addInitializationThread(String roomName,Cancellable thread) {
            runningThreads.put(roomName, thread);
        }
        
        public static boolean getThread(String roomName) {
            if(runningThreads.containsKey(roomName))
                return true;
            else
                return false;
        }
        
        public static void cancelThread(final String roomName) {
            final Cancellable thread = runningThreads.get(roomName);
            if(thread!=null) {
                thread.cancel();
                Akka.system()
				.scheduler()
				.scheduleOnce(Duration.create(1000, TimeUnit.MILLISECONDS),
						new Runnable() {
							@Override
							public void run() {
								while(!thread.isCancelled()) {
                                                                    try {
                                                                       Thread.sleep(100);
                                                                    } catch (Exception ex) {
                                                                        Logger.error("Error in waiting the thread termination\n"+ex);
                                                                    }
                                                                }
                                                                runningThreads.remove(roomName);
                                                                Logger.info("Thread cancelled and removed.");
							}
						}, Akka.system().dispatcher());
                }
            
        }

	/**
	 * Retrieving data from the CMS [TODO] Right now we are not retrieving based
	 * on the requirements of our tasks such as completing tasks that have not
	 * been already faced and so on. We will add this feature in the future.
	 * 
	 */
	public static void taskSetInitialization(
			final HashSet<ObjectNode> priorityTaskHashSet,
			final HashSet<ObjectNode> taskHashSet, final Room roomChannel)
			throws Error, JSONException {

		final JsonReader jsonReader = new JsonReader();
		JsonNode retrievedTasks;
		JsonNode retrievedImagesOrdered;
		ArrayList<JsonNode> retrievedImages;
                boolean taskSent = false;

		// [TODO] Fail safe in case of not being able to retrieve the instances
		try {
			retrievedTasks = jsonReader.readJsonArrayFromUrl(rootUrl
					+ "/wsmc/task.json");
			retrievedImagesOrdered = jsonReader.readJsonArrayFromUrl(rootUrl
					+ "/wsmc/image.json");
		} catch (final IllegalArgumentException e) {
			throw new RuntimeException(
					"[CMS] The request to the CMS is malformed");
		}

		int i = 0;
		retrievedImages = new ArrayList<>();

		while (i < retrievedImagesOrdered.size()) {
			retrievedImages.add(i, retrievedImagesOrdered.get(i));
			i++;
		}

		final Long seed = System.nanoTime();
		Collections.shuffle(retrievedImages, new Random(seed));

		// Fill the set of task to be performed with the task that has been
		// explicitly declared

		if (retrievedTasks != null) {
			retrievedTasks = retrievedTasks.get("task");
			for (JsonNode item : retrievedTasks) {
				if (item.getElements().hasNext()) {
					// If the task is still open
					if (item.get("status").asInt() == 1) {
						final String taskId = item.get("id").getTextValue();
						final JsonNode uTasks = item.get("utask");
						item = jsonReader.readJsonArrayFromUrl(rootUrl
								+ "/wsmc/task/" + item.get("id").getTextValue()
								+ ".json");
						item = item.get("task");
						Logger.debug("[CMS] Retrieved open task "
								+ item.toString());
						final String id = item.get("image").getElements()
								.next().getElements().next().asText();
						if (uTasks != null) {
							// Retrieve the first uTask for the current task and
							// assign it
							for (JsonNode utask : uTasks) {
								if (utask.getElements().hasNext()) {
									if (utask.get("status").asInt() == 1) {
										utask = jsonReader
												.readJsonArrayFromUrl(rootUrl
														+ "/wsmc/utask/"
														+ utask.get("id")
																.getTextValue()
														+ ".json");
										final JsonNode image = jsonReader
												.readJsonArrayFromUrl(rootUrl
														+ "/wsmc/image/" + id
														+ ".json");
										utask = utask.get("uTask");
										final ObjectNode guessWord = Json
												.newObject();
										guessWord.put("type", "task");
										guessWord.put("id", id);
										// Change the task to assign based on
										// the kind of task that has to be
										// performed
										// for now just tagging and segmentation
										// are supported for the images.
										switch (utask.get("taskType").asText()) {
										case "tagging":
											guessWord.put("tag",
													chooseTag(null));
											guessWord.put("lang",
													LanguagePicker
															.retrieveIsoCode());
											guessWord.put("image", rootUrl
													+ image.get("mediaLocator")
															.asText());
											guessWord.put("width",
													image.get("width").asInt());
											guessWord
													.put("height",
															image.get("height")
																	.asInt());
											guessWord.put("utaskid",
													utask.get("id").asInt());
											guessWord.put("taskid", taskId);
											priorityTaskHashSet.add(guessWord);
                                                                                        if(!taskSent) {
                                                                                            taskSent=true;
                                                                                            sendTaskAcquired(roomChannel);
                                                                                        }
											break;
										case "segmentation":
											HashSet<String> tags;
											// Get all the segments that have
											// been stored for the image
											final JsonNode imageSegments = image
													.get("descriptions");
											if (imageSegments != null) {
												tags = retrieveTags(imageSegments);
												guessWord.put("tag",
														chooseTag(tags));
												guessWord
														.put("lang",
																LanguagePicker
																		.retrieveIsoCode());
												guessWord
														.put("image",
																rootUrl
																		+ image.get(
																				"mediaLocator")
																				.asText());
												guessWord.put("width", image
														.get("width").asInt());
												guessWord.put("height", image
														.get("height").asInt());
												guessWord.put("utaskid", utask
														.get("id").asInt());
												guessWord.put("taskid", taskId);
												priorityTaskHashSet
														.add(guessWord);
												if(!taskSent) {
                                                                                                    taskSent=true;
                                                                                                    sendTaskAcquired(roomChannel);
                                                                                                }
											}
											break;
										}
										break;
									}
								}
							}
						} // There are no more uTasks left, close the task
						else {
							closeTask(Integer.parseInt(taskId));
						}
					}
				}
			}
		}

		// FIXME

		// For each image
		for (final JsonNode item : retrievedImages) {
			if (item.getElements().hasNext()) {
				// Save information related to the image
				final String id = item.get("id").asText();

				if (!ilioCollection.contains(id)) {
					// the image is not part of the collection
					continue;
				}

				final String url = rootUrl + item.get("mediaLocator").asText();
				final Integer width = item.get("width").asInt();
				final Integer height = item.get("height").asInt();

				// Get all the segments that have been stored for the image
				final JsonNode imageSegments = item.get("descriptions");
				HashSet<String> tags = new HashSet<>();
				final ObjectNode guessWord = Json.newObject();
				guessWord.put("type", "task");
				guessWord.put("id", id);

				// Find the valid tags for this task.
				if (imageSegments != null) {
					tags = retrieveTags(imageSegments);
				}

				// Add one tag among the ones that have been retrieved following
				// a particular policy
				guessWord.put("tag", chooseTag(tags));
				guessWord.put("lang", LanguagePicker.retrieveIsoCode());
				guessWord.put("image", url);
				guessWord.put("width", width);
				guessWord.put("height", height);
				taskHashSet.add(guessWord);
                                if(!taskSent) {
                                    taskSent=true;
                                    sendTaskAcquired(roomChannel);
                                }
			}
		}

	}

	/*
	 * Inform the game that at least one task is ready and we can start the game
	 */
	private static void sendTaskAcquired(final Room roomChannel) {
                    final GameMessages.GameEvent taskAcquired = new GameMessages.GameEvent(
                                    roomChannel, GameEventType.taskAcquired);
                    GameBus.getInstance().publish(taskAcquired);
	}

	public static HashSet<String> retrieveTags(JsonNode imageSegments) {
		final JsonReader jsonReader = new JsonReader();
		final HashSet<String> tags = new HashSet<>();
		imageSegments = imageSegments.get("availableTags");
		if (imageSegments != null) {
			if (imageSegments.getElements().hasNext()) {
				for (final JsonNode segment : imageSegments) {
					// Retrieve the content descriptor
					if (null != segment) {
						JsonNode retrieved = jsonReader
								.readJsonArrayFromUrl(rootUrl
										+ "/wsmc/content/"
										+ segment.get("id").getTextValue()
										+ ".json");
						retrieved = retrieved.get("itemAnnotations").get(0);
						// If the annotation is a tag and is in the same
						// language as the one defined in the system, add the
						// tag to the list of possible tags
						if ((retrieved.get("name").asText().equals("tag"))
								&& (retrieved
										.get("language")
										.asText()
										.equals(LanguagePicker
												.retrieveIsoCode()) || LanguagePicker
										.retrieveIsoCode().equals(""))) {
							tags.add(retrieved.get("value").asText());
						}
					}
				}
			}
		}
		return tags;
	}

	/**
	 * Returns all the items of a selected collection
	 * 
	 * @param collectonId
	 * @return
	 */
	public static HashSet<String> getCollection(final String collectonId) {

		final HashSet<String> photos = new HashSet<>();
		final JsonReader jsonReader = new JsonReader();
		final JsonNode retrieved = jsonReader.readJsonArrayFromUrl(rootUrl
				+ "/wsmc/content/" + collectonId + ".json");
		// (retrieved.get("name").asText().equals("tag")
		return photos;
	}

	/**
	 * Retrive all the images' Ids that are stored in the system
	 * 
	 * @param jsonImages
	 *            JsonNode off all images
	 * @return JSONArray with all the ids sorted depending on number of
	 *         annotations
	 * @throws JSONException
	 */
	public static JSONArray retriveImageId(final JsonNode jsonImages)
			throws JSONException {

		final JSONArray imageIds = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		JsonNode object;
		JSONObject element;
		int num = 0;

		int i = 0;
		while (i < jsonImages.size()) {
			sorting = new SortObject() {
			};
			object = jsonImages.get(i);
			sorting.setId(object.get("id").asText());
			sorting.setMedia(rootUrl + object.get("mediaLocator").asText());
			if (object.has("descriptions")) {
				if (object.get("descriptions").has("segmentation")) {
					num = object.get("descriptions").get("segmentation").size();
				} else {
					num = 0;
				}
			}
			sorting.setNum(num);
			tempList.add(i, sorting);
			num = 0;
			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() > o2.getNum()) {
					return -1;
				} else if (o1.getNum() < o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		while (it.hasNext()) {
			element = new JSONObject();
			final SortObject obj = it.next();
			element.put("id", obj.getId());
			element.put("media", obj.getMedia());
			imageIds.put(element);
		}

		return imageIds;
	}

	/**
	 * Retrive all the tasks' ids that are stored in the system
	 * 
	 * @param jsonTask
	 *            JsonNode off all tasks
	 * @return JSONArray with all the ids
	 * @throws JSONException
	 */
	public static JSONArray retriveTaskId(final JsonNode jsonTask)
			throws JSONException {

		final JSONArray taskIds = new JSONArray();
		JsonNode object;
		JSONObject element;
		int i = 0;

		while (i < jsonTask.get("task").size()) {
			element = new JSONObject();
			object = jsonTask.get("task").get(i);
			element.put("id", object.get("id"));
			element.put("taskType", object.get("taskType"));
			taskIds.put(element);
			i++;
		}
		return taskIds;
	}

	/**
	 * Retrive the stats of the system
	 * 
	 * @param jsonImages
	 *            JsonNode off all images
	 * @return JSONArray with number of tags and number of segmentation
	 * @throws JSONException
	 */
	public static JSONArray retriveStats(final JsonNode jsonImages)
			throws JSONException {

		final JSONArray values = new JSONArray();
		JsonNode object;
		JsonNode descObj;
		JsonNode tmpArr;
		final JSONObject element = new JSONObject();
		int numTag = 0;
		int numSegment = 0;
		int i = 0;

		while (i < jsonImages.size()) {
			object = jsonImages.get(i);
			if (object.has("descriptions")) {
				descObj = object.get("descriptions");
				if (descObj.has("availableTags")) {
					tmpArr = descObj.get("availableTags");
					numTag = numTag + tmpArr.size();
				}// if se descObject ha dei availableTags
				if (descObj.has("segmentation")) {
					tmpArr = descObj.get("segmentation");
					numSegment = numSegment + tmpArr.size();
				}
			}// if se c'è il campo description
			i++;
		}// fine while
		element.append("numTag", numTag);
		element.append("numSegment", numSegment);
		values.put(element);
		return values;
	}

	/**
	 * Retrive info for a specific image
	 * 
	 * @param jsonImages
	 *            The specific image which I'm evalueting
	 * @return It's tags, medialocator and number of annotation
	 * @throws JSONException
	 */
	public static String retriveImgInfo(final JsonNode jsonImages)
			throws JSONException {

		final JSONArray info = new JSONArray();
		final JsonReader jsonReader = new JsonReader();
		JsonNode itemTag;
		JsonNode segmentArr, object2, tagId;
		JsonNode descObj;
		JsonNode tagArr;
		JSONObject element;
		final JSONArray tags = new JSONArray();
		int numSegment = 0;
		int j = 0;
		String tmpTag;
		JsonNode media;

		media = jsonImages.get("mediaLocator");
		if (jsonImages.has("descriptions")) {
			descObj = jsonImages.get("descriptions");
			if (descObj.has("availableTags")) {
				tagArr = descObj.get("availableTags");
				while (j < tagArr.size()) {
					tagId = tagArr.get(j);
					tmpTag = tagId.get("id").toString();
					tmpTag = tmpTag.substring(1, tmpTag.length() - 1);
					itemTag = jsonReader.readJsonArrayFromUrl(rootUrl
							+ "/wsmc/content/" + tmpTag + ".json");
					object2 = itemTag.get("itemAnnotations").get(0)
							.get("value");
					element = new JSONObject();
					element.put("tag", object2);
					tags.put(element);
					j++;
				}// fine while
			}// if se descObject ha dei availableTags
			if (descObj.has("segmentation")) {
				segmentArr = descObj.get("segmentation");
				numSegment = segmentArr.size();
			}
		}// if se c'è il campo description
		element = new JSONObject();
		element.put("tags", tags);
		element.put("medialocator", media);
		element.put("annotations", numSegment);
		info.put(element);
		final String result = info.toString();
		return result;
	}

	/**
	 * Retrive the microTask of a particular task
	 * 
	 * @param jsonTasks
	 *            JsonNode of all the Tasks
	 * @param selected
	 *            id of the task that I want
	 * @return the status of the task (open or closed) and the information of
	 *         its microtask (id, type, status)
	 * @throws JSONException
	 */
	public static String retriveTaskInfo(final JsonNode jsonTasks,
			final String selected) throws JSONException {

		final JSONArray info = new JSONArray();
		JsonNode object, object2;
		JsonNode taskObj;
		JSONObject element;
		final JSONArray uTasks = new JSONArray();
		int i = 0;
		int j = 0;
		String tmpId;
		JsonNode status = null;

		object = jsonTasks.get("task");
		while (i < object.size()) {
			object2 = object.get(i);
			tmpId = object2.get("id").asText();
			if (tmpId.equals(selected)) {
				status = object2.get("status");
				if (object2.has("utask")) {
					element = new JSONObject();
					element.put("utask", "full");
					taskObj = object2.get("utask");
					while (j < taskObj.size()) {
						element = new JSONObject();
						element.put("id", taskObj.get(j).get("id"));
						element.put("taskType", taskObj.get(j).get("taskType"));
						element.put("status", taskObj.get(j).get("status"));
						uTasks.put(element);
						j++;
					}
					break;
				}// if se c'è il campo uTask
				else {
					element = new JSONObject();
					element.put("utask", "empty");
					uTasks.put(element);
				}
			}
			i++;
		}
		element = new JSONObject();
		element.put("status", status);
		element.put("uTasks", uTasks);
		info.put(element);
		final String result = info.toString();
		return result;
	}

	/**
	 * Close a particulr task
	 * 
	 * @param taskID
	 *            id of the task that I want to close
	 * @throws IOException
	 */
	public static void closeTask2(final String taskID) throws IOException {
		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPut httpPut = new HttpPut(rootUrl + "/wsmc/task/" + taskID
				+ "/close");
		final CloseableHttpResponse response1 = httpclient.execute(httpPut);

		final HttpEntity entity1 = response1.getEntity();
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
	}

	/**
	 * Open a new task
	 * 
	 * @param taskType
	 *            The type (segmentation or tagging) of the new task that i want
	 *            to open
	 * @param selectedImg
	 *            The id of the image which will be associated to the new task
	 * @return The id of the new task
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String addTask(final String taskType, final String selectedImg)
			throws IOException, JSONException {

		String newId;

		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPost httpPost = new HttpPost(rootUrl + "/wsmc/task.json");
		final List<NameValuePair> nvps = new ArrayList<>();
		nvps.add(new BasicNameValuePair("taskType", taskType));
		nvps.add(new BasicNameValuePair("image", selectedImg));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		final CloseableHttpResponse response1 = httpclient.execute(httpPost);
		final HttpEntity entity1 = response1.getEntity();
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				entity1.getContent()));
		final String inputLine = in.readLine();
		final JSONObject obj = new JSONObject(inputLine);
		newId = obj.getString("nid");
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
		return newId;
	}

	/**
	 * Open a new microTask
	 * 
	 * @param taskType
	 *            The type (segmentation or tagging) of the new microTask that i
	 *            want to open
	 * @param selectionTask
	 *            The id of the task which will be associated to the new
	 *            microTask
	 * @return The id of the new microTask
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String addUTask(final String taskType,
			final String selectionTask) throws IOException, JSONException {

		String newId;

		final CloseableHttpClient httpclient = HttpClients.createDefault();
		final HttpPost httpPost = new HttpPost(rootUrl + "/wsmc/utask.json");
		final List<NameValuePair> nvps = new ArrayList<>();
		nvps.add(new BasicNameValuePair("taskType", taskType));
		nvps.add(new BasicNameValuePair("task", selectionTask));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		final CloseableHttpResponse response1 = httpclient.execute(httpPost);
		final HttpEntity entity1 = response1.getEntity();
		final BufferedReader in = new BufferedReader(new InputStreamReader(
				entity1.getContent()));
		final String inputLine = in.readLine();
		final JSONObject obj = new JSONObject(inputLine);
		newId = obj.getString("nid");
		EntityUtils.consume(entity1);
		response1.close();
		httpclient.close();
		return newId;
	}

	public static JSONArray loadFirst(final JsonNode jsonImages)
			throws JSONException {
		final JSONArray graph = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		JsonNode object;
		JSONObject element;
		int num = 0;

		int i = 0;
		while (i < jsonImages.size()) {
			sorting = new SortObject() {
			};
			object = jsonImages.get(i);
			sorting.setId(object.get("id").asText());
			if (object.has("descriptions")) {
				if (object.get("descriptions").has("segmentation")) {
					num = object.get("descriptions").get("segmentation").size();
				} else {
					num = 0;
				}
			}
			sorting.setNum(num);
			tempList.add(i, sorting);
			num = 0;
			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() < o2.getNum()) {
					return -1;
				} else if (o1.getNum() > o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getNum();
		int count = 0;

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getNum() == tmp) {
				count++;
			} else if (obj.getNum() != tmp) {
				element = new JSONObject();
				element.put("occurence", count);
				element.put("annotations", tmp);
				graph.put(element);
				do {
					tmp++;
					count = 0;
					if (obj.getNum() != tmp) {
						element = new JSONObject();
						element.put("occurence", count);
						element.put("annotations", tmp);
						graph.put(element);
					}
				} while (obj.getNum() != tmp);
				count = 1;
			}
		}
		element = new JSONObject();
		element.put("occurence", count);
		element.put("annotations", tmp);
		graph.put(element);

		return graph;
	}

	public static JSONArray loadSecond(final JsonNode actions)
			throws JSONException {
		final JSONArray graph = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		final ArrayList<SortObject> tempList2 = new ArrayList<>();
		JsonNode object;
		JSONObject element;

		int i = 0;
		int j = 0;
		while (i < actions.size()) {
			object = actions.get(i);
			if (object.get("type").asText().equals("segmentation")) {
				if (object.has("user")) {
					if (object.get("user").has("cubrik_userid")) {
						sorting = new SortObject() {
						};
						sorting.setIdU(object.get("user").get("cubrik_userid")
								.asInt());
						tempList.add(j, sorting);
						j++;
					}
				}
			}

			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getIdU() < o2.getIdU()) {
					return -1;
				} else if (o1.getIdU() > o2.getIdU()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getIdU();
		int count = 0;

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getIdU() == tmp) {
				count++;
			} else if (obj.getIdU() != tmp) {
				sorting = new SortObject() {
				};
				sorting.setIdU(tmp);
				sorting.setNum(count);
				tempList2.add(sorting);
				do {
					tmp++;

				} while (obj.getIdU() != tmp);
				count = 1;
			}
		}
		sorting = new SortObject() {
		};
		sorting.setIdU(tmp);
		sorting.setNum(count);
		tempList2.add(sorting);

		Collections.sort(tempList2, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getNum() < o2.getNum()) {
					return -1;
				} else if (o1.getNum() > o2.getNum()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it2 = tempList2.iterator();
		tmp = 0;
		count = 0;

		while (it2.hasNext()) {
			final SortObject obj = it2.next();

			if (obj.getNum() == tmp) {
				count++;
			} else if (obj.getNum() != tmp) {
				element = new JSONObject();
				element.put("users", count);
				element.put("images", tmp);
				graph.put(element);
				do {
					tmp++;
					count = 0;
					if (obj.getNum() != tmp) {
						element = new JSONObject();
						element.put("users", count);
						element.put("images", tmp);
						graph.put(element);
					}
				} while (obj.getNum() != tmp);
				count = 1;
			}
		}
		element = new JSONObject();
		element.put("users", count);
		element.put("images", tmp);
		graph.put(element);

		return graph;
	}

	public static JSONArray download1(final JsonNode actions)
			throws JSONException {
		final JSONArray down1 = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		final ArrayList<DownObject> tempList2 = new ArrayList<>();
		JsonNode object;

		int i = 0;
		int j = 0;
		while (i < actions.size()) {
			object = actions.get(i);
			if (object.get("type").asText().equals("segmentation")) {
				if (object.has("user")) {
					if (object.get("user").has("cubrik_userid")) {
						sorting = new SortObject() {
						};
						sorting.setIdU(object.get("user").get("cubrik_userid")
								.asInt());
						sorting.setIdTmp(object.get("id").asInt());
						sorting.setImgTmp(object.get("image").asInt());
						tempList.add(j, sorting);
						j++;
					}
				}
			}

			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getIdU() < o2.getIdU()) {
					return -1;
				} else if (o1.getIdU() > o2.getIdU()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getIdU();
		DownObject user;
		StoredStatObj stat;
		ArrayList<StoredStatObj> elements = new ArrayList<>();

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getIdU() == tmp) {

				stat = new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
				elements.add(stat);
			} else if (obj.getIdU() != tmp) {
				user = new DownObject() {
				};
				user.setId(tmp);
				user.setElement(elements);
				tempList2.add(user);
				do {
					tmp++;

				} while (obj.getIdU() != tmp);
				elements = new ArrayList<>();
				stat = new StoredStatObj(obj.getIdTmp(), obj.getImgTmp());
				elements.add(stat);
			}
		}
		user = new DownObject() {
		};
		user.setId(tmp);
		user.setElement(elements);
		tempList2.add(user);

		final Iterator<DownObject> it2 = tempList2.iterator();

		JSONObject son;
		JSONArray body;
		JSONObject content;

		while (it2.hasNext()) {
			son = new JSONObject();
			body = new JSONArray();
			final DownObject obj = it2.next();
			son.put("user", obj.getId());

			final Iterator<StoredStatObj> it3 = obj.getElement().iterator();

			while (it3.hasNext()) {
				final StoredStatObj obj2 = it3.next();
				content = new JSONObject();
				content.put("segment", obj2.getId1());
				content.put("image", obj2.getId2());
				body.put(content);
			}
			son.put("sketch", body);
			down1.put(son);
		}

		return down1;
	}

	public static JSONArray download2(final JsonNode actions)
			throws JSONException {
		final JSONArray down2 = new JSONArray();
		SortObject sorting;
		final ArrayList<SortObject> tempList = new ArrayList<>();
		final ArrayList<DownObject> tempList2 = new ArrayList<>();
		JsonNode object;

		int i = 0;
		int j = 0;
		while (i < actions.size()) {
			object = actions.get(i);
			if (object.get("type").asText().equals("segmentation")) {
				if (object.has("user")) {
					if (object.get("user").has("cubrik_userid")) {
						sorting = new SortObject() {
						};
						sorting.setIdU(object.get("user").get("cubrik_userid")
								.asInt());
						sorting.setIdTmp(object.get("id").asInt());
						sorting.setImgTmp(object.get("image").asInt());
						tempList.add(j, sorting);
						j++;
					}
				}
			}

			i++;
		}

		Collections.sort(tempList, new Comparator<SortObject>() {
			@Override
			public int compare(final SortObject o1, final SortObject o2) {
				if (o1.getImgTmp() < o2.getImgTmp()) {
					return -1;
				} else if (o1.getImgTmp() > o2.getImgTmp()) {
					return 1;
				}
				return 0;
			}

		});

		final Iterator<SortObject> it = tempList.iterator();
		int tmp = tempList.get(0).getImgTmp();
		DownObject image;
		StoredStatObj stat;
		ArrayList<StoredStatObj> elements = new ArrayList<>();

		while (it.hasNext()) {
			final SortObject obj = it.next();

			if (obj.getImgTmp() == tmp) {

				stat = new StoredStatObj(obj.getIdTmp(), obj.getIdU());
				elements.add(stat);
			} else if (obj.getImgTmp() != tmp) {
				image = new DownObject() {
				};
				image.setId(tmp);
				image.setElement(elements);
				tempList2.add(image);
				do {
					tmp++;

				} while (obj.getImgTmp() != tmp);
				elements = new ArrayList<>();
				stat = new StoredStatObj(obj.getIdTmp(), obj.getIdU());
				elements.add(stat);
			}
		}
		image = new DownObject() {
		};
		image.setId(tmp);
		image.setElement(elements);
		tempList2.add(image);

		final Iterator<DownObject> it2 = tempList2.iterator();

		JSONObject son;
		JSONArray body;
		JSONObject content;

		while (it2.hasNext()) {
			son = new JSONObject();
			body = new JSONArray();
			final DownObject obj = it2.next();
			son.put("image", obj.getId());

			final Iterator<StoredStatObj> it3 = obj.getElement().iterator();

			while (it3.hasNext()) {
				final StoredStatObj obj2 = it3.next();
				content = new JSONObject();
				content.put("segment", obj2.getId1());
				content.put("user", obj2.getId2());
				body.put(content);
			}
			son.put("sketch", body);
			down2.put(son);
		}

		return down2;
	}

	/*
	 * Returns a tag based on a particular choice policy
	 * 
	 * @return String retrieved tag following a policy
	 */
	private static String chooseTag(final HashSet<String> tags) {
		if (tags != null && tags.size() > 0) {
			final Object[] stringTags = tags.toArray();
			final String toReturn = (String) stringTags[(new Random()
					.nextInt(tags.size()))];
			return toReturn;
		} else {
			return "";
		}
	}

}
