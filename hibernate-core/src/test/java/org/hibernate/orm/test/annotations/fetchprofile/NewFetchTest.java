package org.hibernate.orm.test.annotations.fetchprofile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.annotations.FetchMode.JOIN;
import static org.hibernate.annotations.FetchMode.SELECT;
import static org.hibernate.annotations.FetchMode.SUBSELECT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {NewFetchTest.class,NewFetchTest.E.class, NewFetchTest.F.class, NewFetchTest.G.class})
@FetchProfile(name = NewFetchTest.NEW_PROFILE)
@FetchProfile(name = NewFetchTest.OLD_PROFILE,
		fetchOverrides = @FetchProfile.FetchOverride(entity = NewFetchTest.E.class, association = "f"))
@FetchProfile(name = NewFetchTest.SUBSELECT_PROFILE)
@FetchProfile(name = NewFetchTest.SELECT_PROFILE)
@FetchProfile(name = NewFetchTest.JOIN_PROFILE)
@FetchProfile(name = NewFetchTest.OLD_SUBSELECT_PROFILE,
		fetchOverrides = @FetchProfile.FetchOverride(entity = NewFetchTest.F.class, association = "es", mode = SUBSELECT))
public class NewFetchTest {

	static final String NEW_PROFILE = "new-profile";
	static final String OLD_PROFILE = "old-profile";
	static final String SUBSELECT_PROFILE = "subselect-profile";
	static final String SELECT_PROFILE = "select-profile";
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

		F f = scope.fromSession( s -> s.find(F.class, 1));
		assertFalse( isInitialized( f.g ) );
		assertFalse( isInitialized( f.es ) );
		F ff = scope.fromSession( s -> {
			s.enableFetchProfile(NEW_PROFILE);
			return s.find(F.class, 1);
		} );
		assertTrue( isInitialized( ff.g ) );
		assertTrue( isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.find(E.class, 1));
		assertFalse( isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			s.enableFetchProfile(OLD_PROFILE);
			return s.find(E.class, 1);
		} );
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
			s.enableFetchProfile(SELECT_PROFILE);
			return s.find(F.class, id);
		});
		assertTrue(isInitialized(f1.es));
		F f2 = scope.fromSession( s -> {
			s.enableFetchProfile(JOIN_PROFILE);
			return s.find(F.class, id);
		});
		assertTrue(isInitialized(f2.es));
		F f3 = scope.fromSession( s -> {
			s.enableFetchProfile(SUBSELECT_PROFILE);
			return s.find(F.class, id);
		});
		assertTrue(isInitialized(f3.es));
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
			s.enableFetchProfile(SUBSELECT_PROFILE);
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

	@Entity(name = "E")
	static class E {
		@Id @GeneratedValue
		Long id;
		@ManyToOne(fetch = LAZY)
		F f;
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
		@FetchProfileOverride(mode = SUBSELECT, profile = SUBSELECT_PROFILE)
		@FetchProfileOverride(mode = SELECT, profile = SELECT_PROFILE)
		@FetchProfileOverride(mode = JOIN, profile = JOIN_PROFILE)
		Set<E> es;
	}
	@Entity(name = "G")
	static class G {
		@Id @GeneratedValue
		Long id;
	}
}
