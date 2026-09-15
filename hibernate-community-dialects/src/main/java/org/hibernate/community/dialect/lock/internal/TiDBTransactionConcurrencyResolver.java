/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.internal.AbstractTransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.jboss.logging.Logger;

/// TiDB current reads and row protection depend on pessimistic transaction mode.
/// A connection's configured transaction mode must also apply to transactions
/// used by the factory; explicit BEGIN mode overrides are outside this baseline.
///
/// @since 8.0
/// @author Steve Ebersole
public final class TiDBTransactionConcurrencyResolver implements TransactionConcurrencyResolver {
	public static final TiDBTransactionConcurrencyResolver INSTANCE = new TiDBTransactionConcurrencyResolver();

	private TiDBTransactionConcurrencyResolver() {
	}

	@Override
	public TransactionConcurrency resolve(Dialect dialect, JdbcMetadata metadata, Connection connection,
			Object declaration, JdbcMetadataOnBoot access) {
		if ( declaration instanceof TransactionConcurrency supplied ) {
			return supplied;
		}
		Boolean pessimistic = null;
		if ( connection != null && access != JdbcMetadataOnBoot.DISALLOW ) {
			try ( var statement = connection.createStatement();
					var rows = statement.executeQuery( "select @@tidb_txn_mode" ) ) {
				if ( !rows.next() ) {
					throw new SQLException( "No TiDB transaction mode returned" );
				}
				final String mode = rows.getString( 1 );
				if ( mode == null ) {
					throw new SQLException( "Null TiDB transaction mode returned" );
				}
				pessimistic = "pessimistic".equalsIgnoreCase( mode ) ? Boolean.TRUE
						: "optimistic".equalsIgnoreCase( mode ) || mode.isEmpty() ? Boolean.FALSE : null;
			}
			catch (SQLException ex) {
				if ( access == JdbcMetadataOnBoot.REQUIRE ) {
					throw new TransactionConcurrencyResolutionException( "Unable to resolve TiDB transaction mode", ex );
				}
				Logger.getLogger( TiDBTransactionConcurrencyResolver.class )
						.warn( "Unable to observe TiDB transaction mode", ex );
			}
		}
		return new ModeResolver( pessimistic ).resolve( dialect, metadata, connection, declaration, access );
	}

	/// Immutable facts for one observed transaction mode.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	private static final class ModeResolver extends AbstractTransactionConcurrencyResolver {
		private final Boolean pessimistic;

		private ModeResolver(Boolean pessimistic) {
			this.pessimistic = pessimistic;
		}

		@Override
		protected BlockingDuration writeWriteBlocking(Integer isolation) {
			return pessimistic == null || isolation != null && isolation == Connection.TRANSACTION_NONE
					? BlockingDuration.UNKNOWN
					: pessimistic ? BlockingDuration.TRANSACTION
					: BlockingDuration.NONE;
		}

		@Override
		protected Boolean versionedReads(Integer isolation) {
			return true;
		}

		@Override
		protected boolean supportsExplicitLocks(Integer isolation) {
			return Boolean.TRUE.equals( pessimistic ) && (isolation == null || isolation != Connection.TRANSACTION_NONE);
		}

		@Override
		protected Guarantees lockingReadGuarantees(Integer isolation) {
			return new Guarantees( true, true, true, true, true );
		}

		@Override
		protected int snapshotIsolation() {
			return Connection.TRANSACTION_REPEATABLE_READ;
		}
	}
}
