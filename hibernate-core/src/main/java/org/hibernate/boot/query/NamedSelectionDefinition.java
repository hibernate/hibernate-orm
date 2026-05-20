/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
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
	String getQueryString();

	@Override
	default String getName() {
		return getRegistrationName();
	}

	/**
	 * The expected result type of the query, or {@code null}.
	 */
	@Nullable
	Class<R> getResultType();

	Boolean getReadOnly();

	Boolean getCacheable();

	String getCacheRegion();

	CacheMode getCacheMode();

	LockMode getHibernateLockMode();

	PessimisticLockScope getLockScope();

	Timeout getLockTimeout();

	Locking.FollowOn getFollowOnLockingStrategy();

	Integer getFetchSize();

	@Override
	String getEntityGraphName();
}
