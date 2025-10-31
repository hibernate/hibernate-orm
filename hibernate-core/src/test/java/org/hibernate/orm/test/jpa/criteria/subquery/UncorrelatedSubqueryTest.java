/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Alias;
import org.hibernate.orm.test.jpa.metamodel.Country;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
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
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		Address.class, Alias.class, Country.class, CreditCard.class, Customer.class,
		Info.class, LineItem.class, Order.class, Phone.class, Product.class,
		ShelfLife.class, Spouse.class
})
public class UncorrelatedSubqueryTest {
	@Test
	public void testGetCorrelatedParentIllegalStateException(EntityManagerFactoryScope scope) {
		// test that attempting to call getCorrelatedParent on an uncorrelated query/subquery
		// throws ISE

		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();
		scope.inTransaction( entityManager -> {

			CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
			Root<Customer> customerRoot = criteria.from( Customer.class );
			Join<Customer, Order> orderJoin = customerRoot.join( Customer_.orders );
			criteria.select( customerRoot );
			Subquery<Double> subCriteria = criteria.subquery( Double.class );
			Root<Order> subqueryOrderRoot = subCriteria.from( Order.class );
			subCriteria.select( builder.min( subqueryOrderRoot.get( Order_.totalPrice ) ) );
			criteria.where( builder.equal( orderJoin.get( "totalPrice" ), builder.all( subCriteria ) ) );

			assertFalse( customerRoot.isCorrelated() );
			assertFalse( subqueryOrderRoot.isCorrelated() );

			assertThrows(
					IllegalStateException.class,
					customerRoot::getCorrelationParent,
					"Should have resulted in IllegalStateException"
			);

			assertThrows(
					IllegalStateException.class,
					subqueryOrderRoot::getCorrelationParent,
					"Should have resulted in IllegalStateException"
			);
		} );
	}

	@Test
	public void testEqualAll(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();
		scope.inTransaction( entityManager -> {

			CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
			Root<Customer> customerRoot = criteria.from( Customer.class );
			Join<Customer, Order> orderJoin = customerRoot.join( Customer_.orders );
			criteria.select( customerRoot );
			Subquery<Double> subCriteria = criteria.subquery( Double.class );
			Root<Order> subqueryOrderRoot = subCriteria.from( Order.class );
			subCriteria.select( builder.min( subqueryOrderRoot.get( Order_.totalPrice ) ) );
			criteria.where( builder.equal( orderJoin.get( "totalPrice" ), builder.all( subCriteria ) ) );
			entityManager.createQuery( criteria ).getResultList();

		} );
	}

	@Test
	public void testLessThan(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();
		scope.inTransaction( entityManager -> {

			CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
			Root<Customer> customerRoot = criteria.from( Customer.class );

			Subquery<Double> subCriteria = criteria.subquery( Double.class );
			Root<Customer> subQueryCustomerRoot = subCriteria.from( Customer.class );
			subCriteria.select( builder.avg( subQueryCustomerRoot.get( Customer_.age ) ) );

			criteria.where(
					builder.lessThan(
							customerRoot.get( Customer_.age ),
							subCriteria.getSelection().as( Integer.class )
					)
			);
			entityManager.createQuery( criteria ).getResultList();

		} );
	}
}
