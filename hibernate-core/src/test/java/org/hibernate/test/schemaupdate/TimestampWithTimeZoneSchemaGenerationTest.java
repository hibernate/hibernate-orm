/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.usertype.UserType;
import org.junit.Test;

/**
 * @author Philippe Marschall
 */
@TestForIssue(jiraKey = "HHH-11773")
public class TimestampWithTimeZoneSchemaGenerationTest {
	private static final Logger LOGGER = Logger.getLogger( TimestampWithTimeZoneSchemaGenerationTest.class );

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
				if ( command.toLowerCase().contains( "create table log_entry" ) ) {
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

	@Entity
	@Table(name = "log_entry")
	public static class LogEntry {

		@Id
		private Integer id;

		@Type(type = "org.hibernate.test.schemaupdate.TimestampWithTimeZoneSchemaGenerationTest$TimestampWithTimeZoneType")
		@Column(name = "created_date")
		private OffsetDateTime createdDate;
	}
	
	public static class TimestampWithTimeZoneType implements UserType {

		@Override
		public int[] sqlTypes() {
			return new int[] {Types.TIMESTAMP_WITH_TIMEZONE};
		}

		@Override
		public Class<?> returnedClass() {
			return OffsetDateTime.class;
		}

		@Override
		public boolean equals(Object x, Object y) {
			return Objects.equals(x, y);
		}

		@Override
		public int hashCode(Object x) {
			return Objects.hashCode(x);
		}

		@Override
		public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			return rs.getObject(names[0], OffsetDateTime.class);
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			if (value == null) {
				st.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
			} else {
				st.setObject(index, value, Types.TIMESTAMP_WITH_TIMEZONE);
			}
			
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return (Serializable) value;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return cached;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return original;
		}
	}

}
