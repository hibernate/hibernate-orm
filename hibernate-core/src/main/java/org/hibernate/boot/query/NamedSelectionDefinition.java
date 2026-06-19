/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import jakarta.annotation.Nonnull;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.query.spi.JpaTypedQueryReference;


/**
 * Boot-model mapping of named queries which define a selection query.
 *
 * @author Steve Ebersole
 */
public interface NamedSelectionDefinition<R>
		extends NamedQueryDefinition<R>, JpaTypedQueryReference<R> {

	@Nonnull
	String getQueryString();

	@Override
	@Nonnull
	default String getName() {
		return getRegistrationName();
	}

	/**
	 * The expected result type of the query, or {@code null}.
	 */
	@Nullable //FIXME: declared @Nonnull by JPA
	Class<R> getResultType();

	@Nullable
	Boolean getReadOnly();

	@Nullable
	Boolean getCacheable();

	@Nullable
	String getCacheRegion();

	@Nullable
	CacheMode getCacheMode();

	@Nullable
	LockMode getHibernateLockMode();

	@Nullable
	PessimisticLockScope getLockScope();

	@Nullable
	Timeout getLockTimeout();

	@Nullable
	Locking.FollowOn getFollowOnLockingStrategy();

	@Nullable
	Integer getFetchSize();

	@Override
	@Nullable
	String getEntityGraphName();
}
