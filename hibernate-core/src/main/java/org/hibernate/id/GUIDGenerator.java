/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
			PreparedStatement st = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				ResultSet rs = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st );
				final String result;
				try {
					if ( !rs.next() ) {
						throw new HibernateException( "The database returned no GUID identity value" );
					}
					result = rs.getString(1);
				}
				finally {
					session.getTransactionCoordinator().getJdbcCoordinator().release( rs, st );
				}
                LOG.guidGenerated(result);
				return result;
			}
			finally {
				session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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
