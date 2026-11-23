package org.hibernate.orm.test.jpa.criteria;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.SetJoin;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises both casts used to treat a set fetch in HHH-13652.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { TreatSetFetchTest.Human.class, TreatSetFetchTest.Droid.class,
		TreatSetFetchTest.SpecialDroid.class } )
@SessionFactory( useCollectingStatementObserver = true )
@JiraKey( "HHH-13652" )
class TreatSetFetchTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Droid ordinary = new Droid();
			ordinary.id = 1;
			SpecialDroid special = new SpecialDroid();
			special.id = 2;
			special.secondaryFunction = "translation";
			session.persist( ordinary );
			session.persist( special );
			Human matching = new Human( 1 );
			matching.friends.add( ordinary );
			matching.friends.add( special );
			Human nonmatching = new Human( 2 );
			nonmatching.friends.add( ordinary );
			session.persist( matching );
			session.persist( nonmatching );
			session.persist( new Human( 3 ) );
		} );
	}

	@Test
	void treatAsSetJoin(SessionFactoryScope scope) {
		assertTreatedFetch( scope, true );
	}

	@Test
	void treatAsPath(SessionFactoryScope scope) {
		assertTreatedFetch( scope, false );
	}

	@SuppressWarnings( "unchecked" )
	private void assertTreatedFetch(SessionFactoryScope scope, boolean setJoin) {
		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var criteria = builder.createQuery( Human.class );
			var human = criteria.from( Human.class );
			Fetch<Human, Droid> fetch = human.fetch( "friends", JoinType.LEFT );
			Path<SpecialDroid> treated = setJoin
					? builder.treat( (SetJoin<Human, Droid>) fetch, SpecialDroid.class )
					: builder.treat( (Path<Droid>) fetch, SpecialDroid.class );
			var parameter = builder.parameter( String.class, "function" );
			criteria.select( human ).distinct( true );
			criteria.where( builder.like( treated.get( "secondaryFunction" ), parameter ) );

			var observer = scope.getCollectingStatementObserver();
			observer.clear();
			var results = session.createQuery( criteria ).setParameter( parameter, "trans%" ).getResultList();
			assertThat( results ).extracting( result -> result.id ).containsExactly( 1 );
			assertThat( Hibernate.isInitialized( results.get( 0 ).friends ) ).isTrue();
			assertThat( results.get( 0 ).friends ).anySatisfy( friend -> {
				assertThat( friend ).isInstanceOf( SpecialDroid.class );
				assertThat( ((SpecialDroid) friend).secondaryFunction ).isEqualTo( "translation" );
			} );
			observer.assertStatements().hasSize( 1 );
		} );
	}

	@Entity( name = "FetchHuman" )
	public static class Human {
		@Id
		Integer id;
		@ManyToMany
		Set<Droid> friends = new HashSet<>();

		public Human() {
		}

		Human(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "FetchDroid" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static class Droid {
		@Id
		Integer id;
	}

	@Entity( name = "FetchSpecialDroid" )
	public static class SpecialDroid extends Droid {
		String secondaryFunction;
	}
}
