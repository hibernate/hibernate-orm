/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				NestedEmbeddableWithLockingDeletionTest.Product.class,

		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "20")
		}
)
@JiraKey("HHH-16959")
public class NestedEmbeddableWithLockingDeletionTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void shouldDeleteAllProducts(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypeOneBenefit typeOneBenefit = new TypeOneBenefit( BigDecimal.TEN );
					Product product = new Product( "ID1", new Benefits( typeOneBenefit, null ), "test" );
					session.persist( product );

					Product product2 = new Product( "ID2", new Benefits( null, "data" ) );

					session.persist( product2 );

					Product product3 = new Product( "ID3", null, "test" );
					session.persist( product3 );
				}
		);

		deleteAllProducts( scope );

		assertProductsHaveBeenDeleted( scope );
	}

	@Test
	public void shouldDeleteOneProduct1(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product product = new Product( "ID1", new Benefits( null, "data" ) );
					session.persist( product );
				}
		);

		deleteAllProducts( scope );

		assertProductsHaveBeenDeleted( scope );

	}

	@Test
	public void shouldDeleteOneProduct2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product product = new Product( "ID1", null, "test" );
					session.persist( product );

				}
		);

		deleteAllProducts( scope );

		assertProductsHaveBeenDeleted( scope );
	}

	@Test
	public void shouldDeleteOneProduct3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypeOneBenefit typeOneBenefit = new TypeOneBenefit( BigDecimal.TEN );
					Product product = new Product( "ID1", new Benefits( typeOneBenefit, null ), "test" );
					session.persist( product );
				}
		);

		deleteAllProducts( scope );

		assertProductsHaveBeenDeleted( scope );

	}

	private static void deleteAllProducts(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Product> products = session.createQuery(
							"select p from Product p",
							Product.class
					).list();

					products.forEach(
							p -> {
								session.remove( p );
							}
					);
				}
		);
	}

	private static void assertProductsHaveBeenDeleted(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Product> products = session.createQuery( "select  p from Product p", Product.class ).list();
					assertThat( products.size() ).isEqualTo( 0 );
				}
		);
	}

	@IdClass(ProductPK.class)
	@Entity(name = "Product")
	@OptimisticLocking(type = OptimisticLockType.ALL)
	@DynamicUpdate
	@Table(name = "PRODUCTS")
	public static class Product {

		@Id
		private String productId;


		private String description;

		@Embedded
		private Benefits benefits;

		public Product() {
		}

		public Product(String productId) {
			this.productId = productId;
		}

		public Product(String productId, Benefits benefits) {
			this.productId = productId;
			this.benefits = benefits;
		}

		public Product(String productId, Benefits benefits, String description) {
			this.productId = productId;
			this.benefits = benefits;
			this.description = description;
		}
	}

	public static class ProductPK implements Serializable {
		private String productId;

		public ProductPK() {
		}

		public ProductPK(String productId) {
			this.productId = productId;
		}
	}

	@Embeddable
	public static class Benefits {

		@Embedded
		TypeOneBenefit credit;

		String data;

		public Benefits() {
		}

		public Benefits(TypeOneBenefit credit, String data) {
			this.credit = credit;
			this.data = data;
		}
	}

	@Embeddable
	public static class TypeOneBenefit {
		@Column(name = "BENEFIT_ONE_BASE_AMOUNT")
		BigDecimal baseAmount;

		public TypeOneBenefit() {
		}

		public TypeOneBenefit(BigDecimal baseAmount) {
			this.baseAmount = baseAmount;
		}
	}

}
