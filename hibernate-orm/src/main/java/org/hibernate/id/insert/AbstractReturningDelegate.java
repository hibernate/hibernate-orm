/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract InsertGeneratedIdentifierDelegate implementation where the
 * underlying strategy causes the generated identifier to be returned as an
 * effect of performing the insert statement.  Thus, there is no need for an
 * additional sql statement to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractReturningDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	public AbstractReturningDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	@Override
	public final Serializable performInsert(
			String insertSQL,
			SharedSessionContractImplementor session,
			Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = prepare( insertSQL, session );
			try {
				binder.bindValues( insert );
				return executeAndExtract( insert, session );
			}
			finally {
				releaseStatement( insert, session );
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}
	}

	protected PostInsertIdentityPersister getPersister() {
		return persister;
	}

	protected abstract PreparedStatement prepare(String insertSQL, SharedSessionContractImplementor session) throws SQLException;

	protected abstract Serializable executeAndExtract(PreparedStatement insert, SharedSessionContractImplementor session)
			throws SQLException;

	protected void releaseStatement(PreparedStatement insert, SharedSessionContractImplementor session) throws SQLException {
		session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( insert );
		session.getJdbcCoordinator().afterStatementExecution();
	}
}
