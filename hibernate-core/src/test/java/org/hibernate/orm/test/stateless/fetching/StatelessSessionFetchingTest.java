/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.fetching;

import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { Producer.class, Product.class, Vendor.class },
		xmlMappings = "org/hibernate/orm/test/stateless/fetching/Mappings.hbm.xml"

)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.PHYSICAL_NAMING_STRATEGY, value = "org.hibernate.orm.test.stateless.fetching.TestingNamingStrategy"),
				@Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true")
		}
)
@SessionFactory
public class StatelessSessionFetchingTest {

	@Test
	public void testDynamicFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Date now = new Date();
					User me = new User( "me" );
					User you = new User( "you" );
					Resource yourClock = new Resource( "clock", you );
					Task task = new Task( me, "clean", yourClock, now ); // :)
					session.persist( me );
					session.persist( you );
					session.persist( yourClock );
					session.persist( task );
				}
		);

		scope.inStatelessTransaction(
				session -> {
					Task taskRef = (Task) session.createQuery( "from Task t join fetch t.resource join fetch t.user" )
							.uniqueResult();
					assertNotNull( taskRef );
					assertTrue( Hibernate.isInitialized( taskRef ) );
					assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
					assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
					assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
				}
		);

	}

	@Test
	public void testDynamicFetchScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Date now = new Date();

					User me = new User( "me" );
					User you = new User( "you" );
					Resource yourClock = new Resource( "clock", you );
					Task task = new Task( me, "clean", yourClock, now ); // :)

					session.persist( me );
					session.persist( you );
					session.persist( yourClock );
					session.persist( task );

					User u3 = new User( "U3" );
					User u4 = new User( "U4" );
					Resource it = new Resource( "it", u4 );
					Task task2 = new Task( u3, "beat", it, now ); // :))

					session.persist( u3 );
					session.persist( u4 );
					session.persist( it );
					session.persist( task2 );
				}
		);

		scope.inStatelessTransaction(
				session -> {
					final Query query = session.createQuery( "from Task t join fetch t.resource join fetch t.user" );
					try (ScrollableResults scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY )) {
						while ( scrollableResults.next() ) {
							Task taskRef = (Task) scrollableResults.get();
							assertTrue( Hibernate.isInitialized( taskRef ) );
							assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
							assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
							assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
						}
					}
				}
		);
	}

	@Test
	public void testDynamicFetchScrollSession(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Date now = new Date();

					User me = new User( "me" );
					User you = new User( "you" );
					Resource yourClock = new Resource( "clock", you );
					Task task = new Task( me, "clean", yourClock, now ); // :)

					session.persist( me );
					session.persist( you );
					session.persist( yourClock );
					session.persist( task );

					User u3 = new User( "U3" );
					User u4 = new User( "U4" );
					Resource it = new Resource( "it", u4 );
					Task task2 = new Task( u3, "beat", it, now ); // :))

					session.persist( u3 );
					session.persist( u4 );
					session.persist( it );
					session.persist( task2 );
				}
		);

		scope.inStatelessTransaction(
				session -> {
					final Query query = session.createQuery( "from Task t join fetch t.resource join fetch t.user" );
					try (ScrollableResults scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY )) {
						while ( scrollableResults.next() ) {
							Task taskRef = (Task) scrollableResults.get();
							assertTrue( Hibernate.isInitialized( taskRef ) );
							assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
							assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
							assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
						}
					}
				}
		);
	}

	@Test
	public void testDynamicFetchCollectionScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Producer p1 = new Producer( 1, "Acme" );
					Producer p2 = new Producer( 2, "ABC" );

					session.persist( p1 );
					session.persist( p2 );

					Vendor v1 = new Vendor( 1, "v1" );
					Vendor v2 = new Vendor( 2, "v2" );

					session.persist( v1 );
					session.persist( v2 );

					final Product product1 = new Product( 1, "123", v1, p1 );
					final Product product2 = new Product( 2, "456", v1, p1 );
					final Product product3 = new Product( 3, "789", v1, p2 );

					session.persist( product1 );
					session.persist( product2 );
					session.persist( product3 );
				}
		);

		scope.inStatelessTransaction(
				session -> {
					final Query query = session.createQuery( "select p from Producer p join fetch p.products" );
					try (ScrollableResults scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY )) {

						while ( scrollableResults.next() ) {
							Producer producer = (Producer) scrollableResults.get();
							assertTrue( Hibernate.isInitialized( producer ) );
							assertTrue( Hibernate.isInitialized( producer.getProducts() ) );

							for ( Product product : producer.getProducts() ) {
								assertTrue( Hibernate.isInitialized( product ) );
								assertFalse( Hibernate.isInitialized( product.getVendor() ) );
							}
						}
					}
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
