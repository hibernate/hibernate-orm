/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Generates <tt>string</tt> values using the SQL Server NEWID() function.
 *
 * @author Joseph Fifield
 */
public class GUIDGenerator implements IdentifierGenerator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( GUIDGenerator.class );

	private static boolean WARNED;

	public GUIDGenerator() {
		if ( !WARNED ) {
			WARNED = true;
			LOG.deprecatedUuidGenerator( UUIDGenerator.class.getName(), UUIDGenerationStrategy.class.getName() );
		}
	}

	public Serializable generate(SessionImplementor session, Object obj) throws HibernateException {
		final String sql = session.getFactory().getDialect().getSelectGUIDString();
		try {
			PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );
				final String result;
				try {
					if ( !rs.next() ) {
						throw new HibernateException( "The database returned no GUID identity value" );
					}
					result = rs.getString( 1 );
				}
				finally {
					session.getJdbcCoordinator().getResourceRegistry().release( rs, st );
				}
				LOG.guidGenerated( result );
				return result;
			}
			finally {
				session.getJdbcCoordinator().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not retrieve GUID",
					sql
			);
		}
	}
}
