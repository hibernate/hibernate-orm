/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityBasedAssociationAttribute;
import org.hibernate.tuple.entity.EntityBasedBasicAttribute;
import org.hibernate.tuple.entity.EntityBasedCompositionAttribute;
import org.hibernate.tuple.entity.VersionProperty;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper.includeInBaseFetchGroup;

/**
 * @deprecated No direct replacement
 */
@Deprecated(forRemoval = true)
public final class PropertyFactory {
	private PropertyFactory() {
	}

	/**
	 * Generates the attribute representation of the identifier for a given entity mapping.
	 *
	 * @param mappedEntity The mapping definition of the entity.
	 * @param generator The identifier value generator to use for this identifier.
	 *
	 * @return The appropriate IdentifierProperty definition.
	 */
	public static IdentifierProperty buildIdentifierAttribute(
			PersistentClass mappedEntity,
			Generator generator) {
		final var type = mappedEntity.getIdentifier().getType();
		final var property = mappedEntity.getIdentifierProperty();
		if ( property == null ) {
			// this is a virtual id property...
			return new IdentifierProperty(
					type,
					mappedEntity.hasEmbeddedIdentifier(),
					mappedEntity.hasIdentifierMapper(),
					generator
			);
		}
		else {
			return new IdentifierProperty(
					property.getName(),
					type,
					mappedEntity.hasEmbeddedIdentifier(),
					generator
			);
		}
	}

	/**
	 * Generates a VersionProperty representation for an entity mapping given its
	 * version mapping Property.
	 *
	 * @param property The version mapping Property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 *
	 * @return The appropriate VersionProperty definition.
	 */
	public static VersionProperty buildVersionProperty(
			EntityPersister persister,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			Property property,
			boolean lazyAvailable) {
		final boolean lazy = lazyAvailable && property.isLazy();
		return new VersionProperty(
				persister,
				sessionFactory,
				attributeNumber,
				property.getName(),
				property.getValue().getType(),
				new BaselineAttributeInformation.Builder()
						.setLazy( lazy )
						.setInsertable( property.isInsertable() )
						.setUpdateable( property.isUpdatable() )
						.setNullable( property.isOptional() )
						.setDirtyCheckable( property.isUpdatable() && !lazy )
						.setVersionable( property.isOptimisticLocked() )
						.setCascadeStyle( property.getCascadeStyle() )
						.setOnDeleteAction( property.getOnDeleteAction() )
						.createInformation()
		);
	}

	public enum NonIdentifierAttributeNature {
		BASIC,
		COMPOSITE,
		ANY,
		ENTITY,
		COLLECTION
	}

	/**
	 * Generate a non-identifier (and non-version) attribute based on the given mapped property from the given entity
	 *
	 * @param property The mapped property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 *
	 * @return The appropriate NonIdentifierProperty definition.
	 */
	public static NonIdentifierAttribute buildEntityBasedAttribute(
			EntityPersister persister,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			Property property,
			boolean lazyAvailable,
			RuntimeModelCreationContext creationContext) {
		final var type = property.getValue().getType();

		// we need to dirty check collections, since they can cause an owner
		// version number increment

		// we need to dirty check many-to-ones with not-found="ignore" in order
		// to update the cache (not the database), since in this case a null
		// entity reference can lose information

		final boolean alwaysDirtyCheck = type.isAssociationType()
				&& ( (AssociationType) type ).isAlwaysDirtyChecked();

		final boolean lazy = !includeInBaseFetchGroup(
				property,
				lazyAvailable,
				entityName -> {
					final var entityBinding =
							creationContext.getMetadata()
									.getEntityBinding( entityName );
					assert entityBinding != null;
					return entityBinding.hasSubclasses();
				},
				sessionFactory.getSessionFactoryOptions()
						.isCollectionsInDefaultFetchGroupEnabled()
		);

		return switch ( decode( type ) ) {
			case BASIC ->
					new EntityBasedBasicAttribute(
							persister,
							sessionFactory,
							attributeNumber,
							property.getName(),
							type,
							new BaselineAttributeInformation.Builder()
									.setLazy( lazy )
									.setInsertable( property.isInsertable() )
									.setUpdateable( property.isUpdatable() )
									.setNullable( property.isOptional() )
									.setDirtyCheckable( alwaysDirtyCheck || property.isUpdatable() )
									.setVersionable( property.isOptimisticLocked() )
									.setCascadeStyle( property.getCascadeStyle() )
									.setOnDeleteAction( property.getOnDeleteAction() )
									.setFetchMode( property.getValue().getFetchMode() )
									.createInformation()
					);
			case COMPOSITE ->
					new EntityBasedCompositionAttribute(
							persister,
							sessionFactory,
							attributeNumber,
							property.getName(),
							(CompositeType) type,
							new BaselineAttributeInformation.Builder()
									.setLazy( lazy )
									.setInsertable( property.isInsertable() )
									.setUpdateable( property.isUpdatable() )
									.setNullable( property.isOptional() )
									.setDirtyCheckable( alwaysDirtyCheck || property.isUpdatable() )
									.setVersionable( property.isOptimisticLocked() )
									.setCascadeStyle( property.getCascadeStyle() )
									.setOnDeleteAction( property.getOnDeleteAction() )
									.setFetchMode( property.getValue().getFetchMode() )
									.createInformation()
					);
			case ENTITY, ANY, COLLECTION ->
					new EntityBasedAssociationAttribute(
							persister,
							sessionFactory,
							attributeNumber,
							property.getName(),
							(AssociationType) type,
							new BaselineAttributeInformation.Builder()
									.setLazy( lazy )
									.setInsertable( property.isInsertable() )
									.setUpdateable( property.isUpdatable() )
									.setNullable( property.isOptional() )
									.setDirtyCheckable( alwaysDirtyCheck || property.isUpdatable() )
									.setVersionable( property.isOptimisticLocked() )
									.setCascadeStyle( property.getCascadeStyle() )
									.setOnDeleteAction( property.getOnDeleteAction() )
									.setFetchMode( property.getValue().getFetchMode() )
									.createInformation()
					);
		};
	}

	private static NonIdentifierAttributeNature decode(Type type) {
		if ( type instanceof CollectionType ) {
			return NonIdentifierAttributeNature.COLLECTION;
		}
		else if ( type instanceof EntityType ) {
			return NonIdentifierAttributeNature.ENTITY;
		}
		else if ( type instanceof AnyType ) {
			return NonIdentifierAttributeNature.ANY;
		}
		else if ( type instanceof ComponentType ) {
			return NonIdentifierAttributeNature.COMPOSITE;
		}
		else {
			return NonIdentifierAttributeNature.BASIC;
		}
	}
}
