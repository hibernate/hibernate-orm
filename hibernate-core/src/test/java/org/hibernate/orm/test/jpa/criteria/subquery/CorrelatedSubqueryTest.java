/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.LineItem_;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Order_;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CorrelatedSubqueryTest extends AbstractMetamodelSpecificTest {

	@Test
	public void testBasicCorrelation() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
		Root<Customer> customer = criteria.from( Customer.class );
		criteria.select( customer );
		Subquery<Order> orderSubquery = criteria.subquery( Order.class );
		Root<Customer> customerCorrelationRoot = orderSubquery.correlate( customer );
		Join<Customer, Order> customerOrderCorrelationJoin = customerCorrelationRoot.join( Customer_.orders );
		orderSubquery.select( customerOrderCorrelationJoin );
		criteria.where( builder.not( builder.exists( orderSubquery ) ) );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testRestrictedCorrelation() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

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
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey("HHH-3032")
	@SkipForDialect(dialectClass= SybaseASEDialect.class, majorVersion = 15)
	public void testCorrelationExplicitSelectionCorrelation() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Customer> customerCriteria = builder.createQuery( Customer.class );
		Root<Customer> customer = customerCriteria.from( Customer.class );
		Join<Customer, Order> o = customer.join( Customer_.orders );
		Subquery<Order> sq = customerCriteria.subquery(Order.class);
		Join<Customer, Order> sqo = sq.correlate(o);
		Join<Order, LineItem> sql = sqo.join(Order_.lineItems);
		sq.where( builder.gt(sql.get( LineItem_.quantity), 3) );
		// use the correlation itself as the subquery selection (initially caused problems wrt aliases)
		sq.select(sqo);
		customerCriteria.select(customer).distinct(true);
		customerCriteria.where(builder.exists(sq));
		em.createQuery( customerCriteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testRestrictedCorrelationNoExplicitSelection() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Order> criteria = builder.createQuery( Order.class );
		Root<Order> orderRoot = criteria.from( Order.class );
		criteria.select( orderRoot );
		// create correlated subquery
		Subquery<Customer> customerSubquery = criteria.subquery( Customer.class );
		Root<Order> orderRootCorrelation = customerSubquery.correlate( orderRoot );
		Join<Order, Customer> orderCustomerJoin = orderRootCorrelation.join( "customer" );
		customerSubquery.where( builder.like( orderCustomerJoin.get( "name" ), "%Caruso" ) );
		criteria.where( builder.exists( customerSubquery ) );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-8556")
	public void testCorrelatedJoinsFromSubquery() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		CriteriaQuery<Customer> cquery = builder.createQuery(Customer.class);
		Root<Customer> customer = cquery.from(Customer.class);
		cquery.select(customer);
		Subquery<Order> sq = cquery.subquery(Order.class);
		Join<Customer, Order> sqo = sq.correlate(customer.join(Customer_.orders));
		sq.select(sqo);
		Set<Join<?, ?>> cJoins = sq.getCorrelatedJoins();

		// ensure the join is returned in #getCorrelatedJoins
		assertEquals( cJoins.size(), 1);
	}

	@Test
	public void testVariousSubqueryJoinSemantics() {
		// meant to assert semantics of #getJoins versus #getCorrelatedJoins
	}
}
