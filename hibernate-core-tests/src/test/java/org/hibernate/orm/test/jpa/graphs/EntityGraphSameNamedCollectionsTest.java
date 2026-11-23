package org.hibernate.orm.test.jpa.graphs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A graph attribute must not fetch a same-named collection on a different entity.
///
/// @author Steve Ebersole
@JiraKey( "HHH-14084" )
@Jpa( annotatedClasses = {
		EntityGraphSameNamedCollectionsTest.Parent.class,
		EntityGraphSameNamedCollectionsTest.Child.class,
		EntityGraphSameNamedCollectionsTest.ParentToy.class,
		EntityGraphSameNamedCollectionsTest.ChildToy.class
} )
public class EntityGraphSameNamedCollectionsTest {
	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ParentToy parentToy = new ParentToy();
			parentToy.id = 1L;
			entityManager.persist( parentToy );
			final ChildToy childToy = new ChildToy();
			childToy.id = 2L;
			entityManager.persist( childToy );
			final Child child = new Child();
			child.id = 3L;
			child.toys.add( childToy );
			entityManager.persist( child );
			final Parent parent = new Parent();
			parent.id = 4L;
			parent.child = child;
			parent.toys.add( parentToy );
			entityManager.persist( parent );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@EnumSource( GraphSemantic.class )
	public void testGraphDoesNotFetchChildToys(GraphSemantic semantic, EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Parent parent = entityManager.find(
					Parent.class,
					4L,
					Map.of( semantic.getJakartaHintName(), entityManager.getEntityGraph( "parentWithToysAndChild" ) )
			);
			assertTrue( Hibernate.isInitialized( parent.toys ) );
			assertTrue( Hibernate.isInitialized( parent.child ) );
			assertFalse( Hibernate.isInitialized( parent.child.toys ) );
			assertEquals( List.of( 1L ), parent.toys.stream().map( toy -> toy.id ).toList() );
		} );
	}

	@Entity( name = "Parent" )
	@Table( name = "hhh14084_parent" )
	@NamedEntityGraph( name = "parentWithToysAndChild", attributeNodes = {
			@NamedAttributeNode( "toys" ),
			@NamedAttributeNode( "child" )
	} )
	public static class Parent {
		@Id
		private Long id;

		@OneToOne( fetch = FetchType.LAZY )
		private Child child;

		@OneToMany( fetch = FetchType.LAZY )
		private List<ParentToy> toys = new ArrayList<>();
	}

	@Entity( name = "Child" )
	@Table( name = "hhh14084_child" )
	public static class Child {
		@Id
		private Long id;

		@OneToMany( fetch = FetchType.LAZY )
		private List<ChildToy> toys = new ArrayList<>();
	}

	@Entity( name = "ParentToy" )
	@Table( name = "hhh14084_parent_toy" )
	public static class ParentToy {
		@Id
		private Long id;
	}

	@Entity( name = "ChildToy" )
	@Table( name = "hhh14084_child_toy" )
	public static class ChildToy {
		@Id
		private Long id;
	}
}
