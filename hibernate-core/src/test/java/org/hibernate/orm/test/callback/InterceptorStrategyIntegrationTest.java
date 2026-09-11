/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Interceptor;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link org.hibernate.callback.spi.InterceptorStrategy} implementations
 * and regression coverage for all interceptor resolution patterns.
 *
 * @author Sean Okafor
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12168")
@ServiceRegistry(settings = @Setting(
		name = SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
		value = "org.hibernate.orm.test.callback.InterceptorStrategyIntegrationTest$TrackingInterceptor"))
@DomainModel(annotatedClasses = InterceptorStrategyIntegrationTest.SimpleEntity.class)
@SessionFactory
public class InterceptorStrategyIntegrationTest {

	@BeforeEach
	void setUp() {
		TrackingInterceptor.clearInstances();
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	// -- New functionality tests (ScopedInterceptorStrategy via class-level factory) --

	@Test
	public void testEachSessionGetsDistinctInterceptor(SessionFactoryScope factoryScope) {
		try (var session1 = factoryScope.getSessionFactory().openSession();
			var session2 = factoryScope.getSessionFactory().openSession()) {
			final var interceptor1 = session1.getInterceptor();
			final var interceptor2 = session2.getInterceptor();
			assertNotSame( interceptor1, interceptor2 );
		}
	}

	@Test
	public void testInterceptorCallbacksFire(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new SimpleEntity( "test" ) );
		} );
		assertEquals( 1, TrackingInterceptor.INSTANCES.size() );
		assertTrue( TrackingInterceptor.INSTANCES.get( 0 ).isPersistCalled() );
	}

	@Test
	public void testPerSessionStateIsolation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new SimpleEntity( "one" ) );
		} );
		factoryScope.inTransaction( session -> {
			session.persist( new SimpleEntity( "two" ) );
			session.persist( new SimpleEntity( "three" ) );
		} );
		//checks there is only 2 instances of the interceptor
		assertEquals( 2, TrackingInterceptor.INSTANCES.size() );
		//checks that the interceptors have the correct persistCount
		assertEquals( 1, TrackingInterceptor.INSTANCES.get( 0 ).getPersistCount() );
		assertEquals( 2, TrackingInterceptor.INSTANCES.get( 1 ).getPersistCount() );
	}

	@Test
	public void testCleanupOnSessionClose(SessionFactoryScope factoryScope) {
		final var session = factoryScope.getSessionFactory().openSession();
		session.beginTransaction();
		session.persist( new SimpleEntity( "cleanup" ) );
		session.getTransaction().commit();
		assertDoesNotThrow( session::close );
		assertEquals( 1, TrackingInterceptor.INSTANCES.size() );
		assertTrue( TrackingInterceptor.INSTANCES.get( 0 ).isPersistCalled() );
	}

	// -- Regression tests (verify existing interceptor patterns still work) --

	@Test
	public void testNoInterceptorConfigured() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build()) {
			try (var sf = new MetadataSources( ssr )
					.addAnnotatedClass( ManualTestEntity.class )
					.buildMetadata()
					.buildSessionFactory()) {
				final var session = sf.openSession();
				session.beginTransaction();
				session.persist( new ManualTestEntity( "no-interceptor" ) );
				session.getTransaction().commit();
				assertDoesNotThrow( session::close );
			}
		}
	}

	@Test
	public void testFactoryLevelInterceptorAsClassName() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( SessionEventSettings.INTERCEPTOR, TrackingInterceptor.class.getName() )
				.build()) {
			try (var sf = new MetadataSources( ssr )
					.addAnnotatedClass( ManualTestEntity.class )
					.buildMetadata()
					.buildSessionFactory()) {
				Interceptor interceptor1;
				try (var session1 = sf.openSession()) {
					interceptor1 = ((SharedSessionContractImplementor) session1).getInterceptor();
					session1.beginTransaction();
					session1.persist( new ManualTestEntity( "class-name" ) );
					session1.getTransaction().commit();
				}
				Interceptor interceptor2;
				try (var session2 = sf.openSession()) {
					interceptor2 = ((SharedSessionContractImplementor) session2).getInterceptor();
				}
				assertSame( interceptor1, interceptor2 );
				assertTrue( TrackingInterceptor.INSTANCES.stream()
						.anyMatch( TrackingInterceptor::isPersistCalled ) );
			}
		}
	}

	@Test
	public void testFactoryLevelInterceptorAsInstance() {
		final var interceptor = new TrackingInterceptor();
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( SessionEventSettings.INTERCEPTOR, interceptor )
				.build()) {
			try (var sf = new MetadataSources( ssr )
					.addAnnotatedClass( ManualTestEntity.class )
					.buildMetadata()
					.buildSessionFactory()) {
				Interceptor resolved1;
				try (var session1 = sf.openSession()) {
					resolved1 = ((SharedSessionContractImplementor) session1).getInterceptor();
					session1.beginTransaction();
					session1.persist( new ManualTestEntity( "instance" ) );
					session1.getTransaction().commit();
				}
				Interceptor resolved2;
				try (var session2 = sf.openSession()) {
					resolved2 = ((SharedSessionContractImplementor) session2).getInterceptor();
				}
				assertSame( interceptor, resolved1 );
				assertSame( resolved1, resolved2 );
				assertTrue( interceptor.isPersistCalled() );
			}
		}
	}

	@Test
	public void testPerSessionInterceptorViaSessionBuilder(SessionFactoryScope factoryScope) {
		final var perSessionInterceptor = new TrackingInterceptor();
		factoryScope.inTransaction(
				factory -> factory.withOptions().interceptor( perSessionInterceptor ).openSession(),
				session -> {
					session.persist( new SimpleEntity( "per-session" ) );
				}
		);
		assertTrue( perSessionInterceptor.isPersistCalled() );
	}

	@Test
	public void testPerSessionInterceptorTakesPriority(SessionFactoryScope factoryScope) {
		final var perSessionInterceptor = new TrackingInterceptor();
		try (var session = factoryScope.getSessionFactory().withOptions()
				.interceptor( perSessionInterceptor ).openSession()) {
			final var resolved = session.getInterceptor();
			assertSame( perSessionInterceptor, resolved );
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSupplierBasedSessionScopedInterceptor() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
						(Supplier<TrackingInterceptor>) TrackingInterceptor::new )
				.build()) {
			try (var sf = new MetadataSources( ssr )
					.addAnnotatedClass( ManualTestEntity.class )
					.buildMetadata()
					.buildSessionFactory()) {
				try (var session = sf.openSession()) {
					session.beginTransaction();
					session.persist( new ManualTestEntity( "supplier" ) );
					session.getTransaction().commit();
				}
				assertEquals( 1, TrackingInterceptor.INSTANCES.size() );
				assertTrue( TrackingInterceptor.INSTANCES.get( 0 ).isPersistCalled() );
			}
		}
	}

	@Test
	public void testSessionCloseWithPerSessionInterceptorNoException(SessionFactoryScope factoryScope) {
		final var perSessionInterceptor = new TrackingInterceptor();
		final var session = factoryScope.getSessionFactory().withOptions()
				.interceptor( perSessionInterceptor ).openSession();
		session.beginTransaction();
		session.persist( new SimpleEntity( "close-test" ) );
		session.getTransaction().commit();
		assertDoesNotThrow( session::close );
	}

	@Test
	public void testSessionCloseWithNoInterceptorNoException() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build()) {
			try (var sf = new MetadataSources( ssr )
					.addAnnotatedClass( ManualTestEntity.class )
					.buildMetadata()
					.buildSessionFactory()) {
				final var session = sf.openSession();
				session.beginTransaction();
				session.persist( new ManualTestEntity( "close-no-interceptor" ) );
				session.getTransaction().commit();
				assertDoesNotThrow( session::close );
			}
		}
	}

	// -- Inner classes --

	public static class TrackingInterceptor implements Interceptor {
		static final CopyOnWriteArrayList<TrackingInterceptor> INSTANCES = new CopyOnWriteArrayList<>();

		private int persistCount;

		public TrackingInterceptor() {
			INSTANCES.add( this );
		}

		@Override
		public boolean onPersist(Object entity, Object id, Object[] state,
								String[] propertyNames, Type[] types) {
			persistCount++;
			return false;
		}

		public boolean isPersistCalled() {
			return persistCount > 0;
		}

		public int getPersistCount() {
			return persistCount;
		}

		static void clearInstances() {
			INSTANCES.clear();
		}
	}

	@Entity(name = "SimpleEntity")
	static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;

		protected SimpleEntity() {
		}

		SimpleEntity(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ManualTestEntity")
	static class ManualTestEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;

		protected ManualTestEntity() {
		}

		ManualTestEntity(String name) {
			this.name = name;
		}
	}
}
