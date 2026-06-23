/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nonnull
	Object createCollectionKey(
			@Nonnull Object id,
			@Nonnull CollectionPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier);

	@Nonnull
	Object createEntityKey(
			@Nonnull Object id,
			@Nonnull EntityPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier);

	@Nonnull
	Object createNaturalIdKey(
			@Nonnull Object naturalIdValues,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session);

	@Nonnull
	Object getEntityId(@Nonnull Object cacheKey);

	@Nonnull
	Object getCollectionId(@Nonnull Object cacheKey);

	@Nonnull
	Object getNaturalIdValues(@Nonnull Object cacheKey);
}
