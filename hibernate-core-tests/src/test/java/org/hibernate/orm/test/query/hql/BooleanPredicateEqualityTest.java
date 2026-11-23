package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Compares the boolean values of two HQL predicates.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = BooleanPredicateEqualityTest.Item.class)
@SessionFactory
@JiraKey("HHH-11472")
class BooleanPredicateEqualityTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var item = new Item();
			item.id = 1L;
			session.persist( item );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@FailureExpected(jiraKey = "HHH-11472", reason = "HQL comparisons do not accept predicates as operands")
	void testEqualPredicates(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BooleanEqualityItem where (1 > 2) = (3 > 4)", Item.class
		).getResultList() ).extracting( item -> item.id ).containsExactly( 1L ) );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-11472", reason = "HQL comparisons do not accept predicates as operands")
	void testDifferentPredicates(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BooleanEqualityItem where (1 < 2) = (3 > 4)", Item.class
		).getResultList() ).isEmpty() );
	}

	@Test
	void testBooleanLiteralControl(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "from BooleanEqualityItem where true = true", Item.class ).getResultList() )
					.extracting( item -> item.id ).containsExactly( 1L );
			assertThat( session.createQuery( "from BooleanEqualityItem where true = false", Item.class ).getResultList() )
					.isEmpty();
		} );
	}

	@Entity(name = "BooleanEqualityItem")
	public static class Item {
		@Id
		Long id;
	}
}
