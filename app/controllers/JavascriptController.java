package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import play.Logger;
import play.Routes;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Result;

public class JavascriptController extends Controller {

	public static Result i18n() {
		final Lang l = request().acceptLanguages().get(0);
		String properties = "";
		Scanner sc = null;
		InputStream in = null;
		try {
			in = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("messages." + l.language());
			sc = new java.util.Scanner(in);
			properties = sc.useDelimiter("\\A").next();
		} catch (final NoSuchElementException e) {
			Logger.error("[SYSTEM] Failed to read messages file");
		} finally {
			if (sc != null) {
				sc.close();
			}
			if (in != null) {
				try {
					in.close();
				} catch (final IOException e) {
					Logger.error("Unable to close InputStream", e);
				}
			}
		}
		return ok(properties).as("text/plain");
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutes",
		// Routes
				controllers.routes.javascript.Sketchness.leaderboard()));
	}
}