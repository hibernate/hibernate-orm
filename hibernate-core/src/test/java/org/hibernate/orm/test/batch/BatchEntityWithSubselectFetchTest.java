/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
@DomainModel(annotatedClasses = {
		BatchEntityWithSubselectFetchTest.Order.class,
		BatchEntityWithSubselectFetchTest.Product.class,
})
@JiraKey("HHH-16890")
public class BatchEntityWithSubselectFetchTest {

	@BeforeAll
	public void setupData(SessionFactoryScope scope) {
		Product cheese1 = new Product( 1l, "Cheese 1" );
		Product cheese2 = new Product( 2l, "Cheese 2" );
		Product cheese3 = new Product( 3l, "Cheese 3" );

		Order order = new Order( 1l, "Hibernate" );

		order.addProduct( cheese1 );
		order.addProduct( cheese2 );

		cheese1.setBestCheese( cheese1 );
		cheese2.setBestCheese( cheese1 );

		cheese1.setReplacement( cheese2 );
		cheese2.setReplacement( cheese1 );

		scope.inTransaction( s -> {
			s.persist( cheese1 );
			s.persist( cheese2 );
			s.persist( cheese3 );
			s.persist( order );
		} );
	}

	@Test
	public void testGetOrder(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Order o = s.get( Order.class, 1 );

			assertEquals( 2, o.getProducts().size() );
		} );
	}

	@Test
	public void testGetProduct(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product3 = s.getReference( Product.class, 3l );
			assertFalse( Hibernate.isInitialized( product3 ) );

			Product product1 = s.get( Product.class, 1l );

			assertThat( product1 ).isNotNull();
			assertTrue( Hibernate.isInitialized( product3 ) );
		} );
	}

	@Test
	public void testCriteriaQuery(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Product> cr = cb.createQuery( Product.class );
			Root<Product> root = cr.from( Product.class );
			CriteriaQuery<Product> query = cr.select( root );

			List<Product> products = s.createQuery( query ).getResultList();

			assertEquals( 3, products.size() );
		} );
	}

	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {
		@Id
		Long id;

		String name;

		public Order() {
		}

		public Order(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@OneToMany
		List<Product> products = new ArrayList<>();

		public List<Product> getProducts() {
			return products;
		}

		public void addProduct(Product product) {
			this.products.add( product );
		}
	}

	@Entity(name = "Product")
	@BatchSize(size = 512)
	@Cacheable
	public static class Product {
		@Id
		Long id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		Product replacement;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		Product bestCheese;

		public Product() {
		}

		public Product(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Product getReplacement() {
			return replacement;
		}

		public void setReplacement(Product replacement) {
			this.replacement = replacement;
		}

		public Product getBestCheese() {
			return bestCheese;
		}

		public void setBestCheese(Product bestCheese) {
			this.bestCheese = bestCheese;
		}
	}
}
