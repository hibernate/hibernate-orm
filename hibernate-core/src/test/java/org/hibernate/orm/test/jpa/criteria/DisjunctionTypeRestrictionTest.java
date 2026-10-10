package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Disjunction analysis must preserve complete branch restrictions and surrounding type restrictions.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = {
		DisjunctionTypeRestrictionTest.Base.class,
		DisjunctionTypeRestrictionTest.First.class,
		DisjunctionTypeRestrictionTest.Second.class,
		DisjunctionTypeRestrictionTest.Third.class,
		DisjunctionTypeRestrictionTest.Other.class
} )
@SessionFactory
class DisjunctionTypeRestrictionTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Base[] entities = { new First(), new Second(), new Third(), new Other() };
			for ( int i = 0; i < entities.length; i++ ) {
				entities[i].id = i + 1L;
				entities[i].commonValue = 42;
				session.persist( entities[i] );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource( ints = { 0, 1, 2, 3, 4, 5 } )
	void overlappingThreeBranchRestrictions(int permutation, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( Base.class );
			final var first = firstBranch( builder, root );
			final var second = secondBranch( builder, root );
			final var thirdRoot = builder.treat( root, Third.class );
			final var third = builder.and(
					builder.equal( thirdRoot.get( "commonValue" ), 42 ),
					builder.isNull( thirdRoot.get( "thirdValue" ) )
			);
			// The sets overlap, but no restriction may be removed from an earlier branch
			// while the intersection is still being computed.
			final Predicate[] branches = { builder.or( first, second ), second, builder.or( second, third ) };
			final int[][] orders = { { 0, 1, 2 }, { 0, 2, 1 }, { 1, 0, 2 }, { 1, 2, 0 }, { 2, 0, 1 }, { 2, 1, 0 } };
			final var order = orders[permutation];
			query.select( root.get( "id" ) ).where(
					builder.or( branches[order[0]], branches[order[1]], branches[order[2]] )
			);
			assertThat( session.createQuery( query ).getResultList() ).containsExactlyInAnyOrder( 1L, 2L, 3L );
		} );
	}

	@ParameterizedTest
	@ValueSource( booleans = { false, true } )
	void surroundingTypeFilterSurvivesUnrestrictedBranch(boolean filterFirst, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( Base.class );
			final var filter = builder.equal( root.type(), First.class );
			final var disjunction = builder.or(
					builder.equal( root.type(), Second.class ),
					builder.equal( builder.literal( 1 ), builder.literal( 1 ) )
			);
			query.select( root.get( "id" ) ).where( filterFirst
					? builder.and( filter, disjunction )
					: builder.and( disjunction, filter ) );
			assertThat( session.createQuery( query ).getResultList() ).containsExactly( 1L );
		} );
	}

	@ParameterizedTest
	@ValueSource( booleans = { false, true } )
	void surroundingTreatSurvivesUnrestrictedBranch(boolean treatFirst, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( Base.class );
			final var treat = firstBranch( builder, root );
			final var disjunction = builder.or(
					firstBranch( builder, root ),
					builder.equal( root.get( "commonValue" ), 42 )
			);
			query.select( root.get( "id" ) ).where( treatFirst
					? builder.and( treat, disjunction )
					: builder.and( disjunction, treat ) );
			assertThat( session.createQuery( query ).getResultList() ).containsExactly( 1L );
		} );
	}

	private Predicate firstBranch(CriteriaBuilder builder, Root<Base> root) {
		final var first = builder.treat( root, First.class );
		return builder.and(
				builder.equal( first.get( "commonValue" ), 42 ),
				builder.isNull( first.get( "firstValue" ) )
		);
	}

	private Predicate secondBranch(CriteriaBuilder builder, Root<Base> root) {
		final var second = builder.treat( root, Second.class );
		return builder.and(
				builder.equal( second.get( "commonValue" ), 42 ),
				builder.isNull( second.get( "secondValue" ) )
		);
	}

	@Entity( name = "DisjunctionBase" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public abstract static class Base {
		@Id
		Long id;
		int commonValue;
	}

	@Entity( name = "DisjunctionFirst" )
	public static class First extends Base {
		Integer firstValue;
	}

	@Entity( name = "DisjunctionSecond" )
	public static class Second extends Base {
		Integer secondValue;
	}

	@Entity( name = "DisjunctionThird" )
	public static class Third extends Base {
		Integer thirdValue;
	}

	@Entity( name = "DisjunctionOther" )
	public static class Other extends Base {
	}
}
