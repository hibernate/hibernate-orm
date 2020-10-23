/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.Value;

/**
 * @author Steve Ebersole
 */
public class ObjectFactory {
	public static MetamodelClass metamodelClass(PersistentClass entityDescriptor) {
		return new MetamodelClass( entityDescriptor.getMappedClass().getName(), determineSuperTypeName( entityDescriptor ) );
	}

	private static String determineSuperTypeName(PersistentClass entityDescriptor) {
		if ( entityDescriptor.getSuperMappedSuperclass() != null ) {
			return entityDescriptor.getSuperMappedSuperclass().getMappedClass().getName();
		}

		if ( entityDescriptor.getSuperclass() != null ) {
			return entityDescriptor.getSuperclass().getMappedClass().getName();
		}

		return null;
	}

	public static MetamodelClass metamodelClass(MappedSuperclass mappedSuperclassDescriptor) {
		return new MetamodelClass( mappedSuperclassDescriptor.getMappedClass().getName(), ObjectFactory.determineSuperTypeName( mappedSuperclassDescriptor ) );
	}

	private static String determineSuperTypeName(MappedSuperclass mappedSuperclassDescriptor) {
		if ( mappedSuperclassDescriptor.getSuperMappedSuperclass() != null ) {
			return mappedSuperclassDescriptor.getSuperMappedSuperclass().getMappedClass().getName();
		}

		if ( mappedSuperclassDescriptor.getSuperPersistentClass() != null ) {
			return mappedSuperclassDescriptor.getSuperPersistentClass().getMappedClass().getName();
		}


		return null;
	}

	public static MetamodelAttribute attribute(Property property, Value propertyValueMapping, MetamodelClass metamodelClass) {
		if ( propertyValueMapping instanceof DependantValue ) {
			final DependantValue dependantValue = (DependantValue) propertyValueMapping;
			final KeyValue wrappedValue = dependantValue.getWrappedValue();
			return attribute( property, wrappedValue, metamodelClass );
		}

		if ( propertyValueMapping instanceof Set ) {
			return setAttribute( property, (Set) propertyValueMapping, metamodelClass );
		}

		if ( propertyValueMapping instanceof Collection ) {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}


		return new AttributeSingular(
				metamodelClass,
				property.getName(),
				propertyValueMapping.getType().getReturnedClass()
		);
	}

	private static AttributeSet setAttribute(Property property, Set valueMapping, MetamodelClass metamodelClass) {
		return new AttributeSet(
				metamodelClass,
				property.getName(),
				valueMapping.getElement().getType().getReturnedClass()
		);
	}
}
