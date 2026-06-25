/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Collection;
import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that Java {@link List} mappings use list semantics by default.
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		ImplicitListDefaultSemanticsTests.AnEntity.class,
		ImplicitListDefaultSemanticsTests.ChildEntity.class
} )
public class ImplicitListDefaultSemanticsTests {
	@Test
	void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( AnEntity.class, (descriptor) -> {
			final Property implicitList = descriptor.getProperty( "implicitList" );
			assertThat( implicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );

			final Property implicitBag = descriptor.getProperty( "implicitBag" );
			assertThat( implicitBag.getValue() ).isInstanceOf( Bag.class );

			final Property explicitBag = descriptor.getProperty( "explicitBag" );
			assertThat( explicitBag.getValue() ).isInstanceOf( Bag.class );

			final Property explicitList = descriptor.getProperty( "explicitList" );
			assertThat( explicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );

			final Property orderByList = descriptor.getProperty( "orderByList" );
			assertThat( orderByList.getValue() ).isInstanceOf( Bag.class );

			final Property sqlOrderList = descriptor.getProperty( "sqlOrderList" );
			assertThat( sqlOrderList.getValue() ).isInstanceOf( Bag.class );

			final Property inverseOneToManyList = descriptor.getProperty( "inverseOneToManyList" );
			assertThat( inverseOneToManyList.getValue() ).isInstanceOf( Bag.class );

			final Property inverseIndexedOneToManyList = descriptor.getProperty( "inverseIndexedOneToManyList" );
			assertThat( inverseIndexedOneToManyList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
		} );
	}

	@Test
	@ServiceRegistry
	void orderColumnMayNotBeCombinedWithOrderBy(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( InvalidOrderedListEntity.class )
				.buildMetadata() )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( InvalidOrderedListEntity.class.getName() + ".invalidList" )
				.hasMessageContaining( "@OrderColumn" )
				.hasMessageContaining( "@OrderBy" );
	}

	@Entity( name = "AnEntity" )
	@Table( name = "t_entity" )
	public static class AnEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		@ElementCollection
		private List<String> implicitList;

		@ElementCollection
		private Collection<String> implicitBag;

		@ElementCollection
		@org.hibernate.annotations.Bag
		private List<String> explicitBag;

		@ElementCollection
		@OrderColumn( name = "explicit_list_position" )
		private List<String> explicitList;

		@ElementCollection
		@OrderBy
		private List<String> orderByList;

		@ElementCollection
		@org.hibernate.annotations.SQLOrder( "value desc" )
		private List<String> sqlOrderList;

		@OneToMany( mappedBy = "owner" )
		private List<ChildEntity> inverseOneToManyList;

		@OneToMany( mappedBy = "orderedOwner" )
		@OrderColumn
		private List<ChildEntity> inverseIndexedOneToManyList;

		private AnEntity() {
			// for use by Hibernate
		}

		public AnEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "ChildEntity" )
	@Table( name = "t_child_entity" )
	public static class ChildEntity {
		@Id
		private Integer id;

		@ManyToOne
		private AnEntity owner;

		@ManyToOne
		private AnEntity orderedOwner;
	}

	@Entity( name = "InvalidOrderedListEntity" )
	@Table( name = "invalid_ordered_list_entity" )
	public static class InvalidOrderedListEntity {
		@Id
		private Integer id;

		@ElementCollection
		@OrderBy
		@OrderColumn
		private List<String> invalidList;
	}
}
