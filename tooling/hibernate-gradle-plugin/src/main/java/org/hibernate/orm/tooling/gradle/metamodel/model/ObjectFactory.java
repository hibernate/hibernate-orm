/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.util.Locale;
import java.util.function.Consumer;

import org.gradle.api.GradleException;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * @author Steve Ebersole
 */
public class ObjectFactory {
	private final MetadataImplementor metadata;

	public ObjectFactory(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	public MetamodelClass metamodelClass(PersistentClass entityDescriptor) {
		return new MetamodelClass( entityDescriptor.getMappedClass().getName(), determineSuperTypeName( entityDescriptor ) );
	}

	private String determineSuperTypeName(PersistentClass entityDescriptor) {
		if ( entityDescriptor.getSuperMappedSuperclass() != null ) {
			return entityDescriptor.getSuperMappedSuperclass().getMappedClass().getName();
		}

		if ( entityDescriptor.getSuperclass() != null ) {
			return entityDescriptor.getSuperclass().getMappedClass().getName();
		}

		return null;
	}

	public MetamodelClass metamodelClass(MappedSuperclass mappedSuperclassDescriptor) {
		return new MetamodelClass( mappedSuperclassDescriptor.getMappedClass().getName(), determineSuperTypeName( mappedSuperclassDescriptor ) );
	}

	private String determineSuperTypeName(MappedSuperclass mappedSuperclassDescriptor) {
		if ( mappedSuperclassDescriptor.getSuperMappedSuperclass() != null ) {
			return mappedSuperclassDescriptor.getSuperMappedSuperclass().getMappedClass().getName();
		}

		if ( mappedSuperclassDescriptor.getSuperPersistentClass() != null ) {
			return mappedSuperclassDescriptor.getSuperPersistentClass().getMappedClass().getName();
		}


		return null;
	}

	public MetamodelClass metamodelClass(Component embeddedMapping) {
		return new MetamodelClass( embeddedMapping.getComponentClassName(), null );
	}

	public MetamodelAttribute attribute(
			Property property,
			Value propertyValueMapping,
			MetamodelClass metamodelClass,
			Consumer<Component> componentConsumer) {
		if ( propertyValueMapping instanceof DependantValue ) {
			final DependantValue dependantValue = (DependantValue) propertyValueMapping;
			final KeyValue wrappedValue = dependantValue.getWrappedValue();
			return attribute( property, wrappedValue, metamodelClass, componentConsumer );
		}

		if ( propertyValueMapping instanceof Collection ) {
			return pluralAttribute( property, (Collection) propertyValueMapping, metamodelClass, componentConsumer );
		}

		final Class<?> propertyJavaType = determineSingularJavaType( property, propertyValueMapping, metamodelClass, componentConsumer );
		return new AttributeSingular( metamodelClass, property.getName(), propertyJavaType );
	}

	private Class<?> determineSingularJavaType(
			Property property,
			Value propertyValueMapping,
			MetamodelClass metamodelClass,
			Consumer<Component> componentConsumer) {
		if ( propertyValueMapping instanceof BasicValue ) {
			final BasicValue basicValue = (BasicValue) propertyValueMapping;
			return basicValue.resolve().getDomainJavaType().getJavaTypeClass();
		}

		if ( propertyValueMapping instanceof Component ) {
			final Component component = (Component) propertyValueMapping;
			componentConsumer.accept( component );
			return component.getComponentClass();
		}

		if ( propertyValueMapping instanceof Any ) {
			return Object.class;
		}

		if ( propertyValueMapping instanceof ToOne ) {
			final ToOne toOne = (ToOne) propertyValueMapping;
			final String referencedEntityName = toOne.getReferencedEntityName();
			final PersistentClass entityBinding = metadata.getEntityBinding( referencedEntityName );
			final Class<?> mappedClass = entityBinding.getMappedClass();
			if ( mappedClass == null ) {
				throw new GradleException(
						String.format(
								Locale.ROOT,
								"Could not determine ToOne java type : %s#%s",
								metamodelClass.getDomainClassName(),
								property.getName()
						)
				);
			}
			return mappedClass;
		}

		propertyValueMapping.setTypeUsingReflection( metamodelClass.getDomainClassName(), property.getName() );
		return propertyValueMapping.getType().getReturnedClass();
	}

	private MetamodelAttribute pluralAttribute(
			Property property,
			Collection collectionMapping,
			MetamodelClass metamodelClass,
			Consumer<Component> componentConsumer) {
		if ( collectionMapping instanceof Set ) {
			return new AttributeSet(
					metamodelClass,
					property.getName(),
					determineCollectionPartJavaType( property, collectionMapping.getElement(), metamodelClass, componentConsumer )
			);
		}

		if ( collectionMapping instanceof Bag ) {
			return new AttributeBag(
					metamodelClass,
					property.getName(),
					determineCollectionPartJavaType( property, collectionMapping.getElement(), metamodelClass, componentConsumer )
			);
		}

		if ( collectionMapping instanceof List ) {
			return new AttributeList(
					metamodelClass,
					property.getName(),
					determineCollectionPartJavaType( property, collectionMapping.getElement(), metamodelClass, componentConsumer )
			);
		}

		if ( collectionMapping instanceof Map ) {
			return new AttributeMap(
					metamodelClass,
					property.getName(),
					determineCollectionPartJavaType( property, ( (Map) collectionMapping ).getIndex(), metamodelClass, componentConsumer ),
					determineCollectionPartJavaType( property, collectionMapping.getElement(), metamodelClass, componentConsumer )
			);
		}

		throw new UnsupportedOperationException( "Unsupported plural value type : " + collectionMapping.getClass().getName() );
	}

	private Class<?> determineCollectionPartJavaType(
			Property property,
			Value partJavaType,
			MetamodelClass metamodelClass,
			Consumer<Component> componentConsumer) {
		if ( partJavaType instanceof DependantValue ) {
			final DependantValue dependantValue = (DependantValue) partJavaType;
			final KeyValue wrappedValue = dependantValue.getWrappedValue();
			return determineCollectionPartJavaType( property, wrappedValue, metamodelClass, componentConsumer );
		}

		if ( partJavaType instanceof BasicValue ) {
			final BasicValue basicValue = (BasicValue) partJavaType;
			return basicValue.resolve().getDomainJavaType().getJavaTypeClass();
		}

		if ( partJavaType instanceof Component ) {
			final Component component = (Component) partJavaType;
			componentConsumer.accept( component );
			return component.getComponentClass();
		}

		if ( partJavaType instanceof Any ) {
			return Object.class;
		}

		if ( partJavaType instanceof OneToMany ) {
			final OneToMany oneToMany = (OneToMany) partJavaType;
			final PersistentClass associatedClass = oneToMany.getAssociatedClass();
			return associatedClass.getMappedClass();
		}

		if ( partJavaType instanceof ToOne ) {
			final ToOne toOne = (ToOne) partJavaType;
			final String referencedEntityName = toOne.getReferencedEntityName();
			if ( referencedEntityName != null ) {
				final PersistentClass entityBinding = metadata.getEntityBinding( referencedEntityName );
				if ( entityBinding != null ) {
					final Class<?> mappedClass = entityBinding.getMappedClass();
					if ( mappedClass != null ) {
						return mappedClass;
					}
				}
			}
			throw new GradleException(
					String.format(
							Locale.ROOT,
							"Could not determine ToOne java type : %s#%s",
							metamodelClass.getDomainClassName(),
							property.getName()
					)
			);
		}

		return partJavaType.getType().getReturnedClass();
	}
}
