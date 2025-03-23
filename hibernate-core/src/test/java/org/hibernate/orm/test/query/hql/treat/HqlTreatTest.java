/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.treat;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Jpa(
		annotatedClasses = {
				HqlTreatTest.Product.class,
				HqlTreatTest.SoftwareProduct.class,
				HqlTreatTest.LineItem.class
		}
)
public class HqlTreatTest {


	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Product product1 = new Product( 1, "Monitor" );
					Product product2 = new SoftwareProduct( 2, "Linux" );
					LineItem lineItem1 = new LineItem( 1, 3, product1 );
					LineItem lineItem2 = new LineItem( 2, 5, product2 );
					entityManager.persist( product1 );
					entityManager.persist( product2 );
					entityManager.persist( lineItem1 );
					entityManager.persist( lineItem2 );
				}
		);
	}


	@Test
	public void treatJoinClassTest(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					List<String> names = entityManager.createQuery(
									"SELECT s.name FROM LineItem l JOIN TREAT(l.product AS SoftwareProduct) s" )
							.getResultList();
					assertEquals( 1, names.size() );
					assertEquals( "Linux", names.get( 0 ) );
				}
		);
	}


	@Entity(name = "LineItem")
	@Table(name = "LINEITEM_TABLE")
	public static class LineItem {

		@Id
		private Integer id;

		private int quantity;

		@ManyToOne
		private Product product;

		public LineItem() {
		}

		public LineItem(Integer id, int quantity, Product product) {
			this.id = id;
			this.quantity = quantity;
			this.product = product;
		}
	}

	@Entity(name = "Product")
	@Table(name = "PRODUCT_TABLE")
	@SecondaryTables({
			@SecondaryTable(name = "PRODUCT_DETAILS", pkJoinColumns = @PrimaryKeyJoinColumn(name = "ID"))
	})
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "PRODUCT_TYPE", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue("Product")
	public static class Product {

		@Id
		private Integer id;

		private String name;

		public Product() {
		}

		public Product(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "SoftwareProduct")
	@DiscriminatorValue("SW")
	public static class SoftwareProduct extends Product {

		public SoftwareProduct() {
		}

		public SoftwareProduct(Integer id, String name) {
			super( id, name );
		}
	}

}
