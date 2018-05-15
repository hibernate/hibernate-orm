/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Properties;
import javax.persistence.EnumType;

import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import org.dom4j.Element;

/**
 * Generates metadata for basic properties: immutable types (including enums).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class BasicMetadataGenerator {

	private static final String ENUM = "enumClass";
	private static final String NAMED = "useNamed";
	private static final String TYPE = "type";

	@SuppressWarnings({"unchecked"})
	boolean addBasic(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			ValueMapping value,
			SimpleMapperBuilder mapper,
			boolean insertable,
			boolean key) {

		if ( value instanceof BasicValue ) {
			final BasicValue basicValue = (BasicValue) value;
			if ( parent != null ) {
				final Element propMapping = buildProperty(
						parent,
						propertyAuditingData,
						basicValue,
						insertable,
						key
				);

				if ( isAddNestedType( basicValue ) ) {
					applyNestedType( basicValue, propMapping );
				}
			}

			// A null mapper means that we only want to add xml mappings
			if ( mapper != null ) {
				mapper.add( propertyAuditingData.resolvePropertyData( value.getJavaTypeMapping().getJavaTypeDescriptor() ) );
			}
			return true;
		}

		return false;
	}

	private void mapEnumerationValue(Element parent, BasicValue value, Properties parameters) {
		final String enumClass;
		if ( parameters.getProperty( ENUM ) != null ) {
			enumClass = parameters.getProperty( ENUM );
		}
		else {
			enumClass = value.getJavaTypeMapping().getTypeName();
		}
		parent.addElement( "param" ).addAttribute( "name", ENUM ).setText( enumClass );

		final String useNamed;
		if ( parameters.getProperty( NAMED ) != null ) {
			useNamed = parameters.getProperty( NAMED );
		}
		else {
			final SqlTypeDescriptor descriptor = value.resolveType().getSqlTypeDescriptor();
			useNamed = descriptor.equals( IntegerSqlDescriptor.INSTANCE ) ? "false" : "true";
		}
		parent.addElement( "param" ).addAttribute( "name", NAMED ).setText( useNamed );
	}

	@SuppressWarnings({"unchecked"})
	boolean addManyToOne(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			ValueMapping value,
			SimpleMapperBuilder mapper) {

		// A null mapper occurs when adding to composite-id element
		final Element manyToOneElement = parent.addElement( mapper != null ? "many-to-one" : "key-many-to-one" );
		manyToOneElement.addAttribute( "name", propertyAuditingData.getName() );
		manyToOneElement.addAttribute( "class", value.getJavaTypeMapping().getTypeName() );

		// HHH-11107
		// Use FK hbm magic value 'none' to skip making foreign key constraints between the Envers
		// schema and the base table schema when a @ManyToOne is present in an identifier.
		if ( mapper == null ) {
			manyToOneElement.addAttribute( "foreign-key", "none" );
		}

		MetadataTools.addColumns( manyToOneElement, value.getMappedColumns() );

		// A null mapper means that we only want to add xml mappings
		if ( mapper != null ) {
			mapper.add( propertyAuditingData.resolvePropertyData( value.getJavaTypeMapping().getJavaTypeDescriptor() ) );
		}

		return true;
	}

	private boolean isAddNestedType(BasicValue basicValue) {
		return basicValue.getTypeParameters() != null;
	}

	private Element buildProperty(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			BasicValue value,
			boolean insertable,
			boolean key) {
		final Element propMapping = MetadataTools.addProperty(
				parent,
				propertyAuditingData.getName(),
				isAddNestedType( value )
						? null
						: value.getJavaTypeMapping().getTypeName(),
				propertyAuditingData.isForceInsertable() || insertable,
				key
		);

		MetadataTools.addColumns( propMapping, value.getMappedColumns() );

		return propMapping;
	}

	private void applyNestedType(BasicValue value, Element propertyMapping) {
		final Properties typeParameters = value.getTypeParameters();
		final Element typeMapping = propertyMapping.addElement( "type" );
		final String typeName = value.getJavaTypeMapping().getTypeName();

		typeMapping.addAttribute( "name", typeName );

		if ( EnumType.class.getName().equals( typeName ) ) {
			// Proper handling of enumeration type
			mapEnumerationValue( typeMapping, value, typeParameters );
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
}
