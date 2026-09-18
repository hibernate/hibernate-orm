/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CDI lifecycle integration tests for {@link org.hibernate.callback.spi.InterceptorStrategy} implementations.
 * Verifies CDI-specific behaviors such as session close safety and shared dependency state
 * for both scoped and global interceptor strategies.
 * Access-type-specific coverage is in {@link CdiScopedInterceptorAccessTest}
 * and {@link CdiGlobalInterceptorAccessTest}.
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
					value = "org.hibernate.orm.test.callback.CdiTrackingInterceptor"
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

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = @Setting(
					name = SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
					value = "org.hibernate.orm.test.callback.CdiTrackingInterceptor"
			),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testScopedInjectedDependencyAccumulatesAcrossSessions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "first" ) );
		} );
		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "second" ) );
		} );
		assertEquals( 2, InterceptorMonitor.currentPersistCount() );
		assertEquals( 2, CdiTrackingInterceptor.INSTANCES.size() );
	}

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = @Setting(
					name = SessionEventSettings.INTERCEPTOR,
					value = "org.hibernate.orm.test.callback.CdiTrackingInterceptor"
			),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testGlobalCdiManagedInterceptorSessionCloseNoException(SessionFactoryScope factoryScope) {
		final var session = factoryScope.getSessionFactory().openSession();
		session.beginTransaction();
		session.persist( new CdiSimpleEntity( "global-cdi-close" ) );
		session.getTransaction().commit();
		assertDoesNotThrow( session::close );
		assertEquals( 1, CdiTrackingInterceptor.INSTANCES.size() );
		assertTrue( CdiTrackingInterceptor.INSTANCES.get( 0 ).isPersistCalled() );
	}

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = @Setting(
					name = SessionEventSettings.INTERCEPTOR,
					value = "org.hibernate.orm.test.callback.CdiTrackingInterceptor"
			),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testGlobalInjectedDependencySharedAcrossSessions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "first" ) );
		} );
		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "second" ) );
		} );
		assertEquals( 2, InterceptorMonitor.currentPersistCount() );
		// global: only one interceptor instance created, shared across sessions
		assertEquals( 1, CdiTrackingInterceptor.INSTANCES.size() );
	}
}
