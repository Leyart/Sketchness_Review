package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import play.db.DB;

/**
 * Created with IntelliJ IDEA. User: Riboni1989 Date: 28/10/13 Time: 14:51 To
 * change this template use File | Settings | File Templates.
 */
public class IsOnline {

	public static void checkOnline() {

		Date ora;
		java.util.Date utilDate;
		Connection connection = null;
		PreparedStatement statement = null;
		PreparedStatement statement1 = null;
		ResultSet rs = null;
		try {
			connection = DB.getConnection();
			final String query = "SELECT * FROM USERS WHERE ONLINE=? ";
			final String query1 = "UPDATE USERS SET ONLINE = ? WHERE NAME = ? ";

			statement = connection.prepareStatement(query);
			statement.setBoolean(1, true);

			rs = statement.executeQuery();

			if (rs.next()) {
				ora = new Date();
				utilDate = rs.getTimestamp("LAST_ACTIVE");
				// 1 sec X 60 sec X 2 = 2 minuti
				if (ora.getTime() - utilDate.getTime() >= 1000 * 60 * 2) {

					try {
						statement1 = connection.prepareStatement(query1);

						statement1.setBoolean(1, false);
						statement1.setString(2, rs.getString("NAME"));
						statement1.executeUpdate();

					} catch (final SQLException ex) {
						play.Logger.error("Unable to update status for user:"
								+ rs.getString("NAME"), ex);
					} finally {
						if (rs != null) {
							rs.close();
						}
						if (statement1 != null)
							statement1.close();
					}
				}

			}

		} catch (final SQLException ex) {
			play.Logger.error("Unable to open a SQL connection", ex);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (statement != null)
					statement.close();
				if (connection != null)
					connection.close();
			} catch (final SQLException e) {
				play.Logger.error("Unable to close a SQL connection.");
			}
		}
	}

	public static void keepOnline(final String name) {

		final Date utilDate = new Date();
		final Timestamp active = new java.sql.Timestamp(utilDate.getTime());
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = DB.getConnection();
			final String query = "UPDATE USERS SET LAST_ACTIVE = ?, ONLINE = ? WHERE NAME = ? ";

			statement = connection.prepareStatement(query);

			statement.setTimestamp(1, active);
			statement.setBoolean(2, true);
			statement.setString(3, name);
			statement.executeUpdate();

			connection.close();
		} catch (final SQLException ex) {
			play.Logger.error("Unable to update user status: " + name, ex);
		} finally {
			try {
				if (statement != null)
					statement.close();

				if (connection != null)
					connection.close();
			} catch (final SQLException e) {
				play.Logger.error("Unable to close a SQL connection.");
			}
		}
	}

	public static void putOffline(final String name) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = DB.getConnection();
			final String query = "UPDATE USERS SET ONLINE = ? WHERE NAME = ? ";

			statement = connection.prepareStatement(query);

			statement.setBoolean(1, false);
			statement.setString(2, name);
			statement.executeUpdate();

			connection.close();
		} catch (final SQLException ex) {
			play.Logger.error("Unable to put offline user:" + name, ex);
		} finally {
			try {
				if (statement != null)
					statement.close();

				if (connection != null)
					connection.close();
			} catch (final SQLException e) {
				play.Logger.error("Unable to close a SQL connection.");
			}
		}

	}

}
