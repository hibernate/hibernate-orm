/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import org.hibernate.cfg.ManagedBeanSettings;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link org.hibernate.callback.internal.ScopedInterceptorStrategy}
 * works correctly with all three CDI access types: immediate, delayed, and extended.
 *
 * @author Sean Okafor
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12168")
public class CdiScopedInterceptorAccessTest {

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
	public void testScopedInterceptorWithImmediateCdiAccess(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "scoped-immediate" ) );
		} );
		assertTrue( InterceptorMonitor.wasInstantiated() );
		assertEquals( 1, InterceptorMonitor.currentPersistCount() );
		assertTrue( CdiTrackingInterceptor.INSTANCES.get( 0 ).isPostConstructCalled() );

		try (var s1 = factoryScope.getSessionFactory().openSession();
			var s2 = factoryScope.getSessionFactory().openSession()) {
			assertNotSame( s1.getInterceptor(), s2.getInterceptor() );
		}
	}

	@Test
	@ExtendWith(InterceptorMonitor.Resetter.class)
	@CdiContainer(beanClasses = { InterceptorMonitor.class, CdiTrackingInterceptor.class })
	@ServiceRegistry(
			settings = {
					@Setting(name = SessionEventSettings.SESSION_SCOPED_INTERCEPTOR,
							value = "org.hibernate.orm.test.callback.CdiTrackingInterceptor"),
					@Setting(name = ManagedBeanSettings.DELAY_CDI_ACCESS, value = "true")
			},
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = ManagedBeanSettings.JAKARTA_CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testScopedInterceptorWithDelayedCdiAccess(SessionFactoryScope factoryScope) {
		// delayed: bean should NOT be created during bootstrap
		assertFalse( InterceptorMonitor.wasInstantiated() );

		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "scoped-delayed" ) );
		} );

		// bean should now be initialized on first use
		assertTrue( InterceptorMonitor.wasInstantiated() );
		assertEquals( 1, InterceptorMonitor.currentPersistCount() );
		assertTrue( CdiTrackingInterceptor.INSTANCES.get( 0 ).isPostConstructCalled() );

		try (var s1 = factoryScope.getSessionFactory().openSession();
			var s2 = factoryScope.getSessionFactory().openSession()) {
			assertNotSame( s1.getInterceptor(), s2.getInterceptor() );
		}
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
					resolver = CdiContainerLinker.ExtendedResolver.class
			)
	)
	@DomainModel(annotatedClasses = CdiSimpleEntity.class)
	@SessionFactory
	public void testScopedInterceptorWithExtendedCdiAccess(CdiContainerScope cdiContainerScope,
															SessionFactoryScope factoryScope) {
		// extended: bean should NOT be created before CDI signals readiness
		assertFalse( InterceptorMonitor.wasInstantiated() );

		cdiContainerScope.triggerReadyForUse();

		factoryScope.inTransaction( session -> {
			session.persist( new CdiSimpleEntity( "scoped-extended" ) );
		} );

		// bean should now be initialized after CDI became ready
		assertTrue( InterceptorMonitor.wasInstantiated() );
		assertEquals( 1, InterceptorMonitor.currentPersistCount() );
		assertTrue( CdiTrackingInterceptor.INSTANCES.get( 0 ).isPostConstructCalled() );

		try (var s1 = factoryScope.getSessionFactory().openSession();
			var s2 = factoryScope.getSessionFactory().openSession()) {
			assertNotSame( s1.getInterceptor(), s2.getInterceptor() );
		}
	}
}
