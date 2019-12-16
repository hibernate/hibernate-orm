package org.hibernate.test.id;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Nathan Xu
 */
public class ForeignGeneratorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Product.class,
			ProductDetails.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13456")
	public void testForeignGeneratorInStatelessSession() {

		inStatelessSession(statelessSession -> {

			Transaction tx = statelessSession.beginTransaction();

			Product product = new Product();

			ProductDetails productDetails = new ProductDetails( product );

			statelessSession.insert( productDetails );

			tx.commit();
		});
	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "ProductDetails")
	public static class ProductDetails {

		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@MapsId
		private Product product;

		public ProductDetails() {
		}

		public ProductDetails( Product product ) {
			this.product = product;
		}
	}
}
