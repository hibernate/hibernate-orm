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

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;

import org.dom4j.Element;

/**
 * Generates metadata for components.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public final class ComponentMetadataGenerator {
	private final AuditConfiguration.AuditConfigurationContext context;

	private final AuditMetadataGenerator mainGenerator;

    ComponentMetadataGenerator(AuditConfiguration.AuditConfigurationContext context, AuditMetadataGenerator auditMetadataGenerator) {
		this.context = context;
        mainGenerator = auditMetadataGenerator;
    }

    @SuppressWarnings({"unchecked"})
    public void addComponent(
            Element parent, PropertyAuditingData propertyAuditingData,
            EmbeddedAttributeBinding embeddedAttributeBinding, CompositeMapperBuilder mapper, String entityName,
            EntityXmlMappingData xmlMappingData, boolean firstPass) {

        final Class componentClass;
		final EntityMode entityMode = embeddedAttributeBinding.getEmbeddableBinding().seekEntityBinding().getHierarchyDetails().getEntityMode();
        if ( entityMode == EntityMode.MAP ) {
            componentClass = context.getClassLoaderService().classForName( Map.class.getCanonicalName() );
        } else {
			// TODO: get rid of classloading.
            componentClass = context.getClassLoaderService().classForName(
					embeddedAttributeBinding.getHibernateTypeDescriptor().getJavaTypeDescriptor().getName().toString()
            );
        }
        final CompositeMapperBuilder componentMapper = mapper.addComponent(
                propertyAuditingData.getPropertyData(),
                componentClass
        );

        // The property auditing data must be for a component.
        final ComponentAuditingData componentAuditingData = (ComponentAuditingData) propertyAuditingData;

        // Adding all properties of the component
        for ( AttributeBinding attributeBinding : embeddedAttributeBinding.getEmbeddableBinding().attributeBindings() ) {

            final PropertyAuditingData componentPropertyAuditingData =
                    componentAuditingData.getPropertyAuditingData( attributeBinding.getAttribute().getName() );

            // Checking if that property is audited
            if (componentPropertyAuditingData != null) {
                mainGenerator.addValue(
                        parent, attributeBinding, componentMapper, entityName, xmlMappingData,
                        componentPropertyAuditingData, firstPass, false
                );
            }
        }
    }
}
