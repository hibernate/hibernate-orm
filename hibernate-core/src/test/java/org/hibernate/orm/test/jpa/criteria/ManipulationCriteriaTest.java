/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class ManipulationCriteriaTest extends AbstractMetamodelSpecificTest {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_COMPLIANCE, "true" );
	}

	@Test
	public void basicTest() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		{
			CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
			deleteCriteria.from( Customer.class );
			em.createQuery( deleteCriteria ).executeUpdate();
		}

		{
			CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
			Root<Customer> root = deleteCriteria.from( Customer.class );
			deleteCriteria.where(
					builder.equal(
							root.get( Customer_.name ),
							"Acme"
					)
			);
			em.createQuery( deleteCriteria ).executeUpdate();
		}

		{
			CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
			updateCriteria.from( Customer.class );
			updateCriteria.set( Customer_.name, "Acme" );
			em.createQuery( updateCriteria ).executeUpdate();
		}

		{
			CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
			Root<Customer> root = updateCriteria.from( Customer.class );
			updateCriteria.set( Customer_.age, 23 );
			updateCriteria.where(
					builder.equal(
							root.get( Customer_.name ),
							"Acme"
					)
			);
			em.createQuery( updateCriteria ).executeUpdate();
		}

		{
			CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
			Root<Customer> root = deleteCriteria.from( Customer.class );
			deleteCriteria.where(
					builder.equal(
							root.get( Customer_.name ),
							"Acme"
					),
					builder.equal(
							root.get(Customer_.age),
							23
					)
			);
			em.createQuery( deleteCriteria ).executeUpdate();
		}

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testNoAssignments() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		try {
			CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
			updateCriteria.from( Customer.class );
			em.createQuery( updateCriteria ).executeUpdate();
			fail( "Expecting failure due to no assignments" );
		}
		catch (IllegalArgumentException iae) {
			// expected
		}

		// changed to rollback since HHH-8442 causes transaction to be marked for rollback only
		assertTrue( em.getTransaction().getRollbackOnly() );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-8434")
	public void basicMultipleAssignments() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
		updateCriteria.from( Customer.class );
		updateCriteria.set( Customer_.name, "Bob" );
		updateCriteria.set( Customer_.age, 99 );
		em.createQuery( updateCriteria ).executeUpdate();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testJoinsAndFetchesDisallowed() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		{
			try {
				CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
				Root<Customer> root = deleteCriteria.from( Customer.class );
				root.join( Customer_.spouse );
				em.createQuery( deleteCriteria ).executeUpdate();
				fail( "Expected failure dues to attempt to join" );
			}
			catch (IllegalArgumentException expected) {
			}
		}

		{
			try {
				CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
				Root<Customer> root = deleteCriteria.from( Customer.class );
				root.fetch( Customer_.spouse );
				em.createQuery( deleteCriteria ).executeUpdate();
				fail( "Expected failure dues to attempt to fetch" );
			}
			catch (IllegalArgumentException expected) {
			}
		}

		{
			try {
				CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
				Root<Customer> root = updateCriteria.from( Customer.class );
				root.join( Customer_.spouse );
				em.createQuery( updateCriteria ).executeUpdate();
				fail( "Expected failure dues to attempt to join" );
			}
			catch (IllegalArgumentException expected) {
			}
		}

		{
			try {
				CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
				Root<Customer> root = updateCriteria.from( Customer.class );
				root.fetch( Customer_.spouse );
				em.createQuery( updateCriteria ).executeUpdate();
				fail( "Expected failure dues to attempt to fetch" );
			}
			catch (IllegalArgumentException expected) {
			}
		}

		em.getTransaction().commit();
		em.close();

	}

	@Test
	// MySQL does not allow "delete/update from" and subqueries to use the same table
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true)
	public void testDeleteWithUnCorrelatedSubquery() {
		CriteriaBuilder builder = entityManagerFactory().getCriteriaBuilder();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// attempt to delete Customers who's age is less than the AVG age
		CriteriaDelete<Customer> criteria = builder.createCriteriaDelete( Customer.class );
		Root<Customer> customerRoot = criteria.from( Customer.class );

		Subquery<Double> subCriteria = criteria.subquery( Double.class );
		Root<Customer> subQueryCustomerRoot = subCriteria.from( Customer.class );
		subCriteria.select( builder.avg( subQueryCustomerRoot.get( Customer_.age ) ) );

		// also illustrates the new capability to use the subquery selection as an expression!
		criteria.where(
				builder.lessThan(
						customerRoot.get( Customer_.age ),
						subCriteria.getSelection().as( Integer.class )
				)
		);

		// make sure Subquery#getParent fails...
		try {
			subCriteria.getParent();
			fail( "Expecting Subquery.getParent call to fail on DELETE containing criteria" );
		}
		catch (IllegalStateException expected) {
		}

		Query query = em.createQuery( criteria );
		try {
			// first, make sure an attempt to list fails
			query.getResultList();
			fail( "Attempt to getResultList() on delete criteria should have failed" );
		}
		catch (IllegalStateException expected) {
		}
		query.executeUpdate();

		em.getTransaction().commit();
		em.close();
	}
}
