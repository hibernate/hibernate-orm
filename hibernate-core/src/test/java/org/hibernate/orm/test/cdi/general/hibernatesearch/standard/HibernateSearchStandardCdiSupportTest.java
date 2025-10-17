/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.hibernatesearch.standard;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.se.SeContainer;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
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
import org.hibernate.orm.test.cdi.general.hibernatesearch.delayed.HibernateSearchDelayedCdiSupportTest;
import org.hibernate.orm.test.cdi.general.hibernatesearch.extended.HibernateSearchExtendedCdiSupportTest;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests support for requesting CDI beans in Hibernate Search
 * when the CDI BeanManager access is <strong>available right away</strong> during bootstrap
 * (not {@link HibernateSearchDelayedCdiSupportTest delayed}
 * nor {@link HibernateSearchExtendedCdiSupportTest lazy}).
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
@SuppressWarnings("JUnitMalformedDeclaration")
public class HibernateSearchStandardCdiSupportTest {
	@Test
	@ExtendWith( Monitor.Resetter.class )
	@CdiContainer(beanClasses = {
			TheApplicationScopedBean.class,
			TheNamedApplicationScopedBean.class,
			TheMainNamedApplicationScopedBeanImpl.class,
			TheAlternativeNamedApplicationScopedBeanImpl.class,
			TheSharedApplicationScopedBean.class,
			TheDependentBean.class,
			TheNamedDependentBean.class,
			TheMainNamedDependentBeanImpl.class,
			TheAlternativeNamedDependentBeanImpl.class,
			TheNestedDependentBean.class,
			TheNonHibernateBeanConsumer.class
	})
	public void testIt(CdiContainerScope cdiContainerScope) {
		assertFalse( cdiContainerScope.isContainerAvailable() );

		final TheFallbackBeanInstanceProducer fallbackBeanInstanceProducer = new TheFallbackBeanInstanceProducer();
		final HibernateSearchSimulatedIntegrator beanConsumingIntegrator = new HibernateSearchSimulatedIntegrator( fallbackBeanInstanceProducer );

		try (SeContainer cdiContainer = cdiContainerScope.getContainer()) {
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
				// Here, the HibernateSearchSimulatedIntegrator has just been integrated and has requested beans
				// See HibernateSearchSimulatedIntegrator for a detailed list of requested beans

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

			// Here, the HibernateSearchSimulatedIntegrator has just been disintegrated and has released beans

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
			HibernateSearchSimulatedIntegrator beanConsumingIntegrator) {
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyIntegrator( beanConsumingIntegrator )
				.build();

		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
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
