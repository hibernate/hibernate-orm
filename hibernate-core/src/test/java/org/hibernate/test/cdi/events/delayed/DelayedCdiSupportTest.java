/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events.delayed;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.test.cdi.events.MyIdGenerator;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.cdi.events.Monitor;
import org.hibernate.test.cdi.events.TheEntity;
import org.hibernate.test.cdi.events.TheListener;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests support for CDI delaying access to the CDI container until
 * first needed
 *
 * @author Steve Ebersole
 */
public class DelayedCdiSupportTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		Monitor.reset();

		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( Monitor.class, TheListener.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.applySetting( AvailableSettings.DELAY_CDI_ACCESS, "true" )
					.build();

			final SessionFactoryImplementor sessionFactory;

			try {
				sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
						.addAnnotatedClass( TheEntity.class )
						.buildMetadata()
						.getSessionFactoryBuilder()
						.build();
			}
			catch ( Exception e ) {
				StandardServiceRegistryBuilder.destroy( ssr );
				throw e;
			}

			// The CDI bean should not be built immediately...
			assertFalse( Monitor.wasInstantiated() );
			assertEquals( 0, Monitor.currentCount() );

			try {
				inTransaction(
						sessionFactory,
						session -> session.persist( new TheEntity() )
				);

				// The CDI bean should have been built on first use
				assertTrue( Monitor.wasInstantiated() );
				assertEquals( 1, Monitor.currentCount() );

				inTransaction(
						sessionFactory,
						session -> {
							TheEntity it = session.find( TheEntity.class, MyIdGenerator.HARDCODED_ID);
							assertNotNull( it );
						}
				);
			}
			finally {
				inTransaction(
						sessionFactory,
						session -> {
							session.createQuery( "delete TheEntity" ).executeUpdate();
						}
				);

				sessionFactory.close();
			}
		}
	}
}
