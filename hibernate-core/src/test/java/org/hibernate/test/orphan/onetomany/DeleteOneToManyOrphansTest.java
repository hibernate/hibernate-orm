/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.onetomany;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badnmer
 */
public class DeleteOneToManyOrphansTest extends BaseCoreFunctionalTestCase {

	private void createData() {
		Session s = openSession();
		s.getTransaction().begin();

		Feature newFeature = new Feature();
		newFeature.setName("Feature 1");
		s.persist( newFeature );

		Product product = new Product();
		newFeature.setProduct( product );
		product.getFeatures().add( newFeature );
		s.persist( product );

		s.getTransaction().commit();
		s.clear();

	}

	private void cleanupData() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Feature" ).executeUpdate();
		session.createQuery( "delete Product" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9330")
	public void testOrphanedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Feature" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Product" ).list();
		assertEquals( 1, results.size() );
		Product product = ( Product ) results.get( 0 );
		assertEquals( 1, product.getFeatures().size() );
		product.getFeatures().clear();
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		product = ( Product ) session.get( Product.class, product.getId() );
		assertEquals( 0, product.getFeatures().size() );
		results = session.createQuery( "from Feature" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Product" ).list();
		assertEquals( 1, results.size() );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9330")
	public void testOrphanedWhileManagedMergeOwner() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Feature" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Product" ).list();
		assertEquals( 1, results.size() );
		Product product = ( Product ) results.get( 0 );
		assertEquals( 1, product.getFeatures().size() );
		product.getFeatures().clear();
		session.merge( product );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		product = ( Product ) session.get( Product.class, product.getId() );
		assertEquals( 0, product.getFeatures().size() );
		results = session.createQuery( "from Feature" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Product" ).list();
		assertEquals( 1, results.size() );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9330")
	public void testReplacedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Feature" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Product" ).list();
		assertEquals( 1, results.size() );
		Product product = ( Product ) results.get( 0 );
		assertEquals( 1, product.getFeatures().size() );

		// Replace with a new Feature instance
		product.getFeatures().remove( 0 );
		Feature featureNew = new Feature();
		featureNew.setName( "Feature 2" );
		featureNew.setProduct( product );
		product.getFeatures().add( featureNew );
		session.persist( featureNew );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		results = session.createQuery( "from Feature" ).list();
		assertEquals( 1, results.size() );
		Feature featureQueried = (Feature) results.get( 0 );
		assertEquals( featureNew.getId(), featureQueried.getId() );
		results = session.createQuery( "from Product" ).list();
		assertEquals( 1, results.size() );
		Product productQueried =  (Product) results.get( 0 );
		assertEquals( 1, productQueried.getFeatures().size() );
		assertEquals( featureQueried, productQueried.getFeatures().get( 0 ) );

		session.getTransaction().commit();
		session.close();

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
