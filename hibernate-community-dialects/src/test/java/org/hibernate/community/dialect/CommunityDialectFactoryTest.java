/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.HashMap;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CommunityDialectFactoryTest extends BaseUnitTestCase {
	private StandardServiceRegistry registry;
	private DialectFactoryImpl dialectFactory;

	@Before
	public void setUp() {
		final BootstrapServiceRegistry bootReg = new BootstrapServiceRegistryBuilder().applyClassLoader(
				CommunityDialectFactoryTest.class.getClassLoader()
		).build();
		registry = ServiceRegistryUtil.serviceRegistryBuilder( bootReg ).build();

		dialectFactory = new DialectFactoryImpl();
		dialectFactory.injectServices( (ServiceRegistryImplementor) registry );
	}

	@Test
	public void testPreregisteredDialects() {
		DialectResolver resolver = new CommunityDialectResolver();
		testDetermination( "Ingres", IngresDialect.class, resolver );
		testDetermination( "ingres", IngresDialect.class, resolver );
		testDetermination( "INGRES", IngresDialect.class, resolver );
		testDetermination( "Adaptive Server Anywhere", SybaseAnywhereDialect.class, resolver );
		testDetermination( "Informix Dynamic Server", InformixDialect.class, resolver );
	}

	private void testDetermination(String databaseName, Class expected, DialectResolver resolver) {
		testDetermination( databaseName, -9999, expected, resolver );
	}

	private void testDetermination(String databaseName, int databaseMajorVersion, Class expected, DialectResolver resolver) {
		testDetermination( databaseName, databaseMajorVersion, -9999, expected, resolver );
	}

	private void testDetermination(
			final String databaseName,
			final int majorVersion,
			final int minorVersion,
			Class expected,
			DialectResolver resolver) {
		testDetermination( databaseName, null, majorVersion, minorVersion, expected, resolver );
	}

	private void testDetermination(
			final String databaseName,
			final String driverName,
			final int majorVersion,
			final int minorVersion,
			Class expected,
			DialectResolver resolver) {
		dialectFactory.setDialectResolver( resolver );
		Dialect resolved = dialectFactory.buildDialect(
				new HashMap<>(),
				() -> TestingDialectResolutionInfo.forDatabaseInfo( databaseName, driverName, majorVersion, minorVersion )
		);
		assertEquals( expected, resolved.getClass() );
	}
}
