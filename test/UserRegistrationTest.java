import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.Test;

import play.db.DB;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

public class UserRegistrationTest {

	// @Test
	public void registerExistingNickName() {
		running(testServer(3333, fakeApplication()), new Runnable() {

			// running(testServer(3333), new Runnable() {
			@Override
			public void run() {
				final WSRequestHolder request = WS
						.url("http://localhost:3333/signup");
				request.setQueryParameter("name", "testName");
				request.setQueryParameter("email", "test@test.it");
				request.setQueryParameter("password", "test123");
				request.setQueryParameter("RepeatPassword", "test123");
				request.setQueryParameter("nation", "Italy");
				request.setQueryParameter("gender", "true");
				request.setQueryParameter("accept_terms", "true");

				final Promise<Response> response = request.post("content");
				System.out.println("status : " + response.get().getStatus());
				assertThat(response.get().getStatus()).isEqualTo(200);
				assertThat(response.get().getBody()
						.contains("Verify your e-mail"));
				System.out
						.println("The registration ended with no errors, the ");
			}
		});
	}

	@Test
	public void registerExistingEmail() {
		running(testServer(3333, fakeApplication()), new Runnable() {

			// running(testServer(3333), new Runnable() {
			@Override
			public void run() {

				// ---------------- basic registration -------------
				System.out
						.println("Registering a new User, everything correct.");
				Response r = sendRequest4registerUser("testName",
						"test@test.it", "test123");
				System.out.println("status : " + r.getStatus());
				assertThat(r.getStatus()).isEqualTo(200);
				assertThat(r.getBody().contains("Verify your e-mail"));
				System.out
						.println("The registration ended with no errors, the backend returns a request to validate the page");

				// ---------------- registration with wrong nickname
				// -------------
				System.out
						.println("Registering a new User, nickname already used.");
				r = sendRequest4registerUser("testName", "test@test.it",
						"test123");
				System.out.println("status : " + r.getStatus());
				assertThat(r.getStatus()).isEqualTo(200);
				// assertThat(r.getBody().contains("Verify your e-mail"));
				System.out.println("body " + r.getBody());
				System.out
						.println("The registration ended with no errors, the backend returns a request to validate the page");

				// ---------------- basic registration -------------
				// System.out
				// .println("Registering a new User, everything correct.");
				// final Response r = sendRequest4registerUser("testName",
				// "test@test.it", "test123");
				// System.out.println("status : " + r.getStatus());
				// assertThat(r.getStatus()).isEqualTo(200);
				// assertThat(r.getBody().contains("Verify your e-mail"));
				// System.out
				// .println("The registration ended with no errors, the backend returns a request to validate the page");
			}
		});

	}

	private Response sendRequest4registerUser(final String name,
			final String email, final String psw) {

		final WSRequestHolder request = WS.url("http://localhost:3333/signup");
		request.setQueryParameter("name", name);
		request.setQueryParameter("email", email);
		request.setQueryParameter("password", psw);
		request.setQueryParameter("RepeatPassword", psw);
		request.setQueryParameter("nation", "Italy");
		request.setQueryParameter("gender", "true");
		request.setQueryParameter("accept_terms", "true");

		final Promise<Response> response = request.post("content");
		return response.get();

	}

	// @Test
	public void registerGuest() {
		running(testServer(3333, fakeApplication()), new Runnable() {

			// running(testServer(3333), new Runnable() {
			@Override
			public void run() {
				// assertThat(
				// WS.url("http://localhost:3333").get().get().getStatus())
				// .isEqualTo(OK);
				final WSRequestHolder request = WS
						.url("http://localhost:3333/signup");
				request.setQueryParameter("name", "testName");
				request.setQueryParameter("email", "test@test.it");
				request.setQueryParameter("password", "test123");
				request.setQueryParameter("RepeatPassword", "test123");
				request.setQueryParameter("nation", "Italy");
				request.setQueryParameter("gender", "true");
				request.setQueryParameter("accept_terms", "true");

				final Promise<Response> response = request.post("content");
				System.out.println("status : " + response.get().getStatus());
				assertThat(response.get().getStatus()).isEqualTo(200);
				assertThat(response.get().getBody()
						.contains("Verify your e-mail"));
				System.out
						.println("The registration ended with no errors, the ");
			}
		});
	}

	// @Test
	public void registerUser() {

		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				System.out.println("sono dentroooooo");

				assertThat(isSaved("pippo"));

			}

			private Boolean isSaved(final String user) {
				Connection connection = null;
				connection = DB.getConnection();
				final Boolean result = isSaved2(user, connection);

				try {
					if (connection != null)
						connection.close();
				} catch (final SQLException e) {
					play.Logger.error("Unable to close a SQL connection.");
				}

				return result;

			}
		});

	}

	private Boolean isSaved2(final String user, final Connection connection) {
		PreparedStatement statement = null;
		try {

			final String query = "SELECT * FROM USERS";
			statement = connection.prepareStatement(query);

			final ResultSet rs = statement.executeQuery();

			final ResultSetMetaData rsmd = rs.getMetaData();

			final int numberOfColumns = rsmd.getColumnCount();

			for (int i = 1; i <= numberOfColumns; i++) {
				if (i > 1)
					System.out.print(",  ");
				final String columnName = rsmd.getColumnName(i);
				System.out.print(columnName);
				if (columnName.equals(user)) {
					return true;
				}
			}
			System.out.println("");

			while (rs.next()) {
				System.out.println(rs.getString("NAME"));
			}

		} catch (final SQLException ex) {
		} finally {
			try {
				if (statement != null)
					statement.close();

			} catch (final SQLException e) {
				play.Logger.error("Unable to close a SQL connection.");
			}

		}
		return false;

	}
}
