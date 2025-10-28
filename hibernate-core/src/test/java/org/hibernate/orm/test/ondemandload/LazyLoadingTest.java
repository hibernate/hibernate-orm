/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondemandload;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		annotatedClasses = {
				Store.class,
				Inventory.class,
				Product.class
		}
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "true")
		}
)
public class LazyLoadingTest {

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Store store = new Store( 1 )
							.setName( "Acme Super Outlet" );
					session.persist( store );

					Product product = new Product( "007" )
							.setName( "widget" )
							.setDescription( "FooBar" );
					session.persist( product );

					store.addInventoryProduct( product )
							.setQuantity( 10L )
							.setStorePrice( new BigDecimal( 500 ) );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLazyCollectionLoadingWithClearedSession(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				session -> {
					// first load the store, making sure collection is not initialized
					Store store = (Store) session.get( Store.class, 1 );
					assertNotNull( store );
					assertFalse( Hibernate.isInitialized( store.getInventories() ) );

					assertThat( statistics.getSessionOpenCount(), is( 1l ) );
					assertThat( statistics.getSessionCloseCount(), is( 0l ) );

					// then clear session and try to initialize collection
					session.clear();
					store.getInventories().size();
					assertTrue( Hibernate.isInitialized( store.getInventories() ) );

					assertThat( statistics.getSessionOpenCount(), is( 2l ) );
					assertThat( statistics.getSessionCloseCount(), is( 1l ) );

					session.clear();
					store = (Store) session.get( Store.class, 1 );
					assertNotNull( store );
					assertFalse( Hibernate.isInitialized( store.getInventories() ) );

					assertThat( statistics.getSessionOpenCount(), is( 2l ) );
					assertThat( statistics.getSessionCloseCount(), is( 1l ) );

					session.clear();
					store.getInventories().iterator();
					assertTrue( Hibernate.isInitialized( store.getInventories() ) );

					assertThat( statistics.getSessionOpenCount(), is( 3l ) );
					assertThat( statistics.getSessionCloseCount(), is( 2l ) );
				}
		);
	}

	@Test
	public void testLazyCollectionLoadingWithClosedSession(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		Store s = scope.fromTransaction(
				session -> {
					// first load the store, making sure collection is not initialized
					Store store = session.get( Store.class, 1 );
					assertNotNull( store );
					assertFalse( Hibernate.isInitialized( store.getInventories() ) );

					assertThat( statistics.getSessionOpenCount(), is( 1l ) );
					assertThat( statistics.getSessionCloseCount(), is( 0l ) );
					return store;
				}
		);


		// close the session and try to initialize collection

		assertThat( statistics.getSessionOpenCount(), is( 1l ) );
		assertThat( statistics.getSessionCloseCount(), is( 1l ) );

		s.getInventories().size();
		assertTrue( Hibernate.isInitialized( s.getInventories() ) );

		assertThat( statistics.getSessionOpenCount(), is( 2l ) );
		assertThat( statistics.getSessionCloseCount(), is( 2l ) );
	}

	@Test
	public void testLazyEntityLoadingWithClosedSession(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		Store s = scope.fromTransaction(
				session -> {
					// first load the store, making sure it is not initialized
					Store store = session.getReference( Store.class, 1 );
					assertNotNull( store );
					assertFalse( Hibernate.isInitialized( store ) );

					assertThat( statistics.getSessionOpenCount(), is( 1l ) );
					assertThat( statistics.getSessionCloseCount(), is( 0l ) );
					return store;
				}
		);


		// close the session and try to initialize store

		assertThat( statistics.getSessionOpenCount(), is( 1l ) );
		assertThat( statistics.getSessionCloseCount(), is( 1l ) );

		s.getName();
		assertTrue( Hibernate.isInitialized( s ) );

		assertThat( statistics.getSessionOpenCount(), is( 2l ) );
		assertThat( statistics.getSessionCloseCount(), is( 2l ) );
	}

}
