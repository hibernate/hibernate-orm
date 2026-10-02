package org.hibernate.dialect.lock.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.jboss.logging.Logger;

/// Spanner serializable read/write transactions acquire retained read locks in
/// pessimistic mode. Optimistic mode and repeatable-read snapshots do not
/// establish the same guarantees. The JDBC read-lock mode is observed without
/// changing transaction settings; per-transaction overrides are outside this baseline.
///
/// @since 8.0
/// @author Steve Ebersole
public final class SpannerTransactionConcurrencyResolver implements TransactionConcurrencyResolver {
	private final boolean postgres;
	private final boolean lockingClause;

	public SpannerTransactionConcurrencyResolver(boolean postgres, boolean lockingClause) {
		this.postgres = postgres;
		this.lockingClause = lockingClause;
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
					var rows = statement.executeQuery( postgres ? "show spanner.read_lock_mode" : "show variable read_lock_mode" ) ) {
				if ( !rows.next() || rows.getString( 1 ) == null ) {
					throw new SQLException( "No Spanner read-lock mode returned" );
				}
				pessimistic = switch ( rows.getString( 1 ) ) {
					case "PESSIMISTIC", "READ_LOCK_MODE_UNSPECIFIED", "UNSPECIFIED" -> true;
					case "OPTIMISTIC" -> false;
					default -> null;
				};
				if ( connection.isReadOnly() ) {
					pessimistic = false;
				}
			}
			catch (SQLException ex) {
				if ( access == JdbcMetadataOnBoot.REQUIRE ) {
					throw new TransactionConcurrencyResolutionException( "Unable to resolve Spanner read-lock mode", ex );
				}
				Logger.getLogger( SpannerTransactionConcurrencyResolver.class ).warn( "Unable to observe Spanner read-lock mode", ex );
				pessimistic = null;
			}
		}
		return new ModeResolver( pessimistic, lockingClause ).resolve( dialect, metadata, connection, declaration, access );
	}

	/// Immutable facts for an observed Spanner transaction mode.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	private static final class ModeResolver extends AbstractTransactionConcurrencyResolver {
		private final Boolean pessimistic;
		private final boolean lockingClause;

		private ModeResolver(Boolean pessimistic, boolean lockingClause) {
			this.pessimistic = pessimistic;
			this.lockingClause = lockingClause;
		}

		@Override
		protected Boolean versionedReads(Integer isolation) {
			return isolation != null && isolation == Connection.TRANSACTION_SERIALIZABLE
					? pessimistic == null ? null : !pessimistic : null;
		}

		@Override
		protected boolean supportsExplicitLocks(Integer isolation) {
			return lockingClause && Boolean.FALSE.equals( versionedReads( isolation ) );
		}

		@Override
		protected Guarantees lockingReadGuarantees(Integer isolation) {
			return new Guarantees( true, true, true, true, true );
		}

		@Override
		protected BlockingDuration writeWriteBlocking(Integer isolation) {
			return Boolean.FALSE.equals( versionedReads( isolation ) ) ? BlockingDuration.TRANSACTION : BlockingDuration.UNKNOWN;
		}
	}
}
