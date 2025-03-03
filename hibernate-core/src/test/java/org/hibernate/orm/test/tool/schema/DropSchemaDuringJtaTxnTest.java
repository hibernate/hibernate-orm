/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class DropSchemaDuringJtaTxnTest extends BaseUnitTestCase {
	@Test
	public void testDrop() {
		final SessionFactory sessionFactory = buildSessionFactory();
		sessionFactory.close();
	}
	@Test
	public void testDropDuringActiveJtaTransaction() throws Exception {
		final SessionFactory sessionFactory = buildSessionFactory();

		JtaPlatformStandardTestingImpl.INSTANCE.transactionManager().begin();
		try {
			sessionFactory.close();
		}
		finally {
			JtaPlatformStandardTestingImpl.INSTANCE.transactionManager().commit();
		}
	}

	private SessionFactory buildSessionFactory() {
		Map<String, Object> settings = new HashMap<>( PropertiesHelper.map( Environment.getProperties() ) );
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );

		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder().applySettings( settings ).build();
		try {
			return new MetadataSources( ssr )
					.addAnnotatedClass( TestEntity.class )
					.buildMetadata()
					.buildSessionFactory();
		}
		catch (Throwable t) {
			ssr.close();
			throw t;
		}
	}


	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		public Integer id;
		String name;
	}
}
