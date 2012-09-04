/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tuple;

import java.lang.reflect.Constructor;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BackRefAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.NonAggregatedCompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeAssociationElementBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * Responsible for generation of runtime metamodel {@link Property} representations.
 * Makes distinction between identifier, version, and other (standard) properties.
 *
 * @author Steve Ebersole
 */
public class PropertyFactory {

	/**
	 * Generates an IdentifierProperty representation of the for a given entity mapping.
	 *
	 * @param mappedEntity The mapping definition of the entity.
	 * @param generator The identifier value generator to use for this identifier.
	 * @return The appropriate IdentifierProperty definition.
	 */
	public static IdentifierProperty buildIdentifierProperty(PersistentClass mappedEntity, IdentifierGenerator generator) {

		String mappedUnsavedValue = mappedEntity.getIdentifier().getNullValue();
		Type type = mappedEntity.getIdentifier().getType();
		Property property = mappedEntity.getIdentifierProperty();
		
		IdentifierValue unsavedValue = UnsavedValueFactory.getUnsavedIdentifierValue(
				mappedUnsavedValue,
				getGetter( property ),
				type,
				getConstructor(mappedEntity)
			);

		if ( property == null ) {
			// this is a virtual id property...
			return new IdentifierProperty(
			        type,
					mappedEntity.hasEmbeddedIdentifier(),
					mappedEntity.hasIdentifierMapper(),
					unsavedValue,
					generator
				);
		}
		else {
			return new IdentifierProperty(
					property.getName(),
					property.getNodeName(),
					type,
					mappedEntity.hasEmbeddedIdentifier(),
					unsavedValue,
					generator
				);
		}
	}

	/**
	 * Generates an IdentifierProperty representation of the for a given entity mapping.
	 *
	 * @param mappedEntity The mapping definition of the entity.
	 * @param rootEntityName The entity name for the EntityBinding at the root of this hierarchy.
	 * @param sessionFactory The session factory.
	 * @return The appropriate IdentifierProperty definition.
	 */
	public static IdentifierProperty buildIdentifierProperty(
			EntityBinding mappedEntity,
			String rootEntityName,
			SessionFactoryImplementor sessionFactory) {

		final IdentifierGenerator generator = sessionFactory.getIdentifierGenerator( rootEntityName );

		final SingularAttributeBinding attributeBinding = mappedEntity.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();

		// TODO: the following will cause an NPE with "virtual" IDs; how should they be set?
		// (steve) virtual attributes will still be attributes, they will simply be marked as virtual.
		//		see org.hibernate.metamodel.domain.AbstractAttributeContainer.locateOrCreateVirtualAttribute()

		final String mappedUnsavedValue = mappedEntity.getHierarchyDetails().getEntityIdentifier().getUnsavedValue();
		final Type type;
		if ( mappedEntity.getHierarchyDetails().getEntityIdentifier().isIdentifierMapper() ) {
			type = sessionFactory.getTypeResolver().getTypeFactory().component(
					new ComponentMetamodel( (NonAggregatedCompositeAttributeBinding) attributeBinding, true, true )
			);
		}
		else {
			type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		}

		IdentifierValue unsavedValue = UnsavedValueFactory.getUnsavedIdentifierValue(
				mappedUnsavedValue,
				getGetterOrNull( attributeBinding ),
				type,
				getConstructor( mappedEntity )
			);

		if ( attributeBinding.getAttribute().isSynthetic()  ) {
			// this is a virtual id property...
			return new IdentifierProperty(
			        type,
					mappedEntity.getHierarchyDetails().getEntityIdentifier().isNonAggregatedComposite() &&
							mappedEntity.getHierarchyDetails().getEntityIdentifier().getIdClassClass() == null,
					mappedEntity.getHierarchyDetails().getEntityIdentifier().isIdentifierMapper(),
					unsavedValue,
					generator
				);
		}
		else {
			return new IdentifierProperty(
					attributeBinding.getAttribute().getName(),
					null,
					type,
					mappedEntity.getHierarchyDetails().getEntityIdentifier().isNonAggregatedComposite(),
					unsavedValue,
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
	 * @return The appropriate VersionProperty definition.
	 */
	public static VersionProperty buildVersionProperty(Property property, boolean lazyAvailable) {
		String mappedUnsavedValue = ( (KeyValue) property.getValue() ).getNullValue();
		
		VersionValue unsavedValue = UnsavedValueFactory.getUnsavedVersionValue(
				mappedUnsavedValue,
				getGetter( property ),
				(VersionType) property.getType(),
				getConstructor( property.getPersistentClass() )
			);

		boolean lazy = lazyAvailable && property.isLazy();

		return new VersionProperty(
		        property.getName(),
		        property.getNodeName(),
		        property.getValue().getType(),
		        lazy,
				property.isInsertable(),
				property.isUpdateable(),
		        property.getGeneration() == PropertyGeneration.INSERT || property.getGeneration() == PropertyGeneration.ALWAYS,
				property.getGeneration() == PropertyGeneration.ALWAYS,
				property.isOptional(),
				property.isUpdateable() && !lazy,
				property.isOptimisticLocked(),
		        property.getCascadeStyle(),
		        unsavedValue
			);
	}

	/**
	 * Generates a VersionProperty representation for an entity mapping given its
	 * version mapping Property.
	 *
	 * @param property The version mapping Property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 * @return The appropriate VersionProperty definition.
	 */
	public static VersionProperty buildVersionProperty(
			EntityBinding entityBinding,
			BasicAttributeBinding property,
			boolean lazyAvailable) {
		final String mappedUnsavedValue = entityBinding.getHierarchyDetails().getEntityVersion().getUnsavedValue();
		final VersionValue unsavedValue = UnsavedValueFactory.getUnsavedVersionValue(
				mappedUnsavedValue,
				getGetterOrNull( property ),
				(VersionType) property.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				getConstructor( (EntityBinding) property.getContainer() )
		);

		boolean lazy = lazyAvailable && property.isLazy();

		final CascadeStyle cascadeStyle = property.isAssociation()
				? ( (SingularAssociationAttributeBinding) property ).getCascadeStyle()
				: CascadeStyles.NONE;

		return new VersionProperty(
		        property.getAttribute().getName(),
		        null,
		        property.getHibernateTypeDescriptor().getResolvedTypeMapping(),
		        lazy,
				true, // insertable
				true, // updatable
		        property.getGeneration() == PropertyGeneration.INSERT
						|| property.getGeneration() == PropertyGeneration.ALWAYS,
				property.getGeneration() == PropertyGeneration.ALWAYS,
				property.isNullable(),
				!lazy,
				property.isIncludedInOptimisticLocking(),
				cascadeStyle,
		        unsavedValue
			);
	}

	/**
	 * Generate a "standard" (i.e., non-identifier and non-version) based on the given
	 * mapped property.
	 *
	 * @param property The mapped property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 * @return The appropriate StandardProperty definition.
	 */
	public static StandardProperty buildStandardProperty(Property property, boolean lazyAvailable) {
		
		final Type type = property.getValue().getType();
		
		// we need to dirty check collections, since they can cause an owner
		// version number increment
		
		// we need to dirty check many-to-ones with not-found="ignore" in order 
		// to update the cache (not the database), since in this case a null
		// entity reference can lose information
		
		boolean alwaysDirtyCheck = type.isAssociationType() && 
				( (AssociationType) type ).isAlwaysDirtyChecked(); 

		return new StandardProperty(
				property.getName(),
				property.getNodeName(),
				type,
				lazyAvailable && property.isLazy(),
				property.isInsertable(),
				property.isUpdateable(),
		        property.getGeneration() == PropertyGeneration.INSERT || property.getGeneration() == PropertyGeneration.ALWAYS,
				property.getGeneration() == PropertyGeneration.ALWAYS,
				property.isOptional(),
				alwaysDirtyCheck || property.isUpdateable(),
				property.isOptimisticLocked(),
				property.getCascadeStyle(),
		        property.getValue().getFetchMode()
			);
	}

	/**
	 * Generate a "standard" (i.e., non-identifier and non-version) based on the given
	 * mapped property.
	 *
	 * @param property The mapped property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 * @return The appropriate StandardProperty definition.
	 */
	public static StandardProperty buildStandardProperty(AttributeBinding property, boolean lazyAvailable) {

		final Type type = property.getHibernateTypeDescriptor().getResolvedTypeMapping();

		// we need to dirty check collections, since they can cause an owner
		// version number increment

		// we need to dirty check many-to-ones with not-found="ignore" in order
		// to update the cache (not the database), since in this case a null
		// entity reference can lose information

		final boolean alwaysDirtyCheck = type.isAssociationType() && ( (AssociationType) type ).isAlwaysDirtyChecked();

		if ( property.getAttribute().isSingular() ) {
			final SingularAttributeBinding singularAttributeBinding = ( SingularAttributeBinding ) property;
			final CascadeStyle cascadeStyle = singularAttributeBinding.isAssociation()
					? ( (SingularAssociationAttributeBinding) singularAttributeBinding ).getCascadeStyle()
					: CascadeStyles.NONE;
			final FetchMode fetchMode = singularAttributeBinding.isAssociation()
					? ( (SingularAssociationAttributeBinding) singularAttributeBinding ).getFetchMode()
					: FetchMode.DEFAULT;

			PropertyGeneration propertyGeneration = BasicAttributeBinding.class.isInstance( property )
					? ( (BasicAttributeBinding) property ).getGeneration()
					: PropertyGeneration.NEVER;
			return new StandardProperty(
					singularAttributeBinding.getAttribute().getName(),
					null,
					type,
					lazyAvailable && singularAttributeBinding.isLazy(),
					singularAttributeBinding.isIncludedInInsert(), // insertable
					singularAttributeBinding.isIncludedInUpdate(), // updatable
					propertyGeneration == PropertyGeneration.INSERT
							|| propertyGeneration == PropertyGeneration.ALWAYS,
					propertyGeneration == PropertyGeneration.ALWAYS,
					singularAttributeBinding.isNullable(),
					alwaysDirtyCheck || singularAttributeBinding.isIncludedInUpdate(),
					singularAttributeBinding.isIncludedInOptimisticLocking(),
					cascadeStyle,
					fetchMode
			);
		}
		else {
			final AbstractPluralAttributeBinding pluralAttributeBinding = (AbstractPluralAttributeBinding) property;
			final CascadeStyle cascadeStyle = pluralAttributeBinding.isAssociation()
					? ( (PluralAttributeAssociationElementBinding) pluralAttributeBinding.getPluralAttributeElementBinding() ).getCascadeStyle()
					: CascadeStyles.NONE;
			final FetchMode fetchMode = pluralAttributeBinding.isAssociation()
					? pluralAttributeBinding.getFetchMode()
					: FetchMode.DEFAULT;

			return new StandardProperty(
					pluralAttributeBinding.getAttribute().getName(),
					null,
					type,
					lazyAvailable && pluralAttributeBinding.isLazy(),
					// TODO: fix this when HHH-6356 is fixed; for now assume AbstractPluralAttributeBinding is updatable and insertable
					true, // pluralAttributeBinding.isInsertable(),
					true, //pluralAttributeBinding.isUpdatable(),
					false,
					false,
					true, // plural attributes are nullable
					// TODO: fix this when HHH-6356 is fixed; for now assume AbstractPluralAttributeBinding is updatable and insertable
					//alwaysDirtyCheck || pluralAttributeBinding.isUpdatable(),
					true,
					pluralAttributeBinding.isIncludedInOptimisticLocking(),
					cascadeStyle,
					fetchMode
				);
		}
	}

	private static Constructor getConstructor(PersistentClass persistentClass) {
		if ( persistentClass == null || !persistentClass.hasPojoRepresentation() ) {
			return null;
		}

		try {
			return ReflectHelper.getDefaultConstructor( persistentClass.getMappedClass() );
		}
		catch( Throwable t ) {
			return null;
		}
	}

	private static Constructor getConstructor(EntityBinding entityBinding) {
		if ( entityBinding == null || entityBinding.getEntity() == null ) {
			return null;
		}

		try {
			return ReflectHelper.getDefaultConstructor( entityBinding.getEntity().getClassReference() );
		}
		catch( Throwable t ) {
			return null;
		}
	}

	private static Getter getGetter(Property mappingProperty) {
		if ( mappingProperty == null || !mappingProperty.getPersistentClass().hasPojoRepresentation() ) {
			return null;
		}

		PropertyAccessor pa = PropertyAccessorFactory.getPropertyAccessor( mappingProperty, EntityMode.POJO );
		return pa.getGetter( mappingProperty.getPersistentClass().getMappedClass(), mappingProperty.getName() );
	}

	public static Getter getGetter(AttributeBinding mappingProperty) {
		return getPropertyAccessor( mappingProperty ).getGetter(
				mappingProperty.getContainer().getClassReference(),
				mappingProperty.getAttribute().getName()
		);
	}

	//TODO: Remove this...
	public static Getter getIdentifierMapperGetter(
			String propertyPath,
			String identifierMapperPropertyAccessorName,
			EntityMode entityMode,
			Class<?> identifierMapperClassReference) {
		final PropertyAccessor pa =
				PropertyAccessorFactory.getPropertyAccessor( identifierMapperPropertyAccessorName, entityMode );
		return pa.getGetter( identifierMapperClassReference, propertyPath );

	}

	//TODO: Remove this...
	public static Setter getIdentifierMapperSetter(
			String propertyPath,
			String identifierMapperPropertyAccessorName,
			EntityMode entityMode,
			Class<?> identifierMapperClassReference) {
		final PropertyAccessor pa =
				PropertyAccessorFactory.getPropertyAccessor( identifierMapperPropertyAccessorName, entityMode );
		return pa.getSetter( identifierMapperClassReference, propertyPath );
	}

	private static Getter getGetterOrNull(AttributeBinding mappingProperty) {
		try {
			return getGetter( mappingProperty );
		}
		catch ( PropertyNotFoundException ex ) {
			// ignore exception
		}
		return null;
	}

	public static Setter getSetter(AttributeBinding mappingProperty) {
		return getPropertyAccessor( mappingProperty ).getSetter(
				mappingProperty.getContainer().getClassReference(),
				mappingProperty.getAttribute().getName()
		);
	}

	private static PropertyAccessor getPropertyAccessor(AttributeBinding mappingProperty) {
		EntityMode entityMode = mappingProperty.getContainer().seekEntityBinding().getHierarchyDetails().getEntityMode();

		if ( mappingProperty.isBackRef() ) {
			BackRefAttributeBinding backRefAttributeBinding = (BackRefAttributeBinding) mappingProperty;
			return PropertyAccessorFactory.getBackRefPropertyAccessor(
					backRefAttributeBinding.getEntityName(), backRefAttributeBinding.getCollectionRole(), entityMode
			);
		}
		else {
			return PropertyAccessorFactory.getPropertyAccessor( mappingProperty.getPropertyAccessorName(), entityMode );
		}
	}

}
