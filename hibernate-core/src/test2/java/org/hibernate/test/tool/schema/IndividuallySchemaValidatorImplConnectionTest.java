/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tool.schema;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.IndividuallySchemaValidatorImpl;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaValidator;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.boot.JdbcConnectionAccessImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.test.util.DdlTransactionIsolatorTestingImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11864")
@RequiresDialect(H2Dialect.class)
public class IndividuallySchemaValidatorImplConnectionTest extends BaseUnitTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, IndividuallySchemaValidatorImplConnectionTest.class.getName() ) );

	private StandardServiceRegistry ssr;

	protected HibernateSchemaManagementTool tool;

	private Map configurationValues;

	private ExecutionOptions executionOptions;

	private DriverManagerConnectionProviderImpl connectionProvider;

	private Connection connection;

	@Before
	public void setUp() throws Exception {
		connectionProvider =
				new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( properties() );

		connection = connectionProvider.getConnection();

		ssr = new StandardServiceRegistryBuilder()
			.applySetting( AvailableSettings.HBM2DDL_CONNECTION, connection )
			.build();

		tool = (HibernateSchemaManagementTool) ssr.getService( SchemaManagementTool.class );

		configurationValues = ssr.getService( ConfigurationService.class ).getSettings();

		executionOptions = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};
	}

	@After
	public void tearsDown() {
		try {
			connection.close();
		}
		catch (SQLException e) {
			log.error( e.getMessage() );
		}
		connectionProvider.stop();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testMissingEntityContainsUnqualifiedEntityName() throws Exception {
		MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( UnqualifiedMissingEntity.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		Map<String, Object> settings = new HashMap<>(  );

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
				.applySettings( settings )
				.build();

		DriverManagerConnectionProviderImpl connectionProvider =
				new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( properties() );

		final GenerationTargetToDatabase schemaGenerator =  new GenerationTargetToDatabase(
				new DdlTransactionIsolatorTestingImpl(
						serviceRegistry,
						new JdbcConnectionAccessImpl( connectionProvider )
				)
		);

		try {
			new SchemaCreatorImpl( ssr ).doCreation(
					metadata,
					serviceRegistry,
					settings,
					true,
					schemaGenerator
			);

			metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( UnqualifiedMissingEntity.class );

			metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.validate();

			SchemaValidator schemaValidator = new IndividuallySchemaValidatorImpl( tool, DefaultSchemaFilter.INSTANCE );
			assertFalse( connection.getAutoCommit() );
			schemaValidator.doValidation( metadata, executionOptions );
			assertFalse( connection.getAutoCommit() );
		}
		finally {
			new SchemaDropperImpl( serviceRegistry ).doDrop( metadata, false, schemaGenerator );
			serviceRegistry.destroy();
			connectionProvider.stop();
		}
	}

	protected Properties properties() {
		Properties properties = new Properties( );
		URL propertiesURL = Thread.currentThread().getContextClassLoader().getResource( "hibernate.properties" );
		try(FileInputStream inputStream = new FileInputStream( propertiesURL.getFile() )) {
			properties.load( inputStream );
		}
		catch (IOException e) {
			throw new IllegalArgumentException( e );
		}
		return properties;
	}

	@Entity
	@Table(name = "UnqualifiedMissingEntity")
	public static class UnqualifiedMissingEntity {

		@Id
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
