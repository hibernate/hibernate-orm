package org.hibernate.envers.configuration.metadata;

import org.dom4j.Element;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.ModificationStore;

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
	public void addComponent(Element parent, PersistentPropertyAuditingData persistentPropertyAuditingData,
							 Value value, CompositeMapperBuilder mapper, String entityName,
							 EntityXmlMappingData xmlMappingData, boolean firstPass) {
		Component prop_component = (Component) value;

		CompositeMapperBuilder componentMapper = mapper.addComponent(persistentPropertyAuditingData.getPropertyData());

		// Adding all properties of the component
		Iterator<Property> properties = (Iterator<Property>) prop_component.getPropertyIterator();
		while (properties.hasNext()) {
			Property property = properties.next();
			// The name of the property in the entity will consist of the name of the component property concatenated
			// with the name of the property in the bean, to avoid conflicts.
			PersistentPropertyAuditingData propertyAuditingData = new PersistentComponentPropertyAuditingData(
					persistentPropertyAuditingData.getName() + "_" + property.getName(),
					property.getName(), property.getPropertyAccessorName(), ModificationStore.FULL);

			mainGenerator.addValue(parent, property.getValue(), componentMapper,
					entityName, xmlMappingData, propertyAuditingData, property.isInsertable(), firstPass);
		}
	}
}
