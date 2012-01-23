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
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * A convenience bas class for listeners responding to save events.
 *
 * @author Steve Ebersole.
 */
public abstract class AbstractSaveEventListener extends AbstractReassociateEventListener {
    public enum EntityState{
        PERSISTENT, TRANSIENT, DETACHED, DELETED
    }

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       AbstractSaveEventListener.class.getName());

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
			// TODO: define toString()s for generators
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Generated identifier: %s, using strategy: %s",
						persister.getIdentifierType().toLoggableString( generatedId, source.getFactory() ),
						persister.getIdentifierGenerator().getClass().getName() );
			}

			return performSave( entity, generatedId, persister, false, anything, source, true );
		}
	}

	/**
	 * Prepares the save call by checking the session caches for a pre-existing
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

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Saving {0}", MessageHelper.infoString( persister, id, source.getFactory() ) );
		}

		final EntityKey key;
		if ( !useIdentityColumn ) {
			key = source.generateEntityKey( id, persister );
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
		if ( persister.implementsLifecycle() ) {
			LOG.debug( "Calling onSave()" );
			if ( ( ( Lifecycle ) entity ).onSave( source ) ) {
				LOG.debug( "Insertion vetoed by onSave()" );
				return true;
			}
		}
		return false;
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

		Serializable id = key == null ? null : key.getIdentifier();

		boolean inTxn = source.getTransactionCoordinator().isTransactionInProgress();
		boolean shouldDelayIdentityInserts = !inTxn && !requiresImmediateIdAccess;

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
			persister.setPropertyValues( entity, values );
		}

		TypeHelper.deepCopy(
				values,
				types,
				persister.getPropertyUpdateability(),
				values,
				source
		);

		AbstractEntityInsertAction insert = addInsertAction(
				values, id, entity, persister, useIdentityColumn, source, shouldDelayIdentityInserts
		);

		// postpone initializing id in case the insert has non-nullable transient dependencies
		// that are not resolved until cascadeAfterSave() is executed
		cascadeAfterSave( source, persister, entity, anything );
		if ( useIdentityColumn && insert.isEarlyInsert() ) {
			if ( ! EntityIdentityInsertAction.class.isInstance( insert ) ) {
				throw new IllegalStateException(
						"Insert should be using an identity column, but action is of unexpected type: " +
								insert.getClass().getName() );
			}
			id = ( ( EntityIdentityInsertAction ) insert ).getGeneratedId();
		}

		markInterceptorDirty( entity, persister, source );

		return id;
	}

	private AbstractEntityInsertAction addInsertAction(
			Object[] values,
			Serializable id,
			Object entity,
			EntityPersister persister,
			boolean useIdentityColumn,
			EventSource source,
			boolean shouldDelayIdentityInserts) {
		if ( useIdentityColumn ) {
			EntityIdentityInsertAction insert = new EntityIdentityInsertAction(
					values, entity, persister, isVersionIncrementDisabled(), source, shouldDelayIdentityInserts
			);
			source.getActionQueue().addAction( insert );
			return insert;
		}
		else {
			Object version = Versioning.getVersion( values, persister );
			EntityInsertAction insert = new EntityInsertAction(
					id, values, entity, version, persister, isVersionIncrementDisabled(), source
			);
			source.getActionQueue().addAction( insert );
			return insert;
		}
	}

	private void markInterceptorDirty(Object entity, EntityPersister persister, EventSource source) {
		if ( persister.getInstrumentationMetadata().isInstrumented() ) {
			FieldInterceptor interceptor = persister.getInstrumentationMetadata().injectInterceptor(
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
	protected EntityState getEntityState(
			Object entity,
			String entityName,
			EntityEntry entry, //pass this as an argument only to avoid double looking
			SessionImplementor source) {

		if ( entry != null ) { // the object is persistent

			//the entity is associated with the session, so check its status
			if ( entry.getStatus() != Status.DELETED ) {
				// do nothing for persistent instances
				if ( LOG.isTraceEnabled() ) {
					LOG.tracev( "Persistent instance of: {0}", getLoggableName( entityName, entity ) );
				}
				return EntityState.PERSISTENT;
			}
			// ie. e.status==DELETED
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Deleted instance of: {0}", getLoggableName( entityName, entity ) );
			}
			return EntityState.DELETED;
		}
		// the object is transient or detached

		// the entity is not associated with the session, so
		// try interceptor and unsaved-value

		if ( ForeignKeys.isTransient( entityName, entity, getAssumedUnsaved(), source )) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Transient instance of: {0}", getLoggableName( entityName, entity ) );
			}
			return EntityState.TRANSIENT;
		}
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Detached instance of: {0}", getLoggableName( entityName, entity ) );
		}
		return EntityState.DETACHED;
	}

	protected String getLoggableName(String entityName, Object entity) {
		return entityName == null ? entity.getClass().getName() : entityName;
	}

	protected Boolean getAssumedUnsaved() {
		return null;
	}
}
