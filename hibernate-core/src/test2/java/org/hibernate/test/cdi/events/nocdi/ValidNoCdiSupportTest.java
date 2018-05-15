/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events.nocdi;

import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests valid use of ManagedBeanRegistry when CDI is not available -
 * meaning injection is not requested.
 *
 * @author Steve Ebersole
 */
public class ValidNoCdiSupportTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		AnotherListener.reset();

		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build();

		final SessionFactoryImplementor sessionFactory;

		try {
			sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( AnotherEntity.class )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
		}
		catch ( Exception e ) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}

		// The CDI bean should have been built immediately...
		assertTrue( AnotherListener.wasInstantiated() );
		assertEquals( 0, AnotherListener.currentCount() );

		try {
			inTransaction(
					sessionFactory,
					session -> session.persist( new AnotherEntity( 1 ) )
			);

			assertEquals( 1, AnotherListener.currentCount() );

			inTransaction(
					sessionFactory,
					session -> {
						AnotherEntity it = session.find( AnotherEntity.class, 1 );
						assertNotNull( it );
					}
			);
		}
		finally {
			inTransaction(
					sessionFactory,
					session -> {
						session.createQuery( "delete AnotherEntity" ).executeUpdate();
					}
			);

			sessionFactory.close();
		}
	}


	@Entity( name = "AnotherEntity" )
	@Table( name = "another_entity")
	@EntityListeners( AnotherListener.class )
	public static class AnotherEntity {
		private Integer id;
		private String name;

		public AnotherEntity() {
		}

		public AnotherEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class AnotherListener {
		private static final AtomicInteger count = new AtomicInteger( 0 );
		private static boolean instantiated;

		public AnotherListener() {
			instantiated = true;
		}

		public static void reset() {
			count.set( 0 );
			instantiated = false;
		}

		public static boolean wasInstantiated() {
			return instantiated;
		}

		public static int currentCount() {
			return count.get();
		}

		@PrePersist
		public void onCreate(Object entity) {
			count.getAndIncrement();
		}
	}
}
