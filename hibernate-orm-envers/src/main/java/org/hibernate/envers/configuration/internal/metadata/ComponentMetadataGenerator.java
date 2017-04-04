/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		if ( propComponent.isDynamic() ) {
			componentClass = ReflectionTools.loadClass(
					Map.class.getCanonicalName(),
					mainGenerator.getClassLoaderService()
			);

		}
		else {
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
		while ( properties.hasNext() ) {
			final Property property = properties.next();

			final PropertyAuditingData componentPropertyAuditingData =
					componentAuditingData.getPropertyAuditingData( property.getName() );

			// Checking if that property is audited
			if ( componentPropertyAuditingData != null ) {
				mainGenerator.addValue(
						parent, property.getValue(), componentMapper, entityName, xmlMappingData,
						componentPropertyAuditingData, property.isInsertable(), firstPass, false
				);
			}
		}
	}
}
