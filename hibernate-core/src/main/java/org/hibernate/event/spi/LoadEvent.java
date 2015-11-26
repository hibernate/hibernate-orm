/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;

/**
 *  Defines an event class for the loading of an entity.
 *
 * @author Steve Ebersole
 */
public class LoadEvent extends AbstractEvent {
	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;
	//Performance hotspot: avoid allocating unneeded LockOptions
	public static final LockOptions DEFAULT_LOCK_OPTIONS = new LockOptions() {
		@Override
		public LockOptions setLockMode(LockMode lockMode) {
			throw new AssertionFailure( "Should not be invoked: DEFAULT_LOCK_OPTIONS needs to be treated as immutable." );
		}
		@Override
		public LockOptions setAliasSpecificLockMode(String alias, LockMode lockMode) {
			throw new AssertionFailure( "Should not be invoked: DEFAULT_LOCK_OPTIONS needs to be treated as immutable." );
		}
		@Override
		public LockOptions setTimeOut(int timeout) {
			throw new AssertionFailure( "Should not be invoked: DEFAULT_LOCK_OPTIONS needs to be treated as immutable." );
		}
		@Override
		public LockOptions setScope(boolean scope) {
			throw new AssertionFailure( "Should not be invoked: DEFAULT_LOCK_OPTIONS needs to be treated as immutable." );
		}
	};

	private Serializable entityId;
	private String entityClassName;
	private Object instanceToLoad;
	private LockOptions lockOptions;
	private boolean isAssociationFetch;
	private Object result;
	private PostLoadEvent postLoadEvent;

	public LoadEvent(Serializable entityId, Object instanceToLoad, EventSource source) {
		this( entityId, null, instanceToLoad, DEFAULT_LOCK_OPTIONS, false, source );
	}

	public LoadEvent(Serializable entityId, String entityClassName, LockMode lockMode, EventSource source) {
		this( entityId, entityClassName, null, lockMode, false, source );
	}

	public LoadEvent(Serializable entityId, String entityClassName, LockOptions lockOptions, EventSource source) {
		this( entityId, entityClassName, null, lockOptions, false, source );
	}

	public LoadEvent(Serializable entityId, String entityClassName, boolean isAssociationFetch, EventSource source) {
		this( entityId, entityClassName, null, DEFAULT_LOCK_OPTIONS, isAssociationFetch, source );
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
		this( entityId, entityClassName, instanceToLoad,
				lockMode == DEFAULT_LOCK_MODE ? DEFAULT_LOCK_OPTIONS : new LockOptions().setLockMode( lockMode ),
				isAssociationFetch, source );
	}

	private LoadEvent(
			Serializable entityId,
			String entityClassName,
			Object instanceToLoad,
			LockOptions lockOptions,
			boolean isAssociationFetch,
			EventSource source) {

		super(source);

		if ( entityId == null ) {
			throw new IllegalArgumentException("id to load is required for loading");
		}

		if ( lockOptions.getLockMode() == LockMode.WRITE ) {
			throw new IllegalArgumentException("Invalid lock mode for loading");
		}
		else if ( lockOptions.getLockMode() == null ) {
			lockOptions.setLockMode(DEFAULT_LOCK_MODE);
		}

		this.entityId = entityId;
		this.entityClassName = entityClassName;
		this.instanceToLoad = instanceToLoad;
		this.lockOptions = lockOptions;
		this.isAssociationFetch = isAssociationFetch;
		this.postLoadEvent = new PostLoadEvent( source );
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

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public LockMode getLockMode() {
		return lockOptions.getLockMode();
	}

	public void setLockMode(LockMode lockMode) {
		if ( lockMode != lockOptions.getLockMode() ) {
			writingOnLockOptions();
			this.lockOptions.setLockMode( lockMode );
		}
	}

	private void writingOnLockOptions() {
		if ( lockOptions == DEFAULT_LOCK_OPTIONS ) {
			this.lockOptions = new LockOptions();
		}
	}

	public void setLockTimeout(int timeout) {
		if ( timeout != lockOptions.getTimeOut() ) {
			writingOnLockOptions();
			this.lockOptions.setTimeOut( timeout );
		}
	}

	public int getLockTimeout() {
		return this.lockOptions.getTimeOut();
	}

	public void setLockScope(boolean cascade) {
		if ( lockOptions.getScope() != cascade ) {
			writingOnLockOptions();
			this.lockOptions.setScope( cascade );
		}
	}

	public boolean getLockScope() {
		return this.lockOptions.getScope();
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
}
