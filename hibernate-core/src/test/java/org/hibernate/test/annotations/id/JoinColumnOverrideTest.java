/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id;

import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.annotations.id.entities.Bunny;
import org.hibernate.test.annotations.id.entities.PointyTooth;
import org.hibernate.test.annotations.id.entities.TwinkleToes;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class JoinColumnOverrideTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( JoinColumnOverrideTest.class );

	private static final String expectedSqlPointyTooth = "create table PointyTooth (id numeric(128,0) not null, " +
			"bunny_id numeric(128,0), primary key (id))";
	private static final String expectedSqlTwinkleToes = "create table TwinkleToes (id numeric(128,0) not null, " +
			"bunny_id numeric(128,0), primary key (id))";

	@Test
	@TestForIssue( jiraKey = "ANN-748" )
	public void testBlownPrecision() throws Exception {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, "SQLServer" )
				.build();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Bunny.class )
					.addAnnotatedClass( PointyTooth.class )
					.addAnnotatedClass( TwinkleToes.class )
					.buildMetadata();

			boolean foundPointyToothCreate = false;
			boolean foundTwinkleToesCreate = false;

			List<String> commands = new SchemaCreatorImpl().generateCreationCommands( metadata, false );
			for ( String command : commands ) {
				log.debug( command );

				if ( expectedSqlPointyTooth.equals( command ) ) {
					foundPointyToothCreate = true;
				}
				else if ( expectedSqlTwinkleToes.equals( command ) ) {
					foundTwinkleToesCreate = true;
				}
			}

			assertTrue( "Expected create table command for PointyTooth entity not found", foundPointyToothCreate );
			assertTrue( "Expected create table command for TwinkleToes entity not found", foundTwinkleToesCreate );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
