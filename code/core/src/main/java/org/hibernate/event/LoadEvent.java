//$Id: LoadEvent.java 7785 2005-08-08 23:24:44Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.LockMode;

/**
 *  Defines an event class for the loading of an entity.
 *
 * @author Steve Ebersole
 */
public class LoadEvent extends AbstractEvent {

	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	private Serializable entityId;
	private String entityClassName;
	private Object instanceToLoad;
	private LockMode lockMode;
	private boolean isAssociationFetch;
	private Object result;

	public LoadEvent(Serializable entityId, Object instanceToLoad, EventSource source) {
		this(entityId, null, instanceToLoad, null, false, source);
	}

	public LoadEvent(Serializable entityId, String entityClassName, LockMode lockMode, EventSource source) {
		this(entityId, entityClassName, null, lockMode, false, source);
	}
	
	public LoadEvent(Serializable entityId, String entityClassName, boolean isAssociationFetch, EventSource source) {
		this(entityId, entityClassName, null, null, isAssociationFetch, source);
	}
	
	public boolean isAssociationFetch() {
		return isAssociationFetch;
	}

	private LoadEvent(
			Serializable entityId,
			String entityClassName,
			Object instanceToLoad,
			LockMode lockMode,
			boolean isAssociationFetch,
			EventSource source) {

		super(source);

		if ( entityId == null ) {
			throw new IllegalArgumentException("id to load is required for loading");
		}

		if ( lockMode == LockMode.WRITE ) {
			throw new IllegalArgumentException("Invalid lock mode for loading");
		}
		else if ( lockMode == null ) {
			lockMode = DEFAULT_LOCK_MODE;
		}

		this.entityId = entityId;
		this.entityClassName = entityClassName;
		this.instanceToLoad = instanceToLoad;
		this.lockMode = lockMode;
		this.isAssociationFetch = isAssociationFetch;
	}

	public Serializable getEntityId() {
		return entityId;
	}

	public void setEntityId(Serializable entityId) {
		this.entityId = entityId;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public void setEntityClassName(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public Object getInstanceToLoad() {
		return instanceToLoad;
	}

	public void setInstanceToLoad(Object instanceToLoad) {
		this.instanceToLoad = instanceToLoad;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
