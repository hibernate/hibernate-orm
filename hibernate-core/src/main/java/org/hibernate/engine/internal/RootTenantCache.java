/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import jakarta.annotation.Nullable;

import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Invalidates owning-tenant cache entries before mutations by a root tenant.
 * Root sessions never read or populate the shared cache themselves.
 */
public final class RootTenantCache {
	private RootTenantCache() {
	}

	public static void invalidateEntity(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		if ( session.isRootTenant() ) {
			if ( persister.canWriteToCache() ) {
				final var state = entityCacheState( id, persister, session );
				if ( state != null ) {
					final var cache = persister.getCacheAccessStrategy();
					final Object key = cache.generateCacheKey( id, persister, session.getFactory(), state.tenantIdentifier() );
					invalidateItem( key, state.version(), cache, session );
				}
			}
			if ( persister.hasNaturalIdCache() ) {
				// Natural-id cache key generation accepts a session, not an explicit tenant.
				// Use the provider's region invalidation protocol instead of a root key.
				final var cache = persister.getNaturalIdCacheAccessStrategy();
				final var lock = cache.lockRegion();
				session.getTransactionCompletionCallbacks().registerCallback(
						(success, source) -> cache.unlockRegion( lock ) );
				cache.removeAll( session );
			}
		}
	}

	private record EntityCacheState(String tenantIdentifier, Object version) {}

	private static @Nullable EntityCacheState entityCacheState(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		final var loader = persister.getTenantIdLoader();
		final var snapshot = loader == null ? null : loader.loadCacheSnapshot( id, session );
		final String tenant = snapshot == null
				? tenantIdentifier( id, persister, session ) : tenantIdentifier( snapshot.tenantId(), session );
		if ( tenant == null ) {
			return null;
		}
		final Object version = snapshot != null ? snapshot.version()
				: persister.isVersioned() ? persister.getCurrentVersion( id, session ) : null;
		return new EntityCacheState( tenant, version );
	}

	public static void invalidateCollection(
			Object key, CollectionPersister persister, SharedSessionContractImplementor session) {
		if ( session.isRootTenant() && persister.hasCache() ) {
			final var owner = persister.getOwnerEntityPersister();
			final String property = persister.getCollectionType().getLHSPropertyName();
			final Object id = property == null ? key : owner.getIdByUniqueKey( key, property, session );
			final String tenant = id == null ? null : tenantIdentifier( id, owner, session );
			if ( tenant != null ) {
				final var cache = persister.getCacheAccessStrategy();
				invalidateItem( cache.generateCacheKey( key, persister, session.getFactory(), tenant ), null, cache, session );
			}
		}
	}

	private static void invalidateItem(
			Object key, Object version, CachedDomainDataAccess cache, SharedSessionContractImplementor session) {
		final var lock = cache.lockItem( session, key, version );
		session.getTransactionCompletionCallbacks().registerCallback(
				(success, source) -> cache.unlockItem( source, key, lock ) );
		cache.remove( session, key );
	}

	private static String tenantIdentifier(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		final var mapping = persister.getTenantIdMapping();
		if ( mapping == null ) {
			return null;
		}
		// Detached state may contain a different tenant, and root writes may omit it.
		final var loader = persister.getTenantIdLoader();
		return tenantIdentifier( loader == null
				? mapping.getTenantIdFromIdentifier( id, session ) : loader.loadTenantId( id, session ), session );
	}

	private static String tenantIdentifier(Object tenant, SharedSessionContractImplementor session) {
		return tenant == null ? null : session.getFactory().getTenantIdentifierJavaType().toString( tenant );
	}
}
