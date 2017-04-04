/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

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
