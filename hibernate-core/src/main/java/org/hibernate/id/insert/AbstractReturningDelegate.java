/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.AbstractGeneratedValuesMutationDelegate;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.pretty.MessageHelper.infoString;

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
		final String sql = statementDetails.getSqlString();
		session.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		try {
			valueBindings.beforeStatement( statementDetails );
			return executeAndExtractReturning( sql, statementDetails.getStatement(), session );
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
		session.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		// prepare and execute the insert
		final var insert = prepareStatement( sql, session );
		try {
			binder.bindValues( insert );
			return executeAndExtractReturning( sql, insert, session );
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"Could not insert: " + infoString( persister ),
					sql
			);
		}
		finally {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insert );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	protected abstract GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session);
}
