/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				ForeignGeneratorTest.Product.class,
				ForeignGeneratorTest.ProductDetails.class
		}
)
@SessionFactory
public class ForeignGeneratorTest {

	@Test
	@JiraKey(value = "HHH-13456")
	public void testForeignGeneratorInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( statelessSession -> {

			Product product = new Product();

			ProductDetails productDetails = new ProductDetails( product );

			statelessSession.insert( productDetails );
		} );
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

		public ProductDetails(Product product) {
			this.product = product;
		}
	}
}
