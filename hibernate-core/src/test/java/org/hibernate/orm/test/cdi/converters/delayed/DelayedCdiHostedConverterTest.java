/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.delayed;

import org.hibernate.orm.test.cdi.converters.ConverterBean;
import org.hibernate.orm.test.cdi.converters.MonitorBean;
import org.hibernate.orm.test.cdi.converters.TheEntity;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.cfg.ManagedBeanSettings.CDI_BEAN_MANAGER;
import static org.hibernate.cfg.ManagedBeanSettings.DELAY_CDI_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BaseUnitTest
public class DelayedCdiHostedConverterTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@ExtendWith(MonitorBean.Resetter.class )
	@CdiContainer(beanClasses = {MonitorBean.class, ConverterBean.class})
	@ServiceRegistry(
			settings = @Setting(name=DELAY_CDI_ACCESS, value = "true"),
			resolvableSettings = @ServiceRegistry.ResolvableSetting(
					settingName = CDI_BEAN_MANAGER,
					resolver = CdiContainerLinker.StandardResolver.class
			)
	)
	@DomainModel(annotatedClasses = TheEntity.class)
	@SessionFactory
	public void testIt(CdiContainerScope containerScope, SessionFactoryScope factoryScope) {
		// The CDI bean should _not_ have been built immediately...
		assertFalse( MonitorBean.wasInstantiated() );
		assertEquals( 0, MonitorBean.currentFromDbCount() );
		assertEquals( 0, MonitorBean.currentToDbCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "me", 5 ) );
		} );

		// The CDI bean should have been built on first use
		assertTrue( MonitorBean.wasInstantiated() );
		assertEquals( 0, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );

		factoryScope.inTransaction( (session) -> {
			TheEntity it = session.find( TheEntity.class, 1 );
			assertNotNull( it );
		} );

		assertEquals( 1, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );
	}
}
