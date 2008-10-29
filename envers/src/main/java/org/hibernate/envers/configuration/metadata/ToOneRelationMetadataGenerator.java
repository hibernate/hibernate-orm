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
package org.jboss.envers.configuration.metadata;

import org.dom4j.Element;
import org.jboss.envers.entities.EntityConfiguration;
import org.jboss.envers.entities.IdMappingData;
import org.jboss.envers.entities.mapper.CompositeMapperBuilder;
import org.jboss.envers.entities.mapper.id.IdMapper;
import org.jboss.envers.entities.mapper.relation.OneToOneNotOwningMapper;
import org.jboss.envers.entities.mapper.relation.ToOneIdMapper;

import org.hibernate.MappingException;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * Generates metadata for to-one relations (reference-valued properties).
 * @author Adam Warski (adam at warski dot org)
 */
public final class ToOneRelationMetadataGenerator {
    private final VersionsMetadataGenerator mainGenerator;

    ToOneRelationMetadataGenerator(VersionsMetadataGenerator versionsMetadataGenerator) {
        mainGenerator = versionsMetadataGenerator;
    }

    @SuppressWarnings({"unchecked"})
    void addToOne(Element parent, String name, Value value, CompositeMapperBuilder mapper, String entityName) {
        String referencedEntityName = ((ToOne) value).getReferencedEntityName();

        EntityConfiguration configuration = mainGenerator.getEntitiesConfigurations().get(referencedEntityName);
        if (configuration == null) {
            throw new MappingException("A versioned relation to a non-versioned entity " + referencedEntityName + "!");
        }

        IdMappingData idMapping = configuration.getIdMappingData();

        String lastPropertyPrefix = name + "_";

        // Generating the id mapper for the relation
        IdMapper relMapper = idMapping.getIdMapper().prefixMappedProperties(lastPropertyPrefix);

        // Storing information about this relation
        mainGenerator.getEntitiesConfigurations().get(entityName).addToOneRelation(name, referencedEntityName, relMapper);

        // Adding an element to the mapping corresponding to the references entity id's
        Element properties = (Element) idMapping.getXmlRelationMapping().clone();
        properties.addAttribute("name", name);

        MetadataTools.prefixNamesInPropertyElement(properties, lastPropertyPrefix,
                MetadataTools.getColumnNameIterator(value.getColumnIterator()), false);
        parent.add(properties);

        // Adding mapper for the id
        mapper.addComposite(name, new ToOneIdMapper(relMapper, name, referencedEntityName));
    }

    @SuppressWarnings({"unchecked"})
    void addOneToOneNotOwning(String name, Value value, CompositeMapperBuilder mapper, String entityName) {
        OneToOne propertyValue = (OneToOne) value;

        String owningReferencePropertyName = propertyValue.getReferencedPropertyName(); // mappedBy

        EntityConfiguration configuration = mainGenerator.getEntitiesConfigurations().get(entityName);
        if (configuration == null) {
            throw new MappingException("A versioned relation to a non-versioned entity " + entityName + "!");
        }

        IdMappingData ownedIdMapping = configuration.getIdMappingData();

        if (ownedIdMapping == null) {
            throw new MappingException("A versioned relation to a non-versioned entity " + entityName + "!");
        }

        String lastPropertyPrefix = owningReferencePropertyName + "_";
        String referencedEntityName = propertyValue.getReferencedEntityName();

        // Generating the id mapper for the relation
        IdMapper ownedIdMapper = ownedIdMapping.getIdMapper().prefixMappedProperties(lastPropertyPrefix);

        // Storing information about this relation
        mainGenerator.getEntitiesConfigurations().get(entityName).addToOneNotOwningRelation(name, owningReferencePropertyName,
                referencedEntityName, ownedIdMapper);

        // Adding mapper for the id
        mapper.addComposite(name, new OneToOneNotOwningMapper(owningReferencePropertyName,
                referencedEntityName, name));
    }
}
