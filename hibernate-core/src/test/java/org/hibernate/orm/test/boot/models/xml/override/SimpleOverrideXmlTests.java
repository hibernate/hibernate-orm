/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.override;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
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

			final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder( serviceRegistry )
					.addXmlMappings( "mappings/models/override/simple-override.xml" )
					.build();
			final ClassDetailsRegistry classDetailsRegistry =
					SourceModelTestHelper.createBuildingContext( managedResources, serviceRegistry )
							.getClassDetailsRegistry();

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
