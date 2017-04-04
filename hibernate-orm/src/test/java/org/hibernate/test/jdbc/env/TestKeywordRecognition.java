/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.env;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
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
		serviceRegistry = new StandardServiceRegistryBuilder()
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
	@TestForIssue( jiraKey = "HHH_9768" )
	public void testAnsiSqlKeyword() {
		// END is ANSI SQL keyword

		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		assertTrue( jdbcEnvironment.getIdentifierHelper().isReservedWord( "end" ) );
		assertTrue( jdbcEnvironment.getIdentifierHelper().isReservedWord( "END" ) );

		Identifier identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier( "end" );
		assertTrue( identifier.isQuoted() );
	}
}
