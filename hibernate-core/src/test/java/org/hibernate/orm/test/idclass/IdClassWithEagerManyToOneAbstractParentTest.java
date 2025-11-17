/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		IdClassWithEagerManyToOneAbstractParentTest.Operator.class,
		IdClassWithEagerManyToOneAbstractParentTest.OperatorPK.class,
		IdClassWithEagerManyToOneAbstractParentTest.Product.class,
		IdClassWithEagerManyToOneAbstractParentTest.ProductPK.class,
		IdClassWithEagerManyToOneAbstractParentTest.FixedProduct.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17387" )
public class IdClassWithEagerManyToOneAbstractParentTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Operator operator = new Operator( "operator_1", "USA" );
			session.persist( operator );
			session.persist( new FixedProduct( "product_1", operator, "Fixed product #1" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Product" ).executeUpdate();
			session.createMutationQuery( "delete from Operator" ).executeUpdate();
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Operator operator = session.find( Operator.class, new OperatorPK( "operator_1", "USA" ) );
			assertThat( operator ).isNotNull();
			final Product product = session.find( Product.class, new ProductPK( "product_1", operator ) );
			assertThat( product.getProductId() ).isEqualTo( "product_1" );
			assertThat( product.getOperator().getOperatorId() ).isEqualTo( "operator_1" );
			assertThat( product.getOperator().getCountry() ).isEqualTo( "USA" );
		} );
	}

	@Test
	public void testJoinFetchQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Product product = session.createQuery(
					"from Product p join fetch p.operator",
					Product.class
			).getSingleResult();
			assertThat( product.getProductId() ).isEqualTo( "product_1" );
			assertThat( product.getOperator().getOperatorId() ).isEqualTo( "operator_1" );
			assertThat( product.getOperator().getCountry() ).isEqualTo( "USA" );
		} );
	}

	@Embeddable
	@SuppressWarnings( "unused" )
	public static class OperatorPK implements Serializable {
		private String operatorId;
		private String country;

		public OperatorPK() {
		}

		public OperatorPK(String operatorId, String country) {
			this.operatorId = operatorId;
			this.country = country;
		}
	}

	@Entity( name = "Operator" )
	@IdClass( OperatorPK.class )
	public static class Operator {
		@Id
		@Column( name = "operator_id" )
		private String operatorId;

		@Id
		@Column( name = "country" )
		private String country;

		@OneToMany( mappedBy = "operator" )
		private List<Product> products = new ArrayList<>();

		public Operator() {
		}

		public Operator(String operatorId, String country) {
			this.operatorId = operatorId;
			this.country = country;
		}

		public String getOperatorId() {
			return operatorId;
		}

		public String getCountry() {
			return country;
		}
	}

	@Embeddable
	@SuppressWarnings( "unused" )
	public static class ProductPK implements Serializable {
		private String productId;
		private Operator operator;

		public ProductPK() {
		}

		public ProductPK(String productId, Operator operator) {
			this.productId = productId;
			this.operator = operator;
		}
	}

	@IdClass( ProductPK.class )
	@Entity( name = "Product" )
	@Inheritance( strategy = JOINED )
	public abstract static class Product {
		@Id
		private String productId;

		@Id
		@ManyToOne( fetch = EAGER )
		@JoinColumn( name = "operator_id", referencedColumnName = "operator_id" )
		@JoinColumn( name = "country", referencedColumnName = "country" )
		private Operator operator;

		public Product() {
		}

		public Product(String productId, Operator operator) {
			this.productId = productId;
			this.operator = operator;
		}

		public String getProductId() {
			return productId;
		}

		public Operator getOperator() {
			return operator;
		}
	}

	@Entity( name = "FixedProduct" )
	public static class FixedProduct extends Product {
		private String description;

		public FixedProduct() {
		}

		public FixedProduct(String productId, Operator operator, String description) {
			super( productId, operator );
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
