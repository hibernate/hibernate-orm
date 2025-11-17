/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Map;

import org.hibernate.envers.boot.model.AttributeContainer;
import org.hibernate.envers.boot.registry.classloading.ClassLoaderAccessHelper;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.internal.EmbeddableCompositeUserTypeInstantiator;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorPojoIndirecting;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordIndirecting;
import org.hibernate.metamodel.internal.EmbeddableInstantiatorRecordStandard;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.usertype.CompositeUserType;

/**
 * Generates metadata for components.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 * @author Chris Cranford
 */
public final class ComponentMetadataGenerator extends AbstractMetadataGenerator {

	private final ValueMetadataGenerator valueGenerator;

	ComponentMetadataGenerator(EnversMetadataBuildingContext metadataBuildingContext, ValueMetadataGenerator valueGenerator) {
		super( metadataBuildingContext );
		this.valueGenerator = valueGenerator;
	}

	@SuppressWarnings("unchecked")
	public void addComponent(
			AttributeContainer attributeContainer,
			PropertyAuditingData propertyAuditingData,
			Value value,
			CompositeMapperBuilder mapper,
			String entityName,
			EntityMappingData mappingData,
			boolean firstPass) {
		final Component propComponent = (Component) value;
		final EmbeddableInstantiator instantiator;
		if ( propComponent.getCustomInstantiator() != null ) {
			if ( !getMetadataBuildingContext().getBuildingOptions().isAllowExtensionsInCdi() ) {
				instantiator = FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( propComponent.getCustomInstantiator() );
			}
			else {
				instantiator =
						getMetadataBuildingContext().getBootstrapContext().getManagedBeanRegistry()
								.getBean( propComponent.getCustomInstantiator() )
								.getBeanInstance();
			}
		}
		else if ( propComponent.getTypeName() != null ) {
			final Class<CompositeUserType<?>> userTypeClass = getMetadataBuildingContext().getBootstrapContext()
					.getClassLoaderAccess()
					.classForName( propComponent.getTypeName() );
			if ( !getMetadataBuildingContext().getBuildingOptions().isAllowExtensionsInCdi() ) {
				final CompositeUserType<?> compositeUserType = FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( userTypeClass );
				//noinspection rawtypes
				instantiator = new EmbeddableCompositeUserTypeInstantiator( (CompositeUserType) compositeUserType );
			}
			else {
				final CompositeUserType<Object> compositeUserType = (CompositeUserType<Object>)
						getMetadataBuildingContext().getBootstrapContext().getManagedBeanRegistry()
								.getBean( userTypeClass )
								.getBeanInstance();
				instantiator = new EmbeddableCompositeUserTypeInstantiator( compositeUserType );
			}
		}
		else if ( propComponent.getInstantiator() != null ) {
			instantiator = EmbeddableInstantiatorPojoIndirecting.of(
					propComponent.getPropertyNames(),
					propComponent.getInstantiator(),
					propComponent.getInstantiatorPropertyNames()
			);
		}
		else if ( propComponent.getComponentClass() != null &&
					propComponent.getComponentClass().isRecord() ) {
			if ( propComponent.sortProperties() == null ) {
				instantiator = new EmbeddableInstantiatorRecordStandard( propComponent.getComponentClass() );
			}
			else {
				instantiator = EmbeddableInstantiatorRecordIndirecting.of(
						propComponent.getComponentClass(),
						propComponent.getPropertyNames()
				);
			}
		}
		else {
			instantiator = null;
		}
		final CompositeMapperBuilder componentMapper = mapper.addComponent(
				propertyAuditingData.resolvePropertyData(),
				ClassLoaderAccessHelper.loadClass(
						getMetadataBuildingContext(),
						getClassNameForComponent( propComponent )
				),
				instantiator
		);

		// The property auditing data must be for a component.
		final ComponentAuditingData componentAuditingData = (ComponentAuditingData) propertyAuditingData;

		// Adding all properties of the component
		propComponent.sortProperties();
		for ( Property property : propComponent.getProperties() ) {
			final PropertyAuditingData componentPropertyAuditingData =
					componentAuditingData.getPropertyAuditingData( property.getName() );

			// Checking if that property is audited
			if ( componentPropertyAuditingData != null ) {
				valueGenerator.addValue(
						attributeContainer,
						property.getValue(),
						property.getPropertyAccessStrategy(),
						componentMapper,
						entityName,
						mappingData,
						componentPropertyAuditingData,
						property.isInsertable(),
						firstPass,
						false
				);
			}
		}

		if ( !firstPass ) {
			final EntityConfiguration owningEntityConfiguration = getAuditedEntityConfigurations().get( entityName );
			owningEntityConfiguration.addToOneComponent( propertyAuditingData.getName(), componentAuditingData );
		}
	}

	private String getClassNameForComponent(Component component) {
		return component.isDynamic() ? Map.class.getCanonicalName() : component.getComponentClassName();
	}
}
