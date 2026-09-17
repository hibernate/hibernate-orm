/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.bootstrapsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.ApplicationScoped;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.BeanInstanceCaching;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.orm.test.cdi.general.mixed.HostedBean;
import org.hibernate.orm.test.cdi.general.mixed.InjectedHostedBean;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies ownership with real CDI containers, including session-time concurrency.
///
/// @author Steve Ebersole
@Timeout(60)
class BeanContainerLifecycleTest {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	@CdiContainer(beanClasses = ApplicationBean.class)
	void cachedBeansRespectLifecyclePolicy(boolean jpaFirst, CdiContainerScope cdi) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.build() ) {
			final var container = services.requireService( ManagedBeanRegistry.class ).getBeanContainer();
			final var first = container.getBean( ApplicationBean.class,
					new CacheOptions( jpaFirst ), FallbackBeanInstanceProducer.INSTANCE );
			final var second = container.getBean( ApplicationBean.class,
					new CacheOptions( !jpaFirst ), FallbackBeanInstanceProducer.INSTANCE );
			final var jpaBean = jpaFirst ? first : second;
			final var contextualBean = jpaFirst ? second : first;
			final var contextualInstance = cdi.getContainer().select( ApplicationBean.class ).get();
			assertNotSame( jpaBean, contextualBean );
			assertSame( contextualInstance.identity(), contextualBean.getBeanInstance().identity() );
			assertNotSame( contextualInstance.identity(), jpaBean.getBeanInstance().identity() );
			assertSame( jpaBean, container.getBean( ApplicationBean.class,
					new CacheOptions( true ), FallbackBeanInstanceProducer.INSTANCE ) );
			assertSame( contextualBean, container.getBean( ApplicationBean.class,
					new CacheOptions( false ), FallbackBeanInstanceProducer.INSTANCE ) );
			final var jpaInstance = jpaBean.getBeanInstance();
			container.releaseBean( jpaBean );
			assertEquals( 1, jpaInstance.destructions );
			assertSame( contextualBean, container.getBean( ApplicationBean.class,
					new CacheOptions( false ), FallbackBeanInstanceProducer.INSTANCE ) );
			container.stop();
			assertEquals( 1, jpaInstance.destructions );
		}
	}

	private record CacheOptions(boolean useJpaCompliantCreation) implements BeanContainer.LifecycleOptions {
		@Override
		public boolean canUseCachedReferences() {
			return true;
		}
	}

	@ApplicationScoped
	public static class ApplicationBean {
		private final Object identity = new Object();
		int destructions;

		public Object identity() {
			return identity;
		}

		@PreDestroy
		void destroy() {
			destructions++;
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = { HostedBean.class, InjectedHostedBean.class })
	void bootstrapAcquisitionReuseAndInjection(String access, CdiContainerScope cdi) {
		assertFalse( cdi.isContainerAvailable() );
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER,
						access.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager() )
				.applySetting( AvailableSettings.DELAY_CDI_ACCESS, access.equals( "delayed" ) )
				.build() ) {
			final var registry = services.requireService( ManagedBeanRegistry.class );
			final var bean = registry.getBootstrapSafeBean( HostedBean.class );
			assertSame( bean, registry.getBootstrapSafeBean( HostedBean.class ) );
			if ( access.equals( "extended" ) ) {
				assertFalse( cdi.isContainerAvailable() );
				cdi.triggerReadyForUse();
			}
			final var instance = bean.getBeanInstance();
			assertNotNull( instance.getInjectedHostedBean() );
			assertSame( instance, registry.getBootstrapSafeBean( HostedBean.class ).getBeanInstance() );
		}
	}

	@Test
	@CdiContainer(beanClasses = { HostedBean.class, InjectedHostedBean.class })
	void injectionOnlyBeanCannotResolveBeforeCdiReady(CdiContainerScope cdi) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getExtendedBeanManager() )
				.build() ) {
			final var bean = services.requireService( ManagedBeanRegistry.class )
					.getBootstrapSafeBean( HostedBean.class );
			// HostedBean requires constructor injection and cannot be constructed by the fallback producer.
			assertThrows( IllegalStateException.class, bean::getBeanInstance );
			assertFalse( cdi.isContainerAvailable() );
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = LifecycleBean.class)
	void distinctReleaseAndShutdown(String access, CdiContainerScope cdi) {
		assertFalse( cdi.isContainerAvailable() );
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER,
						access.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager() )
				.applySetting( AvailableSettings.DELAY_CDI_ACCESS, access.equals( "delayed" ) )
				.build() ) {
			final var registry = services.requireService( ManagedBeanRegistry.class );
			// Acquire before the extended manager signals readiness.
			final var first = registry.getBean( LifecycleBean.class, BeanInstanceCaching.DISALLOW );
			final var second = registry.getBean( LifecycleBean.class, BeanInstanceCaching.DISALLOW );
			if ( access.equals( "extended" ) ) {
				assertFalse( cdi.isContainerAvailable() );
				cdi.triggerReadyForUse();
			}
			final var firstInstance = first.getBeanInstance();
			final var secondInstance = second.getBeanInstance();
			assertNotSame( firstInstance, secondInstance );
			assertEquals( 1, firstInstance.initializations );
			registry.releaseBean( first );
			registry.releaseBean( first );
			assertEquals( 1, firstInstance.destructions );
			assertEquals( 0, secondInstance.destructions );
			registry.getBeanContainer().stop();
			assertEquals( 1, firstInstance.destructions );
			assertEquals( 1, secondInstance.destructions );
			registry.releaseBean( second );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	@CdiContainer(beanClasses = LifecycleBean.class)
	void bootstrapRelease(boolean resolve, CdiContainerScope cdi) {
		assertFalse( cdi.isContainerAvailable() );
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.build() ) {
			final var registry = services.requireService( ManagedBeanRegistry.class );
			final var bean = registry.getBootstrapSafeBean( LifecycleBean.class );
			final var instance = resolve ? bean.getBeanInstance() : null;
			registry.releaseBean( bean );
			registry.releaseBean( bean );
			assertThrows( IllegalStateException.class, bean::getBeanInstance );
			final var replacement = registry.getBootstrapSafeBean( LifecycleBean.class );
			assertNotSame( bean, replacement );
			registry.getBeanContainer().stop();
			if ( resolve ) {
				assertEquals( 1, instance.destructions );
			}
		}
	}

	@Test
	@CdiContainer(beanClasses = LifecycleBean.class)
	void concurrentDistinctAcquisitionAndRelease(CdiContainerScope cdi) throws Exception {
		assertFalse( cdi.isContainerAvailable() );
		final var executor = Executors.newFixedThreadPool( 8 );
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.build() ) {
			final var registry = services.requireService( ManagedBeanRegistry.class );
			final var start = new CountDownLatch( 1 );
			final var contested = registry.getBean( LifecycleBean.class, BeanInstanceCaching.DISALLOW );
			final var contestedInstance = contested.getBeanInstance();
			final List<Future<List<LifecycleBean>>> futures = new ArrayList<>();
			for ( int thread = 0; thread < 8; thread++ ) {
				futures.add( executor.submit( () -> {
					start.await();
					registry.releaseBean( contested );
					final List<LifecycleBean> instances = new ArrayList<>();
					for ( int i = 0; i < 20; i++ ) {
						final var bean = registry.getBean( LifecycleBean.class, BeanInstanceCaching.DISALLOW );
						instances.add( bean.getBeanInstance() );
						if ( i % 2 == 0 ) {
							registry.releaseBean( bean );
						}
					}
					return instances;
				} ) );
			}
			start.countDown();
			final List<LifecycleBean> instances = new ArrayList<>();
			for ( var future : futures ) {
				instances.addAll( future.get() );
			}
			registry.getBeanContainer().stop();
			instances.forEach( bean -> assertEquals( 1, bean.destructions ) );
			assertEquals( 1, contestedInstance.destructions );
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Dependent
	public static class LifecycleBean {
		int initializations;
		int destructions;

		@PostConstruct
		void initialize() {
			initializations++;
		}

		@PreDestroy
		void destroy() {
			destructions++;
		}
	}

}
