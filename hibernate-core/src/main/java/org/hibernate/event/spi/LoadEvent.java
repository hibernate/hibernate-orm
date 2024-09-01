/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;

/**
 *  Defines an event class for the loading of an entity.
 *
 * @author Steve Ebersole
 */
public class LoadEvent extends AbstractEvent {
//	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;
//	public static final LockOptions DEFAULT_LOCK_OPTIONS = DEFAULT_LOCK_MODE.toLockOptions();

	private Object entityId;
	private String entityClassName;
	private Object instanceToLoad;
	private final LockOptions lockOptions;
	private final boolean isAssociationFetch;
	private Object result;
	private PostLoadEvent postLoadEvent;
	private Boolean readOnly;

	public LoadEvent(Object entityId, Object instanceToLoad, EventSource source, Boolean readOnly) {
		this( entityId, null, instanceToLoad, LockMode.NONE.toLockOptions(), false, source, readOnly );
	}

	public LoadEvent(Object entityId, String entityClassName, LockMode lockMode, EventSource source, Boolean readOnly) {
		this( entityId, entityClassName, null, lockMode, false, source, readOnly );
	}

	public LoadEvent(Object entityId, String entityClassName, LockOptions lockOptions, EventSource source, Boolean readOnly) {
		this( entityId, entityClassName, null, lockOptions, false, source, readOnly );
	}

	public LoadEvent(Object entityId, String entityClassName, boolean isAssociationFetch, EventSource source, Boolean readOnly) {
		this( entityId, entityClassName, null, LockMode.NONE.toLockOptions(), isAssociationFetch, source, readOnly );
	}

	private LoadEvent(
			Object entityId,
			String entityClassName,
			Object instanceToLoad,
			LockMode lockMode,
			boolean isAssociationFetch,
			EventSource source,
			Boolean readOnly) {
		this(
				entityId,
				entityClassName,
				instanceToLoad,
				lockMode.toLockOptions(),
				isAssociationFetch,
				source,
				readOnly
		);
	}

	private LoadEvent(
			Object entityId,
			String entityClassName,
			Object instanceToLoad,
			LockOptions lockOptions,
			boolean isAssociationFetch,
			EventSource source,
			Boolean readOnly) {

		super( source );

		if ( entityId == null ) {
			throw new IllegalArgumentException( "id to load is required for loading" );
		}

		if ( lockOptions.getLockMode() == LockMode.WRITE ) {
			throw new IllegalArgumentException("Invalid lock mode for loading");
		}
		else if ( lockOptions.getLockMode() == null ) {
			lockOptions.setLockMode( LockMode.NONE );
		}

		this.entityId = entityId;
		this.entityClassName = entityClassName;
		this.instanceToLoad = instanceToLoad;
		this.lockOptions = lockOptions;
		this.isAssociationFetch = isAssociationFetch;
		this.postLoadEvent = new PostLoadEvent( source );
		this.readOnly = readOnly;
	}

	public Object getEntityId() {
		return entityId;
	}

	public void setEntityId(Object entityId) {
		this.entityId = entityId;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public void setEntityClassName(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public boolean isAssociationFetch() {
		return isAssociationFetch;
	}

	public Object getInstanceToLoad() {
		return instanceToLoad;
	}

	public void setInstanceToLoad(Object instanceToLoad) {
		this.instanceToLoad = instanceToLoad;
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

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public PostLoadEvent getPostLoadEvent() {
		return postLoadEvent;
	}

	public void setPostLoadEvent(PostLoadEvent postLoadEvent) {
		this.postLoadEvent = postLoadEvent;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
	}
}
