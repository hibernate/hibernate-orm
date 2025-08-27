/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TestKeywordRecognition extends BaseUnitTestCase {
	private StandardServiceRegistry serviceRegistry;

	@Before
	public void prepareServiveRegistry() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.build();
	}

	@After
	public void releaseServiveRegistry() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey( value = "HHH-9768" )
	public void testAnsiSqlKeyword() {
		// END is ANSI SQL keyword

		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		assertTrue( jdbcEnvironment.getIdentifierHelper().isReservedWord( "end" ) );
		assertTrue( jdbcEnvironment.getIdentifierHelper().isReservedWord( "END" ) );

		Identifier identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier( "end" );
		assertTrue( identifier.isQuoted() );
	}
}
