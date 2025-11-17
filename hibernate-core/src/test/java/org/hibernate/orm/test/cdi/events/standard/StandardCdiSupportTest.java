/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events.standard;

import org.hibernate.orm.test.cdi.events.Monitor;
import org.hibernate.orm.test.cdi.events.TheEntity;
import org.hibernate.orm.test.cdi.events.TheListener;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.cfg.ManagedBeanSettings.CDI_BEAN_MANAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests support for CDI as (implicitly) defined by JPA in terms of
 * immediate availability
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class StandardCdiSupportTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@ExtendWith( Monitor.Resetter.class )
	@CdiContainer(beanClasses = {Monitor.class, TheListener.class})
	@ServiceRegistry(resolvableSettings = @ServiceRegistry.ResolvableSetting(
			settingName = CDI_BEAN_MANAGER,
			resolver = CdiContainerLinker.StandardResolver.class
	))
	@DomainModel(annotatedClasses = TheEntity.class)
	@SessionFactory
	public void testIt(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();

		// The CDI bean should have been built immediately...
		assertTrue( Monitor.wasInstantiated() );
		assertEquals( 0, Monitor.currentCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1 ) );
		} );

		assertEquals( 1, Monitor.currentCount() );
	}
}
