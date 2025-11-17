/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@Jpa(annotatedClasses = {
		Product.class,
		Feature.class
})
public class DeleteOneToManyOrphansTest {

	@BeforeEach
	public void createData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Feature newFeature = new Feature();
					newFeature.setName("Feature 1");
					entityManager.persist( newFeature );

					Product product = new Product();
					newFeature.setProduct( product );
					product.getFeatures().add( newFeature );
					entityManager.persist( product );
				}
		);
	}

	@AfterEach
	public void cleanupData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-9568")
//	@FailureExpected( jiraKey = "HHH-9568" )
	public void testOrphanedWhileManaged(EntityManagerFactoryScope scope) {
		Long productId = scope.fromTransaction(
				entityManager -> {
					List results = entityManager.createQuery( "from Feature" ).getResultList();
					assertEquals( 1, results.size() );
					results = entityManager.createQuery( "from Product" ).getResultList();
					assertEquals( 1, results.size() );
					Product product = ( Product ) results.get( 0 );
					assertEquals( 1, product.getFeatures().size() );
					product.getFeatures().clear();
					return product.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Product _product = entityManager.find( Product.class, productId );
					assertEquals( 0, _product.getFeatures().size() );
					List results = entityManager.createQuery( "from Feature" ).getResultList();
					assertEquals( 0, results.size() );
					results = entityManager.createQuery( "from Product" ).getResultList();
					assertEquals( 1, results.size() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9568")
//	@FailureExpected( jiraKey = "HHH-9568" )
	public void testOrphanedWhileManagedMergeOwner(EntityManagerFactoryScope scope) {
		Long productId = scope.fromTransaction(
				entityManager -> {
					List results = entityManager.createQuery( "from Feature" ).getResultList();
					assertEquals( 1, results.size() );
					results = entityManager.createQuery( "from Product" ).getResultList();
					assertEquals( 1, results.size() );
					Product product = ( Product ) results.get( 0 );
					assertEquals( 1, product.getFeatures().size() );
					product.getFeatures().clear();
					entityManager.merge( product );
					return product.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Product _product = entityManager.find( Product.class, productId );
					assertEquals( 0, _product.getFeatures().size() );
					List results = entityManager.createQuery( "from Feature" ).getResultList();
					assertEquals( 0, results.size() );
					results = entityManager.createQuery( "from Product" ).getResultList();
					assertEquals( 1, results.size() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9568")
//	@FailureExpected( jiraKey = "HHH-9568" )
	public void testReplacedWhileManaged(EntityManagerFactoryScope scope) {
		Long featureNewId = scope.fromTransaction(
				entityManager -> {
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
					return featureNew.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					List results = entityManager.createQuery( "from Feature" ).getResultList();
					assertEquals( 1, results.size() );
					Feature featureQueried = (Feature) results.get( 0 );
					assertEquals( featureNewId, featureQueried.getId() );
					results = entityManager.createQuery( "from Product" ).getResultList();
					assertEquals( 1, results.size() );
					Product productQueried =  (Product) results.get( 0 );
					assertEquals( 1, productQueried.getFeatures().size() );
					assertEquals( featureQueried, productQueried.getFeatures().get( 0 ) );
				}
		);
	}
}
