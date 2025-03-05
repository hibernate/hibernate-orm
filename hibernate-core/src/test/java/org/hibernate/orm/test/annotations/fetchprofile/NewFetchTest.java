/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.annotations.FetchMode.JOIN;
import static org.hibernate.annotations.FetchMode.SELECT;
import static org.hibernate.annotations.FetchMode.SUBSELECT;
import static org.hibernate.engine.profile.DefaultFetchProfile.HIBERNATE_DEFAULT_PROFILE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {NewFetchTest.class,NewFetchTest.E.class, NewFetchTest.F.class, NewFetchTest.G.class, NewFetchTest.H.class})
@FetchProfile(name = NewFetchTest.NEW_PROFILE)
@FetchProfile(name = NewFetchTest.OLD_PROFILE,
		fetchOverrides = @FetchProfile.FetchOverride(entity = NewFetchTest.E.class, association = "f"))
@FetchProfile(name = NewFetchTest.LAZY_SUBSELECT_PROFILE)
@FetchProfile(name = NewFetchTest.EAGER_SUBSELECT_PROFILE)
@FetchProfile(name = NewFetchTest.LAZY_SELECT_PROFILE)
@FetchProfile(name = NewFetchTest.EAGER_SELECT_PROFILE)
@FetchProfile(name = NewFetchTest.JOIN_PROFILE)
@FetchProfile(name = NewFetchTest.OLD_SUBSELECT_PROFILE,
		fetchOverrides = @FetchProfile.FetchOverride(entity = NewFetchTest.F.class, association = "es", fetch = LAZY, mode = SUBSELECT))
public class NewFetchTest {

	static final String NEW_PROFILE = "new-profile";
	static final String OLD_PROFILE = "old-profile";
	static final String LAZY_SUBSELECT_PROFILE = "lazy-subselect-profile";
	static final String EAGER_SUBSELECT_PROFILE = "eager-subselect-profile";
	static final String LAZY_SELECT_PROFILE = "lazy-select-profile";
	static final String EAGER_SELECT_PROFILE = "eager-subselect-profile";
	static final String JOIN_PROFILE = "join-profile";
	static final String OLD_SUBSELECT_PROFILE = "old-subselect-profile";

	@Test void test(SessionFactoryScope scope) {
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
			s.enableFetchProfile(NEW_PROFILE);
			return s.find(F.class, 1);
		} );
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.find(E.class, 1) );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			s.enableFetchProfile(OLD_PROFILE);
			return s.find(E.class, 1);
		} );
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testById(SessionFactoryScope scope) {
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
		F ff = scope.fromSession( s -> s.byId(F.class).enableFetchProfile(NEW_PROFILE).load(1) );
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.byId(E.class).load(1) );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> s.byId(E.class).enableFetchProfile(OLD_PROFILE).load(1) );
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testBySimpleNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			e.s = "1";
			f.s = "1";
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession( s -> s.bySimpleNaturalId(F.class).load("1") );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> s.bySimpleNaturalId(F.class).enableFetchProfile(NEW_PROFILE).load("1") );
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.bySimpleNaturalId(E.class).load("1") );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> s.bySimpleNaturalId(E.class).enableFetchProfile(OLD_PROFILE).load("1") );
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testByNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			e.s = "2";
			f.s = "2";
			s.persist(g);
			s.persist(f);
			s.persist(e);
		});

		F f = scope.fromSession( s -> s.byNaturalId(F.class).using("s", "2").load() );
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> s.byNaturalId(F.class).using("s", "2").enableFetchProfile(NEW_PROFILE).load() );
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.byNaturalId(E.class).using("s", "2").load() );
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> s.byNaturalId(E.class).using("s", "2").enableFetchProfile(OLD_PROFILE).load() );
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

		F f = scope.fromSession( s ->
				s.createSelectionQuery("from F where id = 1", F.class)
						.getSingleResult());
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s ->
				s.createSelectionQuery("from F where id = 1", F.class)
						.enableFetchProfile(NEW_PROFILE).getSingleResult());
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s ->
				s.createSelectionQuery("from E where id = 1", E.class)
						.getSingleResult());
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s ->
				s.createSelectionQuery("from E where id = 1", E.class)
						.enableFetchProfile(OLD_PROFILE)
						.getSingleResult());
		assertTrue( isInitialized( ee.f ) );
	}

	@Test void testStyles(SessionFactoryScope scope) {
		long id = scope.fromTransaction( s-> {
			G g = new G();
			F f = new F();
			E e = new E();
			f.g = g;
			e.f = f;
			s.persist(g);
			s.persist(f);
			s.persist(e);
			return f.id;
		});
		F f0 = scope.fromSession( s -> s.find(F.class, id));
		assertFalse(isInitialized(f0.es));
		F f1 = scope.fromSession( s -> {
			s.enableFetchProfile(LAZY_SELECT_PROFILE);
			return s.find(F.class, id);
		});
		assertFalse(isInitialized(f1.es));
		F f2 = scope.fromSession( s -> {
			s.enableFetchProfile(EAGER_SELECT_PROFILE);
			return s.find(F.class, id);
		});
		assertTrue(isInitialized(f2.es));
		F f3 = scope.fromSession( s -> {
			s.enableFetchProfile(JOIN_PROFILE);
			return s.find(F.class, id);
		});
		assertTrue(isInitialized(f3.es));
		F f4 = scope.fromSession( s -> {
			s.enableFetchProfile(LAZY_SUBSELECT_PROFILE);
			return s.find(F.class, id);
		});
		assertFalse(isInitialized(f4.es));
		F f5 = scope.fromSession( s -> {
			s.enableFetchProfile(EAGER_SUBSELECT_PROFILE);
			return s.find(F.class, id);
		});
		assertTrue(isInitialized(f5.es));
	}

	@Test void subselectTest(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			G g = new G();
			F f1 = new F();
			F f2 = new F();
			E e1 = new E();
			E e2 = new E();
			E e3 = new E();
			E e4 = new E();
			f1.g = g;
			f2.g = g;
			e1.f = f1;
			e2.f = f1;
			e3.f = f1;
			e4.f = f2;
			s.persist(g);
			s.persist(f1);
			s.persist(f2);
			s.persist(e1);
			s.persist(e2);
			s.persist(e3);
		});
		scope.inSession( s -> {
			List<F> fs = s.createSelectionQuery("from F", F.class).getResultList();
			F f0 = fs.get(0);
			F f1 = fs.get(1);
			assertFalse( isInitialized( f0.es ) );
			assertFalse( isInitialized( f1.es ) );
			f0.es.size();
			assertTrue( isInitialized( f0.es ) );
			assertFalse( isInitialized( f1.es ) );
		});
		scope.inSession( s -> {
			s.enableFetchProfile(LAZY_SUBSELECT_PROFILE);
			List<F> fs = s.createSelectionQuery("from F", F.class).getResultList();
			F f0 = fs.get(0);
			F f1 = fs.get(1);
			assertFalse( isInitialized( f0.es ) );
			assertFalse( isInitialized( f1.es ) );
			f0.es.size();
			assertTrue( isInitialized( f0.es ) );
			assertTrue( isInitialized( f1.es ) );
		});
		scope.inSession( s -> {
			s.enableFetchProfile(EAGER_SUBSELECT_PROFILE);
			List<F> fs = s.createSelectionQuery("from F", F.class).getResultList();
			F f0 = fs.get(0);
			F f1 = fs.get(1);
			assertTrue( isInitialized( f0.es ) );
			assertTrue( isInitialized( f1.es ) );
			f0.es.size();
			assertTrue( isInitialized( f0.es ) );
			assertTrue( isInitialized( f1.es ) );
		});
		scope.inSession( s -> {
			s.enableFetchProfile(OLD_SUBSELECT_PROFILE);
			List<F> fs = s.createSelectionQuery("from F", F.class).getResultList();
			F f0 = fs.get(0);
			F f1 = fs.get(1);
			assertFalse( isInitialized( f0.es ) );
			assertFalse( isInitialized( f1.es ) );
			f0.es.size();
			assertTrue( isInitialized( f0.es ) );
			assertTrue( isInitialized( f1.es ) );
		});
	}

	@Test void testDefaultProfile(SessionFactoryScope scope) {
		scope.getCollectingStatementInspector().clear();
		scope.inTransaction( s-> {
			G g = new G();
			H h1 = new H();
			h1.g = g;
			H h2 = new H();
			h2.g = g;
			s.persist(g);
			s.persist(h1);
			s.persist(h2);
		});
		scope.getCollectingStatementInspector().clear();

		List<H> hs1 = scope.fromSession( s -> {
			return s.createSelectionQuery("from H", H.class).getResultList();
		});
		assertTrue( isInitialized( hs1.get(0).g ) );
		scope.getCollectingStatementInspector().assertExecutedCount(2);
		scope.getCollectingStatementInspector().assertNumberOfJoins(0, 0);
		scope.getCollectingStatementInspector().assertNumberOfJoins(1, 0);

		scope.getCollectingStatementInspector().clear();
		List<H> hs2 = scope.fromSession( s -> {
			s.enableFetchProfile( HIBERNATE_DEFAULT_PROFILE );
			return s.createSelectionQuery("from H", H.class).getResultList();
		});
		assertTrue( isInitialized( hs2.get(0).g ) );
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().assertNumberOfJoins(0,1);

		scope.getCollectingStatementInspector().clear();
		List<H> hs3 = scope.fromSession( s -> {
			s.enableFetchProfile("test");
			return s.createSelectionQuery("from H", H.class).getResultList();
		});
		assertTrue( isInitialized( hs3.get(0).g ) );
		scope.getCollectingStatementInspector().assertExecutedCount(1);
		scope.getCollectingStatementInspector().assertNumberOfJoins(0,1);
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
		@FetchProfileOverride(mode = JOIN, profile = NEW_PROFILE)
		G g;
		@OneToMany(mappedBy = "f")
		@FetchProfileOverride(mode = JOIN, profile = NEW_PROFILE)
		@FetchProfileOverride(mode = SUBSELECT, fetch = LAZY, profile = LAZY_SUBSELECT_PROFILE)
		@FetchProfileOverride(mode = SUBSELECT, fetch = EAGER, profile = EAGER_SUBSELECT_PROFILE)
		@FetchProfileOverride(mode = SELECT, fetch = LAZY, profile = LAZY_SELECT_PROFILE)
		@FetchProfileOverride(mode = SELECT, fetch = EAGER, profile = EAGER_SELECT_PROFILE)
		@FetchProfileOverride(mode = JOIN, profile = JOIN_PROFILE)
		Set<E> es;
		@NaturalId String s;
	}
	@Entity(name = "G")
	static class G {
		@Id @GeneratedValue
		Long id;
	}

	@FetchProfile(name = "test")
	@Entity(name = "H")
	static class H {
		@Id @GeneratedValue
		Long id;
		@FetchProfileOverride(profile = "test", mode = JOIN)
		@ManyToOne G g;
	}
}
