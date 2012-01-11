/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.event.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;

/**
 * Defines an event class for the resolving of an entity id from the entity's natural-id
 * 
 * @author Eric Dalquist
 */
public class ResolveNaturalIdEvent extends AbstractEvent {
	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	private Map<String, Object> naturalId;
	private LockOptions lockOptions;
	private String entityClassName;
	private Serializable entityId;

	public ResolveNaturalIdEvent(Map<String, Object> naturalId, String entityClassName, EventSource source) {
		this( naturalId, entityClassName, new LockOptions(), source );
	}

	public ResolveNaturalIdEvent(Map<String, Object> naturalId, String entityClassName, LockOptions lockOptions,
			EventSource source) {
		super( source );

		if ( naturalId == null || naturalId.isEmpty() ) {
			throw new IllegalArgumentException( "id to load is required for loading" );
		}

		if ( lockOptions.getLockMode() == LockMode.WRITE ) {
			throw new IllegalArgumentException( "Invalid lock mode for loading" );
		}
		else if ( lockOptions.getLockMode() == null ) {
			lockOptions.setLockMode( DEFAULT_LOCK_MODE );
		}

		this.naturalId = naturalId;
		this.entityClassName = entityClassName;
	}

	public Map<String, Object> getNaturalId() {
		return Collections.unmodifiableMap( naturalId );
	}

	public void setNaturalId(Map<String, Object> naturalId) {
		this.naturalId = naturalId;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public void setEntityClassName(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public Serializable getEntityId() {
		return entityId;
	}

	public void setEntityId(Serializable entityId) {
		this.entityId = entityId;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public void setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}
}
