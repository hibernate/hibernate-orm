/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.jdbc.env;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
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
		serviceRegistry = new StandardServiceRegistryBuilder().build();
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

		// keywords are kept defined in upper case in here...
		assertTrue( AnsiSqlKeywords.INSTANCE.sql2003().contains( "END" ) );

		// But JdbcEnvironment uses a case-insensitive Set to store them...
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		assertTrue( jdbcEnvironment.isReservedWord( "end" ) );
		assertTrue( jdbcEnvironment.isReservedWord( "END" ) );

		Identifier identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier( "end" );
		assertTrue( identifier.isQuoted() );
	}
}
