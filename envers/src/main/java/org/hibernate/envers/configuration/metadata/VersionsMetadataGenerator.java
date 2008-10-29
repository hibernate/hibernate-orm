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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.VersionsJoinTable;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.configuration.VersionsEntitiesConfiguration;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.IdMappingData;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.entities.mapper.SubclassPropertyMapper;
import org.hibernate.envers.entity.VersionsInheritanceEntityPersister;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.tools.log.YLog;
import org.hibernate.envers.tools.log.YLogManager;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 */
public final class VersionsMetadataGenerator {
    private final Configuration cfg;
    private final GlobalConfiguration globalCfg;
    private final VersionsEntitiesConfiguration verEntCfg;
    private final Element revisionInfoRelationMapping;

    private final BasicMetadataGenerator basicMetadataGenerator;
    private final IdMetadataGenerator idMetadataGenerator;
    private final ToOneRelationMetadataGenerator toOneRelationMetadataGenerator;

    private final Map<String, EntityConfiguration> entitiesConfigurations;

    // Map entity name -> (join descriptor -> element describing the "versioned" join)
    private final Map<String, Map<Join, Element>> entitiesJoins;

    private YLog log = YLogManager.getLogManager().getLog(VersionsMetadataGenerator.class);

    public VersionsMetadataGenerator(Configuration cfg, GlobalConfiguration globalCfg,
                                     VersionsEntitiesConfiguration verEntCfg,
                                     Element revisionInfoRelationMapping) {
        this.cfg = cfg;
        this.globalCfg = globalCfg;
        this.verEntCfg = verEntCfg;
        this.revisionInfoRelationMapping = revisionInfoRelationMapping;

        this.basicMetadataGenerator = new BasicMetadataGenerator();
        this.idMetadataGenerator = new IdMetadataGenerator(this);
        this.toOneRelationMetadataGenerator = new ToOneRelationMetadataGenerator(this);

        entitiesConfigurations = new HashMap<String, EntityConfiguration>();
        entitiesJoins = new HashMap<String, Map<Join, Element>>();
    }

    void addRevisionInfoRelation(Element any_mapping) {
        Element rev_mapping = (Element) revisionInfoRelationMapping.clone();
        rev_mapping.addAttribute("name", verEntCfg.getRevisionPropName());
        MetadataTools.addColumn(rev_mapping, verEntCfg.getRevisionPropName(), null);

        any_mapping.add(rev_mapping);
    }

    void addRevisionType(Element any_mapping) {
        Element revTypeProperty = MetadataTools.addProperty(any_mapping, verEntCfg.getRevisionTypePropName(),
                verEntCfg.getRevisionTypePropType(), true, false);
        revTypeProperty.addAttribute("type", "org.hibernate.envers.entities.RevisionTypeType");
    }

    private ModificationStore getStoreForProperty(Property property, PropertyStoreInfo propertyStoreInfo,
                                                  List<String> unversionedProperties) {
        /*
         * Checks if a property is versioned, which is when:
         * - the property isn't unversioned
         * - the whole entity is versioned, then the default store is not null
         * - there is a store defined for this entity, which is when this property is annotated 
         */

        if (unversionedProperties.contains(property.getName())) {
            return null;
        }

        ModificationStore store = propertyStoreInfo.propertyStores.get(property.getName());

        if (store == null) {
            return propertyStoreInfo.defaultStore;
        }

        return store;
    }

    @SuppressWarnings({"unchecked"})
    void addValue(Element parent, String name, Value value, CompositeMapperBuilder currentMapper,
                  ModificationStore store, String entityName, EntityXmlMappingData xmlMappingData,
                  VersionsJoinTable joinTable, String mapKey, boolean insertable, boolean firstPass) {
        Type type = value.getType();

        // only first pass
        if (firstPass) {
            if (basicMetadataGenerator.addBasic(parent, name, value, currentMapper, store, entityName, insertable,
                    false)) {
                // The property was mapped by the basic generator.
                return;
            }
        }

        if (type instanceof ManyToOneType) {
            // only second pass
            if (!firstPass) {
                toOneRelationMetadataGenerator.addToOne(parent, name, value, currentMapper, entityName);
            }
        } else if (type instanceof OneToOneType) {
            // only second pass
            if (!firstPass) {
                toOneRelationMetadataGenerator.addOneToOneNotOwning(name, value, currentMapper, entityName);
            }
        } else if (type instanceof CollectionType) {
            // only second pass
            if (!firstPass) {
                CollectionMetadataGenerator collectionMetadataGenerator = new CollectionMetadataGenerator(this,
                        name, (Collection) value, currentMapper, entityName, xmlMappingData, joinTable, mapKey);
                collectionMetadataGenerator.addCollection();
            }
        } else {
            if (firstPass) {
                // If we got here in the first pass, it means the basic mapper didn't map it, and none of the
                // above branches either.
                throwUnsupportedTypeException(type, entityName, name);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addProperties(Element parent, Iterator<Property> properties, CompositeMapperBuilder currentMapper,
                               PersistentClassVersioningData versioningData, String entityName, EntityXmlMappingData xmlMappingData,
                               boolean firstPass) {
        while (properties.hasNext()) {
            Property property = properties.next();
            if (!"_identifierMapper".equals(property.getName())) {
                ModificationStore store = getStoreForProperty(property, versioningData.propertyStoreInfo,
                        versioningData.unversionedProperties);

                if (store != null) {
                    addValue(parent, property.getName(), property.getValue(), currentMapper, store, entityName,
                            xmlMappingData, versioningData.versionsJoinTables.get(property.getName()),
                            versioningData.mapKeys.get(property.getName()), property.isInsertable(), firstPass);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void createJoins(PersistentClass pc, Element parent, PersistentClassVersioningData versioningData) {
        Iterator<Join> joins = pc.getJoinIterator();

        Map<Join, Element> joinElements = new HashMap<Join, Element>();
        entitiesJoins.put(pc.getEntityName(), joinElements);

        while (joins.hasNext()) {
            Join join = joins.next();

            // Determining the table name. If there is no entry in the dictionary, just constructing the table name
            // as if it was an entity (by appending/prepending configured strings).
            String originalTableName = join.getTable().getName();
            String versionedTableName = versioningData.secondaryTableDictionary.get(originalTableName);
            if (versionedTableName == null) {
                versionedTableName = verEntCfg.getVersionsEntityName(originalTableName);
            }

            String schema = versioningData.versionsTable.schema();
            if (StringTools.isEmpty(schema)) {
                schema = join.getTable().getSchema();
            }

            String catalog = versioningData.versionsTable.catalog();
            if (StringTools.isEmpty(catalog)) {
                catalog = join.getTable().getCatalog();
            }

            Element joinElement = MetadataTools.createJoin(parent, versionedTableName, schema, catalog);
            joinElements.put(join, joinElement);

            Element joinKey = joinElement.addElement("key");
            MetadataTools.addColumns(joinKey, join.getKey().getColumnIterator());
            MetadataTools.addColumn(joinKey, verEntCfg.getRevisionPropName(), null);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addJoins(PersistentClass pc, CompositeMapperBuilder currentMapper, PersistentClassVersioningData versioningData,
                          String entityName, EntityXmlMappingData xmlMappingData,boolean firstPass) {
        Iterator<Join> joins = pc.getJoinIterator();

        while (joins.hasNext()) {
            Join join = joins.next();
            Element joinElement = entitiesJoins.get(entityName).get(join);

            addProperties(joinElement, join.getPropertyIterator(), currentMapper, versioningData, entityName,
                    xmlMappingData, firstPass);
        }
    }

    private void addPersisterHack(Element class_mapping) {
        class_mapping.addAttribute("persister", VersionsInheritanceEntityPersister.class.getName() );
    }

    @SuppressWarnings({"unchecked"})
    public void generateFirstPass(PersistentClass pc, PersistentClassVersioningData versioningData,
                                  EntityXmlMappingData xmlMappingData) {
        String schema = versioningData.versionsTable.schema();
        if (StringTools.isEmpty(schema)) {
            schema = pc.getTable().getSchema();
        }

        String catalog = versioningData.versionsTable.catalog();
        if (StringTools.isEmpty(catalog)) {
            catalog = pc.getTable().getCatalog();
        }

        String entityName = pc.getEntityName();
        String versionsEntityName = verEntCfg.getVersionsEntityName(entityName);
        String versionsTableName = verEntCfg.getVersionsTableName(entityName, pc.getTable().getName());

        // Generating a mapping for the id
        IdMappingData idMapper = idMetadataGenerator.addId(pc);

        Element class_mapping;
        ExtendedPropertyMapper propertyMapper;

        InheritanceType inheritanceType = InheritanceType.get(pc);
        String parentEntityName = null;

        switch (inheritanceType) {
            case NONE:
                class_mapping = MetadataTools.createEntity(xmlMappingData.getMainXmlMapping(), versionsEntityName, versionsTableName,
                        schema, catalog, pc.getDiscriminatorValue());
                propertyMapper = new MultiPropertyMapper();

                // Checking if there is a discriminator column
                if (pc.getDiscriminator() != null) {
                    Element discriminator_element = class_mapping.addElement("discriminator");
                    MetadataTools.addColumns(discriminator_element, pc.getDiscriminator().getColumnIterator());
                    discriminator_element.addAttribute("type", pc.getDiscriminator().getType().getName());

                    // If so, there is some inheritance scheme -> using the persister hack.
                    addPersisterHack(class_mapping);
                }

                // Adding the id mapping
                class_mapping.add((Element) idMapper.getXmlMapping().clone());

                // Adding the "revision type" property
                addRevisionType(class_mapping);

                break;
            case SINGLE:
                String extendsEntityName = verEntCfg.getVersionsEntityName(pc.getSuperclass().getEntityName());
                class_mapping = MetadataTools.createSubclassEntity(xmlMappingData.getMainXmlMapping(), versionsEntityName,
                        versionsTableName, schema, catalog, extendsEntityName, pc.getDiscriminatorValue());

                addPersisterHack(class_mapping);

                // The id and revision type is already mapped in the parent

                // Getting the property mapper of the parent - when mapping properties, they need to be included
                parentEntityName = pc.getSuperclass().getEntityName();
                ExtendedPropertyMapper parentPropertyMapper = entitiesConfigurations.get(parentEntityName).getPropertyMapper();
                propertyMapper = new SubclassPropertyMapper(new MultiPropertyMapper(), parentPropertyMapper);

                break;
            case JOINED:
                throw new MappingException("Joined inheritance strategy not supported for versioning!");
            case TABLE_PER_CLASS:
                throw new MappingException("Table-per-class inheritance strategy not supported for versioning!");
            default:
                throw new AssertionError("Impossible enum value.");
        }

        // Mapping unjoined properties
        addProperties(class_mapping, (Iterator<Property>) pc.getUnjoinedPropertyIterator(), propertyMapper,
                versioningData, pc.getEntityName(), xmlMappingData,
                true);

        // Creating and mapping joins (first pass)
        createJoins(pc, class_mapping, versioningData);
        addJoins(pc, propertyMapper, versioningData, pc.getEntityName(), xmlMappingData, true);

        // Storing the generated configuration
        EntityConfiguration entityCfg = new EntityConfiguration(versionsEntityName, idMapper,
                propertyMapper, parentEntityName);
        entitiesConfigurations.put(pc.getEntityName(), entityCfg);
    }

    @SuppressWarnings({"unchecked"})
    public void generateSecondPass(PersistentClass pc, PersistentClassVersioningData versioningData,
                                   EntityXmlMappingData xmlMappingData) {
        String entityName = pc.getEntityName();

        CompositeMapperBuilder propertyMapper = entitiesConfigurations.get(entityName).getPropertyMapper();

        // Mapping unjoined properties
        Element parent = xmlMappingData.getMainXmlMapping().getRootElement().element("class");
        if (parent == null) {
            parent = xmlMappingData.getMainXmlMapping().getRootElement().element("subclass");
        }

        addProperties(parent, (Iterator<Property>) pc.getUnjoinedPropertyIterator(),
                propertyMapper, versioningData, entityName, xmlMappingData, false);

        // Mapping joins (second pass)
        addJoins(pc, propertyMapper, versioningData, entityName, xmlMappingData, false);
    }

    public Map<String, EntityConfiguration> getEntitiesConfigurations() {
        return entitiesConfigurations;
    }

    // Getters for generators and configuration

    BasicMetadataGenerator getBasicMetadataGenerator() {
        return basicMetadataGenerator;
    }

    Configuration getCfg() {
        return cfg;
    }

    GlobalConfiguration getGlobalCfg() {
        return globalCfg;
    }

    VersionsEntitiesConfiguration getVerEntCfg() {
        return verEntCfg;
    }

    void throwUnsupportedTypeException(Type type, String entityName, String propertyName) {
        String message = "Type not supported for versioning: " + type.getClass().getName() +
                ", on entity " + entityName + ", property '" + propertyName + "'.";
        if (globalCfg.isWarnOnUnsupportedTypes()) {
            log.warn(message);
        } else {
            throw new MappingException(message);
        }
    }
}
