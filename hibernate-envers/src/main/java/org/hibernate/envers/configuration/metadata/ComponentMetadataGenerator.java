package org.hibernate.envers.configuration.metadata;

import org.dom4j.Element;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.configuration.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;

import java.util.Iterator;

/**
 * Generates metadata for components.
 * @author Adam Warski (adam at warski dot org)
 */
public final class ComponentMetadataGenerator {
	private final AuditMetadataGenerator mainGenerator;

	ComponentMetadataGenerator(AuditMetadataGenerator auditMetadataGenerator) {
		mainGenerator = auditMetadataGenerator;
	}

	@SuppressWarnings({"unchecked"})
	public void addComponent(Element parent, PropertyAuditingData propertyAuditingData,
							 Value value, CompositeMapperBuilder mapper, String entityName,
							 EntityXmlMappingData xmlMappingData, boolean firstPass) {
		Component prop_component = (Component) value;

		CompositeMapperBuilder componentMapper = mapper.addComponent(propertyAuditingData.getPropertyData(),
				prop_component.getComponentClassName());

		// The property auditing data must be for a component.
		ComponentAuditingData componentAuditingData = (ComponentAuditingData) propertyAuditingData;

		// Adding all properties of the component
		Iterator<Property> properties = (Iterator<Property>) prop_component.getPropertyIterator();
		while (properties.hasNext()) {
			Property property = properties.next();

			PropertyAuditingData componentPropertyAuditingData =
					componentAuditingData.getPropertyAuditingData(property.getName());

			// Checking if that property is audited
			if (componentPropertyAuditingData != null) {
				mainGenerator.addValue(parent, property.getValue(), componentMapper, entityName, xmlMappingData,
						componentPropertyAuditingData, property.isInsertable(), firstPass);
			}
		}
	}
}
