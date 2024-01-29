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
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract {@link InsertGeneratedIdentifierDelegate} implementation where
 * the underlying strategy causes the generated identifier to be returned as
 * an effect of performing the insert statement.  Thus, there is no need for
 * an additional sql statement to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractReturningDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	public AbstractReturningDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	@Override
	public Object performInsert(
			PreparedStatementDetails insertStatementDetails,
			JdbcValueBindings valueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		session.getJdbcServices().getSqlStatementLogger().logStatement( insertStatementDetails.getSqlString() );
		try {
			valueBindings.beforeStatement( insertStatementDetails );
			return executeAndExtract(
					insertStatementDetails.getSqlString(),
					insertStatementDetails.getStatement(),
					session
			);
		}
		finally {
			if ( insertStatementDetails.getStatement() != null ) {
				insertStatementDetails.releaseStatement( session );
			}
			valueBindings.afterStatement( insertStatementDetails.getMutatingTableDetails() );
			session.getJdbcCoordinator().afterStatementExecution();
		}
	}

	@Override
	public final Object performInsert(String insertSql, SharedSessionContractImplementor session, Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = prepareStatement( insertSql, session );
			try {
				binder.bindValues( insert );
				return executeAndExtract( insertSql, insert, session );
			}
			finally {
				releaseStatement( insert, session );
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
					insertSql
			);
		}
	}

	protected PostInsertIdentityPersister getPersister() {
		return persister;
	}

	protected abstract Object executeAndExtract(
			String insertSql,
			PreparedStatement insertStatement,
			SharedSessionContractImplementor session);

	protected void releaseStatement(PreparedStatement insert, SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insert );
		jdbcCoordinator.afterStatementExecution();
	}
}
