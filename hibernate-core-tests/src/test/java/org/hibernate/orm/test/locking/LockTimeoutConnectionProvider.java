package org.hibernate.orm.test.locking;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.orm.test.resource.ConnectionProviderDelegate;

/// A single-physical-connection fixture with observable JDBC ownership. Tests
/// run on one thread; aggressive release reacquires the same transaction-bound
/// connection. A returned connection is never reset before assertions inspect it.
///
/// @author Steve Ebersole
public class LockTimeoutConnectionProvider extends ConnectionProviderDelegate {
	private Connection physical;
	private Connection connection;
	final List<String> events = new ArrayList<>();
	final List<Connection> borrowedConnections = new ArrayList<>();
	final List<Connection> returnedConnections = new ArrayList<>();
	final List<Resources> resources = new ArrayList<>();
	Consumer<Connection> onBorrow = c -> {};
	Consumer<Connection> onReturn = c -> {};
	Consumer<Connection> onExecute = c -> {};
	RuntimeException nextFailure;
	Error closeFailure;
	private Thread owner;

	public LockTimeoutConnectionProvider() {
		super( true );
	}

	@Override
	public synchronized Connection getConnection() throws SQLException {
		if ( owner == null ) {
			owner = Thread.currentThread();
		}
		if ( owner != Thread.currentThread() ) {
			throw new IllegalStateException( "The fixture reserves its connection for one test thread" );
		}
		if ( connection == null ) {
			physical = super.getConnection();
			connection = proxy( Connection.class, (method, args) -> {
				final var result = invoke( physical, method, args );
				if ( method.getName().equals( "prepareStatement" ) && args[0] instanceof String sql
						&& sql.toLowerCase( java.util.Locale.ROOT ).startsWith( "select" )
						&& sql.contains( "timeout_lifecycle_item" ) ) {
					final var resource = new Resources();
					resources.add( resource );
					return statement( (PreparedStatement) result, resource );
				}
				return result;
			} );
		}
		events.add( "borrow" );
		borrowedConnections.add( physical.unwrap( Connection.class ) );
		onBorrow.accept( connection );
		return connection;
	}

	@Override
	public void closeConnection(Connection returned) throws SQLException {
		if ( returned != connection ) {
			throw new IllegalArgumentException( "Unexpected physical connection" );
		}
		events.add( "return" );
		returnedConnections.add( physical.unwrap( Connection.class ) );
		onReturn.accept( connection );
	}

	@Override
	public void stop() {
		try {
			if ( physical != null ) {
				super.closeConnection( physical );
				physical = null;
			}
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}
		finally {
			super.stop();
		}
	}

	void resetObservations() {
		events.clear();
		borrowedConnections.clear();
		returnedConnections.clear();
		resources.clear();
		nextFailure = null;
		closeFailure = null;
		onBorrow = c -> {};
		onReturn = c -> {};
		onExecute = c -> {};
	}

	private PreparedStatement statement(PreparedStatement statement, Resources resource) {
		return proxy( PreparedStatement.class, (method, args) -> {
			if ( method.getName().equals( "close" ) ) {
				resource.statementCloses++;
			}
			if ( method.getName().equals( "executeQuery" ) ) {
				onExecute.accept( connection );
			}
			final var result = invoke( statement, method, args );
			if ( method.getName().equals( "executeQuery" ) ) {
				events.add( "execute" );
				return resultSet( (ResultSet) result, resource );
			}
			if ( method.getName().equals( "close" ) && closeFailure != null ) {
				throw closeFailure;
			}
			return result;
		} );
	}

	private ResultSet resultSet(ResultSet rows, Resources resource) {
		return proxy( ResultSet.class, (method, args) -> {
			if ( method.getName().equals( "next" ) && nextFailure != null ) {
				throw nextFailure;
			}
			if ( method.getName().equals( "close" ) ) {
				resource.resultSetCloses++;
			}
			return invoke( rows, method, args );
		} );
	}

	private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke( target, args );
		}
		catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private static <T> T proxy(Class<T> type, Invocation invocation) {
		return type.cast( Proxy.newProxyInstance( type.getClassLoader(), new Class<?>[] { type },
				(proxy, method, args) -> invocation.call( method, args ) ) );
	}

	private interface Invocation {
		Object call(Method method, Object[] args) throws Throwable;
	}

	static class Resources {
		int statementCloses;
		int resultSetCloses;
	}
}
