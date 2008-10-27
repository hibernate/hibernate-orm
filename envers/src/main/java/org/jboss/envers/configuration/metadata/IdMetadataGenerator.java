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
import org.dom4j.tree.DefaultElement;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Component;
import org.hibernate.type.Type;
import org.hibernate.type.ImmutableType;
import org.hibernate.MappingException;
import org.jboss.envers.entities.mapper.SimpleMapperBuilder;
import org.jboss.envers.entities.mapper.id.SimpleIdMapperBuilder;
import org.jboss.envers.entities.mapper.id.MultipleIdMapper;
import org.jboss.envers.entities.mapper.id.EmbeddedIdMapper;
import org.jboss.envers.entities.mapper.id.SingleIdMapper;
import org.jboss.envers.entities.IdMappingData;
import org.jboss.envers.ModificationStore;

import java.util.Iterator;

/**
 * Generates metadata for primary identifiers (ids) of versions entities.
 * @author Adam Warski (adam at warski dot org)
 */
public final class IdMetadataGenerator {
    private final VersionsMetadataGenerator mainGenerator;

    IdMetadataGenerator(VersionsMetadataGenerator versionsMetadataGenerator) {
        mainGenerator = versionsMetadataGenerator;
    }

    @SuppressWarnings({"unchecked"})
    private void addIdProperties(Element parent, Iterator<Property> properties, SimpleMapperBuilder mapper, boolean key) {
        while (properties.hasNext()) {
            Property property = properties.next();
            Type propertyType = property.getType();
            if (!"_identifierMapper".equals(property.getName())) {
                if (propertyType instanceof ImmutableType) {
                    // Last but one parameter: ids are always insertable
                    mainGenerator.getBasicMetadataGenerator().addBasicNoComponent(parent, property.getName(),
                            property.getValue(), mapper, ModificationStore.FULL, true, key);
                } else {
                    throw new MappingException("Type not supported: " + propertyType.getClass().getName());
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    IdMappingData addId(PersistentClass pc) {
        // Xml mapping which will be used for relations
        Element rel_id_mapping = new DefaultElement("properties");
        // Xml mapping which will be used for the primary key of the versions table
        Element orig_id_mapping = new DefaultElement("composite-id");

        Property id_prop = pc.getIdentifierProperty();
        Component id_mapper = pc.getIdentifierMapper();

        SimpleIdMapperBuilder mapper;
        if (id_mapper != null) {
            // Multiple id

            mapper = new MultipleIdMapper(((Component) pc.getIdentifier()).getComponentClassName());
            addIdProperties(rel_id_mapping, (Iterator<Property>) id_mapper.getPropertyIterator(), mapper, false);

            // null mapper - the mapping where already added the first time, now we only want to generate the xml
            addIdProperties(orig_id_mapping, (Iterator<Property>) id_mapper.getPropertyIterator(), null, true);
        } else if (id_prop.isComposite()) {
            // Embedded id

            Component id_component = (Component) id_prop.getValue();

            mapper = new EmbeddedIdMapper(id_prop.getName(), id_component.getComponentClassName());
            addIdProperties(rel_id_mapping, (Iterator<Property>) id_component.getPropertyIterator(), mapper, false);

            // null mapper - the mapping where already added the first time, now we only want to generate the xml
            addIdProperties(orig_id_mapping, (Iterator<Property>) id_component.getPropertyIterator(), null, true);
        } else {
            // Single id
            
            mapper = new SingleIdMapper();

            // Last but one parameter: ids are always insertable
            mainGenerator.getBasicMetadataGenerator().addBasicNoComponent(rel_id_mapping, id_prop.getName(),
                    id_prop.getValue(), mapper, ModificationStore.FULL, true, false);

            // null mapper - the mapping where already added the first time, now we only want to generate the xml
            mainGenerator.getBasicMetadataGenerator().addBasicNoComponent(orig_id_mapping, id_prop.getName(),
                    id_prop.getValue(), null, ModificationStore.FULL, true, true);
        }

        orig_id_mapping.addAttribute("name", mainGenerator.getVerEntCfg().getOriginalIdPropName());

        // Adding a relation to the revision entity (effectively: the "revision number" property)
        mainGenerator.addRevisionInfoRelation(orig_id_mapping);

        return new IdMappingData(mapper, orig_id_mapping, rel_id_mapping);
    }
}
