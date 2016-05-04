/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Customer_;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ManipulationCriteriaTest extends AbstractMetamodelSpecificTest {
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
			updateCriteria.set( Customer_.name, "Acme" );
			updateCriteria.where(
					builder.equal(
							root.get( Customer_.name ),
							"Acme"
					)
			);
			em.createQuery( updateCriteria ).executeUpdate();
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
	@TestForIssue(jiraKey = "HHH-8434")
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
	@SkipForDialect(MySQLDialect.class)
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
