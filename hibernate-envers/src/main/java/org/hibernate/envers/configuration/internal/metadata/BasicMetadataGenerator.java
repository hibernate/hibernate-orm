/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;

import org.dom4j.Element;

/**
 * Generates metadata for basic properties: immutable types (including enums).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class BasicMetadataGenerator {

	private final Metadata metadata;

	public BasicMetadataGenerator(AuditMetadataGenerator mainGenerator) {
		this.metadata = mainGenerator.getMetadata();
	}

	boolean addBasic(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleMapperBuilder mapper,
			boolean insertable,
			boolean key) {

		if ( value.getType() instanceof BasicType ) {
			if ( parent != null ) {
				final Element propMapping = buildProperty(
						parent,
						propertyAuditingData,
						value,
						insertable,
						key
				);

				if ( isAddNestedType( value ) ) {
					applyNestedType( (SimpleValue) value, propMapping );
				}
			}

			// A null mapper means that we only want to add xml mappings
			if ( mapper != null ) {
				final PropertyData propertyData = propertyAuditingData.resolvePropertyData( value.getType() );
				mapper.add( propertyData );
			}

			return true;
		}

		return false;
	}

	private void mapEnumerationType(Element parent, Type type, Properties parameters) {
		if ( parameters.getProperty( EnumType.ENUM ) != null ) {
			parent.addElement( "param" )
					.addAttribute( "name", EnumType.ENUM )
					.setText( parameters.getProperty( EnumType.ENUM ) );
		}
		else {
			parent.addElement( "param" )
					.addAttribute( "name", EnumType.ENUM )
					.setText( type.getReturnedClass().getName() );
		}
		if ( parameters.getProperty( EnumType.NAMED ) != null ) {
			parent.addElement( "param" )
					.addAttribute( "name", EnumType.NAMED )
					.setText( parameters.getProperty( EnumType.NAMED ) );
		}
		else {
			parent.addElement( "param" )
					.addAttribute( "name", EnumType.NAMED )
					.setText( "" + !( (EnumType) ( (CustomType) type ).getUserType() ).isOrdinal() );
		}
	}

	private boolean isAddNestedType(Value value) {
		if ( value instanceof SimpleValue ) {
			if ( ( (SimpleValue) value ).getTypeParameters() != null ) {
				return true;
			}
		}
		return false;
	}

	private Element buildProperty(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			Value value,
			boolean insertable,
			boolean key) {
		final Element propMapping = MetadataTools.addProperty(
				parent,
				propertyAuditingData.getName(),
				isAddNestedType( value ) ? null : getBasicTypeName( value.getType() ),
				propertyAuditingData.isForceInsertable() || insertable,
				key
		);

		MetadataTools.addColumns( propMapping, value.getColumnIterator() );

		return propMapping;
	}

	private void applyNestedType(SimpleValue value, Element propertyMapping) {
		final Properties typeParameters = value.getTypeParameters();
		final Element typeMapping = propertyMapping.addElement( "type" );
		final String typeName = getBasicTypeName( value.getType() );

		typeMapping.addAttribute( "name", typeName );

		if ( isEnumType( value.getType(), typeName ) ) {
			// Proper handling of enumeration type
			mapEnumerationType( typeMapping, value.getType(), typeParameters );
		}
		else {
			// By default copying all Hibernate properties
			for ( Object object : typeParameters.keySet() ) {
				final String keyType = (String) object;
				final String property = typeParameters.getProperty( keyType );
				if ( property != null ) {
					typeMapping.addElement( "param" ).addAttribute( "name", keyType ).setText( property );
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
		// Check if a custom type implementation is used and it extends the EnumType directly.
		if ( CustomType.class.isInstance( type ) ) {
			final CustomType customType = (CustomType) type;
			if ( EnumType.class.isInstance( customType.getUserType() ) ) {
				return true;
			}
		}

		// Check if its an EnumType without a custom type
		if ( EnumType.class.getName().equals( typeName ) ) {
			return true;
		}

		return false;
	}
}
