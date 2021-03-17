/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events.extended;

import java.util.function.Consumer;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.cdi.events.Monitor;
import org.hibernate.test.cdi.events.TheEntity;
import org.hibernate.test.cdi.events.TheListener;
import org.hibernate.test.cdi.testsupport.TestingExtendedBeanManager;
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
public class ValidExtendedCdiSupportTest extends BaseUnitTestCase {
	@Test
	public void test() {
		doTest( TestingExtendedBeanManager.create() );
	}

	/**
	 * NOTE : we use the deprecated one here to make sure this continues to work.
	 * Scott still uses this in WildFly and we need it to continue to work there
	 */
	@Test
	public void testLegacy() {
		doTest( TestingExtendedBeanManager.createLegacy() );
	}

	private void doTest(TestingExtendedBeanManager beanManager) {
		Monitor.reset();

		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.CDI_BEAN_MANAGER, beanManager )
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


		try {
			// The CDI bean should not be built immediately...
			assertFalse( Monitor.wasInstantiated() );
			assertEquals( 0, Monitor.currentCount() );


			// But now lets initialize CDI and do the callback

			final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
					.disableDiscovery()
					.addBeanClasses( Monitor.class, TheListener.class );
			try (final SeContainer cdiContainer = cdiInitializer.initialize()) {

				beanManager.notifyListenerReady( cdiContainer.getBeanManager() );

				// at this point the bean should have been accessed
				assertTrue( Monitor.wasInstantiated() );
				assertEquals( 0, Monitor.currentCount() );

				try {
					inTransaction(
							sessionFactory,
							session -> session.persist( new TheEntity( 1 ) )
					);

					inTransaction(
							sessionFactory,
							session -> {
								TheEntity it = session.find( TheEntity.class, 1 );
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
				}
			}
		}
		finally {
			sessionFactory.close();
		}
	}
}
