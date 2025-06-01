/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;

/**
 * Event class for {@link org.hibernate.Session#lock}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#lock
 */
public class LockEvent extends AbstractEvent {

	private Object object;
	private final LockOptions lockOptions;
	private String entityName;

	public LockEvent(String entityName, Object original, LockMode lockMode, EventSource source) {
		this(original, lockMode, source);
		this.entityName = entityName;
	}

	public LockEvent(String entityName, Object original, LockOptions lockOptions, EventSource source) {
		this(original, lockOptions, source);
		this.entityName = entityName;
	}

	public LockEvent(Object object, LockMode lockMode, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		if (lockMode == null) {
			throw new IllegalArgumentException( "LockMode may not be null" );
		}
		this.object = object;
		this.lockOptions = lockMode.toLockOptions();
	}

	public LockEvent(Object object, LockOptions lockOptions, EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		if (lockOptions == null) {
			throw new IllegalArgumentException( "LockOptions may not be null" );
		}
		this.object = object;
		this.lockOptions = lockOptions;
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

	public LockMode getLockMode() {
		return lockOptions.getLockMode();
	}

	public int getLockTimeout() {
		return lockOptions.getTimeOut();
	}

	public boolean getLockScope() {
		return lockOptions.getLockScope() == PessimisticLockScope.EXTENDED;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
