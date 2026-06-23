/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * Second-level cache providers now have the option to use custom key implementations.
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

	@Nonnull
	public static Object staticCreateCollectionKey(
			@Nonnull Object id,
			@Nonnull CollectionPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		final Type keyType = persister.getKeyType();
		final var coercedId = getCoercedId( id, keyType );
		final var disassembledKey = keyType.disassemble( coercedId, factory );
		final boolean idIsArray = disassembledKey.getClass().isArray();
		final String role = persister.getRole();
		return tenantIdentifier == null && !idIsArray
				? new BasicCacheKeyImplementation( coercedId, disassembledKey, keyType, role )
				: new CacheKeyImplementation( coercedId, disassembledKey, keyType, role, tenantIdentifier );
	}

	@Nonnull
	public static Object staticCreateEntityKey(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		final Type keyType = persister.getIdentifierType();
		final var coercedId = getCoercedId( id, keyType );
		final var disassembledKey = keyType.disassemble( coercedId, factory );
		final boolean idIsArray = disassembledKey.getClass().isArray();
		final String rootEntityName = persister.getRootEntityName();
		return tenantIdentifier == null && !idIsArray
				? new BasicCacheKeyImplementation( coercedId, disassembledKey, keyType, rootEntityName )
				: new CacheKeyImplementation( coercedId, disassembledKey, keyType, rootEntityName, tenantIdentifier );
	}

	@Nonnull
	private static Object getCoercedId(@Nonnull Object id, @Nonnull Type keyType) {
		return keyType instanceof BasicType<?> basicType
				? basicType.getJavaTypeDescriptor().coerce( id )
				: id;
	}

	@Nonnull
	public static Object staticCreateNaturalIdKey(
			@Nonnull Object naturalIdValues,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		return NaturalIdCacheKey.from( naturalIdValues, persister, session );
	}

	@Nonnull
	public static Object staticGetEntityId(@Nonnull Object cacheKeyObject) {
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

	@Nonnull
	public static Object staticGetCollectionId(@Nonnull Object cacheKey) {
		return staticGetEntityId( cacheKey );
	}

	@Nonnull
	public static Object staticGetNaturalIdValues(@Nonnull Object cacheKey) {
		return ( (NaturalIdCacheKey) cacheKey ).getNaturalIdValues();
	}

	@Override
	@Nonnull
	public Object createCollectionKey(
			@Nonnull Object id,
			@Nonnull CollectionPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return staticCreateCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	@Nonnull
	public Object createEntityKey(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return staticCreateEntityKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	@Nonnull
	public Object createNaturalIdKey(
			@Nonnull Object naturalIdValues,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		return staticCreateNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	@Nonnull
	public Object getEntityId(@Nonnull Object cacheKey) {
		return staticGetEntityId( cacheKey );
	}

	@Override
	@Nonnull
	public Object getCollectionId(@Nonnull Object cacheKey) {
		return staticGetCollectionId( cacheKey );
	}

	@Override
	@Nonnull
	public Object getNaturalIdValues(@Nonnull Object cacheKey) {
		return staticGetNaturalIdValues( cacheKey );
	}
}
