/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import jakarta.persistence.LockModeType;
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
	String getSelectionString();

	Boolean getReadOnly();

	Integer getFetchSize();

	Integer getFirstResult();
	Integer getMaxResults();

	Boolean getCacheable();
	String getCacheRegion();
	CacheMode getCacheMode();

	LockMode getHibernateLockMode();
	@Override
	LockModeType getLockMode();
	@Override
	PessimisticLockScope getPessimisticLockScope();
	Timeout getLockTimeout();
	Locking.FollowOn getFollowOnLockingStrategy();
}
