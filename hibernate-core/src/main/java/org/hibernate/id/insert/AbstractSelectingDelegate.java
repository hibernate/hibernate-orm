/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.AbstractGeneratedValuesMutationDelegate;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;

/**
 * Abstract {@link org.hibernate.generator.values.GeneratedValuesMutationDelegate} implementation where
 * the underlying strategy requires a subsequent {@code select} after the
 * {@code insert} to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectingDelegate extends AbstractGeneratedValuesMutationDelegate
		implements InsertGeneratedIdentifierDelegate {
	/**
	 * @deprecated Use {@link #AbstractSelectingDelegate(EntityPersister, EventType, boolean, boolean)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	protected AbstractSelectingDelegate(PostInsertIdentityPersister persister) {
		super( persister, EventType.INSERT );
	}

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
	 * @deprecated No substitute.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	protected Object extractGeneratedValues(ResultSet resultSet, SharedSessionContractImplementor session)
			throws SQLException {
		final GeneratedValues generatedValues = extractReturningValues( resultSet, session );
		return generatedValues.getGeneratedValue( persister.getIdentifierMapping() );
	}

	/**
	 * Extract the generated key value from the given result set after execution of {@link #getSelectSQL()}.
	 */
	private GeneratedValues extractReturningValues(ResultSet resultSet, SharedSessionContractImplementor session)
			throws SQLException {
		return getGeneratedValues( resultSet, persister, getTiming(), session );
	}

	@Override
	public PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getMutationStatementPreparer().prepareStatement( insertSql, NO_GENERATED_KEYS );
	}

	@Override
	public GeneratedValues performMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings jdbcValueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

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
		final PreparedStatement idSelect = jdbcCoordinator.getStatementPreparer().prepareStatement( idSelectSql );
		try {
			bindParameters( entity, idSelect, session );

			final ResultSet resultSet = session.getJdbcCoordinator().getResultSetReturn().extract( idSelect, idSelectSql );
			try {
				return extractReturningValues( resultSet, session );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to execute post-insert id selection query",
						idSelectSql
				);
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( idSelect );
				session.getJdbcCoordinator().afterStatementExecution();
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
		JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		StatementPreparer statementPreparer = jdbcCoordinator.getStatementPreparer();
		try {
			// prepare and execute the insert
			PreparedStatement insert = statementPreparer.prepareStatement( sql, NO_GENERATED_KEYS );
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
					"could not insert: " + MessageHelper.infoString( persister ),
					sql
			);
		}

		final String selectSQL = getSelectSQL();

		try {
			//fetch the generated id in a separate query
			PreparedStatement idSelect = statementPreparer.prepareStatement( selectSQL, false );
			try {
				bindParameters( binder.getEntity(), idSelect, session );
				ResultSet resultSet = jdbcCoordinator.getResultSetReturn().extract( idSelect, selectSQL );
				try {
					return extractReturningValues( resultSet, session );
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
					"could not retrieve generated id after insert: " + MessageHelper.infoString( persister ),
					selectSQL
			);
		}
	}

}
