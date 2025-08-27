/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.override;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.source.internal.annotations.DomainModelSource;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.orm.test.jpa.xml.Employee;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.model.process.spi.MetadataBuildingProcess.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class AttributeOverrideXmlTests {
	@Test
	@ServiceRegistry
	void testBasicHandling(ServiceRegistryScope serviceRegistryScope) {
		final StandardServiceRegistry registry = serviceRegistryScope.getRegistry();

		final MetadataSources metadataSources = new MetadataSources().addResource( "org/hibernate/orm/test/jpa/xml/orm3.xml" );
		final MetadataBuildingOptionsImpl options = new MetadataBuildingOptionsImpl( registry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( registry, options );
		options.setBootstrapContext( bootstrapContext );

		final ManagedResources managedResources = MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, options );

		final DomainModelSource domainModelSource = processManagedResources(
				managedResources,
				metadataCollector,
				bootstrapContext,
				new MetadataBuilderImpl.MappingDefaultsImpl( registry )
		);

		final ClassDetailsRegistry classDetailsRegistry = domainModelSource.getClassDetailsRegistry();
		final ClassDetails employeeClassDetails = classDetailsRegistry.getClassDetails( Employee.class.getName() );
		assertThat( employeeClassDetails.getFields() ).hasSize( 4 );

		final FieldDetails homeAddressField = employeeClassDetails.findFieldByName( "homeAddress" );
		checkOverrides( homeAddressField, "home_" );

		final FieldDetails mailAddressField = employeeClassDetails.findFieldByName( "mailAddress" );
		checkOverrides( mailAddressField, "mail_" );


		final ClassDetails embeddable = homeAddressField.getType().determineRawClass();
		assertThat( embeddable ).isNotNull();
		assertThat( embeddable.getFields() ).hasSize( 4 );

		final FieldDetails cityField = embeddable.findFieldByName( "city" );
		assertThat( cityField.hasDirectAnnotationUsage( Column.class ) ).isFalse();
	}

	private static void checkOverrides(FieldDetails embedded, String prefix) {
		final AttributeOverrides overridesUsage = embedded.getDirectAnnotationUsage( AttributeOverrides.class );
		assertThat( overridesUsage ).isNotNull();

		final AttributeOverride[] overrideList = overridesUsage.value();
		assertThat( overrideList ).hasSize( 4 );

		for ( AttributeOverride overrideUsage : overrideList ) {
			final String attributeName = overrideUsage.name();

			final Column columnUsage = overrideUsage.column();
			final String columnName = columnUsage.name();

			switch ( attributeName ) {
				case "street" -> assertThat( columnName ).isEqualTo( prefix + "street" );
				case "city" -> assertThat( columnName ).isEqualTo( prefix + "city" );
				case "state" -> assertThat( columnName ).isEqualTo( prefix + "state" );
				case "zip" -> assertThat( columnName ).isEqualTo( prefix + "zip" );
			}
		}

	}
}
