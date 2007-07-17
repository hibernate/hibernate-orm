//$Id: ConnectionProvider.java 9191 2006-02-01 14:40:34Z epbernard $
package org.hibernate.connection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;

/**
 * A strategy for obtaining JDBC connections.
 * <br><br>
 * Implementors might also implement connection pooling.<br>
 * <br>
 * The <tt>ConnectionProvider</tt> interface is not intended to be
 * exposed to the application. Instead it is used internally by
 * Hibernate to obtain connections.<br>
 * <br>
 * Implementors should provide a public default constructor.
 *
 * @see ConnectionProviderFactory
 * @author Gavin King
 */
public interface ConnectionProvider {
	/**
	 * Initialize the connection provider from given properties.
	 * @param props <tt>SessionFactory</tt> properties
	 */
	public void configure(Properties props) throws HibernateException;
	/**
	 * Grab a connection, with the autocommit mode specified by
	 * <tt>hibernate.connection.autocommit</tt>.
	 * @return a JDBC connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException;
	/**
	 * Dispose of a used connection.
	 * @param conn a JDBC connection
	 * @throws SQLException
	 */
	public void closeConnection(Connection conn) throws SQLException;

	/**
	 * Release all resources held by this provider. JavaDoc requires a second sentence.
	 * @throws HibernateException
	 */
	public void close() throws HibernateException;

	/**
	 * Does this connection provider support aggressive release of JDBC
	 * connections and re-acquistion of those connections (if need be) later?
	 * <p/>
	 * This is used in conjunction with {@link org.hibernate.cfg.Environment.RELEASE_CONNECTIONS}
	 * to aggressively release JDBC connections.  However, the configured ConnectionProvider
	 * must support re-acquisition of the same underlying connection for that semantic to work.
	 * <p/>
	 * Typically, this is only true in managed environments where a container
	 * tracks connections by transaction or thread.
	 *
	 * Note that JTA semantic depends on the fact that the underlying connection provider does
	 * support aggressive release.
	 */
	public boolean supportsAggressiveRelease();
}







