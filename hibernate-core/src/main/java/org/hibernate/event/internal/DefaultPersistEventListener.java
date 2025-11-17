/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.event.internal.EntityState.getEntityState;
import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;
import static org.hibernate.pretty.MessageHelper.infoString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Defines the default event listener used by Hibernate for persisting
 * transient entities in response to generated persist events.
 *
 * @author Gavin King
 */
public class DefaultPersistEventListener
		extends AbstractSaveEventListener<PersistContext>
		implements PersistEventListener, CallbackRegistryConsumer {

	@Override
	protected CascadingAction<PersistContext> getCascadeAction() {
		return CascadingActions.PERSIST;
	}

	/**
	 * Handle the given create event.
	 *
	 * @param event The create event to be handled.
	 *
	 */
	@Override
	public void onPersist(PersistEvent event) throws HibernateException {
		onPersist( event, PersistContext.create() );
	}

	/**
	 * Handle the given persist event.
	 *
	 * @param event The persist event to be handled
	 *
	 */
	@Override
	public void onPersist(PersistEvent event, PersistContext createCache) throws HibernateException {
		final Object object = event.getObject();
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				if ( lazyInitializer.getSession() != event.getSession() ) {
					throw new PersistentObjectException( "uninitialized proxy passed to persist()" );
				}
			}
			else {
				persist( event, createCache, lazyInitializer.getImplementation() );
			}
		}
		else {
			persist( event, createCache, object );
		}
	}

	private void persist(PersistEvent event, PersistContext createCache, Object entity) {
		final var source = event.getSession();
		final var entityEntry = source.getPersistenceContextInternal().getEntry( entity );
		final String entityName = entityName( event, entity, entityEntry );
		switch ( getEntityState( entity, entityName, entityEntry, source, true ) ) {
			case DETACHED:
				throw new PersistentObjectException( "Detached entity passed to persist: "
						+ EventUtil.getLoggableName( event.getEntityName(), entity) );
			case PERSISTENT:
				entityIsPersistent( event, createCache );
				break;
			case TRANSIENT:
				entityIsTransient( event, createCache );
				break;
			case DELETED:
				entityEntry.setStatus( Status.MANAGED );
				entityEntry.setDeletedState( null );
				source.getActionQueue().unScheduleDeletion( entityEntry, event.getObject() );
				entityIsDeleted( event, createCache );
				break;
			default:
				throw new ObjectDeletedException(
						"Deleted entity passed to persist",
						null,
						EventUtil.getLoggableName( event.getEntityName(), entity )
				);
		}
	}

	private static String entityName(PersistEvent event, Object entity, EntityEntry entityEntry) {
		if ( event.getEntityName() != null ) {
			return event.getEntityName();
		}
		else {
			// changes event.entityName by side effect!
			final String entityName = event.getSession().bestGuessEntityName( entity, entityEntry );
			event.setEntityName( entityName );
			return entityName;
		}
	}

	protected void entityIsPersistent(PersistEvent event, PersistContext createCache) {
		final var source = event.getSession();
		final String entityName = event.getEntityName();
		//TODO: check that entry.getIdentifier().equals(requestedId)
		final Object entity = source.getPersistenceContextInternal().unproxy( event.getObject() );
		if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
			final var persister = source.getEntityPersister( entityName, entity );
			EVENT_LISTENER_LOGGER.ignoringPersistentInstance(
					infoString( entityName, persister.getIdentifier( entity ) ) );
		}
		if ( createCache.add( entity ) ) {
			final var persister = source.getEntityPersister( entityName, entity );
			justCascade( createCache, source, entity, persister );
		}
	}

	private void justCascade(PersistContext createCache, EventSource source, Object entity, EntityPersister persister) {
		//TODO: merge into one method!
		cascadeBeforeSave( source, persister, entity, createCache );
		cascadeAfterSave( source, persister, entity, createCache );
	}

	protected void entityIsTransient(PersistEvent event, PersistContext createCache) {
		EVENT_LISTENER_LOGGER.persistingTransientInstance();
		final var source = event.getSession();
		final Object entity = source.getPersistenceContextInternal().unproxy( event.getObject() );
		if ( createCache.add( entity ) ) {
			saveWithGeneratedId( entity, event.getEntityName(), createCache, source, false );
		}
	}

	private void entityIsDeleted(PersistEvent event, PersistContext createCache) {
		final var source = event.getSession();
		final Object entity = source.getPersistenceContextInternal().unproxy( event.getObject() );
		final var persister = source.getEntityPersister( event.getEntityName(), entity );
		if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
			final Object id = persister.getIdentifier( entity, source );
			EVENT_LISTENER_LOGGER.unschedulingEntityDeletion(
					infoString( persister, id, source.getFactory() ) );
		}
		if ( createCache.add( entity ) ) {
			justCascade( createCache, source, entity, persister );
		}
	}
}
