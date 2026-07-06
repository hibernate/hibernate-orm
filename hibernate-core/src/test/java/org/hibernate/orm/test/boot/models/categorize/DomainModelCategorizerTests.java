/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorize;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
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

			final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( List.of( root, included ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext );

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
			final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( List.of( embeddable ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext );

			assertThat( result.getEntityHierarchies() ).isEmpty();
			assertThat( result.getEmbeddables() ).containsEntry( EmbeddableType.class.getName(), embeddable );
		}
	}

	@Test
	void processorDiscoversReachableEmbeddablesByDefault() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetailsRegistry classDetailsRegistry = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry();

			final ClassDetails entity = classDetailsRegistry.resolveClassDetails( ListedWithUnlistedEmbeddable.class.getName() );
			final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( List.of( entity ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext );

			assertThat( result.getEmbeddables() )
					.containsKey( UnlistedEmbeddable.class.getName() )
					.containsKey( NestedUnlistedEmbeddable.class.getName() )
					.containsKey( UnlistedCollectionEmbeddable.class.getName() );
		}
	}

	@Test
	void processorRejectsReachableEmbeddableWhenUnlistedTypesAreExcluded() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetailsRegistry classDetailsRegistry = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry();

			final ClassDetails entity = classDetailsRegistry.resolveClassDetails( ListedWithUnlistedEmbeddable.class.getName() );
			final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( List.of( entity ), emptyList(), emptyList(), false );

			assertThatThrownBy( () -> DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext ) )
					.isInstanceOf( MappingException.class )
					.hasMessageContaining( UnlistedEmbeddable.class.getName() )
					.hasMessageContaining( ListedWithUnlistedEmbeddable.class.getName() );
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
			final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( List.of( entity ), emptyList(), emptyList() );

			final var result = DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext );

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
			final PreparedMappingSources resolvedMappingSources = new PreparedMappingSources( List.of( entity ), emptyList(), emptyList(), false );

			assertThatThrownBy( () -> DomainModelCategorizer.categorize( resolvedMappingSources, metadataBuildingContext ) )
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

	@Entity
	public static class ListedWithUnlistedEmbeddable {
		@Id
		private Long id;
		private UnlistedEmbeddable embeddable;
		private List<UnlistedCollectionEmbeddable> collectionEmbeddables;
	}

	@Embeddable
	public static class EmbeddableType {
		private String name;
	}

	@Embeddable
	public static class UnlistedEmbeddable {
		private String name;
		private NestedUnlistedEmbeddable nested;
	}

	@Embeddable
	public static class NestedUnlistedEmbeddable {
		private String name;
	}

	@Embeddable
	public static class UnlistedCollectionEmbeddable {
		private String name;
	}
}
