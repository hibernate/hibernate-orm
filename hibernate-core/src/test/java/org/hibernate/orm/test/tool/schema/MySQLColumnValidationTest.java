/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.tool.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.IndividuallySchemaValidatorImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaValidator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Jan Schatteman
 */
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "none" )
		}
)
@DomainModel(
		annotatedClasses = {
				MySQLColumnValidationTest.TestEntity1.class,
				MySQLColumnValidationTest.TestEntity2.class,
				MySQLColumnValidationTest.TestEntity3.class
		}
)
@SessionFactory( exportSchema = false )
@JiraKey( value = "HHH-16578" )
@RequiresDialect( value = MySQLDialect.class, matchSubTypes = false )
public class MySQLColumnValidationTest {

	private DriverManagerConnectionProviderImpl connectionProvider;

	@BeforeAll
	public void init() {
		connectionProvider = new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( PropertiesHelper.map( Environment.getProperties() ) );

		try( Connection connection = connectionProvider.getConnection();
			Statement statement = connection.createStatement() ) {

			try {
				statement.execute( "DROP TABLE TEST_DATA1" );
				statement.execute( "DROP TABLE TEST_DATA2" );
				statement.execute( "DROP TABLE TEST_DATA3" );
			}
			catch (SQLException e) {
			}

			statement.execute( "CREATE TABLE `TEST_DATA1` ( " +
									   "  `ID` int unsigned NOT NULL, " +
									   "  `INTEGRAL1` tinyint unsigned DEFAULT '0', " +
									   "  `INTEGRAL2` tinyint unsigned DEFAULT '0', " +
									   "   PRIMARY KEY (`ID`)" +
									   ") ENGINE=InnoDB" );

			statement.execute( "CREATE TABLE `TEST_DATA2` ( " +
									   "  `ID` int unsigned NOT NULL, " +
									   "  `INTEGRAL1` tinyint unsigned DEFAULT '0', " +
									   "   PRIMARY KEY (`ID`)" +
									   ") ENGINE=InnoDB" );

			statement.execute( "CREATE TABLE `TEST_DATA3` ( " +
									   "  `ID` int unsigned NOT NULL, " +
									   "  `INTEGRAL1` tinyint unsigned DEFAULT '0', " +
									   "   PRIMARY KEY (`ID`)" +
									   ") ENGINE=InnoDB" );

		}
		catch (SQLException e) {
			fail(e.getMessage());
		}
	}

	@AfterAll
	public void releaseResources() {
		try( Connection connection = connectionProvider.getConnection();
			Statement statement = connection.createStatement() ) {
			try {
				statement.execute( "DROP TABLE TEST_DATA1" );
				statement.execute( "DROP TABLE TEST_DATA2" );
				statement.execute( "DROP TABLE TEST_DATA3" );
			}
			catch (SQLException e) {
			}
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}

		connectionProvider.stop();
	}

	@Test
	public void testValidateColumn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity1 te1 = new TestEntity1( 1, 1, 2 );
					session.persist( te1 );
				}
		);

		ConfigurationService configurationService = scope.getSessionFactory().getServiceRegistry().getService(
				ConfigurationService.class );
		ExecutionOptions executionOptions = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map getConfigurationValues() {
				return configurationService.getSettings();
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}

			@Override
			public SchemaFilter getSchemaFilter() {
				return SchemaFilter.ALL;
			}
		};

		HibernateSchemaManagementTool hsmt = (HibernateSchemaManagementTool) scope.getSessionFactory()
				.getServiceRegistry()
				.getService( SchemaManagementTool.class );
		SchemaValidator schemaValidator = new IndividuallySchemaValidatorImpl( hsmt, DefaultSchemaFilter.INSTANCE );

		try {
			schemaValidator.doValidation( scope.getMetadataImplementor(), executionOptions,
										  contributed -> {
											return "test_data1".equalsIgnoreCase( contributed.getExportIdentifier() );
										  } );
		}
		catch (SchemaManagementException e) {
			fail( e.getMessage() );
		}

		try {
			schemaValidator.doValidation( scope.getMetadataImplementor(), executionOptions,
										  contributed -> {
											  return "test_data2".equalsIgnoreCase( contributed.getExportIdentifier() );
										  } );
			fail( "SchemaManagementException expected" );
		}
		catch (SchemaManagementException e) {
			assertEquals(
					"Schema-validation: wrong column type encountered in column [integral1] in table [TEST_DATA2]; found [tinyint unsigned (Types#TINYINT)], but expecting [tinyint (Types#INTEGER)]",
					e.getMessage()
			);
		}

		schemaValidator.doValidation( scope.getMetadataImplementor(), executionOptions,
									  contributed -> {
											return "test_data3".equalsIgnoreCase( contributed.getExportIdentifier() );
										} );
	}

	@Entity(name = "test_entity1")
	@Table(name = "TEST_DATA1")
	public static class TestEntity1 {
		@Id
		@Column(name = "id", columnDefinition = "INT(10) UNSIGNED NOT NULL")
		private Integer id;

		@JdbcTypeCode( Types.TINYINT )
		@Column(name = "integral1", columnDefinition = "tinyint UNSIGNED")
		private int integral1;

		@JdbcTypeCode( Types.TINYINT )
		@Column(name = "integral2", columnDefinition = "tinyint UNSIGNED DEFAULT '0'")
		private int integral2;

		public TestEntity1( Integer id, int integral1, int integral2 ) {
			this.id = id;
			this.integral1 = integral1;
			this.integral2 = integral2;
		}
	}

	@Entity(name = "test_entity2")
	@Table(name = "TEST_DATA2")
	public static class TestEntity2 {
		@Id
		@Column(name = "id", columnDefinition = "INT(10) UNSIGNED NOT NULL")
		private Integer id;

		@Column(name = "integral1", columnDefinition = "tinyint")
		private int integral1;

		public TestEntity2( Integer id, int integral1 ) {
			this.id = id;
			this.integral1 = integral1;
		}
	}

	@Entity(name = "test_entity3")
	@Table(name = "TEST_DATA3")
	public static class TestEntity3 {
		@Id
		@Column(name = "id", columnDefinition = "INT(10) UNSIGNED NOT NULL")
		private Integer id;

		@Column(name = "integral1", columnDefinition = "tinyint UNSIGNED DEFAULT '0'")
		private int integral1;

		public TestEntity3( Integer id, int integral1 ) {
			this.id = id;
			this.integral1 = integral1;
		}
	}

}
