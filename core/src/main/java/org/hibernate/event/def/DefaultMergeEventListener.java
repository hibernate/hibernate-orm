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
package org.hibernate.event.def;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.event.EventSource;
import org.hibernate.event.MergeEvent;
import org.hibernate.event.MergeEventListener;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.IdentityMap;

/**
 * Defines the default copy event listener used by hibernate for copying entities
 * in response to generated copy events.
 *
 * @author Gavin King
 */
public class DefaultMergeEventListener extends AbstractSaveEventListener 
	implements MergeEventListener {

	private static final Logger log = LoggerFactory.getLogger(DefaultMergeEventListener.class);
	
	protected Map getMergeMap(Object anything) {
		return IdentityMap.invert( (Map) anything );
	}

	/**
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 * @throws HibernateException
	 */
	public void onMerge(MergeEvent event) throws HibernateException {
		Map copyCache = IdentityMap.instantiate(10);
		onMerge( event, copyCache );
		for ( Iterator it=copyCache.values().iterator(); it.hasNext(); ) {
			Object entity = it.next();
			if ( entity instanceof HibernateProxy ) {
				entity = ( (HibernateProxy) entity ).getHibernateLazyInitializer().getImplementation();
			}
			EntityEntry entry = event.getSession().getPersistenceContext().getEntry( entity );
			if ( entry == null ) {
				throw new TransientObjectException(
						"object references an unsaved transient instance - save the transient instance before merging: " +
						event.getSession().guessEntityName( entity )
				);
				// TODO: cache the entity name somewhere so that it is available to this exception
				// entity name will not be available for non-POJO entities
			}
			if ( entry.getStatus() != Status.MANAGED ) {
				throw new AssertionFailure( "Merged entity does not have status set to MANAGED; "+entry+" status="+entry.getStatus() );
			}
		}
	}

	/** 
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 * @throws HibernateException
	 */
	public void onMerge(MergeEvent event, Map copyCache) throws HibernateException {

		final EventSource source = event.getSession();
		final Object original = event.getOriginal();

		if ( original != null ) {

			final Object entity;
			if ( original instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) original ).getHibernateLazyInitializer();
				if ( li.isUninitialized() ) {
					log.trace("ignoring uninitialized proxy");
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
			
			if ( copyCache.containsKey(entity) &&
					source.getContextEntityIdentifier( copyCache.get( entity ) ) != null ) {
				log.trace("already merged");
				event.setResult(entity);
			}
			else {
				event.setEntity( entity );
				int entityState = -1;

				// Check the persistence context for an entry relating to this
				// entity to be merged...
				EntityEntry entry = source.getPersistenceContext().getEntry( entity );
				if ( entry == null ) {
					EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
					Serializable id = persister.getIdentifier( entity, source.getEntityMode() );
					if ( id != null ) {
						EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
						Object managedEntity = source.getPersistenceContext().getEntity( key );
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
		log.trace("ignoring persistent instance");
		
		//TODO: check that entry.getIdentifier().equals(requestedId)
		
		final Object entity = event.getEntity();
		final EventSource source = event.getSession();
		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		
		copyCache.put(entity, entity); //before cascade!
		
		cascadeOnMerge(source, persister, entity, copyCache);
		copyValues(persister, entity, entity, source, copyCache);
		
		event.setResult(entity);
	}
	
	protected void entityIsTransient(MergeEvent event, Map copyCache) {
		
		log.trace("merging transient instance");
		
		final Object entity = event.getEntity();
		final EventSource source = event.getSession();

		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		final String entityName = persister.getEntityName();
		
		final Serializable id = persister.hasIdentifierProperty() ?
				persister.getIdentifier( entity, source.getEntityMode() ) :
		        null;
		if ( copyCache.containsKey( entity ) ) {
			persister.setIdentifier( copyCache.get( entity ), id, source.getEntityMode() );
		}
		else {
			copyCache.put(entity, persister.instantiate( id, source.getEntityMode() ) ); //before cascade!
			//TODO: should this be Session.instantiate(Persister, ...)?
		}
		final Object copy = copyCache.get( entity );

		// cascade first, so that all unsaved objects get their
		// copy created before we actually copy
		//cascadeOnMerge(event, persister, entity, copyCache, Cascades.CASCADE_BEFORE_MERGE);
		super.cascadeBeforeSave(source, persister, entity, copyCache);
		copyValues(persister, entity, copy, source, copyCache, ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT);
		
		//this bit is only *really* absolutely necessary for handling 
		//requestedId, but is also good if we merge multiple object 
		//graphs, since it helps ensure uniqueness
		final Serializable requestedId = event.getRequestedId();
		if (requestedId==null) {
			saveWithGeneratedId( copy, entityName, copyCache, source, false );
		}
		else {
			saveWithRequestedId( copy, requestedId, entityName, copyCache, source );
		}
		
		// cascade first, so that all unsaved objects get their 
		// copy created before we actually copy
		super.cascadeAfterSave(source, persister, entity, copyCache);
		copyValues(persister, entity, copy, source, copyCache, ForeignKeyDirection.FOREIGN_KEY_TO_PARENT);
		
		event.setResult(copy);

	}

	protected void entityIsDetached(MergeEvent event, Map copyCache) {
		
		log.trace("merging detached instance");
		
		final Object entity = event.getEntity();
		final EventSource source = event.getSession();

		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		final String entityName = persister.getEntityName();
			
		Serializable id = event.getRequestedId();
		if ( id == null ) {
			id = persister.getIdentifier( entity, source.getEntityMode() );
		}
		else {
			// check that entity id = requestedId
			Serializable entityId = persister.getIdentifier( entity, source.getEntityMode() );
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
			copyCache.put(entity, result); //before cascade!
	
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
			Serializable id = persister.getIdentifier( entity, source.getEntityMode() );
			if ( id != null ) {
				EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
				Object managedEntity = source.getPersistenceContext().getEntity( key );
				entry = source.getPersistenceContext().getEntry( managedEntity );
			}
		}

		if ( entry == null ) {
			// perhaps this should be an exception since it is only ever used
			// in the above method?
			return false;
		}
		else {
			return entry.isExistsInDatabase();
		}
	}
	
	protected void copyValues(
		final EntityPersister persister, 
		final Object entity, 
		final Object target, 
		final SessionImplementor source,
		final Map copyCache
	) {
		
		final Object[] copiedValues = TypeFactory.replace(
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
			copiedValues = TypeFactory.replaceAssociations(
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
			copiedValues = TypeFactory.replace(
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


	protected CascadingAction getCascadeAction() {
		return CascadingAction.MERGE;
	}

	protected Boolean getAssumedUnsaved() {
		return Boolean.FALSE;
	}
	
	/**
	 * Cascade behavior is redefined by this subclass, disable superclass behavior
	 */
	protected void cascadeAfterSave(EventSource source, EntityPersister persister, Object entity, Object anything) 
	throws HibernateException {
	}

	/**
	 * Cascade behavior is redefined by this subclass, disable superclass behavior
	 */
	protected void cascadeBeforeSave(EventSource source, EntityPersister persister, Object entity, Object anything) 
	throws HibernateException {
	}

}
