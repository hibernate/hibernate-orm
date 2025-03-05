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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import static org.hibernate.event.internal.EntityState.getEntityState;

/**
 * Defines the default create event listener used by hibernate for creating
 * transient entities in response to generated create events.
 *
 * @author Gavin King
 */
public class DefaultPersistEventListener
		extends AbstractSaveEventListener<PersistContext>
		implements PersistEventListener, CallbackRegistryConsumer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultPersistEventListener.class );

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
	 * Handle the given create event.
	 *
	 * @param event The create event to be handled.
	 *
	 */
	@Override
	public void onPersist(PersistEvent event, PersistContext createCache) throws HibernateException {
		final Object object = event.getObject();
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( object );
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
		final EventSource source = event.getSession();
		final EntityEntry entityEntry = source.getPersistenceContextInternal().getEntry( entity );
		final String entityName = entityName( event, entity, entityEntry );
		switch ( getEntityState( entity, entityName, entityEntry, source, true ) ) {
			case DETACHED:
				throw new PersistentObjectException( "detached entity passed to persist: "
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
						"deleted entity passed to persist",
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
		LOG.trace( "Ignoring persistent instance" );
		final EventSource source = event.getSession();
		//TODO: check that entry.getIdentifier().equals(requestedId)
		final Object entity = source.getPersistenceContextInternal().unproxy( event.getObject() );
		if ( createCache.add( entity ) ) {
			justCascade( createCache, source, entity, source.getEntityPersister( event.getEntityName(), entity ) );
		}
	}

	private void justCascade(PersistContext createCache, EventSource source, Object entity, EntityPersister persister) {
		//TODO: merge into one method!
		cascadeBeforeSave( source, persister, entity, createCache );
		cascadeAfterSave( source, persister, entity, createCache );
	}

	/**
	 * Handle the given create event.
	 *
	 * @param event The save event to be handled.
	 * @param createCache The copy cache of entity instance to merge/copy instance.
	 */
	protected void entityIsTransient(PersistEvent event, PersistContext createCache) {
		LOG.trace( "Saving transient instance" );
		final EventSource source = event.getSession();
		final Object entity = source.getPersistenceContextInternal().unproxy( event.getObject() );
		if ( createCache.add( entity ) ) {
			saveWithGeneratedId( entity, event.getEntityName(), createCache, source, false );
		}
	}

	private void entityIsDeleted(PersistEvent event, PersistContext createCache) {
		final EventSource source = event.getSession();
		final Object entity = source.getPersistenceContextInternal().unproxy( event.getObject() );
		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
				"un-scheduling entity deletion [%s]",
				MessageHelper.infoString( persister, persister.getIdentifier( entity, source ), event.getFactory() )
			);
		}
		if ( createCache.add( entity ) ) {
			justCascade( createCache, source, entity, persister );
		}
	}
}
