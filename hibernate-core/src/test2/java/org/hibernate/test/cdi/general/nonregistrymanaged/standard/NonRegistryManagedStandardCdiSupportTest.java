/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.nonregistrymanaged.standard;

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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests support for requesting CDI beans from the {@link ManagedBeanRegistry}
 * when the CDI BeanManager is available right away during bootstrap,
 * and when the registry should not manage the lifecycle of beans, but leave it up to CDI.
 *
 * @author Steve Ebersole
 * @author Yoann Rodiere
 */
public class NonRegistryManagedStandardCdiSupportTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		Monitor.reset();

		final TheFallbackBeanInstanceProducer fallbackBeanInstanceProducer =
				new TheFallbackBeanInstanceProducer();
		final NonRegistryManagedBeanConsumingIntegrator beanConsumingIntegrator =
				new NonRegistryManagedBeanConsumingIntegrator( fallbackBeanInstanceProducer );

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

			// Expect the shared bean to have been instantiated already, but only that one
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

			try (SessionFactoryImplementor sessionFactory = buildSessionFactory( cdiContainer, beanConsumingIntegrator )) {
				// Here, the NonRegistryManagedBeanConsumingIntegrator has just been integrated and has requested beans
				// See NonRegistryManagedBeanConsumingIntegrator for a detailed list of requested beans

				beanConsumingIntegrator.ensureInstancesInitialized();

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

			// Here, the NonRegistryManagedBeanConsumingIntegrator has just been disintegrated and has released beans

			// release() should have an effect on exclusively used application-scoped beans
			assertEquals( 1, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
			assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
			assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );

			// release() should have no effect on shared application-scoped beans (they will be released when they are no longer used)
			assertEquals( 0, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );

			// release() should have an effect on dependent-scoped beans
			assertEquals( 2, Monitor.theDependentBean().currentPreDestroyCount() );
			assertEquals( 2, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
			assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
			// The nested dependent bean instances should have been destroyed along with the beans that depend on them
			// (the instances used in application-scoped beans should not have been destroyed)
			assertEquals( 6, Monitor.theNestedDependentBean().currentPreDestroyCount() );
		}

		// After the CDI context has ended, PreDestroy should have been called on every created CDI bean
		// (see the assertions about instantiations above for an explanation of the expected counts)
		assertEquals( 1, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
		assertEquals( 2, Monitor.theDependentBean().currentPreDestroyCount() );
		assertEquals( 2, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
		assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
		assertEquals( 7, Monitor.theNestedDependentBean().currentPreDestroyCount() );
	}

	private SessionFactoryImplementor buildSessionFactory(SeContainer cdiContainer,
			NonRegistryManagedBeanConsumingIntegrator beanConsumingIntegrator) {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyIntegrator( beanConsumingIntegrator )
				.build();

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
				.build();

		try {
			return (SessionFactoryImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
		}
		catch ( Exception e ) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}
	}

}
