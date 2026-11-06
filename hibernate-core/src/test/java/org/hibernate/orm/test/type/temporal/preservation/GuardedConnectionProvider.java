package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.testing.jdbc.GradleParallelTestingResolver;

/// A non-pooling test provider that cannot initialize a connection behind the
/// bootstrap guard, even when Hibernate catches the acquisition exception.
///
/// @author Steve Ebersole
public final class GuardedConnectionProvider implements ConnectionProvider, Configurable {
	boolean blocked;
	int blockedAttempts;
	int runtimeAcquisitions;
	int temporalBindings;
	private Map<String, Object> settings;

	GuardedConnectionProvider(boolean blocked) {
		this.blocked = blocked;
	}

	@Override public Connection getConnection() throws SQLException {
		if ( blocked ) {
			blockedAttempts++;
			throw new SQLException( "Temporal preservation bootstrap must not acquire a connection" );
		}
		runtimeAcquisitions++;
		try {
			Class.forName( (String) settings.get( "hibernate.connection.driver_class" ) );
		}
		catch (ClassNotFoundException ex) {
			throw new SQLException( ex );
		}
		final var properties = ConnectionProviderInitiator.getConnectionProperties( settings );
		GradleParallelTestingResolver.resolve( properties );
		final String url = System.getProperty( "hibernate.test.temporal.matrix_url",
				(String) settings.get( "hibernate.connection.url" ) );
		final var connection = DriverManager.getConnection( GradleParallelTestingResolver.resolveUrl( url ), properties );
		connection.setAutoCommit( false );
		final String sql = (String) settings.get( "hibernate.connection.init_sql" );
		if ( sql != null && !sql.isBlank() ) {
			try ( var statement = connection.createStatement() ) {
				statement.execute( sql );
			}
		}
		return (Connection) java.lang.reflect.Proxy.newProxyInstance( getClass().getClassLoader(), new Class<?>[] { Connection.class },
				(proxy, method, arguments) -> {
					try {
						final Object result = method.invoke( connection, arguments );
						if ( result instanceof java.sql.PreparedStatement prepared ) {
							final var temporalParameters = temporalParameters( arguments != null && arguments.length > 0 && arguments[0] instanceof String preparedSql ? preparedSql : "" );
							return java.lang.reflect.Proxy.newProxyInstance( getClass().getClassLoader(),
									new Class<?>[] { result instanceof java.sql.CallableStatement ? java.sql.CallableStatement.class : java.sql.PreparedStatement.class },
									(statementProxy, statementMethod, statementArguments) -> {
										if ( statementMethod.getName().startsWith( "set" ) && statementArguments != null && statementArguments.length >= 2
												&& statementArguments[0] instanceof Integer index && temporalParameters.contains( index ) ) temporalBindings++;
										try { return statementMethod.invoke( prepared, statementArguments ); }
										catch (java.lang.reflect.InvocationTargetException ex) { throw ex.getCause(); }
									} );
						}
						return result;
					}
					catch (java.lang.reflect.InvocationTargetException ex) { throw ex.getCause(); }
				} );
	}


	@Override public void configure(Map<String, Object> settings) { this.settings = new HashMap<>( settings ); }
	@Override public void closeConnection(Connection connection) throws SQLException { connection.close(); }
	@Override public boolean supportsAggressiveRelease() { return false; }
	@Override public boolean isUnwrappableAs(Class<?> type) { return type.isInstance( this ); }
	@Override public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) return type.cast( this );
		throw new UnknownUnwrapTypeException( type );
	}
	private static java.util.Set<Integer> temporalParameters(String sql) {
		// Fixture inserts use simple column/value lists. Count only temporal
		// parameters, so unrelated entity-property binding order is irrelevant.
		final var matcher = java.util.regex.Pattern.compile( "(?is)insert\\s+into\\s+[^()]+\\(([^)]*)\\)\\s+values\\s*\\(([^)]*)\\)" ).matcher( sql );
		final java.util.Set<Integer> indices = new java.util.HashSet<>();
		if ( !matcher.find() ) return indices;
		final String[] columns = matcher.group( 1 ).split( "," ), values = matcher.group( 2 ).split( "," );
		int parameter = 0;
		for ( int i = 0; i < columns.length; i++ ) {
			final int count = (int) values[i].chars().filter( c -> c == '?' ).count();
			if ( columns[i].trim().equalsIgnoreCase( "temporal_value" ) || columns[i].trim().equalsIgnoreCase( "offset_seconds" ) ) {
				for ( int n = 1; n <= count; n++ ) indices.add( parameter + n );
			}
			parameter += count;
		}
		return indices;
	}

}
