/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.ondemandload;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10055" )
@DomainModel(
		annotatedClasses = {
			OnDemandLoadTest.Store.class, OnDemandLoadTest.Inventory.class, OnDemandLoadTest.Product.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class OnDemandLoadTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Store store = new Store( 1L ).setName( "Acme Super Outlet" );
			s.persist( store );

			Product product = new Product( "007" ).setName( "widget" ).setDescription( "FooBar" );
			s.persist( product );

			store.addInventoryProduct( product ).setQuantity( 10L ).setStorePrice( new BigDecimal( 500 ) );
		} );
	}

	@Test
	public void testClosedSession(SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
		Store[] store = new Store[1];

		scope.inTransaction( s -> {
			// first load the store, making sure it is not initialized
			store[0] = s.getReference( Store.class, 1L );
			assertNotNull( store[0] );
			assertFalse( isPropertyInitialized( store[0], "inventories" ) );

			assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 0, scope.getSessionFactory().getStatistics().getSessionCloseCount() );
		} );

		assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionCloseCount() );

		store[0].getInventories();
		assertTrue( isPropertyInitialized( store[0], "inventories" ) );

		assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
		assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionCloseCount() );
	}

	@Test
	public void testClearedSession(SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction( s -> {
			// first load the store, making sure collection is not initialized
			Store store = s.get( Store.class, 1L );
			assertNotNull( store );
			assertFalse( isInitialized( store.getInventories() ) );
			assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 0, scope.getSessionFactory().getStatistics().getSessionCloseCount() );

			// then clear session and try to initialize collection
			s.clear();
			assertNotNull( store );
			assertFalse( isInitialized( store.getInventories() ) );
			store.getInventories().size();
			assertTrue( isInitialized( store.getInventories() ) );

			// the extra Session is the temp Sessions needed to perform the collection init (since it's lazy)
			assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionCloseCount() );

			// clear Session again.  The collection should still be recognized as initialized from above
			s.clear();
			assertNotNull( store );
			assertTrue( isInitialized( store.getInventories() ) );
			assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionCloseCount() );

			// lets clear the Session again and this time reload the Store
			s.clear();
			store = s.get( Store.class, 1L );
			s.clear();
			assertNotNull( store );

			// collection should be back to uninitialized since we have a new entity instance
			assertFalse( isInitialized( store.getInventories() ) );
			assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 1, scope.getSessionFactory().getStatistics().getSessionCloseCount() );
			store.getInventories().size();
			assertTrue( isInitialized( store.getInventories() ) );

			// the extra Session is the temp Sessions needed to perform the collection init (since it's lazy)
			assertEquals( 3, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionCloseCount() );

			// clear Session again.  The collection should still be recognized as initialized from above
			s.clear();
			assertNotNull( store );
			assertTrue( isInitialized( store.getInventories() ) );
			assertEquals( 3, scope.getSessionFactory().getStatistics().getSessionOpenCount() );
			assertEquals( 2, scope.getSessionFactory().getStatistics().getSessionCloseCount() );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	// --- //

	@Entity
	@Table( name = "STORE" )
	static class Store {
		@Id
		Long id;

		String name;

		@OneToMany( mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		List<Inventory> inventories = new ArrayList<>();

		@Version
		Integer version;

		Store() {
		}

		Store(long id) {
			this.id = id;
		}

		Store setName(String name) {
			this.name = name;
			return this;
		}

		Inventory addInventoryProduct(Product product) {
			Inventory inventory = new Inventory( this, product );
			inventories.add( inventory );
			return inventory;
		}

		public List<Inventory> getInventories() {
			return inventories;
		}
	}

	@Entity
	@Table( name = "INVENTORY" )
	static class Inventory {

		@Id
		@GeneratedValue
		@GenericGenerator( name = "increment", strategy = "increment" )
		Long id = -1L;

		@ManyToOne
		@JoinColumn( name = "STORE_ID" )
		Store store;

		@ManyToOne
		@JoinColumn( name = "PRODUCT_ID" )
		Product product;

		Long quantity;

		BigDecimal storePrice;

		public Inventory() {
		}

		public Inventory(Store store, Product product) {
			this.store = store;
			this.product = product;
		}

		Inventory setStore(Store store) {
			this.store = store;
			return this;
		}

		Inventory setProduct(Product product) {
			this.product = product;
			return this;
		}

		Inventory setQuantity(Long quantity) {
			this.quantity = quantity;
			return this;
		}

		Inventory setStorePrice(BigDecimal storePrice) {
			this.storePrice = storePrice;
			return this;
		}
	}

	@Entity
	@Table( name = "PRODUCT" )
	static class Product {
		@Id
		String id;

		String name;

		String description;

		BigDecimal msrp;

		@Version
		Long version;

		Product() {
		}

		Product(String id) {
			this.id = id;
		}

		Product setName(String name) {
			this.name = name;
			return this;
		}

		Product setDescription(String description) {
			this.description = description;
			return this;
		}

		Product setMsrp(BigDecimal msrp) {
			this.msrp = msrp;
			return this;
		}
	}
}
