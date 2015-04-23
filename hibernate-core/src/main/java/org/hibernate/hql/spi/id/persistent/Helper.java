/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.hql.spi.id.persistent;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractSessionImpl;
import org.hibernate.type.UUIDCharType;

/**
 * @author Steve Ebersole
 */
public class Helper {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	public static final String SESSION_ID_COLUMN_NAME = "hib_sess_id";

	private Helper() {
	}


	public void bindSessionIdentifier(PreparedStatement ps, SessionImplementor session, int position) throws SQLException {
		if ( ! AbstractSessionImpl.class.isInstance( session ) ) {
			throw new HibernateException( "Only available on SessionImpl instances" );
		}
		UUIDCharType.INSTANCE.set( ps, ( (AbstractSessionImpl) session ).getSessionIdentifier(), position, session );
	}

	public void cleanUpRows(String tableName, SessionImplementor session) {
		final String sql = "delete from " + tableName + " where " + SESSION_ID_COLUMN_NAME + "=?";
		try {
			PreparedStatement ps = null;
			try {
				ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				bindSessionIdentifier( ps, session, 1 );
				session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
			}
			finally {
				if ( ps != null ) {
					try {
						session.getTransactionCoordinator().getJdbcCoordinator().release( ps );
					}
					catch( Throwable ignore ) {
						// ignore
					}
				}
			}
		}
		catch (SQLException e) {
			throw convert( session.getFactory(), e, "Unable to clean up id table [" + tableName + "]", sql );
		}
	}

	public JDBCException convert(SessionFactoryImplementor factory, SQLException e, String message, String sql) {
		throw factory.getSQLExceptionHelper().convert( e, message, sql );
	}
}
