/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;

/**
 * Event class for {@link org.hibernate.Session#lock}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#lock
 */
public class LockEvent extends AbstractEvent {
	public static final String ILLEGAL_SKIP_LOCKED = "Skip-locked is not valid option for #lock";

	private Object object;
	private final LockOptions lockOptions;
	private String entityName;

	public LockEvent(String entityName, Object object, LockMode lockMode, EventSource source) {
		this( entityName, object, lockMode.toLockOptions(), source );
	}

	public LockEvent(String entityName, Object object, LockOptions lockOptions, EventSource source) {
		super(source);
		this.object = object;
		this.lockOptions = lockOptions;
		if ( lockOptions.getLockMode() == LockMode.UPGRADE_SKIPLOCKED
			|| lockOptions.getTimeout().milliseconds() == Timeouts.SKIP_LOCKED_MILLI ) {
			throw new IllegalArgumentException( ILLEGAL_SKIP_LOCKED );
		}
	}

	public LockEvent(Object object, LockMode lockMode, EventSource source) {
		this( object, lockMode.toLockOptions(), source );
	}

	public LockEvent(Object object, LockOptions lockOptions, EventSource source) {
		this( null, object, lockOptions, source );
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}


	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
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
		return lockOptions.getScope() != Locking.Scope.ROOT_ONLY;
	}
}
