/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;

/**
 *  Defines an event class for the loading of an entity.
 *
 * @author Steve Ebersole
 */
public class LoadEvent extends AbstractSessionEvent {

	private Object entityId;
	private String entityClassName;
	private Object instanceToLoad;
	private LockOptions lockOptions;
	private boolean isAssociationFetch;
	private Object result;
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

	public void setAssociationFetch(boolean associationFetch) {
		isAssociationFetch = associationFetch;
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

	public void setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
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
		return lockOptions.getTimeout().milliseconds();
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	public boolean getLockScope() {
		return lockOptions.getScope() != Locking.Scope.ROOT_ONLY;
	}
}
