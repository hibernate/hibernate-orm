/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.query.spi.JpaTypedQueryReference;

/// Models NamedQueryMemento which is a selection-query
///
/// @author Steve Ebersole
public interface NamedSelectionMemento<T> extends NamedQueryMemento<T>, JpaTypedQueryReference<T> {
	@Nonnull
	String getSelectionString();

	@Nullable
	Boolean getReadOnly();

	@Nullable
	Integer getFetchSize();

	@Nullable
	Integer getFirstResult();

	@Nullable
	Integer getMaxResults();

	@Nullable
	Boolean getCacheable();

	@Nullable
	String getCacheRegion();

	@Nullable
	CacheMode getCacheMode();

	@Nullable
	LockMode getHibernateLockMode();

	@Nullable
	PessimisticLockScope getPessimisticLockScope();

	@Nullable
	Timeout getLockTimeout();

	@Nullable
	Locking.FollowOn getFollowOnLockingStrategy();
}
