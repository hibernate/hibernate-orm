/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 *
 * See the copyright.txt in the distribution for a  full listing of individual
 * contributors. This copyrighted material is made available to anyone wishing
 * to use,  modify, copy, or redistribute it subject to the terms and
 * conditions of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT A WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.configuration.metadata;

import org.dom4j.Element;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.MappingException;
import org.jboss.envers.entities.mapper.CompositeMapperBuilder;
import org.jboss.envers.entities.mapper.relation.ToOneIdMapper;
import org.jboss.envers.entities.mapper.relation.OneToOneNotOwningMapper;
import org.jboss.envers.entities.mapper.id.IdMapper;
import org.jboss.envers.entities.EntityConfiguration;
import org.jboss.envers.entities.IdMappingData;

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
