/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.boot.JdbcConnectionAccessImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.IndividuallySchemaValidatorImpl;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11864")
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class IndividuallySchemaValidatorImplConnectionTest {
	private StandardServiceRegistry ssr;
	protected HibernateSchemaManagementTool tool;
	private Map<String,Object> configurationValues;
	private ExecutionOptions executionOptions;
	private DriverManagerConnectionProvider connectionProvider;
	private Connection connection;

	@BeforeEach
	public void setUp() throws Exception {
		connectionProvider =
				new DriverManagerConnectionProvider();
		connectionProvider.configure( PropertiesHelper.map( properties() ) );

		connection = connectionProvider.getConnection();

		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
			.applySetting( AvailableSettings.JAKARTA_HBM2DDL_CONNECTION, connection )
			.build();

		tool = (HibernateSchemaManagementTool) ssr.getService( SchemaManagementTool.class );

		configurationValues = ssr.requireService( ConfigurationService.class ).getSettings();

		executionOptions = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map<String,Object> getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};
	}

	@AfterEach
	public void tearsDown() {
		try {
			connectionProvider.closeConnection( connection );
		}
		catch (SQLException e) {
		}
		connectionProvider.stop();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testMissingEntityContainsUnqualifiedEntityName() throws Exception {
		MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( UnqualifiedMissingEntity.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		Map<String, Object> settings = new HashMap<>(  );

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings )
				.build();

		DriverManagerConnectionProvider connectionProvider =
				new DriverManagerConnectionProvider();
		connectionProvider.configure( PropertiesHelper.map( properties() ) );

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
			metadata.orderColumns( false );
			metadata.validate();

			SchemaValidator schemaValidator = new IndividuallySchemaValidatorImpl( tool, DefaultSchemaFilter.INSTANCE );
			assertFalse( connection.getAutoCommit() );
			schemaValidator.doValidation( metadata, executionOptions, ContributableMatcher.ALL );
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
		try(FileInputStream inputStream = new FileInputStream( propertiesURL.getFile() ) ) {
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
