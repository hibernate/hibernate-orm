/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.AbstractGeneratedValuesMutationDelegate;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Abstract {@link org.hibernate.generator.values.GeneratedValuesMutationDelegate} implementation where
 * the underlying strategy requires a subsequent {@code select} after the
 * {@code insert} to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectingDelegate extends AbstractGeneratedValuesMutationDelegate
		implements InsertGeneratedIdentifierDelegate {

	protected AbstractSelectingDelegate(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId) {
		super( persister, timing, supportsArbitraryValues, supportsRowId );
	}

	/**
	 * Get the SQL statement to be used to retrieve generated key values.
	 *
	 * @return The SQL command string
	 */
	protected abstract String getSelectSQL();

	protected void bindParameters(Object entity, PreparedStatement ps, SharedSessionContractImplementor session)
			throws SQLException {
	}

	/**
	 * Extract the generated key value from the given result set after execution of {@link #getSelectSQL()}.
	 */
	private GeneratedValues extractReturningValues(ResultSet resultSet, PreparedStatement statement, SharedSessionContractImplementor session)
			throws SQLException {
		return getGeneratedValues( resultSet, statement, persister, getTiming(), session );
	}

	@Override
	public PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getMutationStatementPreparer()
				.prepareStatement( insertSql, NO_GENERATED_KEYS );
	}

	@Override
	public GeneratedValues performMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings jdbcValueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var jdbcServices = session.getJdbcServices();

		jdbcServices.getSqlStatementLogger().logStatement( statementDetails.getSqlString() );

		try {
			jdbcValueBindings.beforeStatement( statementDetails );
			jdbcCoordinator.getResultSetReturn()
					.executeUpdate( statementDetails.resolveStatement(), statementDetails.getSqlString() );
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			jdbcValueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
		}

		// the insert is complete, select the generated id...

		final String idSelectSql = getSelectSQL();
		final var idSelect = jdbcCoordinator.getStatementPreparer().prepareStatement( idSelectSql );
		try {
			bindParameters( entity, idSelect, session );

			final ResultSet resultSet = jdbcCoordinator.getResultSetReturn().extract( idSelect, idSelectSql );
			try {
				return extractReturningValues( resultSet, idSelect, session );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to execute post-insert id selection query",
						idSelectSql
				);
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( idSelect );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to bind parameters for post-insert id selection query",
					idSelectSql
			);
		}
	}

	@Override
	public final GeneratedValues performInsertReturning(String sql, SharedSessionContractImplementor session, Binder binder) {
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var statementPreparer = jdbcCoordinator.getStatementPreparer();
		try {
			// prepare and execute the insert
			final var insert = statementPreparer.prepareStatement( sql, NO_GENERATED_KEYS );
			try {
				binder.bindValues( insert );
				jdbcCoordinator.getResultSetReturn().executeUpdate( insert, sql );
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insert );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + infoString( persister ),
					sql
			);
		}

		final String selectSQL = getSelectSQL();

		try {
			//fetch the generated id in a separate query
			final var idSelect = statementPreparer.prepareStatement( selectSQL );
			try {
				bindParameters( binder.getEntity(), idSelect, session );
				final ResultSet resultSet = jdbcCoordinator.getResultSetReturn().extract( idSelect, selectSQL );
				try {
					return extractReturningValues( resultSet, idSelect, session );
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( resultSet, idSelect );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( idSelect );
				jdbcCoordinator.afterStatementExecution();
			}

		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not retrieve generated id after insert: " + infoString( persister ),
					selectSQL
			);
		}
	}

}
