/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Event class for {@link org.hibernate.Session#lock}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#lock
 */
public class LockEvent extends AbstractSessionEvent {
	public static final String ILLEGAL_SKIP_LOCKED = "Skip-locked is not valid option for #lock";

	private Object object;
	private final LockOptions lockOptions;
	private String entityName;

	public LockEvent(@Nullable String entityName, @Nonnull Object object, @Nonnull LockOptions lockOptions, @Nonnull EventSource source) {
		super(source);

		if (object == null) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		if (lockOptions == null) {
			throw new IllegalArgumentException( "LockOptions may not be null" );
		}
		if ( lockOptions.getLockMode() == LockMode.UPGRADE_SKIPLOCKED
			|| lockOptions.getTimeout().milliseconds() == Timeouts.SKIP_LOCKED_MILLI ) {
			throw new IllegalArgumentException( ILLEGAL_SKIP_LOCKED );
		}

		this.entityName = entityName;
		this.object = object;
		this.lockOptions = lockOptions;
	}

	public LockEvent(@Nonnull Object object, @Nonnull LockOptions lockOptions, @Nonnull EventSource source) {
		this( null, object, lockOptions, source );
	}

	/**
	 * @deprecated Use {@linkplain LockEvent#LockEvent(Object, LockOptions, EventSource)} instead.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockEvent(@Nonnull Object object, @Nonnull LockMode lockMode, @Nonnull EventSource source) {
		this( object, lockMode.toLockOptions(), source );
	}

	/**
	 * @deprecated Use {@linkplain LockEvent#LockEvent(String, Object, LockOptions, EventSource)} instead.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public LockEvent(@Nullable String entityName, @Nonnull Object object, @Nonnull LockMode lockMode, @Nonnull EventSource source) {
		this( entityName, object, lockMode.toLockOptions(), source );
	}

	@Nonnull
	public Object getObject() {
		return object;
	}

	public void setObject(@Nonnull Object object) {
		this.object = object;
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
