/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
import org.hibernate.orm.test.jpa.metamodel.CreditCard_;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the various predicates.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class PredicateTest extends AbstractMetamodelSpecificTest {
	private CriteriaBuilder builder;

	@BeforeEach
	public void prepareTestData() {
		builder = entityManagerFactory().getCriteriaBuilder();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Order( "order-1", 1.0d ) );
		em.persist( new Order( "order-2", 10.0d ) );
		em.persist( new Order( "order-3", new char[]{'r','u'} ) );
		em.getTransaction().commit();
		em.close();
	}

	@AfterEach
	public void cleanUp() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		em.createQuery( "delete from Order" ).executeUpdate();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEmptyConjunction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		// yes this is a retarded case, but explicitly allowed in the JPA spec
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.isTrue( builder.conjunction() ) );
		em.createQuery( orderCriteria ).getResultList();

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 3, orders.size() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEmptyDisjunction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		// yes this is a retarded case, but explicitly allowed in the JPA spec
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.isFalse( builder.disjunction() ) );
		em.createQuery( orderCriteria ).getResultList();

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 3, orders.size() );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Check simple not.
	 */
	@Test
	public void testSimpleNot() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		final Predicate p = builder.not( builder.equal( orderRoot.get( "id" ), "order-1" ) );
		assertEquals( Predicate.BooleanOperator.AND, p.getOperator() );
		orderCriteria.where( p );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 2, orders.size() );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Check simple not.
	 */
	@Test
	public void testSimpleNot2() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		final Predicate p = builder.equal( orderRoot.get( "id" ), "order-1" ).not();
		assertEquals( Predicate.BooleanOperator.AND, p.getOperator() );
		orderCriteria.where( p );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 2, orders.size() );
		em.getTransaction().commit();
		em.close();

	}

	/**
	 * Check complicated not.
	 */
	@Test
	public void testComplicatedNotOr() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
		Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
		Predicate compoundPredicate = builder.not( builder.or( p1, p2 ) );
		// negated OR should become an AND
		assertEquals( Predicate.BooleanOperator.AND, compoundPredicate.getOperator() );
		orderCriteria.where( compoundPredicate );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 1, orders.size() );
		Order order = orders.get( 0 );
		assertEquals( "order-3", order.getId() );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Check complicated not.
	 */
	@Test
	public void testNotMultipleOr() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
		Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
		Predicate p3 = builder.equal( orderRoot.get( "id" ), "order-3" );
		final Predicate compoundPredicate = builder.or( p1, p2, p3 ).not();
		// negated OR should become an AND
		assertEquals( Predicate.BooleanOperator.AND, compoundPredicate.getOperator() );
		orderCriteria.where( compoundPredicate );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 0, orders.size() );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Check complicated not.
	 */
	@Test
	public void testComplicatedNotAnd() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
		Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
		Predicate compoundPredicate = builder.and( p1, p2 ).not();
		orderCriteria.where( compoundPredicate );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 3, orders.size() );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Check predicate for field which has simple char array type (char[]).
	 */
	@Test
	public void testCharArray() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p = builder.equal( orderRoot.get( "domen" ), new char[]{'r','u'} );
		orderCriteria.where( p );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertEquals( 1, orders.size() );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Check predicate for field which has simple byte array type (byte[]).
	 */
	@Test
	@JiraKey( "HHH-10603" )
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 12,
			reason = "Oracle12cDialect uses blob to store byte arrays and it's not possible to compare blobs with simple equality operators.")
	public void testByteArray() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );

		orderCriteria.select( orderRoot );
		Predicate p = builder.equal( orderRoot.get( "number" ), new byte[]{'1','2'} );
		orderCriteria.where( p );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.isEmpty() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5803" )
	@SkipForDialect( dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/41943")
	public void testQuotientConversion() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
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
		assertTrue( orders.isEmpty() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testExplicitBuilderBooleanHandling() {
		// just checking syntax of the resulting query
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// note : these may fail on various matrix db jobs depending on how the dialect handles booleans
		{
			CriteriaQuery<CreditCard> criteriaQuery = builder.createQuery( CreditCard.class );
			Root<CreditCard> root = criteriaQuery.from( CreditCard.class );
			criteriaQuery.where( builder.isFalse( root.get( CreditCard_.approved ) ) );
			em.createQuery( criteriaQuery ).getResultList();
		}

		{
			CriteriaQuery<Order> criteriaQuery = builder.createQuery( Order.class );
			Root<Order> root = criteriaQuery.from( Order.class );
			criteriaQuery.where( builder.isFalse( root.get( Order_.creditCard ).get( CreditCard_.approved ) ) );
			em.createQuery( criteriaQuery ).getResultList();
		}

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8901" )
	public void testEmptyInPredicate() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.in( orderRoot.get("totalPrice") ) );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.isEmpty() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-17804" )
	public void testEmptyInPredicate2() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.in( orderRoot.get("id") ) );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertTrue( orders.isEmpty() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-17804" )
	public void testEmptyInPredicate3() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = orderCriteria.from( Order.class );
		orderCriteria.select( orderRoot );
		orderCriteria.where( builder.in( orderRoot.get("id") ).not() );

		List<Order> orders = em.createQuery( orderCriteria ).getResultList();
		assertFalse( orders.isEmpty() );
		em.getTransaction().commit();
		em.close();
	}
}
