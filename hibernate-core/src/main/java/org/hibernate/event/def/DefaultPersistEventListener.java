/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.event.def;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.event.EventSource;
import org.hibernate.event.PersistEvent;
import org.hibernate.event.PersistEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.util.IdentityMap;

/**
 * Defines the default create event listener used by hibernate for creating
 * transient entities in response to generated create events.
 *
 * @author Gavin King
 */
public class DefaultPersistEventListener extends AbstractSaveEventListener implements PersistEventListener {
	private static final Logger log = LoggerFactory.getLogger(DefaultPersistEventListener.class);

	/** 
	 * Handle the given create event.
	 *
	 * @param event The create event to be handled.
	 * @throws HibernateException
	 */
	public void onPersist(PersistEvent event) throws HibernateException {
		onPersist( event, IdentityMap.instantiate(10) );
	}
		

	/** 
	 * Handle the given create event.
	 *
	 * @param event The create event to be handled.
	 * @throws HibernateException
	 */
	public void onPersist(PersistEvent event, Map createCache) throws HibernateException {
		final SessionImplementor source = event.getSession();
		final Object object = event.getObject();
		
		final Object entity;
		if ( object instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				if ( li.getSession() == source ) {
					return; //NOTE EARLY EXIT!
				}
				else {
					throw new PersistentObjectException( "uninitialized proxy passed to persist()" );
				}
			}
			entity = li.getImplementation();
		}
		else {
			entity = object;
		}

		final String entityName;
		if ( event.getEntityName() != null ) {
			entityName = event.getEntityName();
		}
		else {
			entityName = source.bestGuessEntityName( entity );
			event.setEntityName( entityName );
		}

		final EntityEntry entityEntry = source.getPersistenceContext().getEntry( entity );
		int entityState = getEntityState( entity, entityName, entityEntry, source );
		if ( entityState == DETACHED ) {
			// JPA 2, in its version of a "foreign generated", allows the id attribute value
			// to be manually set by the user, even though this manual value is irrelevant.
			// The issue is that this causes problems with the Hibernate unsaved-value strategy
			// which comes into play here in determining detached/transient state.
			//
			// Detect if we have this situation and if so null out the id value and calculate the
			// entity state again.

			// NOTE: entityEntry must be null to get here, so we cannot use any of its values
			EntityPersister persister = source.getFactory().getEntityPersister( entityName );
			if ( ForeignGenerator.class.isInstance( persister.getIdentifierGenerator() ) ) {
				if ( log.isDebugEnabled() ) {
					if ( persister.getIdentifier( entity, source ) != null ) {
						log.debug( "Resetting entity id attribute to null for foreign generator" );
					}
				}
				persister.setIdentifier( entity, null, source );
				entityState = getEntityState( entity, entityName, entityEntry, source );
			}
		}

		switch ( entityState ) {
			case DETACHED:
				throw new PersistentObjectException(
						"detached entity passed to persist: " +
								getLoggableName( event.getEntityName(), entity )
				);
			case PERSISTENT:
				entityIsPersistent( event, createCache );
				break;
			case TRANSIENT:
				entityIsTransient( event, createCache );
				break;
			default:
				throw new ObjectDeletedException(
						"deleted entity passed to persist",
						null,
						getLoggableName( event.getEntityName(), entity )
				);
		}

	}
		
	protected void entityIsPersistent(PersistEvent event, Map createCache) {
		log.trace("ignoring persistent instance");
		final EventSource source = event.getSession();
		
		//TODO: check that entry.getIdentifier().equals(requestedId)
		
		final Object entity = source.getPersistenceContext().unproxy( event.getObject() );
		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		
		if ( createCache.put(entity, entity)==null ) {
			//TODO: merge into one method!
			cascadeBeforeSave(source, persister, entity, createCache);
			cascadeAfterSave(source, persister, entity, createCache);
		}

	}
	
	/** 
	 * Handle the given create event.
	 *
	 * @param event The save event to be handled.
	 * @throws HibernateException
	 */
	protected void entityIsTransient(PersistEvent event, Map createCache) throws HibernateException {
		
		log.trace("saving transient instance");

		final EventSource source = event.getSession();
		
		final Object entity = source.getPersistenceContext().unproxy( event.getObject() );
		
		if ( createCache.put(entity, entity)==null ) {
			saveWithGeneratedId( entity, event.getEntityName(), createCache, source, false );
		}

	}

	protected CascadingAction getCascadeAction() {
		return CascadingAction.PERSIST;
	}
	
	protected Boolean getAssumedUnsaved() {
		return Boolean.TRUE;
	}

}
