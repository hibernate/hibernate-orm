/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.EmbeddedIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.id.MultipleIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.SimpleIdMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.id.SingleIdMapper;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.loader.PropertyPath;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

/**
 * Generates metadata for primary identifiers (ids) of versions entities.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class IdMetadataGenerator {
	private final AuditMetadataGenerator mainGenerator;

	IdMetadataGenerator(AuditMetadataGenerator auditMetadataGenerator) {
		mainGenerator = auditMetadataGenerator;
	}

	private Class<?> loadClass(Component component) {
		final String className = component.getComponentClassName();
		return ReflectionTools.loadClass( className, mainGenerator.getClassLoaderService() );
	}

	private static boolean isSameType(Property left, Property right) {
		return left.getType().getName().equals( right.getType().getName() );
	}

	private boolean addIdProperty(
			Element parent,
			boolean key,
			SimpleIdMapperBuilder mapper,
			Property mappedProperty,
			Property virtualProperty) {

		if ( PropertyPath.IDENTIFIER_MAPPER_PROPERTY.equals( mappedProperty.getName() ) ) {
			return false;
		}

		final PropertyAuditingData propertyAuditingData = getIdPersistentPropertyAuditingData( mappedProperty );

		if ( ManyToOneType.class.isInstance( mappedProperty.getType() ) ) {
			// This can technically be a @ManyToOne or logical @OneToOne
			final boolean added = addManyToOne( parent, propertyAuditingData, mappedProperty.getValue(), mapper );
			if ( added && mapper != null ) {
				if ( virtualProperty != null && !isSameType( mappedProperty, virtualProperty ) ) {
					// A virtual property is only available when an @IdClass is used.  We specifically need to map
					// both the value and virtual types when they differ so we can adequately map between them at
					// appropriate points.
					final Type valueType = mappedProperty.getType();
					final Type virtualValueType = virtualProperty.getType();
					mapper.add( propertyAuditingData.resolvePropertyData( valueType, virtualValueType ) );
				}
				else {
					// In this branch the identifier either doesn't use an @IdClass or the property types between
					// the @IdClass and containing entity are identical, allowing us to use prior behavior.
					mapper.add( propertyAuditingData.resolvePropertyData( mappedProperty.getType() ) );
				}
			}

			return added;
		}

		return addBasic( parent, propertyAuditingData, mappedProperty.getValue(), mapper, key );
	}

	private boolean addIdProperties(
			Element parent,
			Component component,
			Component virtualComponent,
			SimpleIdMapperBuilder mapper,
			boolean key,
			boolean audited) {
		final Iterator properties = component.getPropertyIterator();
		while ( properties.hasNext() ) {
			final Property property = (Property) properties.next();

			final Property virtualProperty;
			if ( virtualComponent != null ) {
				virtualProperty = virtualComponent.getProperty( property.getName() );
			}
			else {
				virtualProperty = null;
			}

			if ( !addIdProperty( parent, key, mapper, property, virtualProperty ) ) {
				// If the entity is audited, and a non-supported id component is used, throw exception.
				if ( audited ) {
					throw new MappingException(
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
		Iterator properties = component.getPropertyIterator();
		while ( properties.hasNext() ) {
			final Property property = (Property) properties.next();
			if ( property.getValue() instanceof ToOne ) {
				final PropertyAuditingData propertyData = getIdPersistentPropertyAuditingData( property );
				final String referencedEntityName = ( (ToOne) property.getValue() ).getReferencedEntityName();

				final String prefix = mainGenerator.getVerEntCfg().getOriginalIdPropName() + "." + propertyData.getName();

				final IdMapper relMapper;
				if ( mainGenerator.getEntitiesConfigurations().containsKey( referencedEntityName ) ) {
					relMapper = mainGenerator.getEntitiesConfigurations().get( referencedEntityName ).getIdMapper();
				}
				else if ( mainGenerator.getNotAuditedEntitiesConfigurations().containsKey( referencedEntityName ) ) {
					relMapper = mainGenerator.getNotAuditedEntitiesConfigurations().get( referencedEntityName ).getIdMapper();
				}
				else {
					throw new MappingException( "Unable to locate entity configuration for [" + referencedEntityName + "]" );
				}

				final IdMapper prefixedMapper = relMapper.prefixMappedProperties( prefix + "." );

				mainGenerator.getEntitiesConfigurations().get( entityName ).addToOneRelation(
						prefix,
						referencedEntityName,
						prefixedMapper,
						true,
						false
				);
			}
		}
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
			final Class componentClass = loadClass( (Component) pc.getIdentifier() );
			final Component virtualComponent = (Component) pc.getIdentifier();
			mapper = new MultipleIdMapper( componentClass, pc.getServiceRegistry() );
			if ( !addIdProperties(
					relIdMapping,
					idMapper,
					virtualComponent,
					mapper,
					false,
					audited
			) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties(
					origIdMapping,
					idMapper,
					virtualComponent,
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
			final Class embeddableClass = loadClass( idComponent );
			mapper = new EmbeddedIdMapper( getIdPropertyData( idProp ), embeddableClass, pc.getServiceRegistry() );
			if ( !addIdProperties(
					relIdMapping,
					idComponent,
					null,
					mapper,
					false,
					audited
			) ) {
				return null;
			}

			// null mapper - the mapping where already added the first time, now we only want to generate the xml
			if ( !addIdProperties(
					origIdMapping,
					idComponent,
					null,
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

		// HHH-11107
		// Use FK hbm magic value 'none' to skip making foreign key constraints between the Envers
		// schema and the base table schema when a @ManyToOne is present in an identifier.
		manyToOneElement.addAttribute( "foreign-key", "none" );

		MetadataTools.addColumns( manyToOneElement, value.getColumnIterator() );

		return true;
	}

	boolean addBasic(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleIdMapperBuilder mapper,
			boolean key) {
		return mainGenerator.getBasicMetadataGenerator().addBasic(
				parent,
				propertyAuditingData,
				value,
				mapper,
				true,
				key
		);
	}
}
