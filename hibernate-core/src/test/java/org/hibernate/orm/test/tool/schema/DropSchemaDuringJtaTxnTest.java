/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.orm.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settingConfigurations = @SettingConfiguration( configurer = TestingJtaBootstrap.class ))
public class DropSchemaDuringJtaTxnTest {
	@Test
	public void testDrop(ServiceRegistryScope registryScope) {
		final SessionFactory sessionFactory = buildSessionFactory( registryScope );
		sessionFactory.close();
	}

	@Test
	public void testDropDuringActiveJtaTransaction(ServiceRegistryScope registryScope) throws Exception {
		final SessionFactory sessionFactory = buildSessionFactory( registryScope );

		JtaPlatformStandardTestingImpl.INSTANCE.transactionManager().begin();
		try {
			sessionFactory.close();
		}
		finally {
			JtaPlatformStandardTestingImpl.INSTANCE.transactionManager().commit();
		}
	}

	private SessionFactory buildSessionFactory(ServiceRegistryScope registryScope) {
		return org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory(
				MetadataBuildingTestHelper.buildMetadata(
						registryScope.getRegistry(),
						new MappingSources().addManagedClass( TestEntity.class )
				)
		);
	}


	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		public Integer id;
		String name;
	}
}
