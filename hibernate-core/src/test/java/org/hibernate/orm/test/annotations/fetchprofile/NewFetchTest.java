package org.hibernate.orm.test.annotations.fetchprofile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.annotations.FetchMode.JOIN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {NewFetchTest.class,NewFetchTest.E.class, NewFetchTest.F.class, NewFetchTest.G.class})
@FetchProfile(name = NewFetchTest.NEW_PROFILE)
@FetchProfile(name = NewFetchTest.OLD_PROFILE,
		fetchOverrides = @FetchProfile.FetchOverride(entity = NewFetchTest.E.class, association = "f", mode = JOIN))
public class NewFetchTest {

	public static final String NEW_PROFILE = "new-profile";
	public static final String OLD_PROFILE = "old-profile";

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
		assertFalse( Hibernate.isInitialized( f.g ) );
		assertFalse( Hibernate.isInitialized( f.es ) );
		F ff = scope.fromSession( s -> {
			s.enableFetchProfile(NEW_PROFILE);
			return s.find(F.class, 1);
		} );
		assertTrue( Hibernate.isInitialized( ff.g ) );
		assertTrue( Hibernate.isInitialized( ff.es ) );

		E e = scope.fromSession( s -> s.find(E.class, 1));
		assertFalse( Hibernate.isInitialized( e.f ) );
		E ee = scope.fromSession( s -> {
			s.enableFetchProfile(OLD_PROFILE);
			return s.find(E.class, 1);
		} );
		assertTrue( Hibernate.isInitialized( ee.f ) );
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
		@Fetch(value = JOIN, profile = NEW_PROFILE)
		G g;
		@OneToMany(mappedBy = "f")
		@Fetch(value = JOIN, profile = NEW_PROFILE)
		Set<E> es;
	}
	@Entity(name = "G")
	static class G {
		@Id @GeneratedValue
		Long id;
	}
}
