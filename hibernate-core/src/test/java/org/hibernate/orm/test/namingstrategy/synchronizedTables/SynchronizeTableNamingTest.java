/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.synchronizedTables;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class SynchronizeTableNamingTest {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void before() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testAnnotationHandling() {
		final Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( DynamicEntity.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( TestingPhysicalNamingStrategy.INSTANCE )
				.build();
		verify( metadata.getEntityBinding( DynamicEntity.class.getName() ) );
	}

	@Test
	public void testHbmXmlHandling() {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( "org/hibernate/orm/test/namingstrategy/synchronizedTables/mapping.hbm.xml" )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( TestingPhysicalNamingStrategy.INSTANCE )
				.build();
		verify( metadata.getEntityBinding( DynamicEntity.class.getName() ) );
	}

	private void verify(PersistentClass entityBinding) {
		assertTrue( entityBinding.getSynchronizedTables().contains( "tbl_a" ) );
	}

	public static class TestingPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {
		/**
		 * Singleton access
		 */
		public static final TestingPhysicalNamingStrategy INSTANCE = new TestingPhysicalNamingStrategy();

		@Override
		public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context) {
			String baseName = logicalName.render( context.getDialect() );
			if ( baseName.equals( "table_a" ) ) {
				baseName = "tbl_a";
			}
			return context.getIdentifierHelper().toIdentifier( baseName );
		}
	}
}
