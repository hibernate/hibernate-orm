/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Subgraph;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.util.MutableLong;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;

/**
 * @author Oliver Breidenbach
 */
@DomainModel(
		annotatedClasses = {
				InheritedEntityGraphTest.Foo2.class,
				InheritedEntityGraphTest.Foo.class,
				InheritedEntityGraphTest.Bar.class
		}
)
@SessionFactory
public class InheritedEntityGraphTest {

	@Test
	@JiraKey(value = "HHH-10261")
	void singleAttributeNodeInheritanceTest(SessionFactoryScope scope) {

		MutableLong fooId = new MutableLong();

		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					session.persist(bar);

					Foo foo = new Foo();
					foo.bar = bar;
					session.persist( foo );

					fooId.set( foo.id );
				}
		);

		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo> entityGraph = em.createEntityGraph( Foo.class );
					entityGraph.addSubgraph( "bar" );

					Map<String, Object> properties = Collections.singletonMap(
							GraphSemantic.LOAD.getJpaHintName(), entityGraph
					);

					Foo result = em.find( Foo.class, fooId.get(), properties );

					assertThat( result, isInitialized() );
					assertThat( result.bar, isInitialized() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10261")
	void collectionAttributeNodeInheritanceTest(SessionFactoryScope scope) {

		MutableLong fooId = new MutableLong();

		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					session.persist(bar);

					Foo foo = new Foo();
					foo.bar = bar;
					session.persist( foo );

					fooId.set( foo.id );
				}
		);

		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo> entityGraph = em.createEntityGraph( Foo.class );
					entityGraph.addSubgraph( "bars" );

					Map<String, Object> properties = Collections.singletonMap(
							GraphSemantic.LOAD.getJpaHintName(), entityGraph
					);

					Foo result = em.find( Foo.class, fooId.get(), properties );

					assertThat( result, isInitialized() );
					assertThat( result.bars, isInitialized() );
				}
		);
	}


	@Test
	@JiraKey(value = "HHH-10261")
	void singleAttributeSubgraphInheritanceTest(SessionFactoryScope scope) {

		MutableLong foo2Id = new MutableLong();

		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					session.persist(bar);

					Foo foo = new Foo();
					foo.bar = bar;
					session.persist( foo );

					Foo2 foo2 = new Foo2();
					foo2.foo = foo;
					session.persist( foo2 );

					foo2Id.set( foo2.id );
				}
		);

		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo2> entityGraph = em.createEntityGraph( Foo2.class );
					Subgraph<Foo> subgraphFoo = entityGraph.addSubgraph( "foo" );
					subgraphFoo.addSubgraph( "bar" );

					Map<String, Object> properties = Collections.singletonMap(
							GraphSemantic.LOAD.getJpaHintName(), entityGraph
					);

					Foo2 result = em.find( Foo2.class, foo2Id.get(), properties );

					assertThat( result, isInitialized() );
					assertThat( result.foo, isInitialized() );
					assertThat( result.foo.bar, isInitialized() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10261")
	void collectionAttributeSubgraphInheritanceTest(SessionFactoryScope scope) {

		MutableLong foo2Id = new MutableLong();

		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					session.persist(bar);

					Foo foo = new Foo();
					foo.bar = bar;
					session.persist( foo );

					Foo2 foo2 = new Foo2();
					foo2.foo = foo;
					session.persist( foo2 );

					foo2Id.set( foo2.id );
				}
		);

		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph<Foo2> entityGraph = em.createEntityGraph( Foo2.class );
					Subgraph<Foo> subgraphFoo = entityGraph.addSubgraph( "foo" );
					subgraphFoo.addSubgraph( "bars" );

					Map<String, Object> properties = Collections.singletonMap(
							GraphSemantic.LOAD.getJpaHintName(), entityGraph
					);

					Foo2 result = em.find( Foo2.class, foo2Id.get(), properties );

					assertThat( result, isInitialized() );
					assertThat( result.foo, isInitialized() );
					assertThat( result.foo.bars, isInitialized() );
				}
		);
	}

	@MappedSuperclass
	public static class MappedSupperclass {
		@Id
		@GeneratedValue
		public long id;

		@OneToOne(fetch = FetchType.LAZY)
		public Bar bar;

		@OneToMany
		public Set<Bar> bars = new HashSet<Bar>();

	}

	@Entity(name = "Bar")
	public static class Bar {
		@Id @GeneratedValue
		public long id;

	}

	@Entity(name = "Foo")
	public static class Foo extends MappedSupperclass {

	}

	@Entity(name = "Foo2")
	public static class Foo2 {
		@Id @GeneratedValue
		public long id;

		@OneToOne(fetch = FetchType.LAZY)
		public Foo foo;

	}
}
