/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;

import org.hibernate.testing.junit5.schema.FunctionalMetaModelTesting;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaScopeProducer;
import org.hibernate.testing.junit5.schema.SchemaTestExtension;
import org.hibernate.testing.junit5.serviceregistry.ServiceRegistryAccess;
import org.hibernate.testing.junit5.serviceregistry.ServiceRegistryContainer;
import org.hibernate.testing.junit5.template.TestParameter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Andrea Boriero
 */
@FunctionalMetaModelTesting
public abstract class BaseSchemaUnitTestCase
		implements ServiceRegistryAccess, ServiceRegistryContainer, SchemaScopeProducer {
	protected static final Class<?>[] NO_CLASSES = new Class[0];
	protected static final String[] NO_MAPPINGS = new String[0];

	private StandardServiceRegistry standardServiceRegistry;
	private DatabaseModel databaseModel;
	private MetadataImplementor metadata;

	private File output;
	private SchemaScope schemaScope;
	private Dialect dialect;

	@BeforeEach
	public void setUp(SchemaScope scope) {
		beforeEach( scope );
	}

	@AfterEach
	public void tearDown(SchemaScope scope) {
		try {
			try {
				scope.clearScope();
				afterEach( scope );
			}
			finally {
				if ( createSqlScriptTempOutputFile() ) {
					output.delete();
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
			schemaScope = null;
			metadata = null;
			databaseModel = null;
		}
	}

	@Override
	public void setStandardServiceRegistry(StandardServiceRegistry standardServiceRegistry) {
		this.standardServiceRegistry = standardServiceRegistry;
	}

	@Override
	public StandardServiceRegistry getStandardServiceRegistry() {
		return standardServiceRegistry;
	}

	@Override
	public SchemaScope produceTestScope(TestParameter<String> metadataExtractionStrategy) {
		final String metadataExtractionStrategyValue = metadataExtractionStrategy.getValue();
		if ( schemaScope == null ) {
			schemaScope = produceSchemaScope( metadataExtractionStrategyValue );
		}
		return schemaScope;
	}

	private SchemaScope produceSchemaScope(String metadataExtractionStrategyValue) {
		try {
			createTempOutputFile();
		}
		catch (IOException e) {
			fail( "Fail creating temporary file" + e.getMessage() );
		}

		setStandardServiceRegistry( buildServiceRegistry( metadataExtractionStrategyValue ) );
		afterServiceRegistryCreation( standardServiceRegistry );
		metadata = buildMetadata( standardServiceRegistry );
		metadata.validate();

		afterMetadataCreation( metadata );

		databaseModel = Helper.buildDatabaseModel( metadata );
		schemaScope = new SchemaScopeImpl( databaseModel, standardServiceRegistry, dropSchemaAfterTest() );
		return schemaScope;
	}

	public DatabaseModel getDatabaseModel() {
		return databaseModel;
	}

	public MetadataImplementor getMetadata() {
		return metadata;
	}

	public String getSqlScriptOutputFileContent() throws IOException {
		if ( createSqlScriptTempOutputFile() ) {
			return new String( Files.readAllBytes( output.toPath() ) );
		}
		else {
			throw new RuntimeException(
					"Temporary Output file was not created, the BaseSchemaTest createSqlScriptTempOutputFile() method must be overridden to return true" );
		}
	}

	protected void afterServiceRegistryCreation(StandardServiceRegistry standardServiceRegistry) {
	}

	public List<String> getSqlScriptOutputFileLines() throws IOException {
		if ( createSqlScriptTempOutputFile() ) {
			return Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		}
		else {
			throw new RuntimeException(
					"Temporary Output file was not created, the BaseSchemaTest createSqlScriptTempOutputFile() method must be overridden to return true" );
		}
	}

	public Dialect getDialect() {
		/**
		 * This method is called before {@link #produceTestScope(TestParameter)}, but to have access to the Dialect value we need
		 * to create an instance of {@link MetadataImplementor}, creating here an instance {@link SchemaScope}
		 * with the value of the {@link TestParameter} passed to the {@link #produceTestScope(TestParameter)} when it
		 * will be later called by {@link SchemaTestExtension} avoids the recreation of {@link MetadataImplementor}
		 */
		if ( dialect == null ) {
			if ( schemaScope == null ) {
				schemaScope = produceSchemaScope( SchemaTestExtension.METADATA_ACCESS_STRATEGIES.get( 0 ) );
			}
			dialect = metadata.getDatabase().getDialect();
		}
		return dialect;
	}

	public void executeSqlStatement(String statement) {
		final ConnectionProvider connectionProvider = standardServiceRegistry.getService( ConnectionProvider.class );

		DdlTransactionIsolator transactionIsolator = new DdlTransactionIsolatorTestingImpl(
				standardServiceRegistry,
				connectionProvider
		);
		GenerationTargetToDatabase targetToDatabase = new GenerationTargetToDatabase( transactionIsolator, true );
		try {
			targetToDatabase.accept( statement );
		}
		finally {
			targetToDatabase.release();
		}
	}

	protected void beforeEach(SchemaScope scope) {
	}

	protected void afterEach(SchemaScope scope) {
	}

	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	protected String getOutputTempScriptFileName() {
		return "update_script";
	}

	protected String getOutputTempScriptFileAbsolutePath() {
		return output.getAbsolutePath();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getHmbMappingFiles() {
		return NO_MAPPINGS;
	}

	protected boolean createSqlScriptTempOutputFile() {
		return false;
	}

	protected boolean dropSchemaAfterTest() {
		return true;
	}

	protected void afterMetadataCreation(MetadataImplementor metadata) {
	}

	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
	}

	private void createTempOutputFile() throws IOException {
		if ( createSqlScriptTempOutputFile() ) {
			output = File.createTempFile( getOutputTempScriptFileName(), ".sql" );
		}
	}

	private StandardServiceRegistry buildServiceRegistry(String metadataExtractionStrategy) {
		StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
		applySettings( standardServiceRegistryBuilder );
		return standardServiceRegistryBuilder
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						metadataExtractionStrategy
				)
				.build();
	}

	private MetadataImplementor buildMetadata(StandardServiceRegistry standardServiceRegistry) {
		final MetadataSources metadataSources = new MetadataSources( standardServiceRegistry );
		addAnnotatedClass( metadataSources );
		addResources( metadataSources );

		return (MetadataImplementor) metadataSources.buildMetadata();
	}

	private void addResources(MetadataSources metadataSources) {
		String[] mappings = getHmbMappingFiles();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				metadataSources.addResource(
						getBaseForMappings() + mapping
				);
			}
		}
	}

	private void addAnnotatedClass(MetadataSources metadataSources) {
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		for ( int i = 0; i < annotatedClasses.length; i++ ) {
			metadataSources.addAnnotatedClass( annotatedClasses[i] );
		}
	}

	public class SchemaScopeImpl implements SchemaScope {
		private final DatabaseModel databaseModel;
		private final StandardServiceRegistry standardServiceRegistry;
		private final boolean dropSchemaAfterTest;

		public SchemaScopeImpl(
				DatabaseModel databaseModel,
				StandardServiceRegistry standardServiceRegistry, boolean dropSchemaAfterTest) {
			this.databaseModel = databaseModel;
			this.standardServiceRegistry = standardServiceRegistry;
			this.dropSchemaAfterTest = dropSchemaAfterTest;
		}

		@Override
		public void withSchemaUpdate(Consumer<SchemaUpdate> counsumer) {
			counsumer.accept( createSchemaUpdate() );
		}

		@Override
		public void withSchemaValidator(Consumer<SchemaValidator> counsumer) {
			counsumer.accept( createSchemaValidator() );
		}

		@Override
		public void withSchemaMigrator(Consumer<SchemaMigrator> counsumer) {
			counsumer.accept( createSchemaMigrator() );
		}

		@Override
		public void withSchemaExport(Consumer<SchemaExport> counsumer) {
			counsumer.accept( createSchemaExport() );
		}

		@Override
		public void withSchemaCreator(SchemaFilter filter, Consumer<SchemaCreatorImpl> consumer) {
			consumer.accept( createSchemaCreator( filter ) );
		}

		@Override
		public void withSchemaDropper(SchemaFilter filter, Consumer<SchemaDropperImpl> consumer) {
			consumer.accept( createSchemaDropper( filter ) );
		}

		@Override
		public void clearScope() {
			if ( dropSchemaAfterTest ) {
				createSchemaExport().drop( EnumSet.of( TargetType.DATABASE ) );
			}
		}

		private SchemaUpdate createSchemaUpdate() {
			SchemaUpdate schemaUpdate = new SchemaUpdate( databaseModel, standardServiceRegistry );
			if ( createSqlScriptTempOutputFile() ) {
				schemaUpdate.setOutputFile( output.getAbsolutePath() );
			}
			return schemaUpdate;
		}

		private SchemaExport createSchemaExport() {
			SchemaExport schemaExport = new SchemaExport( databaseModel, standardServiceRegistry );
			if ( createSqlScriptTempOutputFile() ) {
				schemaExport.setOutputFile( output.getAbsolutePath() );
			}
			return schemaExport;
		}

		private SchemaValidator createSchemaValidator() {
			return new SchemaValidator( databaseModel, standardServiceRegistry );
		}

		private SchemaMigrator createSchemaMigrator() {
			return standardServiceRegistry
					.getService( SchemaManagementTool.class )
					.getSchemaMigrator( getDatabaseModel(), Collections.emptyMap() );
		}

		private SchemaCreatorImpl createSchemaCreator(SchemaFilter filter) {
			if ( filter != null ) {
				return new SchemaCreatorImpl( databaseModel, standardServiceRegistry, filter );
			}
			else {
				return new SchemaCreatorImpl( databaseModel, standardServiceRegistry );
			}
		}

		private SchemaDropperImpl createSchemaDropper(SchemaFilter filter) {
			if ( filter != null ) {
				return new SchemaDropperImpl( databaseModel, standardServiceRegistry, filter );
			}
			return new SchemaDropperImpl( databaseModel, standardServiceRegistry );
		}
	}

}
