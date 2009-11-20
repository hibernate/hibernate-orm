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
import org.hibernate.LockRequest;

/**
 *  Defines an event class for the locking of an entity.
 *
 * @author Steve Ebersole
 */
public class LockEvent extends AbstractEvent {

	private Object object;
	private LockRequest lockRequest;
	private String entityName;

	public LockEvent(String entityName, Object original, LockMode lockMode, EventSource source) {
		this(original, lockMode, source);
		this.entityName = entityName;
	}

	public LockEvent(String entityName, Object original, LockRequest lockRequest, EventSource source) {
		this(original, lockRequest, source);
		this.entityName = entityName;
	}

	public LockEvent(Object object, LockMode lockMode, EventSource source) {
		super(source);
		this.object = object;
		this.lockRequest = new LockRequest().setLockMode(lockMode);
	}

	public LockEvent(Object object, LockRequest lockRequest, EventSource source) {
		super(source);
		this.object = object;
		this.lockRequest = lockRequest;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public LockRequest getLockRequest() {
		return lockRequest;	
	}

	public LockMode getLockMode() {
		return lockRequest.getLockMode();
	}

	public void setLockMode(LockMode lockMode) {
		this.lockRequest.setLockMode(lockMode);
	}

	public void setLockTimeout(int timeout) {
		this.lockRequest.setTimeOut(timeout);
	}

	public int getLockTimeout() {
		return this.lockRequest.getTimeOut();
	}

	public void setLockScope(boolean cascade) {
		this.lockRequest.setScope(cascade);
	}

	public boolean getLockScope() {
		return this.lockRequest.getScope();
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
