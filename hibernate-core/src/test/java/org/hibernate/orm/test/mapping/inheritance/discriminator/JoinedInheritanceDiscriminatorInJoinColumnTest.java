/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SourceType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		JoinedInheritanceDiscriminatorInJoinColumnTest.Product.class,
		JoinedInheritanceDiscriminatorInJoinColumnTest.Operator.class,
		JoinedInheritanceDiscriminatorInJoinColumnTest.FixedProduct.class,
		JoinedInheritanceDiscriminatorInJoinColumnTest.RandomProduct.class,
		JoinedInheritanceDiscriminatorInJoinColumnTest.MetaOperator.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-17020")
public class JoinedInheritanceDiscriminatorInJoinColumnTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testPersistAndQuery(SessionFactoryScope scope) {
		String string = "ID";
		String operatorID = "operatorID";
		Product.ProductPK id = new Product.ProductPK(string, operatorID, Country.USA);
		String test = "test";

		scope.inTransaction( session -> {
			Operator operator = new Operator(operatorID);
			Product product = new FixedProduct(string, operator);
			product.setDescription(test);
			session.persist(product);
		} );
		scope.inTransaction( session -> {
			FixedProduct byId = session.find(FixedProduct.class, id);
			assertThat(byId.getDescription()).isEqualTo(test);
			assertThat(byId.getOperator().getOperatorId()).isEqualTo(operatorID);
		} );
	}

	@IdClass(Product.ProductPK.class)
	@Entity(name = "Product")
	@OptimisticLocking(type = OptimisticLockType.VERSION)
	@DynamicUpdate
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "PRODUCTS")
	public static abstract class Product {
		public Product() {
		}

		protected Product(String productId, Operator operator) {
			this.productId = productId;
			this.operator = operator;
		}
		protected Product(String productId, Operator operator, Benefits benefits) {
			this.productId = productId;
			this.operator = operator;
			this.benefits = benefits;
		}
		@Id
		@Column(name = "PRODUCT_ID", nullable = false)
		private String productId;
		@Id
		@ManyToOne(fetch = LAZY, optional = false, cascade = PERSIST)
		@JoinColumn(name = "OPERATOR_ID", referencedColumnName = "OPERATOR_ID", nullable = false)
		@JoinColumn(name = "COUNTRY", referencedColumnName = "COUNTRY", nullable = false)
		private Operator operator;
		@Column(name = "DESCRIPTION")
		private String description;
		@Embedded
		private Benefits benefits;
		@Version
		@CurrentTimestamp(source = SourceType.VM)
		@Column(name = "MODIFICATION_DATE", nullable = false, insertable = true)
		private Instant modificationDate;

		@Embeddable
		public static class ProductPK implements Serializable {
			private String productId;
			private Operator.OperatorPK operator;

			public ProductPK() {
			}

			public ProductPK(String productId, Operator.OperatorPK operator) {
				this.productId = productId;
				this.operator = operator;
			}
			public ProductPK(String productId, String operatorID, Country country) {
				this.productId = productId;
				this.operator = new Operator.OperatorPK(operatorID, country);
			}
		}
		@Embeddable
		public static class Benefits {
			@Embedded
			TypeOneBenefit credit;
			@Embedded
			TypeTwoBenefit data;
		}
		@Embeddable
		public static class TypeOneBenefit {
			@Column(name = "BENEFIT_ONE_BASE_AMOUNT")
			BigDecimal baseAmount;
		}
		@Embeddable
		public static class TypeTwoBenefit {
			@Column(name = "BENEFIT_TWO_BASE_AMOUNT")
			String baseAmount;
		}

		public String getProductId() {
			return productId;
		}

		public void setProductId(String productId) {
			this.productId = productId;
		}

		public Operator getOperator() {
			return operator;
		}

		public void setOperator(Operator operator) {
			this.operator = operator;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Benefits getBenefits() {
			return benefits;
		}

		public void setBenefits(Benefits benefits) {
			this.benefits = benefits;
		}

		public Instant getModificationDate() {
			return modificationDate;
		}

		public void setModificationDate(Instant modificationDate) {
			this.modificationDate = modificationDate;
		}
	}

	@Entity(name = "Operator")
	@Table(name = "OPERATORS")
	@IdClass(Operator.OperatorPK.class)
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class Operator {
		@Id
		@Enumerated(STRING)
		@Column(name = "COUNTRY", nullable = false)
		private Country country;
		@Id
		@Column(name = "OPERATOR_ID", nullable = false)
		private String operatorId;
		@ManyToOne
		@JoinColumn(name = "meta_operator_id", referencedColumnName = "ID")
		private MetaOperator metaOperator;
		@OneToMany(mappedBy = "operator",
				cascade = {PERSIST, MERGE, REMOVE},
				orphanRemoval = true,
				fetch = LAZY)
		private List<Product> products = new ArrayList<>();

		public Operator() {
		}

		public Operator(String operatorId) {
			this.operatorId = operatorId;
			// default country
			this.country = Country.USA;
		}
		public void setMetaOperator(MetaOperator metaOperator) {
			this.metaOperator = metaOperator;
		}
		public void setProducts(List<Product> products) {
			this.products = products;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}

		public String getOperatorId() {
			return operatorId;
		}

		public void setOperatorId(String operatorId) {
			this.operatorId = operatorId;
		}

		public MetaOperator getMetaOperator() {
			return metaOperator;
		}

		public List<Product> getProducts() {
			return products;
		}

		@Embeddable
		public static class OperatorPK implements Serializable {
			String operatorId;
			@Enumerated(STRING)
			Country country;

			public OperatorPK() {
			}

			public OperatorPK(String operatorId, Country country) {
				this.operatorId = operatorId;
				this.country = country;
			}
		}
	}

	public enum Country {
		USA,
		FRA;
	}

	@Entity(name = "FixedProduct")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@OnDelete(action = OnDeleteAction.CASCADE)
	@Table(name = "FIXED_PRODUCT")
	public static class FixedProduct extends Product {
		@Column(name = "BONUS")
		private Long bonus;

		public FixedProduct() {
		}

		public FixedProduct(String productId, Operator operator) {
			super(productId, operator);
		}
		public FixedProduct(String productId, Operator operator, Benefits benefits) {
			super(productId, operator, benefits);
		}
	}

	@Entity(name = "RandomProduct")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@OnDelete(action = OnDeleteAction.CASCADE)
	@Table(name = "RANDOM_PRODUCT")
	public static class RandomProduct extends Product {
		@Column(name = "PRICE_DESCRIPTION")
		private String PriceDescription;

		public RandomProduct() {
		}

		public RandomProduct(String productId, Operator operator) {
			super(productId, operator);
		}
		public RandomProduct(String productId, Operator operator, Benefits benefits) {
			super(productId, operator, benefits);
		}
	}

	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@Entity(name = "MetaOperator")
	public static class MetaOperator {
		@Id
		@Column(name = "ID", nullable = false)
		private String id;
		@OneToMany(mappedBy = "metaOperator", fetch = EAGER, cascade = {PERSIST, MERGE})
		private final List<Operator> linkedOperators = new ArrayList<>();

		public MetaOperator() {
		}

		public MetaOperator(String id) {
			this.id = id;
		}
		public void addOperator(Operator operator) {
			operator.setMetaOperator(this);
			linkedOperators.add(operator);
		}
	}


}
