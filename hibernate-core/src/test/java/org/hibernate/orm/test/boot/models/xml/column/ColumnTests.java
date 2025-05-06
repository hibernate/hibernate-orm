/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.column;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
public class ColumnTests {
	@Test
	void testCompleteColumn(ServiceRegistryScope scope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/column/complete.xml" )
				.build();
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions =
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				metadataBuildingOptions
		);
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		final ModelsContext ModelsContext = createBuildingContext(
				managedResources,
				false,
				metadataBuildingOptions,
				bootstrapContext
		);

		final ClassDetails anEntityDetails = ModelsContext.getClassDetailsRegistry().getClassDetails( AnEntity.class.getName() );

		final FieldDetails nameField = anEntityDetails.findFieldByName( "name" );
		assertThat( nameField ).isNotNull();
		final Column annotationUsage = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( annotationUsage.name() ).isEqualTo( "nombre" );
		assertThat( annotationUsage.length() ).isEqualTo( 256 );
		assertThat( annotationUsage.comment() ).isEqualTo( "The name column" );
		assertThat( annotationUsage.table() ).isEqualTo( "tbl" );
		assertThat( annotationUsage.options() ).isEqualTo( "the options" );
		assertThat( annotationUsage.check() ).isNotEmpty();
	}

	@Test
	void testOverrideColumn(ServiceRegistryScope scope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/column/override.xml" )
				.build();

		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions =
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				metadataBuildingOptions
		);
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		final ModelsContext ModelsContext = createBuildingContext(
				managedResources,
				false,
				metadataBuildingOptions,
				bootstrapContext
		);

		final ClassDetails anEntityDetails = ModelsContext.getClassDetailsRegistry().getClassDetails( AnEntity.class.getName() );

		final FieldDetails nameField = anEntityDetails.findFieldByName( "name" );
		assertThat( nameField ).isNotNull();
		final Column columnAnn = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( columnAnn ).isNotNull();
		assertThat( columnAnn.name() ).isEqualTo( "nombre" );
	}
}
