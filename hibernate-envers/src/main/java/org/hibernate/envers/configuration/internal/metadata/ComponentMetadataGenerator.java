/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.Iterator;
import java.util.Map;

import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

import org.dom4j.Element;

/**
 * Generates metadata for components.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public final class ComponentMetadataGenerator {
    private final AuditMetadataGenerator mainGenerator;

    ComponentMetadataGenerator(AuditMetadataGenerator auditMetadataGenerator) {
        mainGenerator = auditMetadataGenerator;
    }

    @SuppressWarnings({"unchecked"})
    public void addComponent(
            Element parent, PropertyAuditingData propertyAuditingData,
            Value value, CompositeMapperBuilder mapper, String entityName,
            EntityXmlMappingData xmlMappingData, boolean firstPass) {
        final Component propComponent = (Component) value;

        final Class componentClass;
        if (propComponent.isDynamic()) {
            componentClass = ReflectionTools.loadClass(
                    Map.class.getCanonicalName(),
                    mainGenerator.getClassLoaderService());

        } else {
            componentClass = ReflectionTools.loadClass(
                    propComponent.getComponentClassName(),
                    mainGenerator.getClassLoaderService()
            );
        }
        final CompositeMapperBuilder componentMapper = mapper.addComponent(
                propertyAuditingData.getPropertyData(),
                componentClass
        );

        // The property auditing data must be for a component.
        final ComponentAuditingData componentAuditingData = (ComponentAuditingData) propertyAuditingData;

        // Adding all properties of the component
        final Iterator<Property> properties = (Iterator<Property>) propComponent.getPropertyIterator();
        while (properties.hasNext()) {
            final Property property = properties.next();

            final PropertyAuditingData componentPropertyAuditingData =
                    componentAuditingData.getPropertyAuditingData(property.getName());

            // Checking if that property is audited
            if (componentPropertyAuditingData != null) {
                mainGenerator.addValue(
                        parent, property.getValue(), componentMapper, entityName, xmlMappingData,
                        componentPropertyAuditingData, property.isInsertable(), firstPass, false
                );
            }
        }
    }
}
