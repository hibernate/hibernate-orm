/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tuple;
import java.lang.reflect.Constructor;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityIdentifier;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.internal.util.ReflectHelper;

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
	 * @param generator The identifier value generator to use for this identifier.
	 * @return The appropriate IdentifierProperty definition.
	 */
	public static IdentifierProperty buildIdentifierProperty(EntityBinding mappedEntity, IdentifierGenerator generator) {

		final SimpleAttributeBinding property = mappedEntity.getEntityIdentifier().getValueBinding();

		// TODO: the following will cause an NPE with "virtual" IDs; how should they be set?
		final String mappedUnsavedValue = property.getUnsavedValue();
		final Type type = property.getHibernateTypeDescriptor().getExplicitType();

		IdentifierValue unsavedValue = UnsavedValueFactory.getUnsavedIdentifierValue(
				mappedUnsavedValue,
				getGetter( property ),
				type,
				getConstructor( mappedEntity )
			);

		if ( property == null ) {
			// this is a virtual id property...
			return new IdentifierProperty(
			        type,
					mappedEntity.getEntityIdentifier().isEmbedded(),
					mappedEntity.getEntityIdentifier().isIdentifierMapper(),
					unsavedValue,
					generator
				);
		}
		else {
			return new IdentifierProperty(
					property.getAttribute().getName(),
					null,
					type,
					mappedEntity.getEntityIdentifier().isEmbedded(),
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
	public static VersionProperty buildVersionProperty(SimpleAttributeBinding property, boolean lazyAvailable) {
		String mappedUnsavedValue = ( (KeyValue) property.getValue() ).getNullValue();

		VersionValue unsavedValue = UnsavedValueFactory.getUnsavedVersionValue(
				mappedUnsavedValue,
				getGetter( property ),
				( VersionType ) property.getHibernateTypeDescriptor().getExplicitType(),
				getConstructor( property.getEntityBinding() )
		);

		boolean lazy = lazyAvailable && property.isLazy();

		return new VersionProperty(
		        property.getAttribute().getName(),
		        null,
		        property.getHibernateTypeDescriptor().getExplicitType(),
		        lazy,
				property.isInsertable(),
				property.isUpdatable(),
		        property.getGeneration() == PropertyGeneration.INSERT || property.getGeneration() == PropertyGeneration.ALWAYS,
				property.getGeneration() == PropertyGeneration.ALWAYS,
				property.isNullable(),
				property.isUpdatable() && !lazy,
				property.isOptimisticLockable(),
				// TODO: get cascadeStyle from property when HHH-6355 is fixed; for now, assume NONE
				//property.getCascadeStyle(),
				CascadeStyle.NONE,
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

		final Type type = property.getHibernateTypeDescriptor().getExplicitType();

		// we need to dirty check collections, since they can cause an owner
		// version number increment

		// we need to dirty check many-to-ones with not-found="ignore" in order
		// to update the cache (not the database), since in this case a null
		// entity reference can lose information

		boolean alwaysDirtyCheck = type.isAssociationType() &&
				( (AssociationType) type ).isAlwaysDirtyChecked();

		if ( property.isSimpleValue() ) {
			SimpleAttributeBinding simpleProperty = ( SimpleAttributeBinding ) property;
			return new StandardProperty(
					simpleProperty.getAttribute().getName(),
					null,
					type,
					lazyAvailable && simpleProperty.isLazy(),
					simpleProperty.isInsertable(),
					simpleProperty.isUpdatable(),
					simpleProperty.getGeneration() == PropertyGeneration.INSERT || simpleProperty.getGeneration() == PropertyGeneration.ALWAYS,
					simpleProperty.getGeneration() == PropertyGeneration.ALWAYS,
					simpleProperty.isNullable(),
					alwaysDirtyCheck || simpleProperty.isUpdatable(),
					simpleProperty.isOptimisticLockable(),
					// TODO: get cascadeStyle from simpleProperty when HHH-6355 is fixed; for now, assume NONE
					//simpleProperty.getCascadeStyle(),
					CascadeStyle.NONE,
					// TODO: get fetchMode() from simpleProperty when HHH-6357 is fixed; for now, assume FetchMode.DEFAULT
					//simpleProperty.getFetchMode()
					FetchMode.DEFAULT
				);
		}
		else {
			PluralAttributeBinding pluralProperty = ( PluralAttributeBinding ) property;

			return new StandardProperty(
					pluralProperty.getAttribute().getName(),
					null,
					type,
					lazyAvailable && pluralProperty.isLazy(),
					// TODO: fix this when HHH-6356 is fixed; for now assume PluralAttributeBinding is updatable and insertable
					// pluralProperty.isInsertable(),
					//pluralProperty.isUpdatable(),
					true,
					true,
					false,
					false,
					pluralProperty.isNullable(),
					// TODO: fix this when HHH-6356 is fixed; for now assume PluralAttributeBinding is updatable and insertable
					//alwaysDirtyCheck || pluralProperty.isUpdatable(),
					true,
					pluralProperty.isOptimisticLocked(),
					// TODO: get cascadeStyle from property when HHH-6355 is fixed; for now, assume NONE
					//pluralProperty.getCascadeStyle(),
					CascadeStyle.NONE,
					// TODO: get fetchMode() from simpleProperty when HHH-6357 is fixed; for now, assume FetchMode.DEFAULT
					//pluralProperty.getFetchMode()
					FetchMode.DEFAULT
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

	private static Getter getGetter(AttributeBinding mappingProperty) {
		if ( mappingProperty == null || mappingProperty.getEntityBinding().getEntity() == null ) {
			return null;
		}

		PropertyAccessor pa = PropertyAccessorFactory.getPropertyAccessor( mappingProperty, EntityMode.POJO );
		return pa.getGetter(
				mappingProperty.getEntityBinding().getEntity().getClassReference(),
				mappingProperty.getAttribute().getName()
		);
	}


}
