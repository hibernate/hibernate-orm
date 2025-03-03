/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch.extended;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.cdi.general.hibernatesearch.Monitor;
import org.hibernate.orm.test.cdi.general.hibernatesearch.HibernateSearchSimulatedIntegrator;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheAlternativeNamedApplicationScopedBeanImpl;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheAlternativeNamedDependentBeanImpl;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheApplicationScopedBean;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheDependentBean;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheEntity;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheFallbackBeanInstanceProducer;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheMainNamedApplicationScopedBeanImpl;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheMainNamedDependentBeanImpl;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheNamedApplicationScopedBean;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheNamedDependentBean;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheNestedDependentBean;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheNonHibernateBeanConsumer;
import org.hibernate.orm.test.cdi.general.hibernatesearch.TheSharedApplicationScopedBean;
import org.hibernate.orm.test.cdi.testsupport.TestingExtendedBeanManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests support for requesting CDI beans in Hibernate Search
 * when the CDI BeanManager access is <strong>lazy</strong> (beans are instantiated when instances are first requested).
 *
 * In Hibernate Search,
 * beans are retrieved directly from the {@link org.hibernate.resource.beans.container.spi.BeanContainer}
 * because Hibernate Search is not bound by the JPA spec
 * and wants to leave the lifecycle of beans up to CDI instead
 * of controlling it in {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry}.
 * This involves using {@code canUseCachedReferences = false} and {@code useJpaCompliantCreation = false}
 * in {@link org.hibernate.resource.beans.container.spi.BeanContainer.LifecycleOptions}).
 *
 * @author Steve Ebersole
 * @author Yoann Rodiere
 *
 * @see HibernateSearchSimulatedIntegrator
 */
public class HibernateSearchExtendedCdiSupportTest extends BaseUnitTestCase {
	@Test
	public void test() {
		doTest( TestingExtendedBeanManager.create() );
	}

	private void doTest(TestingExtendedBeanManager beanManager) {
		Monitor.reset();

		final TheFallbackBeanInstanceProducer fallbackBeanInstanceProducer =
				new TheFallbackBeanInstanceProducer();
		final HibernateSearchSimulatedIntegrator beanConsumingIntegrator =
				new HibernateSearchSimulatedIntegrator( fallbackBeanInstanceProducer );

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

				// Here, the HibernateSearchSimulatedIntegrator has just been integrated and has requested beans
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

				// Here the HibernateSearchSimulatedIntegrator *did* fetch an instance of each bean,
				// so all beans should have been instantiated.
				// See HibernateSearchSimulatedIntegrator for a detailed list of requested beans

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

		// Here, the HibernateSearchSimulatedIntegrator has just been disintegrated and has released beans
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
			HibernateSearchSimulatedIntegrator beanConsumingIntegrator) {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyIntegrator( beanConsumingIntegrator )
				.build();

		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
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
