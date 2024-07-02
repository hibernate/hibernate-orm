/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
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
			new Nullability( getSession() ).checkNullability( getState(), getPersister(), false );
			areTransientReferencesNullified = true;
		}
	}

	/**
	 * Make the entity "managed" by the persistence context.
	 */
	public final void makeEntityManaged() {
		nullifyTransientReferencesIfNotAlready();
		final Object version = getVersion( getState(), getPersister() );
		final PersistenceContext persistenceContextInternal = getSession().getPersistenceContextInternal();
		final EntityHolder entityHolder = persistenceContextInternal.addEntityHolder(
				getEntityKey(),
				getInstance()
		);
		final EntityEntry entityEntry = persistenceContextInternal.addEntry(
				getInstance(),
				( getPersister().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
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
			addCollectionsByKeyToPersistenceContext( persistenceContextInternal, getState() );
		}
	}

	protected void addCollectionsByKeyToPersistenceContext(PersistenceContext persistenceContext, Object[] objects) {
		for ( int i = 0; i < objects.length; i++ ) {
			final AttributeMapping attributeMapping = getPersister().getAttributeMapping( i );
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
			final EmbeddableMappingType descriptor = attributeMapping.getEmbeddableTypeDescriptor();
			final EmbeddableMappingType.ConcreteEmbeddableType concreteEmbeddableType = descriptor.findSubtypeBySubclass(
					object.getClass().getName()
			);
			final AttributeMappingsList attributeMappings = descriptor.getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attribute = attributeMappings.get( i );
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
			Object o,
			PersistenceContext persistenceContext) {
		if ( o instanceof PersistentCollection ) {
			final CollectionPersister collectionPersister = pluralAttributeMapping.getCollectionDescriptor();
			final Object key = ( (AbstractEntityPersister) getPersister() ).getCollectionKey(
					collectionPersister,
					getInstance(),
					persistenceContext.getEntry( getInstance() ),
					getSession()
			);
			if ( key != null ) {
				final CollectionKey collectionKey = new CollectionKey( collectionPersister, key );
				persistenceContext.addCollectionByKey( collectionKey, (PersistentCollection<?>) o );
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

	@Override
	public void afterDeserialize(EventSource session) {
		super.afterDeserialize( session );
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			final EntityEntry entityEntry = session.getPersistenceContextInternal().getEntry( getInstance() );
			this.state = entityEntry.getLoadedState();
		}
	}

	/**
	 * Handle sending notifications needed for natural-id before saving
	 */
	protected void handleNaturalIdPreSaveNotifications() {
		// before save, we need to add a natural id cross-reference to the persistence-context
		final NaturalIdMapping naturalIdMapping = getPersister().getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			getSession().getPersistenceContextInternal().getNaturalIdResolutions().manageLocalResolution(
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
		final NaturalIdMapping naturalIdMapping = getPersister().getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			final Object naturalIdValues = naturalIdMapping.extractNaturalIdFromEntityState( state );
			if ( isEarlyInsert() ) {
				// with early insert, we still need to add a local (transactional) natural id cross-reference
				getSession().getPersistenceContextInternal().getNaturalIdResolutions().manageLocalResolution(
						generatedId,
						naturalIdValues,
						getPersister(),
						CachedNaturalIdValueSource.INSERT
				);
			}
			// after save, we need to manage the shared cache entries
			getSession().getPersistenceContextInternal().getNaturalIdResolutions().manageSharedResolution(
					generatedId,
					naturalIdValues,
					null,
					getPersister(),
					CachedNaturalIdValueSource.INSERT
			);
		}
	}
}
