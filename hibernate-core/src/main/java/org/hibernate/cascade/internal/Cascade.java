/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.cascade.spi.CascadePropertySelection;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.cascade.spi.PropertySelectionKind;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.SemanticCollectionChange;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyIterator;
import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Delegate responsible for, in conjunction with the various
 * {@linkplain CascadingAction actions}, implementing cascade processing.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @see CascadingAction
 */
public final class Cascade {
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
		if ( !isBuiltInAction( action ) ) {
			throw new IllegalArgumentException(
					"Unsupported cascading action implementation: " + action.getClass().getName()
			);
		}
		cascade( new CascadeTraversalContext<>( action, cascadePoint, eventSource, persister, parent, anything ) );
	}

	private static boolean isBuiltInAction(CascadingAction<?> action) {
		return action == CascadingActions.REMOVE
				|| action == CascadingActions.REFRESH
				|| action == CascadingActions.EVICT
				|| action == CascadingActions.MERGE
				|| action == CascadingActions.PERSIST
				|| action == CascadingActions.PERSIST_ON_FLUSH
				|| action == CascadingActions.CHECK_ON_FLUSH;
	}

	static <T> void cascade(CascadeTraversalContext<T> context) throws HibernateException {
		CascadeWalker.instance().traverse( context );
	}

	static <T> void cascade(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final EntityPersister persister,
			final Object parent,
			final T anything,
			final CascadeDecisionTrace decisionTrace) throws HibernateException {
		cascade(
				action,
				cascadePoint,
				eventSource,
				persister,
				parent,
				anything,
				decisionTrace,
				CascadeEffectMode.EXECUTE
		);
	}

	static <T> void cascade(
			final CascadingAction<T> action,
			final CascadePoint cascadePoint,
			final EventSource eventSource,
			final EntityPersister persister,
			final Object parent,
			final T anything,
			final CascadeDecisionTrace decisionTrace,
			final CascadeEffectMode effectMode) throws HibernateException {
		CascadeWalker.instance().traverse(
				new CascadeTraversalContext<>(
						action,
						cascadePoint,
						eventSource,
						persister,
						parent,
						anything,
						decisionTrace,
						effectMode
				)
		);
	}

	static <T> void traverseMetadata(CascadeTraversalContext<T> context) throws HibernateException {
		traverseMetadata( context, null );
	}

	static <T> void traverseMetadata(
			CascadeTraversalContext<T> context,
			CascadePropertySelection propertySelectionOverride) throws HibernateException {
		final var action = context.action();
		final var persister = context.rootPersister();
		final var eventSource = context.session();
		final var parent = context.root();
		final boolean anythingToCascade = action.anythingToCascade( persister );
		if ( context.traceEnabled() ) {
			context.trace( new CascadeTraceEvent.Root(
					context.rootTraceLocation(),
					anythingToCascade
							? CascadeTraceEvent.RootDecision.TRAVERSE
							: CascadeTraceEvent.RootDecision.NOTHING_TO_CASCADE
			) );
		}
		if ( anythingToCascade ) { // performance opt
			final boolean traceEnabled = CORE_LOGGER.isTraceEnabled();
			if ( traceEnabled ) {
				CORE_LOGGER.processingCascade( action, persister.getEntityName() );
			}
			final var bytecodeEnhancement = persister.getBytecodeEnhancementMetadata();
			final EntityEntry entry;
			if ( bytecodeEnhancement.isEnhancedForLazyLoading() ) {
				entry = eventSource.getPersistenceContextInternal().getEntry( parent );
				if ( entry != null
						&& entry.getLoadedState() == null
						&& entry.getStatus() == Status.MANAGED ) {
					if ( context.traceEnabled() ) {
						context.trace( new CascadeTraceEvent.Root(
								context.rootTraceLocation(),
								CascadeTraceEvent.RootDecision.MANAGED_WITHOUT_LOADED_STATE
						) );
					}
					return;
				}
			}
			else {
				entry = null;
			}

			final Type[] types = persister.getPropertyTypes();
			final String[] propertyNames = persister.getPropertyNames();
			final var cascadeStyles = persister.getPropertyCascadeStyles();
			final boolean hasUninitializedLazyProperties = bytecodeEnhancement.hasUnFetchedAttributes( parent );
			final var propertySelection = determinePropertySelection(
					context,
					persister,
					propertySelectionOverride
			);
			final var propertySelectionKind = propertySelection.getKind();
			final boolean selectedProperties =
					propertySelectionKind == PropertySelectionKind.SELECTED;
			final int numberOfProperties = switch ( propertySelectionKind ) {
				case NONE -> 0;
				case ALL -> types.length;
				case SELECTED -> propertySelection.getSelectedPropertyCount();
			};

			for ( int selectionPosition = 0; selectionPosition < numberOfProperties; selectionPosition++ ) {
				final int i = selectedProperties
						? propertySelection.getSelectedProperty( selectionPosition )
						: selectionPosition;
				final var style = cascadeStyles[ i ];
				final String propertyName = propertyNames[ i ];
				final Type type = types[i];
				final boolean isUninitializedProperty =
						hasUninitializedLazyProperties
								&& !bytecodeEnhancement.isAttributeLoaded( parent, propertyName );
				final boolean isCascadeDeleteEnabled = cascadeDeleteEnabled( action, persister, i );
				final boolean applies = action.appliesTo( type, style );
				if ( context.traceEnabled() ) {
					context.trace( new CascadeTraceEvent.Node(
							context.traceLocation( type, style, List.of( propertyName ) ),
							applies,
							isCascadeDeleteEnabled
					) );
				}

				if ( applies ) {
					final Object child;
					if ( isUninitializedProperty ) {
						assert bytecodeEnhancement.isEnhancedForLazyLoading();
						// Hibernate does not support lazy embeddables
						assert !type.isComponentType();
						// parent is a bytecode enhanced entity.
						// Cascade to an uninitialized, lazy value only if
						// parent is managed in the PersistenceContext.
						// If parent is a detached entity being merged,
						// then parent will not be in the PersistenceContext
						// (so lazy attributes must not be initialized).
						if ( entry == null ) {
							// parent was not in the PersistenceContext
							traceLazy(
									context,
									type,
									style,
									propertyName,
									CascadeTraceEvent.LazyDecision.DETACHED_PARENT_SKIP
							);
							continue;
						}
						else if ( type instanceof CollectionType collectionType ) {
							// CollectionType.getCollection() gets the PersistentCollection
							// that corresponds to the uninitialized collection from the
							// PersistenceContext. If not present, an uninitialized
							// PersistentCollection will be added to the PersistenceContext.
							// The action may initialize it later, if necessary.
							// This needs to be done even when action.performOnLazyProperty()
							// returns false.
							child = collectionType.getCollection(
									collectionType.getKeyOfOwner( parent, eventSource ),
									eventSource,
									parent,
									null
							);
							traceLazy(
									context,
									type,
									style,
									propertyName,
									CascadeTraceEvent.LazyDecision.COLLECTION_WRAPPER
							);
							traceValue(
									context,
									type,
									style,
									propertyName,
									CascadeTraceEvent.ValueResolution.LAZY_COLLECTION_WRAPPER
							);
						}
						else if ( action.performOnLazyProperty() && type instanceof EntityType ) {
							// Only need to initialize a lazy entity attribute when
							// action.performOnLazyProperty() returns true.
							child = bytecodeEnhancement.extractInterceptor( parent )
									.fetchAttribute( parent, propertyName );
							traceLazy(
									context,
									type,
									style,
									propertyName,
									CascadeTraceEvent.LazyDecision.TO_ONE_FETCH
							);
							traceValue(
									context,
									type,
									style,
									propertyName,
									CascadeTraceEvent.ValueResolution.LAZY_TO_ONE_FETCH
							);

						}
						else {
							// Nothing to do, so just skip cascading to this lazy attribute.
							traceLazy(
									context,
									type,
									style,
									propertyName,
									CascadeTraceEvent.LazyDecision.ACTION_SKIP
							);
							continue;
						}
					}
					else {
						traceLazy(
								context,
								type,
								style,
								propertyName,
								CascadeTraceEvent.LazyDecision.LOADED
						);
						child = persister.getValue( parent, i );
						traceValue(
								context,
								type,
								style,
								propertyName,
								CascadeTraceEvent.ValueResolution.ENTITY_PROPERTY
						);
					}
					cascadeProperty(
							context,
							i,
							parent,
							child,
							type,
							style,
							propertyName,
							isCascadeDeleteEnabled
					);
				}
				else if ( action.deleteOrphans()
						// If the property is uninitialized, there cannot be any orphans.
						&& !isUninitializedProperty
						&& isLogicalOneToOne( type ) ) {
					final var previousParent = context.currentParent();
					final var previousPropertyName = context.currentPropertyName();
					final var previousPropertyType = context.currentPropertyType();
					final var previousCascadeStyle = context.currentCascadeStyle();
					final int previousPropertyIndex = context.currentPropertyIndex();
					context.setCurrentProperty( parent, propertyName, type, style, i );
					try {
						traceValue(
								context,
								type,
								style,
								propertyName,
								CascadeTraceEvent.ValueResolution.ENTITY_PROPERTY
						);
						cascadeLogicalOneToOneOrphanRemoval(
								context,
								persister.getValue( parent, i ),
								isCascadeDeleteEnabled
						);
					}
					finally {
						context.setCurrentProperty(
								previousParent,
								previousPropertyName,
								previousPropertyType,
								previousCascadeStyle,
								previousPropertyIndex
						);
					}
				}
			}

			if ( traceEnabled ) {
				CORE_LOGGER.doneProcessingCascade( action, persister.getEntityName() );
			}
		}
	}

	private static CascadePropertySelection determinePropertySelection(
			CascadeTraversalContext<?> context,
			EntityPersister persister,
			CascadePropertySelection propertySelectionOverride) {
		if ( context.traceEnabled() ) {
			return CascadePropertySelection.all();
		}
		if ( propertySelectionOverride != null ) {
			return propertySelectionOverride;
		}
		return persister instanceof AbstractEntityPersister abstractPersister
				? abstractPersister.getCascadePropertySelection( context.action() )
				: CascadePropertySelection.all();
	}

	static <T> void traceLazy(
			CascadeTraversalContext<T> context,
			Type type,
			CascadeStyle style,
			String propertyName,
			CascadeTraceEvent.LazyDecision decision) {
		if ( context.traceEnabled() ) {
			context.trace( new CascadeTraceEvent.Lazy(
					context.traceLocation( type, style, List.of( propertyName ) ),
					decision
			) );
		}
	}

	static <T> void traceValue(
			CascadeTraversalContext<T> context,
			Type type,
			CascadeStyle style,
			String propertyName,
			CascadeTraceEvent.ValueResolution resolution) {
		if ( context.traceEnabled() ) {
			context.trace( new CascadeTraceEvent.Value(
					context.traceLocation( type, style, List.of( propertyName ) ),
					resolution
			) );
		}
	}

	private static void traceOrphan(
			CascadeTraversalContext<?> context,
			CascadeTraceEvent.OrphanEligibility eligibility,
			CascadeTraceEvent.OrphanSource source,
			CascadeTraceEvent.OrphanTiming timing) {
		if ( context.traceEnabled() ) {
			context.trace( new CascadeTraceEvent.Orphan(
					context.currentTraceLocation(),
					eligibility,
					source,
					timing
			) );
		}
	}

	/**
	 * Cascade an action to the child or children
	 */
	private static <T> void cascadeProperty(
			final CascadeTraversalContext<T> context,
			final int propertyIndex,
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final String propertyName,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		final var previousParent = context.currentParent();
		final var previousPropertyName = context.currentPropertyName();
		final var previousPropertyType = context.currentPropertyType();
		final var previousCascadeStyle = context.currentCascadeStyle();
		final int previousPropertyIndex = context.currentPropertyIndex();
		context.setCurrentProperty( parent, propertyName, type, style, propertyIndex );
		try {
			cascadePropertyValue( context, child, isCascadeDeleteEnabled );
		}
		catch (RuntimeException | Error failure) {
			context.traceFailure( failure );
			throw failure;
		}
		finally {
			context.setCurrentProperty(
					previousParent,
					previousPropertyName,
					previousPropertyType,
					previousCascadeStyle,
					previousPropertyIndex
			);
		}
	}

	private static <T> void cascadePropertyValue(
			final CascadeTraversalContext<T> context,
			final Object child,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		final var action = context.action();
		final var cascadePoint = context.cascadePoint();
		final var eventSource = context.session();
		final var type = context.currentPropertyType();

		if ( child != null ) {
			if ( type instanceof EntityType || type instanceof CollectionType || type instanceof AnyType ) {
				final boolean cascadeNow = action.cascadeNow(
						cascadePoint,
						(AssociationType) type,
						eventSource.getFactory()
				);
				traceAssociation( context, cascadeNow );
				if ( cascadeNow ) {
					cascadeAssociation( context, child, isCascadeDeleteEnabled );
				}
			}
			else if ( type instanceof ComponentType componentType ) {
				final var propertyName = context.currentPropertyName();
				if ( context.path() == null && propertyName == null ) {
					cascadeComponent( context, child, componentType );
				}
				else {
					context.pushPath( propertyName );
					try {
						cascadeComponent( context, child, componentType );
					}
					finally {
						context.popPath();
					}
				}
			}
		}
		if ( isLogicalOneToOne( type ) ) {
			cascadeLogicalOneToOneOrphanRemoval(
					context,
					child,
					isCascadeDeleteEnabled
			);
		}
	}

	private static void traceAssociation(CascadeTraversalContext<?> context, boolean cascadeNow) {
		if ( context.traceEnabled() ) {
			context.trace( new CascadeTraceEvent.Association(
					context.currentTraceLocation(),
					cascadeNow
			) );
		}
	}

	/** potentially we need to handle orphan deletes for one-to-ones here...*/
	static <T> void cascadeLogicalOneToOneOrphanRemoval(
			final CascadeTraversalContext<T> context,
			final Object child,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		final var action = context.action();
		final var eventSource = context.session();
		final var componentPath = context.path();
		final var parent = context.currentParent();
		final var type = context.currentPropertyType();
		final var style = context.currentCascadeStyle();
		final var propertyName = context.currentPropertyName();
		final boolean eligible = style.hasOrphanDelete() && action.deleteOrphans();
		traceOrphan(
				context,
				eligible
						? CascadeTraceEvent.OrphanEligibility.ELIGIBLE
						: CascadeTraceEvent.OrphanEligibility.INELIGIBLE,
				CascadeTraceEvent.OrphanSource.NONE,
				CascadeTraceEvent.OrphanTiming.NONE
		);

		// We have a physical or logical one-to-one.  See if the attribute cascade settings and action-type require
		// orphan checking
		if ( eligible ) {
			// value is orphaned if loaded state for this property shows not null
			// because it is currently null.
			final var persistenceContext = eventSource.getPersistenceContextInternal();
			final var entry = persistenceContext.getEntry( parent );
			if ( entry != null && entry.getStatus() != Status.SAVING ) {
				if ( context.traceEnabled() ) {
					context.trace( new CascadeTraceEvent.Value(
							context.currentTraceLocation(),
							CascadeTraceEvent.ValueResolution.LOADED_STATE
					) );
				}
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
					if ( propertyType instanceof ComponentType componentType ) {
						loadedValue = entry.getLoadedValue( componentPath.get( 0 ) );
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
						// unproxy and reassociate for cascade operation
						// useful for @OneToOne defined as FetchType.LAZY
						//TODO: what should really happen here???
						loadedValue = persistenceContext.unproxyAndReassociate( loadedValue );
						valueEntry = persistenceContext.getEntry( loadedValue );
						// HHH-11965
						// Should the unwrapped proxy value be equal via reference to the entity's property value
						// provided by the 'child' variable, we should not trigger the orphan removal of the
						// associated one-to-one.
						if ( child == loadedValue ) {
							// do nothing
							traceOrphan(
									context,
									CascadeTraceEvent.OrphanEligibility.NOT_ORPHANED,
									CascadeTraceEvent.OrphanSource.ENTITY_LOADED_STATE,
									CascadeTraceEvent.OrphanTiming.NONE
							);
							return;
						}
					}

					if ( valueEntry != null ) {
						traceOrphan(
								context,
								CascadeTraceEvent.OrphanEligibility.ORPHANED,
								CascadeTraceEvent.OrphanSource.ENTITY_LOADED_STATE,
								isForeignKeyToParent( type )
										? CascadeTraceEvent.OrphanTiming.BEFORE_UPDATES
										: CascadeTraceEvent.OrphanTiming.AFTER_UPDATES
						);
						if ( context.executeEffects() ) {
							final var persister = valueEntry.getPersister();
							final String entityName = persister.getEntityName();
							if ( CORE_LOGGER.isTraceEnabled() ) {
								CORE_LOGGER.deletingOrphan(
										infoString( entityName, persister.getIdentifier( loadedValue, eventSource ) ) );
							}
							if ( isForeignKeyToParent( type ) ) {
								// If FK direction is to-parent, we must remove the orphan *before* the queued update(s)
								// occur. Otherwise, replacing the association on a managed entity, without manually
								// nulling and flushing, causes FK constraint violations.
								eventSource.removeOrphanBeforeUpdates( entityName, loadedValue );
							}
							else {
								// Else, we must delete after the updates.
								eventSource.delete(
										entityName,
										loadedValue,
										isCascadeDeleteEnabled,
										DeleteContext.create()
								);
							}
						}
					}
					else {
						traceOrphan(
								context,
								CascadeTraceEvent.OrphanEligibility.ORPHANED,
								CascadeTraceEvent.OrphanSource.ENTITY_LOADED_STATE,
								CascadeTraceEvent.OrphanTiming.NONE
						);
					}
				}
				else {
					traceOrphan(
							context,
							CascadeTraceEvent.OrphanEligibility.NOT_ORPHANED,
							CascadeTraceEvent.OrphanSource.ENTITY_LOADED_STATE,
							CascadeTraceEvent.OrphanTiming.NONE
					);
				}
			}
		}
	}

	private static boolean isForeignKeyToParent(Type type) {
		return type instanceof CollectionType
			|| type instanceof OneToOneType oneToOneType
				&& oneToOneType.getForeignKeyDirection() == ForeignKeyDirection.TO_PARENT;
	}

	/**
	 * Check if the association is a one to one in the logical model (either a shared-pk
	 * or unique fk).
	 *
	 * @param type The type representing the attribute metadata
	 *
	 * @return True if the attribute represents a logical one-to-one association
	 */
	private static boolean isLogicalOneToOne(Type type) {
		return type instanceof EntityType entityType
			&& entityType.isLogicalOneToOne();
	}

	private static <T> void cascadeComponent(
			final CascadeTraversalContext<T> context,
			final Object child,
			final CompositeType componentType) {
		final var action = context.action();
		final var eventSource = context.session();
		Object[] children = null;
		final Type[] types = componentType.getSubtypes();
		final String[] propertyNames = componentType.getPropertyNames();
		for ( int i = 0; i < types.length; i++ ) {
			final CascadeStyle componentPropertyStyle = componentType.getCascadeStyle( i );
			final String subPropertyName = propertyNames[i];
			final Type subPropertyType = types[i];
			final boolean applies = action.appliesTo( subPropertyType, componentPropertyStyle );
			final boolean cascadeDeleteEnabled = cascadeDeleteEnabled( action, componentType, i );
			if ( context.traceEnabled() ) {
				context.trace( new CascadeTraceEvent.Node(
						context.childTraceLocation( subPropertyType, componentPropertyStyle, subPropertyName ),
						applies,
						cascadeDeleteEnabled
				) );
			}
			if ( applies
					|| componentPropertyStyle.hasOrphanDelete() && action.deleteOrphans() ) {
				if ( children == null ) {
					// Get children on demand.
					children = componentType.getPropertyValues( child, eventSource );
					if ( context.traceEnabled() ) {
						context.trace( new CascadeTraceEvent.Value(
								context.currentTraceLocation(),
								CascadeTraceEvent.ValueResolution.COMPONENT_PROPERTIES
						) );
					}
				}
				if ( context.traceEnabled() ) {
					context.trace( new CascadeTraceEvent.Value(
							context.childTraceLocation( subPropertyType, componentPropertyStyle, subPropertyName ),
							CascadeTraceEvent.ValueResolution.COMPONENT_PROPERTY
					) );
				}
				cascadeProperty(
						context,
						i,
						context.currentParent(),
						children[i],
						subPropertyType,
						componentPropertyStyle,
						subPropertyName,
						cascadeDeleteEnabled
				);
			}
		}
	}

	private static <T> void cascadeAssociation(
			final CascadeTraversalContext<T> context,
			final Object child,
			final boolean isCascadeDeleteEnabled) {
		final var type = context.currentPropertyType();
		if ( type instanceof EntityType || type instanceof AnyType ) {
			cascadeToOne( context, child, isCascadeDeleteEnabled );
		}
		else if ( type instanceof CollectionType collectionType ) {
			cascadeCollection( context, child, collectionType );
		}
	}

	/**
	 * Cascade an action to a collection
	 */
	private static <T> void cascadeCollection(
			final CascadeTraversalContext<T> context,
			final Object child,
			final CollectionType type) {
		final var action = context.action();
		final var eventSource = context.session();
		final var persister =
				eventSource.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );
		final Type elemType = persister.getElementType();
		final boolean cascadeDeleteEnabled = cascadeDeleteEnabled( action, persister );
		//cascade to current collection elements
		if ( elemType instanceof EntityType || elemType instanceof AnyType || elemType instanceof ComponentType ) {
			cascadeCollectionElements(
					context,
					child,
					type,
					elemType,
					cascadeDeleteEnabled
			);
		}
	}

	/**
	 * Cascade an action to a to-one association or any type
	 */
	private static <T> void cascadeToOne(
			final CascadeTraversalContext<T> context,
			final Object child,
			final boolean isCascadeDeleteEnabled) {
		final var action = context.action();
		final var eventSource = context.session();
		final var parent = context.currentParent();
		final var type = context.currentPropertyType();
		final var style = context.currentCascadeStyle();
		final boolean reallyDoCascade = style.reallyDoCascade( action );
		if ( context.traceEnabled() ) {
			context.trace( new CascadeTraceEvent.Action(
					context.currentTraceLocation(),
					reallyDoCascade
							? context.executeEffects()
									? CascadeTraceEvent.ActionDecision.INVOKED
									: CascadeTraceEvent.ActionDecision.SUPPRESSED_BY_DECISION_ONLY
							: CascadeTraceEvent.ActionDecision.SKIPPED_BY_STYLE,
					isCascadeDeleteEnabled
			) );
		}
		if ( reallyDoCascade && context.executeEffects() ) {
			//not really necessary, but good for consistency...
			final var persistenceContext = eventSource.getPersistenceContextInternal();
			persistenceContext.addChildParent( child, parent );
			final String childEntityName =
					type instanceof EntityType entityType
							? entityType.getAssociatedEntityName()
							: null;
			CORE_LOGGER.cascading( action, childEntityName );
			try {
				action.cascade(
						eventSource,
						child,
						childEntityName,
						context.rootEntityName(),
						context.currentPropertyName(),
						context.path(),
						context.actionContext(),
						isCascadeDeleteEnabled
				);
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
			final CascadeTraversalContext<T> context,
			final Object child,
			final CollectionType collectionType,
			final Type elemType,
			final boolean isCascadeDeleteEnabled) throws HibernateException {
		final var action = context.action();
		final var eventSource = context.session();
		final var parent = context.currentParent();
		final var propertyName = context.currentPropertyName();
		final var style = context.currentCascadeStyle();
		final var originalCascadePoint = context.cascadePoint();
		final var collectionCascadePoint =
				originalCascadePoint == CascadePoint.AFTER_INSERT_BEFORE_DELETE
						? CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION
						: originalCascadePoint;

		context.changeCascadePoint( collectionCascadePoint );
		try {
			final boolean reallyDoCascade = style.reallyDoCascade( action );
			if ( context.traceEnabled() ) {
				context.trace( new CascadeTraceEvent.Style(
						context.currentTraceLocation(),
						reallyDoCascade
				) );
			}
			if ( reallyDoCascade ) {
				final boolean traceEnabled = CORE_LOGGER.isTraceEnabled();
				if ( traceEnabled ) {
					CORE_LOGGER.cascadingCollection( action, collectionType.getRole() );
				}
				final var iterator = action.getCascadableChildrenIterator( eventSource, collectionType, child );
				if ( context.traceEnabled() ) {
					context.trace( new CascadeTraceEvent.CollectionIterator(
							context.currentTraceLocation(),
							collectionType.getRole(),
							collectionIteratorMode( action, iterator )
					) );
					context.trace( new CascadeTraceEvent.Value(
							context.currentTraceLocation(),
							CascadeTraceEvent.ValueResolution.COLLECTION_ITERATOR
					) );
				}
				while ( iterator.hasNext() ) {
					if ( context.traceEnabled() ) {
						context.trace( new CascadeTraceEvent.Node(
								context.currentPathTraceLocation( elemType, style ),
								true,
								isCascadeDeleteEnabled
						) );
					}
					cascadeProperty(
							context,
							-1,
							parent,
							iterator.next(),
							elemType,
							style,
						propertyName,
						isCascadeDeleteEnabled
				);
				}
				if ( traceEnabled ) {
					CORE_LOGGER.doneCascadingCollection( action, collectionType.getRole() );
				}
			}
		}
		finally {
			context.changeCascadePoint( originalCascadePoint );
		}

		// a newly instantiated collection can't have orphans
		final var persistentCollection =
				child instanceof PersistentCollection<?> collection
						? collection
						: eventSource.getPersistenceContextInternal()
								.getCollectionHolder( child );

		final boolean deleteOrphans = style.hasOrphanDelete()
				&& action.deleteOrphans()
				&& elemType instanceof EntityType
				&& persistentCollection != null
				// a newly instantiated collection can't have orphans
				&& !persistentCollection.isNewlyInstantiated();
		if ( context.traceEnabled() ) {
			final CascadeTraceEvent.OrphanSource orphanSource =
					persistentCollection == null
							? CascadeTraceEvent.OrphanSource.NONE
							: persistentCollection.wasInitialized()
									? CascadeTraceEvent.OrphanSource.COLLECTION_LOADED_STATE
									: CascadeTraceEvent.OrphanSource.QUEUED_OPERATIONS;
			traceOrphan(
					context,
					deleteOrphans
							? CascadeTraceEvent.OrphanEligibility.ELIGIBLE
							: CascadeTraceEvent.OrphanEligibility.INELIGIBLE,
					orphanSource,
					deleteOrphans
							? CascadeTraceEvent.OrphanTiming.DURING_COLLECTION_TRAVERSAL
							: CascadeTraceEvent.OrphanTiming.NONE
			);
		}

		if ( deleteOrphans ) {
			final boolean traceEnabled = context.executeEffects() && CORE_LOGGER.isTraceEnabled();
			if ( traceEnabled ) {
				CORE_LOGGER.deletingOrphans( collectionType.getRole() );
			}
			// we can do the cast since orphan-delete does not apply to:
			// 1. newly instantiated collections
			// 2. arrays (we can't track orphans for detached arrays)
			final String elementEntityName = collectionType.getAssociatedEntityName( eventSource.getFactory() );
			deleteOrphans( context, elementEntityName, persistentCollection );
			if ( traceEnabled ) {
				CORE_LOGGER.doneDeletingOrphans( collectionType.getRole() );
			}
		}
	}

	private static CascadeTraceEvent.CollectionIteratorMode collectionIteratorMode(
			CascadingAction<?> action,
			Iterator<?> iterator) {
		if ( action == CascadingActions.REMOVE ) {
			return CascadeTraceEvent.CollectionIteratorMode.ALL;
		}
		else if ( action == CascadingActions.REFRESH || action == CascadingActions.CHECK_ON_FLUSH ) {
			return iterator == emptyIterator()
					? CascadeTraceEvent.CollectionIteratorMode.EMPTY
					: CascadeTraceEvent.CollectionIteratorMode.LOADED;
		}
		else if ( action == CascadingActions.EVICT
				|| action == CascadingActions.MERGE
				|| action == CascadingActions.PERSIST
				|| action == CascadingActions.PERSIST_ON_FLUSH ) {
			return CascadeTraceEvent.CollectionIteratorMode.LOADED;
		}
		else {
			return CascadeTraceEvent.CollectionIteratorMode.CUSTOM;
		}
	}

	/**
	 * Delete any entities that were removed from the collection
	 */
	private static void deleteOrphans(
			CascadeTraversalContext<?> context,
			String entityName,
			PersistentCollection<?> collection) {
		//TODO: suck this logic into the collection!
		for ( Object orphan : getOrphans( context.session(), entityName, collection ) ) {
			if ( orphan != null && context.executeEffects() ) {
				CORE_LOGGER.deletingOrphanOfType( entityName );
				context.session().delete( entityName, orphan, false, DeleteContext.create() );
			}
		}
	}

	static Collection<?> getOrphans(EventSource eventSource, String entityName, PersistentCollection<?> collection) {
		final var tracker = eventSource.getPersistenceContextInternal().getCollectionFlushActionTracker();
		if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
			final var interpretation = flushProcessingContext.getValidCollectionInterpretation( collection );
			if ( interpretation != null
					&& interpretation.semanticChange() instanceof SemanticCollectionChange.Delta delta ) {
				return delta.delta().orphanCandidates();
			}
		}
		if ( collection.wasInitialized() ) {
			final var collectionEntry =
					eventSource.getPersistenceContextInternal()
							.getCollectionEntry( collection );
			return collectionEntry == null
					? EMPTY_LIST
					: collectionEntry.getOrphans( entityName, collection );
		}
		else {
			return collection.getQueuedOrphans( entityName );
		}
	}

	private static <T> boolean cascadeDeleteEnabled(CascadingAction<T> action, CollectionPersister persister) {
		return action.directionAffectedByCascadeDelete() == ForeignKeyDirection.FROM_PARENT
			&& persister.isCascadeDeleteEnabled();
	}

	private static <T> boolean cascadeDeleteEnabled(CascadingAction<T> action, EntityPersister persister, int i) {
		return action.directionAffectedByCascadeDelete() == ForeignKeyDirection.TO_PARENT
			&& persister.getPropertyOnDeleteActions()[i] == OnDeleteAction.CASCADE;
	}

	private static <T> boolean cascadeDeleteEnabled(CascadingAction<T> action, CompositeType componentType, int i) {
		return action.directionAffectedByCascadeDelete() == ForeignKeyDirection.TO_PARENT
			&& componentType.getOnDeleteAction( i ) == OnDeleteAction.CASCADE;
	}
}
