/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.orphan.onetomany;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class DeleteOneToManyOrphansTest extends BaseEntityManagerFunctionalTestCase {

	private void createData() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		Feature newFeature = new Feature();
		newFeature.setName("Feature 1");
		entityManager.persist( newFeature );

		Product product = new Product();
		newFeature.setProduct( product );
		product.getFeatures().add( newFeature );
		entityManager.persist( product );

		entityManager.getTransaction().commit();
		entityManager.clear();

	}

	private void cleanupData() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		entityManager.createQuery( "delete Feature" ).executeUpdate();
		entityManager.createQuery( "delete Product" ).executeUpdate();
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9568")
	@FailureExpected( jiraKey = "HHH-9568" )
	public void testOrphanedWhileManaged() {
		createData();

		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		List results = entityManager.createQuery( "from Feature" ).getResultList();
		assertEquals( 1, results.size() );
		results = entityManager.createQuery( "from Product" ).getResultList();
		assertEquals( 1, results.size() );
		Product product = ( Product ) results.get( 0 );
		assertEquals( 1, product.getFeatures().size() );
		product.getFeatures().clear();
		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		product = entityManager.find( Product.class, product.getId() );
		assertEquals( 0, product.getFeatures().size() );
		results = entityManager.createQuery( "from Feature" ).getResultList();
		assertEquals( 0, results.size() );
		results = entityManager.createQuery( "from Product" ).getResultList();
		assertEquals( 1, results.size() );

		entityManager.getTransaction().commit();
		entityManager.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9568")
	@FailureExpected( jiraKey = "HHH-9568" )
	public void testOrphanedWhileManagedMergeOwner() {
		createData();

		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		List results = entityManager.createQuery( "from Feature" ).getResultList();
		assertEquals( 1, results.size() );
		results = entityManager.createQuery( "from Product" ).getResultList();
		assertEquals( 1, results.size() );
		Product product = ( Product ) results.get( 0 );
		assertEquals( 1, product.getFeatures().size() );
		product.getFeatures().clear();
		entityManager.merge( product );
		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();

		product = entityManager.find( Product.class, product.getId() );
		assertEquals( 0, product.getFeatures().size() );
		results = entityManager.createQuery( "from Feature" ).getResultList();
		assertEquals( 0, results.size() );
		results = entityManager.createQuery( "from Product" ).getResultList();
		assertEquals( 1, results.size() );

		entityManager.getTransaction().commit();
		entityManager.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9568")
	@FailureExpected( jiraKey = "HHH-9568" )
	public void testReplacedWhileManaged() {
		createData();

		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		List results = entityManager.createQuery( "from Feature" ).getResultList();
		assertEquals( 1, results.size() );
		results = entityManager.createQuery( "from Product" ).getResultList();
		assertEquals( 1, results.size() );
		Product product = ( Product ) results.get( 0 );
		assertEquals( 1, product.getFeatures().size() );

		// Replace with a new Feature instance
		product.getFeatures().remove( 0 );
		Feature featureNew = new Feature();
		featureNew.setName( "Feature 2" );
		featureNew.setProduct( product );
		product.getFeatures().add( featureNew );
		entityManager.persist( featureNew );
		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		results = entityManager.createQuery( "from Feature" ).getResultList();
		assertEquals( 1, results.size() );
		Feature featureQueried = (Feature) results.get( 0 );
		assertEquals( featureNew.getId(), featureQueried.getId() );
		results = entityManager.createQuery( "from Product" ).getResultList();
		assertEquals( 1, results.size() );
		Product productQueried =  (Product) results.get( 0 );
		assertEquals( 1, productQueried.getFeatures().size() );
		assertEquals( featureQueried, productQueried.getFeatures().get( 0 ) );

		entityManager.getTransaction().commit();
		entityManager.close();

		cleanupData();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Product.class,
				Feature.class
		};
	}
}
