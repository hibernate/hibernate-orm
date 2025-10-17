/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.standard;

import org.hibernate.orm.test.cdi.converters.ConverterBean;
import org.hibernate.orm.test.cdi.converters.MonitorBean;
import org.hibernate.orm.test.cdi.converters.MyData;
import org.hibernate.orm.test.cdi.converters.OrmXmlConverterBean;
import org.hibernate.orm.test.cdi.converters.TheEntity;
import org.hibernate.orm.test.cdi.converters.TheOrmXmlEntity;
import org.hibernate.orm.test.cdi.testsupport.CdiContainer;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerLinker;
import org.hibernate.orm.test.cdi.testsupport.CdiContainerScope;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.cfg.ManagedBeanSettings.CDI_BEAN_MANAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BaseUnitTest
public class CdiHostedConverterTest {
	@Test
	@ExtendWith(MonitorBean.Resetter.class )
	@CdiContainer(beanClasses = {MonitorBean.class, ConverterBean.class})
	@ServiceRegistry(resolvableSettings = @ServiceRegistry.ResolvableSetting(
			settingName = CDI_BEAN_MANAGER,
			resolver = CdiContainerLinker.StandardResolver.class
	))
	@DomainModel(annotatedClasses = TheEntity.class)
	@SessionFactory
	public void testAnnotations(CdiContainerScope containerScope, SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();

		// The CDI bean should have been built immediately...
		assertTrue( MonitorBean.wasInstantiated() );
		assertEquals( 0, MonitorBean.currentFromDbCount() );
		assertEquals( 0, MonitorBean.currentToDbCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "me", 5 ) );
		} );

		assertEquals( 0, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );

		factoryScope.inTransaction( (session) -> {
			TheEntity it = session.find( TheEntity.class, 1 );
			assertNotNull( it );
		} );

		assertEquals( 1, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );
	}

	@Test
	@JiraKey("HHH-14881")
	@ExtendWith(MonitorBean.Resetter.class )
	@CdiContainer(beanClasses = {MonitorBean.class, OrmXmlConverterBean.class})
	@ServiceRegistry(resolvableSettings = @ServiceRegistry.ResolvableSetting(
			settingName = CDI_BEAN_MANAGER,
			resolver = CdiContainerLinker.StandardResolver.class
	))
	@DomainModel(annotatedClasses = TheEntity.class, xmlMappings = "org/hibernate/test/cdi/converters/orm.xml")
	@SessionFactory
	public void testOrmXml(CdiContainerScope cdiScope, SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();

		// The CDI bean should have been built immediately...
		assertTrue( MonitorBean.wasInstantiated() );
		assertEquals( 0, MonitorBean.currentFromDbCount() );
		assertEquals( 0, MonitorBean.currentToDbCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new TheOrmXmlEntity( 1, "me", new MyData( "foo" ) ) );
		} );

		assertEquals( 0, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );

		factoryScope.inTransaction( (session) -> {
			TheOrmXmlEntity it = session.find( TheOrmXmlEntity.class, 1 );
			assertNotNull( it );
		} );

		assertEquals( 1, MonitorBean.currentFromDbCount() );
		assertEquals( 1, MonitorBean.currentToDbCount() );
	}
}
