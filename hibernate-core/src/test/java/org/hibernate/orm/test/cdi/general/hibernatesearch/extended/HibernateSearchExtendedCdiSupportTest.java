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
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.cdi.general.hibernatesearch.HibernateSearchSimulatedIntegrator;
import org.hibernate.orm.test.cdi.general.hibernatesearch.Monitor;
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
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

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
@BaseUnitTest
public class HibernateSearchExtendedCdiSupportTest {
	@Test
	public void test() {
		Monitor.reset();

		final TestingExtendedBeanManager extendedBeanManager = TestingExtendedBeanManager.create();

		final TheFallbackBeanInstanceProducer fallbackBeanInstanceProducer = new TheFallbackBeanInstanceProducer();
		final HibernateSearchSimulatedIntegrator beanConsumingIntegrator = new HibernateSearchSimulatedIntegrator( fallbackBeanInstanceProducer );

		try (BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyIntegrator( beanConsumingIntegrator )
				.build()) {

			try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( JAKARTA_CDI_BEAN_MANAGER, extendedBeanManager )
					.build()) {

				final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
						.addAnnotatedClass( TheEntity.class )
						.buildMetadata();

				try (SessionFactoryImplementor sessionFactory = metadata.buildSessionFactory()) {
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
						Assertions.assertEquals( 0, Monitor.theApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 0,
								Monitor.theMainNamedApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 1,
								Monitor.theSharedApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 0, Monitor.theDependentBean().currentInstantiationCount() );
						Assertions.assertEquals( 0, Monitor.theMainNamedDependentBean().currentInstantiationCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedDependentBean().currentInstantiationCount() );
						Assertions.assertEquals( 0, fallbackBeanInstanceProducer.currentInstantiationCount() );
						Assertions.assertEquals( 0, fallbackBeanInstanceProducer.currentNamedInstantiationCount() );
						// Nested dependent bean: 1 instance per bean that depends on it
						Assertions.assertEquals( 1, Monitor.theNestedDependentBean().currentInstantiationCount() );

						extendedBeanManager.notifyListenerReady( cdiContainer.getBeanManager() );

						beanConsumingIntegrator.ensureInstancesInitialized();

						// Here the HibernateSearchSimulatedIntegrator *did* fetch an instance of each bean,
						// so all beans should have been instantiated.
						// See HibernateSearchSimulatedIntegrator for a detailed list of requested beans

						// Application scope: maximum 1 instance as soon as at least one was requested
						Assertions.assertEquals( 1, Monitor.theApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 1,
								Monitor.theMainNamedApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedApplicationScopedBean().currentInstantiationCount() );
						Assertions.assertEquals( 1,
								Monitor.theSharedApplicationScopedBean().currentInstantiationCount() );

						// Dependent scope: 1 instance per bean we requested explicitly
						Assertions.assertEquals( 2, Monitor.theDependentBean().currentInstantiationCount() );
						Assertions.assertEquals( 2, Monitor.theMainNamedDependentBean().currentInstantiationCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedDependentBean().currentInstantiationCount() );

						// Reflection-instantiated: 1 instance per bean we requested explicitly
						Assertions.assertEquals( 2, fallbackBeanInstanceProducer.currentInstantiationCount() );
						Assertions.assertEquals( 2, fallbackBeanInstanceProducer.currentNamedInstantiationCount() );

						// Nested dependent bean: 1 instance per bean that depends on it
						Assertions.assertEquals( 7, Monitor.theNestedDependentBean().currentInstantiationCount() );

						// Expect one PostConstruct call per CDI bean instance
						Assertions.assertEquals( 1, Monitor.theApplicationScopedBean().currentPostConstructCount() );
						Assertions.assertEquals( 1,
								Monitor.theMainNamedApplicationScopedBean().currentPostConstructCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedApplicationScopedBean().currentPostConstructCount() );
						Assertions.assertEquals( 1,
								Monitor.theSharedApplicationScopedBean().currentPostConstructCount() );
						Assertions.assertEquals( 2, Monitor.theDependentBean().currentPostConstructCount() );
						Assertions.assertEquals( 2, Monitor.theMainNamedDependentBean().currentPostConstructCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedDependentBean().currentPostConstructCount() );
						Assertions.assertEquals( 7, Monitor.theNestedDependentBean().currentPostConstructCount() );

						// Expect no PreDestroy call yet
						Assertions.assertEquals( 0, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0,
								Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0, Monitor.theDependentBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0,
								Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
						Assertions.assertEquals( 0, Monitor.theNestedDependentBean().currentPreDestroyCount() );
					}

					// After the CDI context has ended, PreDestroy should have been called on every "normal-scoped" CDI bean
					// (i.e. all CDI beans excepting the dependent ones we requested explicitly and haven't released yet)
					Assertions.assertEquals( 1, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
					Assertions.assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
					Assertions.assertEquals( 0,
							Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
					Assertions.assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
					Assertions.assertEquals( 0, Monitor.theDependentBean().currentPreDestroyCount() );
					Assertions.assertEquals( 0, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
					Assertions.assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
					Assertions.assertEquals( 3, Monitor.theNestedDependentBean().currentPreDestroyCount() );
				}
			}
		}

		// Here, the HibernateSearchSimulatedIntegrator has just been disintegrated and has released beans
		// The dependent beans should now have been released as well.
		Assertions.assertEquals( 1, Monitor.theApplicationScopedBean().currentPreDestroyCount() );
		Assertions.assertEquals( 1, Monitor.theMainNamedApplicationScopedBean().currentPreDestroyCount() );
		Assertions.assertEquals( 0, Monitor.theAlternativeNamedApplicationScopedBean().currentPreDestroyCount() );
		Assertions.assertEquals( 1, Monitor.theSharedApplicationScopedBean().currentPreDestroyCount() );
		Assertions.assertEquals( 2, Monitor.theDependentBean().currentPreDestroyCount() );
		Assertions.assertEquals( 2, Monitor.theMainNamedDependentBean().currentPreDestroyCount() );
		Assertions.assertEquals( 0, Monitor.theAlternativeNamedDependentBean().currentPreDestroyCount() );
		Assertions.assertEquals( 7, Monitor.theNestedDependentBean().currentPreDestroyCount() );
	}

}
