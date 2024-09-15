/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.internal.util;

import java.util.stream.Stream;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.TypedQuery;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dragoş Haiduc
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-14231" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class )
@Jpa( annotatedClasses = ScrollableResultsObjectArrayCastingTest.Product.class )
public class ScrollableResultsObjectArrayCastingTest {

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> {
					Product product = new Product();
					product.binaryValue = new byte[] { 1, 2, 3 };
					entityManager.persist( product );
				}
		);
	}

	@AfterEach
	public void dropTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> entityManager.createQuery( "delete Product" ).executeUpdate()
		);
	}


	@Test
	public void testNoClassCastExceptionThrown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> {
					TypedQuery<byte[]> typedQuery = entityManager.createQuery( "select p.binaryValue from Product p", byte[].class );
					try (Stream<byte[]> stream = typedQuery.getResultStream()) {
						//noinspection ResultOfMethodCallIgnored
						stream.findFirst();
					}
				}
		);
	}

	@SuppressWarnings("unused")
	@Entity(name = "Product")
	public static class Product {

		@Id @GeneratedValue
		Integer id;

		String name;

		@Lob
		byte[] binaryValue;
	}

}
