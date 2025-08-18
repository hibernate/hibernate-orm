/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.internal.Nullability.NullabilityCheckType;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.NaturalIdResolutions;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.internal.Versioning.getVersion;

/**
 * A base class for entity insert actions.
 *
 * @author Gail Badner
 */
public abstract class AbstractEntityInsertAction extends EntityAction {
	private transient Object[] state;
	private final boolean isVersionIncrementDisabled;
	private boolean isExecuted;
	private boolean areTransientReferencesNullified;

	/**
	 * Constructs an AbstractEntityInsertAction object.
	 *  @param id - the entity ID
	 * @param state - the entity state
	 * @param instance - the entity
	 * @param isVersionIncrementDisabled - true, if version increment should
*                                     be disabled; false, otherwise
	 * @param persister - the entity persister
	 * @param session - the session
	 */
	protected AbstractEntityInsertAction(
			Object id,
			Object[] state,
			Object instance,
			boolean isVersionIncrementDisabled,
			EntityPersister persister,
			EventSource session) {
		super( session, id, instance, persister );
		this.state = state;
		this.isVersionIncrementDisabled = isVersionIncrementDisabled;
		this.isExecuted = false;
		this.areTransientReferencesNullified = false;

		if ( id != null ) {
			handleNaturalIdPreSaveNotifications();
		}
	}

	/**
	 * Returns the entity state.
	 * <P>
	 * Note that the call to {@link #nullifyTransientReferencesIfNotAlready}
	 * can modify the entity state.
	 *
	 * @return the entity state.
	 *
	 * @see #nullifyTransientReferencesIfNotAlready
	 */
	public Object[] getState() {
		return state;
	}

	/**
	 * Does this insert action need to be executed as soon as possible
	 * (e.g., to generate an ID)?
	 * @return true, if it needs to be executed as soon as possible;
	 *         false, otherwise.
	 */
	public abstract boolean isEarlyInsert();

	/**
	 * Find the transient unsaved entity dependencies that are non-nullable.
	 * @return the transient unsaved entity dependencies that are non-nullable,
	 *         or null if there are none.
	 */
	public NonNullableTransientDependencies findNonNullableTransientEntities() {
		return ForeignKeys.findNonNullableTransientEntities(
				getPersister().getEntityName(),
				getInstance(),
				getState(),
				isEarlyInsert(),
				getSession()
		);
	}

	/**
	 * Nullifies any references to transient entities in the entity state
	 * maintained by this action. References to transient entities
	 * should be nullified when an entity is made "managed" or when this
	 * action is executed, whichever is first.
	 * <p>
	 * References will only be nullified the first time this method is
	 * called for a this object, so it can safely be called both when
	 * the entity is made "managed" and when this action is executed.
	 *
	 * @see #makeEntityManaged()
	 */
	protected final void nullifyTransientReferencesIfNotAlready() {
		if ( !areTransientReferencesNullified ) {
			new ForeignKeys.Nullifier( getInstance(), false, isEarlyInsert(), getSession(), getPersister() )
					.nullifyTransientReferences( getState() );
			new Nullability( getSession(), NullabilityCheckType.CREATE )
					.checkNullability( getState(), getPersister() );
			areTransientReferencesNullified = true;
		}
	}

	/**
	 * Make the entity "managed" by the persistence context.
	 */
	public final void makeEntityManaged() {
		nullifyTransientReferencesIfNotAlready();
		final Object version = getVersion( getState(), getPersister() );
		final var persistenceContext = getSession().getPersistenceContextInternal();
		final var entityHolder = persistenceContext.addEntityHolder( getEntityKey(), getInstance() );
		final var entityEntry = persistenceContext.addEntry(
				getInstance(),
				getPersister().isMutable() ? Status.MANAGED : Status.READ_ONLY,
				getState(),
				getRowId(),
				getEntityKey().getIdentifier(),
				version,
				LockMode.WRITE,
				isExecuted,
				getPersister(),
				isVersionIncrementDisabled
		);
		entityHolder.setEntityEntry( entityEntry );
		if ( isEarlyInsert() ) {
			addCollectionsByKeyToPersistenceContext( persistenceContext, getState() );
		}
	}

	protected void addCollectionsByKeyToPersistenceContext(PersistenceContext persistenceContext, Object[] objects) {
		for ( int i = 0; i < objects.length; i++ ) {
			final var attributeMapping = getPersister().getAttributeMapping( i );
			if ( attributeMapping.isEmbeddedAttributeMapping() ) {
				visitEmbeddedAttributeMapping(
						attributeMapping.asEmbeddedAttributeMapping(),
						objects[i],
						persistenceContext
				);
			}
			else if ( attributeMapping.isPluralAttributeMapping() ) {
				addCollectionKey(
						attributeMapping.asPluralAttributeMapping(),
						objects[i],
						persistenceContext
				);
			}
		}
	}

	private void visitEmbeddedAttributeMapping(
			EmbeddedAttributeMapping attributeMapping,
			Object object,
			PersistenceContext persistenceContext) {
		if ( object != null ) {
			final var descriptor = attributeMapping.getEmbeddableTypeDescriptor();
			final var concreteEmbeddableType =
					descriptor.findSubtypeBySubclass( object.getClass().getName() );
			final var attributeMappings = descriptor.getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final var attribute = attributeMappings.get( i );
				if ( concreteEmbeddableType.declaresAttribute( attribute ) ) {
					if ( attribute.isPluralAttributeMapping() ) {
						addCollectionKey(
								attribute.asPluralAttributeMapping(),
								descriptor.getValue( object, i ),
								persistenceContext
						);
					}
					else if ( attribute.isEmbeddedAttributeMapping() ) {
						visitEmbeddedAttributeMapping(
								attribute.asEmbeddedAttributeMapping(),
								descriptor.getValue( object, i ),
								persistenceContext
						);
					}
				}
			}
		}
	}

	private void addCollectionKey(
			PluralAttributeMapping pluralAttributeMapping,
			Object object,
			PersistenceContext persistenceContext) {
		if ( object instanceof PersistentCollection ) {
			final var collectionPersister = pluralAttributeMapping.getCollectionDescriptor();
			final Object key = AbstractEntityPersister.getCollectionKey(
					collectionPersister,
					getInstance(),
					persistenceContext.getEntry( getInstance() ),
					getSession()
			);
			if ( key != null ) {
				final var collectionKey = new CollectionKey( collectionPersister, key );
				persistenceContext.addCollectionByKey( collectionKey, (PersistentCollection<?>) object );
			}
		}
	}

	/**
	 * Indicate that the action has executed.
	 */
	protected void markExecuted() {
		this.isExecuted = true;
	}

	/**
	 * Returns the {@link EntityKey}.
	 * @return the {@link EntityKey}.
	 */
	protected abstract EntityKey getEntityKey();

	protected abstract Object getRowId();

	private NaturalIdResolutions getNaturalIdResolutions() {
		return getSession().getPersistenceContextInternal().getNaturalIdResolutions();
	}

	@Override
	public void afterDeserialize(EventSource session) {
		super.afterDeserialize( session );
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			final var entityEntry = session.getPersistenceContextInternal().getEntry( getInstance() );
			this.state = entityEntry.getLoadedState();
		}
	}

	/**
	 * Handle sending notifications needed for natural-id before saving
	 */
	protected void handleNaturalIdPreSaveNotifications() {
		// before save, we need to add a natural id cross-reference to the persistence-context
		final var naturalIdMapping = getPersister().getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			getNaturalIdResolutions().manageLocalResolution(
					getId(),
					naturalIdMapping.extractNaturalIdFromEntityState( state ),
					getPersister(),
					CachedNaturalIdValueSource.INSERT
			);
		}
	}

	/**
	 * Handle sending notifications needed for natural-id after saving
	 *
	 * @param generatedId The generated entity identifier
	 */
	public void handleNaturalIdPostSaveNotifications(Object generatedId) {
		final var naturalIdMapping = getPersister().getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			final Object naturalIdValues = naturalIdMapping.extractNaturalIdFromEntityState( state );
			final var resolutions = getNaturalIdResolutions();
			if ( isEarlyInsert() ) {
				// with early insert, we still need to add a local (transactional) natural id cross-reference
				resolutions.manageLocalResolution(
						generatedId,
						naturalIdValues,
						getPersister(),
						CachedNaturalIdValueSource.INSERT
				);
			}
			// after save, we need to manage the shared cache entries
			resolutions.manageSharedResolution(
					generatedId,
					naturalIdValues,
					null,
					getPersister(),
					CachedNaturalIdValueSource.INSERT
			);
		}
	}
}
