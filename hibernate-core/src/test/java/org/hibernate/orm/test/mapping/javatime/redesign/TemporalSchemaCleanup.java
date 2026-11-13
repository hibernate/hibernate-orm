package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.Metadata;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.GenerationTarget;

/// Idempotent cleanup of a fixture's mapped objects, without suppressing unrelated DDL errors.
///
/// @author Steve Ebersole
public final class TemporalSchemaCleanup {
	private TemporalSchemaCleanup() {}

	public static void drop(Metadata metadata, ServiceRegistry registry, Connection connection) throws SQLException {
		new SchemaDropperImpl( registry ).doDrop( metadata, false, new GenerationTarget() {
			@Override public void prepare() {}
			@Override public void release() {}
			@Override public void accept(String command) {
				try { dropStatement( connection, command ); }
				catch (SQLException failure) { throw new IllegalStateException( "Could not clean owned fixture", failure ); }
			}
		} );
		connection.commit();
	}

	public static void dropStatement(Connection connection, String sql) throws SQLException {
		final boolean oracle = connection.getMetaData().getDatabaseProductName().equals( "Oracle" );
		if ( !oracle && sql.startsWith( "drop table " ) && !sql.startsWith( "drop table if exists " ) ) {
			sql = "drop table if exists " + sql.substring( "drop table ".length() );
		}
		try ( var statement = connection.createStatement() ) {
			statement.execute( sql );
		}
		catch (SQLException failure) {
			// Oracle before 23 has no DROP ... IF EXISTS. Ignore only absent owned
			// tables, sequences, constraints or types, never other DDL failures.
			if ( !oracle || switch ( failure.getErrorCode() ) {
				case 942, 2289, 2443, 4043 -> false;
				default -> true;
			} ) throw failure;
		}
	}
}
