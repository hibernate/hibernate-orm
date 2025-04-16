/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;


import jakarta.persistence.Transient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * Test metadata-complete mapping which maps only some "attributes"
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class CompletePartialTests {
	@Test
	@ServiceRegistry
	void testSourceModel(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/complete/partial-complete.xml" )
				.build();

		final ModelsContext ModelsContext = createBuildingContext(
				managedResources,
				registryScope.getRegistry()
		);
		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();
		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( Thing.class.getName() );

		// NOTE : `#createBuildingContext` applies `XmlProcessor`

		assertThat( classDetails.getFields() ).hasSize( 3 );
		classDetails.forEachField( (i, fieldDetails) -> {
			assertThat( fieldDetails.isPersistable() ).isTrue();
			assertThat( fieldDetails.hasDirectAnnotationUsage( Transient.class ) ).isFalse();

		} );
	}

	@Test
	@ServiceRegistry
	@DomainModel( xmlMappings = "mappings/models/complete/partial-complete.xml" )
	public void testBootModel(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( Thing.class );
		assertThat( entityBinding.getIdentifierProperty().getName() ).isEqualTo( "id" );
		assertThat( entityBinding.getProperties().stream().map( Property::getName ) ).contains( "name" ).contains( "somethingElse" );
	}
}
