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
			final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
			try {
				final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, sql );
				try {
					if ( !resultSet.next() ) {
						throw new HibernateException( "The database returned no GUID identity value" );
					}
					return resultSet.getString( 1 );
				}
				finally {
					resourceRegistry.release( resultSet, statement );
				}
			}
			finally {
				resourceRegistry.release( statement );
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
