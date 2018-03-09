/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract InsertGeneratedIdentifierDelegate implementation where the
 * underlying strategy requires a subsequent select after the insert
 * to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectingDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	protected AbstractSelectingDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	@Override
	public final Serializable performInsert(
			String insertSQL,
			SharedSessionContractImplementor session,
			Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( insertSQL, PreparedStatement.NO_GENERATED_KEYS );
			try {
				binder.bindValues( insert );
				session.getJdbcCoordinator().getResultSetReturn().executeUpdate( insert );
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( insert );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}

		final String selectSQL = getSelectSQL();

		try {
			//fetch the generated id in a separate query
			PreparedStatement idSelect = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( selectSQL, false );
			try {
				bindParameters( session, idSelect, binder.getEntity() );
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( idSelect );
				try {
					return getResult( session, rs, binder.getEntity() );
				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, idSelect );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( idSelect );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not retrieve generated id after insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}
	}

	/**
	 * Get the SQL statement to be used to retrieve generated key values.
	 *
	 * @return The SQL command string
	 */
	protected abstract String getSelectSQL();

	/**
	 * Bind any required parameter values into the SQL command {@link #getSelectSQL}.
	 *
	 * @param session The session
	 * @param ps The prepared {@link #getSelectSQL SQL} command
	 * @param entity The entity being saved.
	 *
	 * @throws SQLException
	 */
	protected void bindParameters(
			SharedSessionContractImplementor session,
			PreparedStatement ps,
			Object entity) throws SQLException {
	}

	/**
	 * Extract the generated key value from the given result set.
	 *
	 * @param session The session
	 * @param rs The result set containing the generated primay key values.
	 * @param entity The entity being saved.
	 *
	 * @return The generated identifier
	 *
	 * @throws SQLException
	 */
	protected abstract Serializable getResult(
			SharedSessionContractImplementor session,
			ResultSet rs,
			Object entity) throws SQLException;

}
