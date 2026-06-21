/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.associations;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaClass;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.ToOneValueIntent;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("removal")
public class ToOneAssociationTests {
	@Test
	@ServiceRegistry
	void testImplicitManyToOne(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testJpaFetchOverridesToOneFetchType(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JpaFetchManyToOneOwner.class.getName() );

					final ManyToOne lazyValue = (ManyToOne) entityBinding.getProperty( "lazyParent" ).getValue();
					assertThat( lazyValue.isLazy() ).isTrue();

					final ManyToOne eagerValue = (ManyToOne) entityBinding.getProperty( "eagerParent" ).getValue();
					assertThat( eagerValue.isLazy() ).isFalse();
				},
				scope.getRegistry(),
				Parent.class,
				JpaFetchManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToOneCascade(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CascadeManyToOneOwner.class.getName() );
					final var property = entityBinding.getProperty( "target" );

					assertThat( property.getCascade() )
							.contains( "persist" );
				},
				scope.getRegistry(),
				CascadeManyToOneOwner.class,
				CascadeToOneTarget.class
		);
	}

	@Test
	@ServiceRegistry
	void testSelfReferentialManyToOneNonPrimaryKeyReference(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SelfReferentialManyToOneNonPkOwner.class.getName() );
					final ManyToOne value = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( value.isReferenceToPrimaryKey() ).isFalse();
					assertThat( value.getReferencedPropertyName() ).isEqualTo( "code" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_code" );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				SelfReferentialManyToOneNonPkOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningOneToOne(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.isLogicalOneToOne() ).isTrue();
					assertThat( value.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk" );
					assertThat( value.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( value.getColumns().get( 0 ).isNullable() ).isFalse();
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				Parent.class,
				OneToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningOneToOnePrimaryKeyJoinColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( PrimaryKeyJoinColumnOneToOneOwner.class.getName() );
					final ManyToOne value = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();

					assertThat( value.isLogicalOneToOne() ).isTrue();
					assertThat( value.isReferenceToPrimaryKey() ).isTrue();
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_pk" );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				Parent.class,
				PrimaryKeyJoinColumnOneToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningOneToOneCascadeAndOrphanRemoval(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CascadeOneToOneOwner.class.getName() );
					final var property = entityBinding.getProperty( "target" );

					assertThat( property.getCascade() )
							.contains( "merge" )
							.contains( "delete" )
							.contains( "delete-orphan" );
				},
				scope.getRegistry(),
				CascadeOneToOneOwner.class,
				CascadeToOneTarget.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToOneParent.class.getName() );
					final org.hibernate.mapping.Property property = inverseEntityBinding.getProperty( "child" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.OneToOne.class );
					final org.hibernate.mapping.OneToOne value = (org.hibernate.mapping.OneToOne) property.getValue();

					assertThat( value.getReferencedEntityName() ).isEqualTo( MappedByOneToOneChild.class.getName() );
					assertThat( value.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( value.getReferencedPropertyName() ).isEqualTo( "parent" );
					assertThat( value.isReferenceToPrimaryKey() ).isFalse();
					assertThat( value.getForeignKeyType() ).isEqualTo( org.hibernate.type.ForeignKeyDirection.TO_PARENT );
					assertThat( value.getTable().getName() ).isEqualTo( "mapped_by_one_to_one_parents" );
					assertThat( value.getColumns() ).isEmpty();
				},
				scope.getRegistry(),
				MappedByOneToOneParent.class,
				MappedByOneToOneChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneMappedByJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByJoinTableOneToOneParent.class.getName() );
					final Join inverseJoin = inverseEntityBinding.getJoins().get( 0 );
					final org.hibernate.mapping.Property property = inverseJoin.getProperties().get( 0 );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( inverseJoin.isInverse() ).isTrue();
					assertThat( inverseJoin.getTable().getName() ).isEqualTo( "mapped_by_one_to_one_links" );
					assertThat( inverseJoin.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( value.isLogicalOneToOne() ).isTrue();
					assertThat( value.getReferencedEntityName() ).isEqualTo( MappedByJoinTableOneToOneChild.class.getName() );
					assertThat( value.getReferencedPropertyName() ).isEqualTo( "parent" );
					assertThat( value.isReferenceToPrimaryKey() ).isFalse();
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
				},
				scope.getRegistry(),
				MappedByJoinTableOneToOneParent.class,
				MappedByJoinTableOneToOneChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneMappedByManyToOneFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				InvalidMappedByOneToOneParent.class,
				InvalidMappedByManyToOneChild.class
		) ).isInstanceOf( MappingException.class )
				.hasMessageContaining( "mappedBy did not name an owning one-to-one attribute" );
	}

	@Test
	@ServiceRegistry
	void testCompositeManyToOne(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeManyToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.getReferencedEntityName() ).isEqualTo( CompositeParent.class.getName() );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2" );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				CompositeParent.class,
				CompositeManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeManyToOneWithReferencedColumnNames(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ExplicitCompositeManyToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
				},
				scope.getRegistry(),
				CompositeParent.class,
				ExplicitCompositeManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testMixedJoinColumnTables(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				CompositeParent.class,
				MixedJoinColumnTablesOwner.class
		) ).isInstanceOf( MappingException.class )
					.hasMessageContaining( "To-one join columns cannot span multiple tables" );
	}

	@Test
	@ServiceRegistry
	void testManyToOneJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JoinTableManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );
					final ManyToOne value = (ManyToOne) join.getProperties().get( 0 ).getValue();

					assertThat( join.getTable().getName() ).isEqualTo( "owner_parent_links" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( join.getTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Parent.class,
				JoinTableManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitManyToOneJoinTableName(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitJoinTableManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );

					assertThat( join.getTable().getName() ).isEqualTo( "implicit_join_table_many_to_one_owners_parents" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ImplicitJoinTableManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							ManyToManyOwner.class,
							"parents"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( ToOneValueIntent.class );
					assertThat( collection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( collection.getRole() ).isEqualTo( ManyToManyOwner.class.getName() + ".parents" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_sets" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() )
							.extracting( org.hibernate.mapping.ForeignKey::getName )
							.contains( "fk_owner_parent_sets_owner", "fk_owner_parent_sets_parent" );
					assertThat( context.getMetadataCollector().getCollectionBinding( collection.getRole() ) ).isSameAs( collection );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToManyCascade(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CascadeManyToManyOwner.class.getName() );
					final var property = entityBinding.getProperty( "targets" );

					assertThat( property.getCascade() ).contains( "refresh" );
				},
				scope.getRegistry(),
				CascadeManyToManyOwner.class,
				CascadeManyToManyTarget.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyListJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyListOwner.class.getName() );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) entityBinding.getProperty( "parents" )
							.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_lists" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "position" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyIdBagJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyIdBagOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "parents" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_idbags" );
					assertThat( identifier.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "link_id" );
					assertThat( identifier.getColumns().get( 0 ).isNullable() ).isFalse();
					assertThat( identifier.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Long.class );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyIdBagOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyOrderBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyOrderByOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();

					assertThat( collection.getOrderBy() ).isEqualTo( "name desc" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyOrderByOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyEmptyOrderBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyEmptyOrderByOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();

					assertThat( collection.getOrderBy() ).isNull();
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyEmptyOrderByOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyNonPrimaryKeyReference(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyNonPkReferenceOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( element.isReferenceToPrimaryKey() ).isFalse();
					assertThat( element.getReferencedPropertyName() ).isEqualTo( "code" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_code" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				ManyToManyNonPkReferenceTarget.class,
				ManyToManyNonPkReferenceOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyImplicitJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyImplicitJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() )
							.isEqualTo( "many_to_many_implicit_join_table_owners_parents" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parents_id" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyImplicitJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyMapJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyMapOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final BasicValue index = (BasicValue) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_maps" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyEntityMapKeyJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyEntityMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne index = (ManyToOne) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_entity_key_maps" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_key_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				Child.class,
				ManyToManyEntityMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyEntityMapKeyWithoutMapKeyJoinColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyEntityMapKeyWithoutJoinColumnOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne index = (ManyToOne) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() )
							.isEqualTo( "owner_parent_entity_key_without_join_column_maps" );
					assertThat( index.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( index.isReferenceToPrimaryKey() ).isTrue();
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 3 );
				},
				scope.getRegistry(),
				Parent.class,
				Child.class,
				ManyToManyEntityMapKeyWithoutJoinColumnOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyPropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyPropertyMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_property_maps" );
					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "id" );
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyPropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyToOnePropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyToOnePropertyMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne index = (ManyToOne) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "mapKeyChild" );
					assertThat( index.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "map_key_child_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( ManyToManyToOnePropertyMapKeyParent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Child.class,
				ManyToManyToOnePropertyMapKeyParent.class,
				ManyToManyToOnePropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testSelfReferentialManyToManyPropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SelfReferentialPropertyMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();

					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "code" );
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "code" );
				},
				scope.getRegistry(),
				SelfReferentialPropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyImplicitMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyImplicitMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isNull();
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyImplicitMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyParent.class.getName() );
					final Collection inverseCollection = (Collection) inverseEntityBinding.getProperty( "owners" ).getValue();
					final ManyToOne inverseElement = (ManyToOne) inverseCollection.getElement();

					assertThat( inverseCollection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_owner_parent_sets" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( inverseElement.getReferencedEntityName() )
							.isEqualTo( MappedByManyToManyOwner.class.getName() );
					assertThat( inverseElement.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( context.getMetadataCollector().getCollectionBinding( inverseCollection.getRole() ) )
							.isSameAs( inverseCollection );
				},
				scope.getRegistry(),
				MappedByManyToManyParent.class,
				MappedByManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMapMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyMapParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "owners" )
							.getValue();
					final ManyToOne inverseElement = (ManyToOne) inverseCollection.getElement();
					final BasicValue inverseIndex = (BasicValue) inverseCollection.getIndex();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_owner_parent_maps" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( inverseElement.getReferencedEntityName() )
							.isEqualTo( MappedByManyToManyMapOwner.class.getName() );
					assertThat( inverseElement.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( inverseIndex.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key" );
				},
				scope.getRegistry(),
				MappedByManyToManyMapParent.class,
				MappedByManyToManyMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMapMappedByPropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyPropertyMapKeyParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "owners" )
							.getValue();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.hasMapKeyProperty() ).isTrue();
					assertThat( inverseCollection.getMapKeyPropertyName() ).isEqualTo( "code" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_owner_parent_property_maps" );
					assertThat( inverseCollection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "code" );
				},
				scope.getRegistry(),
				MappedByManyToManyPropertyMapKeyParent.class,
				MappedByManyToManyPropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMapMappedByEntityMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyEntityMapKeyParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "owners" )
							.getValue();
					final ManyToOne inverseIndex = (ManyToOne) inverseCollection.getIndex();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_owner_parent_entity_key_maps" );
					assertThat( inverseIndex.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( inverseIndex.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_key_id" );
				},
				scope.getRegistry(),
				Child.class,
				MappedByManyToManyEntityMapKeyParent.class,
				MappedByManyToManyEntityMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMapMappedByImplicitEntityMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyImplicitEntityMapKeyParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "owners" )
							.getValue();
					final ManyToOne inverseIndex = (ManyToOne) inverseCollection.getIndex();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.getCollectionTable().getName() )
							.isEqualTo( "mapped_by_owner_parent_implicit_entity_key_maps" );
					assertThat( inverseIndex.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( inverseIndex.isReferenceToPrimaryKey() ).isTrue();
					assertThat( inverseIndex.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id_id" );
				},
				scope.getRegistry(),
				Child.class,
				MappedByManyToManyImplicitEntityMapKeyParent.class,
				MappedByManyToManyImplicitEntityMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMapMappedByToOnePropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyToOnePropertyMapKeyParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "owners" )
							.getValue();
					final ManyToOne inverseIndex = (ManyToOne) inverseCollection.getIndex();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.hasMapKeyProperty() ).isTrue();
					assertThat( inverseCollection.getMapKeyPropertyName() ).isEqualTo( "mapKeyChild" );
					assertThat( inverseCollection.getCollectionTable().getName() )
							.isEqualTo( "mapped_by_owner_parent_to_one_property_maps" );
					assertThat( inverseIndex.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( inverseIndex.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "map_key_child_id" );
				},
				scope.getRegistry(),
				Child.class,
				MappedByManyToManyToOnePropertyMapKeyParent.class,
				MappedByManyToManyToOnePropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToManyParent.class.getName() );
					final Collection inverseCollection = (Collection) inverseEntityBinding.getProperty( "children" ).getValue();

					assertThat( inverseCollection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.isOneToMany() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_children" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk" );
					assertThat( inverseCollection.getElement() ).isInstanceOf( org.hibernate.mapping.OneToMany.class );
					assertThat( ( (org.hibernate.mapping.OneToMany) inverseCollection.getElement() ).getReferencedEntityName() )
							.isEqualTo( MappedByChild.class.getName() );
					assertThat( context.getMetadataCollector().getCollectionBinding( inverseCollection.getRole() ) )
							.isSameAs( inverseCollection );
				},
				scope.getRegistry(),
				MappedByOneToManyParent.class,
				MappedByChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyCascadeAndOrphanRemoval(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CascadeOneToManyParent.class.getName() );
					final var property = entityBinding.getProperty( "children" );
					final Collection collection = (Collection) property.getValue();

					assertThat( property.getCascade() )
							.contains( "delete" )
							.contains( "delete-orphan" );
					assertThat( collection.hasOrphanDelete() ).isTrue();
				},
				scope.getRegistry(),
				CascadeOneToManyParent.class,
				CascadeOneToManyChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMapMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToManyMapParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "children" )
							.getValue();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.isOneToMany() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( inverseCollection.hasMapKeyProperty() ).isTrue();
					assertThat( inverseCollection.getMapKeyPropertyName() ).isEqualTo( "code" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_map_children" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk" );
					assertThat( inverseCollection.getElement() ).isInstanceOf( org.hibernate.mapping.OneToMany.class );
					assertThat( ( (org.hibernate.mapping.OneToMany) inverseCollection.getElement() ).getReferencedEntityName() )
							.isEqualTo( MappedByMapChild.class.getName() );
					assertThat( inverseCollection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "code" );
				},
				scope.getRegistry(),
				MappedByOneToManyMapParent.class,
				MappedByMapChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMapMappedByWithoutMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToManyMapWithoutMapKeyParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "children" )
							.getValue();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.isOneToMany() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( inverseCollection.hasMapKeyProperty() ).isTrue();
					assertThat( inverseCollection.getMapKeyPropertyName() ).isNull();
					assertThat( inverseCollection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
				},
				scope.getRegistry(),
				MappedByOneToManyMapWithoutMapKeyParent.class,
				MappedByMapWithoutMapKeyChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMapMappedByImplicitMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToManyMapImplicitMapKeyParent.class.getName() );
					final org.hibernate.mapping.Map inverseCollection = (org.hibernate.mapping.Map) inverseEntityBinding
							.getProperty( "children" )
							.getValue();

					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.isOneToMany() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( inverseCollection.hasMapKeyProperty() ).isTrue();
					assertThat( inverseCollection.getMapKeyPropertyName() ).isNull();
					assertThat( inverseCollection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
				},
				scope.getRegistry(),
				MappedByOneToManyMapImplicitMapKeyParent.class,
				MappedByMapImplicitMapKeyChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMappedByCompositeOwner(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeMappedByOneToManyParent.class.getName() );
					final Collection inverseCollection = (Collection) inverseEntityBinding.getProperty( "children" ).getValue();

					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "composite_mapped_by_children" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
					assertThat( inverseCollection.getElement() ).isInstanceOf( org.hibernate.mapping.OneToMany.class );
				},
				scope.getRegistry(),
				CompositeMappedByOneToManyParent.class,
				CompositeMappedByChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							OneToManyJoinTableOwner.class,
							"children"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( ToOneValueIntent.class );
					assertThat( collection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( collection.getRole() ).isEqualTo( OneToManyJoinTableOwner.class.getName() + ".children" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_child_links" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Child.class,
				OneToManyJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyImplicitJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyImplicitJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() )
							.isEqualTo( "one_to_many_implicit_join_table_owners_children" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "children_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Child.class,
				OneToManyImplicitJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyMapJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyMapJoinTableOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "children" ).getValue();
					final BasicValue index = (BasicValue) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_child_maps" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_key" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				OneToManyMapJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyPropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyPropertyMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_child_property_maps" );
					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "id" );
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				OneToManyPropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyImplicitMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyImplicitMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isNull();
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				OneToManyImplicitMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToManyJoinTableWithCompositeOwner(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeOwnerManyToManyOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "composite_owner_parent_sets" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_fk1", "owner_fk2" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				CompositeOwnerManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToManyJoinTableWithCompositeTarget(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeTargetManyToManyOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_composite_parent_sets" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
				},
				scope.getRegistry(),
				CompositeParent.class,
				CompositeTargetManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOneToManyJoinTableWithCompositeOwner(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeOwnerOneToManyJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "composite_owner_child_links" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_fk1", "owner_fk2" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				CompositeOwnerOneToManyJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOneToManyJoinTableWithCompositeTarget(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeTargetOneToManyJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_composite_child_links" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_fk1", "child_fk2" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( element.getColumns().get( 1 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				CompositeChild.class,
				CompositeTargetOneToManyJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeManyToOneJoinTableWithReferencedColumnNames(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JoinTableCompositeManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );
					final ManyToOne value = (ManyToOne) join.getProperties().get( 0 ).getValue();

					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
				},
				scope.getRegistry(),
				CompositeParent.class,
				JoinTableCompositeManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeOwnerManyToOneJoinTableWithReferencedColumnNames(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeOwnerJoinTableManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );
					final ManyToOne value = (ManyToOne) join.getProperties().get( 0 ).getValue();

					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_fk1", "owner_fk2" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				CompositeOwnerJoinTableManyToOneOwner.class
		);
	}

	@Entity(name="Parent")
	@Table(name="parents")
	public static class Parent {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Child")
	@Table(name="children")
	public static class Child {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="CascadeToOneTarget")
	@Table(name="cascade_to_one_targets")
	public static class CascadeToOneTarget {
		@Id
		private Integer id;
	}

	@Entity(name="ManyToOneOwner")
	@Table(name="many_to_one_owners")
	public static class ManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		private Parent parent;
	}

	@Entity(name="JpaFetchManyToOneOwner")
	@Table(name="jpa_fetch_many_to_one_owners")
	public static class JpaFetchManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne(fetch = FetchType.EAGER)
		@Fetch(type = FetchType.LAZY)
		private Parent lazyParent;
		@jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
		@Fetch(type = FetchType.EAGER)
		private Parent eagerParent;
	}

	@Entity(name="CascadeManyToOneOwner")
	@Table(name="cascade_many_to_one_owners")
	public static class CascadeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne(cascade = CascadeType.PERSIST)
		private CascadeToOneTarget target;
	}

	@Entity(name="SelfReferentialManyToOneNonPkOwner")
	@Table(name="self_referential_many_to_one_non_pk_owners")
	public static class SelfReferentialManyToOneNonPkOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_code", referencedColumnName = "code")
		private SelfReferentialManyToOneNonPkOwner parent;
		private String code;
	}

	@Entity(name="OneToOneOwner")
	@Table(name="one_to_one_owners")
	public static class OneToOneOwner {
		@Id
		private Integer id;
		@OneToOne(optional = false)
		@JoinColumn(name = "parent_fk")
		private Parent parent;
	}

	@Entity(name="PrimaryKeyJoinColumnOneToOneOwner")
	@Table(name="primary_key_join_column_one_to_one_owners")
	public static class PrimaryKeyJoinColumnOneToOneOwner {
		@Id
		private Integer id;
		@OneToOne(optional = false)
		@PrimaryKeyJoinColumn(name = "parent_pk", referencedColumnName = "id")
		private Parent parent;
	}

	@Entity(name="CascadeOneToOneOwner")
	@Table(name="cascade_one_to_one_owners")
	public static class CascadeOneToOneOwner {
		@Id
		private Integer id;
		@OneToOne(cascade = CascadeType.MERGE, orphanRemoval = true)
		private CascadeToOneTarget target;
	}

	@Entity(name="MappedByOneToOneParent")
	@Table(name="mapped_by_one_to_one_parents")
	public static class MappedByOneToOneParent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent")
		private MappedByOneToOneChild child;
	}

	@Entity(name="MappedByOneToOneChild")
	@Table(name="mapped_by_one_to_one_children")
	public static class MappedByOneToOneChild {
		@Id
		private Integer id;
		@OneToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToOneParent parent;
	}

	@Entity(name="MappedByJoinTableOneToOneParent")
	@Table(name="mapped_by_join_table_one_to_one_parents")
	public static class MappedByJoinTableOneToOneParent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent")
		private MappedByJoinTableOneToOneChild child;
	}

	@Entity(name="MappedByJoinTableOneToOneChild")
	@Table(name="mapped_by_join_table_one_to_one_children")
	public static class MappedByJoinTableOneToOneChild {
		@Id
		private Integer id;
		@OneToOne
		@JoinTable(
				name = "mapped_by_one_to_one_links",
				joinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private MappedByJoinTableOneToOneParent parent;
	}

	@Entity(name="InvalidMappedByOneToOneParent")
	@Table(name="invalid_mapped_by_one_to_one_parents")
	public static class InvalidMappedByOneToOneParent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent")
		private InvalidMappedByManyToOneChild child;
	}

	@Entity(name="InvalidMappedByManyToOneChild")
	@Table(name="invalid_mapped_by_many_to_one_children")
	public static class InvalidMappedByManyToOneChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private InvalidMappedByOneToOneParent parent;
	}

	@Entity(name="CompositeParent")
	@Table(name="composite_parents")
	public static class CompositeParent {
		@EmbeddedId
		private Pk id;
		private String name;
	}

	@Entity(name="CompositeChild")
	@Table(name="composite_children")
	public static class CompositeChild {
		@EmbeddedId
		private Pk id;
		private String name;
	}

	@Entity(name="CompositeManyToOneOwner")
	@Table(name="composite_many_to_one_owners")
	public static class CompositeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		private CompositeParent parent;
	}

	@Entity(name="ExplicitCompositeManyToOneOwner")
	@Table(name="explicit_composite_many_to_one_owners")
	public static class ExplicitCompositeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
				@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
		})
		private CompositeParent parent;
	}

	@Entity(name="MixedJoinColumnTablesOwner")
	@Table(name="mixed_join_column_tables")
	@SecondaryTable(name="mixed_join_column_tables_details")
	public static class MixedJoinColumnTablesOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_fk1", referencedColumnName = "id1"),
				@JoinColumn(name = "parent_fk2", referencedColumnName = "id2", table = "mixed_join_column_tables_details")
		})
		private CompositeParent parent;
	}

	@Entity(name="JoinTableManyToOneOwner")
	@Table(name="join_table_many_to_one_owners")
	public static class JoinTableManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				name = "owner_parent_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Parent parent;
	}

	@Entity(name="ImplicitJoinTableManyToOneOwner")
	@Table(name="implicit_join_table_many_to_one_owners")
	public static class ImplicitJoinTableManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Parent parent;
	}

	@Entity(name="ManyToManyOwner")
	@Table(name="many_to_many_owners")
	public static class ManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id"),
				foreignKey = @ForeignKey(name = "fk_owner_parent_sets_owner"),
				inverseForeignKey = @ForeignKey(name = "fk_owner_parent_sets_parent")
		)
		private Set<Parent> parents;
	}

	@Entity(name="ManyToManyListOwner")
	@Table(name="many_to_many_list_owners")
	public static class ManyToManyListOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_lists",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		private List<Parent> parents;
	}

	@Entity(name="ManyToManyIdBagOwner")
	@Table(name="many_to_many_idbag_owners")
	public static class ManyToManyIdBagOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_idbags",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @jakarta.persistence.Column(name = "link_id"))
		@CollectionIdJavaClass(idType = Long.class)
		private java.util.Collection<Parent> parents;
	}

	@Entity(name="ManyToManyEmptyOrderByOwner")
	@Table(name="many_to_many_empty_order_by_owners")
	public static class ManyToManyEmptyOrderByOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_ordered_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@OrderBy
		private Set<Parent> parents;
	}

	@Entity(name="ManyToManyOrderByOwner")
	@Table(name="many_to_many_order_by_owners")
	public static class ManyToManyOrderByOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_ordered_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@OrderBy("name desc")
		private Set<Parent> parents;
	}

	@Entity(name="CascadeManyToManyTarget")
	@Table(name="cascade_many_to_many_targets")
	public static class CascadeManyToManyTarget {
		@Id
		private Integer id;
	}

	@Entity(name="CascadeManyToManyOwner")
	@Table(name="cascade_many_to_many_owners")
	public static class CascadeManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany(cascade = CascadeType.REFRESH)
		private Set<CascadeManyToManyTarget> targets;
	}

	@Entity(name="ManyToManyNonPkReferenceTarget")
	@Table(name="many_to_many_non_pk_reference_targets")
	public static class ManyToManyNonPkReferenceTarget {
		@Id
		private Integer id;
		private String code;
	}

	@Entity(name="ManyToManyNonPkReferenceOwner")
	@Table(name="many_to_many_non_pk_reference_owners")
	public static class ManyToManyNonPkReferenceOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_non_pk_reference_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_code", referencedColumnName = "code")
		)
		private Set<ManyToManyNonPkReferenceTarget> parents;
	}

	@Entity(name="ManyToManyImplicitJoinTableOwner")
	@Table(name="many_to_many_implicit_join_table_owners")
	public static class ManyToManyImplicitJoinTableOwner {
		@Id
		private Integer id;
		@ManyToMany
		private Set<Parent> parents;
	}

	@Entity(name="ManyToManyMapOwner")
	@Table(name="many_to_many_map_owners")
	public static class ManyToManyMapOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_key")
		private Map<String, Parent> parents;
	}

	@Entity(name="ManyToManyEntityMapKeyOwner")
	@Table(name="many_to_many_entity_map_key_owners")
	public static class ManyToManyEntityMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_entity_key_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKeyJoinColumn(name = "child_key_id", referencedColumnName = "id")
		private Map<Child, Parent> parents;
	}

	@Entity(name="ManyToManyEntityMapKeyWithoutJoinColumnOwner")
	@Table(name="many_to_many_entity_map_key_without_join_column_owners")
	public static class ManyToManyEntityMapKeyWithoutJoinColumnOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_entity_key_without_join_column_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Map<Child, Parent> parents;
	}

	@Entity(name="ManyToManyPropertyMapKeyOwner")
	@Table(name="many_to_many_property_map_key_owners")
	public static class ManyToManyPropertyMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey(name = "id")
		private Map<Integer, Parent> parents;
	}

	@Entity(name="ManyToManyToOnePropertyMapKeyParent")
	@Table(name="many_to_many_to_one_property_map_key_parents")
	public static class ManyToManyToOnePropertyMapKeyParent {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "map_key_child_id", referencedColumnName = "id")
		private Child mapKeyChild;
	}

	@Entity(name="ManyToManyToOnePropertyMapKeyOwner")
	@Table(name="many_to_many_to_one_property_map_key_owners")
	public static class ManyToManyToOnePropertyMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_to_one_property_key_parent_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey(name = "mapKeyChild")
		private Map<Child, ManyToManyToOnePropertyMapKeyParent> parents;
	}

	@Entity(name="SelfReferentialPropertyMapKeyOwner")
	@Table(name="self_referential_property_map_key_owners")
	public static class SelfReferentialPropertyMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "self_parent_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey(name = "code")
		private Map<String, SelfReferentialPropertyMapKeyOwner> parents;
		private String code;
	}

	@Entity(name="ManyToManyImplicitMapKeyOwner")
	@Table(name="many_to_many_implicit_map_key_owners")
	public static class ManyToManyImplicitMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_implicit_key_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey
		private Map<Integer, Parent> parents;
	}

	@Entity(name="MappedByManyToManyParent")
	@Table(name="mapped_by_many_to_many_parents")
	public static class MappedByManyToManyParent {
		@Id
		private Integer id;
		@ManyToMany(mappedBy = "parents")
		private Set<MappedByManyToManyOwner> owners;
	}

	@Entity(name="MappedByManyToManyOwner")
	@Table(name="mapped_by_many_to_many_owners")
	public static class MappedByManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Set<MappedByManyToManyParent> parents;
	}

	@Entity(name="MappedByManyToManyMapParent")
	@Table(name="mapped_by_many_to_many_map_parents")
	public static class MappedByManyToManyMapParent {
		@Id
		private Integer id;
		@ManyToMany(mappedBy = "parents")
		private Map<String, MappedByManyToManyMapOwner> owners;
	}

	@Entity(name="MappedByManyToManyMapOwner")
	@Table(name="mapped_by_many_to_many_map_owners")
	public static class MappedByManyToManyMapOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_key")
		private Map<String, MappedByManyToManyMapParent> parents;
	}

	@Entity(name="MappedByManyToManyPropertyMapKeyParent")
	@Table(name="mapped_by_many_to_many_property_map_key_parents")
	public static class MappedByManyToManyPropertyMapKeyParent {
		@Id
		private Integer id;
		private String code;
		@ManyToMany(mappedBy = "parents")
		private Map<String, MappedByManyToManyPropertyMapKeyOwner> owners;
	}

	@Entity(name="MappedByManyToManyPropertyMapKeyOwner")
	@Table(name="mapped_by_many_to_many_property_map_key_owners")
	public static class MappedByManyToManyPropertyMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey(name = "code")
		private Map<String, MappedByManyToManyPropertyMapKeyParent> parents;
	}

	@Entity(name="MappedByManyToManyEntityMapKeyParent")
	@Table(name="mapped_by_many_to_many_entity_map_key_parents")
	public static class MappedByManyToManyEntityMapKeyParent {
		@Id
		private Integer id;
		@ManyToMany(mappedBy = "parents")
		private Map<Child, MappedByManyToManyEntityMapKeyOwner> owners;
	}

	@Entity(name="MappedByManyToManyEntityMapKeyOwner")
	@Table(name="mapped_by_many_to_many_entity_map_key_owners")
	public static class MappedByManyToManyEntityMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_entity_key_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKeyJoinColumn(name = "child_key_id", referencedColumnName = "id")
		private Map<Child, MappedByManyToManyEntityMapKeyParent> parents;
	}

	@Entity(name="MappedByManyToManyImplicitEntityMapKeyParent")
	@Table(name="mapped_by_many_to_many_implicit_entity_map_key_parents")
	public static class MappedByManyToManyImplicitEntityMapKeyParent {
		@Id
		private Integer id;
		@ManyToMany(mappedBy = "parents")
		private Map<Child, MappedByManyToManyImplicitEntityMapKeyOwner> owners;
	}

	@Entity(name="MappedByManyToManyImplicitEntityMapKeyOwner")
	@Table(name="mapped_by_many_to_many_implicit_entity_map_key_owners")
	public static class MappedByManyToManyImplicitEntityMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_implicit_entity_key_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Map<Child, MappedByManyToManyImplicitEntityMapKeyParent> parents;
	}

	@Entity(name="MappedByManyToManyToOnePropertyMapKeyParent")
	@Table(name="mapped_by_many_to_many_to_one_property_map_key_parents")
	public static class MappedByManyToManyToOnePropertyMapKeyParent {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "map_key_child_id", referencedColumnName = "id")
		private Child mapKeyChild;
		@ManyToMany(mappedBy = "parents")
		private Map<Child, MappedByManyToManyToOnePropertyMapKeyOwner> owners;
	}

	@Entity(name="MappedByManyToManyToOnePropertyMapKeyOwner")
	@Table(name="mapped_by_many_to_many_to_one_property_map_key_owners")
	public static class MappedByManyToManyToOnePropertyMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_to_one_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey(name = "mapKeyChild")
		private Map<Child, MappedByManyToManyToOnePropertyMapKeyParent> parents;
	}

	@Entity(name="MappedByOneToManyParent")
	@Table(name="mapped_by_one_to_many_parents")
	public static class MappedByOneToManyParent {
		@Id
		private Integer id;
		@OneToMany(mappedBy = "parent")
		private Set<MappedByChild> children;
	}

	@Entity(name="CascadeOneToManyParent")
	@Table(name="cascade_one_to_many_parents")
	public static class CascadeOneToManyParent {
		@Id
		private Integer id;
		@OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
		private Set<CascadeOneToManyChild> children;
	}

	@Entity(name="CascadeOneToManyChild")
	@Table(name="cascade_one_to_many_children")
	public static class CascadeOneToManyChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private CascadeOneToManyParent parent;
	}

	@Entity(name="MappedByChild")
	@Table(name="mapped_by_children")
	public static class MappedByChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToManyParent parent;
	}

	@Entity(name="MappedByOneToManyMapParent")
	@Table(name="mapped_by_one_to_many_map_parents")
	public static class MappedByOneToManyMapParent {
		@Id
		private Integer id;
		@OneToMany(mappedBy = "parent")
		@MapKey(name = "code")
		private Map<String, MappedByMapChild> children;
	}

	@Entity(name="MappedByMapChild")
	@Table(name="mapped_by_map_children")
	public static class MappedByMapChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToManyMapParent parent;
		private String code;
	}

	@Entity(name="MappedByOneToManyMapWithoutMapKeyParent")
	@Table(name="mapped_by_one_to_many_map_without_map_key_parents")
	public static class MappedByOneToManyMapWithoutMapKeyParent {
		@Id
		private Integer id;
		@OneToMany(mappedBy = "parent")
		private Map<String, MappedByMapWithoutMapKeyChild> children;
	}

	@Entity(name="MappedByMapWithoutMapKeyChild")
	@Table(name="mapped_by_map_without_map_key_children")
	public static class MappedByMapWithoutMapKeyChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToManyMapWithoutMapKeyParent parent;
	}

	@Entity(name="MappedByOneToManyMapImplicitMapKeyParent")
	@Table(name="mapped_by_one_to_many_map_implicit_map_key_parents")
	public static class MappedByOneToManyMapImplicitMapKeyParent {
		@Id
		private Integer id;
		@OneToMany(mappedBy = "parent")
		@MapKey
		private Map<Integer, MappedByMapImplicitMapKeyChild> children;
	}

	@Entity(name="MappedByMapImplicitMapKeyChild")
	@Table(name="mapped_by_map_implicit_map_key_children")
	public static class MappedByMapImplicitMapKeyChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToManyMapImplicitMapKeyParent parent;
	}

	@Entity(name="CompositeMappedByOneToManyParent")
	@Table(name="composite_mapped_by_one_to_many_parents")
	public static class CompositeMappedByOneToManyParent {
		@EmbeddedId
		private Pk id;
		@OneToMany(mappedBy = "parent")
		private Set<CompositeMappedByChild> children;
	}

	@Entity(name="CompositeMappedByChild")
	@Table(name="composite_mapped_by_children")
	public static class CompositeMappedByChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
				@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
		})
		private CompositeMappedByOneToManyParent parent;
	}

	@Entity(name="OneToManyJoinTableOwner")
	@Table(name="one_to_many_join_table_owners")
	public static class OneToManyJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		private Set<Child> children;
	}

	@Entity(name="OneToManyImplicitJoinTableOwner")
	@Table(name="one_to_many_implicit_join_table_owners")
	public static class OneToManyImplicitJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		private Set<Child> children;
	}

	@Entity(name="OneToManyMapJoinTableOwner")
	@Table(name="one_to_many_map_join_table_owners")
	public static class OneToManyMapJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "child_key")
		private Map<String, Child> children;
	}

	@Entity(name="OneToManyPropertyMapKeyOwner")
	@Table(name="one_to_many_property_map_key_owners")
	public static class OneToManyPropertyMapKeyOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		@MapKey(name = "id")
		private Map<Integer, Child> children;
	}

	@Entity(name="OneToManyImplicitMapKeyOwner")
	@Table(name="one_to_many_implicit_map_key_owners")
	public static class OneToManyImplicitMapKeyOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_implicit_key_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		@MapKey
		private Map<Integer, Child> children;
	}

	@Entity(name="CompositeOwnerManyToManyOwner")
	@Table(name="composite_owner_many_to_many_owners")
	public static class CompositeOwnerManyToManyOwner {
		@EmbeddedId
		private Pk id;
		@ManyToMany
		@JoinTable(
				name = "composite_owner_parent_sets",
				joinColumns = {
						@JoinColumn(name = "owner_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_fk1", referencedColumnName = "id1")
				},
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Set<Parent> parents;
	}

	@Entity(name="CompositeTargetManyToManyOwner")
	@Table(name="composite_target_many_to_many_owners")
	public static class CompositeTargetManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_composite_parent_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
				}
		)
		private Set<CompositeParent> parents;
	}

	@Entity(name="CompositeOwnerOneToManyJoinTableOwner")
	@Table(name="composite_owner_one_to_many_join_table_owners")
	public static class CompositeOwnerOneToManyJoinTableOwner {
		@EmbeddedId
		private Pk id;
		@OneToMany
		@JoinTable(
				name = "composite_owner_child_links",
				joinColumns = {
						@JoinColumn(name = "owner_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_fk1", referencedColumnName = "id1")
				},
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		private Set<Child> children;
	}

	@Entity(name="CompositeTargetOneToManyJoinTableOwner")
	@Table(name="composite_target_one_to_many_join_table_owners")
	public static class CompositeTargetOneToManyJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_composite_child_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "child_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "child_fk1", referencedColumnName = "id1")
				}
		)
		private Set<CompositeChild> children;
	}

	@Entity(name="JoinTableCompositeManyToOneOwner")
	@Table(name="join_table_composite_many_to_one_owners")
	public static class JoinTableCompositeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				name = "owner_composite_parent_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
				}
		)
		private CompositeParent parent;
	}

	@Entity(name="CompositeOwnerJoinTableManyToOneOwner")
	@Table(name="composite_owner_join_table_many_to_one_owners")
	public static class CompositeOwnerJoinTableManyToOneOwner {
		@EmbeddedId
		private Pk id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				name = "composite_owner_parent_links",
				joinColumns = {
						@JoinColumn(name = "owner_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_fk1", referencedColumnName = "id1")
				},
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Parent parent;
	}

	private static CollectionValueIntent collectionIntent(
			BootBindingModel bootBindingModel,
			Class<?> javaType,
			String attributeName) {
		final AttributeBindingView attributeBindingView = attribute(
				managedTypeBinding( bootBindingModel, javaType ),
				attributeName
		);
		assertThat( attributeBindingView.valueIntent() ).isSameAs( attributeBindingView.collectionValueIntent() );
		assertThat( attributeBindingView.collectionValueIntent() ).isNotNull();
		return attributeBindingView.collectionValueIntent();
	}

	private static ManagedTypeBinding managedTypeBinding(BootBindingModel bootBindingModel, Class<?> javaType) {
		for ( ManagedTypeBinding managedTypeBinding : bootBindingModel.managedTypeBindings() ) {
			if ( managedTypeBinding.classDetails().toJavaClass().equals( javaType ) ) {
				return managedTypeBinding;
			}
		}
		throw new AssertionError( "Could not locate managed type binding " + javaType.getName() );
	}

	private static AttributeBindingView attribute(ManagedTypeBinding managedTypeBinding, String name) {
		for ( var attributeUsage : managedTypeBinding.attributeUsages() ) {
			if ( attributeUsage.attributeName().equals( name ) ) {
				return new AttributeBindingView( attributeUsage );
			}
		}
		throw new AssertionError( "Could not locate attribute binding " + name );
	}

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
