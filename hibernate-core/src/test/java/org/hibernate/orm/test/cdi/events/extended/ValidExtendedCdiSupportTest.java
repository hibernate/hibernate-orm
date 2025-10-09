/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events.extended;

import org.hibernate.orm.test.cdi.events.Monitor;
import org.hibernate.orm.test.cdi.events.TheEntity;
import org.hibernate.orm.test.cdi.events.TheListener;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.cfg.ManagedBeanSettings.CDI_BEAN_MANAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests support for CDI delaying access to the CDI container until
 * first needed
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ValidExtendedCdiSupportTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@ExtendWith( Monitor.Resetter.class )
	@CdiContainer(beanClasses = {Monitor.class, TheListener.class})
	@ServiceRegistry(resolvableSettings = @ServiceRegistry.ResolvableSetting(
			settingName = CDI_BEAN_MANAGER,
			resolver = CdiContainerLinker.ExtendedResolver.class
	))
	@DomainModel(annotatedClasses = TheEntity.class)
	@SessionFactory
	public void test(CdiContainerScope cdiContainerScope, SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();

		// The CDI bean should not be built immediately...
		assertFalse( Monitor.wasInstantiated() );
		assertEquals( 0, Monitor.currentCount() );

		// But now lets initialize CDI and do the callback
		cdiContainerScope.triggerReadyForUse();
		// at this point the bean should have been accessed
		assertTrue( Monitor.wasInstantiated() );
		assertEquals( 0, Monitor.currentCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1 ) );
		} );

		assertEquals( 1, Monitor.currentCount() );
	}

}
