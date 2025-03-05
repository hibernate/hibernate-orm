/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.sql.ast.SqlAstJoinType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-16960" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17818" )
@DomainModel( annotatedClasses = {
		LazyOptionalOneToOneConcreteProxyTest.Person.class,
		LazyOptionalOneToOneConcreteProxyTest.PersonContact.class,
		LazyOptionalOneToOneConcreteProxyTest.BusinessContact.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class LazyOptionalOneToOneConcreteProxyTest {
	@Test
	public void testFindChildrenContact(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Person person = session.find(
					Person.class,
					1L,
					Map.of( HINT_SPEC_FETCH_GRAPH, session.getEntityGraph( "Person.children" ) )
			);
			assertThat( person.getPersonContact() ).isNull();
			assertThat( person.getChildren() ).matches( Hibernate::isInitialized )
					.hasSize( 1 )
					.element( 0 )
					.extracting( Person::getPersonContact )
					.isInstanceOf( PersonContact.class )
					.matches( contact -> !Hibernate.isInitialized( contact ) );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 3 );
		} );
	}

	@Test
	public void testFindParentContact(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Person person = session.find(
					Person.class,
					3L,
					Map.of( HINT_SPEC_FETCH_GRAPH, session.getEntityGraph( "Person.children" ) )
			);
			assertThat( person.getPersonContact() )
					.isInstanceOf( BusinessContact.class )
					.matches( contact -> !Hibernate.isInitialized( contact ) );
			assertThat( person.getChildren() ).matches( Hibernate::isInitialized )
					.hasSize( 2 )
					.extracting( Person::getPersonContact )
					.containsOnlyNulls();
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 3 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person parent1 = new Person( 1L, "parent1" );
			final Person child1 = new Person( 2L, "child1" );
			child1.setParent( parent1 );
			session.persist( parent1 );
			session.persist( child1 );
			session.persist( new PersonContact( 2L, child1 ) );
			final Person parent2 = new Person( 3L, "parent2" );
			final Person child2 = new Person( 4L, "child2" );
			final Person child3 = new Person( 5L, "child3" );
			child2.setParent( parent2 );
			child3.setParent( parent2 );
			session.persist( parent2 );
			session.persist( child2 );
			session.persist( child3 );
			session.persist( new BusinessContact( 3L, parent2, "business2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from PersonContact" ).executeUpdate();
			session.createQuery( "from Person", Person.class ).getResultList().forEach( p -> {
				p.setParent( null );
				session.remove( p );
			} );
		} );
	}

	@Entity( name = "Person" )
	@NamedEntityGraph( name = "Person.children", attributeNodes = @NamedAttributeNode( "children" ) )
	public static class Person {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Person parent;

		@OneToOne( mappedBy = "person" )
		private PersonContact personContact;

		@OneToMany( mappedBy = "parent" )
		private Set<Person> children = new HashSet<>();

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setParent(@Nullable Person parent) {
			this.parent = parent;
			if ( parent != null ) {
				parent.getChildren().add( this );
			}
		}

		public PersonContact getPersonContact() {
			return personContact;
		}

		public Set<Person> getChildren() {
			return children;
		}
	}

	@Entity( name = "PersonContact" )
	@ConcreteProxy
	public static class PersonContact {
		@Id
		private Long id;

		@OneToOne( optional = false, fetch = FetchType.LAZY )
		@MapsId
		private Person person;

		public PersonContact() {
		}

		public PersonContact(Long id, Person person) {
			this.id = id;
			this.person = person;
		}
	}

	@Entity( name = "BusinessContact" )
	public static class BusinessContact extends PersonContact {
		private String business;

		public BusinessContact() {
		}

		public BusinessContact(Long id, Person person, String business) {
			super( id, person );
			this.business = business;
		}
	}
}
