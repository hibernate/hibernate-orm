package org.hibernate.orm.test.mapping.inheritance.joined;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A count query must retain the parent table join when a disjunction references
/// both inherited and subclass attributes and another predicate references the subclass.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = {
		JoinedInheritanceCriteriaCountTest.AbstractUser.class,
		JoinedInheritanceCriteriaCountTest.AccountUser.class
} )
@SessionFactory
@JiraKey( "HHH-20818" )
public class JoinedInheritanceCriteriaCountTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void countWithInheritedDisjunctionAndSubclassConjunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Long.class );
			final var user = criteria.from( AccountUser.class );
			criteria.select( builder.count( user ) );
			criteria.where( builder.and(
					builder.or(
							builder.isNotNull( user.get( "firstName" ) ),
							builder.isNotNull( user.get( "login" ) )
					),
					builder.isNotNull( user.get( "login" ) )
			) );

			assertEquals( 0L, session.createQuery( criteria ).getSingleResult() );
		} );
	}

	@Test
	void countWithInheritedDisjunctionAndSubclassConjunctionInverse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Long.class );
			final var user = criteria.from( AccountUser.class );
			criteria.select( builder.count( user ) );
			criteria.where( builder.and(
					builder.isNotNull( user.get( "login" ) ),
					builder.or(
							builder.isNotNull( user.get( "firstName" ) ),
							builder.isNotNull( user.get( "login" ) )
					)
			) );

			assertEquals( 0L, session.createQuery( criteria ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource( ints = { 0, 1, 2, 3, 4, 5, 6, 7 } )
	void countWithPopulatedPredicateVariants(int variant, SessionFactoryScope scope) {
		populate( scope );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Long.class );
			final var user = criteria.from( AccountUser.class );
			final var firstName = builder.isNotNull( user.get( "firstName" ) );
			final var login = builder.isNotNull( user.get( "login" ) );
			final var alwaysTrue = builder.equal( builder.literal( 1 ), builder.literal( 1 ) );
			final var disjunction = builder.or( firstName, login );
			criteria.select( builder.count( user ) );
			criteria.where( switch ( variant ) {
				case 0 -> builder.and( disjunction, login );
				case 1 -> builder.and( login, disjunction );
				case 2 -> builder.and( builder.or( login, firstName ), login );
				case 3 -> builder.and( builder.or( firstName, builder.or( login, builder.disjunction() ) ), login );
				case 4 -> builder.and( builder.or( firstName, alwaysTrue ), login );
				case 5 -> builder.and( builder.or( alwaysTrue, firstName ), login );
				case 6 -> builder.and( builder.or( firstName, builder.disjunction(), login ), login );
				case 7 -> builder.and( builder.or( builder.disjunction(), firstName, login ), login );
				default -> throw new IllegalArgumentException( "Unknown variant: " + variant );
			} );
			assertEquals( 3L, session.createQuery( criteria ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource( strings = {
			"(u.firstName is not null or u.login is not null) and u.login is not null",
			"u.login is not null and (u.firstName is not null or u.login is not null)",
			"(u.firstName is not null or 1 = 1) and u.login is not null"
	} )
	void hqlCountWithPopulatedPredicateVariants(String predicate, SessionFactoryScope scope) {
		populate( scope );
		scope.inTransaction( session -> assertEquals( 3L, session.createQuery(
				"select count(u) from AccountUser u where " + predicate,
				Long.class
		).getSingleResult() ) );
	}

	private void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 3; i++ ) {
				final var user = new AccountUser();
				user.login = "login" + i;
				user.firstName = i == 0 ? null : "name" + i;
				session.persist( user );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource( booleans = { false, true } )
	void countAcrossSeparateTableGroups(boolean reverse, SessionFactoryScope scope) {
		populate( scope );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var first = query.from( AccountUser.class );
			final var second = query.from( AccountUser.class );
			final var firstName = builder.isNotNull( first.get( "firstName" ) );
			final var secondName = builder.isNotNull( second.get( "firstName" ) );
			query.select( builder.count( first ) ).where( builder.and(
					reverse ? builder.or( secondName, firstName ) : builder.or( firstName, secondName ),
					builder.isNotNull( first.get( "login" ) ),
					builder.isNotNull( second.get( "login" ) )
			) );
			assertEquals( 8L, session.createQuery( query ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource( booleans = { false, true } )
	void countWithCorrelatedDisjunction(boolean reverse, SessionFactoryScope scope) {
		populate( scope );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var user = query.from( AccountUser.class );
			final var subquery = query.subquery( Integer.class );
			final var correlated = subquery.correlate( user );
			subquery.select( builder.literal( 1 ) ).where( builder.isNotNull( correlated.get( "firstName" ) ) );
			final var exists = builder.exists( subquery );
			final var login = builder.equal( user.get( "login" ), "login0" );
			query.select( builder.count( user ) ).where( builder.and(
					reverse ? builder.or( login, exists ) : builder.or( exists, login ),
					builder.isNotNull( user.get( "login" ) )
			) );
			assertEquals( 3L, session.createQuery( query ).getSingleResult() );
		} );
	}

	@Entity( name = "AbstractUser" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public abstract static class AbstractUser {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		Long id;

		String firstName;
		String email;
	}

	@Entity( name = "AccountUser" )
	public static class AccountUser extends AbstractUser {
		@Column( nullable = false, unique = true )
		String login;
	}
}
