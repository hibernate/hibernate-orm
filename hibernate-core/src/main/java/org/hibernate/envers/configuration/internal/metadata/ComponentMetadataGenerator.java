/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.mapping.Component;

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
			ValueMapping value, CompositeMapperBuilder mapper, String entityName,
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
					propComponent.getEmbeddableClassName(),
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
		List<PersistentAttributeMapping> declaredPersistentAttributes = propComponent.getDeclaredPersistentAttributes();
		declaredPersistentAttributes.forEach( attribute -> {
			final PropertyAuditingData componentPropertyAuditingData =
					componentAuditingData.getPropertyAuditingData( attribute.getName() );

			// Checking if that property is audited
			if ( componentPropertyAuditingData != null ) {
				mainGenerator.addValue(
						parent, attribute.getValueMapping(), componentMapper, entityName, xmlMappingData,
						componentPropertyAuditingData, attribute.isInsertable(), firstPass, false
				);
			}
		} );
	}
}
