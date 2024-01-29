/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.AbstractGeneratedValuesMutationDelegate;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract {@link org.hibernate.generator.values.GeneratedValuesMutationDelegate} implementation where
 * the underlying strategy causes the generated identifier to be returned as
 * an effect of performing the insert statement.  Thus, there is no need for
 * an additional sql statement to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractReturningDelegate extends AbstractGeneratedValuesMutationDelegate
		implements InsertGeneratedIdentifierDelegate {
	/**
	 * @deprecated Use {@link #AbstractReturningDelegate(EntityPersister, EventType, boolean, boolean)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	public AbstractReturningDelegate(PostInsertIdentityPersister persister) {
		super( persister, EventType.INSERT );
	}

	public AbstractReturningDelegate(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId) {
		super( persister, timing, supportsArbitraryValues, supportsRowId );
	}

	@Override
	public GeneratedValues performMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings valueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );
		try {
			valueBindings.beforeStatement( statementDetails );
			return executeAndExtractReturning(
					statementDetails.getSqlString(),
					statementDetails.getStatement(),
					session
			);
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			valueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
			session.getJdbcCoordinator().afterStatementExecution();
		}
	}

	@Override
	public final GeneratedValues performInsertReturning(String sql, SharedSessionContractImplementor session, Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = prepareStatement( sql, session );
			try {
				binder.bindValues( insert );
				return executeAndExtractReturning( sql, insert, session );
			}
			finally {
				releaseStatement( insert, session );
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
					sql
			);
		}
	}

	/**
	 * @deprecated No longer used
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	protected Object executeAndExtract(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		final GeneratedValues generatedValues = executeAndExtractReturning( sql, preparedStatement, session );
		return generatedValues.getGeneratedValue( persister.getIdentifierMapping() );
	}

	protected abstract GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session);

	protected void releaseStatement(PreparedStatement preparedStatement, SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
		jdbcCoordinator.afterStatementExecution();
	}
}
