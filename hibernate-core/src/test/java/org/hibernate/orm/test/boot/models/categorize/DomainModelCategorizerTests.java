/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.categorize;

import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizer;
import org.hibernate.boot.mapping.internal.categorize.EmbeddedAttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.EmbeddedValueMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.PluralAttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.SingularAttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.ValueNature;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
			assertThat( result.getEmbeddables() )
					.extractingByKey( EmbeddableType.class.getName() )
					.extracting( org.hibernate.boot.mapping.internal.categorize.EmbeddableTypeMetadata::getClassDetails )
					.isSameAs( embeddable );
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

	@Test
	void processorCategorizesEmbeddableUsagesAndPluralParts() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext =
					new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetails entity = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( CategorizedStructureEntity.class.getName() );
			final var result = DomainModelCategorizer.categorize(
					new PreparedMappingSources( List.of( entity ), emptyList(), emptyList() ),
					metadataBuildingContext
			);

			final var root = result.getEntityHierarchies().iterator().next().getRoot();
			assertThat( ( (SingularAttributeMetadata) root.findAttribute( "id" ) ).getValue().getNature() )
					.isEqualTo( ValueNature.BASIC );
			final var embedded = (EmbeddedAttributeMetadata) root.findAttribute( "details" );
			assertThat( embedded.getValue().getNature() ).isEqualTo( ValueNature.EMBEDDED );
			assertThat( embedded.getEmbeddableUsage().findAttribute( "inheritedName" ) ).isNotNull();
			assertThat( embedded.getEmbeddableUsage().findAttribute( "name" ) ).isNotNull();

			final var idBag = (PluralAttributeMetadata) root.findAttribute( "detailBag" );
			assertThat( idBag.getCollectionClassification() ).isEqualTo( CollectionClassification.ID_BAG );
			assertThat( ( (EmbeddedValueMetadata) idBag.getElement() )
					.getEmbeddableUsage().findAttribute( "name" ) ).isNotNull();
			assertThat( idBag.getIndex() ).isNull();
			assertThat( idBag.getCollectionId().generatorRegistration().getGeneratorClass() )
					.isEqualTo( IncrementGenerator.class );

			final var map = (PluralAttributeMetadata) root.findAttribute( "detailsByName" );
			assertThat( map.getCollectionClassification() ).isEqualTo( CollectionClassification.MAP );
			assertThat( map.getElement() ).isInstanceOf( EmbeddedValueMetadata.class );
			assertThat( map.getIndex().getNature() )
					.isEqualTo( ValueNature.BASIC );
		}
	}

	@Test
	void processorResolvesGeneratedEmbeddedIdMembersOntoHierarchy() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext =
					new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ClassDetails entity = metadataBuildingContext.getBootstrapContext()
					.getModelsContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( CategorizedEmbeddedIdEntity.class.getName() );
			final var result = DomainModelCategorizer.categorize(
					new PreparedMappingSources( List.of( entity ), emptyList(), emptyList() ),
					metadataBuildingContext
			);

			final var hierarchy = result.getEntityHierarchies().iterator().next();
			final var embeddedId = (EmbeddedAttributeMetadata)
					( (org.hibernate.boot.mapping.internal.categorize.AggregatedKeyMapping) hierarchy.getIdMapping() )
							.getAttribute();
			final var generatedMember = embeddedId.getEmbeddableUsage().findAttribute( "generated" );
			assertThat( hierarchy.getIdentifierGeneratorResolution().find( generatedMember ) ).isNotNull();
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

	@Entity
	public static class CategorizedStructureEntity {
		@Id
		private Long id;

		@Embedded
		private CategorizedDetails details;

		@ElementCollection
		@CollectionId(generatorImplementation = IncrementGenerator.class)
		private List<CategorizedDetails> detailBag;

		@ElementCollection
		private Map<String, CategorizedDetails> detailsByName;
	}

	@Embeddable
	public static class CategorizedDetails extends CategorizedDetailsBase {
		private String name;
	}

	public static class CategorizedDetailsBase {
		private String inheritedName;
	}

	@Entity
	public static class CategorizedEmbeddedIdEntity {
		@EmbeddedId
		private CategorizedEmbeddedId id;
	}

	public static class CategorizedEmbeddedId {
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long generated;
		private String assigned;
	}
}
