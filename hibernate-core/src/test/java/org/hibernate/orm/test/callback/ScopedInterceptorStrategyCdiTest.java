/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.callback.internal.ScopedInterceptorStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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
import static org.junit.jupiter.api.Assertions.assertSame;

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
		withScopedSessionFactory( cdi, sessionFactory -> {
			assertInstanceOf( ScopedInterceptorStrategy.class, sessionFactory.getInterceptorStrategy() );

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
		} );
	}

	@Test
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void childSessionCloseDoesNotReleaseParentInterceptor(CdiContainerScope cdi) {
		withScopedSessionFactory( cdi, sessionFactory -> {
			final CdiScopedInterceptor parentInterceptorRef;
			try ( var parentSession = sessionFactory.withOptions().openSession() ) {
				parentInterceptorRef = (CdiScopedInterceptor) parentSession.getInterceptor();
				assertEquals( 1, parentInterceptorRef.initializations );
				assertEquals( 0, parentInterceptorRef.destructions );

				try ( var childSession = (SessionImplementor) parentSession.sessionWithOptions()
						.interceptor()
						.connection()
						.openSession() ) {
					assertSame( parentInterceptorRef, childSession.getInterceptor() );
				}
				assertEquals( 0, parentInterceptorRef.destructions,
						"Child session close should NOT release the shared interceptor" );
			}
			assertEquals( 1, parentInterceptorRef.destructions );
		} );
	}

	@Test
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void parentClosingFirstDoesNotDestroySharedInterceptorWhileChildStillOpen(CdiContainerScope cdi) {
		withScopedSessionFactory( cdi, sessionFactory -> {
			final var parentSession = sessionFactory.withOptions().openSession();
			final var parentInterceptorRef = (CdiScopedInterceptor) parentSession.getInterceptor();

			final var childSession = (SessionImplementor) parentSession.sessionWithOptions()
					.interceptor()
					.openSession();
			assertSame( parentInterceptorRef, childSession.getInterceptor() );

			parentSession.close();
			assertEquals( 0, parentInterceptorRef.destructions,
					"Interceptor must not be destroyed while the child session is still open" );

			childSession.close();
			assertEquals( 1, parentInterceptorRef.destructions );
		} );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2 })
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void multipleChildrenSharingSameParentInterceptor_DestroyedOnlyAfterLastCloses(
			int closeLastIndex, CdiContainerScope cdi) {
		withScopedSessionFactory( cdi, sessionFactory -> {
			final var parentSession = sessionFactory.withOptions().openSession();
			final var interceptorRef = (CdiScopedInterceptor) parentSession.getInterceptor();

			final var child1 = (SessionImplementor) parentSession.sessionWithOptions()
					.interceptor()
					.openSession();
			final var child2 = (SessionImplementor) parentSession.sessionWithOptions()
					.interceptor()
					.openSession();
			assertSame( interceptorRef, child1.getInterceptor() );
			assertSame( interceptorRef, child2.getInterceptor() );

			final var holders = new ArrayList<Session>( List.of( parentSession, child1, child2 ) );
			final Session closeLast = holders.remove( closeLastIndex );

			for ( var holder : holders ) {
				holder.close();
				assertEquals( 0, interceptorRef.destructions,
						"Interceptor must survive while any holder is still open" );
			}

			closeLast.close();
			assertEquals( 1, interceptorRef.destructions );
		} );
	}

	@Test
	@CdiContainer(beanClasses = { CdiScopedInterceptor.class, InjectedService.class })
	void childResolvingOwnFreshScopedBeanReleasesIndependentlyOfParent(CdiContainerScope cdi) {
		withScopedSessionFactory( cdi, sessionFactory -> {
			final var parentSession = sessionFactory.withOptions().openSession();
			final var parentInterceptorRef = (CdiScopedInterceptor) parentSession.getInterceptor();

			final var childSession = (SessionImplementor) parentSession.sessionWithOptions().openSession();
			final var childInterceptorRef = (CdiScopedInterceptor) childSession.getInterceptor();

			assertNotSame( parentInterceptorRef, childInterceptorRef );

			parentSession.close();
			assertEquals( 1, parentInterceptorRef.destructions );
			assertEquals( 0, childInterceptorRef.destructions );

			childSession.close();
			assertEquals( 1, childInterceptorRef.destructions );
			assertEquals( 1, parentInterceptorRef.destructions );
		} );
	}

	private void withScopedSessionFactory(CdiContainerScope cdi, Consumer<SessionFactoryImplementor> action) {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, cdi.getBeanManager() )
				.applySetting( AvailableSettings.SESSION_SCOPED_INTERCEPTOR, CdiScopedInterceptor.class )
				.applySetting( HBM2DDL_AUTO, "create-drop" )
				.build() ) {
			try ( var sessionFactory = (SessionFactoryImplementor) new MetadataSources( services )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build() ) {
				action.accept( sessionFactory );
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
