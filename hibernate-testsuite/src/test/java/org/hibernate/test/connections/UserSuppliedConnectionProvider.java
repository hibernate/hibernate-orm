package org.hibernate.test.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class UserSuppliedConnectionProvider implements ConnectionProvider {
	private final ConnectionProvider cp;
	private String myData;
	private int invocationCount;

	public UserSuppliedConnectionProvider(ConnectionProvider internalProvider, String myData) {
		this.cp = internalProvider;
		this.myData = myData;
		this.invocationCount = 0;
	}

	public void configure(Properties props) throws HibernateException {
		++invocationCount;
		cp.configure( props );
	}

	public Connection getConnection() throws SQLException {
		++invocationCount;
		return cp.getConnection();
	}

	public void closeConnection(Connection conn) throws SQLException {
		++invocationCount;
		cp.closeConnection( conn );
	}

	public boolean supportsAggressiveRelease() {
		++invocationCount;
		return cp.supportsAggressiveRelease();
	}

	public void close() throws HibernateException {
		++invocationCount;
		cp.close();
	}

	public String getMyData() {
		return myData;
	}

	public int getInvocationCount() {
		return invocationCount;
	}
}
