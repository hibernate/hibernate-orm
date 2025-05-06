/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import java.io.Serializable;
import java.util.stream.StreamSupport;

import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jan Schatteman
 * @author Andrea Boriero
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-16908" )
@SessionFactory
@DomainModel( annotatedClasses = {
		BidirectionalOneToOneWithIdClassesTest.Price.class,
		BidirectionalOneToOneWithIdClassesTest.Product.class,
		BidirectionalOneToOneWithIdClassesTest.Operator.class,
} )
public class BidirectionalOneToOneWithIdClassesTest {
	@Test
	public void test(SessionFactoryScope scope) {
		StreamSupport.stream( scope.getMetadataImplementor().getDatabase().getNamespaces().spliterator(), false )
				.flatMap( namespace -> namespace.getTables().stream() )
				.forEach( t -> {
					if ( t.getName().equals( "Product" ) ) {
						assertThat( t.getColumns().stream().map( Column::getName ) ).contains( "productId" );
					}
					else if ( t.getName().equals( "Price" ) ) {
						assertThat( t.getColumns().stream().map( Column::getName ) ).contains(
								"operator_operatorId",
								"price",
								"product_productId"
						);
					}
				} );
	}

	@Entity( name = "Operator" )
	public static class Operator {
		@Id
		private String operatorId;
	}

	@Entity( name = "Price" )
	@IdClass( PricePK.class )
	public static class Price {
		@Id
		@ManyToOne
		private Operator operator;

		@Id
		private String price;

		@OneToOne
		private Product product;
	}

	@Embeddable
	public static class PricePK implements Serializable {
		@ManyToOne
		private Operator operator;

		private String price;
	}

	@Entity( name = "Product" )
	@IdClass( ProductPK.class )
	public static class Product {
		@Id
		private String productId;

		@OneToOne( mappedBy = "product" )
		private Price wholesalePrice;
	}

	@Embeddable
	public static class ProductPK implements Serializable {
		private String productId;
	}
}
