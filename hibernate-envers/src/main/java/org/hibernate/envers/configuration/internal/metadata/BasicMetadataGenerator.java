/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;
import java.util.Properties;

import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.EmbeddedIdMapper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.Type;

import org.dom4j.Element;

/**
 * Generates metadata for basic properties: immutable types (including enums).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class BasicMetadataGenerator {

	@SuppressWarnings({"unchecked"})
	boolean addBasic(
			Element parent, PropertyAuditingData propertyAuditingData,
			Value value, SimpleMapperBuilder mapper, boolean insertable, boolean key) {
		final Type type = value.getType();

		if ( type instanceof BasicType
				|| type instanceof SerializableToBlobType
				|| "org.hibernate.type.PrimitiveByteArrayBlobType".equals( type.getClass().getName() ) ) {
			if ( parent != null ) {
				final boolean addNestedType = (value instanceof SimpleValue)
						&& ((SimpleValue) value).getTypeParameters() != null;

				String typeName = type.getName();
				if ( typeName == null ) {
					typeName = type.getClass().getName();
				}

				final Element propMapping = MetadataTools.addProperty(
						parent,
						propertyAuditingData.getName(),
						addNestedType ? null : typeName,
						propertyAuditingData.isForceInsertable() || insertable,
						key
				);
				MetadataTools.addColumns( propMapping, value.getColumnIterator() );

				if ( addNestedType ) {
					final Properties typeParameters = ((SimpleValue) value).getTypeParameters();
					final Element typeMapping = propMapping.addElement( "type" );
					typeMapping.addAttribute( "name", typeName );

					if ( "org.hibernate.type.EnumType".equals( typeName ) ) {
						// Proper handling of enumeration type
						mapEnumerationType( typeMapping, type, typeParameters );
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

			// A null mapper means that we only want to add xml mappings
			if ( mapper != null ) {
				mapper.add( propertyAuditingData.getPropertyData() );
			}
		}
		else {
			return false;
		}

		return true;
	}

	private void mapEnumerationType(Element parent, Type type, Properties parameters) {
		if ( parameters.getProperty( EnumType.ENUM ) != null ) {
			parent.addElement( "param" )
					.addAttribute( "name", EnumType.ENUM )
					.setText( parameters.getProperty( EnumType.ENUM ) );
		}
		else {
			parent.addElement( "param" ).addAttribute( "name", EnumType.ENUM ).setText(
					type.getReturnedClass()
							.getName()
			);
		}
		if ( parameters.getProperty( EnumType.NAMED ) != null ) {
			parent.addElement( "param" ).addAttribute( "name", EnumType.NAMED ).setText(
					parameters.getProperty(
							EnumType.NAMED
					)
			);
		}
		else {
			parent.addElement( "param" ).addAttribute( "name", EnumType.NAMED )
					.setText( "" + !((EnumType) ((CustomType) type).getUserType()).isOrdinal() );
		}
	}
	
	public boolean addComponent(Element parent, String beanName, Value value, SimpleMapperBuilder mapper, String path) {

		Iterator it = ( (Component) value ).getPropertyIterator();
		while ( it.hasNext() ) {
			Property property = (Property) it.next();
			if ( property.getType() instanceof ComponentType ) {
				String realPath = getPropertyPath( beanName, path, property );
				addComponent( parent, beanName + "_" + property.getName(), property.getValue(), mapper, realPath );
			}
			else {
				Element element = parent.addElement( "key-property" );
				element.addAttribute( "name", beanName + "_" + property.getName() );
				element.addAttribute( "type", property.getType().getName() );
				MetadataTools.addColumns( element, property.getColumnIterator() );
				if ( mapper != null ) {
					addIdToMapper( beanName, mapper, path, property );
				}
			}
		}

		return true;
	}

	/**
	 * This method returns the path of the property
	 * 
	 * @param beanName
	 * @param path
	 * @param property
	 * @return
	 */
	private String getPropertyPath(String beanName, String path, Property property) {
		String realPath;
		if ( path != null && !path.isEmpty() ) {
			realPath = path + "." + beanName;
		}
		else {
			realPath = beanName + "." + property.getName();
		}
		return realPath;
	}

	/**
	 * This method add the id to Mapper
	 * 
	 * @param beanName
	 * @param mapper
	 * @param path
	 * @param property
	 */
	private void addIdToMapper(String beanName, SimpleMapperBuilder mapper, String path, Property property) {
		PropertyData propertyData = new PropertyAuditingData( beanName, property.getName(), property.getPropertyAccessorName(),
				ModificationStore.FULL, RelationTargetAuditMode.NOT_AUDITED, null, null, false )
						.getPropertyData();
		if ( mapper instanceof EmbeddedIdMapper ) {
			String newPath = path;
			if ( path.isEmpty() ) {
				newPath = beanName + "." + property.getName();
			}
			else {
				newPath = path + "." + property.getName();
			}
			( (EmbeddedIdMapper) mapper ).add( propertyData, beanName + "_", newPath );
		}
		else {
			mapper.add( propertyData );
		}
	}
	
	@SuppressWarnings({"unchecked"})
	boolean addManyToOne(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleMapperBuilder mapper) {
		final Type type = value.getType();

		// A null mapper occurs when adding to composite-id element
		final Element manyToOneElement = parent.addElement( mapper != null ? "many-to-one" : "key-many-to-one" );
		manyToOneElement.addAttribute( "name", propertyAuditingData.getName() );
		manyToOneElement.addAttribute( "class", type.getName() );
		MetadataTools.addColumns( manyToOneElement, value.getColumnIterator() );

		// A null mapper means that we only want to add xml mappings
		if ( mapper != null ) {
			mapper.add( propertyAuditingData.getPropertyData() );
		}

		return true;
	}
}
