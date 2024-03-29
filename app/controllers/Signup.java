package controllers;

import static play.data.Form.form;
import models.TokenAction;
import models.TokenAction.Type;
import models.User;
import play.Logger;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import providers.MyLoginUsernamePasswordAuthUser;
import providers.MyUsernamePasswordAuthProvider;
import providers.MyUsernamePasswordAuthProvider.MyIdentity;
import providers.MyUsernamePasswordAuthUser;
import views.html.sketchness_login;
import views.html.sketchness_signup;
import views.html.sketchness_signupGuest;
import views.html.account.signup.exists;
import views.html.account.signup.no_token_or_invalid;
import views.html.account.signup.oAuthDenied;
import views.html.account.signup.password_forgot;
import views.html.account.signup.password_reset;
import views.html.account.signup.unverified;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.user.AuthUser;

public class Signup extends Controller {

	/**
	 * Retrive the user from the session
	 */
	public static User getLocalUser(final Http.Session session) {
		final AuthUser currentAuthUser = PlayAuthenticate.getUser(session);
		final User localUser = User.findByAuthUserIdentity(currentAuthUser);
		return localUser;
	}

	public static class PasswordReset extends Account.PasswordChange {

		public PasswordReset() {
		}

		public PasswordReset(final String token) {
			this.token = token;
		}

		public String token;

		public String getToken() {
			return token;
		}

		public void setToken(final String token) {
			this.token = token;
		}
	}

	private static final Form<PasswordReset> PASSWORD_RESET_FORM = form(PasswordReset.class);

	public static Result unverified() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());

		return ok(unverified.render());

	}

	public static Result signup() {
		return ok(sketchness_signup
				.render(MyUsernamePasswordAuthProvider.SIGNUP_FORM));
	}

	public static Result doSignup() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<MyUsernamePasswordAuthProvider.MySignup> filledForm = MyUsernamePasswordAuthProvider.SIGNUP_FORM
				.bindFromRequest();

		final String newUser = filledForm.field("name").value();
		Logger.info("Signing up user: " + newUser + " email:"
				+ filledForm.field("email").value());

		if (filledForm.hasErrors()) {
			// User did not fill everything properly
			Logger.warn("The user did not fill everything properly, return an error message:"
					+ filledForm.toString());
			return badRequest(sketchness_signup.render(filledForm));
		} else {

			final User localUser = getLocalUser(session());
			// if (localUser != null) {
			// loggedUser = localUser.name;
			// } else {
			// loggedUser = newUser;
			// }

			// TOFIX, il nome non è guest!!!!
			// if there is already someone logged
			// with the same browser prevent
			// the registration of a guest
			if ((localUser != null) && newUser.equals("Guest")) {
				Logger.debug("Unable to signup guest user, someone is already logged in: "
						+ localUser);
				flash(Application.FLASH_ERROR_KEY,
						Messages.get("error.userInBrowser"));
				return badRequest(sketchness_login.render(filledForm));
			}

			// Everything was filled
			return UsernamePasswordAuthProvider.handleSignup(ctx());
		}
	}

	public static Result doSignupGuest() {
		return ok(sketchness_signupGuest
				.render(MyUsernamePasswordAuthProvider.SIGNUP_FORM));
	}

	private static final Form<MyIdentity> FORGOT_PASSWORD_FORM = form(MyIdentity.class);

	public static Result forgotPassword(final String email) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		Form<MyIdentity> form = FORGOT_PASSWORD_FORM;
		if (email != null && !email.trim().isEmpty()) {
			form = FORGOT_PASSWORD_FORM.fill(new MyIdentity(email));
		}
		return ok(password_forgot.render(form));
	}

	public static Result doForgotPassword() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<MyIdentity> filledForm = FORGOT_PASSWORD_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			// User did not fill in his/her email
			return badRequest(password_forgot.render(filledForm));
		} else {
			// The email address given *BY AN UNKNWON PERSON* to the form - we
			// should find out if we actually have a user with this email
			// address and whether password login is enabled for him/her. Also
			// only send if the email address of the user has been verified.
			final String email = filledForm.get().email;

			// We don't want to expose whether a given email address is signed
			// up, so just say an email has been sent, even though it might not
			// be true - that's protecting our user privacy.
			flash(Application.FLASH_MESSAGE_KEY,
					Messages.get(
							"playauthenticate.reset_password.message.instructions_sent",
							email));

			final User user = User.findByEmail(email);
			if (user != null) {
				// yep, we have a user with this email that is active - we do
				// not know if the user owning that account has requested this
				// reset, though.
				final MyUsernamePasswordAuthProvider provider = MyUsernamePasswordAuthProvider
						.getProvider();
				// User exists
				if (user.emailValidated) {
					provider.sendPasswordResetMailing(user, ctx());
					// In case you actually want to let (the unknown person)
					// know whether a user was found/an email was sent, use,
					// change the flash message
				} else {
					// We need to change the message here, otherwise the user
					// does not understand whats going on - we should not verify
					// with the password reset, as a "bad" user could then sign
					// up with a fake email via OAuth and get it verified by an
					// a unsuspecting user that clicks the link.
					flash(Application.FLASH_MESSAGE_KEY,
							Messages.get("playauthenticate.reset_password.message.email_not_verified"));

					// You might want to re-send the verification email here...
					provider.sendVerifyEmailMailingAfterSignup(user, ctx());
				}
			}

			return redirect(routes.Application.index());
		}
	}

	/**
	 * Returns a token object if valid, null if not
	 * 
	 * @param token
	 * @param type
	 * @return
	 */
	private static TokenAction tokenIsValid(final String token, final Type type) {
		TokenAction ret = null;
		if (token != null && !token.trim().isEmpty()) {
			final TokenAction ta = TokenAction.findByToken(token, type);
			if (ta != null && ta.isValid()) {
				ret = ta;
			}
		}

		return ret;
	}

	public static Result resetPassword(final String token) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final TokenAction ta = tokenIsValid(token, Type.PASSWORD_RESET);
		if (ta == null) {
			return badRequest(no_token_or_invalid.render());
		}

		return ok(password_reset.render(PASSWORD_RESET_FORM
				.fill(new PasswordReset(token))));
	}

	public static Result doResetPassword() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final Form<PasswordReset> filledForm = PASSWORD_RESET_FORM
				.bindFromRequest();
		if (filledForm.hasErrors()) {
			return badRequest(password_reset.render(filledForm));
		} else {
			final String token = filledForm.get().token;
			final String newPassword = filledForm.get().password;

			final TokenAction ta = tokenIsValid(token, Type.PASSWORD_RESET);
			if (ta == null) {
				return badRequest(no_token_or_invalid.render());
			}
			final User u = ta.targetUser;
			try {
				// Pass true for the second parameter if you want to
				// automatically create a password and the exception never to
				// happen
				u.resetPassword(new MyUsernamePasswordAuthUser(newPassword),
						false);
			} catch (final RuntimeException re) {
				flash(Application.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.reset_password.message.no_password_account"));
			}
			final boolean login = MyUsernamePasswordAuthProvider.getProvider()
					.isLoginAfterPasswordReset();
			if (login) {
				// automatically log in
				flash(Application.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.reset_password.message.success.auto_login"));

				return PlayAuthenticate.loginAndRedirect(ctx(),
						new MyLoginUsernamePasswordAuthUser(u.email));
			} else {
				// send the user to the login page
				flash(Application.FLASH_MESSAGE_KEY,
						Messages.get("playauthenticate.reset_password.message.success.manual_login"));
			}
			return redirect(routes.Application.login());
		}
	}

	public static Result oAuthDenied(final String getProviderKey) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		return ok(oAuthDenied.render(getProviderKey));
	}

	public static Result exists() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		return ok(exists.render());
	}

	public static Result existsMail() {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		flash(Application.FLASH_ERROR_KEY, Messages.get("error.mailExists"));
		return redirect(routes.Application.login());
	}

	public static Result verify(final String token) {
		com.feth.play.module.pa.controllers.Authenticate.noCache(response());
		final TokenAction ta = tokenIsValid(token, Type.EMAIL_VERIFICATION);
		if (ta == null) {
			return badRequest(no_token_or_invalid.render());
		}
		final String email = ta.targetUser.email;
		User.verify(ta.targetUser);
		flash(Application.FLASH_MESSAGE_KEY,
				Messages.get("playauthenticate.verify_email.success", email));
		if (Application.getLocalUser(session()) != null) {
			return redirect(routes.Application.index());
		} else {
			return redirect(routes.Application.login());
		}
	}
}
