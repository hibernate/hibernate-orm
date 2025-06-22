/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Mainly a test for testing compliance with the fact that "identification variables" (aliases) need to
 * be treated as case-insensitive according to JPA.
 *
 * @author Steve Ebersole
 */
public class IdentificationVariablesTest extends AbstractJPATest {

	@Test
	public void testUsageInSelect() {
		inSession(
				session ->
						session.createQuery( "select I from Item i" ).list()
		);
	}

	@Test
	public void testUsageInPath() {
		inSession(
				session ->
						session.createQuery( "select I from Item i where I.name = 'widget'" ).list()
		);
	}

	@Test
	public void testMixedTckUsage() {
		inSession(
				session ->
						session.createQuery( "Select DISTINCT OBJECT(P) from Product p where P.quantity < 10" ).list()
		);
	}

	@Test
	public void testUsageInJpaInCollectionSyntax() {
		inSession(
				session ->
						session.createQuery(
										"SELECT DISTINCT object(i) FROM Item I, IN(i.parts) ip where ip.stockNumber = '123'" )
								.list()
		);
	}

	@Test
	public void testUsageInDistinct() {
		inSession(
				session ->
						session.createQuery( "select distinct(I) from Item i" ).list()
		);
	}

	@Test
	public void testUsageInSelectObject() {
		inSession(
				session ->
						session.createQuery( "select OBJECT(I) from Item i" ).list()
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Product.class };
	}

	@Entity(name = "Product")
	@Table(name = "PROD")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "PRODUCT_TYPE", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue("Product")
	public static class Product {
		private String id;
		private String name;
		private double price;
		private int quantity;
		private long partNumber;

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "NAME")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Column(name = "PRICE")
		public double getPrice() {
			return price;
		}

		public void setPrice(double price) {
			this.price = price;
		}

		@Column(name = "QUANTITY")
		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int v) {
			this.quantity = v;
		}

		@Column(name = "PNUM")
		public long getPartNumber() {
			return partNumber;
		}

		public void setPartNumber(long v) {
			this.partNumber = v;
		}
	}
}
