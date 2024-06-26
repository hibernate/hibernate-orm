/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.schemagen;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class SchemaDatabaseFileGenerationFailureTest {
	private Connection connection;
	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	@BeforeEach
	public void setUp() throws IOException, SQLException {
		connection = Mockito.mock( Connection.class );
		when ( connection.getAutoCommit() ).thenReturn( true );
		Statement statement = Mockito.mock( Statement.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.execute( anyString() ) ).thenThrow( new SQLException( "Expected" ) );

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

			assertTrue( commandAcceptanceException.getCause() instanceof SQLException );

			SQLException root = (SQLException) e.getCause().getCause().getCause();
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
		config.put( AvailableSettings.JAKARTA_HBM2DDL_CONNECTION, connection );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "drop" );
		config.put( AvailableSettings.HBM2DDL_HALT_ON_ERROR, true );
		ArrayList<Class> classes = new ArrayList<>();

		classes.addAll( Arrays.asList( new Class[] { TestEntity.class } ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}
}
