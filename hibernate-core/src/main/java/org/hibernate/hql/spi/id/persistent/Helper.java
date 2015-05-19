/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
				ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				bindSessionIdentifier( ps, session, 1 );
				session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
			}
			finally {
				if ( ps != null ) {
					try {
						session.getJdbcCoordinator().getResourceRegistry().release( ps );
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
