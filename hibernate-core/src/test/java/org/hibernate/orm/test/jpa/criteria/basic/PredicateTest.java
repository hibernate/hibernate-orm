/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Alias;
import org.hibernate.orm.test.jpa.metamodel.Country;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
import org.hibernate.orm.test.jpa.metamodel.CreditCard_;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.orm.test.jpa.metamodel.Info;
import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
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
@Jpa(annotatedClasses = {
		Address.class, Alias.class, Country.class, CreditCard.class, Customer.class,
		Info.class, LineItem.class, Order.class, Phone.class, Product.class,
		ShelfLife.class, Spouse.class
})
public class PredicateTest {
	private CriteriaBuilder builder;

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
			entityManager.persist( new Order( "order-1", 1.0d ) );
			entityManager.persist( new Order( "order-2", 10.0d ) );
			entityManager.persist( new Order( "order-3", new char[] {'r', 'u'} ) );
		} );
	}

	@AfterEach
	public void cleanUp(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEmptyConjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			// yes this is a retarded case, but explicitly allowed in the JPA spec
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.isTrue( builder.conjunction() ) );
			entityManager.createQuery( orderCriteria ).getResultList();

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 3, orders.size() );
		} );
	}

	@Test
	public void testEmptyDisjunction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			// yes this is a retarded case, but explicitly allowed in the JPA spec
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.isFalse( builder.disjunction() ) );
			entityManager.createQuery( orderCriteria ).getResultList();

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 3, orders.size() );
		} );
	}

	/**
	 * Check simple not.
	 */
	@Test
	public void testSimpleNot(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			final Predicate p = builder.not( builder.equal( orderRoot.get( "id" ), "order-1" ) );
			assertEquals( Predicate.BooleanOperator.AND, p.getOperator() );
			orderCriteria.where( p );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 2, orders.size() );
		} );
	}

	/**
	 * Check simple not.
	 */
	@Test
	public void testSimpleNot2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			final Predicate p = builder.equal( orderRoot.get( "id" ), "order-1" ).not();
			assertEquals( Predicate.BooleanOperator.AND, p.getOperator() );
			orderCriteria.where( p );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 2, orders.size() );
		} );
	}

	/**
	 * Check complicated not.
	 */
	@Test
	public void testComplicatedNotOr(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
			Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
			Predicate compoundPredicate = builder.not( builder.or( p1, p2 ) );
			// negated OR should become an AND
			assertEquals( Predicate.BooleanOperator.AND, compoundPredicate.getOperator() );
			orderCriteria.where( compoundPredicate );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 1, orders.size() );
			Order order = orders.get( 0 );
			assertEquals( "order-3", order.getId() );
		} );
	}

	/**
	 * Check complicated not.
	 */
	@Test
	public void testNotMultipleOr(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 0, orders.size() );
		} );
	}

	/**
	 * Check complicated not.
	 */
	@Test
	public void testComplicatedNotAnd(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p1 = builder.equal( orderRoot.get( "id" ), "order-1" );
			Predicate p2 = builder.equal( orderRoot.get( "id" ), "order-2" );
			Predicate compoundPredicate = builder.and( p1, p2 ).not();
			orderCriteria.where( compoundPredicate );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 3, orders.size() );
		} );
	}

	/**
	 * Check predicate for field which has simple char array type (char[]).
	 */
	@Test
	public void testCharArray(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p = builder.equal( orderRoot.get( "domen" ), new char[] {'r', 'u'} );
			orderCriteria.where( p );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertEquals( 1, orders.size() );
		} );
	}

	/**
	 * Check predicate for field which has simple byte array type (byte[]).
	 */
	@Test
	@JiraKey( "HHH-10603" )
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 12,
			reason = "Oracle12cDialect uses blob to store byte arrays and it's not possible to compare blobs with simple equality operators.")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Blobs are not allowed in this expression")
	public void testByteArray(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );

			orderCriteria.select( orderRoot );
			Predicate p = builder.equal( orderRoot.get( "number" ), new byte[] {'1', '2'} );
			orderCriteria.where( p );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertTrue( orders.isEmpty() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-5803" )
	@SkipForDialect( dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/41943")
	public void testQuotientConversion(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertTrue( orders.isEmpty() );
		} );
	}

	@Test
	public void testExplicitBuilderBooleanHandling(EntityManagerFactoryScope scope) {
		// just checking syntax of the resulting query
		scope.inTransaction( entityManager -> {

			// note : these may fail on various matrix db jobs depending on how the dialect handles booleans
			{
				CriteriaQuery<CreditCard> criteriaQuery = builder.createQuery( CreditCard.class );
				Root<CreditCard> root = criteriaQuery.from( CreditCard.class );
				criteriaQuery.where( builder.isFalse( root.get( CreditCard_.approved ) ) );
				entityManager.createQuery( criteriaQuery ).getResultList();
			}

			{
				CriteriaQuery<Order> criteriaQuery = builder.createQuery( Order.class );
				Root<Order> root = criteriaQuery.from( Order.class );
				criteriaQuery.where( builder.isFalse( root.get( Order_.creditCard ).get( CreditCard_.approved ) ) );
				entityManager.createQuery( criteriaQuery ).getResultList();
			}

		} );
	}

	@Test
	@JiraKey( value = "HHH-8901" )
	public void testEmptyInPredicate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.in( orderRoot.get( "totalPrice" ) ) );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertTrue( orders.isEmpty() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-17804" )
	public void testEmptyInPredicate2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.in( orderRoot.get( "id" ) ) );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertTrue( orders.isEmpty() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-17804" )
	public void testEmptyInPredicate3(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = orderCriteria.from( Order.class );
			orderCriteria.select( orderRoot );
			orderCriteria.where( builder.in( orderRoot.get( "id" ) ).not() );

			List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
			assertFalse( orders.isEmpty() );
		} );
	}
}
