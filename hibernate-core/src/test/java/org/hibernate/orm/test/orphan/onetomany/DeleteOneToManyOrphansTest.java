/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.onetomany;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-9330")
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
	@JiraKey(value = "HHH-9330")
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
	@JiraKey(value = "HHH-9330")
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
