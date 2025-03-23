/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import java.io.Serializable;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Jan Schatteman
 * @author Andrea Boriero
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-16908" )
public class BidirectionalOneToOneWithIdClassesUnownedAssociationTest {
	@Test
	public void test() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			new MetadataSources( ssr ).addAnnotatedClass( Price.class )
					.addAnnotatedClass( Product.class )
					.addAnnotatedClass( Operator.class )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
			fail( "Unowned association should not be allowed as an entity identifier" );
			// See JPA spec: https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#a149
			// > The identity of an entity may be derived from the identity of another entity (the “parent” entity) when
			// > the former entity (the “dependent” entity) is the owner of a many-to-one or one-to-one relationship to
			// > the parent entity and a foreign key maps the relationship from dependent to parent.
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( AnnotationException.class );
			assertThat( e.getMessage() ).contains(
					"wholesalePrice' is the inverse side of a '@OneToOne' association and cannot be used as identifier"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
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

		@OneToOne( mappedBy = "product" )
		private Price wholesalePrice;
	}
}
