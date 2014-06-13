/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.TypeHelper;

/**
 * Defines the default copy event listener used by hibernate for copying entities
 * in response to generated copy events.
 *
 * @author Gavin King
 */
public class DefaultMergeEventListener extends AbstractSaveEventListener implements MergeEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultMergeEventListener.class );

	@Override
	protected Map getMergeMap(Object anything) {
		return ( (MergeContext) anything ).invertMap();
	}

	protected EntityCopyObserver createEntityCopyObserver() {
		return new EntityCopyNotAllowedObserver();
	}

	/**
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 *
	 * @throws HibernateException
	 */
	public void onMerge(MergeEvent event) throws HibernateException {
		final EntityCopyObserver entityCopyObserver = createEntityCopyObserver();
		final MergeContext mergeContext = new MergeContext( event.getSession(), entityCopyObserver );
		try {
			onMerge( event, mergeContext );
			entityCopyObserver.topLevelMergeComplete( event.getSession() );
		}
		finally {
			entityCopyObserver.clear();
			mergeContext.clear();
		}
	}

	/**
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 *
	 * @throws HibernateException
	 */
	public void onMerge(MergeEvent event, Map copiedAlready) throws HibernateException {

		final MergeContext copyCache = (MergeContext) copiedAlready;
		final EventSource source = event.getSession();
		final Object original = event.getOriginal();

		if ( original != null ) {

			final Object entity;
			if ( original instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) original ).getHibernateLazyInitializer();
				if ( li.isUninitialized() ) {
					LOG.trace( "Ignoring uninitialized proxy" );
					event.setResult( source.load( li.getEntityName(), li.getIdentifier() ) );
					return; //EARLY EXIT!
				}
				else {
					entity = li.getImplementation();
				}
			}
			else {
				entity = original;
			}

			if ( copyCache.containsKey( entity ) &&
					( copyCache.isOperatedOn( entity ) ) ) {
				LOG.trace( "Already in merge process" );
				event.setResult( entity );
			}
			else {
				if ( copyCache.containsKey( entity ) ) {
					LOG.trace( "Already in copyCache; setting in merge process" );
					copyCache.setOperatedOn( entity, true );
				}
				event.setEntity( entity );
				EntityState entityState = null;

				// Check the persistence context for an entry relating to this
				// entity to be merged...
				EntityEntry entry = source.getPersistenceContext().getEntry( entity );
				if ( entry == null ) {
					EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
					Serializable id = persister.getIdentifier( entity, source );
					if ( id != null ) {
						final EntityKey key = source.generateEntityKey( id, persister );
						final Object managedEntity = source.getPersistenceContext().getEntity( key );
						entry = source.getPersistenceContext().getEntry( managedEntity );
						if ( entry != null ) {
							// we have specialized case of a detached entity from the
							// perspective of the merge operation.  Specifically, we
							// have an incoming entity instance which has a corresponding
							// entry in the current persistence context, but registered
							// under a different entity instance
							entityState = EntityState.DETACHED;
						}
					}
				}

				if ( entityState == null ) {
					entityState = getEntityState( entity, event.getEntityName(), entry, source );
				}

				switch ( entityState ) {
					case DETACHED:
						entityIsDetached( event, copyCache );
						break;
					case TRANSIENT:
						entityIsTransient( event, copyCache );
						break;
					case PERSISTENT:
						entityIsPersistent( event, copyCache );
						break;
					default: //DELETED
						throw new ObjectDeletedException(
								"deleted instance passed to merge",
								null,
								getLoggableName( event.getEntityName(), entity )
						);
				}
			}

		}

	}

	protected void entityIsPersistent(MergeEvent event, Map copyCache) {
		LOG.trace( "Ignoring persistent instance" );

		//TODO: check that entry.getIdentifier().equals(requestedId)

		final Object entity = event.getEntity();
		final EventSource source = event.getSession();
		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );

		( (MergeContext) copyCache ).put( entity, entity, true );  //before cascade!

		cascadeOnMerge( source, persister, entity, copyCache );
		copyValues( persister, entity, entity, source, copyCache );

		event.setResult( entity );
	}

	protected void entityIsTransient(MergeEvent event, Map copyCache) {

		LOG.trace( "Merging transient instance" );

		final Object entity = event.getEntity();
		final EventSource source = event.getSession();

		final String entityName = event.getEntityName();
		final EntityPersister persister = source.getEntityPersister( entityName, entity );

		final Serializable id = persister.hasIdentifierProperty() ?
				persister.getIdentifier( entity, source ) :
				null;
		if ( copyCache.containsKey( entity ) ) {
			persister.setIdentifier( copyCache.get( entity ), id, source );
		}
		else {
			( (MergeContext) copyCache ).put( entity, source.instantiate( persister, id ), true ); //before cascade!
		}
		final Object copy = copyCache.get( entity );

		// cascade first, so that all unsaved objects get their
		// copy created before we actually copy
		//cascadeOnMerge(event, persister, entity, copyCache, Cascades.CASCADE_BEFORE_MERGE);
		super.cascadeBeforeSave( source, persister, entity, copyCache );
		copyValues( persister, entity, copy, source, copyCache, ForeignKeyDirection.FROM_PARENT );

		saveTransientEntity( copy, entityName, event.getRequestedId(), source, copyCache );

		// cascade first, so that all unsaved objects get their
		// copy created before we actually copy
		super.cascadeAfterSave( source, persister, entity, copyCache );
		copyValues( persister, entity, copy, source, copyCache, ForeignKeyDirection.TO_PARENT );

		event.setResult( copy );
	}

	private void saveTransientEntity(
			Object entity,
			String entityName,
			Serializable requestedId,
			EventSource source,
			Map copyCache) {
		//this bit is only *really* absolutely necessary for handling
		//requestedId, but is also good if we merge multiple object
		//graphs, since it helps ensure uniqueness
		if ( requestedId == null ) {
			saveWithGeneratedId( entity, entityName, copyCache, source, false );
		}
		else {
			saveWithRequestedId( entity, requestedId, entityName, copyCache, source );
		}
	}

	protected void entityIsDetached(MergeEvent event, Map copyCache) {

		LOG.trace( "Merging detached instance" );

		final Object entity = event.getEntity();
		final EventSource source = event.getSession();

		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		final String entityName = persister.getEntityName();

		Serializable id = event.getRequestedId();
		if ( id == null ) {
			id = persister.getIdentifier( entity, source );
		}
		else {
			// check that entity id = requestedId
			Serializable entityId = persister.getIdentifier( entity, source );
			if ( !persister.getIdentifierType().isEqual( id, entityId, source.getFactory() ) ) {
				throw new HibernateException( "merge requested with id not matching id of passed entity" );
			}
		}

		String previousFetchProfile = source.getFetchProfile();
		source.setFetchProfile( "merge" );
		//we must clone embedded composite identifiers, or
		//we will get back the same instance that we pass in
		final Serializable clonedIdentifier = (Serializable) persister.getIdentifierType()
				.deepCopy( id, source.getFactory() );
		final Object result = source.get( entityName, clonedIdentifier );
		source.setFetchProfile( previousFetchProfile );

		if ( result == null ) {
			//TODO: we should throw an exception if we really *know* for sure
			//      that this is a detached instance, rather than just assuming
			//throw new StaleObjectStateException(entityName, id);

			// we got here because we assumed that an instance
			// with an assigned id was detached, when it was
			// really persistent
			entityIsTransient( event, copyCache );
		}
		else {
			( (MergeContext) copyCache ).put( entity, result, true ); //before cascade!

			final Object target = source.getPersistenceContext().unproxy( result );
			if ( target == entity ) {
				throw new AssertionFailure( "entity was not detached" );
			}
			else if ( !source.getEntityName( target ).equals( entityName ) ) {
				throw new WrongClassException(
						"class of the given object did not match class of persistent copy",
						event.getRequestedId(),
						entityName
				);
			}
			else if ( isVersionChanged( entity, source, persister, target ) ) {
				if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
					source.getFactory().getStatisticsImplementor()
							.optimisticFailure( entityName );
				}
				throw new StaleObjectStateException( entityName, id );
			}

			// cascade first, so that all unsaved objects get their
			// copy created before we actually copy
			cascadeOnMerge( source, persister, entity, copyCache );
			copyValues( persister, entity, target, source, copyCache );

			//copyValues works by reflection, so explicitly mark the entity instance dirty
			markInterceptorDirty( entity, target, persister );

			event.setResult( result );
		}

	}

	private void markInterceptorDirty(final Object entity, final Object target, EntityPersister persister) {
		if ( persister.getInstrumentationMetadata().isInstrumented() ) {
			FieldInterceptor interceptor = persister.getInstrumentationMetadata().extractInterceptor( target );
			if ( interceptor != null ) {
				interceptor.dirty();
			}
		}
	}

	private boolean isVersionChanged(Object entity, EventSource source, EntityPersister persister, Object target) {
		if ( !persister.isVersioned() ) {
			return false;
		}
		// for merging of versioned entities, we consider the version having
		// been changed only when:
		// 1) the two version values are different;
		//      *AND*
		// 2) The target actually represents database state!
		//
		// This second condition is a special case which allows
		// an entity to be merged during the same transaction
		// (though during a seperate operation) in which it was
		// originally persisted/saved
		boolean changed = !persister.getVersionType().isSame(
				persister.getVersion( target ),
				persister.getVersion( entity )
		);

		// TODO : perhaps we should additionally require that the incoming entity
		// version be equivalent to the defined unsaved-value?
		return changed && existsInDatabase( target, source, persister );
	}

	private boolean existsInDatabase(Object entity, EventSource source, EntityPersister persister) {
		EntityEntry entry = source.getPersistenceContext().getEntry( entity );
		if ( entry == null ) {
			Serializable id = persister.getIdentifier( entity, source );
			if ( id != null ) {
				final EntityKey key = source.generateEntityKey( id, persister );
				final Object managedEntity = source.getPersistenceContext().getEntity( key );
				entry = source.getPersistenceContext().getEntry( managedEntity );
			}
		}

		return entry != null && entry.isExistsInDatabase();
	}

	protected void copyValues(
			final EntityPersister persister,
			final Object entity,
			final Object target,
			final SessionImplementor source,
			final Map copyCache) {
		final Object[] copiedValues = TypeHelper.replace(
				persister.getPropertyValues( entity ),
				persister.getPropertyValues( target ),
				persister.getPropertyTypes(),
				source,
				target,
				copyCache
		);

		persister.setPropertyValues( target, copiedValues );
	}

	protected void copyValues(
			final EntityPersister persister,
			final Object entity,
			final Object target,
			final SessionImplementor source,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {

		final Object[] copiedValues;

		if ( foreignKeyDirection == ForeignKeyDirection.TO_PARENT ) {
			// this is the second pass through on a merge op, so here we limit the
			// replacement to associations types (value types were already replaced
			// during the first pass)
			copiedValues = TypeHelper.replaceAssociations(
					persister.getPropertyValues( entity ),
					persister.getPropertyValues( target ),
					persister.getPropertyTypes(),
					source,
					target,
					copyCache,
					foreignKeyDirection
			);
		}
		else {
			copiedValues = TypeHelper.replace(
					persister.getPropertyValues( entity ),
					persister.getPropertyValues( target ),
					persister.getPropertyTypes(),
					source,
					target,
					copyCache,
					foreignKeyDirection
			);
		}

		persister.setPropertyValues( target, copiedValues );
	}

	/**
	 * Perform any cascades needed as part of this copy event.
	 *
	 * @param source The merge event being processed.
	 * @param persister The persister of the entity being copied.
	 * @param entity The entity being copied.
	 * @param copyCache A cache of already copied instance.
	 */
	protected void cascadeOnMerge(
			final EventSource source,
			final EntityPersister persister,
			final Object entity,
			final Map copyCache
	) {
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadeAction(), CascadePoint.BEFORE_MERGE, source ).cascade(
					persister,
					entity,
					copyCache
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}


	@Override
	protected CascadingAction getCascadeAction() {
		return CascadingActions.MERGE;
	}

	@Override
	protected Boolean getAssumedUnsaved() {
		return Boolean.FALSE;
	}

	/**
	 * Cascade behavior is redefined by this subclass, disable superclass behavior
	 */
	@Override
	protected void cascadeAfterSave(EventSource source, EntityPersister persister, Object entity, Object anything)
			throws HibernateException {
	}

	/**
	 * Cascade behavior is redefined by this subclass, disable superclass behavior
	 */
	@Override
	protected void cascadeBeforeSave(EventSource source, EntityPersister persister, Object entity, Object anything)
			throws HibernateException {
	}
}
