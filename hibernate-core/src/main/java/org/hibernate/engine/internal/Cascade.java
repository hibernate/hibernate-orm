/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * Delegate responsible for, in conjunction with the various
 * {@link org.hibernate.engine.spi.CascadingAction actions}, implementing cascade processing.
 *
 * @author Gavin King
 * @see org.hibernate.engine.spi.CascadingAction
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
	 * @throws HibernateException
	 */
	public static void cascade(
			final CascadingAction action, final CascadePoint cascadePoint,
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
	public static void cascade(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final EntityPersister persister,
			final Object parent,
			final Object anything) throws HibernateException {

		if ( persister.hasCascades() || action.requiresNoCascadeChecking() ) { // performance opt
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Processing cascade {0} for: {1}", action, persister.getEntityName() );
			}
			final PersistenceContext persistenceContext = eventSource.getPersistenceContextInternal();

			final Type[] types = persister.getPropertyTypes();
			final String[] propertyNames = persister.getPropertyNames();
			final CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
			final boolean hasUninitializedLazyProperties = persister.hasUninitializedLazyProperties( parent );
			final int componentPathStackDepth = 0;
			for ( int i = 0; i < types.length; i++) {
				final CascadeStyle style = cascadeStyles[ i ];
				final String propertyName = propertyNames[ i ];
				final boolean isUninitializedProperty =
						hasUninitializedLazyProperties &&
						!persister.getBytecodeEnhancementMetadata().isAttributeLoaded( parent, propertyName );

				if ( style.doCascade( action ) ) {
					final Object child;
					if ( isUninitializedProperty  ) {
						// parent is a bytecode enhanced entity.
						// Cascade to an uninitialized, lazy value only if
						// parent is managed in the PersistenceContext.
						// If parent is a detached entity being merged,
						// then parent will not be in the PersistencContext
						// (so lazy attributes must not be initialized).
						if ( persistenceContext.getEntry( parent ) == null ) {
							// parent was not in the PersistenceContext
							continue;
						}
						if ( types[ i ].isCollectionType() ) {
							// CollectionType#getCollection gets the PersistentCollection
							// that corresponds to the uninitialized collection from the
							// PersistenceContext. If not present, an uninitialized
							// PersistentCollection will be added to the PersistenceContext.
							// The action may initialize it later, if necessary.
							// This needs to be done even when action.performOnLazyProperty() returns false.
							final CollectionType collectionType = (CollectionType) types[i];
							child = collectionType.getCollection(
									collectionType.getKeyOfOwner( parent, eventSource ),
									eventSource,
									parent,
									null
							);
						}
						else if ( types[ i ].isComponentType() ) {
							// Hibernate does not support lazy embeddables, so this shouldn't happen.
							throw new UnsupportedOperationException(
									"Lazy components are not supported."
							);
						}
						else if ( action.performOnLazyProperty() && types[ i ].isEntityType() ) {
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
						child = persister.getPropertyValue( parent, i );
					}
					cascadeProperty(
							action,
							cascadePoint,
							eventSource,
							componentPathStackDepth,
							parent,
							child,
							types[ i ],
							style,
							propertyName,
							anything,
							false
					);
				}
				else {
					if ( action.requiresNoCascadeChecking() ) {
						action.noCascade(
								eventSource,
								parent,
								persister,
								types[i],
								i
						);
					}
					// If the property is uninitialized, then there cannot be any orphans.
					if ( action.deleteOrphans() && !isUninitializedProperty ) {
						cascadeLogicalOneToOneOrphanRemoval(
								action,
								eventSource,
								componentPathStackDepth,
								parent,
								persister.getPropertyValue( parent, i ),
								types[ i ],
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
	private static void cascadeProperty(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final String propertyName,
			final Object anything,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		
		if ( child != null ) {
			if ( type.isAssociationType() ) {
				final AssociationType associationType = (AssociationType) type;
				if ( cascadeAssociationNow( cascadePoint, associationType ) ) {
					cascadeAssociation(
							action,
							cascadePoint,
							eventSource,
							componentPathStackDepth,
							parent,
							child,
							type,
							style,
							anything,
							isCascadeDeleteEnabled
						);
				}
			}
			else if ( type.isComponentType() ) {
				cascadeComponent(
						action,
						cascadePoint,
						eventSource,
						componentPathStackDepth,
						parent,
						child,
						(CompositeType) type,
						anything
				);
			}
		}

		cascadeLogicalOneToOneOrphanRemoval(
				action,
				eventSource,
				componentPathStackDepth,
				parent,
				child,
				type,
				style,
				propertyName,
				isCascadeDeleteEnabled );
	}

	private static void cascadeLogicalOneToOneOrphanRemoval(
			final CascadingAction action,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final String propertyName,
			final boolean isCascadeDeleteEnabled) throws HibernateException {

		// potentially we need to handle orphan deletes for one-to-ones here...
		if ( isLogicalOneToOne( type ) ) {
			// We have a physical or logical one-to-one.  See if the attribute cascade settings and action-type require
			// orphan checking
			if ( style.hasOrphanDelete() && action.deleteOrphans() ) {
				// value is orphaned if loaded state for this property shows not null
				// because it is currently null.
				final PersistenceContext persistenceContext = eventSource.getPersistenceContextInternal();
				final EntityEntry entry = persistenceContext.getEntry( parent );
				if ( entry != null && entry.getStatus() != Status.SAVING ) {
					Object loadedValue;
					if ( componentPathStackDepth == 0 ) {
						// association defined on entity
						loadedValue = entry.getLoadedValue( propertyName );
					}
					else {
						// association defined on component
						// 		todo : this is currently unsupported because of the fact that
						//		we do not know the loaded state of this value properly
						//		and doing so would be very difficult given how components and
						//		entities are loaded (and how 'loaded state' is put into the
						//		EntityEntry).  Solutions here are to either:
						//			1) properly account for components as a 2-phase load construct
						//			2) just assume the association was just now orphaned and
						// 				issue the orphan delete.  This would require a special
						//				set of SQL statements though since we do not know the
						//				orphaned value, something a delete with a subquery to
						// 				match the owner.
//							final EntityType entityType = (EntityType) type;
//							final String getPropertyPath = composePropertyPath( entityType.getPropertyName() );
						loadedValue = null;
					}

					// orphaned if the association was nulled (child == null) or receives a new value while the
					// entity is managed (without first nulling and manually flushing).
					if ( child == null || ( loadedValue != null && child != loadedValue ) ) {
						EntityEntry valueEntry = persistenceContext.getEntry( loadedValue );

						if ( valueEntry == null && loadedValue instanceof HibernateProxy ) {
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
							final String entityName = valueEntry.getPersister().getEntityName();
							if ( LOG.isTraceEnabled() ) {
								final Serializable id = valueEntry.getPersister().getIdentifier( loadedValue, eventSource );
								final String description = MessageHelper.infoString( entityName, id );
								LOG.tracev( "Deleting orphaned entity instance: {0}", description );
							}

							if ( type.isAssociationType() && ( (AssociationType) type ).getForeignKeyDirection().equals(
									ForeignKeyDirection.TO_PARENT
							) ) {
								// If FK direction is to-parent, we must remove the orphan *before* the queued update(s)
								// occur.  Otherwise, replacing the association on a managed entity, without manually
								// nulling and flushing, causes FK constraint violations.
								eventSource.removeOrphanBeforeUpdates( entityName, loadedValue );
							}
							else {
								// Else, we must delete after the updates.
								eventSource.delete( entityName, loadedValue, isCascadeDeleteEnabled, new HashSet() );
							}
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
		return type.isEntityType() && ( (EntityType) type ).isLogicalOneToOne();
	}

	private static boolean cascadeAssociationNow(final CascadePoint cascadePoint, AssociationType associationType) {
		return associationType.getForeignKeyDirection().cascadeNow( cascadePoint );
	}

	private static void cascadeComponent(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final CompositeType componentType,
			final Object anything) {

		Object[] children = null;
		final Type[] types = componentType.getSubtypes();
		final String[] propertyNames = componentType.getPropertyNames();
		for ( int i = 0; i < types.length; i++ ) {
			final CascadeStyle componentPropertyStyle = componentType.getCascadeStyle( i );
			final String subPropertyName = propertyNames[i];
			if ( componentPropertyStyle.doCascade( action ) ) {
				if (children == null) {
					// Get children on demand.
					children = componentType.getPropertyValues( child, eventSource );
				}
				cascadeProperty(
						action,
						cascadePoint,
						eventSource,
						componentPathStackDepth + 1,
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

	private static void cascadeAssociation(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final Object anything,
			final boolean isCascadeDeleteEnabled) {
		if ( type.isEntityType() || type.isAnyType() ) {
			cascadeToOne( action, eventSource, parent, child, type, style, anything, isCascadeDeleteEnabled );
		}
		else if ( type.isCollectionType() ) {
			cascadeCollection(
					action,
					cascadePoint,
					eventSource,
					componentPathStackDepth,
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
	private static void cascadeCollection(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final CascadeStyle style,
			final Object anything,
			final CollectionType type) {
		final CollectionPersister persister = eventSource.getFactory().getCollectionPersister( type.getRole() );
		final Type elemType = persister.getElementType();

		CascadePoint elementsCascadePoint = cascadePoint;
		if ( cascadePoint == CascadePoint.AFTER_INSERT_BEFORE_DELETE ) {
			elementsCascadePoint = CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION;
		}

		//cascade to current collection elements
		if ( elemType.isEntityType() || elemType.isAnyType() || elemType.isComponentType() ) {
			cascadeCollectionElements(
				action,
				elementsCascadePoint,
				eventSource,
				componentPathStackDepth,
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
	private static void cascadeToOne(
			final CascadingAction action,
			final EventSource eventSource,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final Object anything,
			final boolean isCascadeDeleteEnabled) {
		final String entityName = type.isEntityType()
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
	private static void cascadeCollectionElements(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final CollectionType collectionType,
			final CascadeStyle style,
			final Type elemType,
			final Object anything,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		final boolean reallyDoCascade = style.reallyDoCascade( action ) && child != CollectionType.UNFETCHED_COLLECTION;

		if ( reallyDoCascade ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Cascade {0} for collection: {1}", action, collectionType.getRole() );
			}

			final Iterator itr = action.getCascadableChildrenIterator( eventSource, collectionType, child );
			while ( itr.hasNext() ) {
				cascadeProperty(
						action,
						cascadePoint,
						eventSource,
						componentPathStackDepth,
						parent,
						itr.next(),
						elemType,
						style,
						null,
						anything,
						isCascadeDeleteEnabled
				);
			}

			if ( traceEnabled ) {
				LOG.tracev( "Done cascade {0} for collection: {1}", action, collectionType.getRole() );
			}
		}

		final boolean deleteOrphans = style.hasOrphanDelete()
				&& action.deleteOrphans()
				&& elemType.isEntityType()
				// a newly instantiated collection can't have orphans
				&& child instanceof PersistentCollection;

		if ( deleteOrphans ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Deleting orphans for collection: {0}", collectionType.getRole() );
			}
			// we can do the cast since orphan-delete does not apply to:
			// 1. newly instantiated collections
			// 2. arrays (we can't track orphans for detached arrays)
			final String entityName = collectionType.getAssociatedEntityName( eventSource.getFactory() );
			deleteOrphans( eventSource, entityName, (PersistentCollection) child );

			if ( traceEnabled ) {
				LOG.tracev( "Done deleting orphans for collection: {0}", collectionType.getRole() );
			}
		}
	}

	/**
	 * Delete any entities that were removed from the collection
	 */
	private static void deleteOrphans(EventSource eventSource, String entityName, PersistentCollection pc) throws HibernateException {
		//TODO: suck this logic into the collection!
		final Collection orphans;
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
				eventSource.delete( entityName, orphan, false, new HashSet() );
			}
		}
	}
}
