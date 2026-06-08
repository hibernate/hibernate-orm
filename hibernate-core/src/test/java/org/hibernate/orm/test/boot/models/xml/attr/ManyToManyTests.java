/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Set;

import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@ServiceRegistry
public class ManyToManyTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleManyToMany(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/many-to-many/simple.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails othersField = classDetails.findFieldByName( "others" );
		final ManyToMany manyToManyAnn = othersField.getDirectAnnotationUsage( ManyToMany.class );
		assertThat( manyToManyAnn ).isNotNull();

		final OptimisticLock optLockAnn = othersField.getDirectAnnotationUsage( OptimisticLock.class );
		assertThat( optLockAnn ).isNotNull();
		assertThat( optLockAnn.excluded() ).isFalse();
	}

	@SuppressWarnings("unused")
	@Entity(name = "SYYimpleEntity")
	@Table(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;
		private Set<SimpleEntity> others;
	}
}
