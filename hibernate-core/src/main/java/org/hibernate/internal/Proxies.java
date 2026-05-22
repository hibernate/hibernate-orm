/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Proxy handling used by {@link StatelessSessionImpl}.
 * @author Gavin King
 */
class Proxies {

	static boolean hasProxyFactory(EntityPersister persister) {
		return persister.getRepresentationStrategy().getProxyFactory() != null;
	}

	static Object obtainProxyFromFactory(
			EntityHolder holder,
			EntityKey entityKey,
			PersistenceContext persistenceContext) {
		final var persister = entityKey.getPersister();
		final Object proxy = holder == null ? null : holder.getProxy();
		if ( proxy != null ) {
			return narrowProxy( proxy, persistenceContext, persister, entityKey );
		}
		// Specialized handling for entities with subclasses with
		// a HibernateProxy factory.
		else if ( persister.hasSubclasses() ) {
			// Entities with subclasses that define a ProxyFactory
			// can create a HibernateProxy.
			SESSION_LOGGER.creatingHibernateProxyToHonorLaziness();
			return createProxy( entityKey, persistenceContext );
		}
		else {
			return persister.getBytecodeEnhancementMetadata()
					.createEnhancedProxy( entityKey, false,
							persistenceContext.getSession() );
		}
	}

	static Object narrowProxy(
			Object proxy,
			PersistenceContext persistenceContext,
			EntityPersister persister,
			EntityKey entityKey) {
		SESSION_LOGGER.entityProxyFoundInSessionCache();
		if ( SESSION_LOGGER.isDebugEnabled() && extractLazyInitializer( proxy ).isUnwrap() ) {
			SESSION_LOGGER.ignoringNoProxyToHonorLaziness();
		}

		return persistenceContext.narrowProxy( proxy, persister, entityKey, null );
	}

	static Object createProxy(EntityKey entityKey, PersistenceContext persistenceContext) {
		final Object proxy =
				entityKey.getPersister()
						.createProxy( entityKey.getIdentifier(), persistenceContext.getSession() );
		persistenceContext.addProxy( entityKey, proxy );
		return proxy;
	}

	static Object narrowOrCreateProxy(
			EntityHolder holder,
			EntityKey entityKey,
			PersistenceContext persistenceContext) {
		final Object existingProxy = holder == null ? null : holder.getProxy();
		return existingProxy != null
				? persistenceContext.narrowProxy( existingProxy, entityKey.getPersister(), entityKey, null )
				: createProxy( entityKey, persistenceContext );
	}
}
