/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * The legacy id generator named {@code guid}.
 * <p>
 * Generates {@code string} values using the SQL Server {@code NEWID()} function.
 *
 * @author Joseph Fifield
 *
 * @deprecated use {@link org.hibernate.id.uuid.UuidGenerator}
 */
@Deprecated(since = "6.0")
public class GUIDGenerator implements IdentifierGenerator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( GUIDGenerator.class );

	private static boolean WARNED;

	public GUIDGenerator() {
		if ( !WARNED ) {
			WARNED = true;
			LOG.deprecatedUuidGenerator( UUIDGenerator.class.getName(), UUIDGenerationStrategy.class.getName() );
		}
	}

	public Object generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
		final String sql = session.getJdbcServices().getJdbcEnvironment().getDialect().getSelectGUIDString();
		try {
			final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			try {
				final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st, sql );
				try {
					if ( !rs.next() ) {
						throw new HibernateException( "The database returned no GUID identity value" );
					}
					final String result = rs.getString( 1 );
					LOG.guidGenerated( result );
					return result;
				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not retrieve GUID",
					sql
			);
		}
	}
}
