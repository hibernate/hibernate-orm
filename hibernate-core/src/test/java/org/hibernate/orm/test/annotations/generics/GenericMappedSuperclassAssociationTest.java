/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.tree.domain.SqmPath;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		GenericMappedSuperclassAssociationTest.Parent.class,
		GenericMappedSuperclassAssociationTest.ParentA.class,
		GenericMappedSuperclassAssociationTest.ParentB.class,
		GenericMappedSuperclassAssociationTest.Child.class,
		GenericMappedSuperclassAssociationTest.ChildA.class,
		GenericMappedSuperclassAssociationTest.ChildB.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17405" )
public class GenericMappedSuperclassAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentA parentA = new ParentA( 1L, "parent_a" );
			session.persist( parentA );
			session.persist( new ChildA( "child_a_1", parentA ) );
			session.persist( new ChildA( "child_a_2", parentA ) );
			final ParentB parentB = new ParentB( 2L, "parent_b" );
			session.persist( parentB );
			session.persist( new ChildB( "child_b", parentB ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( String.format( "delete from %s", Child.class.getName() ) ).executeUpdate();
			session.createMutationQuery( String.format( "delete from %s", Parent.class.getName() ) ).executeUpdate();
		} );
	}

	@Test
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<ChildA> resultList = session.createQuery(
					"select c from ChildA c where c.parent.id = 1",
					ChildA.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			resultList.forEach( c -> assertThat( c.getParent().getName() ).isEqualTo( "parent_a" ) );
		} );
	}

	@Test
	public void testCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<ChildB> cq = cb.createQuery( ChildB.class );
			final Root<ChildB> from = cq.from( ChildB.class );
			final Path<Object> parent = from.get( "parent" );
			assertThat( parent.getModel().getBindableJavaType() ).isEqualTo( Parent.class );
			assertThat( ( (SqmPath<?>) parent ).getResolvedModel().getBindableJavaType() ).isEqualTo( ParentB.class );
			cq.select( from ).where( cb.equal( from.get( "parent" ).get( "id" ), 2L ) );
			final ChildB result = session.createQuery( cq ).getSingleResult();
			assertThat( result.getParent().getName() ).isEqualTo( "parent_b" );
		} );
	}

	@MappedSuperclass
	public static abstract class Child<P extends Parent<?>> {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private P parent;

		public Child() {
		}

		public Child(String name, P parent) {
			this.name = name;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public P getParent() {
			return parent;
		}
	}

	@MappedSuperclass
	public static abstract class Parent<C extends Child<?>> {
		private String name;

		@OneToMany( mappedBy = "parent" )
		private List<C> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(String name) {
			this.name = name;
		}

		public abstract Long getId();

		public String getName() {
			return name;
		}

		public List<C> getChildren() {
			return children;
		}
	}

	@Entity( name = "ChildA" )
	public static class ChildA extends Child<ParentA> {
		public ChildA() {
		}

		public ChildA(String name, ParentA parent) {
			super( name, parent );
		}
	}

	@Entity( name = "ChildB" )
	public static class ChildB extends Child<ParentB> {
		public ChildB() {
		}

		public ChildB(String name, ParentB parent) {
			super( name, parent );
		}
	}

	@Entity( name = "ParentA" )
	public static class ParentA extends Parent<ChildA> {
		@Id
		private Long id;

		public ParentA() {
		}

		public ParentA(Long id, String name) {
			super( name );
			this.id = id;
		}

		@Override
		public Long getId() {
			return id;
		}
	}

	@Entity( name = "ParentB" )
	public static class ParentB extends Parent<ChildB> {
		@Id
		private Long id;

		public ParentB() {
		}

		public ParentB(Long id, String name) {
			super( name );
			this.id = id;
		}

		@Override
		public Long getId() {
			return id;
		}
	}
}
