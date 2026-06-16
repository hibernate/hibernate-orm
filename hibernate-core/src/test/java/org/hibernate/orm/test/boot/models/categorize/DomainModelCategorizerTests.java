/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorize;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.pipeline.internal.source.AvailableResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
public class DomainModelCategorizerTests {
	@Test
	void processorBuildsHierarchiesFromAvailableClassDetails() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetailsRegistry classDetailsRegistry = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry();

			final ClassDetails root = classDetailsRegistry.resolveClassDetails( Root.class.getName() );
			final ClassDetails included = classDetailsRegistry.resolveClassDetails( IncludedLeaf.class.getName() );
			classDetailsRegistry.resolveClassDetails( ExcludedLeaf.class.getName() );

			final AvailableResources availableResources = new AvailableResources( List.of( root, included ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( availableResources, metadataBuildingContext );

			assertThat( result.getEntityHierarchies() ).hasSize( 1 );

			final var hierarchy = result.getEntityHierarchies().iterator().next();
			assertThat( hierarchy.getRoot().getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
			assertThat( hierarchy.getRoot().getSubTypes() )
					.extracting( IdentifiableTypeMetadata::getClassDetails )
					.extracting( ClassDetails::getClassName )
					.containsExactly( IncludedLeaf.class.getName() );
		}
	}

	@Test
	void processorCollectsEmbeddablesWithoutCreatingHierarchies() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetailsRegistry classDetailsRegistry = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry();

			final ClassDetails embeddable = classDetailsRegistry.resolveClassDetails( EmbeddableType.class.getName() );
			final AvailableResources availableResources = new AvailableResources( List.of( embeddable ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( availableResources, metadataBuildingContext );

			assertThat( result.getEntityHierarchies() ).isEmpty();
			assertThat( result.getEmbeddables() ).containsEntry( EmbeddableType.class.getName(), embeddable );
		}
	}

	@Test
	void processorCompletesMissingPersistentSuperclassByDefault() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetailsRegistry classDetailsRegistry = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry();

			final ClassDetails entity = classDetailsRegistry.resolveClassDetails( ListedWithUnlistedMappedSuperclass.class.getName() );
			final AvailableResources availableResources = new AvailableResources( List.of( entity ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( availableResources, metadataBuildingContext );

			final var hierarchy = result.getEntityHierarchies().iterator().next();
			assertThat( hierarchy.getRoot().getClassDetails().getClassName() )
					.isEqualTo( ListedWithUnlistedMappedSuperclass.class.getName() );
			assertThat( hierarchy.getRoot().getSuperType().getClassDetails().getClassName() )
					.isEqualTo( UnlistedMappedSuperclass.class.getName() );
		}
	}

	@Test
	void processorRejectsMissingPersistentSuperclassWhenUnlistedTypesAreExcluded() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetailsRegistry classDetailsRegistry = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry();

			final ClassDetails entity = classDetailsRegistry.resolveClassDetails( ListedWithUnlistedMappedSuperclass.class.getName() );
			final AvailableResources availableResources = new AvailableResources( List.of( entity ), emptyList(), emptyList(), false );

			assertThatThrownBy( () -> DomainModelCategorizer.categorize( availableResources, metadataBuildingContext ) )
					.isInstanceOf( MappingException.class )
					.hasMessageContaining( UnlistedMappedSuperclass.class.getName() )
					.hasMessageContaining( ListedWithUnlistedMappedSuperclass.class.getName() );
		}
	}

	@Entity
	public static class Root {
		@Id
		private Long id;
	}

	@Entity
	public static class IncludedLeaf extends Root {
		private String included;
	}

	@Entity
	public static class ExcludedLeaf extends Root {
		private String excluded;
	}

	@MappedSuperclass
	public static class UnlistedMappedSuperclass {
		@Id
		private Long id;
	}

	@Entity
	public static class ListedWithUnlistedMappedSuperclass extends UnlistedMappedSuperclass {
		private String name;
	}

	@Embeddable
	public static class EmbeddableType {
		private String name;
	}
}
