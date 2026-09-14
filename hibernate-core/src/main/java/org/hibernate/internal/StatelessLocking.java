/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.LockMode;
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

	public static LockMode getEffectiveLockMode(LockMode lockMode) {
		return lockMode == LockMode.OPTIMISTIC ? LockMode.PESSIMISTIC_READ
				: lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ? LockMode.PESSIMISTIC_FORCE_INCREMENT
				: lockMode;
	}

	public static LockOptions getEffectiveLockOptions(LockOptions lockOptions, SharedSessionContractImplementor session) {
		if ( session.isStateless() ) {
			final var effectiveLockMode = getEffectiveLockMode( lockOptions.getLockMode() );
			if ( effectiveLockMode != lockOptions.getLockMode() ) {
				return lockOptions.makeCopy().setLockMode( effectiveLockMode );
			}
		}
		return lockOptions;
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
