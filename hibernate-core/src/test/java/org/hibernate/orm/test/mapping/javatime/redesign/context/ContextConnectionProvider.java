package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.*;
import java.util.*;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.testing.jdbc.GradleParallelTestingResolver;

/// Lazy physical connections make all bootstrap acquisition attempts observable.
///
/// @author Steve Ebersole
final class ContextConnectionProvider implements ConnectionProvider, Configurable {
	boolean blocked;
	int attempts;
	private Map<String,Object> settings;
	ContextConnectionProvider(boolean blocked) { this.blocked = blocked; }
	@Override public void configure(Map<String,Object> settings) { this.settings = new HashMap<>( settings ); }
	@Override public Connection getConnection() throws SQLException {
		attempts++;
		if ( blocked ) throw new SQLException( "Context bootstrap attempted JDBC access" );
		try { Class.forName( (String) settings.get( "hibernate.connection.driver_class" ) ); }
		catch (ClassNotFoundException ex) { throw new SQLException( ex ); }
		final var properties = ConnectionProviderInitiator.getConnectionProperties( settings );
		GradleParallelTestingResolver.resolve( properties );
		final var connection = DriverManager.getConnection( GradleParallelTestingResolver.resolveUrl( (String) settings.get( "hibernate.connection.url" ) ), properties );
		connection.setAutoCommit( false );
		final var init = settings.get( "hibernate.connection.init_sql" );
		if ( init != null ) try ( var statement = connection.createStatement() ) { statement.execute( init.toString() ); }
		return connection;
	}
	@Override public void closeConnection(Connection connection) throws SQLException { connection.close(); }
	@Override public boolean supportsAggressiveRelease() { return false; }
	@Override public boolean isUnwrappableAs(Class<?> type) { return type.isInstance( this ); }
	@Override public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) return type.cast( this );
		throw new UnknownUnwrapTypeException( type );
	}
}
