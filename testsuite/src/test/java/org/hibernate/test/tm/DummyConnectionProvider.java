//$Id: DummyConnectionProvider.java 6501 2005-04-24 00:18:28Z oneovthafew $
package org.hibernate.test.tm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * @author Gavin King
 */
public class DummyConnectionProvider implements ConnectionProvider {
	
	ConnectionProvider cp;
	boolean isTransaction;

	public void configure(Properties props) throws HibernateException {
		cp = ConnectionProviderFactory.newConnectionProvider();
	}
	
	public Connection getConnection() throws SQLException {
		DummyTransactionManager dtm = DummyTransactionManager.INSTANCE;
		if ( dtm!=null && dtm.getCurrent()!=null && dtm.getCurrent().getConnection()!=null ) {
			isTransaction = true;
			return dtm.getCurrent().getConnection();
		}
		else {
			isTransaction = false;
			return cp.getConnection();
		}
	}

	public void closeConnection(Connection conn) throws SQLException {
		if (!isTransaction) conn.close();
	}

	public void close() throws HibernateException {

	}

	public boolean supportsAggressiveRelease() {
		return true;
	}

}
