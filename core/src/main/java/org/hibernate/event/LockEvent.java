//$Id: LockEvent.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.event;

import org.hibernate.LockMode;

/**
 *  Defines an event class for the locking of an entity.
 *
 * @author Steve Ebersole
 */
public class LockEvent extends AbstractEvent {

	private Object object;
	private LockMode lockMode;
	private String entityName;

	public LockEvent(String entityName, Object original, LockMode lockMode, EventSource source) {
		this(original, lockMode, source);
		this.entityName = entityName;
	}

	public LockEvent(Object object, LockMode lockMode, EventSource source) {
		super(source);
		this.object = object;
		this.lockMode = lockMode;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
