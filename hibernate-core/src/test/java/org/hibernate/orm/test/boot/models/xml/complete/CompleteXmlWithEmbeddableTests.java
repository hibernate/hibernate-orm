/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class CompleteXmlWithEmbeddableTests {
	@Test
	@ServiceRegistry
	void testModel(ServiceRegistryScope registryScope) {
		final AdditionalManagedResourcesImpl.Builder managedResourcesBuilder = new AdditionalManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/models/complete/simple-person.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext(
				managedResources,
				registryScope.getRegistry()
		);

		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		final ClassDetails personClassDetails = classDetailsRegistry.getClassDetails( SimplePerson.class.getName() );

		final FieldDetails idAttribute = personClassDetails.findFieldByName( "id" );
		assertThat( idAttribute.getDirectAnnotationUsage( Basic.class ) ).isNotNull();
		assertThat( idAttribute.getDirectAnnotationUsage( Id.class ) ).isNotNull();

		final FieldDetails nameAttribute = personClassDetails.findFieldByName( "name" );
		assertThat( nameAttribute.getDirectAnnotationUsage( Embedded.class ) ).isNotNull();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
		}
	}
}
