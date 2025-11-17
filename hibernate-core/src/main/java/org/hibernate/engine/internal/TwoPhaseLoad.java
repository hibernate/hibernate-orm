/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Functionality relating to the Hibernate two-phase loading process, that may be reused by persisters
 * that do not use the Loader framework
 *
 * @author Gavin King
 */
public final class TwoPhaseLoad {

	private TwoPhaseLoad() {
	}

	/**
	 *
	 * @param key The entity key
	 * @param object The entity instance
	 * @param persister The entity persister
	 * @param lockMode The lock mode
	 * @param version The version
	 * @param session The Session
	 */
	public static void addUninitializedCachedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final Object version,
			final SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entityHolder = persistenceContext.addEntityHolder( key, object );
		final var entityEntry = persistenceContext.addEntry(
				object,
				Status.LOADING,
				null,
				null,
				key.getIdentifier(),
				version,
				lockMode,
				true,
				persister,
				false
		);
		entityHolder.setEntityEntry( entityEntry );
		final Object proxy = entityHolder.getProxy();
		if ( proxy != null ) {
			// there is already a proxy for this impl
			final var lazyInitializer = extractLazyInitializer( proxy );
			assert lazyInitializer != null;
			lazyInitializer.setImplementation( object );
		}
	}
}
