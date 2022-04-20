/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.orphan.onetomany;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badnmer
 */
@DomainModel(
		annotatedClasses = {
				Product.class,
				Feature.class
		}
)
@SessionFactory
public class DeleteOneToManyOrphansTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Feature newFeature = new Feature();
					newFeature.setName( "Feature 1" );
					session.persist( newFeature );

					Product product = new Product();
					newFeature.setProduct( product );
					product.getFeatures().add( newFeature );
					session.persist( product );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete Feature" ).executeUpdate();
					session.createQuery( "delete Product" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9330")
	public void testOrphanedWhileManaged(SessionFactoryScope scope) {

		Product p = scope.fromTransaction(
				session -> {
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
					Product product = (Product) results.get( 0 );
					assertEquals( 1, product.getFeatures().size() );
					product.getFeatures().clear();
					return product;
				}
		);

		scope.inTransaction(
				session -> {
					Product product = ( session.get( Product.class, p.getId() ) );
					assertEquals( 0, product.getFeatures().size() );
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9330")
	public void testOrphanedWhileManagedMergeOwner(SessionFactoryScope scope) {

		Product p = scope.fromTransaction(
				session -> {
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
					Product product = (Product) results.get( 0 );
					assertEquals( 1, product.getFeatures().size() );
					product.getFeatures().clear();
					session.merge( product );
					return product;
				}
		);

		scope.inTransaction(
				session -> {
					Product product = session.get( Product.class, p.getId() );
					assertEquals( 0, product.getFeatures().size() );
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
				}
		);

	}

	@Test
	public void testOrphanedWhileQueued(SessionFactoryScope scope) {

		Product p = scope.fromTransaction(
				session -> {
					// given...
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
					Product product = (Product) results.get( 0 );
					assertEquals( 1, product.getFeatures().size() );

					// when...
					// ... "Feature 2" added
					Feature f2 = new Feature(product);
					f2.setName("Feature 2");
					session.persist(f2);

					product.getFeatures().add(f2);

					// ... executing a query queues the entry into session.actionQueue.inserts,
					// but as auto-flush sees that product is queried and not features, it doesn't flush them
					session.createQuery( "from Product" ).list();

					// ... test will only be green with an explicit flush here
					// session.flush();

					// ... "Feature 2" removed, expecting it to be orphan-removed from the database
					product.getFeatures().remove(f2);

					// ... "Feature 3" added
					Feature f3 = new Feature(product);
					f3.setName("Feature 3");
					session.persist( f3 );
  				    product.getFeatures().add(f3);

					return product;
				}
		);

		scope.inTransaction(
				session -> {
					Product product = session.get( Product.class, p.getId() );
					// if there are 3 elements, the orphan removal didn't work as expected
					assertEquals( 2, product.getFeatures().size() );
					assertTrue( product.getFeatures().stream().anyMatch(feature -> feature.getName().equals("Feature 3")) );
				}
		);

	}

	@Test
	@TestForIssue(jiraKey = "HHH-9330")
	public void testReplacedWhileManaged(SessionFactoryScope scope) {

		Feature f = scope.fromTransaction(
				session -> {
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
					Product product = (Product) results.get( 0 );
					assertEquals( 1, product.getFeatures().size() );

					// Replace with a new Feature instance
					product.getFeatures().remove( 0 );
					Feature featureNew = new Feature();
					featureNew.setName( "Feature 2" );
					featureNew.setProduct( product );
					product.getFeatures().add( featureNew );
					session.persist( featureNew );
					return featureNew;
				}
		);

		scope.inTransaction(
				session -> {
					List results = session.createQuery( "from Feature" ).list();
					assertEquals( 1, results.size() );
					Feature featureQueried = (Feature) results.get( 0 );
					assertEquals( f.getId(), featureQueried.getId() );
					results = session.createQuery( "from Product" ).list();
					assertEquals( 1, results.size() );
					Product productQueried = (Product) results.get( 0 );
					assertEquals( 1, productQueried.getFeatures().size() );
					assertEquals( featureQueried, productQueried.getFeatures().get( 0 ) );
				}
		);
	}

}
