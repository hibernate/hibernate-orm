package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.hibernate.HibernateException;
import org.hibernate.service.spi.Stoppable;
import java.util.*;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.testing.jdbc.GradleParallelTestingResolver;

/// Lazy physical connections make all bootstrap acquisition attempts observable.
///
/// @author Steve Ebersole
final class ContextConnectionProvider implements ConnectionProvider, Configurable, Stoppable {
	boolean blocked;
	int attempts;
	final List<String> callableMethods = new ArrayList<>();
	private Map<String,Object> settings;
	private Connection idle;
	private final ConnectionFactory connectionFactory;

	ContextConnectionProvider(boolean blocked) {
		this( blocked, ContextConnectionProvider::connect );
	}

	ContextConnectionProvider(boolean blocked, ConnectionFactory connectionFactory) {
		this.blocked = blocked;
		this.connectionFactory = connectionFactory;
	}

	@FunctionalInterface
	interface ConnectionFactory {
		Connection open(Map<String, Object> settings) throws SQLException;
	}

	private static Connection connect(Map<String, Object> settings) throws SQLException {
		try { Class.forName( (String) settings.get( "hibernate.connection.driver_class" ) ); }
		catch (ClassNotFoundException ex) { throw new SQLException( ex ); }
		final var properties = ConnectionProviderInitiator.getConnectionProperties( settings );
		GradleParallelTestingResolver.resolve( properties );
		return DriverManager.getConnection( GradleParallelTestingResolver.resolveUrl(
				(String) settings.get( "hibernate.connection.url" ) ), properties );
	}
	@Override public void configure(Map<String,Object> settings) { this.settings = new HashMap<>( settings );
		this.settings.putIfAbsent( "hibernate.connection.url", settings.get( "jakarta.persistence.jdbc.url" ) );
		this.settings.putIfAbsent( "hibernate.connection.driver_class", settings.get( "jakarta.persistence.jdbc.driver" ) );
		this.settings.putIfAbsent( "hibernate.connection.username", settings.get( "jakarta.persistence.jdbc.user" ) );
		this.settings.putIfAbsent( "hibernate.connection.password", settings.get( "jakarta.persistence.jdbc.password" ) ); }
	@Override public Connection getConnection() throws SQLException {
		attempts++;
		if ( blocked ) throw new SQLException( "Context bootstrap attempted JDBC access" );
		final Connection connection;
		final boolean reused = idle != null;
		if ( reused ) {
			connection = idle;
			idle = null;
		}
		else {
			connection = connectionFactory.open( settings );
		}
		try {
			initialize( connection );
			return reused ? connection : observed( connection );
		}
		catch (SQLException | RuntimeException | Error failure) {
			try { connection.close(); }
			catch (SQLException cleanup) { failure.addSuppressed( cleanup ); }
			throw failure;
		}
	}

	private void initialize(Connection connection) throws SQLException {
		for ( String part : List.of( "catalog", "schema" ) ) {
			final Object expected = settings.get( "hibernate.test.temporal.expected_" + part );
			if ( expected != null && !expected.equals( part.equals( "catalog" ) ? connection.getCatalog() : connection.getSchema() ) ) {
				throw new SQLException( "Child connection does not belong to the parent's " + part );
			}
		}
		connection.setAutoCommit( false );
		final var init = settings.get( "hibernate.connection.init_sql" );
		if ( init != null && !init.toString().isBlank() ) {
			try ( var statement = connection.createStatement() ) { statement.execute( init.toString() ); }
			// PostgreSQL SET is transactional. Pool cleanup must not undo the requested
			// session environment when the metadata-bootstrap connection is returned.
			connection.commit();
		}
	}

	private Connection observed(Connection connection) {
		if ( Boolean.TRUE.equals( settings.get( "hibernate.test.temporal.observe_callable" ) ) ) {
			return (Connection) Proxy.newProxyInstance( Connection.class.getClassLoader(),
					new Class<?>[] { Connection.class }, (proxy, method, args) -> {
						if ( method.getName().equals( "prepareCall" ) ) {
							callableMethods.add( "prepareCall" );
						}
						final var result = invoke( connection, method, args );
						return method.getName().equals( "prepareCall" ) ? observe( (CallableStatement) result ) : result;
					} );
		}
		return connection;
	}
	// Observe the actual driver boundary, including named-to-positional adaptation.
	// Delegate to the live driver and retain its original exception/cause.
	private CallableStatement observe(CallableStatement statement) {
		return (CallableStatement) Proxy.newProxyInstance( CallableStatement.class.getClassLoader(),
				new Class<?>[] { CallableStatement.class }, (proxy, method, args) -> {
					final var name = method.getName();
					if ( name.startsWith( "set" ) || name.startsWith( "get" )
							|| name.equals( "registerOutParameter" ) || name.startsWith( "execute" ) ) {
						callableMethods.add( name + (args == null || args.length == 0 ? "" : ":" + args[0]) );
					}
					return invoke( statement, method, args );
				} );
	}
	private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
		try { return method.invoke( target, args ); }
		catch (InvocationTargetException failure) { throw failure.getCause(); }
	}
	// Optional single idle connection avoids exhausting Oracle Free's server processes
	// during these sequential operation matrices. Existing isolation tests do not opt in.
	@Override public void closeConnection(Connection connection) throws SQLException {
		if ( Boolean.TRUE.equals(settings.get("hibernate.test.temporal.reuse_connection")) && idle==null ) {
			try {
				connection.rollback();
				idle = connection;
			}
			catch (SQLException | RuntimeException | Error failure) {
				try { connection.close(); }
				catch (SQLException cleanup) { failure.addSuppressed( cleanup ); }
				throw failure;
			}
		}
		else connection.close();
	}
	@Override public void stop() {
		if(idle!=null) {
			try {idle.close();} catch(SQLException e) {throw new HibernateException("Could not close test connection",e);}
			finally {idle=null;}
		}
	}
	@Override public boolean supportsAggressiveRelease() { return false; }
	@Override public boolean isUnwrappableAs(Class<?> type) { return type.isInstance( this ); }
	@Override public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) return type.cast( this );
		throw new UnknownUnwrapTypeException( type );
	}
}
