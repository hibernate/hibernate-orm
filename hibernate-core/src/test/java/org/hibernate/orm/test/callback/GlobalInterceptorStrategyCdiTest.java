/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.hibernate.Interceptor;
import org.hibernate.callback.internal.GlobalInterceptorStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.Type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-12168")
class GlobalInterceptorStrategyCdiTest {

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = { CdiGlobalInterceptor.class, InjectedService.class })
	void globalStrategyResolvesAndInjectsAcrossCdiModes(String access, CdiContainerScope cdi) {
		CdiGlobalInterceptor.destructions = 0;
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER,
						access.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager() )
				.applySetting( AvailableSettings.DELAY_CDI_ACCESS, access.equals( "delayed" ) )
				.build() ) {
			final var strategy = new GlobalInterceptorStrategy( CdiGlobalInterceptor.class, services );

			if ( access.equals( "extended" ) ) {
				assertFalse( cdi.isContainerAvailable() );
				cdi.triggerReadyForUse();
			}

			final var first = strategy.getInterceptorForSession( null );
			final var second = strategy.getInterceptorForSession( null );

			assertSame( first, second );
			assertTrue( ((CdiGlobalInterceptor) first).initialized );
			assertNotNull( ((CdiGlobalInterceptor) first).injectedService );
		}
	}

	@Test
	@CdiContainer(beanClasses = { CdiGlobalInterceptor.class, InjectedService.class })
	void factoryInterceptorReturnsSameCdiInstance(CdiContainerScope cdi) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.build() ) {
			final var strategy = new GlobalInterceptorStrategy( CdiGlobalInterceptor.class, services );

			final var sessionInterceptor = strategy.getInterceptorForSession( null );
			final var factoryInterceptor = strategy.getFactoryInterceptor();

			assertNotNull( factoryInterceptor );
			assertSame( sessionInterceptor, factoryInterceptor );
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = { CdiGlobalInterceptor.class, InjectedService.class })
	void globalBeanDestroyedOnRegistryShutdown(String access, CdiContainerScope cdi) {
		CdiGlobalInterceptor.destructions = 0;
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER,
						access.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager() )
				.applySetting( AvailableSettings.DELAY_CDI_ACCESS, access.equals( "delayed" ) )
				.build() ) {
			services.requireService( ManagedBeanRegistry.class );
			final var strategy = new GlobalInterceptorStrategy( CdiGlobalInterceptor.class, services );

			if ( access.equals( "extended" ) ) {
				assertFalse( cdi.isContainerAvailable() );
				cdi.triggerReadyForUse();
			}

			final var interceptor = (CdiGlobalInterceptor) strategy.getInterceptorForSession( null );
			assertTrue( interceptor.initialized );
			assertEquals( 0, CdiGlobalInterceptor.destructions );
		}
		assertEquals( 1, CdiGlobalInterceptor.destructions );
	}

	@Dependent
	public static class CdiGlobalInterceptor implements Interceptor {
		boolean initialized;
		static int destructions;

		@Inject
		InjectedService injectedService;

		@PostConstruct
		void init() {
			initialized = true;
		}

		@PreDestroy
		void destroy() {
			destructions++;
		}

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
			return false;
		}
	}

	@Dependent
	public static class InjectedService {
	}
}
