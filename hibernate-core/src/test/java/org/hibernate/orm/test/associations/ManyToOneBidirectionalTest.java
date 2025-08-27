/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import java.util.Arrays;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;

@DomainModel(
		annotatedClasses = {
				ManyToOneBidirectionalTest.OrderEntity.class,
				ManyToOneBidirectionalTest.OrderItem.class,
				ManyToOneBidirectionalTest.Product.class,
				ManyToOneBidirectionalTest.Sku.class,
		}
)
@SessionFactory
@JiraKey("HHH-17140")
public class ManyToOneBidirectionalTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product p = new Product( 1l, "abc" );

					Sku sku = new Sku( 1l, "sku", p );

					OrderItem item = new OrderItem( 1L, sku, p );

					OrderEntity orderEntity = new OrderEntity( 1l );
					orderEntity.setItems( Arrays.asList( item ) );

					session.persist( p );
					session.persist( sku );
					session.persist( item );
					session.persist( orderEntity );
				}
		);
	}

	@Test
	public void testFindFolloweByAGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					OrderEntity order = session.find( OrderEntity.class, 1l );
					order.getItems().get( 0 );
					Sku responseItem = session.get( Sku.class, 1l );
				}
		);
	}

	@Entity(name = "OrderEntity")
	public static class OrderEntity {

		@Id
		Long id;

		@OneToMany
		List<OrderItem> items;

		public OrderEntity() {
		}

		public OrderEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<OrderItem> getItems() {
			return items;
		}

		public void setItems(List<OrderItem> items) {
			this.items = items;
		}
	}

	@Entity(name = "OrderItem")
	public static class OrderItem {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "SKU_ID", nullable = false)
		private Sku sku;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "PRODUCT_ID")
		protected Product product;

		public OrderItem() {
		}

		public OrderItem(Long id, Sku sku, Product product) {
			this.id = id;
			this.sku = sku;
			this.product = product;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Sku getSku() {
			return sku;
		}

		public void setSku(Sku sku) {
			this.sku = sku;
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "DEFAULT_SKU_ID")
		private Sku defaultSku;

		public Product() {
		}

		public Product(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Sku getDefaultSku() {
			return defaultSku;
		}

		public void setDefaultSku(Sku defaultSku) {
			this.defaultSku = defaultSku;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Sku")
	public static class Sku {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "DEFAULT_PRODUCT_ID", foreignKey = @ForeignKey(NO_CONSTRAINT))
		protected Product defaultProduct;

		public Sku() {
		}

		public Sku(Long id, String name, Product product) {
			this.id = id;
			this.name = name;
			this.defaultProduct = product;
			defaultProduct.setDefaultSku( this );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Product getDefaultProduct() {
			return defaultProduct;
		}

		public void setDefaultProduct(Product defaultProduct) {
			this.defaultProduct = defaultProduct;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
