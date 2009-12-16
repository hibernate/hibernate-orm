/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.configuration.metadata;

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
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.IdMappingData;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.SinglePropertyMapper;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.entities.mapper.relation.*;
import org.hibernate.envers.entities.mapper.relation.component.*;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.ListProxy;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.MapProxy;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.SetProxy;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.SortedMapProxy;
import org.hibernate.envers.entities.mapper.relation.lazy.proxy.SortedSetProxy;
import org.hibernate.envers.entities.mapper.relation.query.OneAuditEntityQueryGenerator;
import org.hibernate.envers.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.tools.MappingTools;

import org.hibernate.MappingException;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.type.BagType;
import org.hibernate.type.ListType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.MapType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates metadata for a collection-valued property.
 * @author Adam Warski (adam at warski dot org)
 */
public final class CollectionMetadataGenerator {
    private static final Logger log = LoggerFactory.getLogger(CollectionMetadataGenerator.class);

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
     * @param currentMapper Mapper, to which the appropriate {@link org.hibernate.envers.entities.mapper.PropertyMapper}
     * will be added.
     * @param referencingEntityName Name of the entity that owns this collection.
     * @param xmlMappingData In case this collection requires a middle table, additional mapping documents will
     * be created using this object.
     * @param propertyAuditingData Property auditing (meta-)data. Among other things, holds the name of the
     * property that references the collection in the referencing entity, the user data for middle (join)
     * table and the value of the <code>@MapKey</code> annotation, if there was one. 
     */
    public CollectionMetadataGenerator(AuditMetadataGenerator mainGenerator,
                                       Collection propertyValue, CompositeMapperBuilder currentMapper,
                                       String referencingEntityName, EntityXmlMappingData xmlMappingData,
                                       PropertyAuditingData propertyAuditingData) {
        this.mainGenerator = mainGenerator;
        this.propertyValue = propertyValue;
        this.currentMapper = currentMapper;
        this.referencingEntityName = referencingEntityName;
        this.xmlMappingData = xmlMappingData;
        this.propertyAuditingData = propertyAuditingData;

        this.propertyName = propertyAuditingData.getName();

        referencingEntityConfiguration = mainGenerator.getEntitiesConfigurations().get(referencingEntityName);
        if (referencingEntityConfiguration == null) {
            throw new MappingException("Unable to read auditing configuration for " + referencingEntityName + "!");
        }

        referencedEntityName = MappingTools.getReferencedEntityName(propertyValue.getElement());
    }

    void addCollection() {
        Type type = propertyValue.getType();

        boolean oneToManyAttachedType = type instanceof BagType || type instanceof SetType || type instanceof MapType || type instanceof ListType;
        boolean inverseOneToMany = (propertyValue.getElement() instanceof OneToMany) && (propertyValue.isInverse());
        boolean fakeOneToManyBidirectional = (propertyValue.getElement() instanceof OneToMany) && (propertyAuditingData.getAuditMappedBy() != null);

        if (oneToManyAttachedType && (inverseOneToMany || fakeOneToManyBidirectional)) {
            // A one-to-many relation mapped using @ManyToOne and @OneToMany(mappedBy="...")
            addOneToManyAttached(fakeOneToManyBidirectional);
        } else {
            // All other kinds of relations require a middle (join) table.
            addWithMiddleTable();
        }
    }

    private MiddleIdData createMiddleIdData(IdMappingData idMappingData, String prefix, String entityName) {
        return new MiddleIdData(mainGenerator.getVerEntCfg(), idMappingData, prefix, entityName,
                mainGenerator.getEntitiesConfigurations().containsKey(entityName));
    }

    @SuppressWarnings({"unchecked"})
    private void addOneToManyAttached(boolean fakeOneToManyBidirectional) {
        log.debug("Adding audit mapping for property " + referencingEntityName + "." + propertyName +
                ": one-to-many collection, using a join column on the referenced entity.");

        String mappedBy = getMappedBy(propertyValue);

        IdMappingData referencedIdMapping = mainGenerator.getReferencedIdMappingData(referencingEntityName,
                    referencedEntityName, propertyAuditingData, false);
        IdMappingData referencingIdMapping = referencingEntityConfiguration.getIdMappingData();

        // Generating the id mappers data for the referencing side of the relation.
        MiddleIdData referencingIdData = createMiddleIdData(referencingIdMapping,
                mappedBy + "_", referencingEntityName);

        // And for the referenced side. The prefixed mapper won't be used (as this collection isn't persisted
        // in a join table, so the prefix value is arbitrary).
        MiddleIdData referencedIdData = createMiddleIdData(referencedIdMapping,
                null, referencedEntityName);

        // Generating the element mapping.
        MiddleComponentData elementComponentData = new MiddleComponentData(
                new MiddleRelatedComponentMapper(referencedIdData), 0);

        // Generating the index mapping, if an index exists. It can only exists in case a javax.persistence.MapKey
        // annotation is present on the entity. So the middleEntityXml will be not be used. The queryGeneratorBuilder
        // will only be checked for nullnes.
        MiddleComponentData indexComponentData = addIndex(null, null);

        // Generating the query generator - it should read directly from the related entity.
        RelationQueryGenerator queryGenerator = new OneAuditEntityQueryGenerator(mainGenerator.getGlobalCfg(),
                mainGenerator.getVerEntCfg(), referencingIdData, referencedEntityName,
                referencedIdMapping.getIdMapper());

        // Creating common mapper data.
        CommonCollectionMapperData commonCollectionMapperData = new CommonCollectionMapperData(
                mainGenerator.getVerEntCfg(), referencedEntityName,
                propertyAuditingData.getPropertyData(),
                referencingIdData, queryGenerator);

        PropertyMapper fakeBidirectionalRelationMapper;
        PropertyMapper fakeBidirectionalRelationIndexMapper;
        if (fakeOneToManyBidirectional) {
            // In case of a fake many-to-one bidirectional relation, we have to generate a mapper which maps
            // the mapped-by property name to the id of the related entity (which is the owner of the collection).
            String auditMappedBy = propertyAuditingData.getAuditMappedBy();

            // Creating a prefixed relation mapper.
            IdMapper relMapper = referencingIdMapping.getIdMapper().prefixMappedProperties(
                    MappingTools.createToOneRelationPrefix(auditMappedBy));

            fakeBidirectionalRelationMapper = new ToOneIdMapper(
                    relMapper,
                    // The mapper will only be used to map from entity to map, so no need to provide other details
                    // when constructing the PropertyData.
                    new PropertyData(auditMappedBy, null, null, null),
                    referencedEntityName, false);

            // Checking if there's an index defined. If so, adding a mapper for it.
            if (propertyAuditingData.getPositionMappedBy() != null) {
                String positionMappedBy = propertyAuditingData.getPositionMappedBy();
                fakeBidirectionalRelationIndexMapper = new SinglePropertyMapper(new PropertyData(positionMappedBy, null, null, null));

                // Also, overwriting the index component data to properly read the index.
                indexComponentData = new MiddleComponentData(new MiddleStraightComponentMapper(positionMappedBy), 0);
            } else {
                fakeBidirectionalRelationIndexMapper = null;
            }
        } else {
            fakeBidirectionalRelationMapper = null;
            fakeBidirectionalRelationIndexMapper = null;
        }

        // Checking the type of the collection and adding an appropriate mapper.
        addMapper(commonCollectionMapperData, elementComponentData, indexComponentData);

        // Storing information about this relation.
        referencingEntityConfiguration.addToManyNotOwningRelation(propertyName, mappedBy,
                referencedEntityName, referencingIdData.getPrefixedMapper(), fakeBidirectionalRelationMapper,
                fakeBidirectionalRelationIndexMapper);
    }

    /**
     * Adds mapping of the id of a related entity to the given xml mapping, prefixing the id with the given prefix.
     * @param xmlMapping Mapping, to which to add the xml.
     * @param prefix Prefix for the names of properties which will be prepended to properties that form the id.
     * @param columnNameIterator Iterator over the column names that will be used for properties that form the id.
     * @param relatedIdMapping Id mapping data of the related entity.
     */
    @SuppressWarnings({"unchecked"})
    private void addRelatedToXmlMapping(Element xmlMapping, String prefix,
                                        MetadataTools.ColumnNameIterator columnNameIterator,
                                        IdMappingData relatedIdMapping) {
        Element properties = (Element) relatedIdMapping.getXmlRelationMapping().clone();
        MetadataTools.prefixNamesInPropertyElement(properties, prefix, columnNameIterator, true, true);
        for (Element idProperty : (java.util.List<Element>) properties.elements()) {
            xmlMapping.add((Element) idProperty.clone());
        }
    }

    private String getMiddleTableName(Collection value, String entityName) {
        // We check how Hibernate maps the collection.
        if (value.getElement() instanceof OneToMany && !value.isInverse()) {
            // This must be a @JoinColumn+@OneToMany mapping. Generating the table name, as Hibernate doesn't use a
            // middle table for mapping this relation.
            return StringTools.getLastComponent(entityName) + "_" + StringTools.getLastComponent(MappingTools.getReferencedEntityName(value.getElement()));
        } else {
            // Hibernate uses a middle table for mapping this relation, so we get it's name directly.
            return value.getCollectionTable().getName();
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addWithMiddleTable() {
        log.debug("Adding audit mapping for property " + referencingEntityName + "." + propertyName +
                ": collection with a join table.");

        // Generating the name of the middle table
        String auditMiddleTableName;
        String auditMiddleEntityName;
        if (!StringTools.isEmpty(propertyAuditingData.getJoinTable().name())) {
            auditMiddleTableName = propertyAuditingData.getJoinTable().name();
            auditMiddleEntityName = propertyAuditingData.getJoinTable().name();
        } else {
            String middleTableName = getMiddleTableName(propertyValue, referencingEntityName);
            auditMiddleTableName = mainGenerator.getVerEntCfg().getAuditTableName(null, middleTableName);
            auditMiddleEntityName = mainGenerator.getVerEntCfg().getAuditEntityName(middleTableName);
        }

        log.debug("Using join table name: " + auditMiddleTableName);

        // Generating the XML mapping for the middle entity, only if the relation isn't inverse.
        // If the relation is inverse, will be later checked by comparing middleEntityXml with null.
        Element middleEntityXml;
        if (!propertyValue.isInverse()) {            
            // Generating a unique middle entity name
            auditMiddleEntityName = mainGenerator.getAuditEntityNameRegister().createUnique(auditMiddleEntityName);

            // Registering the generated name
            mainGenerator.getAuditEntityNameRegister().register(auditMiddleEntityName);
                        
            middleEntityXml = createMiddleEntityXml(auditMiddleTableName, auditMiddleEntityName, propertyValue.getWhere());
        } else {
            middleEntityXml = null;
        }

        // ******
        // Generating the mapping for the referencing entity (it must be an entity).
        // ******
        // Getting the id-mapping data of the referencing entity (the entity that "owns" this collection).
        IdMappingData referencingIdMapping = referencingEntityConfiguration.getIdMappingData();

        // Only valid for an inverse relation; null otherwise.
        String mappedBy;

        // The referencing prefix is always for a related entity. So it has always the "_" at the end added.
        String referencingPrefixRelated;
        String referencedPrefix;

        if (propertyValue.isInverse()) {
            // If the relation is inverse, then referencedEntityName is not null.
            mappedBy = getMappedBy(propertyValue.getCollectionTable(), mainGenerator.getCfg().getClassMapping(referencedEntityName));

            referencingPrefixRelated = mappedBy + "_";
            referencedPrefix = StringTools.getLastComponent(referencedEntityName);
        } else {
            mappedBy = null;

            referencingPrefixRelated = StringTools.getLastComponent(referencingEntityName) + "_";
            referencedPrefix = referencedEntityName == null ? "element" : propertyName;
        }

        // Storing the id data of the referencing entity: original mapper, prefixed mapper and entity name.
        MiddleIdData referencingIdData = createMiddleIdData(referencingIdMapping,
                referencingPrefixRelated, referencingEntityName);

        // Creating a query generator builder, to which additional id data will be added, in case this collection
        // references some entities (either from the element or index). At the end, this will be used to build
        // a query generator to read the raw data collection from the middle table.
        QueryGeneratorBuilder queryGeneratorBuilder = new QueryGeneratorBuilder(mainGenerator.getGlobalCfg(),
                mainGenerator.getVerEntCfg(), referencingIdData, auditMiddleEntityName);

        // Adding the XML mapping for the referencing entity, if the relation isn't inverse.
        if (middleEntityXml != null) {
            // Adding related-entity (in this case: the referencing's entity id) id mapping to the xml.
            addRelatedToXmlMapping(middleEntityXml, referencingPrefixRelated,
                    MetadataTools.getColumnNameIterator(propertyValue.getKey().getColumnIterator()),
                    referencingIdMapping);
        }

        // ******
        // Generating the element mapping.
        // ******
        MiddleComponentData elementComponentData = addValueToMiddleTable(propertyValue.getElement(), middleEntityXml,
                queryGeneratorBuilder, referencedPrefix, propertyAuditingData.getJoinTable().inverseJoinColumns());

        // ******
        // Generating the index mapping, if an index exists.
        // ******
        MiddleComponentData indexComponentData = addIndex(middleEntityXml, queryGeneratorBuilder);

        // ******
        // Generating the property mapper.
        // ******
        // Building the query generator.
        RelationQueryGenerator queryGenerator = queryGeneratorBuilder.build(elementComponentData, indexComponentData);

        // Creating common data
        CommonCollectionMapperData commonCollectionMapperData = new CommonCollectionMapperData(
                mainGenerator.getVerEntCfg(), auditMiddleEntityName,
                propertyAuditingData.getPropertyData(),
                referencingIdData, queryGenerator);

        // Checking the type of the collection and adding an appropriate mapper.
        addMapper(commonCollectionMapperData, elementComponentData, indexComponentData);

        // ******
        // Storing information about this relation.
        // ******
        storeMiddleEntityRelationInformation(mappedBy);
    }

    private MiddleComponentData addIndex(Element middleEntityXml, QueryGeneratorBuilder queryGeneratorBuilder) {
        if (propertyValue instanceof IndexedCollection) {
            IndexedCollection indexedValue = (IndexedCollection) propertyValue;
            String mapKey = propertyAuditingData.getMapKey();
            if (mapKey == null) {
                // This entity doesn't specify a javax.persistence.MapKey. Mapping it to the middle entity.
                return addValueToMiddleTable(indexedValue.getIndex(), middleEntityXml,
                        queryGeneratorBuilder, "mapkey", null);
            } else {
                IdMappingData referencedIdMapping = mainGenerator.getEntitiesConfigurations()
                        .get(referencedEntityName).getIdMappingData();
                int currentIndex = queryGeneratorBuilder == null ? 0 : queryGeneratorBuilder.getCurrentIndex();
                if ("".equals(mapKey)) {
                    // The key of the map is the id of the entity.
                    return new MiddleComponentData(new MiddleMapKeyIdComponentMapper(mainGenerator.getVerEntCfg(),
                            referencedIdMapping.getIdMapper()), currentIndex);
                } else {
                    // The key of the map is a property of the entity.
                    return new MiddleComponentData(new MiddleMapKeyPropertyComponentMapper(mapKey,
                            propertyAuditingData.getAccessType()), currentIndex);
                }
            }
        } else {
            // No index - creating a dummy mapper.
            return new MiddleComponentData(new MiddleDummyComponentMapper(), 0);
        }
    }

    /**
     *
     * @param value Value, which should be mapped to the middle-table, either as a relation to another entity,
     * or as a simple value.
     * @param xmlMapping If not <code>null</code>, xml mapping for this value is added to this element.
     * @param queryGeneratorBuilder In case <code>value</code> is a relation to another entity, information about it
     * should be added to the given.
     * @param prefix Prefix for proeprty names of related entities identifiers.
     * @param joinColumns Names of columns to use in the xml mapping, if this array isn't null and has any elements.
     * @return Data for mapping this component.
     */
    @SuppressWarnings({"unchecked"})
    private MiddleComponentData addValueToMiddleTable(Value value, Element xmlMapping,
                                                      QueryGeneratorBuilder queryGeneratorBuilder,
                                                      String prefix, JoinColumn[] joinColumns) {
        Type type = value.getType();
        if (type instanceof ManyToOneType) {
            String prefixRelated = prefix + "_";

            String referencedEntityName = MappingTools.getReferencedEntityName(value);

            IdMappingData referencedIdMapping = mainGenerator.getReferencedIdMappingData(referencingEntityName,
                    referencedEntityName, propertyAuditingData, true);

            // Adding related-entity (in this case: the referenced entities id) id mapping to the xml only if the
            // relation isn't inverse (so when <code>xmlMapping</code> is not null).
            if (xmlMapping != null) {
                addRelatedToXmlMapping(xmlMapping, prefixRelated,
                        joinColumns != null && joinColumns.length > 0
                                ? MetadataTools.getColumnNameIterator(joinColumns)
                                : MetadataTools.getColumnNameIterator(value.getColumnIterator()),
                        referencedIdMapping);
            }

            // Storing the id data of the referenced entity: original mapper, prefixed mapper and entity name.
            MiddleIdData referencedIdData = createMiddleIdData(referencedIdMapping,
                    prefixRelated, referencedEntityName);
            // And adding it to the generator builder.
            queryGeneratorBuilder.addRelation(referencedIdData);

            return new MiddleComponentData(new MiddleRelatedComponentMapper(referencedIdData),
                    queryGeneratorBuilder.getCurrentIndex());
        } else {
            // Last but one parameter: collection components are always insertable
            boolean mapped = mainGenerator.getBasicMetadataGenerator().addBasic(xmlMapping,
                    new PropertyAuditingData(prefix, "field", ModificationStore.FULL, RelationTargetAuditMode.AUDITED, null, null, false),
                    value, null, true, true);

            if (mapped) {
                // Simple values are always stored in the first item of the array returned by the query generator.
                return new MiddleComponentData(new MiddleSimpleComponentMapper(mainGenerator.getVerEntCfg(), prefix), 0);
            } else {
                mainGenerator.throwUnsupportedTypeException(type, referencingEntityName, propertyName);
                // Impossible to get here.
                throw new AssertionError();
            }
        }
    }

    private void addMapper(CommonCollectionMapperData commonCollectionMapperData, MiddleComponentData elementComponentData,
                           MiddleComponentData indexComponentData) {
        Type type = propertyValue.getType();
        if (type instanceof SortedSetType) {
            currentMapper.addComposite(propertyAuditingData.getPropertyData(),
                    new BasicCollectionMapper<Set>(commonCollectionMapperData,
                    TreeSet.class, SortedSetProxy.class, elementComponentData));
        } else if (type instanceof SetType) {
            currentMapper.addComposite(propertyAuditingData.getPropertyData(),
                    new BasicCollectionMapper<Set>(commonCollectionMapperData,
                    HashSet.class, SetProxy.class, elementComponentData));
        } else if (type instanceof SortedMapType) {
            // Indexed collection, so <code>indexComponentData</code> is not null.
            currentMapper.addComposite(propertyAuditingData.getPropertyData(),
                    new MapCollectionMapper<Map>(commonCollectionMapperData,
                    TreeMap.class, SortedMapProxy.class, elementComponentData, indexComponentData));
        } else if (type instanceof MapType) {
            // Indexed collection, so <code>indexComponentData</code> is not null.
            currentMapper.addComposite(propertyAuditingData.getPropertyData(),
                    new MapCollectionMapper<Map>(commonCollectionMapperData,
                    HashMap.class, MapProxy.class, elementComponentData, indexComponentData));
        } else if (type instanceof BagType) {
            currentMapper.addComposite(propertyAuditingData.getPropertyData(),
                    new BasicCollectionMapper<List>(commonCollectionMapperData,
                    ArrayList.class, ListProxy.class, elementComponentData));
        } else if (type instanceof ListType) {
            // Indexed collection, so <code>indexComponentData</code> is not null.
            currentMapper.addComposite(propertyAuditingData.getPropertyData(),
                    new ListCollectionMapper(commonCollectionMapperData,
                    elementComponentData, indexComponentData));
        } else {
            mainGenerator.throwUnsupportedTypeException(type, referencingEntityName, propertyName);
        }
    }

    private void storeMiddleEntityRelationInformation(String mappedBy) {
        // Only if this is a relation (when there is a referenced entity).
        if (referencedEntityName != null) {
            if (propertyValue.isInverse()) {
                referencingEntityConfiguration.addToManyMiddleNotOwningRelation(propertyName, mappedBy, referencedEntityName);
            } else {
                referencingEntityConfiguration.addToManyMiddleRelation(propertyName, referencedEntityName);
            }
        }
    }

    private Element createMiddleEntityXml(String auditMiddleTableName, String auditMiddleEntityName, String where) {
        String schema = mainGenerator.getSchema(propertyAuditingData.getJoinTable().schema(), propertyValue.getCollectionTable());
        String catalog = mainGenerator.getCatalog(propertyAuditingData.getJoinTable().catalog(), propertyValue.getCollectionTable());

        Element middleEntityXml = MetadataTools.createEntity(xmlMappingData.newAdditionalMapping(),
                new AuditTableData(auditMiddleEntityName, auditMiddleTableName, schema, catalog), null);
        Element middleEntityXmlId = middleEntityXml.addElement("composite-id");

        // If there is a where clause on the relation, adding it to the middle entity.
        if (where != null) {
            middleEntityXml.addAttribute("where", where);
        }

        middleEntityXmlId.addAttribute("name", mainGenerator.getVerEntCfg().getOriginalIdPropName());

        // Adding the revision number as a foreign key to the revision info entity to the composite id of the
        // middle table.
        mainGenerator.addRevisionInfoRelation(middleEntityXmlId);

        // Adding the revision type property to the entity xml.
        mainGenerator.addRevisionType(middleEntityXml);

        // All other properties should also be part of the primary key of the middle entity.
        return middleEntityXmlId;
    }

    @SuppressWarnings({"unchecked"})
    private String getMappedBy(Collection collectionValue) {
        PersistentClass referencedClass = ((OneToMany) collectionValue.getElement()).getAssociatedClass();

        // If there's an @AuditMappedBy specified, returning it directly.
        String auditMappedBy = propertyAuditingData.getAuditMappedBy();
        if (auditMappedBy != null) {
            return auditMappedBy;
        }

        Iterator<Property> assocClassProps = referencedClass.getPropertyIterator();

        while (assocClassProps.hasNext()) {
            Property property = assocClassProps.next();

            if (Tools.iteratorsContentEqual(property.getValue().getColumnIterator(),
                    collectionValue.getKey().getColumnIterator())) {
                return property.getName();
            }
        }

        throw new MappingException("Unable to read the mapped by attribute for " + propertyName + " in "
                + referencingEntityName + "!");
    }

    @SuppressWarnings({"unchecked"})
    private String getMappedBy(Table collectionTable, PersistentClass referencedClass) {
        // If there's an @AuditMappedBy specified, returning it directly.
        String auditMappedBy = propertyAuditingData.getAuditMappedBy();
        if (auditMappedBy != null) {
            return auditMappedBy;
        }

        Iterator<Property> properties = referencedClass.getPropertyIterator();
        while (properties.hasNext()) {
            Property property = properties.next();
            if (property.getValue() instanceof Collection) {
                // The equality is intentional. We want to find a collection property with the same collection table.
                //noinspection ObjectEquality
                if (((Collection) property.getValue()).getCollectionTable() == collectionTable) {
                    return property.getName();
                }
            }
        }

        throw new MappingException("Unable to read the mapped by attribute for " + propertyName + " in "
                + referencingEntityName + "!");
    }
}
