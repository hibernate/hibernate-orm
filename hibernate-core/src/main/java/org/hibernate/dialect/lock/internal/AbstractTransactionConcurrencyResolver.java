/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import java.sql.Connection;
import java.util.Locale;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;

import static java.sql.Connection.*;
import static org.hibernate.dialect.lock.spi.BlockingDuration.*;
import static org.hibernate.dialect.lock.spi.Operation.*;

/// Shared parsing and profile construction for dialect-specific resolvers.
/// Subclasses supply database facts explicitly; SQL rendering is never used
/// to infer isolation semantics or the lifetime of locks.
///
/// @since 8.0
/// @author Steve Ebersole
public abstract class AbstractTransactionConcurrencyResolver implements TransactionConcurrencyResolver {
	@Override
	public TransactionConcurrency resolve(
			Dialect dialect, JdbcMetadata metadata, Connection connection,
			Object declaration, JdbcMetadataOnBoot access) {
		if ( declaration instanceof TransactionConcurrency supplied ) {
			return supplied;
		}
		final Integer observedIsolation = metadata.isJdbcMetadataAccessible()
				? metadata.getTransactionIsolation() : null;
		final Boolean versioned = observeVersionedReads( observedIsolation, connection, access );
		if ( declaration == null ) {
			return profile( dialect, observedIsolation, versioned );
		}
		if ( !(declaration instanceof String text) ) {
			throw new TransactionConcurrencyResolutionException( "hibernate.transaction.concurrency requires a registered name or TransactionConcurrency" );
		}
		final String name = text.trim().toUpperCase( Locale.ROOT );
		final int declaredIsolation = switch ( name ) {
			case "READ_UNCOMMITTED" -> TRANSACTION_READ_UNCOMMITTED;
			case "READ_COMMITTED", "LOCKING_READ_COMMITTED", "READ_COMMITTED_SNAPSHOT" -> TRANSACTION_READ_COMMITTED;
			case "REPEATABLE_READ" -> TRANSACTION_REPEATABLE_READ;
			case "SERIALIZABLE" -> TRANSACTION_SERIALIZABLE;
			case "SNAPSHOT" -> snapshotIsolation();
			default -> throw new TransactionConcurrencyResolutionException( "Unknown transaction concurrency declaration: " + text );
		};
		if ( observedIsolation != null && observedIsolation != declaredIsolation ) {
			throw contradiction( name, "connection isolation " + observedIsolation );
		}
		Boolean declaredVersioned = versionedReads( declaredIsolation );
		if ( name.equals( "LOCKING_READ_COMMITTED" ) || name.equals( "READ_COMMITTED_SNAPSHOT" ) ) {
			final boolean requestedVersioned = name.equals( "READ_COMMITTED_SNAPSHOT" );
			if ( declaredVersioned != null && declaredVersioned != requestedVersioned ) {
				throw contradiction( name, "established dialect read behavior" );
			}
			declaredVersioned = requestedVersioned;
		}
		if ( versioned != null && declaredVersioned != null && !versioned.equals( declaredVersioned ) ) {
			throw contradiction( name, "observed snapshot configuration" );
		}
		return profile( dialect, declaredIsolation, versioned != null ? versioned : declaredVersioned );
	}

	protected TransactionConcurrency profile(Dialect dialect, Integer isolation, Boolean versioned) {
		final boolean locking = Boolean.FALSE.equals( versioned );
		final boolean knownIsolation = knownIsolation( isolation );
		final boolean dirty = dirtyReads( isolation );
		final boolean stable = stableReads( isolation );
		final boolean ordinaryCurrent = knownIsolation && !dirty && locking;
		final boolean ordinaryProtection = ordinaryCurrent && stable;
		final var builder = TransactionConcurrencies.builder(
				dialect.getClass().getSimpleName() + ":" + (isolation == null ? "UNKNOWN" : isolation)
						+ ":" + (versioned == null ? "UNKNOWN" : versioned ? "VERSIONED" : "LOCKING") );
		builder.read( READ, new Guarantees( knownIsolation && !dirty, stable,
				ordinaryProtection, ordinaryProtection, ordinaryCurrent ) );
		final boolean explicitLocks = supportsExplicitLocks( isolation );
		final boolean shared = explicitLocks && supportsSharedLocks( isolation );
		final boolean shortCurrent = shortCurrentRead();
		final Guarantees longRead = lockingReadGuarantees( isolation );
		if ( explicitLocks ) {
			if ( supportsUpdateLocks( isolation ) ) {
				builder.read( UPDATE_LOCK_READ, longRead );
			}
			if ( shared ) {
				builder.read( SHARED_LOCK_READ, longRead );
			}
			if ( supportsCurrentRead( isolation ) ) {
				builder.read( CURRENT_READ, shortCurrent
						? new Guarantees( true, false, false, false, true ) : longRead );
			}
		}
		for ( Operation preceding : Operation.values() ) {
			if ( !supported( preceding, isolation, explicitLocks, shared ) ) {
				continue;
			}
			for ( Operation concurrent : Operation.values() ) {
				if ( !supported( concurrent, isolation, explicitLocks, shared ) ) {
					continue;
				}
				BlockingDuration duration;
				if ( preceding == READ ) {
					duration = !knownIsolation || versioned == null ? UNKNOWN
							: dirty || versioned || concurrent == READ || concurrent == SHARED_LOCK_READ
							? NONE : concurrent != WRITE ? UNKNOWN : stable ? TRANSACTION : STATEMENT;
				}
				else if ( concurrent == READ ) {
					duration = !knownIsolation || versioned == null ? UNKNOWN : dirty || versioned ? NONE
							: preceding == WRITE ? TRANSACTION
							: preceding == SHARED_LOCK_READ || shortCurrent && preceding == CURRENT_READ ? NONE
							: updateReadAllowsOrdinaryRead() ? NONE : UNKNOWN;
				}
				else if ( preceding == WRITE && concurrent == WRITE ) {
					duration = writeWriteBlocking( isolation );
				}
				else if ( preceding == WRITE || concurrent == WRITE ) {
					duration = isolation != null && isolation == TRANSACTION_NONE ? UNKNOWN
							: preceding == CURRENT_READ && shortCurrent ? STATEMENT : TRANSACTION;
				}
				else {
					// Explicit lock compatibility varies (e.g. SQL Server update locks
					// coexist with shared locks); do not invent a portable matrix.
					duration = preceding == SHARED_LOCK_READ && concurrent == SHARED_LOCK_READ ? NONE
							: preceding == UPDATE_LOCK_READ && concurrent == UPDATE_LOCK_READ ? TRANSACTION : UNKNOWN;
				}
				builder.blocking( preceding, concurrent, adjustBlocking( isolation, preceding, concurrent, duration ) );
			}
		}
		return builder.build();
	}

	private boolean supported(Operation operation, Integer isolation, boolean explicitLocks, boolean shared) {
		return switch ( operation ) {
			case READ, WRITE -> true;
			case SHARED_LOCK_READ -> shared;
			case CURRENT_READ -> explicitLocks && supportsCurrentRead( isolation );
			case UPDATE_LOCK_READ -> explicitLocks && supportsUpdateLocks( isolation );
		};
	}

	protected Boolean observeVersionedReads(Integer isolation, Connection connection, JdbcMetadataOnBoot access) {
		return versionedReads( isolation );
	}

	protected Boolean versionedReads(Integer isolation) {
		return null;
	}

	protected int snapshotIsolation() {
		throw new TransactionConcurrencyResolutionException( "No registered SNAPSHOT configuration for " + getClass().getSimpleName() );
	}

	protected boolean knownIsolation(Integer isolation) {
		return isolation != null && (isolation == TRANSACTION_READ_UNCOMMITTED
				|| isolation == TRANSACTION_READ_COMMITTED || isolation == TRANSACTION_REPEATABLE_READ
				|| isolation == TRANSACTION_SERIALIZABLE);
	}

	protected boolean dirtyReads(Integer isolation) {
		return isolation != null && isolation == TRANSACTION_READ_UNCOMMITTED;
	}

	protected boolean stableReads(Integer isolation) {
		return isolation != null && (isolation == TRANSACTION_REPEATABLE_READ || isolation == TRANSACTION_SERIALIZABLE);
	}

	protected boolean supportsExplicitLocks(Integer isolation) {
		return false;
	}

	/// Lifetime of exclusion established by a write against another write.
	/// Subclasses must establish this independently of explicit read-lock support.
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return UNKNOWN;
	}

	protected boolean supportsUpdateLocks(Integer isolation) {
		return true;
	}

	protected boolean supportsSharedLocks(Integer isolation) {
		return false;
	}

	protected boolean supportsCurrentRead(Integer isolation) {
		return supportsExplicitLocks( isolation );
	}

	protected boolean shortCurrentRead() {
		return false;
	}

	protected boolean updateReadAllowsOrdinaryRead() {
		return false;
	}

	protected Guarantees lockingReadGuarantees(Integer isolation) {
		return new Guarantees( false, false, false, false, false );
	}

	protected BlockingDuration adjustBlocking(Integer isolation, Operation preceding, Operation concurrent, BlockingDuration duration) {
		return duration;
	}

	private static TransactionConcurrencyResolutionException contradiction(String name, String fact) {
		return new TransactionConcurrencyResolutionException( "Transaction concurrency declaration " + name + " contradicts " + fact );
	}
}
