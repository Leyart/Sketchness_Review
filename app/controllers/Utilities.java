package controllers;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.json.JSONException;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import utils.LoggerUtils;
import utils.Renderer;
import views.html.newRenderer;
import views.html.renderer;

/**
 * 
 * @author Luca Galli <lgalli@elet.polimi.it>
 */
public class Utilities extends Controller {

	public static Result retrieveMask(final String imageID, final String tag) {
		try {
			final File result = Renderer.retrieveMask(imageID, tag);
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the mask for image "
						+ imageID + " and tag " + tag);
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}

	public static Result retrieveTags(final String imageID) {
		try {
			final JsonNode result = Renderer.retrieveTags(imageID);
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the tags for image "
						+ imageID);
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}

	public static Result retrieveImages() {
		try {
			final JsonNode result = Renderer.retrieveImages();
			if (null != result) {
				return ok(result);
			} else {
				return ok("[AGGREGATOR] Cannot retrieve the images");
			}
		} catch (final Exception e) {
			return ok("[AGGREGATOR] " + e.toString());
		}
	}

	public static Result renderer(final String imageID) {
		return ok(renderer.render(imageID));
	}

	public static Result newRendererCall() {
		return ok(newRenderer.render());

	}

	public static Result webToolAjaxCall() throws JSONException {
		final String result = Renderer.webToolAjax();
		return ok(result);
	}

	public static Result taskSelectionCall() throws JSONException {
		final String result = Renderer.taskSelection();
		return ok(result);
	}

	public static Result loadStatsCall() throws JSONException {
		final String result = Renderer.loadStats();
		return ok(result);
	}

	public static Result webInfoAjaxCall() throws JSONException {
		final String selectedImg = request().getHeader("selected");
		final String result = Renderer.webInfoAjax(selectedImg);
		return ok(result);
	}

	public static Result webTaskAjaxCall() throws JSONException {
		final String selectedTask = request().getHeader("selected");
		final String result = Renderer.webInfoTask(selectedTask);
		return ok(result);
	}

	public static Result closeTaskCall() throws IOException {
		final String selectionTask = request().getHeader("selectionTask");
		Renderer.closeTask(selectionTask);
		final String result = "ok";
		return ok(result);
	}

	public static Result addTaskCall() throws IOException, JSONException {
		String newId;
		final String taskType = request().getHeader("taskType");
		final String selectionimg = request().getHeader("selectionimg");
		newId = Renderer.addTask(taskType, selectionimg);
		return ok(newId);
	}

	public static Result addUTaskCall() throws IOException, JSONException {
		String newId;
		final String taskType = request().getHeader("taskType");
		final String selectionTask = request().getHeader("selectionTask");
		newId = Renderer.addUTask(taskType, selectionTask);
		return ok(newId);
	}

	public static Result loadFirstGraphCall() throws JSONException {
		final String result = Renderer.loadFirstGraph();
		return ok(result);
	}

	public static Result loadSecondGraphCall() throws JSONException {
		final String result = Renderer.loadSecondGraph();
		return ok(result);
	}

	public static Result downloadStats1Call() throws JSONException {
		final String result = Renderer.downloadStats1();
		return ok(result);
	}

	public static Result downloadStats2Call() throws JSONException {
		final String result = Renderer.downloadStats2();
		return ok(result);
	}

	public static WebSocket<JsonNode> rendererStream(final String imageID)
			throws JSONException {

		return new WebSocket<JsonNode>() {
			@Override
			public void onReady(final WebSocket.In<JsonNode> in,
					final WebSocket.Out<JsonNode> out) {
				try {
					Renderer.createRenderer(imageID, in, out);
				} catch (final Exception e) {
					LoggerUtils.error("AGGREGATOR", e);
				}
			}
		};
	}

}
