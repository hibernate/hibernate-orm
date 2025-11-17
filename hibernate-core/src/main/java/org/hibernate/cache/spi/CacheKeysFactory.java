/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A factory for keys into the second-level cache.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public interface CacheKeysFactory {
	Object createCollectionKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier);

	Object createEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier);

	Object createNaturalIdKey(Object naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session);

	Object getEntityId(Object cacheKey);

	Object getCollectionId(Object cacheKey);

	Object getNaturalIdValues(Object cacheKey);
}
