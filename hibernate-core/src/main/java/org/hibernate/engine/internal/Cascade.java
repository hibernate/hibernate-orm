/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.ManagedTypeHelper.asManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.asSelfDirtinessTrackerOrNull;
import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;
import static org.hibernate.engine.spi.CascadingActions.CHECK_ON_FLUSH;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Delegate responsible for, in conjunction with the various
 * {@linkplain CascadingAction actions}, implementing cascade processing.
 *
 * @author Gavin King
 * @see CascadingAction
 */
public final class Cascade {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( Cascade.class );

	private Cascade() {
		// NOP
	}

	/**
	 * Cascade an action from the parent entity instance to all its children.
	 *
	 * @param persister The parent's entity persister
	 * @param parent The parent reference.
	 */
	public static <T> void cascade(
			final CascadingAction<T> action, final CascadePoint cascadePoint,
			final EventSource eventSource, final EntityPersister persister, final Object parent)
			throws HibernateException {
		cascade( action, cascadePoint, eventSource, persister, parent, null );
	}

	/**
	 * Cascade an action from the parent entity instance to all its children.  This
	 * form is typically called from within cascade actions.
	 *
	 * @param persister The parent's entity persister
	 * @param parent The parent reference.
	 * @param anything Anything ;)   Typically some form of cascade-local cache
	 * which is specific to each CascadingAction type
	 */
	public static <T> void cascade(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final EntityPersister persister,
			final Object parent,
			final T anything) throws HibernateException {
		if ( persister.hasCascades() || action == CHECK_ON_FLUSH ) { // performance opt
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Processing cascade {0} for: {1}", action, persister.getEntityName() );
			}
			final PersistenceContext persistenceContext = eventSource.getPersistenceContextInternal();
			final boolean enhancedForLazyLoading = persister.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
			final EntityEntry entry;
			final Set<String> dirtyAttributes;
			if ( enhancedForLazyLoading ) {
				entry = persistenceContext.getEntry( parent );
				if ( entry != null && entry.getLoadedState() == null && entry.getStatus() == Status.MANAGED ) {
					final SelfDirtinessTracker selfDirtinessTracker = asSelfDirtinessTrackerOrNull( parent );
					if ( selfDirtinessTracker == null  ) {
						return;
					}
					else {
						if ( asManagedEntity( parent ).$$_hibernate_useTracker() ) {
							dirtyAttributes = Set.of( selfDirtinessTracker.$$_hibernate_getDirtyAttributes() );
						}
						else {
							dirtyAttributes = null;
						}
					}
				}
				else {
					dirtyAttributes = null;
				}
			}
			else {
				dirtyAttributes = null;
				entry = null;
			}
			final Type[] types = persister.getPropertyTypes();
			final String[] propertyNames = persister.getPropertyNames();
			final CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
			final boolean hasUninitializedLazyProperties = persister.hasUninitializedLazyProperties( parent );

			for ( int i = 0; i < types.length; i++) {
				final String propertyName = propertyNames[ i ];
				if ( dirtyAttributes != null && !dirtyAttributes.contains( propertyName ) ) {
					return;
				}
				final CascadeStyle style = cascadeStyles[ i ];
				final Type type = types[i];
				final boolean isUninitializedProperty =
						hasUninitializedLazyProperties &&
						!persister.getBytecodeEnhancementMetadata().isAttributeLoaded( parent, propertyName );

				if ( style.doCascade( action ) ) {
					final Object child;
					if ( isUninitializedProperty  ) {
						assert enhancedForLazyLoading;
						// parent is a bytecode enhanced entity.
						// Cascade to an uninitialized, lazy value only if
						// parent is managed in the PersistenceContext.
						// If parent is a detached entity being merged,
						// then parent will not be in the PersistenceContext
						// (so lazy attributes must not be initialized).
						if ( entry == null ) {
							// parent was not in the PersistenceContext
							continue;
						}
						if ( type instanceof CollectionType ) {
							// CollectionType#getCollection gets the PersistentCollection
							// that corresponds to the uninitialized collection from the
							// PersistenceContext. If not present, an uninitialized
							// PersistentCollection will be added to the PersistenceContext.
							// The action may initialize it later, if necessary.
							// This needs to be done even when action.performOnLazyProperty() returns false.
							final CollectionType collectionType = (CollectionType) type;
							child = collectionType.getCollection(
									collectionType.getKeyOfOwner( parent, eventSource ),
									eventSource,
									parent,
									null
							);
						}
						else if ( type instanceof AnyType || type instanceof ComponentType ) {
							// Hibernate does not support lazy embeddables, so this shouldn't happen.
							throw new UnsupportedOperationException(
									"Lazy components are not supported."
							);
						}
						else if ( action.performOnLazyProperty() && type instanceof EntityType ) {
							// Only need to initialize a lazy entity attribute when action.performOnLazyProperty()
							// returns true.
							LazyAttributeLoadingInterceptor interceptor = persister.getBytecodeEnhancementMetadata()
									.extractInterceptor( parent );
							child = interceptor.fetchAttribute( parent, propertyName );

						}
						else {
							// Nothing to do, so just skip cascading to this lazy attribute.
							continue;
						}
					}
					else {
						child = persister.getValue( parent, i );
					}
					cascadeProperty(
							action,
							cascadePoint,
							eventSource,
							null,
							parent,
							child,
							type,
							style,
							propertyName,
							anything,
							false
					);
				}
				else {
					// If the property is uninitialized, then there cannot be any orphans.
					if ( action.deleteOrphans() && !isUninitializedProperty && isLogicalOneToOne( type ) ) {
						cascadeLogicalOneToOneOrphanRemoval(
								action,
								eventSource,
								null,
								parent,
								persister.getValue( parent, i ),
								type,
								style,
								propertyName,
								false
						);
					}
				}
			}

			if ( traceEnabled ) {
				LOG.tracev( "Done processing cascade {0} for: {1}", action, persister.getEntityName() );
			}
		}
	}

	/**
	 * Cascade an action to the child or children
	 */
	private static <T> void cascadeProperty(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			List<String> componentPath,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final String propertyName,
			final T anything,
			final boolean isCascadeDeleteEnabled) throws HibernateException {

		if ( child != null ) {
			if ( type instanceof EntityType || type instanceof CollectionType || type instanceof AnyType ) {
				final AssociationType associationType = (AssociationType) type;
				final boolean unownedTransient = eventSource.getSessionFactory()
						.getSessionFactoryOptions()
						.isUnownedAssociationTransientCheck();
				if ( cascadeAssociationNow( action, cascadePoint, associationType, eventSource.getFactory(), unownedTransient ) ) {
					cascadeAssociation(
							action,
							cascadePoint,
							eventSource,
							componentPath,
							parent,
							child,
							type,
							style,
							anything,
							isCascadeDeleteEnabled
						);
				}
			}
			else if ( type instanceof ComponentType ) {
				if ( componentPath == null && propertyName != null ) {
					componentPath = new ArrayList<>();
				}
				if ( componentPath != null ) {
					componentPath.add( propertyName );
				}
				cascadeComponent(
						action,
						cascadePoint,
						eventSource,
						componentPath,
						parent,
						child,
						(CompositeType) type,
						anything
				);
				if ( componentPath != null ) {
					componentPath.remove( componentPath.size() - 1 );
				}
			}
		}
		if ( isLogicalOneToOne( type ) ) {
			cascadeLogicalOneToOneOrphanRemoval(
					action,
					eventSource,
					componentPath,
					parent,
					child,
					type,
					style,
					propertyName,
					isCascadeDeleteEnabled );
		}
	}

	/** potentially we need to handle orphan deletes for one-to-ones here...*/
	private static <T> void cascadeLogicalOneToOneOrphanRemoval(
			final CascadingAction<T> action,
			final EventSource eventSource,
			final List<String> componentPath,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final String propertyName,
			final boolean isCascadeDeleteEnabled) throws HibernateException {

		// We have a physical or logical one-to-one.  See if the attribute cascade settings and action-type require
		// orphan checking
		if ( style.hasOrphanDelete() && action.deleteOrphans() ) {
			// value is orphaned if loaded state for this property shows not null
			// because it is currently null.
			final PersistenceContext persistenceContext = eventSource.getPersistenceContextInternal();
			final EntityEntry entry = persistenceContext.getEntry( parent );
			if ( entry != null && entry.getStatus() != Status.SAVING ) {
				Object loadedValue;
				if ( componentPath == null ) {
					// association defined on entity
					loadedValue = entry.getLoadedValue( propertyName );
				}
				else {
					// association defined on component
					// Since the loadedState in the EntityEntry is a flat domain type array
					// We first have to extract the component object and then ask the component type
					// recursively to give us the value of the sub-property of that object
					final Type propertyType = entry.getPersister().getPropertyType( componentPath.get(0) );
					if ( propertyType instanceof ComponentType ) {
						loadedValue = entry.getLoadedValue( componentPath.get( 0 ) );
						ComponentType componentType = (ComponentType) propertyType;
						if ( componentPath.size() != 1 ) {
							for ( int i = 1; i < componentPath.size(); i++ ) {
								final int subPropertyIndex = componentType.getPropertyIndex( componentPath.get( i ) );
								loadedValue = componentType.getPropertyValue( loadedValue, subPropertyIndex );
								componentType = (ComponentType) componentType.getSubtypes()[subPropertyIndex];
							}
						}

						loadedValue = componentType.getPropertyValue( loadedValue, componentType.getPropertyIndex( propertyName ) );
					}
					else {
						// Association is probably defined in an element collection, so we can't do orphan removals
						loadedValue = null;
					}
				}

				// orphaned if the association was nulled (child == null) or receives a new value while the
				// entity is managed (without first nulling and manually flushing).
				if ( child == null || loadedValue != null && child != loadedValue ) {
					EntityEntry valueEntry = persistenceContext.getEntry( loadedValue );

					if ( valueEntry == null && isHibernateProxy( loadedValue ) ) {
						// un-proxy and re-associate for cascade operation
						// useful for @OneToOne defined as FetchType.LAZY
						loadedValue = persistenceContext.unproxyAndReassociate( loadedValue );
						valueEntry = persistenceContext.getEntry( loadedValue );

						// HHH-11965
						// Should the unwrapped proxy value be equal via reference to the entity's property value
						// provided by the 'child' variable, we should not trigger the orphan removal of the
						// associated one-to-one.
						if ( child == loadedValue ) {
							// do nothing
							return;
						}
					}

					if ( valueEntry != null ) {
						final EntityPersister persister = valueEntry.getPersister();
						final String entityName = persister.getEntityName();
						if ( LOG.isTraceEnabled() ) {
							LOG.tracev(
									"Deleting orphaned entity instance: {0}",
									infoString( entityName, persister.getIdentifier( loadedValue, eventSource ) )
							);
						}

						if ( type instanceof CollectionType
								|| type instanceof OneToOneType && ( (OneToOneType) type ).getForeignKeyDirection() == ForeignKeyDirection.TO_PARENT ) {
							// If FK direction is to-parent, we must remove the orphan *before* the queued update(s)
							// occur.  Otherwise, replacing the association on a managed entity, without manually
							// nulling and flushing, causes FK constraint violations.
							eventSource.removeOrphanBeforeUpdates( entityName, loadedValue );
						}
						else {
							// Else, we must delete after the updates.
							eventSource.delete( entityName, loadedValue, isCascadeDeleteEnabled, DeleteContext.create() );
						}
					}
				}
			}
		}
	}

	/**
	 * Check if the association is a one to one in the logical model (either a shared-pk
	 * or unique fk).
	 *
	 * @param type The type representing the attribute metadata
	 *
	 * @return True if the attribute represents a logical one to one association
	 */
	private static boolean isLogicalOneToOne(Type type) {
		return type instanceof EntityType && ( (EntityType) type ).isLogicalOneToOne();
	}

	private static boolean cascadeAssociationNow(
			CascadingAction<?> action,
			CascadePoint cascadePoint,
			AssociationType associationType,
			SessionFactoryImplementor factory,
			boolean unownedTransient) {
		return associationType.getForeignKeyDirection().cascadeNow( cascadePoint )
				// For check on flush, we should only check unowned associations when strictness is enforced
				&& ( action != CHECK_ON_FLUSH || unownedTransient || !isUnownedAssociation( associationType, factory ) );
	}

	private static boolean isUnownedAssociation(AssociationType associationType, SessionFactoryImplementor factory) {
		if ( associationType instanceof ManyToOneType ) {
			final ManyToOneType manyToOne = (ManyToOneType) associationType;
			// logical one-to-one + non-null unique key property name indicates unowned
			return manyToOne.isLogicalOneToOne() && manyToOne.getRHSUniqueKeyPropertyName() != null;
		}
		else if ( associationType instanceof OneToOneType ) {
			final OneToOneType oneToOne = (OneToOneType) associationType;
			// constrained false + non-null unique key property name indicates unowned
			return oneToOne.isNullable() && oneToOne.getRHSUniqueKeyPropertyName() != null;
		}
		else if ( associationType instanceof CollectionType ) {
			// for collections, we can ask the persister if we're on the inverse side
			return ( (CollectionType) associationType ).isInverse( factory );
		}
		return false;
	}

	private static <T> void cascadeComponent(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final List<String> componentPath,
			final Object parent,
			final Object child,
			final CompositeType componentType,
			final T anything) {
		Object[] children = null;
		final Type[] types = componentType.getSubtypes();
		final String[] propertyNames = componentType.getPropertyNames();
		for ( int i = 0; i < types.length; i++ ) {
			final CascadeStyle componentPropertyStyle = componentType.getCascadeStyle( i );
			final String subPropertyName = propertyNames[i];
			if ( componentPropertyStyle.doCascade( action )
					|| componentPropertyStyle.hasOrphanDelete() && action.deleteOrphans() ) {
				if ( children == null ) {
					// Get children on demand.
					children = componentType.getPropertyValues( child, eventSource );
				}
				cascadeProperty(
						action,
						cascadePoint,
						eventSource,
						componentPath,
						parent,
						children[i],
						types[i],
						componentPropertyStyle,
						subPropertyName,
						anything,
						false
					);
			}
		}
	}

	private static <T> void cascadeAssociation(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final List<String> componentPath,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final T anything,
			final boolean isCascadeDeleteEnabled) {
		if ( type instanceof EntityType || type instanceof AnyType ) {
			cascadeToOne( action, eventSource, parent, child, type, style, anything, isCascadeDeleteEnabled );
		}
		else if ( type instanceof CollectionType ) {
			cascadeCollection(
					action,
					cascadePoint,
					eventSource,
					componentPath,
					parent,
					child,
					style,
					anything,
					(CollectionType) type
			);
		}
	}

	/**
	 * Cascade an action to a collection
	 */
	private static <T> void cascadeCollection(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final List<String> componentPath,
			final Object parent,
			final Object child,
			final CascadeStyle style,
			final T anything,
			final CollectionType type) {
		final CollectionPersister persister =
				eventSource.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );
		final Type elemType = persister.getElementType();

		CascadePoint elementsCascadePoint = cascadePoint;
		if ( cascadePoint == CascadePoint.AFTER_INSERT_BEFORE_DELETE ) {
			elementsCascadePoint = CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION;
		}

		//cascade to current collection elements
		if ( elemType instanceof EntityType || elemType instanceof AnyType || elemType instanceof ComponentType ) {
			cascadeCollectionElements(
				action,
				elementsCascadePoint,
				eventSource,
				componentPath,
				parent,
				child,
				type,
				style,
				elemType,
				anything,
				persister.isCascadeDeleteEnabled()
			);
		}
	}

	/**
	 * Cascade an action to a to-one association or any type
	 */
	private static <T> void cascadeToOne(
			final CascadingAction<T> action,
			final EventSource eventSource,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final T anything,
			final boolean isCascadeDeleteEnabled) {
		final String entityName = type instanceof EntityType
				? ( (EntityType) type ).getAssociatedEntityName()
				: null;
		if ( style.reallyDoCascade( action ) ) {
			//not really necessary, but good for consistency...
			final PersistenceContext persistenceContext = eventSource.getPersistenceContextInternal();
			persistenceContext.addChildParent( child, parent );
			try {
				action.cascade( eventSource, child, entityName, anything, isCascadeDeleteEnabled );
			}
			finally {
				persistenceContext.removeChildParent( child );
			}
		}
	}

	/**
	 * Cascade to the collection elements
	 */
	private static <T> void cascadeCollectionElements(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final List<String> componentPath,
			final Object parent,
			final Object child,
			final CollectionType collectionType,
			final CascadeStyle style,
			final Type elemType,
			final T anything,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		final boolean reallyDoCascade = style.reallyDoCascade( action )
				&& child != CollectionType.UNFETCHED_COLLECTION;

		if ( reallyDoCascade ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Cascade {0} for collection: {1}", action, collectionType.getRole() );
			}

			final Iterator<?> itr = action.getCascadableChildrenIterator( eventSource, collectionType, child );
			while ( itr.hasNext() ) {
				cascadeProperty(
						action,
						cascadePoint,
						eventSource,
						componentPath,
						parent,
						itr.next(),
						elemType,
						style,
						collectionType.getRole().substring( collectionType.getRole().lastIndexOf('.') + 1 ),
						anything,
						isCascadeDeleteEnabled
				);
			}

			if ( traceEnabled ) {
				LOG.tracev( "Done cascade {0} for collection: {1}", action, collectionType.getRole() );
			}
		}

		// a newly instantiated collection can't have orphans
		final PersistentCollection<?> persistentCollection;
		if ( child instanceof PersistentCollection<?> ) {
			persistentCollection = (PersistentCollection<?>) child;
		}
		else {
			persistentCollection = eventSource.getPersistenceContext()
					.getCollectionHolder( child );
		}

		final boolean deleteOrphans = style.hasOrphanDelete()
				&& action.deleteOrphans()
				&& elemType instanceof EntityType
				&& persistentCollection != null
				// a newly instantiated collection can't have orphans
				&& !persistentCollection.isNewlyInstantiated();

		if ( deleteOrphans ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Deleting orphans for collection: {0}", collectionType.getRole() );
			}
			// we can do the cast since orphan-delete does not apply to:
			// 1. newly instantiated collections
			// 2. arrays (we can't track orphans for detached arrays)
			final String entityName = collectionType.getAssociatedEntityName( eventSource.getFactory() );
			deleteOrphans( eventSource, entityName, persistentCollection );

			if ( traceEnabled ) {
				LOG.tracev( "Done deleting orphans for collection: {0}", collectionType.getRole() );
			}
		}
	}

	/**
	 * Delete any entities that were removed from the collection
	 */
	private static void deleteOrphans(EventSource eventSource, String entityName, PersistentCollection<?> pc) {
		//TODO: suck this logic into the collection!
		final Collection<?> orphans;
		if ( pc.wasInitialized() ) {
			final CollectionEntry ce = eventSource.getPersistenceContextInternal().getCollectionEntry( pc );
			orphans = ce==null
					? java.util.Collections.EMPTY_LIST
					: ce.getOrphans( entityName, pc );
		}
		else {
			orphans = pc.getQueuedOrphans( entityName );
		}

		for ( Object orphan : orphans ) {
			if ( orphan != null ) {
				LOG.tracev( "Deleting orphaned entity instance: {0}", entityName );
				eventSource.delete( entityName, orphan, false, DeleteContext.create() );
			}
		}
	}
}
