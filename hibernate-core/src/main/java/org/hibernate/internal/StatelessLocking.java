/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.LockMode;
import org.hibernate.HibernateException;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.spi.DelegatingQueryOptions;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;

/**
 * Stateless sessions obtain locks immediately so that version checks and increments
 * do not depend on a persistence context surviving the operation.
 */
public final class StatelessLocking {
	private StatelessLocking() {
	}

	public static LockMode getEffectiveLockMode(LockMode lockMode, SharedSessionContractImplementor session) {
		if ( lockMode == LockMode.OPTIMISTIC ) {
			final var concurrency = session.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
			if ( protectsUntilCompletion( concurrency, Operation.SHARED_LOCK_READ ) ) {
				return LockMode.PESSIMISTIC_READ;
			}
			if ( protectsUntilCompletion( concurrency, Operation.UPDATE_LOCK_READ ) ) {
				return LockMode.PESSIMISTIC_WRITE;
			}
			throw new HibernateException( "Stateless optimistic locking requires transaction-long row protection; no strategy established for "
					+ concurrency.getName() );
		}
		return lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ? LockMode.PESSIMISTIC_FORCE_INCREMENT : lockMode;
	}

	private static boolean protectsUntilCompletion(TransactionConcurrency concurrency, Operation operation) {
		if ( !concurrency.supports( operation ) ) {
			return false;
		}
		final var guarantees = concurrency.getReadGuarantees( operation );
		return guarantees.preventsDirtyReads() && guarantees.preventsConcurrentModification()
				&& guarantees.holdsRowLockUntilTransactionCompletion();
	}

	public static LockOptions getEffectiveLockOptions(LockOptions lockOptions, SharedSessionContractImplementor session) {
		if ( !session.isStateless() ) {
			return lockOptions;
		}
		final var effectiveLockMode = getEffectiveLockMode( lockOptions.getLockMode(), session );
		return effectiveLockMode == lockOptions.getLockMode()
				? lockOptions : lockOptions.makeCopy().setLockMode( effectiveLockMode );
	}

	public static DomainQueryExecutionContext getExecutionContext(DomainQueryExecutionContext context) {
		final var queryOptions = context.getQueryOptions();
		final var lockOptions = getEffectiveLockOptions( queryOptions.getLockOptions(), context.getSession() );
		if ( lockOptions == queryOptions.getLockOptions() ) {
			return context;
		}
		final var effectiveQueryOptions = new DelegatingQueryOptions( queryOptions ) {
			@Override
			public LockOptions getLockOptions() {
				return lockOptions;
			}
		};
		return new DelegatingDomainQueryExecutionContext( context ) {
			@Override
			public QueryOptions getQueryOptions() {
				return effectiveQueryOptions;
			}

			@Override
			public Class<?> getResultType() {
				return context.getResultType();
			}
		};
	}
}
