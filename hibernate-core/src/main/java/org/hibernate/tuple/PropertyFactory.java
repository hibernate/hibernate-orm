/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.lang.reflect.Constructor;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.tuple.entity.EntityBasedAssociationAttribute;
import org.hibernate.tuple.entity.EntityBasedBasicAttribute;
import org.hibernate.tuple.entity.EntityBasedCompositionAttribute;
import org.hibernate.tuple.entity.VersionProperty;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * Responsible for generation of runtime metamodel {@link Property} representations.
 * Makes distinction between identifier, version, and other (standard) properties.
 *
 * @author Steve Ebersole
 */
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
			IdentifierGenerator generator) {
		String mappedUnsavedValue = mappedEntity.getIdentifier().getNullValue();
		Type type = mappedEntity.getIdentifier().getType();
		Property property = mappedEntity.getIdentifierProperty();

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
		String mappedUnsavedValue = ( (KeyValue) property.getValue() ).getNullValue();

		VersionValue unsavedValue = UnsavedValueFactory.getUnsavedVersionValue(
				mappedUnsavedValue,
				getGetter( property ),
				(VersionType) property.getType(),
				getConstructor( property.getPersistentClass() )
		);

		boolean lazy = lazyAvailable && property.isLazy();

		return new VersionProperty(
				persister,
				sessionFactory,
				attributeNumber,
				property.getName(),
				property.getValue().getType(),
				new BaselineAttributeInformation.Builder()
						.setLazy( lazy )
						.setInsertable( property.isInsertable() )
						.setUpdateable( property.isUpdateable() )
						.setValueGenerationStrategy( property.getValueGenerationStrategy() )
						.setNullable( property.isOptional() )
						.setDirtyCheckable( property.isUpdateable() && !lazy )
						.setVersionable( property.isOptimisticLocked() )
						.setCascadeStyle( property.getCascadeStyle() )
						.createInformation(),
				unsavedValue
		);
	}

	public static enum NonIdentifierAttributeNature {
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
			boolean lazyAvailable) {
		final Type type = property.getValue().getType();

		final NonIdentifierAttributeNature nature = decode( type );

		// we need to dirty check collections, since they can cause an owner
		// version number increment

		// we need to dirty check many-to-ones with not-found="ignore" in order 
		// to update the cache (not the database), since in this case a null
		// entity reference can lose information

		boolean alwaysDirtyCheck = type.isAssociationType() &&
				( (AssociationType) type ).isAlwaysDirtyChecked();

		switch ( nature ) {
			case BASIC: {
				return new EntityBasedBasicAttribute(
						persister,
						sessionFactory,
						attributeNumber,
						property.getName(),
						type,
						new BaselineAttributeInformation.Builder()
								.setLazy( lazyAvailable && property.isLazy() )
								.setInsertable( property.isInsertable() )
								.setUpdateable( property.isUpdateable() )
								.setValueGenerationStrategy( property.getValueGenerationStrategy() )
								.setNullable( property.isOptional() )
								.setDirtyCheckable( alwaysDirtyCheck || property.isUpdateable() )
								.setVersionable( property.isOptimisticLocked() )
								.setCascadeStyle( property.getCascadeStyle() )
								.setFetchMode( property.getValue().getFetchMode() )
								.createInformation()
				);
			}
			case COMPOSITE: {
				return new EntityBasedCompositionAttribute(
						persister,
						sessionFactory,
						attributeNumber,
						property.getName(),
						(CompositeType) type,
						new BaselineAttributeInformation.Builder()
								.setLazy( lazyAvailable && property.isLazy() )
								.setInsertable( property.isInsertable() )
								.setUpdateable( property.isUpdateable() )
								.setValueGenerationStrategy( property.getValueGenerationStrategy() )
								.setNullable( property.isOptional() )
								.setDirtyCheckable( alwaysDirtyCheck || property.isUpdateable() )
								.setVersionable( property.isOptimisticLocked() )
								.setCascadeStyle( property.getCascadeStyle() )
								.setFetchMode( property.getValue().getFetchMode() )
								.createInformation()
				);
			}
			case ENTITY:
			case ANY:
			case COLLECTION: {
				return new EntityBasedAssociationAttribute(
						persister,
						sessionFactory,
						attributeNumber,
						property.getName(),
						(AssociationType) type,
						new BaselineAttributeInformation.Builder()
								.setLazy( lazyAvailable && property.isLazy() )
								.setInsertable( property.isInsertable() )
								.setUpdateable( property.isUpdateable() )
								.setValueGenerationStrategy( property.getValueGenerationStrategy() )
								.setNullable( property.isOptional() )
								.setDirtyCheckable( alwaysDirtyCheck || property.isUpdateable() )
								.setVersionable( property.isOptimisticLocked() )
								.setCascadeStyle( property.getCascadeStyle() )
								.setFetchMode( property.getValue().getFetchMode() )
								.createInformation()
				);
			}
			default: {
				throw new HibernateException( "Internal error" );
			}
		}
	}

	private static NonIdentifierAttributeNature decode(Type type) {
		if ( type.isAssociationType() ) {
			AssociationType associationType = (AssociationType) type;

			if ( type.isComponentType() ) {
				// an any type is both an association and a composite...
				return NonIdentifierAttributeNature.ANY;
			}

			return type.isCollectionType()
					? NonIdentifierAttributeNature.COLLECTION
					: NonIdentifierAttributeNature.ENTITY;
		}
		else {
			if ( type.isComponentType() ) {
				return NonIdentifierAttributeNature.COMPOSITE;
			}

			return NonIdentifierAttributeNature.BASIC;
		}
	}

	/**
	 * @deprecated See mainly {@link #buildEntityBasedAttribute}
	 */
	@Deprecated
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
				type,
				lazyAvailable && property.isLazy(),
				property.isInsertable(),
				property.isUpdateable(),
				property.getValueGenerationStrategy(),
				property.isOptional(),
				alwaysDirtyCheck || property.isUpdateable(),
				property.isOptimisticLocked(),
				property.getCascadeStyle(),
				property.getValue().getFetchMode()
		);
	}


	private static Constructor getConstructor(PersistentClass persistentClass) {
		if ( persistentClass == null || !persistentClass.hasPojoRepresentation() ) {
			return null;
		}

		try {
			return ReflectHelper.getDefaultConstructor( persistentClass.getMappedClass() );
		}
		catch (Throwable t) {
			return null;
		}
	}

	private static Getter getGetter(Property mappingProperty) {
		if ( mappingProperty == null || !mappingProperty.getPersistentClass().hasPojoRepresentation() ) {
			return null;
		}

		final PropertyAccessStrategyResolver propertyAccessStrategyResolver =
				mappingProperty.getPersistentClass().getServiceRegistry().getService( PropertyAccessStrategyResolver.class );

		final PropertyAccessStrategy propertyAccessStrategy = propertyAccessStrategyResolver.resolvePropertyAccessStrategy(
				mappingProperty.getPropertyAccessorName(),
				EntityMode.POJO
		);

		final PropertyAccess propertyAccess = propertyAccessStrategy.buildPropertyAccess(
				mappingProperty.getPersistentClass().getMappedClass(),
				mappingProperty.getName()
		);

		return propertyAccess.getGetter();
	}

}
