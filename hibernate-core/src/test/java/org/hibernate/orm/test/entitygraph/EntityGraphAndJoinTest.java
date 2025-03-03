/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EntityGraphAndJoinTest.Person.class,
		EntityGraphAndJoinTest.Address.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17629" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18378" )
public class EntityGraphAndJoinTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Address a1 = new Address( 1L, "test" );
			final Address a2 = new Address( 2L, "test" );
			session.persist( a1 );
			session.persist( a2 );
			session.persist( new Person( "Marco", a1 ) );
			session.persist( new Person( "Andrea", a2 ) );
		} );
	}

	@Test
	public void testHqlJoin(SessionFactoryScope scope) {
		executeQuery( scope, false, false, false );
	}

	@Test
	public void testHqlLeftJoin(SessionFactoryScope scope) {
		executeQuery( scope, false, true, false );
	}

	@Test
	public void testCriteriaJoin(SessionFactoryScope scope) {
		executeQuery( scope, true, false, false );
	}

	@Test
	public void testCriteriaLeftJoin(SessionFactoryScope scope) {
		executeQuery( scope, true, true, false );
	}

	@Test
	public void testHqlJoinWhere(SessionFactoryScope scope) {
		executeQuery( scope, false, false, true );
	}

	@Test
	public void testCriteriaLeftJoinWhere(SessionFactoryScope scope) {
		executeQuery( scope, true, true, true );
	}

	private void executeQuery(SessionFactoryScope scope, boolean criteria, boolean leftJoin, boolean where) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final TypedQuery<Person> query;
			if ( criteria ) {
				final CriteriaBuilder cb = session.getCriteriaBuilder();
				final CriteriaQuery<Person> cq = cb.createQuery( Person.class );
				final Root<Person> root = cq.from( Person.class );
				final Join<Person, Address> join = root.join( "address", leftJoin ? JoinType.LEFT : JoinType.INNER );
				if ( where ) {
					cq.where( cb.equal( join.get( "description" ), "test" ) );
				}
				query = session.createQuery( cq.distinct( true ) );
			}
			else {
				query = session.createQuery( String.format(
						"select distinct p from Person p %s p.address a %s",
						leftJoin ? "left join" : "join",
						where ? "where a.description = 'test'" : ""
				), Person.class );
			}
			final EntityGraph<?> entityGraph = session.getEntityGraph( "test-graph" );
			final List<Person> resultList = query.setHint( HINT_SPEC_FETCH_GRAPH, entityGraph ).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( p -> p.getAddress().getId() ) ).containsExactly( 1L, 2L );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQuery( 0, "join", where ? 2 : 1 );
		} );
	}

	@Entity( name = "Address" )
	public static class Address {
		@Id
		private Long id;

		private String description;

		public Address() {
		}

		public Address(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}
	}

	@Entity( name = "Person" )
	@NamedEntityGraph( name = "test-graph", attributeNodes = {
			@NamedAttributeNode( "address" ),
	} )
	public static class Person {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "address_id" )
		private Address address;

		public Person() {
		}

		public Person(String name, Address address) {
			this.name = name;
			this.address = address;
		}

		public Address getAddress() {
			return address;
		}
	}
}
