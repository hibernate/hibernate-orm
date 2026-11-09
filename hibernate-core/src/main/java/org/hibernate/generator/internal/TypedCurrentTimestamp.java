package org.hibernate.generator.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Executes a provider-declared current temporal selection through its exact JDBC pair.
///
/// @author Steve Ebersole
public final class TypedCurrentTimestamp {
	private TypedCurrentTimestamp() {}

	public static <T> T generate(SharedSessionContractImplementor session, JavaType<T> javaType, JdbcType jdbcType) {
		final var selection = session.getJdbcServices().getDialect().getCurrentTemporalSupport()
				.getCurrentTimestampSelection( javaType, jdbcType );
		if ( selection == null ) {
			throw new MappingException( "No preserving database timestamp selection for "
					+ javaType.getJavaTypeClass().getName() + " / " + jdbcType.getClass().getName() );
		}
		return generate( session, javaType, jdbcType, selection );
	}

	public static <T> T generate(SharedSessionContractImplementor session, JavaType<T> javaType,
			JdbcType jdbcType, CurrentTimestampSelection selection) {
		final var coordinator = session.getJdbcCoordinator();
		PreparedStatement statement = null;
		try {
			statement = coordinator.getStatementPreparer().prepareStatement( selection.command(), selection.callable() );
			final var extractor = jdbcType.getExtractor( javaType );
			if ( selection.callable() ) {
				final var callable = (CallableStatement) statement;
				jdbcType.registerOutParameter( callable, 1 );
				coordinator.getResultSetReturn().execute( callable, selection.command() );
				return extractor.extract( callable, 1, session );
			}
			final var results = coordinator.getResultSetReturn().extract( statement, selection.command() );
			if ( !results.next() ) {
				throw new org.hibernate.HibernateException( "Database current timestamp selection returned no row" );
			}
			return extractor.extract( results, 1, session );
		}
		catch (SQLException ex) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					ex, "Could not obtain preserving database timestamp", selection.command() );
		}
		finally {
			if ( statement != null ) {
				coordinator.getLogicalConnection().getResourceRegistry().release( statement );
				coordinator.afterStatementExecution();
			}
		}
	}
}
