/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractDelegateSessionImplementor extends SessionDelegatorBaseImpl implements SessionImplementor {
	public AbstractDelegateSessionImplementor(SessionImplementor delegate) {
		super( delegate );
	}

	public abstract Object doImmediateLoad(String entityName);

	@Override
	public Object immediateLoad(@Nonnull String entityName, @Nonnull Object id) throws HibernateException {
		return doImmediateLoad( entityName );
	}

	@Override
	@SuppressWarnings("removal")
	public LockOptions getDefaultLockOptions() {
		return delegate.getDefaultLockOptions();
	}

	@Override
	@Nullable
	public Timeout getDefaultLockTimeout() {
		return delegate.getDefaultLockTimeout();
	}

	@Override
	@Nullable
	public Timeout getDefaultTimeout() {
		return delegate.getDefaultTimeout();
	}
}
