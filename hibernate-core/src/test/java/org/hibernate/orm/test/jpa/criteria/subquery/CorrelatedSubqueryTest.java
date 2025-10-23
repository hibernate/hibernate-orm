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
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Alias;
import org.hibernate.orm.test.jpa.metamodel.Country;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.orm.test.jpa.metamodel.Entity1;
import org.hibernate.orm.test.jpa.metamodel.Entity2;
import org.hibernate.orm.test.jpa.metamodel.Entity3;
import org.hibernate.orm.test.jpa.metamodel.Info;
import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.LineItem_;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.ShelfLife;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.orm.test.jpa.metamodel.Thing;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.VersionedEntity;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		Address.class, Alias.class, Country.class, CreditCard.class, Customer.class,
		Entity1.class, Entity2.class, Entity3.class,
		Info.class, LineItem.class, Order.class, Phone.class, Product.class,
		ShelfLife.class, Spouse.class, Thing.class, ThingWithQuantity.class,
		VersionedEntity.class
})
public class CorrelatedSubqueryTest {

	@Test
	public void testBasicCorrelation(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
			CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
			Root<Customer> customer = criteria.from( Customer.class );
			criteria.select( customer );
			Subquery<Order> orderSubquery = criteria.subquery( Order.class );
			Root<Customer> customerCorrelationRoot = orderSubquery.correlate( customer );
			Join<Customer, Order> customerOrderCorrelationJoin = customerCorrelationRoot.join( Customer_.orders );
			orderSubquery.select( customerOrderCorrelationJoin );
			criteria.where( builder.not( builder.exists( orderSubquery ) ) );
			entityManager.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	public void testRestrictedCorrelation(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> criteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = criteria.from( Order.class );
			criteria.select( orderRoot );
			// create correlated subquery
			Subquery<Customer> customerSubquery = criteria.subquery( Customer.class );
			Root<Order> orderRootCorrelation = customerSubquery.correlate( orderRoot );
			Join<Order, Customer> orderCustomerJoin = orderRootCorrelation.join( Order_.customer );
			customerSubquery.where( builder.like( orderCustomerJoin.get( Customer_.name ), "%Caruso" ) )
					.select( orderCustomerJoin );
			criteria.where( builder.exists( customerSubquery ) );
			entityManager.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	@JiraKey("HHH-3032")
	@SkipForDialect(dialectClass= SybaseASEDialect.class, majorVersion = 15)
	public void testCorrelationExplicitSelectionCorrelation(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
			CriteriaQuery<Customer> customerCriteria = builder.createQuery( Customer.class );
			Root<Customer> customer = customerCriteria.from( Customer.class );
			Join<Customer, Order> o = customer.join( Customer_.orders );
			Subquery<Order> sq = customerCriteria.subquery( Order.class );
			Join<Customer, Order> sqo = sq.correlate( o );
			Join<Order, LineItem> sql = sqo.join( Order_.lineItems );
			sq.where( builder.gt( sql.get( LineItem_.quantity ), 3 ) );
			// use the correlation itself as the subquery selection (initially caused problems wrt aliases)
			sq.select( sqo );
			customerCriteria.select( customer ).distinct( true );
			customerCriteria.where( builder.exists( sq ) );
			entityManager.createQuery( customerCriteria ).getResultList();
		} );
	}

	@Test
	public void testRestrictedCorrelationNoExplicitSelection(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();

		scope.inTransaction( entityManager -> {
			CriteriaQuery<Order> criteria = builder.createQuery( Order.class );
			Root<Order> orderRoot = criteria.from( Order.class );
			criteria.select( orderRoot );
			// create correlated subquery
			Subquery<Customer> customerSubquery = criteria.subquery( Customer.class );
			Root<Order> orderRootCorrelation = customerSubquery.correlate( orderRoot );
			Join<Order, Customer> orderCustomerJoin = orderRootCorrelation.join( "customer" );
			customerSubquery.where( builder.like( orderCustomerJoin.get( "name" ), "%Caruso" ) );
			criteria.where( builder.exists( customerSubquery ) );
			entityManager.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	@JiraKey(value = "HHH-8556")
	public void testCorrelatedJoinsFromSubquery(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();
		CriteriaQuery<Customer> cquery = builder.createQuery(Customer.class);
		Root<Customer> customer = cquery.from(Customer.class);
		cquery.select(customer);
		Subquery<Order> sq = cquery.subquery(Order.class);
		Join<Customer, Order> sqo = sq.correlate(customer.join(Customer_.orders));
		sq.select(sqo);
		Set<Join<?, ?>> cJoins = sq.getCorrelatedJoins();

		// ensure the join is returned in #getCorrelatedJoins
		assertEquals( 1, cJoins.size() );
	}
}
