/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.EmbeddedIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.MultipleIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.SimpleIdMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.SingleIdMapper;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

/**
 * Generates metadata for primary identifiers (ids) of versions entities.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class IdMetadataGenerator {
	private final AuditMetadataGenerator mainGenerator;

	IdMetadataGenerator(AuditMetadataGenerator auditMetadataGenerator) {
		mainGenerator = auditMetadataGenerator;
	}

	@SuppressWarnings({"unchecked"})
	private boolean addIdProperties(
			Element parent,
			Iterator<Property> properties,
			SimpleMapperBuilder mapper,
			boolean key,
			boolean audited) {
		while ( properties.hasNext() ) {
			final Property property = properties.next();
			final Type propertyType = property.getType();
			if ( !"_identifierMapper".equals( property.getName() ) ) {
				boolean added = false;
				if ( propertyType instanceof ManyToOneType ) {
					added = mainGenerator.getBasicMetadataGenerator().addManyToOne(
							parent,
							getIdPersistentPropertyAuditingData( property ),
							property.getValue(),
							mapper
					);
				}
				else {
					// Last but one parameter: ids are always insertable
					added = mainGenerator.getBasicMetadataGenerator().addBasic(
							parent,
							getIdPersistentPropertyAuditingData( property ),
							property.getValue(),
							mapper,
							true,
							key
					);
				}
				if ( !added ) {
					// If the entity is audited, and a non-supported id component is used, throwing an exception.
					// If the entity is not audited, then we simply don't support this entity, even in
					// target relation mode not audited.
					if ( audited ) {
						throw new MappingException( "Type not supported: " + propertyType.getClass().getName() );
					}
					else {
						return false;
					}
				}
			}
		}

		return true;
	}

	@SuppressWarnings({"unchecked"})
	IdMappingData addId(PersistentClass pc, boolean audited) {
		// Xml mapping which will be used for relations
		final Element relIdMapping = new DefaultElement( "properties" );
		// Xml mapping which will be used for the primary key of the versions table
		final Element origIdMapping = new DefaultElement( "composite-id" );

		final Property idProp = pc.getIdentifierProperty();
		final Component idMapper = pc.getIdentifierMapper();

		// Checking if the id mapping is supported
		if ( idMapper == null && idProp == null ) {
			return null;
		}

		SimpleIdMapperBuilder mapper;
		if ( idMapper != null ) {
			// Multiple id
			final Class componentClass = ReflectionTools.loadClass(
					( (Component) pc.getIdentifier() ).getComponentClassName(),
					mainGenerator.getClassLoaderService()
			);
			mapper = new MultipleIdMapper( componentClass, pc.getServiceRegistry() );
			if ( !addIdProperties(
					relIdMapping,
					(Iterator<Property>) idMapper.getPropertyIterator(),
					mapper,
					false,
					audited
			) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties(
					origIdMapping,
					(Iterator<Property>) idMapper.getPropertyIterator(),
					null,
					true,
					audited
			) ) {
				return null;
			}
		}
		else if ( idProp.isComposite() ) {
			// Embedded id
			final Component idComponent = (Component) idProp.getValue();
			final Class embeddableClass = ReflectionTools.loadClass(
					idComponent.getComponentClassName(),
					mainGenerator.getClassLoaderService()
			);
			mapper = new EmbeddedIdMapper( getIdPropertyData( idProp ), embeddableClass, pc.getServiceRegistry() );
			if ( !addIdProperties(
					relIdMapping,
					(Iterator<Property>) idComponent.getPropertyIterator(),
					mapper,
					false,
					audited
			) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties(
					origIdMapping,
					(Iterator<Property>) idComponent.getPropertyIterator(),
					null,
					true,
					audited
			) ) {
				return null;
			}
		}
		else {
			// Single id
			mapper = new SingleIdMapper( pc.getServiceRegistry() );

			// Last but one parameter: ids are always insertable
			mainGenerator.getBasicMetadataGenerator().addBasic(
					relIdMapping,
					getIdPersistentPropertyAuditingData( idProp ),
					idProp.getValue(),
					mapper,
					true,
					false
			);

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			mainGenerator.getBasicMetadataGenerator().addBasic(
					origIdMapping,
					getIdPersistentPropertyAuditingData( idProp ),
					idProp.getValue(),
					null,
					true,
					true
			);
		}

		origIdMapping.addAttribute( "name", mainGenerator.getVerEntCfg().getOriginalIdPropName() );

		// Adding a relation to the revision entity (effectively: the "revision number" property)
		mainGenerator.addRevisionInfoRelation( origIdMapping );

		return new IdMappingData( mapper, origIdMapping, relIdMapping );
	}

	private PropertyData getIdPropertyData(Property property) {
		return new PropertyData(
				property.getName(), property.getName(), property.getPropertyAccessorName(),
				ModificationStore.FULL
		);
	}

	private PropertyAuditingData getIdPersistentPropertyAuditingData(Property property) {
		return new PropertyAuditingData(
				property.getName(), property.getPropertyAccessorName(),
				ModificationStore.FULL, RelationTargetAuditMode.AUDITED, null, null, false
		);
	}
}
