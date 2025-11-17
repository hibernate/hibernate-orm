/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {NewGraphTest.class, NewGraphTest.E.class, NewGraphTest.F.class, NewGraphTest.G.class, NewGraphTest.H.class,
				NewGraphTest.A.class, NewGraphTest.Aa.class, NewGraphTest.B.class})
public class NewGraphTest {

	@Test void testByIdEntityGraph(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession( s -> s.byId(F.class).load(1) );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.byId(F.class).withFetchGraph(graph).load(1);
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.byId(E.class).load(1) );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.byId(E.class).withFetchGraph(graph).load(1);
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testFind(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession( s -> s.find(F.class, 1) );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.find(graph, 1);
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.find(E.class, 1) );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.find(graph, 1);
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testGet(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		SessionFactoryImplementor factory = scope.getSessionFactory();

		F f = factory.fromStatelessSession( s -> s.get(F.class, 1L) );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = factory.fromStatelessSession( s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.get(graph, 1L);
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = factory.fromStatelessSession( s -> s.get(E.class, 1L) );
		assertFalse( isInitialized( e.f ) );
		E ee = factory.fromStatelessSession( s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.get(graph, 1L);
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testBySimpleNaturalIdEntityGraph(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			e.s = "3";
			f.s = "3";
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession( s -> s.bySimpleNaturalId(F.class).load("3") );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.bySimpleNaturalId(F.class).withFetchGraph(graph).load("3");
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.bySimpleNaturalId(E.class).load("3") );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.bySimpleNaturalId(E.class).withFetchGraph(graph).load("3");
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testNaturalIdEntityGraph(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			e.s = "4";
			f.s = "4";
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession( s -> s.byNaturalId(F.class).using("s", "4").load() );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.byNaturalId(F.class).withFetchGraph(graph).using("s", "4").load();
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.byNaturalId(E.class).using("s", "4").load() );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.byNaturalId(E.class).withFetchGraph(graph).using("s", "4").load();
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession(s ->
				s.createSelectionQuery("from F where id = 1", F.class)
						.getSingleResult());
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession(s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.createSelectionQuery("from F where id = 1", F.class)
					.setEntityGraph(graph, GraphSemantic.FETCH)
					.getSingleResult();
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession(s ->
				s.createSelectionQuery("from E where id = 1", E.class)
						.getSingleResult());
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession(s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.createSelectionQuery("from E where id = 1", E.class)
					.setEntityGraph(graph, GraphSemantic.LOAD)
					.getSingleResult();
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testSelectionQuery(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession(s ->
				s.createSelectionQuery("from F where id = 1", F.class)
						.getSingleResult());
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession(s -> {
			RootGraph<F> graph = s.createEntityGraph(F.class);
			graph.addAttributeNodes("g", "es");
			return s.createSelectionQuery("from F where id = 1", graph)
					.getSingleResult();
		});
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession(s ->
				s.createSelectionQuery("from E where id = 1", E.class)
						.getSingleResult());
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession(s -> {
			RootGraph<E> graph = s.createEntityGraph(E.class);
			graph.addAttributeNodes("f");
			return s.createSelectionQuery("from E where id = 1", graph)
					.getSingleResult();
		});
		assertTrue( isInitialized( ee.f ) );
	}

	@Test
	void subTypeEntityGraph(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			A a = new A();
			Aa aa = new Aa();
			B b1 = new B();
			B b2 = new B();
			B b3 = new B();
			b1.a = a;
			a.bs = new HashSet<>();
			a.bs.add( b1 );

			b2.a = aa;
			aa.bs = new HashSet<>();
			aa.bs.add( b2 );

			b3.a = aa;
			aa.bss = new HashSet<>();
			aa.bss.add( b3 );

			s.persist( a );
			s.persist( aa );
			s.persist( b1 );
			s.persist( b2 );
			s.persist( b3 );
		} );

		A a = scope.fromSession( s ->
				s.createSelectionQuery( "from A", A.class )
						.setMaxResults( 1 )
						.getSingleResult() );
		assertFalse( isInitialized( a.bs ) );

		List<Aa> as = scope.fromSession( s -> {
			SelectionQuery<Aa> query = s.createSelectionQuery( "from Aa", Aa.class );
			RootGraph<A> graph = s.createEntityGraph(A.class);
			graph.addAttributeNodes("bs");
			return query
					.setEntityGraph( graph, GraphSemantic.FETCH )
					.getResultList();
		} );
		for ( Aa el : as ) {
			assertTrue( isInitialized( el.bs ) );
			assertFalse( isInitialized( el.bss ) );
		}

		as = scope.fromSession( s -> {
			SelectionQuery<Aa> query = s.createSelectionQuery( "from Aa", Aa.class );
			RootGraph<Aa> graph = s.createEntityGraph(Aa.class);
			graph.addAttributeNodes("bs", "bss");
			return query
					.setEntityGraph( graph, GraphSemantic.FETCH )
					.getResultList();
		} );
		for ( Aa el : as ) {
			assertTrue( isInitialized( el.bs ) );
			assertTrue( isInitialized( el.bss ) );
		}
	}

	@Entity(name = "E")
	static class E {
		@Id @GeneratedValue
		Long id;
		@ManyToOne(fetch = LAZY)
		F f;
		@NaturalId String s;
	}

	@Entity(name = "F")
	static class F {
		@Id @GeneratedValue
		Long id;
		@ManyToOne(fetch = LAZY)
		G g;
		@OneToMany(mappedBy = "f")
		Set<E> es;
		@NaturalId String s;
	}
	@Entity(name = "G")
	static class G {
		@Id @GeneratedValue
		Long id;
	}

	@Entity(name = "H")
	static class H {
		@Id @GeneratedValue
		Long id;
		@ManyToOne G g;
	}

	@Entity(name = "A")
	static class A {
		@Id @GeneratedValue
		Long id;

		@OneToMany(mappedBy = "a")
		Set<B> bs;
	}

	@Entity(name = "Aa")
	static class Aa extends A {
		@Id @GeneratedValue
		Long id;

		@OneToMany(mappedBy = "aa")
		Set<B> bss;
	}

	@Entity(name = "B")
	static class B {
		@Id @GeneratedValue
		Long id;

		@ManyToOne(fetch = LAZY)
		A a;

		@ManyToOne(fetch = LAZY)
		Aa aa;
	}
}
