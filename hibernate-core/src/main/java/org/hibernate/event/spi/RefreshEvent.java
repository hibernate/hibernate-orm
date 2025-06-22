/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;

/**
 * Event class for {@link org.hibernate.Session#refresh}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#refresh
 */
public class RefreshEvent extends AbstractEvent {

	private final Object object;
	private String entityName;

	private LockOptions lockOptions = new LockOptions(LockMode.READ);

	public RefreshEvent(Object object, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException("Entity may not be null");
		}
		this.object = object;
	}

	public RefreshEvent(String entityName, Object object, EventSource source){
		this(object, source);
		this.entityName = entityName;
	}

	public RefreshEvent(Object object, LockMode lockMode, EventSource source) {
		this(object, source);
		if (lockMode == null) {
			throw new IllegalArgumentException("LockMode may not be null");
		}
		this.lockOptions.setLockMode(lockMode);
	}

	public RefreshEvent(Object object, LockOptions lockOptions, EventSource source) {
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
	public RefreshEvent(String entityName, Object object, LockOptions lockOptions, EventSource source){
		this(object,lockOptions,source);
		this.entityName = entityName;
	}

	public Object getObject() {
		return object;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public LockMode getLockMode() {
		return lockOptions.getLockMode();
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public int getLockTimeout() {
		return lockOptions.getTimeOut();
	}

	public boolean getLockScope() {
		return lockOptions.getLockScope() == PessimisticLockScope.EXTENDED;
	}
}
