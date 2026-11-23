package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Each treated branch must retain its subtype semantics, including tests of null attributes.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		TreatedPredicateGroupingTest.Base.class,
		TreatedPredicateGroupingTest.Sub1.class,
		TreatedPredicateGroupingTest.Sub2.class
})
@SessionFactory
@JiraKey("HHH-10768")
class TreatedPredicateGroupingTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var first = new Sub1();
			first.id = 1L;
			first.baseValue = 200;
			var second = new Sub2();
			second.id = 2L;
			second.baseValue = 100;
			session.persist( first );
			session.persist( second );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testHqlDisjunctionRejectsWrongSubtypes(SessionFactoryScope scope) {
		assertThat( hql( scope, "or", 100, 200 ) ).isEmpty();
	}

	@Test
	void testHqlConjunctionRejectsIncompatibleSubtypes(SessionFactoryScope scope) {
		assertThat( hql( scope, "and", 100, 100 ) ).isEmpty();
	}

	@Test
	void testHqlDisjunctionMatchesBothSubtypes(SessionFactoryScope scope) {
		assertThat( hql( scope, "or", 200, 100 ) ).extracting( entity -> entity.id )
				.containsExactlyInAnyOrder( 1L, 2L );
	}

	@Test
	void testCriteriaDisjunctionRejectsWrongSubtypes(SessionFactoryScope scope) {
		assertThat( criteria( scope, false, 100, 200 ) ).isEmpty();
	}

	@Test
	void testCriteriaConjunctionRejectsIncompatibleSubtypes(SessionFactoryScope scope) {
		assertThat( criteria( scope, true, 100, 100 ) ).isEmpty();
	}

	@Test
	void testCriteriaDisjunctionMatchesBothSubtypes(SessionFactoryScope scope) {
		assertThat( criteria( scope, false, 200, 100 ) ).extracting( entity -> entity.id )
				.containsExactlyInAnyOrder( 1L, 2L );
	}

	private List<Base> hql(SessionFactoryScope scope, String junction, int first, int second) {
		return scope.fromTransaction( session -> session.createQuery(
				"from GroupedTreatBase b where "
						+ "(treat(b as GroupedTreatSub1).baseValue = :first and treat(b as GroupedTreatSub1).subValue1 is null) "
						+ junction
						+ " (treat(b as GroupedTreatSub2).baseValue = :second and treat(b as GroupedTreatSub2).subValue2 is null)",
				Base.class
		).setParameter( "first", first ).setParameter( "second", second ).getResultList() );
	}

	private List<Base> criteria(SessionFactoryScope scope, boolean conjunction, int first, int second) {
		return scope.fromTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createQuery( Base.class );
			var root = query.from( Base.class );
			var sub1 = builder.treat( root, Sub1.class );
			var sub2 = builder.treat( root, Sub2.class );
			var firstBranch = builder.and(
					builder.equal( sub1.get( "baseValue" ), first ), builder.isNull( sub1.get( "subValue1" ) ) );
			var secondBranch = builder.and(
					builder.equal( sub2.get( "baseValue" ), second ), builder.isNull( sub2.get( "subValue2" ) ) );
			query.select( root ).where( conjunction
					? builder.and( firstBranch, secondBranch )
					: builder.or( firstBranch, secondBranch ) );
			return session.createQuery( query ).getResultList();
		} );
	}

	@Entity(name = "GroupedTreatBase")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class Base {
		@Id
		Long id;
		int baseValue;
	}

	@Entity(name = "GroupedTreatSub1")
	public static class Sub1 extends Base {
		Integer subValue1;
	}

	@Entity(name = "GroupedTreatSub2")
	public static class Sub2 extends Base {
		Integer subValue2;
	}
}
