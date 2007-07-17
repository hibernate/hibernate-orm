package org.hibernate.jdbc;

import org.hibernate.HibernateException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;

/**
 * A proxy for <i>borrowed</i> connections which funnels all requests back
 * into the ConnectionManager from which it was borrowed to be properly
 * handled (in terms of connection release modes).
 * <p/>
 * Note: the term borrowed here refers to connection references obtained
 * via {@link org.hibernate.Session#connection()} for application usage.
 *
 * @author Steve Ebersole
 */
public class BorrowedConnectionProxy implements InvocationHandler {

	private static final Class[] PROXY_INTERFACES = new Class[] { Connection.class, ConnectionWrapper.class };

	private final ConnectionManager connectionManager;
	private boolean useable = true;

	public BorrowedConnectionProxy(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "close".equals( method.getName() ) ) {
			connectionManager.releaseBorrowedConnection();
			return null;
		}
		// should probably no-op commit/rollback here, at least in JTA scenarios
		if ( !useable ) {
			throw new HibernateException( "connnection proxy not usable after transaction completion" );
		}

		if ( "getWrappedConnection".equals( method.getName() ) ) {
			return connectionManager.getConnection();
		}

		try {
			return method.invoke( connectionManager.getConnection(), args );
		}
		catch( InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}

	/**
	 * Generates a Connection proxy wrapping the connection managed by the passed
	 * connection manager.
	 *
	 * @param connectionManager The connection manager to wrap with the
	 * connection proxy.
	 * @return The generated proxy.
	 */
	public static Connection generateProxy(ConnectionManager connectionManager) {
		BorrowedConnectionProxy handler = new BorrowedConnectionProxy( connectionManager );
		return ( Connection ) Proxy.newProxyInstance(
				getProxyClassLoader(),
		        PROXY_INTERFACES,
		        handler
		);
	}

	/**
	 * Marks a borrowed connection as no longer usable.
	 *
	 * @param connection The connection (proxy) to be marked.
	 */
	public static void renderUnuseable(Connection connection) {
		if ( connection != null && Proxy.isProxyClass( connection.getClass() ) ) {
			InvocationHandler handler = Proxy.getInvocationHandler( connection );
			if ( BorrowedConnectionProxy.class.isAssignableFrom( handler.getClass() ) ) {
				( ( BorrowedConnectionProxy ) handler ).useable = false;
			}
		}
	}

	/**
	 * Convience method for unwrapping a connection proxy and getting a
	 * handle to an underlying connection.
	 *
	 * @param connection The connection (proxy) to be unwrapped.
	 * @return The unwrapped connection.
	 */
	public static Connection getWrappedConnection(Connection connection) {
		if ( connection != null && connection instanceof ConnectionWrapper ) {
			return ( ( ConnectionWrapper ) connection ).getWrappedConnection();
		}
		else {
			return connection;
		}
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = BorrowedConnectionProxy.class.getClassLoader();
		}
		return cl;
	}
}
