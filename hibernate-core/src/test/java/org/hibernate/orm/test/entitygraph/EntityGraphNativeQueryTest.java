/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphNativeQueryTest.Foo.class,
				EntityGraphNativeQueryTest.Bar.class,
				EntityGraphNativeQueryTest.Baz.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-12476" )
public class EntityGraphNativeQueryTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					session.persist( bar );

					Baz baz = new Baz();
					session.persist( baz );

					Foo foo = new Foo();
					foo.bar = bar;
					foo.baz = baz;
					session.persist( foo );
				}
		);
	}

	@Test
	void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
					fooGraph.addAttributeNodes( "bar", "baz" );

					Foo foo = em.createQuery( "select f from Foo f", Foo.class )
							.setHint( GraphSemantic.LOAD.getJpaHintName(), fooGraph )
							.getSingleResult();
					assertThat( foo.bar, notNullValue() );
					assertThat( foo.baz, notNullValue() );
				}
		);
	}

	@Test
	void testNativeQueryLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
					fooGraph.addAttributeNodes( "bar", "baz" );

					try {
						em.createNativeQuery(
								"select " +
										"	f.id as id, " +
										"	f.bar_id as bar_id, " +
										"	f.baz_id as baz_id " +
										"from Foo f", Foo.class )
								.setHint( GraphSemantic.LOAD.getJpaHintName(), fooGraph )
								.getSingleResult();
						fail("Should throw exception");
					}
					catch (Exception e) {
						if ( e.getMessage().equals( "A native SQL query cannot use EntityGraphs" ) ) {
							// success
						}
						else {
							throw new RuntimeException( "Unexpected exception", e );
						}
					}
				}
		);
	}

	@Test
	void testNativeQueryFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo> fooGraph = em.createEntityGraph( Foo.class );
					fooGraph.addAttributeNodes( "bar", "baz" );

					try {
						em.createNativeQuery(
								"select " +
										"	f.id as id, " +
										"	f.bar_id as bar_id, " +
										"	f.baz_id as baz_id " +
										"from Foo f", Foo.class )
								.setHint( GraphSemantic.FETCH.getJpaHintName(), fooGraph )
								.getSingleResult();
						fail( "Should throw exception" );
					}
					catch (Exception e) {
						if ( e.getMessage().equals( "A native SQL query cannot use EntityGraphs" ) ) {
							// success
						}
						else {
							throw new RuntimeException( "Unexpected exception", e );
						}
					}
				}
		);
	}

	@Entity(name = "Foo")
	public static class Foo {

		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Bar bar;

		@ManyToOne(fetch = FetchType.LAZY)
		public Baz baz;
	}

	@Entity(name = "Bar")
	public static class Bar {

		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(mappedBy = "bar")
		public Set<Foo> foos = new HashSet<>();
	}

	@Entity(name = "Baz")
	public static class Baz {

		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(mappedBy = "baz")
		public Set<Foo> foos = new HashSet<>();

	}

}
