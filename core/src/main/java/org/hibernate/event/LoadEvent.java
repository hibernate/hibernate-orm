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
