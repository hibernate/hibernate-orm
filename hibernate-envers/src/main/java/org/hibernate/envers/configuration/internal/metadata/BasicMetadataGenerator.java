/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Properties;

import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.model.BasicAttribute;
import org.hibernate.envers.boot.model.TypeSpecification;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.internal.NamedEnumValueConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;

/**
 * Generates metadata for basic properties: immutable types (including enums).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class BasicMetadataGenerator {

	public BasicMetadataGenerator() {
	}

	public boolean addBasic(
			AttributeContainer attributeContainer,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleMapperBuilder mapper,
			boolean insertable,
			boolean key) {
		if ( value.getType() instanceof BasicType ) {
			if ( attributeContainer != null ) {
				BasicAttribute attribute = buildProperty(propertyAuditingData, value, insertable, key );
				attributeContainer.addAttribute( attribute );

				if ( isAddNestedType( value ) ) {
					applyNestedType( (SimpleValue) value, attribute );
				}
			}

			// A null mapper means that we only want to add xml mappings
			if ( mapper != null ) {
				propertyAuditingData.setPropertyType( value.getType() );
				mapper.add( propertyAuditingData.resolvePropertyData() );
			}

			return true;
		}

		return false;
	}

	private void mapEnumerationType(TypeSpecification typeDefinition, Type type, Properties parameters) {
		if ( parameters.getProperty( EnumType.ENUM ) != null ) {
			typeDefinition.setParameter( EnumType.ENUM, parameters.getProperty( EnumType.ENUM ) );
		}
		else {
			typeDefinition.setParameter( EnumType.ENUM, type.getReturnedClass().getName() );
		}
		if ( parameters.getProperty( EnumType.NAMED ) != null ) {
			typeDefinition.setParameter( EnumType.NAMED, parameters.getProperty( EnumType.NAMED ) );
		}
		else {
			final ConvertedBasicType<?> convertedType = (ConvertedBasicType<?>) type;
			final EnumValueConverter<?, ?> valueConverter = (EnumValueConverter<?, ?>) convertedType.getValueConverter();
			final boolean isNamed = valueConverter instanceof NamedEnumValueConverter;
			typeDefinition.setParameter( EnumType.NAMED, Boolean.toString( isNamed ) );
		}
	}

	private boolean isAddNestedType(Value value) {
		if ( value instanceof SimpleValue ) {
			return ((SimpleValue) value).getTypeParameters() != null;
		}
		return false;
	}

	private BasicAttribute buildProperty(PropertyAuditingData propertyAuditingData, Value value, boolean insertable, boolean key) {
		BasicAttribute attribute = new BasicAttribute(
				propertyAuditingData.getName(),
				isAddNestedType( value ) ? null : getBasicTypeName( value.getType() ),
				propertyAuditingData.isForceInsertable() || insertable,
				key
		);

		attribute.addColumnsFromValue( value );
		return attribute;
	}

	private void applyNestedType(SimpleValue value, BasicAttribute attribute) {
		final Properties typeParameters = value.getTypeParameters();
		final String typeName = getBasicTypeName( value.getType() );

		final TypeSpecification type = new TypeSpecification(typeName );
		attribute.setType( type );

		if ( isEnumType( value.getType(), typeName ) ) {
			// Proper handling of enumeration type
			mapEnumerationType( type, value.getType(), typeParameters );
		}
		else {
			// By default, copying all Hibernate properties
			for ( Object object : typeParameters.keySet() ) {
				final String keyType = (String) object;
				final String property = typeParameters.getProperty( keyType );
				if ( property != null ) {
					type.setParameter( keyType, property );
				}
			}
		}
	}

	private String getBasicTypeName(Type type) {
		String typeName = type.getName();
		if ( typeName == null ) {
			typeName = type.getClass().getName();
		}
		return typeName;
	}

	private boolean isEnumType(Type type, String typeName) {
		if ( type instanceof ConvertedBasicType ) {
			final ConvertedBasicType<?> convertedType = (ConvertedBasicType<?>) type;
			return convertedType.getValueConverter() instanceof EnumValueConverter;
		}
		return false;
	}
}
