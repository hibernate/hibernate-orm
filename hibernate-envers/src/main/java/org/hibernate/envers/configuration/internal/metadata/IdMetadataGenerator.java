/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Locale;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.model.CompositeIdentifier;
import org.hibernate.envers.boot.model.Identifier;
import org.hibernate.envers.boot.model.IdentifierRelation;
import org.hibernate.envers.boot.model.ManyToOneAttribute;
import org.hibernate.envers.boot.registry.classloading.ClassLoaderAccessHelper;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.EmbeddedIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.id.MultipleIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.NestedEmbeddedIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.SimpleIdMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.SingleIdMapper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

/**
 * Generates metadata for primary identifiers (ids) of versions entities.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class IdMetadataGenerator extends AbstractMetadataGenerator {

	private final BasicMetadataGenerator basicMetadataGenerator;

	public IdMetadataGenerator(EnversMetadataBuildingContext metadataBuildingContext, BasicMetadataGenerator basicMetadataGenerator) {
		super( metadataBuildingContext );
		this.basicMetadataGenerator = basicMetadataGenerator;
	}

	private Class<?> loadClass(Component component) {
		return ClassLoaderAccessHelper.loadClass( getMetadataBuildingContext(), component.getComponentClassName() );
	}

	private static boolean isSameType(Property left, Property right) {
		return left.getType().getName().equals( right.getType().getName() );
	}

	private boolean addIdProperty(
			AttributeContainer attributeContainer,
			boolean key,
			SimpleIdMapperBuilder mapper,
			Property mappedProperty,
			Property virtualProperty,
			boolean audited) {

		if ( NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( mappedProperty.getName() ) ) {
			return false;
		}

		final PropertyAuditingData propertyAuditingData = getIdPersistentPropertyAuditingData( mappedProperty );

		if ( ManyToOneType.class.isInstance( mappedProperty.getType() ) ) {
			// This can technically be a @ManyToOne or logical @OneToOne
			final boolean added = addManyToOne( attributeContainer, propertyAuditingData, mappedProperty.getValue(), mapper );
			if ( added && mapper != null ) {
				if ( virtualProperty != null && !isSameType( mappedProperty, virtualProperty ) ) {
					// A virtual property is only available when an @IdClass is used.  We specifically need to map
					// both the value and virtual types when they differ so we can adequately map between them at
					// appropriate points.
					propertyAuditingData.setPropertyType( mappedProperty.getType() );
					propertyAuditingData.setVirtualPropertyType( virtualProperty.getType() );
					mapper.add( propertyAuditingData.resolvePropertyData() );
				}
				else {
					// In this branch the identifier either doesn't use an @IdClass or the property types between
					// the @IdClass and containing entity are identical, allowing us to use prior behavior.
					propertyAuditingData.setPropertyType( mappedProperty.getType() );
					mapper.add( propertyAuditingData.resolvePropertyData() );
				}
			}

			return added;
		}
		else if ( ComponentType.class.isInstance( mappedProperty.getType() ) ) {
			final Component component = (Component) mappedProperty.getValue();
			final NestedEmbeddedIdMapper nestedMapper;
			if ( mapper != null ) {
				final PropertyData propertyData = propertyAuditingData.resolvePropertyData();
				nestedMapper = new NestedEmbeddedIdMapper( propertyData, component );
				mapper.add( propertyData, nestedMapper );
			}
			else {
				nestedMapper = null;
			}
			return addIdProperties( attributeContainer, component, null, nestedMapper, key, audited );
		}

		return addBasic( attributeContainer, propertyAuditingData, mappedProperty.getValue(), mapper, key );
	}

	private boolean addIdProperties(
			AttributeContainer attributeContainer,
			Component component,
			Component virtualComponent,
			SimpleIdMapperBuilder mapper,
			boolean key,
			boolean audited) {
		for ( Property property : component.getProperties() ) {
			final Property virtualProperty = virtualComponent != null
					? virtualComponent.getProperty( property.getName() )
					: null;
			if ( !addIdProperty( attributeContainer, key, mapper, property, virtualProperty, audited ) ) {
				// If the entity is audited, and a non-supported id component is used, throw exception.
				if ( audited ) {
					throw new EnversMappingException(
							String.format(
									Locale.ROOT,
									"Type not supported: %s",
									property.getType().getClass().getName()
							)
					);
				}
				return false;
			}
		}
		return true;
	}

	public void generateSecondPass(String entityName, PersistentClass persistentClass) {
		final Component identifierMapper = persistentClass.getIdentifierMapper();
		final Property identifierProperty = persistentClass.getIdentifierProperty();
		if ( identifierMapper != null ) {
			generateSecondPass( entityName, identifierMapper );
		}
		else if ( identifierProperty != null && identifierProperty.isComposite() ) {
			final Component component = (Component) identifierProperty.getValue();
			generateSecondPass( entityName, component );
		}
	}

	private void generateSecondPass(String entityName, Component component) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof ToOne ) {
				final PropertyAuditingData propertyData = getIdPersistentPropertyAuditingData( property );
				final String referencedEntityName = ( (ToOne) value).getReferencedEntityName();

				final String prefix = getMetadataBuildingContext().getConfiguration()
						.getOriginalIdPropertyName() + "." + propertyData.getName();

				final IdMapper relMapper;
				if ( hasAuditedEntityConfiguration( referencedEntityName ) ) {
					relMapper = getAuditedEntityConfiguration( referencedEntityName ).getIdMapper();
				}
				else if ( hasNotAuditedEntityConfiguration( referencedEntityName ) ) {
					relMapper = getNotAuditedEntityConfiguration( referencedEntityName ).getIdMapper();
				}
				else {
					throw new EnversMappingException( "Unable to locate entity configuration for [" + referencedEntityName + "]" );
				}

				final IdMapper prefixedMapper = relMapper.prefixMappedProperties( prefix + "." );

				getAuditedEntityConfiguration( entityName ).addToOneRelation(
						prefix,
						referencedEntityName,
						prefixedMapper,
						true,
						false
				);
			}
		}
	}

	public IdMappingData addIdAndGetMappingData(PersistentClass persistentClass, boolean audited) {
		final Property idProp = persistentClass.getIdentifierProperty();
		final Component idMapper = persistentClass.getIdentifierMapper();

		Identifier identifier = new CompositeIdentifier( getMetadataBuildingContext() );
		IdentifierRelation relation = new IdentifierRelation();

		// Check if the id mapping is supported
		if ( idMapper == null && idProp == null ) {
			return null;
		}

		SimpleIdMapperBuilder mapper;
		if ( idMapper != null ) {
			// Multiple id
			final Component virtualComponent = (Component) persistentClass.getIdentifier();
			mapper = new MultipleIdMapper( virtualComponent );

			if ( !addIdProperties( relation, idMapper, virtualComponent, mapper, false, audited ) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties( identifier, idMapper, virtualComponent, null, true, audited ) ) {
				return null;
			}
		}
		else if ( idProp.isComposite() ) {
			// Embedded id
			final Component idComponent = (Component) idProp.getValue();
			mapper = new EmbeddedIdMapper( getIdPropertyData( idProp ), idComponent );

			if ( !addIdProperties( relation, idComponent, null, mapper, false, audited ) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties( identifier, idComponent, null, null, true, audited ) ) {
				return null;
			}
		}
		else {
			// Single id
			mapper = new SingleIdMapper( persistentClass.getServiceRegistry() );

			// Last but one parameter: ids are always insertable
			basicMetadataGenerator.addBasic(
					relation,
					getIdPersistentPropertyAuditingData( idProp ),
					idProp.getValue(),
					mapper,
					true,
					false
			);

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			basicMetadataGenerator.addBasic(
					identifier,
					getIdPersistentPropertyAuditingData( idProp ),
					idProp.getValue(),
					null,
					true,
					true
			);
		}

		// Adding a relation to the revision entity (effectively: the "revision number" property)
		addRevisionInfoRelationToIdentifier( identifier );

		return new IdMappingData( mapper, identifier, relation );
	}

	private PropertyData getIdPropertyData(Property property) {
		return new PropertyData(
				property.getName(),
				property.getName(),
				property.getPropertyAccessorName()
		);
	}

	private PropertyAuditingData getIdPersistentPropertyAuditingData(Property property) {
		return new PropertyAuditingData( property.getName(), property.getPropertyAccessorName(), false );
	}

	public boolean addManyToOne(
			AttributeContainer attributeContainer,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleMapperBuilder mapper) {
		final Type type = value.getType();

		// A null mapper occurs when adding to composite-id element
		final ManyToOneAttribute attribute = new ManyToOneAttribute(
				propertyAuditingData.getName(),
				"",
				true,
				true,
				mapper == null,
				type.getName()
		);

		// HHH-11107
		// Use FK hbm magic value 'none' to skip making foreign key constraints between the Envers
		// schema and the base table schema when a @ManyToOne is present in an identifier.
		attribute.setForeignKey( "none" );

		attribute.addColumnsFromValue( value );
		attributeContainer.addAttribute( attribute );

		return true;
	}

	public boolean addBasic(
			AttributeContainer attributeContainer,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleIdMapperBuilder mapper,
			boolean key) {
		return basicMetadataGenerator.addBasic(
				attributeContainer,
				propertyAuditingData,
				value,
				mapper,
				true,
				key
		);
	}
}
