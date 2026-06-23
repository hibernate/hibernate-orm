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

/**
 * Factory that does not fill in the entityName or role
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class SimpleCacheKeysFactory implements CacheKeysFactory {
	public static final String SHORT_NAME = "simple";
	public static CacheKeysFactory INSTANCE = new SimpleCacheKeysFactory();

	@Override
	@Nonnull
	public Object createCollectionKey(
			@Nonnull Object id,
			@Nonnull CollectionPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return id;
	}

	@Override
	@Nonnull
	public Object createEntityKey(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return id;
	}

	@Override
	@Nonnull
	public Object createNaturalIdKey(
			@Nonnull Object naturalIdValues,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		// natural ids always need to be wrapped
		return NaturalIdCacheKey.from( naturalIdValues, persister, null, session );
	}

	@Override
	@Nonnull
	public Object getEntityId(@Nonnull Object cacheKey) {
		return cacheKey;
	}

	@Override
	@Nonnull
	public Object getCollectionId(@Nonnull Object cacheKey) {
		return cacheKey;
	}

	@Override
	@Nonnull
	public Object getNaturalIdValues(@Nonnull Object cacheKey) {
		return ((NaturalIdCacheKey) cacheKey).getNaturalIdValues();
	}
}
