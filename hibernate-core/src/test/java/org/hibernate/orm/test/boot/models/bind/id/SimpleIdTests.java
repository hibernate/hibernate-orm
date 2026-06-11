/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.id;

import java.util.Set;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.buildHierarchyMetadata;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SimpleIdTests {
	@Test
	void testSimpleId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( BasicIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final BasicKeyMapping naturalIdMapping = (BasicKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( NaturalId.class ) ).isNotNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	void testAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( AggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final AggregatedKeyMapping idMapping = (AggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNull();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNotNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		assertThat( entityHierarchy.getNaturalIdMapping() ).isInstanceOf( AggregatedKeyMapping.class );
		final AggregatedKeyMapping naturalIdMapping = (AggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( NaturalId.class ) ).isNotNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	@ServiceRegistry
	void testAggregatedIdBinding(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( AggregatedIdEntity.class.getName() );
					assertThat( entityBinding.getIdentifier() ).isInstanceOf( Component.class );
					assertThat( entityBinding.hasEmbeddedIdentifier() ).isTrue();
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );

					final org.hibernate.mapping.Property naturalId = entityBinding.getProperty( "naturalId" );
					assertThat( naturalId.isNaturalIdentifier() ).isTrue();
					assertThat( naturalId.getValue() ).isInstanceOf( Component.class );
					assertThat( ( (Component) naturalId.getValue() ).getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "key1", "key2" );
				},
				scope.getRegistry(),
				AggregatedIdEntity.class
		);
	}

	@Test
	void testNonAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( NonAggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final NonAggregatedKeyMapping idMapping = (NonAggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( idMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) ).containsExactly( "id1", "id2" );
		assertThat( idMapping.getIdClassType().getClassName() ).isEqualTo( NonAggregatedIdEntity.Pk.class.getName() );

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final NonAggregatedKeyMapping naturalIdMapping = (NonAggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( naturalIdMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) )
				.containsExactly( "naturalKey1", "naturalKey2" );

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.TRANSACTIONAL );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.TABLE_PER_CLASS );
	}

	@Test
	@ServiceRegistry
	void testNonAggregatedIdBinding(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( NonAggregatedIdEntity.class.getName() );
					assertThat( entityBinding.getIdentifier() ).isInstanceOf( Component.class );
					assertThat( entityBinding.hasEmbeddedIdentifier() ).isFalse();
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				NonAggregatedIdEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapsIdWithEmbeddedId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( MapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.getIdentifier() ).isInstanceOf( Component.class );
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id", "child_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
					assertThat( entityBinding.getProperty( "parent" ).isOptional() ).isFalse();
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				MapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitMapsIdWithEmbeddedId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitMapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false, false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false, false );
					assertThat( entityBinding.getProperty( "parent" ).isOptional() ).isFalse();
				},
				scope.getRegistry(),
				CompositeMapsIdParent.class,
				ImplicitMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapsIdWithComponentIdentifierAttribute(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ComponentAttributeMapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2", "child_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false, false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false, false );
				},
				scope.getRegistry(),
				CompositeMapsIdParent.class,
				ComponentAttributeMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapsIdWithIdClass(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( IdClassMapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id", "child_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				IdClassMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testOneToOneMapsIdWithEmbeddedId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( OneToOneMapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( parent.isLogicalOneToOne() ).isTrue();
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
					assertThat( entityBinding.getProperty( "parent" ).isOptional() ).isFalse();
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				OneToOneMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdClassAssociationAsId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( AssociationIdChild.class.getName() );
					final Component identifier = (Component) entityBinding.getIdentifier();
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.hasEmbeddedIdentifier() ).isFalse();
					assertThat( identifier.getProperty( "parent" ).getValue() ).isSameAs( parent );
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id", "child_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				AssociationIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdClassOneToOneAssociationAsId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( OneToOneAssociationIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( parent.isLogicalOneToOne() ).isTrue();
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id", "child_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				OneToOneAssociationIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdClassCompositeAssociationAsId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CompositeAssociationIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2", "child_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2" );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false, false );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				CompositeMapsIdParent.class,
				CompositeAssociationIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedMapsId(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( EmbeddedMapsIdChild.class.getName() );
					final Component details = (Component) entityBinding.getProperty( "details" ).getValue();
					final ManyToOne parent = (ManyToOne) details.getProperty( "parent" ).getValue();

					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				EmbeddedMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapsIdWithImplicitJoinColumnUsesIdentifierColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitJoinColumnMapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				ImplicitJoinColumnMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapsIdWithNonPrimaryKeyReference(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( NonPrimaryKeyMapsIdChild.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( parent.isReferenceToPrimaryKey() ).isFalse();
					assertThat( parent.getReferencedPropertyName() ).isEqualTo( "code" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
				},
				scope.getRegistry(),
				NonPrimaryKeyMapsIdParent.class,
				NonPrimaryKeyMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testAssociationIdWithJoinTable(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				MapsIdParent.class,
				JoinTableAssociationIdChild.class
		) )
				.hasMessageContaining( "must have same number of columns as the referenced primary key" );
	}

	@Test
	@ServiceRegistry
	void testAssociationIdWithNonPrimaryKeyReferenceFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				NonPrimaryKeyMapsIdParent.class,
				NonPrimaryKeyAssociationIdChild.class
		) ).isInstanceOf( org.hibernate.MappingException.class )
				.hasMessageContaining( "Unable to match association identifier join column referencedColumnName to target identifier column" );
	}

	@Test
	@ServiceRegistry
	void testAssociationIdJoinColumnCountMismatchFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				CompositeMapsIdParent.class,
				JoinColumnCountMismatchAssociationIdChild.class
		) ).isInstanceOf( org.hibernate.MappingException.class )
				.hasMessageContaining( "Association identifier join column count did not match target identifier column count" );
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneAssociationIdFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				InverseOneToOneAssociationIdParent.class,
				InverseOneToOneAssociationIdChild.class
		) ).isInstanceOf( UnsupportedOperationException.class )
				.hasMessageContaining( "Association identifiers are only implemented for owning to-one attributes" );
	}

	@Test
	@ServiceRegistry
	void testMapsIdMissingAttributeFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				MapsIdParent.class,
				MissingMapsIdAttributeChild.class
		) ).isInstanceOf( org.hibernate.MappingException.class )
				.hasMessageContaining( "@MapsId named unknown identifier attribute `missing`" );
	}

	@Test
	@ServiceRegistry
	void testMapsIdWithToOneIdentifierAttribute(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ToOneAttributeMapsIdChild.class.getName() );
					final Component identifier = (Component) entityBinding.getIdentifier();
					final ManyToOne identifierParent = (ManyToOne) identifier.getProperty( "parentRef" ).getValue();
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id", "child_id" );
					assertThat( identifierParent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( parent.getColumnInsertability() ).containsExactly( false );
					assertThat( parent.getColumnUpdateability() ).containsExactly( false );
				},
				scope.getRegistry(),
				MapsIdParent.class,
				ToOneAttributeMapsIdChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapsIdJoinColumnNameMismatchFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				MapsIdParent.class,
				JoinColumnNameMismatchMapsIdChild.class
		) ).isInstanceOf( org.hibernate.MappingException.class )
				.hasMessageContaining( "@MapsId join column name did not match identifier attribute column name `parent_id`" );
	}

	@Test
	@ServiceRegistry
	void testMapsIdJoinColumnCountMismatchFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				CompositeMapsIdParent.class,
				JoinColumnCountMismatchMapsIdChild.class
		) ).isInstanceOf( org.hibernate.MappingException.class )
				.hasMessageContaining( "@MapsId identifier attribute column count did not match target identifier column count" );
	}

	@Entity(name = "MapsIdParent")
	@Table(name = "maps_id_parents")
	public static class MapsIdParent {
		@Id
		@Column(name = "parent_id")
		private Integer id;
	}

	@Entity(name = "InverseOneToOneAssociationIdParent")
	@Table(name = "inverse_one_to_one_association_id_parents")
	public static class InverseOneToOneAssociationIdParent {
		@Id
		@Column(name = "parent_id")
		private Integer id;

		@OneToOne
		private InverseOneToOneAssociationIdChild parent;
	}

	@Entity(name = "NonPrimaryKeyMapsIdParent")
	@Table(name = "non_pk_maps_id_parents")
	public static class NonPrimaryKeyMapsIdParent {
		@Id
		@Column(name = "parent_id")
		private Integer id;

		@Column(name = "parent_code")
		private String code;
	}

	@Entity(name = "CompositeMapsIdParent")
	@Table(name = "composite_maps_id_parents")
	public static class CompositeMapsIdParent {
		@EmbeddedId
		private CompositeMapsIdPk id;
	}

	@Entity(name = "MapsIdChild")
	@Table(name = "maps_id_children")
	public static class MapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;

		private String name;
	}

	@Entity(name = "ImplicitMapsIdChild")
	@Table(name = "implicit_maps_id_children")
	public static class ImplicitMapsIdChild {
		@EmbeddedId
		private CompositeMapsIdPk id;

		@jakarta.persistence.ManyToOne
		@MapsId
		@JoinColumns({
				@JoinColumn(name = "parent_id1", referencedColumnName = "parent_id1"),
				@JoinColumn(name = "parent_id2", referencedColumnName = "parent_id2")
		})
		private CompositeMapsIdParent parent;
	}

	@Entity(name = "ComponentAttributeMapsIdChild")
	@Table(name = "component_attribute_maps_id_children")
	public static class ComponentAttributeMapsIdChild {
		@EmbeddedId
		private ComponentAttributeMapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumns({
				@JoinColumn(name = "parent_id1", referencedColumnName = "parent_id1"),
				@JoinColumn(name = "parent_id2", referencedColumnName = "parent_id2")
		})
		private CompositeMapsIdParent parent;
	}

	@Entity(name = "IdClassMapsIdChild")
	@Table(name = "id_class_maps_id_children")
	@IdClass(IdClassMapsIdChildPk.class)
	public static class IdClassMapsIdChild {
		@Id
		@Column(name = "parent_id")
		private Integer parentId;

		@Id
		@Column(name = "child_id")
		private Integer childId;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;
	}

	@Entity(name = "OneToOneMapsIdChild")
	@Table(name = "one_to_one_maps_id_children")
	public static class OneToOneMapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@OneToOne
		@MapsId("parentId")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;
	}

	@Entity(name = "AssociationIdChild")
	@Table(name = "association_id_children")
	@IdClass(AssociationIdChildPk.class)
	public static class AssociationIdChild {
		@Id
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "OneToOneAssociationIdChild")
	@Table(name = "one_to_one_association_id_children")
	@IdClass(AssociationIdChildPk.class)
	public static class OneToOneAssociationIdChild {
		@Id
		@OneToOne
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "CompositeAssociationIdChild")
	@Table(name = "composite_association_id_children")
	@IdClass(CompositeAssociationIdChildPk.class)
	public static class CompositeAssociationIdChild {
		@Id
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_id1", referencedColumnName = "parent_id1"),
				@JoinColumn(name = "parent_id2", referencedColumnName = "parent_id2")
		})
		private CompositeMapsIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "JoinTableAssociationIdChild")
	@Table(name = "join_table_association_id_children")
	@IdClass(AssociationIdChildPk.class)
	public static class JoinTableAssociationIdChild {
		@Id
		@jakarta.persistence.ManyToOne
		@jakarta.persistence.JoinTable(
				name = "association_id_join_table",
				joinColumns = @JoinColumn(name = "child_id", referencedColumnName = "child_id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		)
		private MapsIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "NonPrimaryKeyAssociationIdChild")
	@Table(name = "non_pk_association_id_children")
	@IdClass(AssociationIdChildPk.class)
	public static class NonPrimaryKeyAssociationIdChild {
		@Id
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_code")
		private NonPrimaryKeyMapsIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "JoinColumnCountMismatchAssociationIdChild")
	@Table(name = "join_column_count_mismatch_association_id_children")
	@IdClass(AssociationIdChildPk.class)
	public static class JoinColumnCountMismatchAssociationIdChild {
		@Id
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_id1", referencedColumnName = "parent_id1")
		private CompositeMapsIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "InverseOneToOneAssociationIdChild")
	@Table(name = "inverse_one_to_one_association_id_children")
	@IdClass(AssociationIdChildPk.class)
	public static class InverseOneToOneAssociationIdChild {
		@Id
		@OneToOne(mappedBy = "parent")
		private InverseOneToOneAssociationIdParent parent;

		@Id
		@Column(name = "child_id")
		private Integer childId;
	}

	@Entity(name = "EmbeddedMapsIdChild")
	@Table(name = "embedded_maps_id_children")
	public static class EmbeddedMapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		private EmbeddedMapsIdDetails details;
	}

	@Embeddable
	public static class EmbeddedMapsIdDetails {
		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;
	}

	@Entity(name = "ImplicitJoinColumnMapsIdChild")
	@Table(name = "implicit_join_column_maps_id_children")
	public static class ImplicitJoinColumnMapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		private MapsIdParent parent;
	}

	@Entity(name = "NonPrimaryKeyMapsIdChild")
	@Table(name = "non_pk_maps_id_children")
	public static class NonPrimaryKeyMapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_code")
		private NonPrimaryKeyMapsIdParent parent;
	}

	@Entity(name = "MissingMapsIdAttributeChild")
	@Table(name = "missing_maps_id_attribute_children")
	public static class MissingMapsIdAttributeChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("missing")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;
	}

	@Entity(name = "ToOneAttributeMapsIdChild")
	@Table(name = "to_one_attribute_maps_id_children")
	public static class ToOneAttributeMapsIdChild {
		@EmbeddedId
		private ToOneAttributeMapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentRef")
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;
	}

	@Entity(name = "JoinColumnNameMismatchMapsIdChild")
	@Table(name = "join_column_name_mismatch_maps_id_children")
	public static class JoinColumnNameMismatchMapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumn(name = "wrong_parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parent;
	}

	@Entity(name = "JoinColumnCountMismatchMapsIdChild")
	@Table(name = "join_column_count_mismatch_maps_id_children")
	public static class JoinColumnCountMismatchMapsIdChild {
		@EmbeddedId
		private MapsIdChildPk id;

		@jakarta.persistence.ManyToOne
		@MapsId("parentId")
		@JoinColumns({
				@JoinColumn(name = "parent_id1", referencedColumnName = "parent_id1"),
				@JoinColumn(name = "parent_id2", referencedColumnName = "parent_id2")
		})
		private CompositeMapsIdParent parent;
	}

	@Embeddable
	public static class MapsIdChildPk {
		@Column(name = "parent_id")
		private Integer parentId;

		@Column(name = "child_id")
		private Integer childId;
	}

	@Embeddable
	public static class CompositeMapsIdPk {
		@Column(name = "parent_id1")
		private Integer parentId1;

		@Column(name = "parent_id2")
		private Integer parentId2;
	}

	@Embeddable
	public static class ComponentAttributeMapsIdChildPk {
		private CompositeMapsIdPk parentId;

		@Column(name = "child_id")
		private Integer childId;
	}

	@Embeddable
	public static class ToOneAttributeMapsIdChildPk {
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_id", referencedColumnName = "parent_id")
		private MapsIdParent parentRef;

		@Column(name = "child_id")
		private Integer childId;
	}

	public static class IdClassMapsIdChildPk {
		private Integer parentId;
		private Integer childId;
	}

	public static class AssociationIdChildPk {
		private Integer parent;
		private Integer childId;
	}

	public static class CompositeAssociationIdChildPk {
		private CompositeMapsIdPk parent;
		private Integer childId;
	}
}
