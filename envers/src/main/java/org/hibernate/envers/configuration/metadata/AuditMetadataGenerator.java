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
import java.util.Map;

import org.dom4j.Element;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.IdMappingData;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.entities.mapper.SubclassPropertyMapper;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.tools.Triple;
import org.hibernate.envers.RelationTargetAuditMode;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.*;
import org.hibernate.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 * @author Tomasz Bech
 */
public final class AuditMetadataGenerator {
    private static final Logger log = LoggerFactory.getLogger(AuditMetadataGenerator.class);

    private final Configuration cfg;
    private final GlobalConfiguration globalCfg;
    private final AuditEntitiesConfiguration verEntCfg;
    private final Element revisionInfoRelationMapping;

    /*
     * Generators for different kinds of property values/types.
     */
    private final BasicMetadataGenerator basicMetadataGenerator;
	private final ComponentMetadataGenerator componentMetadataGenerator;
    private final IdMetadataGenerator idMetadataGenerator;
    private final ToOneRelationMetadataGenerator toOneRelationMetadataGenerator;

    /*
     * Here information about already generated mappings will be accumulated.
     */
    private final Map<String, EntityConfiguration> entitiesConfigurations;
    private final Map<String, EntityConfiguration> notAuditedEntitiesConfigurations;

    private final AuditEntityNameRegister auditEntityNameRegister;

    // Map entity name -> (join descriptor -> element describing the "versioned" join)
    private final Map<String, Map<Join, Element>> entitiesJoins;

    public AuditMetadataGenerator(Configuration cfg, GlobalConfiguration globalCfg,
                                  AuditEntitiesConfiguration verEntCfg,
                                  Element revisionInfoRelationMapping,
                                  AuditEntityNameRegister auditEntityNameRegister) {
        this.cfg = cfg;
        this.globalCfg = globalCfg;
        this.verEntCfg = verEntCfg;
        this.revisionInfoRelationMapping = revisionInfoRelationMapping;

        this.basicMetadataGenerator = new BasicMetadataGenerator();
		this.componentMetadataGenerator = new ComponentMetadataGenerator(this);
        this.idMetadataGenerator = new IdMetadataGenerator(this);
        this.toOneRelationMetadataGenerator = new ToOneRelationMetadataGenerator(this);

        this.auditEntityNameRegister = auditEntityNameRegister;

        entitiesConfigurations = new HashMap<String, EntityConfiguration>();
        notAuditedEntitiesConfigurations = new HashMap<String, EntityConfiguration>();
        entitiesJoins = new HashMap<String, Map<Join, Element>>();
    }

    /**
     * Clones the revision info relation mapping, so that it can be added to other mappings. Also, the name of
     * the property and the column are set properly.
     * @return A revision info mapping, which can be added to other mappings (has no parent).
     */
    private Element cloneAndSetupRevisionInfoRelationMapping() {
        Element rev_mapping = (Element) revisionInfoRelationMapping.clone();
        rev_mapping.addAttribute("name", verEntCfg.getRevisionFieldName());

        MetadataTools.addOrModifyColumn(rev_mapping, verEntCfg.getRevisionFieldName());

        return rev_mapping;
    }

    void addRevisionInfoRelation(Element any_mapping) {
        any_mapping.add(cloneAndSetupRevisionInfoRelationMapping());
    }

    void addRevisionType(Element any_mapping) {
        Element revTypeProperty = MetadataTools.addProperty(any_mapping, verEntCfg.getRevisionTypePropName(),
                verEntCfg.getRevisionTypePropType(), true, false);
        revTypeProperty.addAttribute("type", "org.hibernate.envers.entities.RevisionTypeType");
    }

    @SuppressWarnings({"unchecked"})
    void addValue(Element parent, Value value, CompositeMapperBuilder currentMapper, String entityName,
                  EntityXmlMappingData xmlMappingData, PropertyAuditingData propertyAuditingData,
                  boolean insertable, boolean firstPass) {
        Type type = value.getType();

        // only first pass
        if (firstPass) {
            if (basicMetadataGenerator.addBasic(parent, propertyAuditingData, value, currentMapper,
                    insertable, false)) {
                // The property was mapped by the basic generator.
                return;
            }
        }

		if (type instanceof ComponentType) {
			// both passes
			componentMetadataGenerator.addComponent(parent, propertyAuditingData, value, currentMapper,
					entityName, xmlMappingData, firstPass);
		} else if (type instanceof ManyToOneType) {
            // only second pass
            if (!firstPass) {
                toOneRelationMetadataGenerator.addToOne(parent, propertyAuditingData, value, currentMapper,
                        entityName, insertable);
            }
        } else if (type instanceof OneToOneType) {
            // only second pass
            if (!firstPass) {
                toOneRelationMetadataGenerator.addOneToOneNotOwning(propertyAuditingData, value,
                        currentMapper, entityName);
            }
        } else if (type instanceof CollectionType) {
            // only second pass
            if (!firstPass) {
                CollectionMetadataGenerator collectionMetadataGenerator = new CollectionMetadataGenerator(this,
                        (Collection) value, currentMapper, entityName, xmlMappingData,
						propertyAuditingData);
                collectionMetadataGenerator.addCollection();
            }
        } else {
            if (firstPass) {
                // If we got here in the first pass, it means the basic mapper didn't map it, and none of the
                // above branches either.
                throwUnsupportedTypeException(type, entityName, propertyAuditingData.getName());
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addProperties(Element parent, Iterator<Property> properties, CompositeMapperBuilder currentMapper,
                               ClassAuditingData auditingData, String entityName, EntityXmlMappingData xmlMappingData,
                               boolean firstPass) {
        while (properties.hasNext()) {
            Property property = properties.next();
            String propertyName = property.getName();
			PropertyAuditingData propertyAuditingData = auditingData.getPropertyAuditingData(propertyName);
            if (propertyAuditingData != null) {
				addValue(parent, property.getValue(), currentMapper, entityName, xmlMappingData, propertyAuditingData,
						property.isInsertable(), firstPass);
            }
        }
    }

	private boolean checkPropertiesAudited(Iterator<Property> properties, ClassAuditingData auditingData) {
		while (properties.hasNext()) {
			Property property = properties.next();
            String propertyName = property.getName();
			PropertyAuditingData propertyAuditingData = auditingData.getPropertyAuditingData(propertyName);
            if (propertyAuditingData == null) {
				return false;
			}
		}

		return true;
	}

    protected String getSchema(String schemaFromAnnotation, Table table) {
        // Get the schema from the annotation ...
        String schema = schemaFromAnnotation;
        // ... if empty, try using the default ...
        if (StringTools.isEmpty(schema)) {
            schema = globalCfg.getDefaultSchemaName();

            // ... if still empty, use the same as the normal table.
            if (StringTools.isEmpty(schema)) {
                schema = table.getSchema();
            }
        }

        return schema;
    }

    protected String getCatalog(String catalogFromAnnotation, Table table) {
        // Get the catalog from the annotation ...
        String catalog = catalogFromAnnotation;
        // ... if empty, try using the default ...
        if (StringTools.isEmpty(catalog)) {
            catalog = globalCfg.getDefaultCatalogName();

            // ... if still empty, use the same as the normal table.
            if (StringTools.isEmpty(catalog)) {
                catalog = table.getCatalog();
            }
        }

        return catalog;
    }

    @SuppressWarnings({"unchecked"})
    private void createJoins(PersistentClass pc, Element parent, ClassAuditingData auditingData) {
        Iterator<Join> joins = pc.getJoinIterator();

        Map<Join, Element> joinElements = new HashMap<Join, Element>();
        entitiesJoins.put(pc.getEntityName(), joinElements);

        while (joins.hasNext()) {
            Join join = joins.next();

			// Checking if all of the join properties are audited
			if (!checkPropertiesAudited(join.getPropertyIterator(), auditingData)) {
				continue;
			}

            // Determining the table name. If there is no entry in the dictionary, just constructing the table name
            // as if it was an entity (by appending/prepending configured strings).
            String originalTableName = join.getTable().getName();
            String auditTableName = auditingData.getSecondaryTableDictionary().get(originalTableName);
            if (auditTableName == null) {
                auditTableName = verEntCfg.getAuditEntityName(originalTableName);
            }

            String schema = getSchema(auditingData.getAuditTable().schema(), join.getTable());
            String catalog = getCatalog(auditingData.getAuditTable().catalog(), join.getTable());

            Element joinElement = MetadataTools.createJoin(parent, auditTableName, schema, catalog);
            joinElements.put(join, joinElement);

            Element joinKey = joinElement.addElement("key");
            MetadataTools.addColumns(joinKey, join.getKey().getColumnIterator());
            MetadataTools.addColumn(joinKey, verEntCfg.getRevisionFieldName(), null, 0, 0, null);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addJoins(PersistentClass pc, CompositeMapperBuilder currentMapper, ClassAuditingData auditingData,
                          String entityName, EntityXmlMappingData xmlMappingData,boolean firstPass) {
        Iterator<Join> joins = pc.getJoinIterator();

        while (joins.hasNext()) {
            Join join = joins.next();
            Element joinElement = entitiesJoins.get(entityName).get(join);

			if (joinElement != null) {
            	addProperties(joinElement, join.getPropertyIterator(), currentMapper, auditingData, entityName,
                	    xmlMappingData, firstPass);
			}
        }
    }

    @SuppressWarnings({"unchecked"})
    private Triple<Element, ExtendedPropertyMapper, String> generateMappingData(
            PersistentClass pc, EntityXmlMappingData xmlMappingData, AuditTableData auditTableData,
            IdMappingData idMapper) {
        Element class_mapping = MetadataTools.createEntity(xmlMappingData.getMainXmlMapping(), auditTableData,
                pc.getDiscriminatorValue());
        ExtendedPropertyMapper propertyMapper = new MultiPropertyMapper();

        // Checking if there is a discriminator column
        if (pc.getDiscriminator() != null) {
            Element discriminator_element = class_mapping.addElement("discriminator");
            MetadataTools.addColumns(discriminator_element, pc.getDiscriminator().getColumnIterator());
            discriminator_element.addAttribute("type", pc.getDiscriminator().getType().getName());
        }

        // Adding the id mapping
        class_mapping.add((Element) idMapper.getXmlMapping().clone());

        // Adding the "revision type" property
        addRevisionType(class_mapping);

        return Triple.make(class_mapping, propertyMapper, null);
    }

    private Triple<Element, ExtendedPropertyMapper, String> generateInheritanceMappingData(
            PersistentClass pc, EntityXmlMappingData xmlMappingData, AuditTableData auditTableData,
            String inheritanceMappingType) {
        String extendsEntityName = verEntCfg.getAuditEntityName(pc.getSuperclass().getEntityName());
        Element class_mapping = MetadataTools.createSubclassEntity(xmlMappingData.getMainXmlMapping(),
                inheritanceMappingType, auditTableData, extendsEntityName, pc.getDiscriminatorValue());

        // The id and revision type is already mapped in the parent

        // Getting the property mapper of the parent - when mapping properties, they need to be included
        String parentEntityName = pc.getSuperclass().getEntityName();

        EntityConfiguration parentConfiguration = entitiesConfigurations.get(parentEntityName);
        if (parentConfiguration == null) {
            throw new MappingException("Entity '" + pc.getEntityName() + "' is audited, but its superclass: '" +
                    parentEntityName + "' is not.");
        }
        
        ExtendedPropertyMapper parentPropertyMapper = parentConfiguration.getPropertyMapper();
        ExtendedPropertyMapper propertyMapper = new SubclassPropertyMapper(new MultiPropertyMapper(), parentPropertyMapper);

        return Triple.make(class_mapping, propertyMapper, parentEntityName);
    }

    @SuppressWarnings({"unchecked"})
    public void generateFirstPass(PersistentClass pc, ClassAuditingData auditingData,
                                  EntityXmlMappingData xmlMappingData, boolean isAudited) {
        String schema = getSchema(auditingData.getAuditTable().schema(), pc.getTable());
        String catalog = getCatalog(auditingData.getAuditTable().catalog(), pc.getTable());

		if (!isAudited) {
			String entityName = pc.getEntityName();
			IdMappingData idMapper = idMetadataGenerator.addId(pc, false);

            if (idMapper == null) {
                // Unsupported id mapping, e.g. key-many-to-one. If the entity is used in auditing, an exception
                // will be thrown later on.
                log.debug("Unable to create auditing id mapping for entity " + entityName +
                        ", because of an unsupported Hibernate id mapping (e.g. key-many-to-one).");
                return;
            }

			ExtendedPropertyMapper propertyMapper = null;
			String parentEntityName = null;
			EntityConfiguration entityCfg = new EntityConfiguration(entityName, idMapper, propertyMapper,
					parentEntityName);
			notAuditedEntitiesConfigurations.put(entityName, entityCfg);
			return;
		}

        String entityName = pc.getEntityName();
        log.debug("Generating first-pass auditing mapping for entity " + entityName + ".");

        String auditEntityName = verEntCfg.getAuditEntityName(entityName);
        String auditTableName = verEntCfg.getAuditTableName(entityName, pc.getTable().getName());

        // Registering the audit entity name, now that it is known
        auditEntityNameRegister.register(auditEntityName);

        AuditTableData auditTableData = new AuditTableData(auditEntityName, auditTableName, schema, catalog);

        // Generating a mapping for the id
        IdMappingData idMapper = idMetadataGenerator.addId(pc, true);

        InheritanceType inheritanceType = InheritanceType.get(pc);

        // These properties will be read from the mapping data
        final Element class_mapping;
        final ExtendedPropertyMapper propertyMapper;
        final String parentEntityName;

        final Triple<Element, ExtendedPropertyMapper, String> mappingData;

        // Reading the mapping data depending on inheritance type (if any)
        switch (inheritanceType) {
            case NONE:
                mappingData = generateMappingData(pc, xmlMappingData, auditTableData, idMapper);
                break;

            case SINGLE:
                mappingData = generateInheritanceMappingData(pc, xmlMappingData, auditTableData, "subclass");
                break;

            case JOINED:
                mappingData = generateInheritanceMappingData(pc, xmlMappingData, auditTableData, "joined-subclass");

                // Adding the "key" element with all id columns...
                Element keyMapping = mappingData.getFirst().addElement("key");
                MetadataTools.addColumns(keyMapping, pc.getTable().getPrimaryKey().columnIterator());

                // ... and the revision number column, read from the revision info relation mapping.
                keyMapping.add((Element) cloneAndSetupRevisionInfoRelationMapping().element("column").clone());
                break;

            case TABLE_PER_CLASS:
                mappingData = generateInheritanceMappingData(pc, xmlMappingData, auditTableData, "union-subclass");
                break;

            default:
                throw new AssertionError("Impossible enum value.");
        }

        class_mapping = mappingData.getFirst();
        propertyMapper = mappingData.getSecond();
        parentEntityName = mappingData.getThird();

        xmlMappingData.setClassMapping(class_mapping);

        // Mapping unjoined properties
        addProperties(class_mapping, (Iterator<Property>) pc.getUnjoinedPropertyIterator(), propertyMapper,
                auditingData, pc.getEntityName(), xmlMappingData,
                true);

        // Creating and mapping joins (first pass)
        createJoins(pc, class_mapping, auditingData);
        addJoins(pc, propertyMapper, auditingData, pc.getEntityName(), xmlMappingData, true);

        // Storing the generated configuration
        EntityConfiguration entityCfg = new EntityConfiguration(auditEntityName, idMapper,
                propertyMapper, parentEntityName);
        entitiesConfigurations.put(pc.getEntityName(), entityCfg);
    }

    @SuppressWarnings({"unchecked"})
    public void generateSecondPass(PersistentClass pc, ClassAuditingData auditingData,
                                   EntityXmlMappingData xmlMappingData) {
        String entityName = pc.getEntityName();
        log.debug("Generating second-pass auditing mapping for entity " + entityName + ".");

        CompositeMapperBuilder propertyMapper = entitiesConfigurations.get(entityName).getPropertyMapper();

        // Mapping unjoined properties
        Element parent = xmlMappingData.getClassMapping();

        addProperties(parent, (Iterator<Property>) pc.getUnjoinedPropertyIterator(),
                propertyMapper, auditingData, entityName, xmlMappingData, false);

        // Mapping joins (second pass)
        addJoins(pc, propertyMapper, auditingData, entityName, xmlMappingData, false);
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

    AuditEntitiesConfiguration getVerEntCfg() {
        return verEntCfg;
    }

    AuditEntityNameRegister getAuditEntityNameRegister() {
        return auditEntityNameRegister;
    }

    void throwUnsupportedTypeException(Type type, String entityName, String propertyName) {
        String message = "Type not supported for auditing: " + type.getClass().getName() +
                ", on entity " + entityName + ", property '" + propertyName + "'.";

        throw new MappingException(message);
    }

    /**
     * Reads the id mapping data of a referenced entity.
     * @param entityName Name of the entity which is the source of the relation.
     * @param referencedEntityName Name of the entity which is the target of the relation.
     * @param propertyAuditingData Auditing data of the property that is the source of the relation.
     * @param allowNotAuditedTarget Are not-audited target entities allowed.
     * @throws MappingException If a relation from an audited to a non-audited entity is detected, which is not
     * mapped using {@link RelationTargetAuditMode#NOT_AUDITED}.
     * @return The id mapping data of the related entity. 
     */
    IdMappingData getReferencedIdMappingData(String entityName, String referencedEntityName,
                                             PropertyAuditingData propertyAuditingData,
                                             boolean allowNotAuditedTarget) {
        EntityConfiguration configuration = getEntitiesConfigurations().get(referencedEntityName);
		if (configuration == null) {
            RelationTargetAuditMode relationTargetAuditMode = propertyAuditingData.getRelationTargetAuditMode();
			configuration = getNotAuditedEntitiesConfigurations().get(referencedEntityName);

			if (configuration == null || !allowNotAuditedTarget || !RelationTargetAuditMode.NOT_AUDITED.equals(relationTargetAuditMode)) {
				throw new MappingException("An audited relation from " + entityName + "."
						+ propertyAuditingData.getName() + " to a not audited entity " + referencedEntityName + "!"
						+ (allowNotAuditedTarget ?
                            " Such mapping is possible, but has to be explicitly defined using @Audited(targetAuditMode = NOT_AUDITED)." :
                            ""));
			}
		}

        return configuration.getIdMappingData();
    }

	/**
	 * Get the notAuditedEntitiesConfigurations property.
	 *
	 * @return the notAuditedEntitiesConfigurations property value
	 */
	public Map<String, EntityConfiguration> getNotAuditedEntitiesConfigurations() {
		return notAuditedEntitiesConfigurations;
	}
}
