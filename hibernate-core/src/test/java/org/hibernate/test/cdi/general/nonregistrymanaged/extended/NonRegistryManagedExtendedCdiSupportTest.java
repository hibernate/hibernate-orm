/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.nonregistrymanaged.extended;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.cdi.general.nonregistrymanaged.Monitor;
import org.hibernate.test.cdi.general.nonregistrymanaged.NonRegistryManagedBeanConsumingIntegrator;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheAlternativeNamedApplicationScopedBeanImpl;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheAlternativeNamedDependentBeanImpl;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheApplicationScopedBean;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheDependentBean;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheEntity;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheFallbackBeanInstanceProducer;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheMainNamedApplicationScopedBeanImpl;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheMainNamedDependentBeanImpl;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheNamedApplicationScopedBean;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheNamedDependentBean;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheNestedDependentBean;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheNonHibernateBeanConsumer;
import org.hibernate.test.cdi.general.nonregistrymanaged.TheSharedApplicationScopedBean;
import org.hibernate.test.cdi.testsupport.TestingExtendedBeanManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests support for requesting CDI beans from the {@link ManagedBeanRegistry}
 * when the CDI BeanManager access is "lazy" (beans are instantiated when instances are first requested),
 * and when the registry should not manage the lifecycle of beans, but leave it up to CDI.
 *
 * @author Steve Ebersole
 * @author Yoann Rodiere
 */
public class NonRegistryManagedExtendedCdiSupportTest extends BaseUnitTestCase {
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

		final TheFallbackBeanInstanceProducer fallbackBeanInstanceProducer =
				new TheFallbackBeanInstanceProducer();
		final NonRegistryManagedBeanConsumingIntegrator beanConsumingIntegrator =
				new NonRegistryManagedBeanConsumingIntegrator( fallbackBeanInstanceProducer );

		try (SessionFactoryImplementor sessionFactory = buildSessionFactory( beanManager, beanConsumingIntegrator )) {
			final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
					.disableDiscovery()
					.addBeanClasses( TheApplicationScopedBean.class )
					.addBeanClasses( TheNamedApplicationScopedBean.class, TheMainNamedApplicationScopedBeanImpl.class,
							TheAlternativeNamedApplicationScopedBeanImpl.class )
					.addBeanClasses( TheSharedApplicationScopedBean.class )
					.addBeanClasses( TheDependentBean.class )
					.addBeanClasses( TheNamedDependentBean.class, TheMainNamedDependentBeanImpl.class,
							TheAlternativeNamedDependentBeanImpl.class )
					.addBeanClasses( TheNestedDependentBean.class )
					.addBeanClasses( TheNonHibernateBeanConsumer.class );
			try (final SeContainer cdiContainer = cdiInitializer.initialize()) {
				// Simulate CDI bean consumers outside of Hibernate ORM
				Instance<TheNonHibernateBeanConsumer> nonHibernateBeanConsumerInstance =
						cdiContainer.getBeanManager().createInstance().select( TheNonHibernateBeanConsumer.class );
				nonHibernateBeanConsumerInstance.get();

				// Here, the NonRegistryManagedBeanConsumingIntegrator has just been integrated and has requested beans
				// BUT it has not fetched instances of beans yet, so non-shared beans should not have been instantiated yet.
				assertEquals( 0, Monitor.theApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theMainNamedApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theDependentBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theMainNamedDependentBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentInstantiationCount() );
				assertEquals( 0, fallbackBeanInstanceProducer.currentInstantiationCount() );
				assertEquals( 0, fallbackBeanInstanceProducer.currentNamedInstantiationCount() );
				// Nested dependent bean: 1 instance per bean that depends on it
				assertEquals( 1, Monitor.theNestedDependentBean().currentInstantiationCount() );

				beanManager.notifyListenerReady( cdiContainer.getBeanManager() );

				beanConsumingIntegrator.ensureInstancesInitialized();

				// Here the NonRegistryManagedBeanConsumingIntegrator *did* fetch an instance of each bean,
				// so all beans should have been instantiated.
				// See NonRegistryManagedBeanConsumingIntegrator for a detailed list of requested beans

				// Application scope: maximum 1 instance as soon as at least one was requested
				assertEquals( 1, Monitor.theApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentInstantiationCount() );
				assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentInstantiationCount() );

				// Dependent scope: 1 instance per bean we requested explicitly
				assertEquals( 2, Monitor.theDependentBean().currentInstantiationCount() );
				assertEquals( 2, Monitor.theMainNamedDependentBean().currentInstantiationCount() );
				assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentInstantiationCount() );

				// Reflection-instantiated: 1 instance per bean we requested explicitly
				assertEquals( 2, fallbackBeanInstanceProducer.currentInstantiationCount() );
				assertEquals( 2, fallbackBeanInstanceProducer.currentNamedInstantiationCount() );

				// Nested dependent bean: 1 instance per bean that depends on it
				assertEquals( 7, Monitor.theNestedDependentBean().currentInstantiationCount() );

				// Expect one PostConstruct call per CDI bean instance
				assertEquals( 1, Monitor.theApplicationScopedBean().currentPostConstructCount() );
				assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPostConstructCount() );
				assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPostConstructCount() );
				assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentPostConstructCount() );
				assertEquals( 2, Monitor.theDependentBean().currentPostConstructCount() );
				assertEquals( 2, Monitor.theMainNamedDependentBean().currentPostConstructCount() );
				assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPostConstructCount() );
				assertEquals( 7, Monitor.theNestedDependentBean().currentPostConstructCount() );

				// Expect no PreDestroy call yet
				assertEquals( 0, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theDependentBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
				assertEquals( 0, Monitor.theNestedDependentBean().currentPreDestroyCount() );
			}

			// After the CDI context has ended, PreDestroy should have been called on every "normal-scoped" CDI bean
			// (i.e. all CDI beans excepting the dependent ones we requested explicitly and haven't released yet)
			assertEquals( 1, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
			assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
			assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
			assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
			assertEquals( 0, Monitor.theDependentBean().currentPreDestroyCount() );
			assertEquals( 0, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
			assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
			assertEquals( 3, Monitor.theNestedDependentBean().currentPreDestroyCount() );
		}

		// Here, the NonRegistryManagedBeanConsumingIntegrator has just been disintegrated and has released beans
		// The dependent beans should now have been released as well.
		assertEquals( 1, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 2, Monitor.theDependentBean().currentPreDestroyCount() );
		assertEquals( 2, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
		assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
		assertEquals( 7, Monitor.theNestedDependentBean().currentPreDestroyCount() );
	}

	private SessionFactoryImplementor buildSessionFactory(TestingExtendedBeanManager beanManager,
			NonRegistryManagedBeanConsumingIntegrator beanConsumingIntegrator) {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyIntegrator( beanConsumingIntegrator )
				.build();

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.CDI_BEAN_MANAGER, beanManager )
				.build();

		try {
			return (SessionFactoryImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
		}
		catch (Exception e) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}
	}
}
