/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.JoinablePersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

import static org.hibernate.type.ForeignKeyDirection.TO_PARENT;

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
	 * @param descriptor The parent's entity descriptor
	 * @param parent The parent reference.
	 * @throws HibernateException
	 */
	public static void cascade(
			final CascadingAction action, final CascadePoint cascadePoint,
			final EventSource eventSource, final EntityTypeDescriptor descriptor, final Object parent)
			throws HibernateException {
		cascade( action, cascadePoint, eventSource, descriptor, parent, null );
	}

	/**
	 * Cascade an action from the parent entity instance to all its children.  This
	 * form is typically called from within cascade actions.
	 *
	 * @param descriptor The parent's entity descriptor
	 * @param parent The parent reference.
	 * @param anything Anything ;)   Typically some form of cascade-local cache
	 * which is specific to each CascadingAction type
	 */
	public static void cascade(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final EntityTypeDescriptor descriptor,
			final Object parent,
			final Object anything) throws HibernateException {

		if ( descriptor.hasCascades() || action.requiresNoCascadeChecking() ) { // performance opt
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Processing cascade {0} for: {1}", action, descriptor.getEntityName() );
			}

			final List<NonIdPersistentAttribute> persistentAttributes = descriptor.getPersistentAttributes();
			final boolean hasUninitializedLazyProperties = descriptor.hasUninitializedLazyProperties( parent );
			final int componentPathStackDepth = 0;
			for ( int i = 0; i < persistentAttributes.size(); i++) {
				final NonIdPersistentAttribute attribute = persistentAttributes.get( i );
				final CascadeStyle style = attribute.getCascadeStyle();

				final boolean isUninitializedProperty =
						hasUninitializedLazyProperties &&
						!descriptor.getBytecodeEnhancementMetadata().isAttributeLoaded( parent, attribute.getName() );

				if ( style.doCascade( action ) ) {
					final Object child;

					// 6.0 code

					if ( isUninitializedProperty  ) {
						// parent is a bytecode enhanced entity.
						// cascading to an uninitialized, lazy value.
						if ( attribute.isCollection() ) {
							// The collection does not need to be loaded from the DB.
							// CollectionType#resolve will return an uninitialized PersistentCollection.
							// The action will initialize the collection later, if necessary.
//							child = types[ i ].resolve( LazyPropertyInitializer.UNFETCHED_PROPERTY, eventSource, parent );
							// TODO: it would be nice to be able to set the attribute in parent using
							throw new NotYetImplementedFor6Exception("cascade uninitialized Collection Property");
							// persister.setPropertyValue( parent, i, child ).
							// Unfortunately, that would cause the uninitialized collection to be
							// loaded from the DB.
						}
						else if ( action.performOnLazyProperty() ) {
							// The (non-collection) attribute needs to be initialized so that
							// the action can be performed on the initialized attribute.
							LazyAttributeLoadingInterceptor interceptor = descriptor.getBytecodeEnhancementMetadata().extractInterceptor( parent );
							child = interceptor.fetchAttribute( parent, attribute.getName() );
						}
						else {
							// Nothing to do, so just skip cascading to this lazy (non-collection) attribute.
							continue;
						}
					}
					else {
						child = descriptor.getPropertyValue( parent, i );
					}
					cascadeProperty(
							action,
							cascadePoint,
							eventSource,
							componentPathStackDepth,
							parent,
							child,
							attribute,
							style,
							attribute.getName(),
							anything,
							false
					);
				}
				else {
					if ( action.requiresNoCascadeChecking() ) {
						action.noCascade(
								eventSource,
								parent,
								descriptor,
								attribute,
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
								attribute.getPropertyAccess().getGetter().get( parent ),
								attribute,
								style,
								attribute.getName(),
								false
						);
					}
				}
			}

			if ( traceEnabled ) {
				LOG.tracev( "Done processing cascade {0} for: {1}", action, descriptor.getEntityName() );
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
			final PersistentAttributeDescriptor attribute,
			final CascadeStyle style,
			final String propertyName,
			final Object anything,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		if ( child != null ) {
			if ( JoinablePersistentAttribute.class.isInstance( attribute ) ) {
				if ( cascadeAssociationNow( cascadePoint, (JoinablePersistentAttribute) attribute ) ) {
					cascadeAssociation(
							action,
							cascadePoint,
							eventSource,
							componentPathStackDepth,
							parent,
							child,
							attribute,
							style,
							anything,
							isCascadeDeleteEnabled
						);
				}
			}
			else if ( attribute.getPersistenceType() == PersistenceType.EMBEDDABLE ) {
				cascadeComponent(
						action,
						cascadePoint,
						eventSource,
						componentPathStackDepth,
						parent,
						child,
						(EmbeddedValuedNavigable) attribute,
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
				attribute,
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
			final PersistentAttributeDescriptor attribute,
			final CascadeStyle style,
			final String propertyName,
			final boolean isCascadeDeleteEnabled) throws HibernateException {

		// potentially we need to handle orphan deletes for one-to-ones here...
		if ( isLogicalOneToOne( attribute ) ) {
			// We have a physical or logical one-to-one.  See if the attribute cascade settings and action-type require
			// orphan checking
			if ( style.hasOrphanDelete() && action.deleteOrphans() ) {
				// value is orphaned if loaded state for this property shows not null
				// because it is currently null.
				final EntityEntry entry = eventSource.getPersistenceContext().getEntry( parent );
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
//							final String getNavigablePath = composePropertyPath( entityType.getPropertyName() );
						loadedValue = null;
					}

					// orphaned if the association was nulled (child == null) or receives a new value while the
					// entity is managed (without first nulling and manually flushing).
					if ( child == null || ( loadedValue != null && child != loadedValue ) ) {
						EntityEntry valueEntry = eventSource
								.getPersistenceContext().getEntry(
										loadedValue );

						if ( valueEntry == null && loadedValue instanceof HibernateProxy ) {
							// un-proxy and re-associate for cascade operation
							// useful for @OneToOne defined as FetchType.LAZY
							loadedValue = eventSource.getPersistenceContext().unproxyAndReassociate( loadedValue );
							valueEntry = eventSource.getPersistenceContext().getEntry( loadedValue );

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
							final String entityName = valueEntry.getDescriptor().getEntityName();
							if ( LOG.isTraceEnabled() ) {
								final Object id = valueEntry.getDescriptor().getIdentifier( loadedValue, eventSource );
								final String description = MessageHelper.infoString( entityName, id );
								LOG.tracev( "Deleting orphaned entity instance: {0}", description );
							}
							
							if (attribute instanceof Joinable
									&& ( (Joinable) attribute).getForeignKeyDirection().equals(
											TO_PARENT
							)) {
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
	 * @param attribute The persistent attribute metadata
	 *
	 * @return True if the attribute represents a logical one to one association
	 */
	private static boolean isLogicalOneToOne(PersistentAttributeDescriptor attribute) {
		return attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE;
	}

	private static boolean cascadeAssociationNow(final CascadePoint cascadePoint, JoinablePersistentAttribute attribute) {
		return attribute.getForeignKeyDirection().cascadeNow( cascadePoint );
	}

	private static void cascadeComponent(
			final CascadingAction action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final int componentPathStackDepth,
			final Object parent,
			final Object child,
			final EmbeddedValuedNavigable attribute,
			final Object anything) {

		Object[] children = null;
		final EmbeddedTypeDescriptor embeddedDescriptor = attribute.getEmbeddedDescriptor();
		final List<PersistentAttributeDescriptor> attributes = embeddedDescriptor.getPersistentAttributes();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final PersistentAttributeDescriptor subattribute = attributes.get( i );
			final CascadeStyle componentPropertyStyle = embeddedDescriptor.getCascadeStyle( i );
			final String subPropertyName = subattribute.getName();
			if ( componentPropertyStyle.doCascade( action ) ) {
				if ( children == null ) {
					// Get children on demand.
					children = embeddedDescriptor.getPropertyValues( child );
				}
				cascadeProperty(
						action,
						cascadePoint,
						eventSource,
						componentPathStackDepth + 1,
						parent,
						children[i],
						subattribute,
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
			final PersistentAttributeDescriptor attribute,
			final CascadeStyle style,
			final Object anything,
			final boolean isCascadeDeleteEnabled) {
		if ( SingularPersistentAttributeEntity.class.isInstance( attribute ) ) {
			cascadeToOne( action, eventSource, parent, child, attribute, style, anything, isCascadeDeleteEnabled );
		}
		else if ( PluralPersistentAttribute.class.isInstance( attribute ) ) {
			cascadeCollection(
					action,

					cascadePoint,
					eventSource,
					componentPathStackDepth,
					parent,
					child,
					style,
					anything,
					(PluralPersistentAttribute) attribute
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
			final PluralPersistentAttribute attribute) {
		final CollectionElement collectionElement = attribute.getPersistentCollectionDescriptor().getElementDescriptor();
		final PersistentCollectionDescriptor descriptor = eventSource.getFactory()
				.getMetamodel()
				.findCollectionDescriptor( attribute.getNavigableName() );

		CascadePoint elementsCascadePoint = cascadePoint;
		if ( cascadePoint == CascadePoint.AFTER_INSERT_BEFORE_DELETE ) {
			elementsCascadePoint = CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION;
		}

		//cascade to current collection elements
		if ( collectionElement.getClassification() != CollectionElement.ElementClassification.BASIC ) {
			cascadeCollectionElements(
				action,
				elementsCascadePoint,
				eventSource,
				componentPathStackDepth,
				parent,
				child,
				attribute,
				style,
				collectionElement,
				anything,
				descriptor
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
			final PersistentAttributeDescriptor persistentAttribute,
			final CascadeStyle style,
			final Object anything,
			final boolean isCascadeDeleteEnabled) {
		final String entityName = persistentAttribute.getPersistenceType() == PersistenceType.ENTITY
				? ( (SingularPersistentAttributeEntity) persistentAttribute ).getEntityName()
				: null;
		if ( style.reallyDoCascade( action ) ) {
			//not really necessary, but good for consistency...
			eventSource.getPersistenceContext().addChildParent( child, parent );
			try {
				action.cascade( eventSource, child, entityName, anything, isCascadeDeleteEnabled );
			}
			finally {
				eventSource.getPersistenceContext().removeChildParent( child );
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
			final PluralPersistentAttribute attribute,
			final CascadeStyle style,
			final CollectionElement collectionElement,
			final Object anything,
			final PersistentCollectionDescriptor descriptor) throws HibernateException {
		final boolean reallyDoCascade = style.reallyDoCascade( action ) && child != PersistentCollectionDescriptor.UNFETCHED_COLLECTION;

		if ( reallyDoCascade ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Cascade {0} for collection: {1}", action, attribute.getNavigableName() );
			}

			final Iterator itr = action.getCascadableChildrenIterator( eventSource, descriptor, child );
			while ( itr.hasNext() ) {
				cascadeProperty(
						action,
						cascadePoint,
						eventSource,
						componentPathStackDepth,
						parent,
						itr.next(),
						attribute,
						style,
						null,
						anything,
						descriptor.getDescribedAttribute().isCascadeDeleteEnabled()
				);
			}

			if ( traceEnabled ) {
				LOG.tracev( "Done cascade {0} for collection: {1}", action, attribute.getNavigableName() );
			}
		}

		final boolean deleteOrphans = style.hasOrphanDelete()
				&& action.deleteOrphans()
				&& ( collectionElement.getJavaTypeDescriptor() instanceof EntityJavaDescriptor)
				// a newly instantiated collection can't have orphans
				&& child instanceof PersistentCollection;

		if ( deleteOrphans ) {
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( traceEnabled ) {
				LOG.tracev( "Deleting orphans for collection: {0}", attribute.getNavigableName() );
			}
			// we can do the cast since orphan-delete does not apply to:
			// 1. newly instantiated collections
			// 2. arrays (we can't track orphans for detached arrays)
			final String entityName = ((EntityJavaDescriptor)collectionElement.getJavaTypeDescriptor()).getEntityName();
			deleteOrphans( eventSource, entityName, (PersistentCollection) child );

			if ( traceEnabled ) {
				LOG.tracev( "Done deleting orphans for collection: {0}", attribute.getNavigableName()  );
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
			final CollectionEntry ce = eventSource.getPersistenceContext().getCollectionEntry( pc );
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
