/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.jboss.logging.Logger;

import static java.sql.Connection.*;

/// SQL Server locking reads, RCSI, and native snapshot isolation.
///
/// @since 8.0
/// @author Steve Ebersole
public final class SQLServerTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final SQLServerTransactionConcurrencyResolver INSTANCE = new SQLServerTransactionConcurrencyResolver();

	private SQLServerTransactionConcurrencyResolver() {
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == java.sql.Connection.TRANSACTION_NONE
				? BlockingDuration.UNKNOWN
				: BlockingDuration.TRANSACTION;
	}

	@Override
	protected boolean supportsExplicitLocks(Integer isolation) {
		return isolation == null || isolation != TRANSACTION_NONE;
	}

	@Override
	protected Guarantees lockingReadGuarantees(Integer isolation) {
		return new Guarantees( true, true, true, true, true );
	}

	@Override
	protected boolean supportsSharedLocks(Integer isolation) {
		return true;
	}

	@Override
	protected int snapshotIsolation() {
		return 4096;
	}

	@Override
	protected Boolean versionedReads(Integer isolation) {
		return isolation == null || isolation == TRANSACTION_READ_COMMITTED ? null : isolation == 4096;
	}

	@Override
	protected Boolean observeVersionedReads(Integer isolation, Connection connection, JdbcMetadataOnBoot access) {
		Boolean versioned = versionedReads( isolation );
		if ( isolation != null
				&& isolation == TRANSACTION_READ_COMMITTED
				&& connection != null && access != JdbcMetadataOnBoot.DISALLOW ) {
			try ( var statement = connection.createStatement();
					var rows = statement.executeQuery(
							"select is_read_committed_snapshot_on from sys.databases where database_id = db_id()" ) ) {
				if ( !rows.next() ) {
					throw new SQLException( "No snapshot configuration returned for the current database" );
				}
				versioned = rows.getBoolean( 1 );
				if ( rows.wasNull() ) {
					throw new SQLException( "Null snapshot configuration returned for the current database" );
				}
			}
			catch (SQLException ex) {
				if ( access == JdbcMetadataOnBoot.REQUIRE ) {
					// Do not let the outer ALLOW metadata fallback discard already extracted metadata.
					throw new TransactionConcurrencyResolutionException( "Unable to resolve SQL Server transaction concurrency", ex );
				}
				Logger.getLogger( SQLServerTransactionConcurrencyResolver.class )
						.warn( "Unable to observe transaction concurrency; retaining other JDBC metadata", ex );
				versioned = null;
			}
		}
		return versioned;
	}

	@Override
	protected boolean knownIsolation(Integer isolation) {
		return super.knownIsolation( isolation ) || isolation != null && isolation == 4096;
	}

	@Override
	protected boolean stableReads(Integer isolation) {
		return super.stableReads( isolation ) || isolation != null && isolation == 4096;
	}

	@Override
	protected boolean shortCurrentRead() {
		return true;
	}

	@Override
	protected boolean updateReadAllowsOrdinaryRead() {
		return true;
	}
}
