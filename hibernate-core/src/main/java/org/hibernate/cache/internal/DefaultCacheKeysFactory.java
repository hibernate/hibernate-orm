/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.io.Serializable;

import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Second level cache providers now have the option to use custom key implementations.
 * This was done as the default key implementation is very generic and is quite
 * a large object to allocate in large quantities at runtime.
 * In some extreme cases, for example when the hit ratio is very low, this was making the efficiency
 * penalty vs its benefits tradeoff questionable.
 * <p>
 * Depending on configuration settings there might be opportunities to
 * use simpler key implementations, for example when multi-tenancy is not being used to
 * avoid the tenant identifier, or when a cache instance is entirely dedicated to a single type
 * to use the primary id only, skipping the role or entity name.
 * <p>
 * Even with multiple types sharing the same cache, their identifiers could be of the same
 * {@link org.hibernate.type.Type}; in this case the cache container could
 * use a single type reference to implement a custom equality function without having
 * to look it up on each equality check: that's a small optimisation but the
 * equality function is often invoked extremely frequently.
 * <p>
 * Another reason is to make it more convenient to implement custom serialization protocols when the
 * implementation supports clustering.
 *
 * @see org.hibernate.type.Type#getHashCode(Object, SessionFactoryImplementor)
 * @see org.hibernate.type.Type#isEqual(Object, Object)
 * @author Sanne Grinovero
 * @since 5.0
 */
public class DefaultCacheKeysFactory implements CacheKeysFactory {
	public static final String SHORT_NAME = "default";
	public static final DefaultCacheKeysFactory INSTANCE = new DefaultCacheKeysFactory();

	public static Object staticCreateCollectionKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		final Type keyType = persister.getKeyType();
		final Serializable disassembledKey = keyType.disassemble( id, factory );
		final boolean idIsArray = disassembledKey.getClass().isArray();
		return tenantIdentifier == null && !idIsArray
				? new BasicCacheKeyImplementation( id, disassembledKey, keyType, persister.getRole() )
				: new CacheKeyImplementation( id, disassembledKey, keyType, persister.getRole(), tenantIdentifier );
	}

	public static Object staticCreateEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		final Type keyType = persister.getIdentifierType();
		final Serializable disassembledKey = keyType.disassemble( id, factory );
		final boolean idIsArray = disassembledKey.getClass().isArray();
		return tenantIdentifier == null && !idIsArray
				? new BasicCacheKeyImplementation( id, disassembledKey, keyType, persister.getRootEntityName() )
				: new CacheKeyImplementation( id, disassembledKey, keyType, persister.getRootEntityName(), tenantIdentifier );
	}

	public static Object staticCreateNaturalIdKey(
			Object naturalIdValues,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return NaturalIdCacheKey.from( naturalIdValues, persister, session );
	}

	public static Object staticGetEntityId(Object cacheKeyObject) {
		if ( cacheKeyObject instanceof BasicCacheKeyImplementation basicCacheKey ) {
			return basicCacheKey.id;
		}
		else if ( cacheKeyObject instanceof CacheKeyImplementation cacheKey) {
			return cacheKey.getId();
		}
		else {
			throw new IllegalArgumentException( "Not an instance of CacheKeyImplementation" + cacheKeyObject );
		}
	}

	public static Object staticGetCollectionId(Object cacheKey) {
		return staticGetEntityId( cacheKey );
	}

	public static Object staticGetNaturalIdValues(Object cacheKey) {
		return ( (NaturalIdCacheKey) cacheKey ).getNaturalIdValues();
	}

	@Override
	public Object createCollectionKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return staticCreateCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object createEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return staticCreateEntityKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object createNaturalIdKey(Object naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return staticCreateNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	public Object getEntityId(Object cacheKey) {
		return staticGetEntityId( cacheKey );
	}

	@Override
	public Object getCollectionId(Object cacheKey) {
		return staticGetCollectionId( cacheKey );
	}

	@Override
	public Object getNaturalIdValues(Object cacheKey) {
		return staticGetNaturalIdValues( cacheKey );
	}
}
