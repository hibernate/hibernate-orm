/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.internal.TenantIdGeneration;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;

/**
 * Invalidates owning-tenant cache entries before mutations by a root tenant.
 * Root sessions never read or populate the shared cache themselves.
 */
public final class RootTenantCache {
	private RootTenantCache() {
	}

	public static void invalidateEntity(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		if ( TenantIdHelper.isRoot( session ) ) {
			if ( persister.canWriteToCache() ) {
				final String tenant = tenantIdentifier( id, persister, session );
				if ( tenant != null ) {
					final var cache = persister.getCacheAccessStrategy();
					final Object key = cache.generateCacheKey( id, persister, session.getFactory(), tenant );
					final Object[] snapshot = persister.isVersioned() ? persister.getDatabaseSnapshot( id, session ) : null;
					final Object version = snapshot == null ? null : snapshot[persister.getVersionPropertyIndex()];
					invalidateItem( key, version, cache, session );
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

	public static void invalidateCollection(
			Object key, CollectionPersister persister, SharedSessionContractImplementor session) {
		if ( TenantIdHelper.isRoot( session ) && persister.hasCache() ) {
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
		final var mapping = TenantIdHelper.tenantIdMapping( persister );
		final Object tenant;
		if ( mapping != null ) {
			// Detached state may contain a different tenant, and root writes may omit it.
			final Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			tenant = snapshot == null ? null : snapshot[mapping.getStateArrayPosition()];
		}
		else if ( persister.getGenerator() instanceof TenantIdGeneration ) {
			tenant = id;
		}
		else if ( persister.getGenerator() instanceof CompositeNestedGeneratedValueGenerator composite ) {
			final var type = (ComponentType) persister.getIdentifierType();
			for ( var plan : composite.getGenerationPlans() ) {
				if ( plan.getGenerator() instanceof TenantIdGeneration ) {
					return session.getFactory().getTenantIdentifierJavaType().toString(
							type.getPropertyValue( id, plan.getPropertyIndex(), session ) );
				}
			}
			return null;
		}
		else {
			return null;
		}
		return tenant == null ? null : session.getFactory().getTenantIdentifierJavaType().toString( tenant );
	}
}
