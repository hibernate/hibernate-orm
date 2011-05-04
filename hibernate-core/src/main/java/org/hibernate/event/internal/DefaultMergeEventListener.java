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
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * Defines the default copy event listener used by hibernate for copying entities
 * in response to generated copy events.
 *
 * @author Gavin King
 */
public class DefaultMergeEventListener extends AbstractSaveEventListener
	implements MergeEventListener {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       DefaultMergeEventListener.class.getName());

	@Override
    protected Map getMergeMap(Object anything) {
		return ( ( EventCache ) anything ).invertMap();
	}

	/**
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 * @throws HibernateException
	 */
	public void onMerge(MergeEvent event) throws HibernateException {
		EventCache copyCache = new EventCache();
		onMerge( event, copyCache );
		// TODO: iteratively get transient entities and retry merge until one of the following conditions:
		//       1) transientCopyCache.size() == 0
		//       2) transientCopyCache.size() is not decreasing and copyCache.size() is not increasing
		// TODO: find out if retrying can add entities to copyCache (don't think it can...)
		// For now, just retry once; throw TransientObjectException if there are still any transient entities
		Map transientCopyCache = getTransientCopyCache(event, copyCache );
		if ( transientCopyCache.size() > 0 ) {
			retryMergeTransientEntities( event, transientCopyCache, copyCache, true );
			// find any entities that are still transient after retry
			transientCopyCache = getTransientCopyCache(event, copyCache );
			if ( transientCopyCache.size() > 0 ) {
				Set transientEntityNames = new HashSet();
				for( Iterator it=transientCopyCache.entrySet().iterator(); it.hasNext(); ) {
					Object transientEntity = ( ( Map.Entry ) it.next() ).getKey();
					String transientEntityName = event.getSession().guessEntityName( transientEntity );
					transientEntityNames.add( transientEntityName );
                    LOG.trace("Transient instance could not be processed by merge when checking nullability: "
                              + transientEntityName + "[" + transientEntity + "]");
				}
                if (isNullabilityCheckedGlobal(event.getSession())) throw new TransientObjectException(
						"one or more objects is an unsaved transient instance - save transient instance(s) before merging: " +
						transientEntityNames );
                LOG.trace("Retry saving transient instances without checking nullability");
                // failures will be detected later...
                retryMergeTransientEntities(event, transientCopyCache, copyCache, false);
			}
		}
		copyCache.clear();
		copyCache = null;
	}

	protected EventCache getTransientCopyCache(MergeEvent event, EventCache copyCache) {
		EventCache transientCopyCache = new EventCache();
		for ( Iterator it=copyCache.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry mapEntry = ( Map.Entry ) it.next();
			Object entity = mapEntry.getKey();
			Object copy = mapEntry.getValue();
			if ( copy instanceof HibernateProxy ) {
				copy = ( (HibernateProxy) copy ).getHibernateLazyInitializer().getImplementation();
			}
			EntityEntry copyEntry = event.getSession().getPersistenceContext().getEntry( copy );
			if ( copyEntry == null ) {
				// entity name will not be available for non-POJO entities
				// TODO: cache the entity name somewhere so that it is available to this exception
                LOG.trace("Transient instance could not be processed by merge: " + event.getSession().guessEntityName(copy) + "["
                          + entity + "]");
				// merge did not cascade to this entity; it's in copyCache because a
				// different entity has a non-nullable reference to this entity;
				// this entity should not be put in transientCopyCache, because it was
				// not included in the merge;
				// if the global setting for checking nullability is false, the non-nullable
				// reference to this entity will be detected later
				if ( isNullabilityCheckedGlobal( event.getSession() ) ) {
					throw new TransientObjectException(
						"object is an unsaved transient instance - save the transient instance before merging: " +
							event.getSession().guessEntityName( copy )
					);
				}
			}
			else if ( copyEntry.getStatus() == Status.SAVING ) {
				transientCopyCache.put( entity, copy, copyCache.isOperatedOn( entity ) );
			}
			else if ( copyEntry.getStatus() != Status.MANAGED && copyEntry.getStatus() != Status.READ_ONLY ) {
				throw new AssertionFailure( "Merged entity does not have status set to MANAGED or READ_ONLY; "+copy+" status="+copyEntry.getStatus() );
			}
		}
		return transientCopyCache;
	}

	protected void retryMergeTransientEntities(
			MergeEvent event,
			Map transientCopyCache,
			EventCache copyCache,
			boolean isNullabilityChecked) {
		// TODO: The order in which entities are saved may matter (e.g., a particular transient entity
		//       may need to be saved before other transient entities can be saved;
		//       Keep retrying the batch of transient entities until either:
		//       1) there are no transient entities left in transientCopyCache
		//       or 2) no transient entities were saved in the last batch
		// For now, just run through the transient entities and retry the merge
		for ( Iterator it=transientCopyCache.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry mapEntry = ( Map.Entry ) it.next();
			Object entity = mapEntry.getKey();
			Object copy = transientCopyCache.get( entity );
			EntityEntry copyEntry = event.getSession().getPersistenceContext().getEntry( copy );
			mergeTransientEntity(
					entity,
					copyEntry.getEntityName(),
					( entity == event.getEntity() ? event.getRequestedId() : copyEntry.getId() ),
					event.getSession(),
					copyCache,
					isNullabilityChecked
			);
		}
	}

	/**
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 * @throws HibernateException
	 */
	public void onMerge(MergeEvent event, Map copiedAlready) throws HibernateException {

		final EventCache copyCache = ( EventCache ) copiedAlready;
		final EventSource source = event.getSession();
		final Object original = event.getOriginal();

		if ( original != null ) {

			final Object entity;
			if ( original instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) original ).getHibernateLazyInitializer();
				if ( li.isUninitialized() ) {
                    LOG.trace("Ignoring uninitialized proxy");
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
                LOG.trace("Already in merge process");
				event.setResult( entity );
			}
			else {
				if ( copyCache.containsKey( entity ) ) {
                    LOG.trace("Already in copyCache; setting in merge process");
					copyCache.setOperatedOn( entity, true );
				}
				event.setEntity( entity );
				int entityState = -1;

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
							entityState = DETACHED;
						}
					}
				}

				if ( entityState == -1 ) {
					entityState = getEntityState( entity, event.getEntityName(), entry, source );
				}

				switch (entityState) {
					case DETACHED:
						entityIsDetached(event, copyCache);
						break;
					case TRANSIENT:
						entityIsTransient(event, copyCache);
						break;
					case PERSISTENT:
						entityIsPersistent(event, copyCache);
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
        LOG.trace("Ignoring persistent instance");

		//TODO: check that entry.getIdentifier().equals(requestedId)

		final Object entity = event.getEntity();
		final EventSource source = event.getSession();
		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );

		( ( EventCache ) copyCache ).put( entity, entity, true  );  //before cascade!

		cascadeOnMerge(source, persister, entity, copyCache);
		copyValues(persister, entity, entity, source, copyCache);

		event.setResult(entity);
	}

	protected void entityIsTransient(MergeEvent event, Map copyCache) {

        LOG.trace("Merging transient instance");

		final Object entity = event.getEntity();
		final EventSource source = event.getSession();

		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		final String entityName = persister.getEntityName();

		event.setResult( mergeTransientEntity( entity, entityName, event.getRequestedId(), source, copyCache, true ) );
	}

	protected Object mergeTransientEntity(Object entity, String entityName, Serializable requestedId, EventSource source, Map copyCache) {
		return mergeTransientEntity( entity, entityName, requestedId, source, copyCache, true );
	}

	private Object mergeTransientEntity(
			Object entity,
			String entityName,
			Serializable requestedId,
			EventSource source,
			Map copyCache,
			boolean isNullabilityChecked) {

        LOG.trace("Merging transient instance");

		final EntityPersister persister = source.getEntityPersister( entityName, entity );

		final Serializable id = persister.hasIdentifierProperty() ?
				persister.getIdentifier( entity, source ) :
		        null;
		if ( copyCache.containsKey( entity ) ) {
			persister.setIdentifier( copyCache.get( entity ), id, source );
		}
		else {
			( ( EventCache ) copyCache ).put( entity, source.instantiate( persister, id ), true ); //before cascade!
		}
		final Object copy = copyCache.get( entity );

		// cascade first, so that all unsaved objects get their
		// copy created before we actually copy
		//cascadeOnMerge(event, persister, entity, copyCache, Cascades.CASCADE_BEFORE_MERGE);
		super.cascadeBeforeSave(source, persister, entity, copyCache);
		copyValues(persister, entity, copy, source, copyCache, ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT);

		try {
			// try saving; check for non-nullable properties that are null or transient entities before saving
			saveTransientEntity( copy, entityName, requestedId, source, copyCache, isNullabilityChecked );
		}
		catch (PropertyValueException ex) {
			String propertyName = ex.getPropertyName();
			Object propertyFromCopy = persister.getPropertyValue( copy, propertyName, source.getEntityMode() );
			Object propertyFromEntity = persister.getPropertyValue( entity, propertyName, source.getEntityMode() );
			Type propertyType = persister.getPropertyType( propertyName );
			EntityEntry copyEntry = source.getPersistenceContext().getEntry( copy );
			if ( propertyFromCopy == null ||
					propertyFromEntity == null ||
					! propertyType.isEntityType() ||
					! copyCache.containsKey( propertyFromEntity ) ) {
				if ( LOG.isTraceEnabled() ) {
                    LOG.trace("Property '" + copyEntry.getEntityName() + "." + propertyName + "' in copy is "
                              + (propertyFromCopy == null ? "null" : propertyFromCopy));
                    LOG.trace("Property '" + copyEntry.getEntityName() + "." + propertyName + "' in original is "
                              + (propertyFromCopy == null ? "null" : propertyFromCopy));
                    LOG.trace("Property '" + copyEntry.getEntityName() + "." + propertyName + "' is"
                              + (propertyType.isEntityType() ? "" : " not") + " an entity type");
                    if (propertyFromEntity != null && !copyCache.containsKey(propertyFromEntity)) LOG.trace("Property '"
                                                                                                            + copyEntry.getEntityName()
                                                                                                            + "."
                                                                                                            + propertyName
                                                                                                            + "' is not in copy cache");
	            }
                if ( isNullabilityCheckedGlobal( source ) ) {
                    throw ex;
                }
                else {
                    // retry save w/o checking for non-nullable properties
                    // (the failure will be detected later)
                    saveTransientEntity( copy, entityName, requestedId, source, copyCache, false );
				}
			}
			if ( LOG.isTraceEnabled() && propertyFromEntity != null ) {
                if (((EventCache)copyCache).isOperatedOn(propertyFromEntity)) LOG.trace("Property '"
                                                                                        + copyEntry.getEntityName()
                                                                                        + "."
                                                                                        + propertyName
                                                                                        + "' from original entity is in copyCache and is in the process of being merged; "
                                                                                        + propertyName + " =[" + propertyFromEntity
                                                                                        + "]");
                else LOG.trace("Property '" + copyEntry.getEntityName() + "." + propertyName
                               + "' from original entity is in copyCache and is not in the process of being merged; "
                               + propertyName + " =[" + propertyFromEntity + "]");
			}
			// continue...; we'll find out if it ends up not getting saved later
		}

		// cascade first, so that all unsaved objects get their
		// copy created before we actually copy
		super.cascadeAfterSave(source, persister, entity, copyCache);
		copyValues(persister, entity, copy, source, copyCache, ForeignKeyDirection.FOREIGN_KEY_TO_PARENT);

		return copy;

	}

	private boolean isNullabilityCheckedGlobal(EventSource source) {
		return source.getFactory().getSettings().isCheckNullability();
	}

	private void saveTransientEntity(
			Object entity,
			String entityName,
			Serializable requestedId,
			EventSource source,
			Map copyCache,
			boolean isNullabilityChecked) {

		boolean isNullabilityCheckedOrig =
			source.getFactory().getSettings().isCheckNullability();
		try {
			source.getFactory().getSettings().setCheckNullability( isNullabilityChecked );
			//this bit is only *really* absolutely necessary for handling
			//requestedId, but is also good if we merge multiple object
			//graphs, since it helps ensure uniqueness
			if (requestedId==null) {
				saveWithGeneratedId( entity, entityName, copyCache, source, false );
			}
			else {
				saveWithRequestedId( entity, requestedId, entityName, copyCache, source );
			}
		}
		finally {
			source.getFactory().getSettings().setCheckNullability( isNullabilityCheckedOrig );
		}
	}
	protected void entityIsDetached(MergeEvent event, Map copyCache) {

        LOG.trace("Merging detached instance");

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
			if ( !persister.getIdentifierType().isEqual( id, entityId, source.getEntityMode(), source.getFactory() ) ) {
				throw new HibernateException( "merge requested with id not matching id of passed entity" );
			}
		}

		String previousFetchProfile = source.getFetchProfile();
		source.setFetchProfile("merge");
		//we must clone embedded composite identifiers, or
		//we will get back the same instance that we pass in
		final Serializable clonedIdentifier = (Serializable) persister.getIdentifierType()
				.deepCopy( id, source.getEntityMode(), source.getFactory() );
		final Object result = source.get(entityName, clonedIdentifier);
		source.setFetchProfile(previousFetchProfile);

		if ( result == null ) {
			//TODO: we should throw an exception if we really *know* for sure
			//      that this is a detached instance, rather than just assuming
			//throw new StaleObjectStateException(entityName, id);

			// we got here because we assumed that an instance
			// with an assigned id was detached, when it was
			// really persistent
			entityIsTransient(event, copyCache);
		}
		else {
			( ( EventCache ) copyCache ).put( entity, result, true ); //before cascade!

			final Object target = source.getPersistenceContext().unproxy(result);
			if ( target == entity ) {
				throw new AssertionFailure("entity was not detached");
			}
			else if ( !source.getEntityName(target).equals(entityName) ) {
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
			cascadeOnMerge(source, persister, entity, copyCache);
			copyValues(persister, entity, target, source, copyCache);

			//copyValues works by reflection, so explicitly mark the entity instance dirty
			markInterceptorDirty( entity, target );

			event.setResult(result);
		}

	}

	private void markInterceptorDirty(final Object entity, final Object target) {
		if ( FieldInterceptionHelper.isInstrumented( entity ) ) {
			FieldInterceptor interceptor = FieldInterceptionHelper.extractFieldInterceptor( target );
			if ( interceptor != null ) {
				interceptor.dirty();
			}
		}
	}

	private boolean isVersionChanged(Object entity, EventSource source, EntityPersister persister, Object target) {
		if ( ! persister.isVersioned() ) {
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
		boolean changed = ! persister.getVersionType().isSame(
				persister.getVersion( target, source.getEntityMode() ),
				persister.getVersion( entity, source.getEntityMode() ),
				source.getEntityMode()
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
				persister.getPropertyValues( entity, source.getEntityMode() ),
				persister.getPropertyValues( target, source.getEntityMode() ),
				persister.getPropertyTypes(),
				source,
				target,
				copyCache
		);

		persister.setPropertyValues( target, copiedValues, source.getEntityMode() );
	}

	protected void copyValues(
			final EntityPersister persister,
			final Object entity,
			final Object target,
			final SessionImplementor source,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {

		final Object[] copiedValues;

		if ( foreignKeyDirection == ForeignKeyDirection.FOREIGN_KEY_TO_PARENT ) {
			// this is the second pass through on a merge op, so here we limit the
			// replacement to associations types (value types were already replaced
			// during the first pass)
			copiedValues = TypeHelper.replaceAssociations(
					persister.getPropertyValues( entity, source.getEntityMode() ),
					persister.getPropertyValues( target, source.getEntityMode() ),
					persister.getPropertyTypes(),
					source,
					target,
					copyCache,
					foreignKeyDirection
			);
		}
		else {
			copiedValues = TypeHelper.replace(
					persister.getPropertyValues( entity, source.getEntityMode() ),
					persister.getPropertyValues( target, source.getEntityMode() ),
					persister.getPropertyTypes(),
					source,
					target,
					copyCache,
					foreignKeyDirection
			);
		}

		persister.setPropertyValues( target, copiedValues, source.getEntityMode() );
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
			new Cascade( getCascadeAction(), Cascade.BEFORE_MERGE, source )
					.cascade(persister, entity, copyCache);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}


	@Override
    protected CascadingAction getCascadeAction() {
		return CascadingAction.MERGE;
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
