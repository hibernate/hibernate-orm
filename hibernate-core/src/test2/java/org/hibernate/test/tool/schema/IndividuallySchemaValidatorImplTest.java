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
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.IndividuallySchemaValidatorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

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

/**
 * @author Dominique Toupin
 */
@TestForIssue(jiraKey = "HHH-10332")
@RequiresDialect(H2Dialect.class)
public class IndividuallySchemaValidatorImplTest extends BaseUnitTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, IndividuallySchemaValidatorImplTest.class.getName() ) );

	private StandardServiceRegistry ssr;

	protected HibernateSchemaManagementTool tool;

	private Map configurationValues;

	protected ExecutionOptions executionOptions;

	@Before
	public void setUp() throws IOException {
		ssr = new StandardServiceRegistryBuilder().build();

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
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testMissingEntityContainsQualifiedEntityName() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( MissingEntity.class );

		final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
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
	public void testMissingEntityContainsUnqualifiedEntityName() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( UnqualifiedMissingEntity.class );

		final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
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
	public void testMissingColumn() throws Exception {
		MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( NoNameColumn.class );

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
			metadataSources.addAnnotatedClass( NameColumn.class );

			metadata = (MetadataImplementor) metadataSources.buildMetadata();
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
	public void testMismatchColumnType() throws Exception {
		MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( NameColumn.class );

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
			metadataSources.addAnnotatedClass( IntegerNameColumn.class );

			metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.validate();

			try {
				getSchemaValidator( metadata );
				Assert.fail( "SchemaManagementException expected" );
			}
			catch (SchemaManagementException e) {
				assertEquals("Schema-validation: wrong column type encountered in column [name] in table [SomeSchema.ColumnEntity]; found [varchar (Types#VARCHAR)], but expecting [integer (Types#INTEGER)]", e.getMessage());
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
				.doValidation( metadata, executionOptions );
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
	@PrimaryKeyJoinColumn
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
	@PrimaryKeyJoinColumn
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
	@PrimaryKeyJoinColumn
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
	@PrimaryKeyJoinColumn
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
	@PrimaryKeyJoinColumn
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
