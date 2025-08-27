/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.standard;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.cdi.converters.MyData;
import org.hibernate.orm.test.cdi.converters.OrmXmlConverterBean;
import org.hibernate.orm.test.cdi.converters.TheOrmXmlEntity;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.cdi.converters.ConverterBean;
import org.hibernate.orm.test.cdi.converters.MonitorBean;
import org.hibernate.orm.test.cdi.converters.TheEntity;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class CdiHostedConverterTest extends BaseUnitTestCase {
	@Test
	public void testAnnotations() {
		MonitorBean.reset();

		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( MonitorBean.class, ConverterBean.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.build();

			final SessionFactoryImplementor sessionFactory;

			try {
				sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
						.addAnnotatedClass( TheEntity.class )
						.buildMetadata()
						.getSessionFactoryBuilder()
						.build();
			}
			catch ( Exception e ) {
				StandardServiceRegistryBuilder.destroy( ssr );
				throw e;
			}

			// The CDI bean should have been built immediately...
			assertTrue( MonitorBean.wasInstantiated() );
			assertEquals( 0, MonitorBean.currentFromDbCount() );
			assertEquals( 0, MonitorBean.currentToDbCount() );

			try {
				inTransaction(
						sessionFactory,
						session -> session.persist( new TheEntity( 1, "me", 5 ) )
				);

				assertEquals( 0, MonitorBean.currentFromDbCount() );
				assertEquals( 1, MonitorBean.currentToDbCount() );

				inTransaction(
						sessionFactory,
						session -> {
							TheEntity it = session.find( TheEntity.class, 1 );
							assertNotNull( it );
						}
				);

				assertEquals( 1, MonitorBean.currentFromDbCount() );
				assertEquals( 1, MonitorBean.currentToDbCount() );
			}
			finally {
				inTransaction(
						sessionFactory,
						session -> {
							session.createQuery( "delete TheEntity" ).executeUpdate();
						}
				);

				sessionFactory.close();
			}
		}
	}

	@Test
	@JiraKey(value = "HHH-14881\n")
	public void testOrmXml() {
		MonitorBean.reset();

		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( MonitorBean.class, OrmXmlConverterBean.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.build();

			final SessionFactoryImplementor sessionFactory;

			try {
				sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
						.addResource( "org/hibernate/test/cdi/converters/orm.xml" )
						.buildMetadata()
						.getSessionFactoryBuilder()
						.build();
			}
			catch ( Exception e ) {
				StandardServiceRegistryBuilder.destroy( ssr );
				throw e;
			}

			// The CDI bean should have been built immediately...
			assertTrue( MonitorBean.wasInstantiated() );
			assertEquals( 0, MonitorBean.currentFromDbCount() );
			assertEquals( 0, MonitorBean.currentToDbCount() );

			try {
				inTransaction(
						sessionFactory,
						session -> session.persist( new TheOrmXmlEntity( 1, "me", new MyData( "foo" ) ) )
				);

				assertEquals( 0, MonitorBean.currentFromDbCount() );
				assertEquals( 1, MonitorBean.currentToDbCount() );

				inTransaction(
						sessionFactory,
						session -> {
							TheOrmXmlEntity it = session.find( TheOrmXmlEntity.class, 1 );
							assertNotNull( it );
						}
				);

				assertEquals( 1, MonitorBean.currentFromDbCount() );
				assertEquals( 1, MonitorBean.currentToDbCount() );
			}
			finally {
				inTransaction(
						sessionFactory,
						session -> {
							session.createQuery( "delete TheOrmXmlEntity" ).executeUpdate();
						}
				);

				sessionFactory.close();
			}
		}
	}
}
