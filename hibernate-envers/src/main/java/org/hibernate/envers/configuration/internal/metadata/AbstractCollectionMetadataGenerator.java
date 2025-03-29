/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jakarta.persistence.EnumType;

import org.hibernate.envers.boot.model.Attribute;
import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.model.BasicAttribute;
import org.hibernate.envers.boot.model.Column;
import org.hibernate.envers.boot.model.CompositeIdentifier;
import org.hibernate.envers.boot.model.Identifier;
import org.hibernate.envers.boot.model.RootPersistentEntity;
import org.hibernate.envers.boot.registry.classloading.ClassLoaderAccessHelper;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditJoinTableData;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditedPropertiesReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PersistentPropertiesSource;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.BasicCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.CommonCollectionMapperData;
import org.hibernate.envers.internal.entities.mapper.relation.ListCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MapCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleMapKeyEnumeratedComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.SortedMapCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.SortedSetCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleDummyComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleEmbeddableComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleMapElementNotKeyComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleMapKeyIdComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleMapKeyPropertyComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleRelatedComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleSimpleComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.ListProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.MapProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.SetProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.SortedMapProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.SortedSetProxy;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Value;
import org.hibernate.type.BagType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ListType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MapType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.Type;

/**
 * Abstract base class for various collection-based metadata generators.
 *
 * @author Chris Cranford
 */
public abstract class AbstractCollectionMetadataGenerator extends AbstractMetadataGenerator {

	private final BasicMetadataGenerator basicMetadataGenerator;
	private final ValueMetadataGenerator valueMetadataGenerator;

	public AbstractCollectionMetadataGenerator(
			EnversMetadataBuildingContext metadataBuildingContext,
			BasicMetadataGenerator basicMetadataGenerator,
			ValueMetadataGenerator valueMetadataGenerator) {
		super( metadataBuildingContext );
		this.basicMetadataGenerator = basicMetadataGenerator;
		this.valueMetadataGenerator = valueMetadataGenerator;
	}

	/**
	 * Entry point for all collection-based metadata generators where the collection will
	 * be inspected and the appropriate boot and runtime mappings are generated.
	 *
	 * @param context the per-collection metadata context
	 */
	public abstract void addCollection(CollectionMetadataContext context);

	protected MiddleIdData createMiddleIdData(IdMappingData idMappingData, String prefix, String entityName) {
		return new MiddleIdData(
				getMetadataBuildingContext().getConfiguration(),
				idMappingData,
				prefix,
				entityName,
				hasAuditedEntityConfiguration( entityName )
		);
	}

	protected List<Attribute> getPrefixedIdAttributes(String prefix, ColumnNameIterator iterator, IdMappingData idMapping) {
		return idMapping.getRelation().getAttributesPrefixed( prefix, iterator, true, true );
	}

	protected void addAttributesToEntity(RootPersistentEntity entity, List<Attribute> attributes) {
		Identifier identifier = entity.getIdentifier();
		if ( identifier == null ) {
			identifier = new CompositeIdentifier( getMetadataBuildingContext() );
			entity.setIdentifier( identifier );
		}
		attributes.forEach( identifier::addAttribute );
	}

	protected CommonCollectionMapperData createCommonCollectionMapperData(
			CollectionMetadataContext context,
			String entityName,
			MiddleIdData idData,
			RelationQueryGenerator queryGenerator) {
		return new CommonCollectionMapperData(
				entityName,
				context.getPropertyAuditingData().resolvePropertyData(),
				idData,
				queryGenerator,
				context.getCollection().getRole()
		);
	}

	protected String getOrderBy(Collection collection) {
		final String orderBy = collection.getOrderBy();
		return orderBy == null ? collection.getManyToManyOrdering() : orderBy;
	}

	protected String getOrderByCollectionRole(Collection collection, String orderBy) {
		return orderBy == null ? null : collection.getRole();
	}

	private boolean hasNoMapKeyOrNonEntityEnumeration(String entityName, PropertyAuditingData propertyAuditingData) {
		final String mapKey = propertyAuditingData.getMapKey();
		final EnumType mapKeyEnumType = propertyAuditingData.getMapKeyEnumType();
		return ( mapKey == null && mapKeyEnumType == null ) || ( mapKeyEnumType != null && entityName == null );
	}

	private boolean hasMapKey(PropertyAuditingData propertyAuditingData) {
		final String mapKey = propertyAuditingData.getMapKey();
		return mapKey != null && mapKey.isEmpty();
	}

	private boolean hasMapKeyEnum(PropertyAuditingData propertyAuditingData) {
		return propertyAuditingData.getMapKeyEnumType() != null;
	}

	private int getCurrentIndex(QueryGeneratorBuilder queryGeneratorBuilder) {
		return queryGeneratorBuilder == null ? 0 : queryGeneratorBuilder.getCurrentIndex();
	}

	protected MiddleComponentData addIndex(
			CollectionMetadataContext context,
			RootPersistentEntity middleEntity,
			QueryGeneratorBuilder queryGeneratorBuilder) {
		if ( context.getCollection().isIndexed() ) {
			final String referencedEntityName = context.getReferencedEntityName();
			final PropertyAuditingData propertyAuditingData = context.getPropertyAuditingData();
			if ( hasNoMapKeyOrNonEntityEnumeration( referencedEntityName, propertyAuditingData ) ) {
				// This entity doesn't specify a jakarta.persistence.MapKey or there is a MapKeyEnumerated but its a non-entity type.
				// Mapping it to the middle entity.
				return addValueToMiddleTable(
						context,
						( (IndexedCollection ) context.getCollection() ).getIndex(),
						middleEntity,
						queryGeneratorBuilder,
						"mapkey",
						null,
						true
				);
			}
			else if ( hasMapKeyEnum( propertyAuditingData ) ) {
				return new MiddleComponentData(
						new MiddleMapKeyEnumeratedComponentMapper( propertyAuditingData.getName() ),
						getCurrentIndex( queryGeneratorBuilder )
				);
			}
			else {
				final IdMappingData referencedIdMapping = getAuditedEntityConfiguration( referencedEntityName ).getIdMappingData();

				final MiddleComponentMapper middleComponentMapper;
				if ( hasMapKey( propertyAuditingData ) ) {
					// The key of the map is the id of the entity.
					middleComponentMapper = new MiddleMapKeyIdComponentMapper(
							getMetadataBuildingContext().getConfiguration(),
							referencedIdMapping.getIdMapper()
					);
				}
				else {
					// The key of the map is a property of the entity.
					middleComponentMapper = new MiddleMapKeyPropertyComponentMapper(
							propertyAuditingData.getMapKey(),
							propertyAuditingData.getAccessType()
					);
				}

				return new MiddleComponentData( middleComponentMapper, getCurrentIndex( queryGeneratorBuilder ) );
			}
		}
		else {
			// No index - creating a dummy mapper.
			return new MiddleComponentData( new MiddleDummyComponentMapper() );
		}
	}

	protected MiddleComponentData addValueToMiddleTable(
			CollectionMetadataContext context,
			Value value,
			RootPersistentEntity entity,
			QueryGeneratorBuilder queryGeneratorBuilder,
			String prefix,
			AuditJoinTableData joinTableData,
			boolean key) {
		final Type type = value.getType();
		if ( type instanceof ManyToOneType ) {
			return addManyToOneValueToMiddleTable(
					context,
					entity,
					value,
					joinTableData,
					prefix,
					queryGeneratorBuilder
			);
		}
		else if ( type instanceof ComponentType ) {
			return addComponentValueToMiddleTable( context, entity, (Component) value, prefix );
		}
		else {
			final MiddleComponentData middleComponentData = addBasicValueToMiddleTable(entity, value, prefix, key );
			if ( middleComponentData == null ) {
				throwUnsupportedTypeException( type, context.getReferencingEntityName(), context.getPropertyName() );
			}

			return middleComponentData;
		}
	}

	private MiddleComponentData addManyToOneValueToMiddleTable(
			CollectionMetadataContext context,
			RootPersistentEntity entity,
			Value value,
			AuditJoinTableData joinTableData,
			String prefix,
			QueryGeneratorBuilder queryGeneratorBuilder) {
		final String prefixRelated = prefix + "_";

		final String referencedEntityName = MappingTools.getReferencedEntityName( value );

		final IdMappingData referencedIdMapping = getReferencedIdMappingData(
				context.getReferencingEntityName(),
				referencedEntityName,
				context.getPropertyAuditingData(),
				true
		);

		// Adding related-entity (in this case: the referenced entities id) id mapping to the xml only if the
		// relation isn't inverse (so when <code>xmlMapping</code> is not null).
		if ( entity != null ) {
			final ColumnNameIterator columnNameIterator;
			if ( joinTableData != null && !joinTableData.getInverseJoinColumnNames().isEmpty() ) {
				columnNameIterator = joinTableData.getInverseJoinColumnNamesIterator();
			}
			else {
				columnNameIterator = ColumnNameIterator.from( value.getSelectables().iterator() );
			}

			addAttributesToEntity(
					entity,
					getPrefixedIdAttributes(
							prefixRelated,
							columnNameIterator,
							referencedIdMapping
					)
			);
		}

		// Storing the id data of the referenced entity: original mapper, prefixed mapper and entity name.
		final MiddleIdData referencedIdData = createMiddleIdData(
				referencedIdMapping,
				prefixRelated,
				referencedEntityName
		);
		// And adding it to the generator builder.
		queryGeneratorBuilder.addRelation( referencedIdData );

		return new MiddleComponentData(
				new MiddleRelatedComponentMapper( referencedIdData ),
				queryGeneratorBuilder.getCurrentIndex()
		);
	}

	private MiddleComponentData addComponentValueToMiddleTable(
			CollectionMetadataContext context,
			RootPersistentEntity entity,
			Component component,
			String prefix) {
		// Collection of embeddable elements.
		final MiddleEmbeddableComponentMapper componentMapper = new MiddleEmbeddableComponentMapper(
				new MultiPropertyMapper(),
				ClassLoaderAccessHelper.loadClass(
						getMetadataBuildingContext(),
						component.getComponentClassName()
				)
		);

		final ComponentAuditingData auditData = new ComponentAuditingData();

		new ComponentAuditedPropertiesReader(
				getMetadataBuildingContext(),
				PersistentPropertiesSource.forComponent( getMetadataBuildingContext(), component ),
				auditData
		).read();

		// Emulating first pass.
		for ( String auditedPropertyName : auditData.getPropertyNames() ) {
			final PropertyAuditingData nestedAuditingData = auditData.getPropertyAuditingData( auditedPropertyName );
			valueMetadataGenerator.addValue(
					entity,
					component.getProperty( auditedPropertyName ).getValue(),
					component.getProperty( auditedPropertyName ).getPropertyAccessStrategy(),
					componentMapper,
					prefix,
					context.getEntityMappingData(),
					nestedAuditingData,
					true,
					true,
					true
			);
		}

		// Emulating second pass so that the relations can be mapped too.
		for ( String auditedPropertyName : auditData.getPropertyNames() ) {
			final PropertyAuditingData nestedAuditingData = auditData.getPropertyAuditingData( auditedPropertyName );
			valueMetadataGenerator.addValue(
					entity,
					component.getProperty( auditedPropertyName ).getValue(),
					component.getProperty( auditedPropertyName ).getPropertyAccessStrategy(),
					componentMapper,
					context.getReferencingEntityName(),
					context.getEntityMappingData(),
					nestedAuditingData,
					true,
					false,
					true
			);
		}

		// Add a column holding a number to make each entry unique within the set.
		// Embeddable properties may contain null values, so cannot be stored within composite primary key.
		if ( context.getCollection().isSet() ) {
			final String setOrdinalPropertyName = getMetadataBuildingContext().getConfiguration()
					.getEmbeddableSetOrdinalPropertyName();

			final BasicAttribute ordinalProperty = new BasicAttribute(
					setOrdinalPropertyName,
					"integer",
					true,
					true
			);

			ordinalProperty.addColumn( new Column( setOrdinalPropertyName ) );
			entity.getIdentifier().addAttribute( ordinalProperty );
		}

		return new MiddleComponentData( componentMapper );
	}

	/**
	 * Adds basic value to the middle table.
	 *
	 * @param entity the middle table entity
	 * @param value the value to be added
	 * @param prefix the property name
	 * @param key whether the property is a key attribute or not
	 * @return the component data or {@code null} if the value could not be added.
	 */
	private MiddleComponentData addBasicValueToMiddleTable(RootPersistentEntity entity, Value value, String prefix, boolean key) {
		// Last but one parameter: collection components are always insertable
		AttributeContainer attributeContainer = entity;
		if ( entity != null && key ) {
			attributeContainer = entity.getIdentifier();
		}

		final boolean mapped = basicMetadataGenerator.addBasic(
				attributeContainer,
				new PropertyAuditingData( prefix, "field", false ),
				value,
				null,
				true,
				key
		);

		if ( mapped ) {
			final MiddleComponentMapper mapper;
			if ( key ) {
				// Simple values are always stored in the first item of the array returned by the query generator.
				final Configuration configuration = getMetadataBuildingContext().getConfiguration();
				mapper = new MiddleSimpleComponentMapper( configuration, prefix );
			}
			else {
				// when mapped but not part of the key, its stored as a dummy mapper??
				mapper = new MiddleMapElementNotKeyComponentMapper( prefix );
			}
			return new MiddleComponentData( mapper );
		}

		// could not be mapped
		// caller should check for this and assert if applicable.
		return null;
	}

	protected void addMapper(
			CollectionMetadataContext context,
			CommonCollectionMapperData commonCollectionMapperData,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		context.getMapperBuilder().addComposite(
				context.getPropertyAuditingData().resolvePropertyData(),
				resolvePropertyMapper(
						context,
						commonCollectionMapperData,
						elementComponentData,
						indexComponentData
				)
		);
	}

	private PropertyMapper resolvePropertyMapper(
			CollectionMetadataContext context,
			CommonCollectionMapperData commonCollectionMapperData,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {

		final String referencingEntityName = context.getReferencingEntityName();
		final boolean embeddableElementType = isEmbeddableElementType( context );
		final boolean lobMapElementType = isLobMapElementType( context );
		final Type collectionType = context.getCollection().getType();

		if ( collectionType instanceof SortedSetType ) {
			return new SortedSetCollectionMapper(
					getMetadataBuildingContext().getConfiguration(),
					commonCollectionMapperData,
					TreeSet.class,
					SortedSetProxy.class,
					elementComponentData,
					context.getCollection().getComparator(),
					embeddableElementType,
					embeddableElementType
			);
		}
		else if ( collectionType instanceof SetType ) {
			return new BasicCollectionMapper<Set>(
					getMetadataBuildingContext().getConfiguration(),
					commonCollectionMapperData,
					HashSet.class,
					SetProxy.class,
					elementComponentData,
					embeddableElementType,
					embeddableElementType
			);
		}
		else if ( collectionType instanceof SortedMapType ) {
			// Indexed collection, so <code>indexComponentData</code> is not null.
			return new SortedMapCollectionMapper(
					getMetadataBuildingContext().getConfiguration(),
					commonCollectionMapperData,
					TreeMap.class,
					SortedMapProxy.class,
					elementComponentData,
					indexComponentData,
					context.getCollection().getComparator(),
					embeddableElementType || lobMapElementType
			);
		}
		else if ( collectionType instanceof MapType ) {
			// Indexed collection, so <code>indexComponentData</code> is not null.
			return new MapCollectionMapper<Map>(
					getMetadataBuildingContext().getConfiguration(),
					commonCollectionMapperData,
					HashMap.class,
					MapProxy.class,
					elementComponentData,
					indexComponentData,
					embeddableElementType || lobMapElementType
			);
		}
		else if ( collectionType instanceof BagType ) {
			return new BasicCollectionMapper<List>(
					getMetadataBuildingContext().getConfiguration(),
					commonCollectionMapperData,
					ArrayList.class,
					ListProxy.class,
					elementComponentData,
					embeddableElementType,
					embeddableElementType
			);
		}
		else if ( collectionType instanceof ListType ) {
			// Indexed collection, so <code>indexComponentData</code> is not null.
			return new ListCollectionMapper(
					getMetadataBuildingContext().getConfiguration(),
					commonCollectionMapperData,
					elementComponentData,
					indexComponentData,
					embeddableElementType
			);
		}

		throwUnsupportedTypeException( collectionType, referencingEntityName, context.getPropertyName() );

		// this is never reached, but java requires it
		throw new AssertionError();
	}

	/**
	 * Returns whether the collection is a map-type and that the map element is defined as a Clob/NClob type.
	 *
	 * @return {@code true} if the element is a Clob/NClob type, otherwise {@code false}.
	 */
	protected boolean isLobMapElementType(CollectionMetadataContext context) {
		final Collection collection = context.getCollection();
		if ( collection instanceof org.hibernate.mapping.Map ) {
			final Type type = collection.getElement().getType();
			// we're only interested in basic types
			if ( !type.isComponentType() && !type.isAssociationType() && type instanceof BasicType<?> ) {
				final BasicType<?> basicType = (BasicType<?>) type;
				return basicType.getJavaType() == String.class && (
						basicType.getJdbcType().getDdlTypeCode() == Types.CLOB
								|| basicType.getJdbcType().getDdlTypeCode() == Types.NCLOB
				);
			}
		}
		return false;
	}

	protected boolean isEmbeddableElementType(CollectionMetadataContext context) {
		final Type elementType = context.getCollection().getElement().getType();
		return elementType instanceof ComponentType;
	}
}
