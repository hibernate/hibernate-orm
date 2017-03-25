/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.JoinColumn;

import org.dom4j.Element;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditedPropertiesReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditedPropertiesReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.SinglePropertyMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.BasicCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.CommonCollectionMapperData;
import org.hibernate.envers.internal.entities.mapper.relation.ListCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MapCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.entities.mapper.relation.SortedMapCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.SortedSetCollectionMapper;
import org.hibernate.envers.internal.entities.mapper.relation.ToOneIdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleDummyComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleEmbeddableComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleMapElementNotKeyComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleMapKeyIdComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleMapKeyPropertyComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleRelatedComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleSimpleComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleStraightComponentMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.ListProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.MapProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.SetProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.SortedMapProxy;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.SortedSetProxy;
import org.hibernate.envers.internal.entities.mapper.relation.query.OneAuditEntityQueryGenerator;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.type.BagType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ListType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MapType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.MaterializedNClobType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * Generates metadata for a collection-valued property.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Chris Cranford
 */
public final class CollectionMetadataGenerator {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			CollectionMetadataGenerator.class.getName()
	);

	private final AuditMetadataGenerator mainGenerator;
	private final String propertyName;
	private final Collection propertyValue;
	private final CompositeMapperBuilder currentMapper;
	private final String referencingEntityName;
	private final EntityXmlMappingData xmlMappingData;
	private final PropertyAuditingData propertyAuditingData;

	private final EntityConfiguration referencingEntityConfiguration;
	/**
	 * Null if this collection isn't a relation to another entity.
	 */
	private final String referencedEntityName;

	/**
	 * @param mainGenerator Main generator, giving access to configuration and the basic mapper.
	 * @param propertyValue Value of the collection, as mapped by Hibernate.
	 * @param currentMapper Mapper, to which the appropriate {@link PropertyMapper} will be added.
	 * @param referencingEntityName Name of the entity that owns this collection.
	 * @param xmlMappingData In case this collection requires a middle table, additional mapping documents will
	 * be created using this object.
	 * @param propertyAuditingData Property auditing (meta-)data. Among other things, holds the name of the
	 * property that references the collection in the referencing entity, the user data for middle (join)
	 * table and the value of the <code>@MapKey</code> annotation, if there was one.
	 */
	public CollectionMetadataGenerator(
			AuditMetadataGenerator mainGenerator,
			Collection propertyValue,
			CompositeMapperBuilder currentMapper,
			String referencingEntityName,
			EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData) {
		this.mainGenerator = mainGenerator;
		this.propertyValue = propertyValue;
		this.currentMapper = currentMapper;
		this.referencingEntityName = referencingEntityName;
		this.xmlMappingData = xmlMappingData;
		this.propertyAuditingData = propertyAuditingData;

		this.propertyName = propertyAuditingData.getName();

		referencingEntityConfiguration = mainGenerator.getEntitiesConfigurations().get( referencingEntityName );
		if ( referencingEntityConfiguration == null ) {
			throw new MappingException( "Unable to read auditing configuration for " + referencingEntityName + "!" );
		}

		referencedEntityName = MappingTools.getReferencedEntityName( propertyValue.getElement() );
	}

	void addCollection() {
		final Type type = propertyValue.getType();
		final Value value = propertyValue.getElement();

		final boolean oneToManyAttachedType = type instanceof BagType || type instanceof SetType || type instanceof MapType || type instanceof ListType;
		final boolean inverseOneToMany = (value instanceof OneToMany) && (propertyValue.isInverse());
		final boolean owningManyToOneWithJoinTableBidirectional = (value instanceof ManyToOne) && (propertyAuditingData.getRelationMappedBy() != null);
		final boolean fakeOneToManyBidirectional = (value instanceof OneToMany) && (propertyAuditingData.getAuditMappedBy() != null);

		if ( oneToManyAttachedType && (inverseOneToMany || fakeOneToManyBidirectional || owningManyToOneWithJoinTableBidirectional) ) {
			// A one-to-many relation mapped using @ManyToOne and @OneToMany(mappedBy="...")
			addOneToManyAttached( fakeOneToManyBidirectional  );
		}
		else {
			// All other kinds of relations require a middle (join) table.
			addWithMiddleTable();
		}
	}

	private MiddleIdData createMiddleIdData(IdMappingData idMappingData, String prefix, String entityName) {
		return new MiddleIdData(
				mainGenerator.getVerEntCfg(),
				idMappingData,
				prefix,
				entityName,
				mainGenerator.getEntitiesConfigurations().containsKey( entityName )
		);
	}

	@SuppressWarnings({"unchecked"})
	private void addOneToManyAttached(boolean fakeOneToManyBidirectional) {
		LOG.debugf(
				"Adding audit mapping for property %s.%s: one-to-many collection, using a join column on the referenced entity",
				referencingEntityName,
				propertyName
		);

		// check whether the property has an @IndexColumn or @OrderColumn because its part of an
		// IndexedCollection mapping type.
		final boolean indexed = ( propertyValue instanceof IndexedCollection ) && ( (IndexedCollection) propertyValue ).getIndex() != null;

		final String mappedBy = getMappedBy( propertyValue );

		final IdMappingData referencedIdMapping = mainGenerator.getReferencedIdMappingData(
				referencingEntityName,
				referencedEntityName,
				propertyAuditingData,
				false
		);
		final IdMappingData referencingIdMapping = referencingEntityConfiguration.getIdMappingData();

		// Generating the id mappers data for the referencing side of the relation.
		final MiddleIdData referencingIdData = createMiddleIdData(
				referencingIdMapping,
				mappedBy + "_",
				referencingEntityName
		);

		// And for the referenced side. The prefixed mapper won't be used (as this collection isn't persisted
		// in a join table, so the prefix value is arbitrary).
		final MiddleIdData referencedIdData = createMiddleIdData(
				referencedIdMapping,
				null, referencedEntityName
		);

		// Generating the element mapping.
		final MiddleComponentData elementComponentData = new MiddleComponentData(
				new MiddleRelatedComponentMapper( referencedIdData ), 0
		);

		// Generating the index mapping, if an index exists. It can only exists in case a javax.persistence.MapKey
		// annotation is present on the entity. So the middleEntityXml will be not be used. The queryGeneratorBuilder
		// will only be checked for nullnes.
		MiddleComponentData indexComponentData = addIndex( null, null );

		// Generating the query generator - it should read directly from the related entity.
		final RelationQueryGenerator queryGenerator = new OneAuditEntityQueryGenerator(
				mainGenerator.getGlobalCfg(),
				mainGenerator.getVerEntCfg(),
				mainGenerator.getAuditStrategy(),
				referencingIdData,
				referencedEntityName,
				referencedIdData,
				isEmbeddableElementType(),
				mappedBy,
				isMappedByKey( propertyValue, mappedBy )
		);

		// Creating common mapper data.
		final CommonCollectionMapperData commonCollectionMapperData = new CommonCollectionMapperData(
				mainGenerator.getVerEntCfg(), referencedEntityName,
				propertyAuditingData.getPropertyData(),
				referencingIdData, queryGenerator
		);

		PropertyMapper fakeBidirectionalRelationMapper;
		PropertyMapper fakeBidirectionalRelationIndexMapper;
		if ( fakeOneToManyBidirectional || indexed ) {
			// In case of a fake many-to-one bidirectional relation, we have to generate a mapper which maps
			// the mapped-by property name to the id of the related entity (which is the owner of the collection).
			final String auditMappedBy;
			if ( fakeOneToManyBidirectional ) {
				auditMappedBy = propertyAuditingData.getAuditMappedBy();
			}
			else {
				auditMappedBy = propertyValue.getMappedByProperty();
			}

			// Creating a prefixed relation mapper.
			final IdMapper relMapper = referencingIdMapping.getIdMapper().prefixMappedProperties(
					MappingTools.createToOneRelationPrefix( auditMappedBy )
			);

			fakeBidirectionalRelationMapper = new ToOneIdMapper(
					relMapper,
					// The mapper will only be used to map from entity to map, so no need to provide other details
					// when constructing the PropertyData.
					new PropertyData( auditMappedBy, null, null, null ),
					referencingEntityName, false
			);

			final String positionMappedBy;
			if ( fakeOneToManyBidirectional ) {
				positionMappedBy = propertyAuditingData.getPositionMappedBy();
			}
			else if ( indexed ) {
				final Value indexValue = ( (IndexedCollection) propertyValue ).getIndex();
				positionMappedBy = indexValue.getColumnIterator().next().getText();
			}
			else {
				positionMappedBy = null;
			}

			// Checking if there's an index defined. If so, adding a mapper for it.
			if ( positionMappedBy != null ) {
				fakeBidirectionalRelationIndexMapper = new SinglePropertyMapper(
						new PropertyData(
								positionMappedBy,
								null,
								null,
								null
						)
				);

				// Also, overwriting the index component data to properly read the index.
				indexComponentData = new MiddleComponentData(
						new MiddleStraightComponentMapper( positionMappedBy ),
						0
				);
			}
			else {
				fakeBidirectionalRelationIndexMapper = null;
			}
		}
		else {
			fakeBidirectionalRelationMapper = null;
			fakeBidirectionalRelationIndexMapper = null;
		}

		// Checking the type of the collection and adding an appropriate mapper.
		addMapper( commonCollectionMapperData, elementComponentData, indexComponentData );

		// Storing information about this relation.
		referencingEntityConfiguration.addToManyNotOwningRelation(
				propertyName,
				mappedBy,
				referencedEntityName,
				referencingIdData.getPrefixedMapper(),
				fakeBidirectionalRelationMapper,
				fakeBidirectionalRelationIndexMapper,
				indexed
		);
	}

	/**
	 * Adds mapping of the id of a related entity to the given xml mapping, prefixing the id with the given prefix.
	 *
	 * @param xmlMapping Mapping, to which to add the xml.
	 * @param prefix Prefix for the names of properties which will be prepended to properties that form the id.
	 * @param columnNameIterator Iterator over the column names that will be used for properties that form the id.
	 * @param relatedIdMapping Id mapping data of the related entity.
	 */
	@SuppressWarnings({"unchecked"})
	private void addRelatedToXmlMapping(
			Element xmlMapping,
			String prefix,
			MetadataTools.ColumnNameIterator columnNameIterator,
			IdMappingData relatedIdMapping) {
		final Element properties = (Element) relatedIdMapping.getXmlRelationMapping().clone();
		MetadataTools.prefixNamesInPropertyElement( properties, prefix, columnNameIterator, true, true );
		for ( Element idProperty : (java.util.List<Element>) properties.elements() ) {
			xmlMapping.add( (Element) idProperty.clone() );
		}
	}

	private String getMiddleTableName(Collection value, String entityName) {
		// We check how Hibernate maps the collection.
		if ( value.getElement() instanceof OneToMany && !value.isInverse() ) {
			// This must be a @JoinColumn+@OneToMany mapping. Generating the table name, as Hibernate doesn't use a
			// middle table for mapping this relation.
			return StringTools.getLastComponent( entityName ) + "_" + StringTools.getLastComponent(
					MappingTools.getReferencedEntityName(
							value.getElement()
					)
			);
		}
		// Hibernate uses a middle table for mapping this relation, so we get it's name directly.
		return value.getCollectionTable().getName();
	}

	@SuppressWarnings({"unchecked"})
	private void addWithMiddleTable() {

		LOG.debugf(
				"Adding audit mapping for property %s.%s: collection with a join table",
				referencingEntityName,
				propertyName
		);

		// Generating the name of the middle table
		String auditMiddleTableName;
		String auditMiddleEntityName;
		if ( !StringTools.isEmpty( propertyAuditingData.getJoinTable().name() ) ) {
			auditMiddleTableName = propertyAuditingData.getJoinTable().name();
			auditMiddleEntityName = propertyAuditingData.getJoinTable().name();
		}
		else {
			final String middleTableName = getMiddleTableName( propertyValue, referencingEntityName );
			auditMiddleTableName = mainGenerator.getVerEntCfg().getAuditTableName( null, middleTableName );
			auditMiddleEntityName = mainGenerator.getVerEntCfg().getAuditEntityName( middleTableName );
		}

		LOG.debugf( "Using join table name: %s", auditMiddleTableName );

		// Generating the XML mapping for the middle entity, only if the relation isn't inverse.
		// If the relation is inverse, will be later checked by comparing middleEntityXml with null.
		Element middleEntityXml;
		if ( !propertyValue.isInverse() ) {
			// Generating a unique middle entity name
			auditMiddleEntityName = mainGenerator.getAuditEntityNameRegister().createUnique( auditMiddleEntityName );

			// Registering the generated name
			mainGenerator.getAuditEntityNameRegister().register( auditMiddleEntityName );

			middleEntityXml = createMiddleEntityXml(
					auditMiddleTableName,
					auditMiddleEntityName,
					propertyValue.getWhere()
			);
		}
		else {
			middleEntityXml = null;
		}

		// ******
		// Generating the mapping for the referencing entity (it must be an entity).
		// ******
		// Getting the id-mapping data of the referencing entity (the entity that "owns" this collection).
		final IdMappingData referencingIdMapping = referencingEntityConfiguration.getIdMappingData();

		// Only valid for an inverse relation; null otherwise.
		String mappedBy;

		// The referencing prefix is always for a related entity. So it has always the "_" at the end added.
		String referencingPrefixRelated;
		String referencedPrefix;

		if ( propertyValue.isInverse() ) {
			// If the relation is inverse, then referencedEntityName is not null.
			mappedBy = getMappedBy(
					propertyValue.getCollectionTable(),
					mainGenerator.getMetadata().getEntityBinding( referencedEntityName )
			);

			referencingPrefixRelated = mappedBy + "_";
			referencedPrefix = StringTools.getLastComponent( referencedEntityName );
		}
		else {
			mappedBy = null;

			referencingPrefixRelated = StringTools.getLastComponent( referencingEntityName ) + "_";
			referencedPrefix = referencedEntityName == null ? "element" : propertyName;
		}

		// Storing the id data of the referencing entity: original mapper, prefixed mapper and entity name.
		final MiddleIdData referencingIdData = createMiddleIdData(
				referencingIdMapping,
				referencingPrefixRelated,
				referencingEntityName
		);

		// Creating a query generator builder, to which additional id data will be added, in case this collection
		// references some entities (either from the element or index). At the end, this will be used to build
		// a query generator to read the raw data collection from the middle table.
		final QueryGeneratorBuilder queryGeneratorBuilder = new QueryGeneratorBuilder(
				mainGenerator.getGlobalCfg(),
				mainGenerator.getVerEntCfg(),
				mainGenerator.getAuditStrategy(),
				referencingIdData,
				auditMiddleEntityName,
				isRevisionTypeInId()
		);

		// Adding the XML mapping for the referencing entity, if the relation isn't inverse.
		if ( middleEntityXml != null ) {
			// Adding related-entity (in this case: the referencing's entity id) id mapping to the xml.
			addRelatedToXmlMapping(
					middleEntityXml, referencingPrefixRelated,
					MetadataTools.getColumnNameIterator( propertyValue.getKey().getColumnIterator() ),
					referencingIdMapping
			);
		}

		// ******
		// Generating the element mapping.
		// ******
		final MiddleComponentData elementComponentData = addValueToMiddleTable(
				propertyValue.getElement(),
				middleEntityXml,
				queryGeneratorBuilder,
				referencedPrefix,
				propertyAuditingData.getJoinTable().inverseJoinColumns(),
				!isLobMapElementType()
		);

		// ******
		// Generating the index mapping, if an index exists.
		// ******
		final MiddleComponentData indexComponentData = addIndex( middleEntityXml, queryGeneratorBuilder );

		// ******
		// Generating the property mapper.
		// ******
		// Building the query generator.
		final RelationQueryGenerator queryGenerator = queryGeneratorBuilder.build( elementComponentData, indexComponentData );

		// Creating common data
		final CommonCollectionMapperData commonCollectionMapperData = new CommonCollectionMapperData(
				mainGenerator.getVerEntCfg(),
				auditMiddleEntityName,
				propertyAuditingData.getPropertyData(),
				referencingIdData,
				queryGenerator
		);

		// Checking the type of the collection and adding an appropriate mapper.
		addMapper( commonCollectionMapperData, elementComponentData, indexComponentData );

		// ******
		// Storing information about this relation.
		// ******
		storeMiddleEntityRelationInformation( mappedBy );
	}

	private MiddleComponentData addIndex(Element middleEntityXml, QueryGeneratorBuilder queryGeneratorBuilder) {
		if ( propertyValue instanceof IndexedCollection ) {
			final IndexedCollection indexedValue = (IndexedCollection) propertyValue;
			final String mapKey = propertyAuditingData.getMapKey();
			if ( mapKey == null ) {
				// This entity doesn't specify a javax.persistence.MapKey. Mapping it to the middle entity.
				return addValueToMiddleTable(
						indexedValue.getIndex(),
						middleEntityXml,
						queryGeneratorBuilder,
						"mapkey",
						null,
						true
				);
			}
			else {
				final IdMappingData referencedIdMapping = mainGenerator.getEntitiesConfigurations()
						.get( referencedEntityName ).getIdMappingData();
				final int currentIndex = queryGeneratorBuilder == null ? 0 : queryGeneratorBuilder.getCurrentIndex();
				if ( "".equals( mapKey ) ) {
					// The key of the map is the id of the entity.
					return new MiddleComponentData(
							new MiddleMapKeyIdComponentMapper(
									mainGenerator.getVerEntCfg(),
									referencedIdMapping.getIdMapper()
							),
							currentIndex
					);
				}
				else {
					// The key of the map is a property of the entity.
					return new MiddleComponentData(
							new MiddleMapKeyPropertyComponentMapper(
									mapKey,
									propertyAuditingData.getAccessType()
							),
							currentIndex
					);
				}
			}
		}
		else {
			// No index - creating a dummy mapper.
			return new MiddleComponentData( new MiddleDummyComponentMapper(), 0 );
		}
	}

	/**
	 * @param value Value, which should be mapped to the middle-table, either as a relation to another entity,
	 * or as a simple value.
	 * @param xmlMapping If not <code>null</code>, xml mapping for this value is added to this element.
	 * @param queryGeneratorBuilder In case <code>value</code> is a relation to another entity, information about it
	 * should be added to the given.
	 * @param prefix Prefix for proeprty names of related entities identifiers.
	 * @param joinColumns Names of columns to use in the xml mapping, if this array isn't null and has any elements.
	 *
	 * @return Data for mapping this component.
	 */
	@SuppressWarnings({"unchecked"})
	private MiddleComponentData addValueToMiddleTable(
			Value value,
			Element xmlMapping,
			QueryGeneratorBuilder queryGeneratorBuilder,
			String prefix,
			JoinColumn[] joinColumns,
			boolean key) {
		final Type type = value.getType();
		if ( type instanceof ManyToOneType ) {
			final String prefixRelated = prefix + "_";

			final String referencedEntityName = MappingTools.getReferencedEntityName( value );

			final IdMappingData referencedIdMapping = mainGenerator.getReferencedIdMappingData(
					referencingEntityName,
					referencedEntityName,
					propertyAuditingData,
					true
			);

			// Adding related-entity (in this case: the referenced entities id) id mapping to the xml only if the
			// relation isn't inverse (so when <code>xmlMapping</code> is not null).
			if ( xmlMapping != null ) {
				addRelatedToXmlMapping(
						xmlMapping, prefixRelated,
						joinColumns != null && joinColumns.length > 0
								? MetadataTools.getColumnNameIterator( joinColumns )
								: MetadataTools.getColumnNameIterator( value.getColumnIterator() ),
						referencedIdMapping
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
		else if ( type instanceof ComponentType ) {
			// Collection of embeddable elements.
			final Component component = (Component) value;
			final Class componentClass = ReflectionTools.loadClass(
					component.getComponentClassName(),
					mainGenerator.getClassLoaderService()
			);
			final MiddleEmbeddableComponentMapper componentMapper = new MiddleEmbeddableComponentMapper(
					new MultiPropertyMapper(),
					componentClass
			);

			final Element parentXmlMapping = xmlMapping.getParent();
			final ComponentAuditingData auditData = new ComponentAuditingData();
			final ReflectionManager reflectionManager = mainGenerator.getMetadata().getMetadataBuildingOptions().getReflectionManager();

			new ComponentAuditedPropertiesReader(
					ModificationStore.FULL,
					new AuditedPropertiesReader.ComponentPropertiesSource( reflectionManager, component ),
					auditData, mainGenerator.getGlobalCfg(), reflectionManager, ""
			).read();

			// Emulating first pass.
			for ( String auditedPropertyName : auditData.getPropertyNames() ) {
				final PropertyAuditingData nestedAuditingData = auditData.getPropertyAuditingData( auditedPropertyName );
				mainGenerator.addValue(
						parentXmlMapping,
						component.getProperty( auditedPropertyName ).getValue(),
						componentMapper,
						prefix, xmlMappingData,
						nestedAuditingData,
						true,
						true,
						true
				);
			}

			// Emulating second pass so that the relations can be mapped too.
			for ( String auditedPropertyName : auditData.getPropertyNames() ) {
				final PropertyAuditingData nestedAuditingData = auditData.getPropertyAuditingData( auditedPropertyName );
				mainGenerator.addValue(
						parentXmlMapping,
						component.getProperty( auditedPropertyName ).getValue(),
						componentMapper,
						referencingEntityName,
						xmlMappingData,
						nestedAuditingData,
						true,
						false,
						true
				);
			}

			// Add an additional column holding a number to make each entry unique within the set.
			// Embeddable properties may contain null values, so cannot be stored within composite primary key.
			if ( propertyValue.isSet() ) {
				final String setOrdinalPropertyName = mainGenerator.getVerEntCfg()
						.getEmbeddableSetOrdinalPropertyName();
				final Element ordinalProperty = MetadataTools.addProperty(
						xmlMapping, setOrdinalPropertyName, "integer", true, true
				);
				MetadataTools.addColumn(
						ordinalProperty, setOrdinalPropertyName, null, null, null, null, null, null, false
				);
			}

			return new MiddleComponentData( componentMapper, 0 );
		}
		else {
			// Last but one parameter: collection components are always insertable
			final boolean mapped = mainGenerator.getBasicMetadataGenerator().addBasic(
					key ? xmlMapping : xmlMapping.getParent(),
					new PropertyAuditingData(
							prefix,
							"field",
							ModificationStore.FULL,
							RelationTargetAuditMode.AUDITED,
							null,
							null,
							false
					),
					value,
					null,
					true,
					key
			);

			if ( mapped && key ) {
				// Simple values are always stored in the first item of the array returned by the query generator.
				return new MiddleComponentData(
						new MiddleSimpleComponentMapper( mainGenerator.getVerEntCfg(), prefix ),
						0
				);
			}
			else if ( mapped && !key ) {
				// when mapped but not part of the key, its stored as a dummy mapper??
				return new MiddleComponentData(
						new MiddleMapElementNotKeyComponentMapper( mainGenerator.getVerEntCfg(), prefix ),
						0
				);
			}
			else {
				mainGenerator.throwUnsupportedTypeException( type, referencingEntityName, propertyName );
				// Impossible to get here.
				throw new AssertionError();
			}
		}
	}

	private void addMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		final Type type = propertyValue.getType();
		final boolean embeddableElementType = isEmbeddableElementType();
		final boolean lobMapElementType = isLobMapElementType();
		if ( type instanceof SortedSetType ) {
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new SortedSetCollectionMapper(
							commonCollectionMapperData,
							TreeSet.class,
							SortedSetProxy.class,
							elementComponentData,
							propertyValue.getComparator(),
							embeddableElementType,
							embeddableElementType
					)
			);
		}
		else if ( type instanceof SetType ) {
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new BasicCollectionMapper<Set>(
							commonCollectionMapperData,
							HashSet.class,
							SetProxy.class,
							elementComponentData,
							embeddableElementType,
							embeddableElementType
					)
			);
		}
		else if ( type instanceof SortedMapType ) {
			// Indexed collection, so <code>indexComponentData</code> is not null.
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new SortedMapCollectionMapper(
							commonCollectionMapperData,
							TreeMap.class,
							SortedMapProxy.class,
							elementComponentData,
							indexComponentData,
							propertyValue.getComparator(),
							embeddableElementType || lobMapElementType
					)
			);
		}
		else if ( type instanceof MapType ) {
			// Indexed collection, so <code>indexComponentData</code> is not null.
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new MapCollectionMapper<Map>(
							commonCollectionMapperData,
							HashMap.class,
							MapProxy.class,
							elementComponentData,
							indexComponentData,
							embeddableElementType || lobMapElementType
					)
			);
		}
		else if ( type instanceof BagType ) {
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new BasicCollectionMapper<List>(
							commonCollectionMapperData,
							ArrayList.class,
							ListProxy.class,
							elementComponentData,
							embeddableElementType,
							embeddableElementType
					)
			);
		}
		else if ( type instanceof ListType ) {
			// Indexed collection, so <code>indexComponentData</code> is not null.
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new ListCollectionMapper(
							commonCollectionMapperData,
							elementComponentData,
							indexComponentData,
							embeddableElementType
					)
			);
		}
		else {
			mainGenerator.throwUnsupportedTypeException( type, referencingEntityName, propertyName );
		}
	}

	private void storeMiddleEntityRelationInformation(String mappedBy) {
		// Only if this is a relation (when there is a referenced entity).
		if ( referencedEntityName != null ) {
			if ( propertyValue.isInverse() ) {
				referencingEntityConfiguration.addToManyMiddleNotOwningRelation(
						propertyName,
						mappedBy,
						referencedEntityName
				);
			}
			else {
				referencingEntityConfiguration.addToManyMiddleRelation( propertyName, referencedEntityName );
			}
		}
	}

	private Element createMiddleEntityXml(String auditMiddleTableName, String auditMiddleEntityName, String where) {
		final String schema = mainGenerator.getSchema(
				propertyAuditingData.getJoinTable().schema(),
				propertyValue.getCollectionTable()
		);
		final String catalog = mainGenerator.getCatalog(
				propertyAuditingData.getJoinTable().catalog(),
				propertyValue.getCollectionTable()
		);

		final Element middleEntityXml = MetadataTools.createEntity(
				xmlMappingData.newAdditionalMapping(),
				new AuditTableData( auditMiddleEntityName, auditMiddleTableName, schema, catalog ), null, null
		);
		final Element middleEntityXmlId = middleEntityXml.addElement( "composite-id" );

		// If there is a where clause on the relation, adding it to the middle entity.
		if ( where != null ) {
			middleEntityXml.addAttribute( "where", where );
		}

		middleEntityXmlId.addAttribute( "name", mainGenerator.getVerEntCfg().getOriginalIdPropName() );

		// Adding the revision number as a foreign key to the revision info entity to the composite id of the
		// middle table.
		mainGenerator.addRevisionInfoRelation( middleEntityXmlId );

		// Adding the revision type property to the entity xml.
		mainGenerator.addRevisionType(
				isRevisionTypeInId() ? middleEntityXmlId : middleEntityXml,
				middleEntityXml,
				isRevisionTypeInId()
		);

		// All other properties should also be part of the primary key of the middle entity.
		return middleEntityXmlId;
	}

	/**
	 * Checks if the collection element is of {@link ComponentType} type.
	 */
	private boolean isEmbeddableElementType() {
		return propertyValue.getElement().getType() instanceof ComponentType;
	}

	private String getMappedBy(Collection collectionValue) {
		final PersistentClass referencedClass = getReferenceCollectionClass( collectionValue );
		final ValueHolder valueHolder = new ValueHolder( collectionValue );
		return getMappedBy( referencedClass, valueHolder );
	}

	private String getMappedBy(Table collectionTable, PersistentClass referencedClass) {
		return getMappedBy( referencedClass, new ValueHolder( collectionTable ) );
	}

	private String getMappedBy(PersistentClass referencedClass, ValueHolder valueHolder) {
		// If there's an @AuditMappedBy specified, returning it directly.
		final String auditMappedBy = propertyAuditingData.getAuditMappedBy();
		if ( auditMappedBy != null ) {
			return auditMappedBy;
		}

		// searching in referenced class
		String mappedBy = this.searchMappedBy( referencedClass, valueHolder );

		if ( mappedBy == null ) {
			LOG.debugf(
					"Going to search the mapped by attribute for %s in superclasses of entity: %s",
					propertyName,
					referencedClass.getClassName()
			);

			PersistentClass tempClass = referencedClass;
			while ( mappedBy == null && tempClass.getSuperclass() != null ) {
				LOG.debugf( "Searching in superclass: %s", tempClass.getSuperclass().getClassName() );
				mappedBy = this.searchMappedBy( tempClass.getSuperclass(), valueHolder );
				tempClass = tempClass.getSuperclass();
			}
		}

		if ( mappedBy == null ) {
			throw new MappingException(
					"Unable to read the mapped by attribute for " + propertyName + " in "
							+ referencedClass.getClassName() + "!"
			);
		}

		return mappedBy;
	}

	private String searchMappedBy(PersistentClass persistentClass, ValueHolder valueHolder) {
		if ( valueHolder.getCollection() != null ) {
			return searchMappedBy( persistentClass, valueHolder.getCollection() );
		}
		return searchMappedBy( persistentClass, valueHolder.getTable() );
	}

	@SuppressWarnings({"unchecked"})
	private String searchMappedBy(PersistentClass referencedClass, Collection collectionValue) {
		final Iterator<Property> assocClassProps = referencedClass.getPropertyIterator();
		while ( assocClassProps.hasNext() ) {
			final Property property = assocClassProps.next();

			if ( Tools.iteratorsContentEqual(
					property.getValue().getColumnIterator(),
					collectionValue.getKey().getColumnIterator()
			) ) {
				return property.getName();
			}
		}
		// HHH-7625
		// Support ToOne relations with mappedBy that point to an @IdClass key property.
		return searchMappedByKey( referencedClass, collectionValue );
	}

	@SuppressWarnings({"unchecked"})
	private String searchMappedBy(PersistentClass referencedClass, Table collectionTable) {
		final Iterator<Property> properties = referencedClass.getPropertyIterator();
		while ( properties.hasNext() ) {
			final Property property = properties.next();
			if ( property.getValue() instanceof Collection ) {
				// The equality is intentional. We want to find a collection property with the same collection table.
				//noinspection ObjectEquality
				if ( ( (Collection) property.getValue() ).getCollectionTable() == collectionTable ) {
					return property.getName();
				}
			}
		}
		return null;
	}

	@SuppressWarnings({"unchecked"})
	private String searchMappedByKey(PersistentClass referencedClass, Collection collectionValue) {
		final Iterator<Value> assocIdClassProps = referencedClass.getKeyClosureIterator();
		while ( assocIdClassProps.hasNext() ) {
			final Value value = assocIdClassProps.next();
			// make sure its a 'Component' because IdClass is registered as this type.
			if ( value instanceof Component ) {
				final Component component = (Component) value;
				final Iterator<Property> componentPropertyIterator = component.getPropertyIterator();
				while ( componentPropertyIterator.hasNext() ) {
					final Property property = componentPropertyIterator.next();
					final Iterator<Selectable> propertySelectables = property.getValue().getColumnIterator();
					final Iterator<Selectable> collectionSelectables = collectionValue.getKey().getColumnIterator();
					if ( Tools.iteratorsContentEqual( propertySelectables, collectionSelectables ) ) {
						return property.getName();
					}
				}
			}
		}
		return null;
	}

	private PersistentClass getReferenceCollectionClass(Collection collectionValue) {
		PersistentClass referencedClass = null;
		if ( collectionValue.getElement() instanceof OneToMany ) {
			final OneToMany oneToManyValue = (OneToMany) collectionValue.getElement();
			referencedClass = oneToManyValue.getAssociatedClass();
		}
		else if ( collectionValue.getElement() instanceof ManyToOne ) {
			// Case for bi-directional relation with @JoinTable on the owning @ManyToOne side.
			final ManyToOne manyToOneValue = (ManyToOne) collectionValue.getElement();
			referencedClass = manyToOneValue.getMetadata().getEntityBinding( manyToOneValue.getReferencedEntityName() );
		}
		return referencedClass;
	}

	private boolean isMappedByKey(Collection collectionValue, String mappedBy) {
		final PersistentClass referencedClass = getReferenceCollectionClass( collectionValue );
		if ( referencedClass != null ) {
			final String keyMappedBy = searchMappedByKey( referencedClass, collectionValue );
			return mappedBy.equals( keyMappedBy );
		}
		return false;
	}

	private class ValueHolder {
		private Collection collection;
		private Table table;

		public ValueHolder(Collection collection) {
			this.collection = collection;
		}

		public ValueHolder(Table table) {
			this.table = table;
		}

		public Collection getCollection() {
			return collection;
		}

		public Table getTable() {
			return table;
		}
	}

	/**
	 * Returns whether the revision type column part of the collection table's primary key.
	 *
	 * @return {@code true} if the revision type should be part of the primary key, otherwise {@code false}.
	 */
	private boolean isRevisionTypeInId() {
		return isEmbeddableElementType() || isLobMapElementType();
	}

	/**
	 * Returns whether the collection is a map-type and that the map element is defined as a Clob/NClob type.
	 *
	 * @return {@code true} if the element is a Clob/NClob type, otherwise {@code false}.
	 */
	private boolean isLobMapElementType() {
		if ( propertyValue instanceof org.hibernate.mapping.Map ) {
			final Type type = propertyValue.getElement().getType();
			// we're only interested in basic types
			if ( !type.isComponentType() && !type.isAssociationType() ) {
				return ( type instanceof MaterializedClobType ) || ( type instanceof MaterializedNClobType );
			}
		}
		return false;
	}
}
