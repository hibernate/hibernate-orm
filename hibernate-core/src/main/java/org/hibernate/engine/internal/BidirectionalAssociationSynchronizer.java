/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;

/// Manages loaded bidirectional associations during flush preparation.
///
/// This synchronizer is runtime-only: it treats the owning side of each mapping
/// as authoritative and updates managed in-memory inverse attributes and loaded
/// managed persistent collections before dirty checking and action scheduling.
/// The inverse-side changes are treated the same way as if user code had made
/// those changes manually.
///
/// The synchronization is driven by the [AssociationPlan] derived from the SessionFactory
/// mapping model.  The [AssociationPlan] is cached here in a weak map keyed by the
/// underlying SessionFactory.  The use of a weak map here is simply for convenience, in case
/// this becomes problematic.
///
/// @since 8.0
/// @author Steve Ebersole
public final class BidirectionalAssociationSynchronizer {
	private static final Map<SessionFactoryImplementor, AssociationPlan> ASSOCIATION_PLANS = new WeakHashMap<>();

	private BidirectionalAssociationSynchronizer() {
	}

	public static void synchronize(SessionImplementor session) {
		final var options = session.getFactory().getSessionFactoryOptions();
		if ( options.isBidirectionalAssociationManagementEnabled() ) {
			new Synchronization(
					session,
					getAssociationPlan( session.getFactory() )
			).run();
		}
	}

	private static AssociationPlan getAssociationPlan(SessionFactoryImplementor factory) {
		synchronized ( ASSOCIATION_PLANS ) {
			return ASSOCIATION_PLANS.computeIfAbsent( factory, AssociationPlan::from );
		}
	}

	private static PluralAttributeMapping findPluralAttribute(EntityPersister persister, String attributeName) {
		final AttributeMapping attributeMapping = persister.findAttributeMapping( attributeName );
		return attributeMapping == null ? null : attributeMapping.asPluralAttributeMapping();
	}

	private static ToOneAttributeMapping findToOneAttribute(EntityPersister persister, String attributeName) {
		final AttributeMapping attributeMapping = persister.findAttributeMapping( attributeName );
		if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			return toOneAttributeMapping;
		}
		return null;
	}

	private static String associationKey(String entityName, String propertyName) {
		return entityName + '#' + propertyName;
	}

	private record AssociationPlan(
			Map<String, OneToManyDescriptor> oneToManyByOwningAttribute,
			Map<String, OneToOneDescriptor> oneToOneByOwningAttribute,
			Map<String, ManyToManyDescriptor> manyToManyByOwningRole) {
		private static AssociationPlan from(SessionFactoryImplementor factory) {
			final Map<String, OneToManyDescriptor> oneToManyByOwningAttribute = new HashMap<>();
			final Map<String, OneToOneDescriptor> oneToOneByOwningAttribute = new HashMap<>();
			final Map<String, ManyToManyDescriptor> manyToManyByOwningRole = new HashMap<>();
			factory.getMappingMetamodel().forEachEntityDescriptor( entityPersister ->
					entityPersister.forEachAttributeMapping( attributeMapping -> {
						if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
							registerToOneAssociation(
									entityPersister,
									toOneAttributeMapping,
									oneToManyByOwningAttribute,
									oneToOneByOwningAttribute
							);
						}
						else if ( attributeMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
							registerManyToManyAssociation( pluralAttributeMapping, manyToManyByOwningRole );
						}
					} ) );
			return new AssociationPlan(
					Map.copyOf( oneToManyByOwningAttribute ),
					Map.copyOf( oneToOneByOwningAttribute ),
					Map.copyOf( manyToManyByOwningRole )
			);
		}

		private boolean isEmpty() {
			return oneToManyByOwningAttribute.isEmpty()
					&& oneToOneByOwningAttribute.isEmpty()
					&& manyToManyByOwningRole.isEmpty();
		}

		private static void registerToOneAssociation(
				EntityPersister declaringPersister,
				ToOneAttributeMapping toOneAttributeMapping,
				Map<String, OneToManyDescriptor> oneToManyByOwningAttribute,
				Map<String, OneToOneDescriptor> oneToOneByOwningAttribute) {
			final var bidirectionalAttributePath = toOneAttributeMapping.getBidirectionalAttributePath();
			if ( bidirectionalAttributePath == null ) {
				return;
			}

			final EntityPersister associatedPersister =
					toOneAttributeMapping.getAssociatedEntityMappingType().getEntityPersister();
			if ( toOneAttributeMapping.getCardinality() == ToOneAttributeMapping.Cardinality.MANY_TO_ONE ) {
				final PluralAttributeMapping inverseCollection =
						findPluralAttribute( associatedPersister, bidirectionalAttributePath.getFullPath() );
				if ( inverseCollection != null
						&& inverseCollection.getCollectionDescriptor().isOneToMany()
						&& inverseCollection.isBidirectionalAttributeName(
								new NavigablePath( toOneAttributeMapping.getAttributeName() ),
								toOneAttributeMapping
						) ) {
					oneToManyByOwningAttribute.put(
							associationKey( declaringPersister.getEntityName(), toOneAttributeMapping.getAttributeName() ),
							new OneToManyDescriptor( toOneAttributeMapping, inverseCollection )
					);
				}
			}
			else if ( toOneAttributeMapping.getCardinality() == ToOneAttributeMapping.Cardinality.LOGICAL_ONE_TO_ONE ) {
				final ToOneAttributeMapping inverseAttribute =
						findToOneAttribute( associatedPersister, bidirectionalAttributePath.getFullPath() );
				if ( inverseAttribute != null ) {
					oneToOneByOwningAttribute.putIfAbsent(
							associationKey( declaringPersister.getEntityName(), toOneAttributeMapping.getAttributeName() ),
							new OneToOneDescriptor( toOneAttributeMapping, inverseAttribute )
					);
				}
			}
			else {
				final ToOneAttributeMapping owningAttribute =
						findToOneAttribute( associatedPersister, bidirectionalAttributePath.getFullPath() );
				if ( owningAttribute != null ) {
					oneToOneByOwningAttribute.putIfAbsent(
							associationKey( associatedPersister.getEntityName(), owningAttribute.getAttributeName() ),
							new OneToOneDescriptor( owningAttribute, toOneAttributeMapping )
					);
				}
			}
		}

		private static void registerManyToManyAssociation(
				PluralAttributeMapping inverseCollection,
				Map<String, ManyToManyDescriptor> manyToManyByOwningRole) {
			final CollectionPersister inverseCollectionDescriptor = inverseCollection.getCollectionDescriptor();
			final String mappedByProperty = inverseCollectionDescriptor.getMappedByProperty();
			if ( mappedByProperty == null
					|| !inverseCollectionDescriptor.isManyToMany()
					|| !inverseCollectionDescriptor.isInverse() ) {
				return;
			}

			final PluralAttributeMapping owningCollection =
					findPluralAttribute( inverseCollectionDescriptor.getElementPersister(), mappedByProperty );
			if ( owningCollection != null
					&& owningCollection.getCollectionDescriptor().isManyToMany()
					&& !owningCollection.getCollectionDescriptor().isInverse() ) {
				manyToManyByOwningRole.put(
						owningCollection.getCollectionDescriptor().getRole(),
						new ManyToManyDescriptor( owningCollection, inverseCollection )
				);
			}
		}
	}

	private static final class Synchronization {
		private final PersistenceContext persistenceContext;
		private final AssociationPlan associationPlan;

		private Synchronization(
				SessionImplementor session,
				AssociationPlan associationPlan) {
			this.persistenceContext = session.getPersistenceContextInternal();
			this.associationPlan = associationPlan;
		}

		private void run() {
			if ( associationPlan.isEmpty() ) {
				return;
			}

			repairToOneAssociations();
			repairManyToManyAssociations();
		}

		private void repairToOneAssociations() {
			final Map<OneToManyDescriptor, Map<Object, Set<Object>>> expectedOneToMany = new IdentityHashMap<>();
			final Map<OneToOneDescriptor, Map<Object, Object>> expectedOneToOne = new IdentityHashMap<>();

			for ( var entry : persistenceContext.reentrantSafeEntityEntries() ) {
				final Object entity = entry.getKey();
				final EntityEntry entityEntry = entry.getValue();
				if ( !isManaged( entityEntry ) ) {
					continue;
				}

				final String entityName = entityEntry.getPersister().getEntityName();
				for ( OneToOneDescriptor descriptor : associationPlan.oneToOneByOwningAttribute.values() ) {
					if ( !descriptor.owningEntityName().equals( entityName ) ) {
						continue;
					}
					final Object target = descriptor.owningAttribute.getValue( entity );
					if ( target != null && isManagedEntity( target ) ) {
						expectedOneToOne.computeIfAbsent( descriptor, ignored -> new IdentityHashMap<>() )
								.put( target, entity );
					}
				}
				for ( OneToManyDescriptor descriptor : associationPlan.oneToManyByOwningAttribute.values() ) {
					if ( !descriptor.owningEntityName().equals( entityName ) ) {
						continue;
					}
					final Object target = descriptor.owningAttribute.getValue( entity );
					if ( target != null && isManagedEntity( target ) ) {
						expectedOneToMany.computeIfAbsent( descriptor, ignored -> new IdentityHashMap<>() )
								.computeIfAbsent( target, ignored -> newIdentitySet() )
								.add( entity );
					}
				}
			}

			repairOneToMany( expectedOneToMany );
			repairOneToOne( expectedOneToOne );
		}

		private void repairOneToMany(Map<OneToManyDescriptor, Map<Object, Set<Object>>> expectedOneToMany) {
			for ( var descriptorEntry : expectedOneToMany.entrySet() ) {
				final OneToManyDescriptor descriptor = descriptorEntry.getKey();
				final Map<Object, Set<Object>> expectedByParent = descriptorEntry.getValue();
				for ( var entry : persistenceContext.reentrantSafeEntityEntries() ) {
					final Object parent = entry.getKey();
					final EntityEntry entityEntry = entry.getValue();
					if ( !isManaged( entityEntry ) || !descriptor.inversePersister().isInstance( parent ) ) {
						continue;
					}

					final Object inverseValue = descriptor.inverseAttribute.getValue( parent );
					if ( !( inverseValue instanceof Collection<?> inverseCollection ) ) {
						continue;
					}
					if ( !prepareCollection( inverseCollection ) ) {
						continue;
					}

					final Set<Object> expectedChildren =
							expectedByParent.getOrDefault( parent, java.util.Collections.emptySet() );
					boolean changed = false;
					if ( inverseCollection.removeIf( child -> isManagedEntity( child )
							&& !expectedChildren.contains( child )
							&& descriptor.owningPersister().isInstance( child )
							&& descriptor.owningAttribute.getValue( child ) != parent ) ) {
						changed = true;
					}
					for ( Object child : expectedChildren ) {
						if ( !inverseCollection.contains( child ) ) {
							changed = add( inverseCollection, child ) || changed;
						}
					}
				}
			}
		}

		private void repairOneToOne(Map<OneToOneDescriptor, Map<Object, Object>> expectedOneToOne) {
			for ( var descriptorEntry : expectedOneToOne.entrySet() ) {
				final OneToOneDescriptor descriptor = descriptorEntry.getKey();
				final Map<Object, Object> expected = descriptorEntry.getValue();
				for ( var entry : persistenceContext.reentrantSafeEntityEntries() ) {
					final Object inverse = entry.getKey();
					final EntityEntry entityEntry = entry.getValue();
					if ( !isManaged( entityEntry ) || !descriptor.inversePersister().isInstance( inverse ) ) {
						continue;
					}

					final Object expectedOwner = expected.get( inverse );
					final Object currentOwner = descriptor.inverseAttribute.getValue( inverse );
					if ( currentOwner != expectedOwner && ( expectedOwner != null || isManagedEntity( currentOwner ) ) ) {
						setInverseProperty( descriptor.inverseAttribute, inverse, expectedOwner );
					}
				}
			}
		}

		private void repairManyToManyAssociations() {
			final Map<ManyToManyDescriptor, Map<Object, Set<Object>>> expectedManyToMany = new IdentityHashMap<>();
			persistenceContext.forEachCollectionEntry( (persistentCollection, collectionEntry) -> {
				final CollectionPersister loadedPersister = collectionEntry.getLoadedPersister();
				if ( loadedPersister == null || !persistentCollection.wasInitialized() ) {
					return;
				}

				final ManyToManyDescriptor descriptor = associationPlan.manyToManyByOwningRole.get( loadedPersister.getRole() );
				if ( descriptor == null ) {
					return;
				}

				final Object owner = persistenceContext.getLoadedCollectionOwnerOrNull( persistentCollection );
				if ( owner == null || !isManagedEntity( owner ) ) {
					return;
				}

				for ( Object element : initializedElements( persistentCollection, loadedPersister ) ) {
					if ( !isManagedEntity( element ) ) {
						continue;
					}
					expectedManyToMany.computeIfAbsent( descriptor, ignored -> new IdentityHashMap<>() )
							.computeIfAbsent( element, ignored -> newIdentitySet() )
							.add( owner );
				}
			}, true );

			for ( var descriptorEntry : expectedManyToMany.entrySet() ) {
				final ManyToManyDescriptor descriptor = descriptorEntry.getKey();
				final Map<Object, Set<Object>> expectedOwnersByElement = descriptorEntry.getValue();
				for ( var entry : persistenceContext.reentrantSafeEntityEntries() ) {
					final Object element = entry.getKey();
					final EntityEntry entityEntry = entry.getValue();
					if ( !isManaged( entityEntry ) || !descriptor.inversePersister().isInstance( element ) ) {
						continue;
					}
					final Object inverseValue = descriptor.inverseAttribute.getValue( element );
					if ( !( inverseValue instanceof Collection<?> inverseCollection ) ) {
						continue;
					}
					if ( !prepareCollection( inverseCollection ) ) {
						continue;
					}

					final Set<Object> expectedOwners =
							expectedOwnersByElement.getOrDefault( element, java.util.Collections.emptySet() );
					boolean changed = false;
					if ( inverseCollection.removeIf( owner -> isManagedEntity( owner )
							&& descriptor.owningPersister().isInstance( owner )
							&& !expectedOwners.contains( owner )
							&& initializedOwningCollectionExcludes( descriptor, owner, element ) ) ) {
						changed = true;
					}
					for ( Object owner : expectedOwners ) {
						if ( !inverseCollection.contains( owner ) ) {
							changed = add( inverseCollection, owner ) || changed;
						}
					}
				}
			}
		}

		private boolean initializedOwningCollectionExcludes(
				ManyToManyDescriptor descriptor,
				Object owner,
				Object element) {
			final Object owningValue = descriptor.owningAttribute.getValue( owner );
			return !( owningValue instanceof PersistentCollection<?> persistentCollection )
					|| persistentCollection.wasInitialized() && !asCollection( owningValue ).contains( element );
		}

		private Collection<?> asCollection(Object value) {
			return (Collection<?>) value;
		}

		private List<Object> initializedElements(
				PersistentCollection<?> persistentCollection,
				CollectionPersister collectionPersister) {
			final List<Object> elements = new ArrayList<>();
			final var iterator = persistentCollection.entries( collectionPersister );
			while ( iterator.hasNext() ) {
				elements.add( iterator.next() );
			}
			return elements;
		}

		private boolean prepareCollection(Collection<?> collection) {
			if ( collection instanceof PersistentCollection<?> persistentCollection
					&& !persistentCollection.wasInitialized() ) {
				return false;
			}
			return true;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private boolean add(Collection collection, Object value) {
			return collection.add( value );
		}

		private void setInverseProperty(
				ToOneAttributeMapping inverseAttribute,
				Object inverse,
				Object expectedOwner) {
			inverseAttribute.setValue( inverse, expectedOwner );
		}

		private boolean isManagedEntity(Object entity) {
			final EntityEntry entityEntry = persistenceContext.getEntry( entity );
			return entityEntry != null && isManaged( entityEntry );
		}

		private boolean isManaged(EntityEntry entityEntry) {
			final Status status = entityEntry.getStatus();
			return status == Status.MANAGED || status == Status.READ_ONLY || status == Status.SAVING;
		}

		private static <E> Set<E> newIdentitySet() {
			return java.util.Collections.newSetFromMap( new IdentityHashMap<>() );
		}
	}

	private record OneToManyDescriptor(
			ToOneAttributeMapping owningAttribute,
			PluralAttributeMapping inverseAttribute) {

		private EntityPersister owningPersister() {
			return owningAttribute.findContainingEntityMapping().getEntityPersister();
		}

		private String owningEntityName() {
			return owningPersister().getEntityName();
		}

		private EntityPersister inversePersister() {
			return inverseAttribute.findContainingEntityMapping().getEntityPersister();
		}
	}

	private record OneToOneDescriptor(
			ToOneAttributeMapping owningAttribute,
			ToOneAttributeMapping inverseAttribute) {
		private String owningEntityName() {
			return owningAttribute.findContainingEntityMapping().getEntityName();
		}

		private EntityPersister inversePersister() {
			return inverseAttribute.findContainingEntityMapping().getEntityPersister();
		}
	}

	private record ManyToManyDescriptor(
			PluralAttributeMapping owningAttribute,
			PluralAttributeMapping inverseAttribute) {

		private EntityPersister owningPersister() {
			return owningAttribute.findContainingEntityMapping().getEntityPersister();
		}

		private EntityPersister inversePersister() {
			return inverseAttribute.findContainingEntityMapping().getEntityPersister();
		}
	}
}
