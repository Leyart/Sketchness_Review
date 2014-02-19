package controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import models.User;
import play.Logger;
import play.Routes;
import play.data.Form;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Http.Session;
import play.mvc.Result;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthProvider.MyLogin;
import providers.MyUsernamePasswordAuthProvider.MySignup;
import views.html.contenuto;
import views.html.login;
import views.html.profile;
import views.html.signup;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.user.AuthUser;

public class Application extends Controller {

	public static final String FLASH_MESSAGE_KEY = "message";
	public static final String FLASH_ERROR_KEY = "error";
	public static final String USER_ROLE = "user";

	public static Result index() {
		return redirect(routes.Application.login());
	}

	public static User getLocalUser(final Session session) {
		final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
		final User localUser = User.findByAuthUserIdentity(currentAuthUser);
		return localUser;
	}

	@Restrict(@Group(Application.USER_ROLE))
	public static Result restricted() {
		final User localUser = getLocalUser(session());
		return ok(contenuto.render(localUser));
	}

	@Restrict(@Group(Application.USER_ROLE))
	public static Result profile() {
		final User localUser = getLocalUser(session());
		return ok(profile.render(localUser));
	}

	public static Result login() {
		Logger.debug("Requested login form");
		return ok(login.render(MyUsernamePasswordAuthProvider.LOGIN_FORM));
	}

	public static Result doLogin() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<MyLogin> filledForm = MyUsernamePasswordAuthProvider.LOGIN_FORM
				.bindFromRequest();
		Logger.debug("Requested login, filled form: " + filledForm);
		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			Logger.debug("The login form has errors: " + filledForm);
			return badRequest(login.render(filledForm));
		} else {
			// Everything was filled
			return UsernamePasswordAuthProvider.handleLogin(ctx());
		}
	}

	public static Result signup() {
		Logger.debug("Requested signin form");
		return ok(signup.render(MyUsernamePasswordAuthProvider.SIGNUP_FORM));
	}

	public static Result jsRoutes() {
		return ok(
				Routes.javascriptRouter("jsRoutes",
						controllers.routes.javascript.Signup.forgotPassword()))
				.as("text/javascript");
	}

	public static Result doSignup() {
		Logger.debug("Doing signup...");
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<MySignup> filledForm = MyUsernamePasswordAuthProvider.SIGNUP_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			Logger.debug("The signup form has errors: " + filledForm);
			return badRequest(signup.render(filledForm));
		} else {
			// Everything was filled
			// do something with your part of the form before handling the user
			// signup
			return UsernamePasswordAuthProvider.handleSignup(ctx());
		}
	}

	public static Result doLogout() {

		final User localUser = getLocalUser(session());

		if (localUser != null) {
			Logger.debug("Doing logout for user: " + localUser.name);

			final String username = localUser.name;

			Connection connection = null;
			PreparedStatement statement = null;

			try {
				connection = DB.getConnection();
				Logger.debug("Updating user status on DB: " + username);
				final String query = "UPDATE USERS SET online = ? WHERE NAME = ? ";
				statement = connection.prepareStatement(query);
				statement.setBoolean(1, false);
				statement.setString(2, username);
				statement.executeUpdate();
				Logger.debug("Updated user status on DB: " + username);
				com.feth.play.module.pa.controllers.Authenticate.logout();

			} catch (final SQLException ex) {
				play.Logger.error("Unable to logout user: " + username, ex);
				return play.mvc.Results.internalServerError();
			} finally {
				try {
					if (statement != null)
						statement.close();
				} catch (final SQLException e) {
					play.Logger.error("Unable to close a SQL connection.", e);
				}
				try {
					if (connection != null)
						connection.close();
				} catch (final SQLException e) {
					play.Logger.error("Unable to close a SQL connection.", e);
				}
			}

		}

		Logger.debug("Tried to logout but no user logged in");

		return redirect(routes.Application.index());
	}

	public static String formatTimestamp(final long t) {
		return new SimpleDateFormat("yyyy-dd-MM HH:mm:ss").format(new Date(t));
	}

}