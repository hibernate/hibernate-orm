/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.internal.TypeHelper;

import static org.hibernate.FlushMode.COMMIT;
import static org.hibernate.FlushMode.MANUAL;

/**
 * A convenience base class for listeners responding to save events.
 *
 * @author Steve Ebersole.
 */
public abstract class AbstractSaveEventListener
		extends AbstractReassociateEventListener
		implements CallbackRegistryConsumer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractSaveEventListener.class );

	public enum EntityState {
		PERSISTENT, TRANSIENT, DETACHED, DELETED
	}

	private CallbackRegistry callbackRegistry;

	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

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
	protected Object saveWithRequestedId(
			Object entity,
			Object requestedId,
			String entityName,
			Object anything,
			EventSource source) {
		callbackRegistry.preCreate( entity );

		return performSave(
				entity,
				requestedId,
				source.getEntityDescriptor( entityName, entity ),
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
	@SuppressWarnings("unchecked")
	protected Object saveWithGeneratedId(
			Object entity,
			String entityName,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {
		callbackRegistry.preCreate( entity );

		if ( entity instanceof SelfDirtinessTracker ) {
			( (SelfDirtinessTracker) entity ).$$_hibernate_clearDirtyAttributes();
		}

		final EntityTypeDescriptor entityDescriptor = source.getEntityDescriptor( entityName, entity );
		final EntityIdentifier<Object, Object> identifierDescriptor = entityDescriptor.getHierarchy()
				.getIdentifierDescriptor();
		Object generatedId = identifierDescriptor
				.getIdentifierValueGenerator()
				.generate( source, entity );
		if ( generatedId == null ) {
			throw new IdentifierGenerationException( "null id generated for:" + entity.getClass() );
		}
		else if ( generatedId == IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR ) {
			return source.getIdentifier( entity );
		}
		else if ( generatedId == IdentifierGeneratorHelper.POST_INSERT_INDICATOR ) {
			return performSave( entity, null, entityDescriptor, true, anything, source, requiresImmediateIdAccess );
		}
		else {
			// TODO: define toString()s for generators
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Generated identifier: %s, using strategy: %s",
						entityDescriptor.getIdentifierDescriptor().getJavaTypeDescriptor().extractLoggableRepresentation( generatedId ),
						identifierDescriptor.getClass().getName()
				);
			}

			return performSave( entity, generatedId, entityDescriptor, false, anything, source, true );
		}
	}

	/**
	 * Prepares the save call by checking the session caches for a pre-existing
	 * entity and performing any lifecycle callbacks.
	 *
	 * @param entity The entity to be saved.
	 * @param id The id by which to save the entity.
	 * @param descriptor The entity's descriptor instance.
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
	protected Object performSave(
			Object entity,
			Object id,
			EntityTypeDescriptor descriptor,
			boolean useIdentityColumn,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Saving {0}", MessageHelper.infoString( descriptor, id, source.getFactory() ) );
		}

		final EntityKey key;
		if ( !useIdentityColumn ) {
			key = source.generateEntityKey( id, descriptor );
			Object old = source.getPersistenceContext().getEntity( key );
			if ( old != null ) {
				if ( source.getPersistenceContext().getEntry( old ).getStatus() == Status.DELETED ) {
					source.forceFlush( source.getPersistenceContext().getEntry( old ) );
				}
				else {
					throw new NonUniqueObjectException( id, descriptor.getEntityName() );
				}
			}
			descriptor.setIdentifier( entity, id, source );
		}
		else {
			key = null;
		}

		if ( invokeSaveLifecycle( entity, descriptor, source ) ) {
			return id; //EARLY EXIT
		}

		return performSaveOrReplicate(
				entity,
				key,
				descriptor,
				useIdentityColumn,
				anything,
				source,
				requiresImmediateIdAccess
		);
	}

	protected boolean invokeSaveLifecycle(Object entity, EntityTypeDescriptor descriptor, EventSource source) {
		// Sub-insertions should occur before containing insertion so
		// Try to do the callback now
		if ( descriptor.implementsLifecycle() ) {
			LOG.debug( "Calling onSave()" );
			if ( ((Lifecycle) entity).onSave( source ) ) {
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
	 * @param entityDescriptor The entity's entityDescriptor instance.
	 * @param useIdentityColumn Should an identity column be used for id generation?
	 * @param anything Generally cascade-specific information.
	 * @param source The session which is the source of the current event.
	 * @param requiresImmediateIdAccess Is access to the identifier required immediately
	 * after the completion of the save?  persist(), for example, does not require this...
	 *
	 * @return The id used to save the entity; may be null depending on the
	 *         type of id generator used and the requiresImmediateIdAccess value
	 */
	protected Object performSaveOrReplicate(
			Object entity,
			EntityKey key,
			EntityTypeDescriptor entityDescriptor,
			boolean useIdentityColumn,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {

		Object id = key == null ? null : key.getIdentifier();

		boolean shouldDelayIdentityInserts = shouldDelayIdentityInserts( requiresImmediateIdAccess, source, entityDescriptor );

		// Put a placeholder in entries, so we don't recurse back and try to save() the
		// same object again. QUESTION: should this be done before onSave() is called?
		// likewise, should it be done before onUpdate()?

		// todo (6.0) : Should we do something here like `org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState#registerLoadingEntity` ?
		EntityEntry original = source.getPersistenceContext().addEntry(
				entity,
				Status.SAVING,
				null,
				null,
				id,
				null,
				LockMode.WRITE,
				useIdentityColumn,
				entityDescriptor,
				false
		);

		cascadeBeforeSave( source, entityDescriptor, entity, anything );

		Object[] values = entityDescriptor.getPropertyValuesToInsert( entity, getMergeMap( anything ), source );
//		final List<Navigable> navigables = entityDescriptor.getNavigables();

		boolean substitute = substituteValuesIfNecessary( entity, id, values, entityDescriptor, source );

		if ( entityDescriptor.hasCollections() ) {
			substitute = substitute || visitCollectionsBeforeSave( entity, id, values, entityDescriptor.getPersistentAttributes(), source );
		}

		if ( substitute ) {
			entityDescriptor.setPropertyValues( entity, values );
		}

		TypeHelper.deepCopy(
				entityDescriptor,
				values,
				values,
				StateArrayContributor::isUpdatable
		);

		AbstractEntityInsertAction insert = addInsertAction(
				values, id, entity, entityDescriptor, useIdentityColumn, source, shouldDelayIdentityInserts
		);

		// postpone initializing id in case the insert has non-nullable transient dependencies
		// that are not resolved until cascadeAfterSave() is executed
		cascadeAfterSave( source, entityDescriptor, entity, anything );
		if ( useIdentityColumn && insert.isEarlyInsert() ) {
			if ( !EntityIdentityInsertAction.class.isInstance( insert ) ) {
				throw new IllegalStateException(
						"Insert should be using an identity column, but action is of unexpected type: " +
								insert.getClass().getName()
				);
			}
			id = ((EntityIdentityInsertAction) insert).getGeneratedId();

			insert.handleNaturalIdPostSaveNotifications( id );
		}

		EntityEntry newEntry = source.getPersistenceContext().getEntry( entity );

		if ( newEntry != original ) {
			EntityEntryExtraState extraState = newEntry.getExtraState( EntityEntryExtraState.class );
			if ( extraState == null ) {
				newEntry.addExtraState( original.getExtraState( EntityEntryExtraState.class ) );
			}
		}

		return id;
	}

	private static boolean shouldDelayIdentityInserts(
			boolean requiresImmediateIdAccess,
			EventSource source,
			EntityTypeDescriptor descriptor) {
		return shouldDelayIdentityInserts(
				requiresImmediateIdAccess,
				isPartOfTransaction( source ),
				source.getHibernateFlushMode(),
				descriptor
		);
	}

	private static boolean shouldDelayIdentityInserts(
			boolean requiresImmediateIdAccess,
			boolean partOfTransaction,
			FlushMode flushMode,
			EntityTypeDescriptor entityDescriptor) {
		if ( !entityDescriptor.getFactory().getSessionFactoryOptions().isPostInsertIdentifierDelayableEnabled() ) {
			return false;
		}

		if ( requiresImmediateIdAccess ) {
			// todo : make this configurable?  as a way to support this behavior with Session#save etc
			return false;
		}

		// otherwise we should delay the IDENTITY insertions if either:
		//		1) we are not part of a transaction
		//		2) we are in FlushMode MANUAL or COMMIT (not AUTO nor ALWAYS)
		if ( !partOfTransaction || flushMode == MANUAL || flushMode == COMMIT ) {
			if ( entityDescriptor.canIdentityInsertBeDelayed() ) {
				return true;
			}
			LOG.debugf(
					"Identity insert for entity [%s] should be delayed; however the persister requested early insert.",
					entityDescriptor.getEntityName()
			);
			return false;
		}
		else {
			return false;
		}
	}

	private static boolean isPartOfTransaction(EventSource source) {
		return source.isTransactionInProgress() && source.getTransactionCoordinator().isJoined();
	}

	private AbstractEntityInsertAction addInsertAction(
			Object[] values,
			Object id,
			Object entity,
			EntityTypeDescriptor descriptor,
			boolean useIdentityColumn,
			EventSource source,
			boolean shouldDelayIdentityInserts) {
		if ( useIdentityColumn ) {
			EntityIdentityInsertAction insert = new EntityIdentityInsertAction(
					values, entity, descriptor, isVersionIncrementDisabled(), source, shouldDelayIdentityInserts
			);
			source.getActionQueue().addAction( insert );
			return insert;
		}
		else {
			Object version = Versioning.getVersion( values, descriptor );
			EntityInsertAction insert = new EntityInsertAction(
					id, values, entity, version, descriptor, isVersionIncrementDisabled(), source
			);
			source.getActionQueue().addAction( insert );
			return insert;
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

	protected boolean visitCollectionsBeforeSave(
			Object entity,
			Object id,
			Object[] values,
			List<PersistentAttributeDescriptor> attributes,
			EventSource source) {
		WrapVisitor visitor = new WrapVisitor( source );
		// substitutes into values by side-effect
		visitor.processEntityPropertyValues( values, attributes );
		return visitor.isSubstitutionRequired();
	}

	/**
	 * Perform any property value substitution that is necessary
	 * (interceptor callback, version initialization...)
	 *
	 * @param entity The entity
	 * @param id The entity identifier
	 * @param values The snapshot entity state
	 * @param entityDescriptor The entity descriptor
	 * @param source The originating session
	 *
	 * @return True if the snapshot state changed such that
	 *         reinjection of the values into the entity is required.
	 */
	protected boolean substituteValuesIfNecessary(
			Object entity,
			Object id,
			Object[] values,
			EntityTypeDescriptor entityDescriptor,
			SessionImplementor source) {
		boolean substitute = source.getInterceptor().onSave(
				entity,
				id,
				values,
				entityDescriptor.getPropertyNames(),
				entityDescriptor.getPropertyJavaTypeDescriptors()
		);

		// keep the existing version number in the case of replicate!
		final VersionDescriptor versionDescriptor = entityDescriptor.getHierarchy().getVersionDescriptor();
		if ( versionDescriptor != null ) {
			substitute = Versioning.seedVersion(
					values,
					versionDescriptor,
					source
			) || substitute;
		}
		return substitute;
	}

	/**
	 * Handles the calls needed to perform pre-save cascades for the given entity.
	 *
	 * @param source The session from whcih the save event originated.
	 * @param descriptor The entity's descriptor instance.
	 * @param entity The entity to be saved.
	 * @param anything Generally cascade-specific data
	 */
	protected void cascadeBeforeSave(
			EventSource source,
			EntityTypeDescriptor descriptor,
			Object entity,
			Object anything) {

		// cascade-save to many-to-one BEFORE the parent is saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascade.cascade(
					getCascadeAction(),
					CascadePoint.BEFORE_INSERT_AFTER_DELETE,
					source,
					descriptor,
					entity,
					anything
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	/**
	 * Handles to calls needed to perform post-save cascades.
	 *
	 * @param source The session from which the event originated.
	 * @param descriptor The entity's descriptor instance.
	 * @param entity The entity beng saved.
	 * @param anything Generally cascade-specific data
	 */
	protected void cascadeAfterSave(
			EventSource source,
			EntityTypeDescriptor descriptor,
			Object entity,
			Object anything) {

		// cascade-save to collections AFTER the collection owner was saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascade.cascade(
					getCascadeAction(),
					CascadePoint.AFTER_INSERT_BEFORE_DELETE,
					source,
					descriptor,
					entity,
					anything
			);
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

		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( entry != null ) { // the object is persistent

			//the entity is associated with the session, so check its status
			if ( entry.getStatus() != Status.DELETED ) {
				// do nothing for persistent instances
				if ( traceEnabled ) {
					LOG.tracev( "Persistent instance of: {0}", getLoggableName( entityName, entity ) );
				}
				return EntityState.PERSISTENT;
			}
			// ie. e.status==DELETED
			if ( traceEnabled ) {
				LOG.tracev( "Deleted instance of: {0}", getLoggableName( entityName, entity ) );
			}
			return EntityState.DELETED;
		}
		// the object is transient or detached

		// the entity is not associated with the session, so
		// try interceptor and unsaved-value

		if ( ForeignKeys.isTransient( entityName, entity, getAssumedUnsaved(), source ) ) {
			if ( traceEnabled ) {
				LOG.tracev( "Transient instance of: {0}", getLoggableName( entityName, entity ) );
			}
			return EntityState.TRANSIENT;
		}
		if ( traceEnabled ) {
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
