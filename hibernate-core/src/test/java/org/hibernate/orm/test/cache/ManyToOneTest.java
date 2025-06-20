/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;

@DomainModel(
		annotatedClasses = {
				ManyToOneTest.Product.class,
				ManyToOneTest.Operator.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
		}
)
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-16673")
public class ManyToOneTest {

	private static final String ID = "ID";
	private static final String OPERATOR_ID = "operatorID";
	private static final ProductPK PRODUCT_PK = new ProductPK( ID, OPERATOR_ID );

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Operator operator = new Operator( OPERATOR_ID );
					Product product = new Product( ID, operator, "test" );

					session.persist( operator );
					session.persist( product );
					session.getFactory().getCache().evictEntityData();
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}


	@Test
	public void testFind(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, PRODUCT_PK );
					assertThat( product ).isNotNull();
					Operator operator = product.getOperator();
					assertThat( operator ).isNotNull();
					assertThat( operator.getOperatorId() ).isEqualTo( OPERATOR_ID );
					// Operator has not been initialized, only the Product has been cached
					assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 1 );
				}
		);

		statistics.clear();

		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, PRODUCT_PK );
					assertThat( product ).isNotNull();
					Operator operator = product.getOperator();
					assertThat( operator ).isNotNull();
					assertThat( operator.getOperatorId() ).isEqualTo( OPERATOR_ID );
					assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
					assertThat( statistics.getSecondLevelCacheMissCount() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, PRODUCT_PK );
					assertThat( product ).isNotNull();
					Operator operator = product.getOperator();
					assertThat( operator ).isNotNull();
					assertThat( operator.getOperatorId() ).isEqualTo( OPERATOR_ID );
					operator.getName();
					// Operator has been initialized, both Product and Operator have been cached
					assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 2 );
				}
		);

		statistics.clear();

		scope.inTransaction(
				session -> {
					Product product = session.find( Product.class, PRODUCT_PK );
					assertThat( product ).isNotNull();
					Operator operator = product.getOperator();
					assertThat( operator ).isNotNull();
					assertThat( operator.getOperatorId() ).isEqualTo( OPERATOR_ID );
					assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2 );
					assertThat( statistics.getSecondLevelCacheMissCount() ).isEqualTo( 0 );
				}
		);
	}

	@Entity(name = "Product")
	@IdClass(ProductPK.class)
	@OptimisticLocking(type = OptimisticLockType.ALL)
	@DynamicUpdate
	@Cacheable
	@Cache(usage = READ_WRITE)
	@Table(name = "PRODUCT_TABLE")
	public static class Product {

		@Id
		@Column(name = "PRODUCT_ID", nullable = false)
		private String productId;

		@Id
		@ManyToOne(fetch = LAZY)
		@JoinColumn
		private Operator operator;

		@Column(name = "DESCRIPTION")
		private String description;

		public Product() {
		}

		public Product(String productId, Operator operator, String description) {
			this.productId = productId;
			this.operator = operator;
			this.description = description;
		}

		public String getProductId() {
			return productId;
		}

		public Operator getOperator() {
			return operator;
		}

		public String getDescription() {
			return description;
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
	@Table(name = "OPERATOR_TABLE")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@Cacheable
	@Cache(usage = READ_WRITE)
	public static class Operator {

		@Id
		@Column(name = "OPERATOR_ID", nullable = false)
		private String operatorId;

		private String name;

		@OneToMany(mappedBy = "operator", cascade = { CascadeType.ALL }, orphanRemoval = true)
		private List<Product> products = new ArrayList<>();

		public Operator() {
		}

		public Operator(String operatorId) {
			this.operatorId = operatorId;
		}

		public String getOperatorId() {
			return operatorId;
		}

		public String getName() {
			return name;
		}

		public List<Product> getProducts() {
			return products;
		}
	}

}
