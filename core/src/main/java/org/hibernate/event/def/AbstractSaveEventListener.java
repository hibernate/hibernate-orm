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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.action.EntityIdentityInsertAction;
import org.hibernate.action.EntityInsertAction;
import org.hibernate.classic.Lifecycle;
import org.hibernate.classic.Validatable;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Nullability;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.event.EventSource;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A convenience bas class for listeners responding to save events.
 *
 * @author Steve Ebersole.
 */
public abstract class AbstractSaveEventListener extends AbstractReassociateEventListener {

	protected static final int PERSISTENT = 0;
	protected static final int TRANSIENT = 1;
	protected static final int DETACHED = 2;
	protected static final int DELETED = 3;

	private static final Logger log = LoggerFactory.getLogger( AbstractSaveEventListener.class );

	/**
	 * Prepares the save call using the given requested id.
	 *
	 * @param entity The entity to be saved.
	 * @param requestedId The id to which to associate the entity.
	 * @param entityName The name of the entity being saved.
	 * @param anything Generally cascade-specific information.
	 * @param source The session which is the source of this save event.
	 *
	 * @return The id used to save the entity.
	 */
	protected Serializable saveWithRequestedId(
			Object entity,
			Serializable requestedId,
			String entityName,
			Object anything,
			EventSource source) {
		return performSave(
				entity,
				requestedId,
				source.getEntityPersister( entityName, entity ),
				false,
				anything,
				source,
				true
		);
	}

	/**
	 * Prepares the save call using a newly generated id.
	 *
	 * @param entity The entity to be saved
	 * @param entityName The entity-name for the entity to be saved
	 * @param anything Generally cascade-specific information.
	 * @param source The session which is the source of this save event.
	 * @param requiresImmediateIdAccess does the event context require
	 * access to the identifier immediately after execution of this method (if
	 * not, post-insert style id generators may be postponed if we are outside
	 * a transaction).
	 *
	 * @return The id used to save the entity; may be null depending on the
	 *         type of id generator used and the requiresImmediateIdAccess value
	 */
	protected Serializable saveWithGeneratedId(
			Object entity,
			String entityName,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {
		EntityPersister persister = source.getEntityPersister( entityName, entity );
		Serializable generatedId = persister.getIdentifierGenerator().generate( source, entity );
		if ( generatedId == null ) {
			throw new IdentifierGenerationException( "null id generated for:" + entity.getClass() );
		}
		else if ( generatedId == IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR ) {
			return source.getIdentifier( entity );
		}
		else if ( generatedId == IdentifierGeneratorHelper.POST_INSERT_INDICATOR ) {
			return performSave( entity, null, persister, true, anything, source, requiresImmediateIdAccess );
		}
		else {

			if ( log.isDebugEnabled() ) {
				log.debug(
						"generated identifier: " +
								persister.getIdentifierType().toLoggableString( generatedId, source.getFactory() ) +
								", using strategy: " +
								persister.getIdentifierGenerator().getClass().getName()
						//TODO: define toString()s for generators
				);
			}

			return performSave( entity, generatedId, persister, false, anything, source, true );
		}
	}

	/**
	 * Ppepares the save call by checking the session caches for a pre-existing
	 * entity and performing any lifecycle callbacks.
	 *
	 * @param entity The entity to be saved.
	 * @param id The id by which to save the entity.
	 * @param persister The entity's persister instance.
	 * @param useIdentityColumn Is an identity column being used?
	 * @param anything Generally cascade-specific information.
	 * @param source The session from which the event originated.
	 * @param requiresImmediateIdAccess does the event context require
	 * access to the identifier immediately after execution of this method (if
	 * not, post-insert style id generators may be postponed if we are outside
	 * a transaction).
	 *
	 * @return The id used to save the entity; may be null depending on the
	 *         type of id generator used and the requiresImmediateIdAccess value
	 */
	protected Serializable performSave(
			Object entity,
			Serializable id,
			EntityPersister persister,
			boolean useIdentityColumn,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {

		if ( log.isTraceEnabled() ) {
			log.trace(
					"saving " +
							MessageHelper.infoString( persister, id, source.getFactory() )
			);
		}

		EntityKey key;
		if ( !useIdentityColumn ) {
			key = new EntityKey( id, persister, source.getEntityMode() );
			Object old = source.getPersistenceContext().getEntity( key );
			if ( old != null ) {
				if ( source.getPersistenceContext().getEntry( old ).getStatus() == Status.DELETED ) {
					source.forceFlush( source.getPersistenceContext().getEntry( old ) );
				}
				else {
					throw new NonUniqueObjectException( id, persister.getEntityName() );
				}
			}
			persister.setIdentifier( entity, id, source );
		}
		else {
			key = null;
		}

		if ( invokeSaveLifecycle( entity, persister, source ) ) {
			return id; //EARLY EXIT
		}

		return performSaveOrReplicate(
				entity,
				key,
				persister,
				useIdentityColumn,
				anything,
				source,
				requiresImmediateIdAccess
		);
	}

	protected boolean invokeSaveLifecycle(Object entity, EntityPersister persister, EventSource source) {
		// Sub-insertions should occur before containing insertion so
		// Try to do the callback now
		if ( persister.implementsLifecycle( source.getEntityMode() ) ) {
			log.debug( "calling onSave()" );
			if ( ( ( Lifecycle ) entity ).onSave( source ) ) {
				log.debug( "insertion vetoed by onSave()" );
				return true;
			}
		}
		return false;
	}

	protected void validate(Object entity, EntityPersister persister, EventSource source) {
		if ( persister.implementsValidatable( source.getEntityMode() ) ) {
			( ( Validatable ) entity ).validate();
		}
	}

	/**
	 * Performs all the actual work needed to save an entity (well to get the save moved to
	 * the execution queue).
	 *
	 * @param entity The entity to be saved
	 * @param key The id to be used for saving the entity (or null, in the case of identity columns)
	 * @param persister The entity's persister instance.
	 * @param useIdentityColumn Should an identity column be used for id generation?
	 * @param anything Generally cascade-specific information.
	 * @param source The session which is the source of the current event.
	 * @param requiresImmediateIdAccess Is access to the identifier required immediately
	 * after the completion of the save?  persist(), for example, does not require this...
	 *
	 * @return The id used to save the entity; may be null depending on the
	 *         type of id generator used and the requiresImmediateIdAccess value
	 */
	protected Serializable performSaveOrReplicate(
			Object entity,
			EntityKey key,
			EntityPersister persister,
			boolean useIdentityColumn,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {

		validate( entity, persister, source );

		Serializable id = key == null ? null : key.getIdentifier();

		boolean inTxn = source.getJDBCContext().isTransactionInProgress();
		boolean shouldDelayIdentityInserts = !inTxn && !requiresImmediateIdAccess;

		if ( useIdentityColumn && !shouldDelayIdentityInserts ) {
			log.trace( "executing insertions" );
			source.getActionQueue().executeInserts();
		}

		// Put a placeholder in entries, so we don't recurse back and try to save() the
		// same object again. QUESTION: should this be done before onSave() is called?
		// likewise, should it be done before onUpdate()?
		source.getPersistenceContext().addEntry(
				entity,
				Status.SAVING,
				null,
				null,
				id,
				null,
				LockMode.WRITE,
				useIdentityColumn,
				persister,
				false,
				false
		);

		cascadeBeforeSave( source, persister, entity, anything );

		Object[] values = persister.getPropertyValuesToInsert( entity, getMergeMap( anything ), source );
		Type[] types = persister.getPropertyTypes();

		boolean substitute = substituteValuesIfNecessary( entity, id, values, persister, source );

		if ( persister.hasCollections() ) {
			substitute = substitute || visitCollectionsBeforeSave( entity, id, values, types, source );
		}

		if ( substitute ) {
			persister.setPropertyValues( entity, values, source.getEntityMode() );
		}

		TypeFactory.deepCopy(
				values,
				types,
				persister.getPropertyUpdateability(),
				values,
				source
		);

		new ForeignKeys.Nullifier( entity, false, useIdentityColumn, source )
				.nullifyTransientReferences( values, types );
		new Nullability( source ).checkNullability( values, persister, false );

		if ( useIdentityColumn ) {
			EntityIdentityInsertAction insert = new EntityIdentityInsertAction(
					values, entity, persister, source, shouldDelayIdentityInserts
			);
			if ( !shouldDelayIdentityInserts ) {
				log.debug( "executing identity-insert immediately" );
				source.getActionQueue().execute( insert );
				id = insert.getGeneratedId();
				key = new EntityKey( id, persister, source.getEntityMode() );
				source.getPersistenceContext().checkUniqueness( key, entity );
			}
			else {
				log.debug( "delaying identity-insert due to no transaction in progress" );
				source.getActionQueue().addAction( insert );
				key = insert.getDelayedEntityKey();
			}
		}

		Object version = Versioning.getVersion( values, persister );
		source.getPersistenceContext().addEntity(
				entity,
				( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				values,
				key,
				version,
				LockMode.WRITE,
				useIdentityColumn,
				persister,
				isVersionIncrementDisabled(),
				false
		);
		//source.getPersistenceContext().removeNonExist( new EntityKey( id, persister, source.getEntityMode() ) );

		if ( !useIdentityColumn ) {
			source.getActionQueue().addAction(
					new EntityInsertAction( id, values, entity, version, persister, source )
			);
		}

		cascadeAfterSave( source, persister, entity, anything );

		markInterceptorDirty( entity, persister, source );

		return id;
	}

	private void markInterceptorDirty(Object entity, EntityPersister persister, EventSource source) {
		if ( FieldInterceptionHelper.isInstrumented( entity ) ) {
			FieldInterceptor interceptor = FieldInterceptionHelper.injectFieldInterceptor(
					entity,
					persister.getEntityName(),
					null,
					source
			);
			interceptor.dirty();
		}
	}

	protected Map getMergeMap(Object anything) {
		return null;
	}

	/**
	 * After the save, will te version number be incremented
	 * if the instance is modified?
	 *
	 * @return True if the version will be incremented on an entity change after save;
	 *         false otherwise.
	 */
	protected boolean isVersionIncrementDisabled() {
		return false;
	}

	protected boolean visitCollectionsBeforeSave(Object entity, Serializable id, Object[] values, Type[] types, EventSource source) {
		WrapVisitor visitor = new WrapVisitor( source );
		// substitutes into values by side-effect
		visitor.processEntityPropertyValues( values, types );
		return visitor.isSubstitutionRequired();
	}

	/**
	 * Perform any property value substitution that is necessary
	 * (interceptor callback, version initialization...)
	 *
	 * @param entity The entity
	 * @param id The entity identifier
	 * @param values The snapshot entity state
	 * @param persister The entity persister
	 * @param source The originating session
	 *
	 * @return True if the snapshot state changed such that
	 * reinjection of the values into the entity is required.
	 */
	protected boolean substituteValuesIfNecessary(
			Object entity,
			Serializable id,
			Object[] values,
			EntityPersister persister,
			SessionImplementor source) {
		boolean substitute = source.getInterceptor().onSave(
				entity,
				id,
				values,
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);

		//keep the existing version number in the case of replicate!
		if ( persister.isVersioned() ) {
			substitute = Versioning.seedVersion(
					values,
					persister.getVersionProperty(),
					persister.getVersionType(),
					source
			) || substitute;
		}
		return substitute;
	}

	/**
	 * Handles the calls needed to perform pre-save cascades for the given entity.
	 *
	 * @param source The session from whcih the save event originated.
	 * @param persister The entity's persister instance.
	 * @param entity The entity to be saved.
	 * @param anything Generally cascade-specific data
	 */
	protected void cascadeBeforeSave(
			EventSource source,
			EntityPersister persister,
			Object entity,
			Object anything) {

		// cascade-save to many-to-one BEFORE the parent is saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadeAction(), Cascade.BEFORE_INSERT_AFTER_DELETE, source )
					.cascade( persister, entity, anything );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	/**
	 * Handles to calls needed to perform post-save cascades.
	 *
	 * @param source The session from which the event originated.
	 * @param persister The entity's persister instance.
	 * @param entity The entity beng saved.
	 * @param anything Generally cascade-specific data
	 */
	protected void cascadeAfterSave(
			EventSource source,
			EntityPersister persister,
			Object entity,
			Object anything) {

		// cascade-save to collections AFTER the collection owner was saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadeAction(), Cascade.AFTER_INSERT_BEFORE_DELETE, source )
					.cascade( persister, entity, anything );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	protected abstract CascadingAction getCascadeAction();

	/**
	 * Determine whether the entity is persistent, detached, or transient
	 *
	 * @param entity The entity to check
	 * @param entityName The name of the entity
	 * @param entry The entity's entry in the persistence context
	 * @param source The originating session.
	 *
	 * @return The state.
	 */
	protected int getEntityState(
			Object entity,
			String entityName,
			EntityEntry entry, //pass this as an argument only to avoid double looking
			SessionImplementor source) {

		if ( entry != null ) { // the object is persistent

			//the entity is associated with the session, so check its status
			if ( entry.getStatus() != Status.DELETED ) {
				// do nothing for persistent instances
				if ( log.isTraceEnabled() ) {
					log.trace(
							"persistent instance of: " +
									getLoggableName( entityName, entity )
					);
				}
				return PERSISTENT;
			}
			else {
				//ie. e.status==DELETED
				if ( log.isTraceEnabled() ) {
					log.trace(
							"deleted instance of: " +
									getLoggableName( entityName, entity )
					);
				}
				return DELETED;
			}

		}
		else { // the object is transient or detached

			//the entity is not associated with the session, so
			//try interceptor and unsaved-value

			if ( ForeignKeys.isTransient( entityName, entity, getAssumedUnsaved(), source ) ) {
				if ( log.isTraceEnabled() ) {
					log.trace(
							"transient instance of: " +
									getLoggableName( entityName, entity )
					);
				}
				return TRANSIENT;
			}
			else {
				if ( log.isTraceEnabled() ) {
					log.trace(
							"detached instance of: " +
									getLoggableName( entityName, entity )
					);
				}
				return DETACHED;
			}

		}
	}

	protected String getLoggableName(String entityName, Object entity) {
		return entityName == null ? entity.getClass().getName() : entityName;
	}

	protected Boolean getAssumedUnsaved() {
		return null;
	}

}
