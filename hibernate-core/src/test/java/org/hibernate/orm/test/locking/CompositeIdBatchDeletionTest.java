/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.io.Serializable;
import java.util.ArrayList;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CompositeIdBatchDeletionTest.Operator.class,
				CompositeIdBatchDeletionTest.Product.class,
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2")
		}
)
@JiraKey("HHH-16810")
public class CompositeIdBatchDeletionTest {
	private final static String PRODUCT_ID = "ID";
	private final static String PRODUCT_ID_2 = "ID2";
	private final static String OPERATOR_ID = "operatorID";
	private final static String OPERATOR_ID_2 = "operatorID2";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Operator operator = new Operator( OPERATOR_ID );
					Operator operator2 = new Operator( OPERATOR_ID_2 );
					Product product = new Product( PRODUCT_ID, operator, "test" );
					Product product2 = new Product( PRODUCT_ID_2, operator, "test 2" );
					session.persist( operator );
					session.persist( operator2 );
					session.persist( product );
					session.persist( product2 );

				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		ProductPK productPK = new ProductPK( PRODUCT_ID, OPERATOR_ID );
		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, productPK );
					session.remove( product );
				}
		);

		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, productPK );
					assertThat( product ).isNull();
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		ProductPK productPK = new ProductPK( PRODUCT_ID, OPERATOR_ID );

		String newDescription = "new description";

		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, productPK );
					product.setDescription( newDescription );
				}
		);

		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, productPK );
					assertThat( product ).isNotNull();
					assertThat( product.getDescription() ).isEqualTo( newDescription );
				}
		);
	}

	@Test
	public void testDelete2(SessionFactoryScope scope) {

		ProductPK productPK = new ProductPK( PRODUCT_ID, OPERATOR_ID );
		ProductPK productPK2 = new ProductPK( PRODUCT_ID_2, OPERATOR_ID_2 );
		scope.inTransaction(
				session -> {
					session.getReference( Product.class, productPK2 );
					Product product = session.find( Product.class, productPK );
					session.remove( product );
				}
		);

		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, productPK );
					assertThat( product ).isNull();
				}
		);
	}

	@IdClass(ProductPK.class)
	@Entity(name = "Product")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@Table(name = "PRODUCTS")
	public static class Product {

		@Id
		private String productId;

		@Id
		@ManyToOne(fetch = LAZY)
		@JoinColumn(nullable = false)
		private Operator operator;

		private String description;

		public Product() {
		}

		public Product(String productId, Operator operator, String description) {
			this.productId = productId;
			this.operator = operator;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class ProductPK implements Serializable {
		private String productId;
		private String operator;

		public ProductPK() {
		}

		public ProductPK(String productId, String operator) {
			this.productId = productId;
			this.operator = operator;
		}
	}

	@Entity(name = "Operator")
	@Table(name = "OPERATORS")
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	public static class Operator {

		@Id
		private String operatorId;

		@OneToMany(mappedBy = "operator")
		private List<Product> products = new ArrayList<>();

		public Operator() {
		}

		public Operator(String operatorId) {
			this.operatorId = operatorId;
		}

		public void setProducts(List<Product> products) {
			this.products = products;
		}
	}
}
