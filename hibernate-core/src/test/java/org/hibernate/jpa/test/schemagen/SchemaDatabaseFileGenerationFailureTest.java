/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class SchemaDatabaseFileGenerationFailureTest {
	private Connection connection;
	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	@Before
	public void setUp() throws IOException, SQLException {
		connection = Mockito.mock( Connection.class );
		Statement statement = Mockito.mock( Statement.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.execute( anyString() ) ).thenThrow( new SQLException( "Expected" ) );

		entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				getConfig()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12192")
	public void testErrorMessageContainsTheFailingDDLCommand() {
		try {
			entityManagerFactoryBuilder.generateSchema();
			fail( "Should haave thrown IOException" );
		}
		catch (Exception e) {
			assertTrue( e instanceof PersistenceException );
			assertTrue( e.getCause() instanceof SchemaManagementException );
			assertTrue( e.getCause().getCause() instanceof CommandAcceptanceException );

			CommandAcceptanceException commandAcceptanceException = (CommandAcceptanceException) e.getCause()
					.getCause();

			boolean ddlCommandFound = Pattern.compile( "drop table( if exists)? test_entity( if exists)?" )
					.matcher( commandAcceptanceException.getMessage().toLowerCase() ).find();
			assertTrue( "The Exception Message does not contain the DDL command causing the failure", ddlCommandFound );

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
		return new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	private Map getConfig() {
		final Map<Object, Object> config = Environment.getProperties();
		config.put( AvailableSettings.HBM2DDL_CONNECTION, connection );
		config.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "drop" );
		config.put( AvailableSettings.HBM2DDL_HALT_ON_ERROR, true );
		ArrayList<Class> classes = new ArrayList<>();

		classes.addAll( Arrays.asList( new Class[] { TestEntity.class } ) );
		config.put( org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}

}
