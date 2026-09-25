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
import org.hibernate.boot.MetadataSources;
import org.hibernate.callback.internal.ScopedInterceptorStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.Type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@JiraKey("HHH-12168")
class ScopedInterceptorStrategyCdiTest {

	@ParameterizedTest
	@ValueSource(strings = { "immediate", "delayed", "extended" })
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void scopedStrategyResolvesInjectsAndReleasesAcrossCdiModes(String access, CdiContainerScope cdi) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER,
						access.equals( "extended" ) ? cdi.getExtendedBeanManager() : cdi.getBeanManager() )
				.applySetting( AvailableSettings.DELAY_CDI_ACCESS, access.equals( "delayed" ) )
				.build() ) {
			// force ManagedBeanRegistry init so it registers its lifecycle listener
			services.requireService( ManagedBeanRegistry.class );
			final var strategy = new ScopedInterceptorStrategy( CdiScopedInterceptor.class, services );

			if ( access.equals( "extended" ) ) {
				assertFalse( cdi.isContainerAvailable() );
				cdi.triggerReadyForUse();
			}

			final var first = (CdiScopedInterceptor) strategy.getInterceptorForSession( null );
			final var second = (CdiScopedInterceptor) strategy.getInterceptorForSession( null );

			assertNotSame( first, second );
			assertEquals( 1, first.initializations );
			assertEquals( 1, second.initializations );
			assertNotNull( first.injectedService );
			assertNotNull( second.injectedService );

			strategy.releaseInterceptor( first );
			assertEquals( 1, first.destructions );
			assertEquals( 0, second.destructions );

			strategy.releaseInterceptor( second );
			assertEquals( 1, second.destructions );
		}
	}

	@Test
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void doubleReleaseIsHarmless(CdiContainerScope cdi) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.build() ) {
			final var strategy = new ScopedInterceptorStrategy( CdiScopedInterceptor.class, services );

			final var interceptor = (CdiScopedInterceptor) strategy.getInterceptorForSession( null );
			strategy.releaseInterceptor( interceptor );
			strategy.releaseInterceptor( interceptor );

			assertEquals( 1, interceptor.destructions );
		}
	}

	@Test
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void sessionCloseTriggersPreDestroy(CdiContainerScope cdi) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.applySetting( AvailableSettings.SESSION_SCOPED_INTERCEPTOR, CdiScopedInterceptor.class )
				.applySetting( HBM2DDL_AUTO, "create-drop" )
				.build() ) {
			try ( var sessionFactory = (SessionFactoryImplementor) new MetadataSources( services )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build() ) {
				assertInstanceOf( ScopedInterceptorStrategy.class,
						sessionFactory.getSessionFactoryOptions().getInterceptorStrategy() );

				final CdiScopedInterceptor interceptorRef;
				try ( var session = sessionFactory.withOptions().openSession() ) {
					final var interceptor = session.getInterceptor();
					assertInstanceOf( CdiScopedInterceptor.class, interceptor );
					interceptorRef = (CdiScopedInterceptor) interceptor;

					assertEquals( 1, interceptorRef.initializations );
					assertNotNull( interceptorRef.injectedService );
					assertEquals( 0, interceptorRef.destructions );
				}
				assertEquals( 1, interceptorRef.destructions );
			}
		}
	}

	@Dependent
	public static class CdiScopedInterceptor implements Interceptor {
		int initializations;
		int destructions;

		@Inject
		InjectedService injectedService;

		@PostConstruct
		void init() {
			initializations++;
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
