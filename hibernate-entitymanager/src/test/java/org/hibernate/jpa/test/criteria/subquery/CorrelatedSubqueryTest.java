/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.criteria.subquery;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Customer_;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.LineItem_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

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
	@SkipForDialect(value=SybaseASE15Dialect.class, jiraKey="HHH-3032")
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
		customerSubquery.where( builder.like( orderCustomerJoin.<String>get( "name" ), "%Caruso" ) );
		criteria.where( builder.exists( customerSubquery ) );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8556")
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
