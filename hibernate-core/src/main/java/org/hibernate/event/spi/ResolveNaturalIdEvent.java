/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Defines an event class for the resolving of an entity id from the entity's natural-id
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public class ResolveNaturalIdEvent extends AbstractEvent {
	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	private final EntityPersister entityPersister;
	private final Map<String, Object> naturalIdValues;
	private final Object[] orderedNaturalIdValues;
	private final LockOptions lockOptions;

	private Serializable entityId;

	public ResolveNaturalIdEvent(Map<String, Object> naturalIdValues, EntityPersister entityPersister, EventSource source) {
		this( naturalIdValues, entityPersister, new LockOptions(), source );
	}

	public ResolveNaturalIdEvent(
			Map<String, Object> naturalIdValues,
			EntityPersister entityPersister,
			LockOptions lockOptions,
			EventSource source) {
		super( source );

		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "EntityPersister is required for loading" );
		}

		if ( ! entityPersister.hasNaturalIdentifier() ) {
			throw new HibernateException( "Entity did not define a natural-id" );
		}

		if ( naturalIdValues == null || naturalIdValues.isEmpty() ) {
			throw new IllegalArgumentException( "natural-id to load is required" );
		}

		if ( entityPersister.getNaturalIdentifierProperties().length != naturalIdValues.size() ) {
			throw new HibernateException(
					String.format(
						"Entity [%s] defines its natural-id with %d properties but only %d were specified",
						entityPersister.getEntityName(),
						entityPersister.getNaturalIdentifierProperties().length,
						naturalIdValues.size()
					)
			);
		}

		if ( lockOptions.getLockMode() == LockMode.WRITE ) {
			throw new IllegalArgumentException( "Invalid lock mode for loading" );
		}
		else if ( lockOptions.getLockMode() == null ) {
			lockOptions.setLockMode( DEFAULT_LOCK_MODE );
		}

		this.entityPersister = entityPersister;
		this.naturalIdValues = naturalIdValues;
		this.lockOptions = lockOptions;

		int[] naturalIdPropertyPositions = entityPersister.getNaturalIdentifierProperties();
		orderedNaturalIdValues = new Object[naturalIdPropertyPositions.length];
		int i = 0;
		for ( int position : naturalIdPropertyPositions ) {
			final String propertyName = entityPersister.getPropertyNames()[position];
			if ( ! naturalIdValues.containsKey( propertyName ) ) {
				throw new HibernateException(
						String.format( "No value specified for natural-id property %s#%s", getEntityName(), propertyName )
				);
			}
			orderedNaturalIdValues[i++] = naturalIdValues.get( entityPersister.getPropertyNames()[position] );
		}
	}

	public Map<String, Object> getNaturalIdValues() {
		return Collections.unmodifiableMap( naturalIdValues );
	}

	public Object[] getOrderedNaturalIdValues() {
		return orderedNaturalIdValues;
	}

	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	public String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public Serializable getEntityId() {
		return entityId;
	}

	public void setEntityId(Serializable entityId) {
		this.entityId = entityId;
	}
}
