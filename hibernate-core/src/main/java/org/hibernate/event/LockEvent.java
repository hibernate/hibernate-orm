/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.event;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;

/**
 *  Defines an event class for the locking of an entity.
 *
 * @author Steve Ebersole
 */
public class LockEvent extends AbstractEvent {

	private Object object;
	private LockOptions lockOptions;
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
		this.object = object;
		this.lockOptions = new LockOptions().setLockMode(lockMode);
	}

	public LockEvent(Object object, LockOptions lockOptions, EventSource source) {
		super(source);
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

	public void setLockMode(LockMode lockMode) {
		this.lockOptions.setLockMode(lockMode);
	}

	public void setLockTimeout(int timeout) {
		this.lockOptions.setTimeOut(timeout);
	}

	public int getLockTimeout() {
		return this.lockOptions.getTimeOut();
	}

	public void setLockScope(boolean cascade) {
		this.lockOptions.setScope(cascade);
	}

	public boolean getLockScope() {
		return this.lockOptions.getScope();
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
