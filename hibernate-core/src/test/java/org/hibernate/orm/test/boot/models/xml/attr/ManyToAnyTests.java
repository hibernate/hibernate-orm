/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.List;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ManyToAny;
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
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Awantika Shinde
 */
@ServiceRegistry
public class ManyToAnyTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleManyToAnyAttribute(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addLoadedClasses( Entity1.class )
				.addLoadedClasses( Entity2.class )
				.addXmlMappings( "mappings/models/attr/many-to-any/simple.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails entity3ClassDetails = classDetailsRegistry.resolveClassDetails( Entity3.class.getName() );

		final FieldDetails associationsField = entity3ClassDetails.findFieldByName( "associations" );
		assertThat( associationsField ).isNotNull();

		final ManyToAny manyToAnyAnn = associationsField.getDirectAnnotationUsage( ManyToAny.class );
		assertThat( manyToAnyAnn ).isNotNull();

		final Cascade cascadeAnn = associationsField.getDirectAnnotationUsage( Cascade.class );
		assertThat( cascadeAnn ).isNotNull();
		assertThat( cascadeAnn.value() ).containsExactly( CascadeType.ALL );
	}

	@Entity(name = "Entity1")
	@Table(name = "Entity1")
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "Entity2")
	@Table(name = "Entity2")
	public static class Entity2 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name = "Entity3")
	@Table(name = "Entity3")
	public static class Entity3 {
		@Id
		private Integer id;
		private String name;
		private List<Object> associations;
	}
}
