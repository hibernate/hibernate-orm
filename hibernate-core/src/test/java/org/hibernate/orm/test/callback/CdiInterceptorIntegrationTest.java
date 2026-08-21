/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Interceptor;
import org.hibernate.cfg.ManagedBeanSettings;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CDI integration tests for {@link org.hibernate.callback.internal.ScopedInterceptorStrategy}.
 * Verifies that interceptors are created via CDI with dependency injection and lifecycle management,
 * as opposed to the reflection fallback tested in {@link InterceptorStrategyIntegrationTest}.
 *
 * @author Sean Okafor
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12168")
public class CdiInterceptorIntegrationTest {

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = @Setting(
					name = SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
					value = "org.hibernate.orm.test.callback.CdiInterceptorIntegrationTest$CdiTrackingInterceptor"
			),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testCdiManagedInterceptorWithInjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "cdi-test" ) );
		} );
		assertTrue( InterceptorMonitor.wasInstantiated() );
		assertEquals( 1, InterceptorMonitor.currentPersistCount() );
		assertEquals( 1, CdiTrackingInterceptor.INSTANCES.size() );
		assertTrue( CdiTrackingInterceptor.INSTANCES.get( 0 ).isPostConstructCalled() );
	}

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = @Setting(
					name = SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
					value = "org.hibernate.orm.test.callback.CdiInterceptorIntegrationTest$CdiTrackingInterceptor"
			),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testEachSessionGetsDistinctCdiManagedInterceptor(SessionFactoryScope factoryScope) {
		try (var session1 = factoryScope.getSessionFactory().openSession();
			var session2 = factoryScope.getSessionFactory().openSession()) {
			final var interceptor1 = session1.getInterceptor();
			final var interceptor2 = session2.getInterceptor();
			assertNotNull( interceptor1 );
			assertNotNull( interceptor2 );
			assertNotSame( interceptor1, interceptor2 );
		}
	}

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = @Setting(
					name = SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
					value = "org.hibernate.orm.test.callback.CdiInterceptorIntegrationTest$CdiTrackingInterceptor"
			),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testCdiManagedInterceptorSessionCloseNoException(SessionFactoryScope factoryScope) {
		final var session = factoryScope.getSessionFactory().openSession();
		session.beginTransaction();
		session.persist( new CdiSimpleEntity( "cdi-close" ) );
		session.getTransaction().commit();
		assertDoesNotThrow( session::close );
		assertEquals( 1, CdiTrackingInterceptor.INSTANCES.size() );
		assertTrue( CdiTrackingInterceptor.INSTANCES.get( 0 ).isPersistCalled() );
	}

	// -- Inner classes --

	public static class InterceptorMonitor {
		private static boolean instantiated;
		private static final AtomicInteger persistCount = new AtomicInteger();

		public InterceptorMonitor() {
			instantiated = true;
		}

		public void entityPersisted() {
			persistCount.incrementAndGet();
		}

		public static boolean wasInstantiated() {
			return instantiated;
		}

		public static int currentPersistCount() {
			return persistCount.get();
		}

		public static void reset() {
			instantiated = false;
			persistCount.set( 0 );
		}

		public static class Resetter implements BeforeEachCallback {
			@Override
			public void beforeEach(ExtensionContext context) {
				InterceptorMonitor.reset();
				CdiTrackingInterceptor.clearInstances();
			}
		}
	}

	public static class CdiTrackingInterceptor implements Interceptor {
		static final CopyOnWriteArrayList<CdiTrackingInterceptor> INSTANCES = new CopyOnWriteArrayList<>();

		private final InterceptorMonitor monitor;
		private boolean postConstructCalled;
		private int persistCount;

		@Inject
		public CdiTrackingInterceptor(InterceptorMonitor monitor) {
			this.monitor = monitor;
			INSTANCES.add( this );
		}

		@PostConstruct
		void onPostConstruct() {
			postConstructCalled = true;
		}

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state,
								String[] propertyNames, Type[] types) {
			persistCount++;
			monitor.entityPersisted();
			return false;
		}

		public boolean isPostConstructCalled() {
			return postConstructCalled;
		}

		public boolean isPersistCalled() {
			return persistCount > 0;
		}

		static void clearInstances() {
			INSTANCES.clear();
		}
	}

	@Entity(name = "CdiSimpleEntity")
	static class CdiSimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;

		protected CdiSimpleEntity() {
		}

		CdiSimpleEntity(String name) {
			this.name = name;
		}
	}
}
