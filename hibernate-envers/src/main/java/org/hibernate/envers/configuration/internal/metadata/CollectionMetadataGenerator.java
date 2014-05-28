/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.persistence.JoinColumn;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.internal.metadata.reader.AuditedPropertiesReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditedPropertiesReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeAssociationElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingEmbedded;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.type.BagType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ListType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MapType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import org.dom4j.Element;

/**
 * Generates metadata for a collection-valued property.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 */
public final class CollectionMetadataGenerator {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			CollectionMetadataGenerator.class.getName()
	);

	private final AuditConfiguration.AuditConfigurationContext context;
	private final AuditMetadataGenerator mainGenerator;
	private final String propertyName;
	private final PluralAttributeBinding pluralAttributeBinding;
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
	 * @param pluralAttributeBinding Value of the collection, as mapped by Hibernate.
	 * @param currentMapper Mapper, to which the appropriate {@link PropertyMapper} will be added.
	 * @param referencingEntityName Name of the entity that owns this collection.
	 * @param xmlMappingData In case this collection requires a middle table, additional mapping documents will
	 * be created using this object.
	 * @param propertyAuditingData Property auditing (meta-)data. Among other things, holds the name of the
	 * property that references the collection in the referencing entity, the user data for middle (join)
	 * table and the value of the <code>@MapKey</code> annotation, if there was one.
	 */
	public CollectionMetadataGenerator(
			AuditConfiguration.AuditConfigurationContext context,
			AuditMetadataGenerator mainGenerator,
			PluralAttributeBinding pluralAttributeBinding,
			CompositeMapperBuilder currentMapper,
			String referencingEntityName, EntityXmlMappingData xmlMappingData,
			PropertyAuditingData propertyAuditingData) {
		this.context = context;
		this.mainGenerator = mainGenerator;
		this.pluralAttributeBinding = pluralAttributeBinding;
		this.currentMapper = currentMapper;
		this.referencingEntityName = referencingEntityName;
		this.xmlMappingData = xmlMappingData;
		this.propertyAuditingData = propertyAuditingData;

		this.propertyName = propertyAuditingData.getName();

		referencingEntityConfiguration = mainGenerator.getEntitiesConfigurations().get( referencingEntityName );
		if ( referencingEntityConfiguration == null ) {
			throw new MappingException( "Unable to read auditing configuration for " + referencingEntityName + "!" );
		}

		referencedEntityName = MappingTools.getReferencedEntityName( pluralAttributeBinding );
	}

	void addCollection() {
		final Type type = pluralAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		final PluralAttributeElementNature elementNature =
				pluralAttributeBinding.getPluralAttributeElementBinding().getNature();

		final boolean oneToManyAttachedType = type instanceof BagType || type instanceof SetType || type instanceof MapType || type instanceof ListType;
		final boolean inverseOneToMany =
				(elementNature == PluralAttributeElementNature.ONE_TO_MANY) &&
						( pluralAttributeBinding.getPluralAttributeKeyBinding().isInverse() );
		final boolean owningManyToOneWithJoinTableBidirectional =
				(elementNature == PluralAttributeElementNature.MANY_TO_MANY) &&
						(propertyAuditingData.getRelationMappedBy() != null);
		final boolean fakeOneToManyBidirectional =
				(elementNature == PluralAttributeElementNature.ONE_TO_MANY)
				&& (propertyAuditingData.getAuditMappedBy() != null);

		if ( oneToManyAttachedType && (inverseOneToMany || fakeOneToManyBidirectional || owningManyToOneWithJoinTableBidirectional) ) {
			// A one-to-many relation mapped using @ManyToOne and @OneToMany(mappedBy="...")
			addOneToManyAttached( fakeOneToManyBidirectional );
		}
		else {
			// All other kinds of relations require a middle (join) table.
			addWithMiddleTable();
		}
	}

	private MiddleIdData createMiddleIdData(IdMappingData idMappingData, String prefix, String entityName) {
		return new MiddleIdData(
				context.getAuditEntitiesConfiguration(), idMappingData, prefix, entityName,
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

		final String mappedBy = getMappedBy(
				(PluralAttributeAssociationElementBinding) pluralAttributeBinding.getPluralAttributeElementBinding()
		);

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
				context.getGlobalConfiguration(),
				context.getAuditEntitiesConfiguration(),
				mainGenerator.getAuditStrategy(),
				referencingIdData,
				referencedEntityName,
				referencedIdData,
				isEmbeddableElementType()
		);

		// Creating common mapper data.
		final CommonCollectionMapperData commonCollectionMapperData = new CommonCollectionMapperData(
				context.getAuditEntitiesConfiguration(), referencedEntityName,
				propertyAuditingData.getPropertyData(),
				referencingIdData, queryGenerator
		);

		PropertyMapper fakeBidirectionalRelationMapper;
		PropertyMapper fakeBidirectionalRelationIndexMapper;
		if ( fakeOneToManyBidirectional ) {
			// In case of a fake many-to-one bidirectional relation, we have to generate a mapper which maps
			// the mapped-by property name to the id of the related entity (which is the owner of the collection).
			final String auditMappedBy = propertyAuditingData.getAuditMappedBy();

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

			// Checking if there's an index defined. If so, adding a mapper for it.
			if ( propertyAuditingData.getPositionMappedBy() != null ) {
				final String positionMappedBy = propertyAuditingData.getPositionMappedBy();
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
				fakeBidirectionalRelationIndexMapper
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
			Element xmlMapping, String prefix,
			MetadataTools.ColumnNameIterator columnNameIterator,
			IdMappingData relatedIdMapping) {
		final Element properties = (Element) relatedIdMapping.getXmlRelationMapping().clone();
		MetadataTools.prefixNamesInPropertyElement( properties, prefix, columnNameIterator, true, true );
		for ( Element idProperty : (java.util.List<Element>) properties.elements() ) {
			xmlMapping.add( (Element) idProperty.clone() );
		}
	}

	private String getMiddleTableName(PluralAttributeBinding attributeBinding, String entityName) {
		// We check how Hibernate maps the collection.
		if ( attributeBinding.getPluralAttributeElementBinding().getNature() ==
				PluralAttributeElementNature.ONE_TO_MANY &&
				!attributeBinding.getPluralAttributeKeyBinding().isInverse() ) {
			// This must be a @JoinColumn+@OneToMany mapping. Generating the table name, as Hibernate doesn't use a
			// middle table for mapping this relation.
			return StringTools.getLastComponent( entityName ) + "_" + StringTools.getLastComponent(
					MappingTools.getReferencedEntityName( attributeBinding )
			);
		}
		// Hibernate uses a middle table for mapping this relation, so we get it's name directly.
		return attributeBinding.getPluralAttributeKeyBinding().getCollectionTable().getLogicalName().getName();
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
			final String middleTableName = getMiddleTableName( pluralAttributeBinding, referencingEntityName );
			auditMiddleTableName = context.getAuditEntitiesConfiguration().getAuditTableName( null, middleTableName );
			auditMiddleEntityName = context.getAuditEntitiesConfiguration().getAuditEntityName( middleTableName );
		}

		LOG.debugf( "Using join table name: %s", auditMiddleTableName );

		// Generating the XML mapping for the middle entity, only if the relation isn't inverse.
		// If the relation is inverse, will be later checked by comparing middleEntityXml with null.
		Element middleEntityXml;
		if ( !pluralAttributeBinding.getPluralAttributeKeyBinding().isInverse() ) {
			// Generating a unique middle entity name
			auditMiddleEntityName = mainGenerator.getAuditEntityNameRegister().createUnique( auditMiddleEntityName );

			// Registering the generated name
			mainGenerator.getAuditEntityNameRegister().register( auditMiddleEntityName );

			middleEntityXml = createMiddleEntityXml(
					auditMiddleTableName,
					auditMiddleEntityName,
					pluralAttributeBinding.getWhere()
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

		if ( pluralAttributeBinding.getPluralAttributeKeyBinding().isInverse() ) {
			// If the relation is inverse, then referencedEntityName is not null.
			mappedBy = getMappedBy(
					pluralAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable(),
					context.getEntityBinding( referencedEntityName )
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
				context.getGlobalConfiguration(),
				context.getAuditEntitiesConfiguration(),
				mainGenerator.getAuditStrategy(),
				referencingIdData,
				auditMiddleEntityName,
				isEmbeddableElementType()
		);

		// Adding the XML mapping for the referencing entity, if the relation isn't inverse.
		if ( middleEntityXml != null ) {
			// Adding related-entity (in this case: the referencing's entity id) id mapping to the xml.
			addRelatedToXmlMapping(
					middleEntityXml, referencingPrefixRelated,
					MetadataTools.getColumnNameIterator(
							pluralAttributeBinding.getPluralAttributeKeyBinding().getValues().iterator()
					),
					referencingIdMapping
			);
		}

		// ******
		// Generating the element mapping.
		// ******
		final MiddleComponentData elementComponentData = addValueToMiddleTable(
				middleEntityXml,
				queryGeneratorBuilder,
				referencedPrefix,
				propertyAuditingData.getJoinTable().inverseJoinColumns(),
				false
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
				context.getAuditEntitiesConfiguration(),
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
		if ( pluralAttributeBinding.getAttribute().getPluralAttributeNature().isIndexed() ) {
			final PluralAttributeIndexBinding indexBinding =
					( (IndexedPluralAttributeBinding) pluralAttributeBinding ).getPluralAttributeIndexBinding();
			final String mapKey = propertyAuditingData.getMapKey();
			if ( mapKey == null ) {
				// This entity doesn't specify a javax.persistence.MapKey. Mapping it to the middle entity.
				return addValueToMiddleTable(
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
									context.getAuditEntitiesConfiguration(),
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

	private PluralAttributeIndexBinding getPluralAttributeIndexBinding() {
		if ( !pluralAttributeBinding.getAttribute().getPluralAttributeNature().isIndexed() ) {
			throw new AssertionFailure( "This method is only valid for an indexed plural attribute binding." );
		}
		return ( (IndexedPluralAttributeBinding) pluralAttributeBinding ).getPluralAttributeIndexBinding();
	}

	/**
	 * @param xmlMapping If not <code>null</code>, xml mapping for this value is added to this element.
	 * @param queryGeneratorBuilder In case <code>value</code> is a relation to another entity, information about it
	 * should be added to the given.
	 * @param prefix Prefix for proeprty names of related entities identifiers.
	 * @param joinColumns Names of columns to use in the xml mapping, if this array isn't null and has any elements.
	 * @param isIndex true, if the value is for the collection index; false, if the value is for the collection element.
	 *
	 * @return Data for mapping this component.
	 */
	@SuppressWarnings({"unchecked"})
	private MiddleComponentData addValueToMiddleTable(
			Element xmlMapping,
			QueryGeneratorBuilder queryGeneratorBuilder,
			String prefix,
			JoinColumn[] joinColumns,
			boolean isIndex) {

		final HibernateTypeDescriptor hibernateTypeDescriptor;
		final List<Value> values;
		if ( isIndex ) {
			hibernateTypeDescriptor = getPluralAttributeIndexBinding().getHibernateTypeDescriptor();
			values = getPluralAttributeIndexBinding().getValues();
		}
		else {
			hibernateTypeDescriptor = pluralAttributeBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor();
			values = pluralAttributeBinding.getPluralAttributeElementBinding().getRelationalValueContainer().values();

		}
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() instanceof ManyToOneType ) {
			final String prefixRelated = prefix + "_";

			final String referencedEntityName = MappingTools.getReferencedEntityName( pluralAttributeBinding );

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
								: MetadataTools.getColumnNameIterator( values.iterator() ),
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
		else if ( hibernateTypeDescriptor.getResolvedTypeMapping() instanceof ComponentType ) {
			final EmbeddableBinding embeddableBinding;
			if ( isIndex ) {
				embeddableBinding =
						( ( CompositePluralAttributeIndexBinding ) getPluralAttributeIndexBinding() ).getCompositeAttributeBindingContainer();
			}
			else {
				embeddableBinding =
						( (PluralAttributeElementBindingEmbedded) pluralAttributeBinding.getPluralAttributeElementBinding() ).getEmbeddableBinding();
			}
			// Collection of embeddable elements.
			final Class componentClass = ReflectionTools.loadClass(
					hibernateTypeDescriptor.getJavaTypeDescriptor().getName().toString(),
					context.getClassLoaderService()
			);
			final MiddleEmbeddableComponentMapper componentMapper = new MiddleEmbeddableComponentMapper(
					new MultiPropertyMapper(),
					componentClass
			);

			final Element parentXmlMapping = xmlMapping.getParent();
			final ComponentAuditingData auditData = new ComponentAuditingData();

			new ComponentAuditedPropertiesReader(
					context,
					auditData,
					new AuditedPropertiesReader.ComponentPropertiesSource(
							context.getClassInfo( embeddableBinding.getAttributeContainer() ),
							embeddableBinding
					),
					""
			).read();

			// Emulating first pass.
			for ( String auditedPropertyName : auditData.getPropertyNames() ) {
				final PropertyAuditingData nestedAuditingData = auditData.getPropertyAuditingData( auditedPropertyName );
				mainGenerator.addValue(
						parentXmlMapping,
						embeddableBinding.locateAttributeBinding( auditedPropertyName ),
						componentMapper,
						prefix, xmlMappingData,
						nestedAuditingData,
						true,
						true
				);
			}

			// Emulating second pass so that the relations can be mapped too.
			for ( String auditedPropertyName : auditData.getPropertyNames() ) {
				final PropertyAuditingData nestedAuditingData = auditData.getPropertyAuditingData( auditedPropertyName );
				mainGenerator.addValue(
						parentXmlMapping,
						embeddableBinding.locateAttributeBinding( auditedPropertyName ),
						componentMapper,
						referencingEntityName,
						xmlMappingData,
						nestedAuditingData,
						false,
						true
				);
			}

			// Add an additional column holding a number to make each entry unique within the set.
			// Embeddable properties may contain null values, so cannot be stored within composite primary key.
			if ( ( pluralAttributeBinding.getAttribute() ).getPluralAttributeNature() == PluralAttributeNature.SET ) {
				final String setOrdinalPropertyName = context.getAuditEntitiesConfiguration()
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
					xmlMapping,
					new PropertyAuditingData(
							prefix,
							"field",
							ModificationStore.FULL,
							RelationTargetAuditMode.AUDITED,
							null,
							null,
							false
					),
					hibernateTypeDescriptor,
					values,
					true, // TODO: is this correct for collection element?
					null,
					true
			);

			if ( mapped ) {
				// Simple values are always stored in the first item of the array returned by the query generator.
				return new MiddleComponentData(
						new MiddleSimpleComponentMapper( context.getAuditEntitiesConfiguration(), prefix ),
						0
				);
			}
			else {
				mainGenerator.throwUnsupportedTypeException(
						hibernateTypeDescriptor.getResolvedTypeMapping(), referencingEntityName, propertyName
				);
				// Impossible to get here.
				throw new AssertionError();
			}
		}
	}

	private void addMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		final Type type = pluralAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		final boolean embeddableElementType = isEmbeddableElementType();
		if ( type instanceof SortedSetType ) {
			currentMapper.addComposite(
					propertyAuditingData.getPropertyData(),
					new SortedSetCollectionMapper(
							commonCollectionMapperData,
							TreeSet.class,
							SortedSetProxy.class,
							elementComponentData,
							pluralAttributeBinding.getComparator(),
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
							pluralAttributeBinding.getComparator(),
							embeddableElementType
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
							embeddableElementType
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
			if ( pluralAttributeBinding.getPluralAttributeKeyBinding().isInverse() ) {
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
				pluralAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable()
		);
		final String catalog = mainGenerator.getCatalog(
				propertyAuditingData.getJoinTable().catalog(),
				pluralAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable()
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

		middleEntityXmlId.addAttribute( "name", context.getAuditEntitiesConfiguration().getOriginalIdPropName() );

		// Adding the revision number as a foreign key to the revision info entity to the composite id of the
		// middle table.
		mainGenerator.addRevisionInfoRelation( middleEntityXmlId );

		// Adding the revision type property to the entity xml.
		mainGenerator.addRevisionType(
				isEmbeddableElementType() ? middleEntityXmlId : middleEntityXml,
				middleEntityXml,
				isEmbeddableElementType()
		);

		// All other properties should also be part of the primary key of the middle entity.
		return middleEntityXmlId;
	}

	/**
	 * Checks if the collection element is of {@link ComponentType} type.
	 */
	private boolean isEmbeddableElementType() {
		return pluralAttributeBinding.getPluralAttributeElementBinding()
				.getHibernateTypeDescriptor().getResolvedTypeMapping().isComponentType();
	}

	private String getMappedBy(PluralAttributeAssociationElementBinding elementBinding) {
		EntityBinding referencedEntityBinding = null;
		final EntityType entityType =
				(EntityType) elementBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
		referencedEntityBinding = context.getEntityBinding( entityType.getAssociatedEntityName() );

		// If there's an @AuditMappedBy specified, returning it directly.
		final String auditMappedBy = propertyAuditingData.getAuditMappedBy();
		if ( auditMappedBy != null ) {
			return auditMappedBy;
		}

		// searching in referenced class
		String mappedBy = this.searchMappedBy( referencedEntityBinding );

		if ( mappedBy == null ) {
			LOG.debugf(
					"Going to search the mapped by attribute for %s in superclasses of entity: %s",
					propertyName,
					referencedEntityBinding.getEntityName()
			);

			EntityBinding tempEntityBinding = referencedEntityBinding;
			while ( (mappedBy == null) && (tempEntityBinding.getSuperEntityBinding() != null) ) {
				LOG.debugf(
						"Searching in superclass: %s",
						tempEntityBinding.getSuperEntityBinding().getEntity().getDescriptor().getName()
				);
				mappedBy = this.searchMappedBy( tempEntityBinding.getSuperEntityBinding() );
				tempEntityBinding = tempEntityBinding.getSuperEntityBinding();
			}
		}

		if ( mappedBy == null ) {
			throw new MappingException(
					"Unable to read the mapped by attribute for " + propertyName + " in "
							+ referencedEntityBinding.getEntity().getDescriptor().getName() + "!"
			);
		}

		return mappedBy;
	}

	@SuppressWarnings({"unchecked"})
	private String searchMappedBy(EntityBinding referencedEntityBinding) {
		for ( AttributeBinding attributeBinding : referencedEntityBinding.attributeBindings() ) {
			if ( !attributeBinding.isAssociation() ) {
				continue;
			}
			final List<Value> attributeValues;
			if ( attributeBinding.getAttribute().isSingular() ) {
				attributeValues = ( (SingularAttributeBinding) attributeBinding ).getValues();
			}
			else {
				attributeValues = ( (PluralAttributeBinding) attributeBinding ).getPluralAttributeElementBinding().getRelationalValueContainer().values();
			}

			if ( Tools.iteratorsContentEqual(
					attributeValues.iterator(),
					pluralAttributeBinding.getPluralAttributeKeyBinding().getValues().iterator()
			) ) {
				return attributeBinding.getAttribute().getName();
			}
		}
		return null;
	}

	private String getMappedBy(TableSpecification collectionTable, EntityBinding referencedClass) {
		// If there's an @AuditMappedBy specified, returning it directly.
		final String auditMappedBy = propertyAuditingData.getAuditMappedBy();
		if ( auditMappedBy != null ) {
			return auditMappedBy;
		}

		// searching in referenced class
		String mappedBy = this.searchMappedBy( referencedClass, collectionTable );

		// not found on referenced class, searching on superclasses
		if ( mappedBy == null ) {
			LOG.debugf(
					"Going to search the mapped by attribute for %s in superclasses of entity: %s",
					propertyName,
					referencedClass.getEntity().getDescriptor().getName()
			);

			EntityBinding tempClass = referencedClass;
			while ( (mappedBy == null) && (tempClass.getSuperEntityBinding() != null) ) {
				LOG.debugf(
						"Searching in superclass: %s",
						tempClass.getSuperEntityBinding().getEntity().getDescriptor().getName()
				);
				mappedBy = this.searchMappedBy( tempClass.getSuperEntityBinding(), collectionTable );
				tempClass = tempClass.getSuperEntityBinding();
			}
		}

		if ( mappedBy == null ) {
			throw new MappingException(
					"Unable to read the mapped by attribute for " + propertyName + " in "
							+ referencedClass.getEntity().getDescriptor().getName() + "!"
			);
		}

		return mappedBy;
	}

	@SuppressWarnings({"unchecked"})
	private String searchMappedBy(EntityBinding referencedClass, TableSpecification collectionTable) {
		for ( AttributeBinding attributeBinding : referencedClass.attributeBindings() ) {
			if ( !attributeBinding.getAttribute().isSingular() ) {
				// The equality is intentional. We want to find a collection property with the same collection table.
				//noinspection ObjectEquality
				if ( ((PluralAttributeBinding) attributeBinding ).getPluralAttributeKeyBinding().getCollectionTable() ==
						collectionTable ) {
					return attributeBinding.getAttribute().getName();
				}
			}
		}
		return null;
	}

}
