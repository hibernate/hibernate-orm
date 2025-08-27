/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.IndividuallySchemaValidatorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.boot.JdbcConnectionAccessImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Dominique Toupin
 */
@JiraKey(value = "HHH-10332")
@RequiresDialect(H2Dialect.class)
public class IndividuallySchemaValidatorImplTest extends BaseUnitTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, IndividuallySchemaValidatorImplTest.class.getName() ) );

	private StandardServiceRegistry ssr;

	protected HibernateSchemaManagementTool tool;

	private Map<String,Object> configurationValues;

	protected ExecutionOptions executionOptions;

	@Before
	public void setUp() throws IOException {
		ssr = ServiceRegistryUtil.serviceRegistry();

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

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testMissingEntityContainsQualifiedEntityName() {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( MissingEntity.class );

		final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		try {
			getSchemaValidator( metadata );
			Assert.fail( "SchemaManagementException expected" );
		}
		catch (SchemaManagementException e) {
			assertEquals("Schema-validation: missing table [SomeCatalog.SomeSchema.MissingEntity]", e.getMessage());
		}
	}

	@Test
	public void testMissingEntityContainsUnqualifiedEntityName() {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( UnqualifiedMissingEntity.class );

		final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		try {
			getSchemaValidator( metadata );
			Assert.fail( "SchemaManagementException expected" );
		}
		catch (SchemaManagementException e) {
			assertEquals("Schema-validation: missing table [UnqualifiedMissingEntity]", e.getMessage());
		}
	}

	@Test
	public void testMissingColumn() {
		MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( NoNameColumn.class );

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
			metadataSources.addAnnotatedClass( NameColumn.class );

			metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			try {
				getSchemaValidator( metadata );
				Assert.fail( "SchemaManagementException expected" );
			}
			catch (SchemaManagementException e) {
				assertEquals("Schema-validation: missing column [name] in table [SomeSchema.ColumnEntity]", e.getMessage());
			}
		}
		finally {
			new SchemaDropperImpl( serviceRegistry ).doDrop( metadata, false, schemaGenerator );
			serviceRegistry.destroy();
			connectionProvider.stop();
		}
	}

	@Test
	public void testMismatchColumnType() {
		MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( NameColumn.class );

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
			metadataSources.addAnnotatedClass( IntegerNameColumn.class );

			metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			try {
				getSchemaValidator( metadata );
				Assert.fail( "SchemaManagementException expected" );
			}
			catch (SchemaManagementException e) {
				if ( metadata.getDatabase().getDialect().getVersion().isSameOrAfter( 2 ) ) {
					// Reports "character varying" since 2.0
					assertEquals(
							"Schema-validation: wrong column type encountered in column [name] in table [SomeSchema.ColumnEntity]; found [character varying (Types#VARCHAR)], but expecting [integer (Types#INTEGER)]",
							e.getMessage()
					);
				}
				else {
					assertEquals(
							"Schema-validation: wrong column type encountered in column [name] in table [SomeSchema.ColumnEntity]; found [varchar (Types#VARCHAR)], but expecting [integer (Types#INTEGER)]",
							e.getMessage()
					);
				}
			}
		}
		finally {
			new SchemaDropperImpl( serviceRegistry ).doDrop( metadata, false, schemaGenerator );
			serviceRegistry.destroy();
			connectionProvider.stop();
		}
	}

	protected void getSchemaValidator(MetadataImplementor metadata) {
		new IndividuallySchemaValidatorImpl( tool, DefaultSchemaFilter.INSTANCE )
				.doValidation( metadata, executionOptions, ContributableMatcher.ALL );
	}

	protected Properties properties() {
		Properties properties = new Properties( );
		URL propertiesURL = Thread.currentThread().getContextClassLoader().getResource( "hibernate.properties" );
		try ( FileInputStream inputStream = new FileInputStream( propertiesURL.getFile() ) ) {
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

		private String id;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "MissingEntity", catalog = "SomeCatalog", schema = "SomeSchema")
	public static class MissingEntity {

		private String id;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "ColumnEntity", schema = "SomeSchema")
	public static class NoNameColumn {

		private String id;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "ColumnEntity", schema = "SomeSchema")
	public static class NameColumn {

		private String id;

		private String name;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "ColumnEntity", schema = "SomeSchema")
	public static class IntegerNameColumn {

		private String id;

		private Integer name;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Integer getName() {
			return name;
		}

		public void setName(Integer name) {
			this.name = name;
		}
	}
}
