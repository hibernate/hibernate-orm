/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events.delayed;

import org.hibernate.orm.test.cdi.events.Monitor;
import org.hibernate.orm.test.cdi.events.TheEntity;
import org.hibernate.orm.test.cdi.events.TheListener;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.cfg.ManagedBeanSettings.CDI_BEAN_MANAGER;
import static org.hibernate.cfg.ManagedBeanSettings.DELAY_CDI_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests support for CDI delaying access to the CDI container until
 * first needed
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BaseUnitTest
public class DelayedCdiSupportTest {
	@Test
	@ExtendWith( Monitor.Resetter.class )
	@CdiContainer(beanClasses = {Monitor.class, TheListener.class})
	@ServiceRegistry(
			settings = @Setting(name=DELAY_CDI_ACCESS, value = "true"),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = TheEntity.class)
	@SessionFactory
	public void testIt(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();

		// The CDI bean should not be built immediately...
		Assertions.assertFalse( Monitor.wasInstantiated() );
		assertEquals( 0, Monitor.currentCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1 ) );
		} );

		// The CDI bean should have been built on first use
		assertTrue( Monitor.wasInstantiated() );
		assertEquals( 1, Monitor.currentCount() );

		factoryScope.inTransaction( (session) -> {
			TheEntity it = session.find( TheEntity.class, 1 );
			assertNotNull( it );

			session.remove( it );
		} );
	}
}
