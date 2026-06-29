/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Event class for {@link org.hibernate.Session#refresh}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#refresh
 */
public class RefreshEvent extends AbstractSessionEvent {

	private final Object object;
	private String entityName;

	private LockOptions lockOptions = new LockOptions(
			LockMode.READ,
			Timeouts.WAIT_FOREVER_MILLI,
			PessimisticLockScope.NORMAL,
			Locking.FollowOn.ALLOW
	);

	public RefreshEvent(@Nonnull Object object, @Nonnull EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException("Entity may not be null");
		}
		this.object = object;
	}

	public RefreshEvent(@Nullable String entityName, @Nonnull Object object, @Nonnull EventSource source){
		this(object, source);
		this.entityName = entityName;
	}

	public RefreshEvent(@Nonnull Object object, @Nonnull LockMode lockMode, @Nonnull EventSource source) {
		this(object, source);
		if (lockMode == null) {
			throw new IllegalArgumentException("LockMode may not be null");
		}
		this.lockOptions.setLockMode(lockMode);
	}

	public RefreshEvent(@Nonnull Object object, @Nonnull LockOptions lockOptions, @Nonnull EventSource source) {
		this(object, source);
		if (lockOptions == null) {
			throw new IllegalArgumentException("LockMode may not be null");
		}
		this.lockOptions = lockOptions;
	}

	/**
	 * @deprecated use {@link #RefreshEvent(Object, LockOptions, EventSource)} instead.
	 */
	@Deprecated(since = "7.0")
	public RefreshEvent(@Nullable String entityName, @Nonnull Object object, @Nonnull LockOptions lockOptions, @Nonnull EventSource source){
		this(object,lockOptions,source);
		this.entityName = entityName;
	}

	@Nonnull
	public Object getObject() {
		return object;
	}

	@Nonnull
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Nullable
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(@Nullable String entityName) {
		this.entityName = entityName;
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	@Nonnull
	public LockMode getLockMode() {
		return lockOptions.getLockMode();
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	public int getLockTimeout() {
		return lockOptions.getTimeOut();
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	public boolean getLockScope() {
		return lockOptions.getLockScope() != PessimisticLockScope.NORMAL;
	}
}
