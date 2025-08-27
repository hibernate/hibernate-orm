/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.override;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.source.internal.annotations.DomainModelSource;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.orm.test.boot.models.xml.SimpleEntity;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleOverrideXmlTests {
	@Test
	@ServiceRegistry
	void testSimpleCompleteEntity(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();

		final MetadataSources metadataSources = new MetadataSources().addResource( "mappings/models/override/simple-override.xml" );
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, options );
		options.setBootstrapContext( bootstrapContext );

		final ManagedResources managedResources = MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, options );
		final DomainModelSource domainModelSource = MetadataBuildingProcess.processManagedResources(
				managedResources,
				metadataCollector,
				bootstrapContext,
				new MetadataBuilderImpl.MappingDefaultsImpl( serviceRegistry )
		);

		final ClassDetailsRegistry classDetailsRegistry = domainModelSource.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getDirectAnnotationUsage( Id.class ) ).isNotNull();

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField.getDirectAnnotationUsage( Basic.class ) ).isNotNull();
		final Column nameColumnAnn = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( nameColumnAnn ).isNotNull();
		assertThat( nameColumnAnn.name() ).isEqualTo( "description" );
		assertThat( nameColumnAnn.columnDefinition() ).isEqualTo( "nvarchar(512)" );
	}
}
