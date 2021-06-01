/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.jpa.test.metamodel.CreditCard;
import org.hibernate.jpa.test.metamodel.CreditCard_;
import org.hibernate.jpa.test.metamodel.Customer_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;
import org.hibernate.orm.test.jpa.criteria.AbstractCriteriaTest;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Test the various predicates.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class PredicateTest extends AbstractCriteriaTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Order( "order-1", 1.0d ) );
			em.persist( new Order( "order-2", 10.0d ) );
			em.persist( new Order( "order-3", new char[] { 'r', 'u' } ) );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.createQuery( "delete Order" ).executeUpdate() );
	}

	@Test
	void testEmptyConjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// yes this is a retarded case, but explicitly allowed in the JPA spec
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.isTrue( builder.conjunction() ) );
			em.createQuery( orderCriteria ).getResultList();

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, hasSize( 3 ) );
		} );
	}

	@Test
	void testEmptyDisjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// yes this is a retarded case, but explicitly allowed in the JPA spec
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.isFalse( builder.disjunction() ) );
			em.createQuery( orderCriteria ).getResultList();

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, hasSize( 3 ) );
		} );
	}

	/**
	 * Check simple not.
	 */
	@Test
	void testSimpleNot(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			final Predicate p = builder.not( builder.equal( orderRoot.get( "id" ), "order-1" ) );
			assertThat( p.getOperator(), is( Predicate.BooleanOperator.AND ) );
			orderCriteria.where( p );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, hasSize( 2 ) );
		} );
	}

	/**
	 * Check simple not.
	 */
	@Test
	void testSimpleNot2(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			final Predicate p = builder.equal( orderRoot.get( "id" ), "order-1" ).not();
			assertThat( p.getOperator(), is( Predicate.BooleanOperator.AND ) );
			orderCriteria.where( p );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, hasSize( 2 ) );
		} );
	}

	/**
	 * Check complicated not.
	 */
	@Test
	void testComplicatedNotOr(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
			Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
			Predicate compoundPredicate = builder.not( builder.or( p1, p2 ) );
			// negated OR should become an AND
			assertThat( compoundPredicate.getOperator(), is( Predicate.BooleanOperator.AND ) );
			orderCriteria.where( compoundPredicate );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, hasSize( 1 ) );
			Order order = orders.get( 0 );
			assertThat( order.getId(), is( "order-3" ) );
		} );
	}

	/**
	 * Check complicated not.
	 */
	@Test
	void testNotMultipleOr(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
			Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
			Predicate p3 = builder.equal( orderRoot.get( "id" ), "order-3" );
			final Predicate compoundPredicate = builder.or( p1, p2, p3 ).not();
			// negated OR should become an AND
			assertThat( compoundPredicate.getOperator(), is( Predicate.BooleanOperator.AND ) );
			orderCriteria.where( compoundPredicate );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, empty() );
		} );
	}

	/**
	 * Check predicate for field which has simple char array type (char[]).
	 */
	@Test
	void testCharArray(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p = builder.equal( orderRoot.get( "domen" ), new char[] { 'r', 'u' } );
			orderCriteria.where( p );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, hasSize( 1 ) );
		} );
	}

	/**
	 * Check predicate for field which has simple byte array type (byte[]).
	 */
	@Test
	@SkipForDialect(
			value = OracleDialect.class, jiraKey = "HHH-10603",
			comment = "Oracle12cDialect uses blob to store byte arrays and it's not possible to compare blobs with simple equality operators."
	)
	void testByteArray(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p = builder.equal( orderRoot.get( "number" ), new byte[] { '1', '2' } );
			orderCriteria.where( p );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, empty() );
		} );
	}

	@Test
	void testExplicitBuilderBooleanHandling(EntityManagerFactoryScope scope) {
		// just checking syntax of the resulting query
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			// note : these may fail on various matrix db jobs depending on how the dialect handles booleans
			{
				CriteriaQuery<CreditCard> criteriaQuery = builder.createQuery( CreditCard.class );
				Root<CreditCard> root = criteriaQuery.from( CreditCard.class );
				criteriaQuery.where( builder.isFalse( root.get( CreditCard_.APPROVED ) ) );
				em.createQuery( criteriaQuery ).getResultList();
			}

			{
				CriteriaQuery<Order> criteriaQuery = builder.createQuery( Order.class );
				Root<Order> root = criteriaQuery.from( Order.class );
				criteriaQuery.where( builder.isFalse( root.get( Order_.CREDIT_CARD ).get( CreditCard_.APPROVED ) ) );
				em.createQuery( criteriaQuery ).getResultList();
			}
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8901" )
	void testEmptyInPredicate(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.in( orderRoot.get("totalPrice") ) );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, empty() );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5803" )
	@SkipForDialect( value = CockroachDialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/41943")
	void testQuotientConversion(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			Long longValue = 999999999L;
			Path<Double> doublePath = orderRoot.get( Order_.totalPrice );
			Path<Integer> integerPath = orderRoot.get( Order_.customer ).get( Customer_.age );

			orderCriteria.select( orderRoot );
			Predicate p = builder.ge(
					builder.quot( integerPath, doublePath ),
					longValue
			);
			orderCriteria.where( p );

			List<Order> orders = em.createQuery( orderCriteria ).getResultList();
			assertThat( orders, empty() );
		} );
	}

}
