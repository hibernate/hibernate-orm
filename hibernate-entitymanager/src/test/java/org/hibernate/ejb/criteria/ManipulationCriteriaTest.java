/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Customer;
import org.hibernate.ejb.metamodel.Customer_;

import org.junit.Test;

import static org.junit.Assert.fail;

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
		catch (IllegalStateException ise) {
			// expected
		}

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
}
