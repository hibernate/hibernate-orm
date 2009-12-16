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

import org.dom4j.Element;
import org.hibernate.MappingException;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.IdMappingData;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.entities.mapper.relation.OneToOneNotOwningMapper;
import org.hibernate.envers.entities.mapper.relation.ToOneIdMapper;
import org.hibernate.envers.tools.MappingTools;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

/**
 * Generates metadata for to-one relations (reference-valued properties).
 * @author Adam Warski (adam at warski dot org)
 */
public final class ToOneRelationMetadataGenerator {
    private final AuditMetadataGenerator mainGenerator;

    ToOneRelationMetadataGenerator(AuditMetadataGenerator auditMetadataGenerator) {
        mainGenerator = auditMetadataGenerator;
    }

    @SuppressWarnings({"unchecked"})
    void addToOne(Element parent, PropertyAuditingData propertyAuditingData, Value value,
                  CompositeMapperBuilder mapper, String entityName, boolean insertable) {
        String referencedEntityName = ((ToOne) value).getReferencedEntityName();

        IdMappingData idMapping = mainGenerator.getReferencedIdMappingData(entityName, referencedEntityName,
                propertyAuditingData, true);

        String lastPropertyPrefix = MappingTools.createToOneRelationPrefix(propertyAuditingData.getName());

        // Generating the id mapper for the relation
        IdMapper relMapper = idMapping.getIdMapper().prefixMappedProperties(lastPropertyPrefix);

        // Storing information about this relation
        mainGenerator.getEntitiesConfigurations().get(entityName).addToOneRelation(
                propertyAuditingData.getName(), referencedEntityName, relMapper, insertable);

        // If the property isn't insertable, checking if this is not a "fake" bidirectional many-to-one relationship,
        // that is, when the one side owns the relation (and is a collection), and the many side is non insertable.
        // When that's the case and the user specified to store this relation without a middle table (using
        // @AuditMappedBy), we have to make the property insertable for the purposes of Envers. In case of changes to
        // the entity that didn't involve the relation, it's value will then be stored properly. In case of changes
        // to the entity that did involve the relation, it's the responsibility of the collection side to store the
        // proper data.
        boolean nonInsertableFake;
        if (!insertable && propertyAuditingData.isForceInsertable()) {
            nonInsertableFake = true;
            insertable = true;
        } else {
            nonInsertableFake = false;
        }

        // Adding an element to the mapping corresponding to the references entity id's
        Element properties = (Element) idMapping.getXmlRelationMapping().clone();
        properties.addAttribute("name", propertyAuditingData.getName());

        MetadataTools.prefixNamesInPropertyElement(properties, lastPropertyPrefix,
                MetadataTools.getColumnNameIterator(value.getColumnIterator()), false, insertable);
        parent.add(properties);

        // Adding mapper for the id
        PropertyData propertyData = propertyAuditingData.getPropertyData();
        mapper.addComposite(propertyData, new ToOneIdMapper(relMapper, propertyData, referencedEntityName, nonInsertableFake));
    }

    @SuppressWarnings({"unchecked"})
    void addOneToOneNotOwning(PropertyAuditingData propertyAuditingData, Value value,
                              CompositeMapperBuilder mapper, String entityName) {
        OneToOne propertyValue = (OneToOne) value;

        String owningReferencePropertyName = propertyValue.getReferencedPropertyName(); // mappedBy

        EntityConfiguration configuration = mainGenerator.getEntitiesConfigurations().get(entityName);
        if (configuration == null) {
            throw new MappingException("An audited relation to a non-audited entity " + entityName + "!");
        }

        IdMappingData ownedIdMapping = configuration.getIdMappingData();

        if (ownedIdMapping == null) {
            throw new MappingException("An audited relation to a non-audited entity " + entityName + "!");
        }

        String lastPropertyPrefix = MappingTools.createToOneRelationPrefix(owningReferencePropertyName);
        String referencedEntityName = propertyValue.getReferencedEntityName();

        // Generating the id mapper for the relation
        IdMapper ownedIdMapper = ownedIdMapping.getIdMapper().prefixMappedProperties(lastPropertyPrefix);

        // Storing information about this relation
        mainGenerator.getEntitiesConfigurations().get(entityName).addToOneNotOwningRelation(
                propertyAuditingData.getName(), owningReferencePropertyName,
                referencedEntityName, ownedIdMapper);

        // Adding mapper for the id
        PropertyData propertyData = propertyAuditingData.getPropertyData();
        mapper.addComposite(propertyData, new OneToOneNotOwningMapper(owningReferencePropertyName,
                referencedEntityName, propertyData));
    }
}
