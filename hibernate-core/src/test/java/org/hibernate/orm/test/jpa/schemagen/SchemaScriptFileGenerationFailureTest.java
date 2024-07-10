/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.schemagen;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
public class SchemaScriptFileGenerationFailureTest {
	private Writer writer;
	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;


	@BeforeEach
	public void setUp() {
		writer = new FailingWriter();

		entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				getConfig()
		);
	}

	@AfterEach
	public void destroy() {
		if ( entityManagerFactoryBuilder != null ) {
			entityManagerFactoryBuilder.cancel();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12192")
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true,
			reason = "on postgres we send 'set client_min_messages = WARNING'")
	public void testErrorMessageContainsTheFailingDDLCommand() {
		try {
			entityManagerFactoryBuilder.generateSchema();
			fail( "Should have thrown IOException" );
		}
		catch (Exception e) {
			assertTrue( e instanceof PersistenceException );
			assertTrue( e.getCause() instanceof SchemaManagementException );
			assertTrue( e.getCause().getCause() instanceof CommandAcceptanceException );

			CommandAcceptanceException commandAcceptanceException = (CommandAcceptanceException) e.getCause()
					.getCause();


			boolean ddlCommandFound = Pattern.compile( "drop table( if exists)? test_entity( if exists)?" )
					.matcher( commandAcceptanceException.getMessage().toLowerCase() ).find();
			assertTrue( ddlCommandFound, "The Exception Message does not contain the DDL command causing the failure" );

			assertTrue( commandAcceptanceException.getCause() instanceof IOException );

			IOException root = (IOException) e.getCause().getCause().getCause();
			assertEquals( "Expected", root.getMessage() );
		}
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private String field;

		private String table;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new EntityManagerFactoryBasedFunctionalTest.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	private Map getConfig() {
		final Map<Object, Object> config = Environment.getProperties();
		ServiceRegistryUtil.applySettings( config );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, writer );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		config.put( AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true" );
		ArrayList<Class> classes = new ArrayList<>();

		classes.addAll( Arrays.asList( new Class[] { TestEntity.class } ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}

	public class FailingWriter extends Writer {

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {

		}

		@Override
		public void flush() throws IOException {
			throw new IOException( "Expected" );
		}

		@Override
		public void close() throws IOException {
		}
	}
}
