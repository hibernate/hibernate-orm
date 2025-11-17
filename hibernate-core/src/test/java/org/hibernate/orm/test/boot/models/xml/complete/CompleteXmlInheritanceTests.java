/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;


import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.buildJandexIndex;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class CompleteXmlInheritanceTests {
	@Test
	@ServiceRegistry
	void testModel(ServiceRegistryScope registryScope) {

		final AdditionalManagedResourcesImpl.Builder managedResourcesBuilder = new AdditionalManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/complete/simple-inherited.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final ModelsContext ModelsContext = createBuildingContext(
				managedResources,
				buildJandexIndex( SIMPLE_CLASS_LOADING, Root.class, Sub.class ),
				registryScope.getRegistry()
		);

		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();
		final ClassDetails root = classDetailsRegistry.getClassDetails( Root.class.getName() );

		final Inheritance inheritance = root.getDirectAnnotationUsage( Inheritance.class );
		assertThat( inheritance.strategy() ).isEqualTo( JOINED );

		assertThat( root.getClassName() ).isEqualTo( Root.class.getName() );
		final FieldDetails idAttr = root.findFieldByName( "id" );
		assertThat( idAttr.getDirectAnnotationUsage( Id.class ) ).isNotNull();
	}
}
