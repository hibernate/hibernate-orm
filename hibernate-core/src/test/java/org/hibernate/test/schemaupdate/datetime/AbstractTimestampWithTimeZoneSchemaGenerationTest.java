/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.datetime;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.apache.log4j.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author Philippe Marschall
 */
@TestForIssue(jiraKey = "HHH-11773")
public abstract class AbstractTimestampWithTimeZoneSchemaGenerationTest {
	private static final Logger LOGGER = Logger.getLogger( AbstractTimestampWithTimeZoneSchemaGenerationTest.class );

	@Test
	public void testSchemaCreationSQLCommand() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			final org.hibernate.boot.Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( LogEntry.class )
					.buildMetadata();

			boolean logEntryTableFound = false;

			final List<String> commands = new SchemaCreatorImpl( ssr ).generateCreationCommands(
					metadata,
					false
			);

			for ( String command : commands ) {
				LOGGER.info( command );
				if ( command.equalsIgnoreCase( expectedTableDefinition() ) ) {
					logEntryTableFound = true;
				}
			}
			assertTrue(
					"Expected create table command for LogEntry entity not found",
					logEntryTableFound
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	protected abstract String expectedTableDefinition();

	@Entity
	@Table(name = "log_entry")
	public static class LogEntry {

		@Id
		private Integer id;

		@Type(type = "timestamp_with_timezone")
		@Column(name = "created_date")
		private OffsetDateTime createdDate;

		@Type(type = "time_with_timezone")
		@Column(name = "start_shift")
		private OffsetTime startShift;
	}

}
