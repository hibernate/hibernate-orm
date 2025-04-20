/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

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
	public Object createCollectionKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return id;
	}

	@Override
	public Object createEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return id;
	}

	@Override
	public Object createNaturalIdKey(
			Object naturalIdValues,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		// natural ids always need to be wrapped
		return NaturalIdCacheKey.from( naturalIdValues, persister, null, session );
	}

	@Override
	public Object getEntityId(Object cacheKey) {
		return cacheKey;
	}

	@Override
	public Object getCollectionId(Object cacheKey) {
		return cacheKey;
	}

	@Override
	public Object getNaturalIdValues(Object cacheKey) {
		return ((NaturalIdCacheKey) cacheKey).getNaturalIdValues();
	}
}
