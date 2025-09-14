/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

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

	private static boolean WARNED;

	public GUIDGenerator() {
		if ( !WARNED ) {
			WARNED = true;
			DEPRECATION_LOGGER.deprecatedUuidGenerator(
					UUIDGenerator.class.getName(),
					UUIDGenerationStrategy.class.getName() );
		}
	}

	public Object generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
		final String sql = session.getJdbcServices().getJdbcEnvironment().getDialect().getSelectGUIDString();
		try {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			try {
				final var rs = jdbcCoordinator.getResultSetReturn().extract( st, sql );
				try {
					if ( !rs.next() ) {
						throw new HibernateException( "The database returned no GUID identity value" );
					}
					return rs.getString( 1 );
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
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
