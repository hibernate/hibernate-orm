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

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Customer_;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Order_;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class UncorrelatedSubqueryTest extends AbstractMetamodelSpecificTest {
	@Test
	public void testGetCorrelatedParentIllegalStateException() {
		// test that attempting to call getCorrelatedParent on a uncorrelated query/subquery
		// throws ISE

		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

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

		try {
			customerRoot.getCorrelationParent();
			fail( "Should have resulted in IllegalStateException" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			subqueryOrderRoot.getCorrelationParent();
			fail( "Should have resulted in IllegalStateException" );
		}
		catch (IllegalStateException expected) {
		}

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEqualAll() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaQuery<Customer> criteria = builder.createQuery( Customer.class );
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Join<Customer, Order> orderJoin = customerRoot.join( Customer_.orders );
		criteria.select( customerRoot );
		Subquery<Double> subCriteria = criteria.subquery( Double.class );
		Root<Order> subqueryOrderRoot = subCriteria.from( Order.class );
		subCriteria.select( builder.min( subqueryOrderRoot.get( Order_.totalPrice ) ) );
		criteria.where( builder.equal( orderJoin.get( "totalPrice" ), builder.all( subCriteria ) ) );
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testLessThan() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

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
		em.createQuery( criteria ).getResultList();

		em.getTransaction().commit();
		em.close();
	}
}
