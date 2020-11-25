/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.annotations.id.sequences;

import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.BaseUnitTest;
import org.hibernate.orm.test.annotations.id.sequences.entities.Bunny;
import org.hibernate.orm.test.annotations.id.sequences.entities.PointyTooth;
import org.hibernate.orm.test.annotations.id.sequences.entities.TwinkleToes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
public class JoinColumnOverrideTest extends BaseUnitTest {

	private static final String expectedSqlPointyTooth = "create table PointyTooth (id numeric(128,0) not null, " +
			"bunny_id numeric(128,0), primary key (id))";
	private static final String expectedSqlTwinkleToes = "create table TwinkleToes (id numeric(128,0) not null, " +
			"bunny_id numeric(128,0), primary key (id))";

	@Test
	@TestForIssue(jiraKey = "ANN-748")
	public void testBlownPrecision() {
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

			List<String> commands = new SchemaCreatorImpl( ssr ).generateCreationCommands( metadata, false );
			for ( String command : commands ) {

				if ( expectedSqlPointyTooth.equals( command ) ) {
					foundPointyToothCreate = true;
				}
				else if ( expectedSqlTwinkleToes.equals( command ) ) {
					foundTwinkleToesCreate = true;
				}
			}

			assertTrue( foundPointyToothCreate, "Expected create table command for PointyTooth entity not found" );
			assertTrue( foundTwinkleToesCreate, "Expected create table command for TwinkleToes entity not found" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
