/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.id.insert;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract InsertGeneratedIdentifierDelegate implementation where the
 * underlying strategy requires an subsequent select after the insert
 * to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectingDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	protected AbstractSelectingDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	public final Serializable performInsert(
			String insertSQL,
			SessionImplementor session,
			Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( insertSQL, PreparedStatement.NO_GENERATED_KEYS );
			try {
				binder.bindValues( insert );
				insert.executeUpdate();
			}
			finally {
				insert.close();
			}
		}
		catch ( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
			        sqle,
			        "could not insert: " + MessageHelper.infoString( persister ),
			        insertSQL
				);
		}

		final String selectSQL = getSelectSQL();

		try {
			//fetch the generated id in a separate query
			PreparedStatement idSelect = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( selectSQL, false );
			try {
				bindParameters( session, idSelect, binder.getEntity() );
				ResultSet rs = idSelect.executeQuery();
				try {
					return getResult( session, rs, binder.getEntity() );
				}
				finally {
					rs.close();
				}
			}
			finally {
				idSelect.close();
			}

		}
		catch ( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
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
	 * @throws SQLException
	 */
	protected void bindParameters(
			SessionImplementor session,
	        PreparedStatement ps,
	        Object entity) throws SQLException {
	}

	/**
	 * Extract the generated key value from the given result set.
	 *
	 * @param session The session
	 * @param rs The result set containing the generated primay key values.
	 * @param entity The entity being saved.
	 * @return The generated identifier
	 * @throws SQLException
	 */
	protected abstract Serializable getResult(
			SessionImplementor session,
	        ResultSet rs,
	        Object entity) throws SQLException;

}
