/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.internal.TargetDatabaseImpl;
import org.hibernate.tool.schema.internal.TargetFileImpl;
import org.hibernate.tool.schema.internal.TargetStdoutImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;

/**
 * A commandline tool to update a database schema. May also be called from inside an application.
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public class SchemaUpdate {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SchemaUpdate.class );

	private final MetadataImplementor metadata;
	private final ServiceRegistry serviceRegistry;

	private final JdbcConnectionAccess jdbcConnectionAccess;
	private final List<Exception> exceptions = new ArrayList<Exception>();
	private String outputFile;

	/**
	 * Creates a SchemaUpdate object.  This form is intended for use from tooling
	 *
	 * @param metadata The metadata defining the schema as it should be after update
	 *
	 * @throws HibernateException
	 */
	public SchemaUpdate(MetadataImplementor metadata) {
		this( metadata.getMetadataBuildingOptions().getServiceRegistry(), metadata );
	}

	/**
	 * Creates a SchemaUpdate object.  This form is intended for use from
	 * {@code hibernate.hbm2ddl.auto} handling, generally from within the SessionFactory
	 * ctor.
	 * <p/>
	 * Note that the passed ServiceRegistry is expected to be of type
	 * {@link org.hibernate.service.spi.SessionFactoryServiceRegistry}, although
	 * any ServiceRegistry type will work as long as it has access to the
	 * {@link org.hibernate.engine.jdbc.spi.JdbcServices} service.
	 *
	 * @param serviceRegistry The ServiceRegistry to use.
	 * @param metadata The metadata defining the schema as it should be after update
	 *
	 * @throws HibernateException
	 */
	public SchemaUpdate(ServiceRegistry serviceRegistry, MetadataImplementor metadata) throws HibernateException {
		this.metadata = metadata;
		this.serviceRegistry = serviceRegistry;
		this.jdbcConnectionAccess = serviceRegistry.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
	}

	/**
	 * Execute the schema updates
	 *
	 * @param script print all DDL to the console
	 */
	public void execute(boolean script, boolean doUpdate) {
		execute( Target.interpret( script, doUpdate ) );
	}

	public void execute(Target target) {
		LOG.runningHbm2ddlSchemaUpdate();

		exceptions.clear();

		List<org.hibernate.tool.schema.spi.Target> toolTargets = buildToolTargets( target );

		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		final SchemaMigrator schemaMigrator = serviceRegistry.getService( SchemaManagementTool.class )
				.getSchemaMigrator( cfgService.getSettings() );

		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		final DatabaseInformation databaseInformation;
		try {
			databaseInformation = new DatabaseInformationImpl(
					serviceRegistry,
					serviceRegistry.getService( JdbcEnvironment.class ),
					jdbcConnectionAccess,
					metadata.getDatabase().getDefaultNamespace().getPhysicalName().getCatalog(),
					metadata.getDatabase().getDefaultNamespace().getPhysicalName().getSchema()
			);
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Error creating DatabaseInformation for schema migration"
			);
		}

		schemaMigrator.doMigration( metadata, databaseInformation, true, toolTargets );
	}

	private List<org.hibernate.tool.schema.spi.Target> buildToolTargets(Target target) {
		List<org.hibernate.tool.schema.spi.Target> toolTargets = new ArrayList<org.hibernate.tool.schema.spi.Target>();

		if ( target.doScript() ) {
			toolTargets.add( new TargetStdoutImpl() );
		}

		if ( target.doExport() ) {
			toolTargets.add( new TargetDatabaseImpl( jdbcConnectionAccess ) );
		}

		if ( outputFile != null ) {
			LOG.writingGeneratedSchemaToFile( outputFile );
			toolTargets.add( new TargetFileImpl( outputFile ) );
		}

		return toolTargets;
	}

	/**
	 * Returns a List of all Exceptions which occured during the export.
	 *
	 * @return A List containig the Exceptions occured during the export
	 */
	public List getExceptions() {
		return exceptions;
	}

	public void setHaltOnError(boolean haltOnError) {
	}

	public void setFormat(boolean format) {
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public void setDelimiter(String delimiter) {
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs parsedArgs = CommandLineArgs.parseCommandLineArgs( args );
			final StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( parsedArgs );

			try {
				final MetadataImplementor metadata = buildMetadata( parsedArgs, serviceRegistry );

				final SchemaUpdate schemaUpdate = new SchemaUpdate( metadata );
				schemaUpdate.setOutputFile( parsedArgs.outFile );
				schemaUpdate.execute( parsedArgs.script, parsedArgs.doUpdate );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
		catch (Exception e) {
			LOG.unableToRunSchemaUpdate( e );
			e.printStackTrace();
		}
	}

	private static StandardServiceRegistry buildStandardServiceRegistry(CommandLineArgs parsedArgs) throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		if ( parsedArgs.cfgXmlFile != null ) {
			ssrBuilder.configure( parsedArgs.cfgXmlFile );
		}

		if ( parsedArgs.propertiesFile != null ) {
			Properties props = new Properties();
			props.load( new FileInputStream( parsedArgs.propertiesFile ) );
			ssrBuilder.applySettings( props );
		}

		return ssrBuilder.build();
	}

	private static MetadataImplementor buildMetadata(CommandLineArgs parsedArgs, ServiceRegistry serviceRegistry)
			throws Exception {
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );

		for ( String filename : parsedArgs.hbmXmlFiles ) {
			metadataSources.addFile( filename );
		}

		for ( String filename : parsedArgs.jarFiles ) {
			metadataSources.addJar( new File( filename ) );
		}


		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		if ( parsedArgs.implicitNamingStrategyImplName != null ) {
			metadataBuilder.applyImplicitNamingStrategy(
					strategySelector.resolveStrategy(
							ImplicitNamingStrategy.class,
							parsedArgs.implicitNamingStrategyImplName
					)
			);
		}
		if ( parsedArgs.physicalNamingStrategyImplName != null ) {
			metadataBuilder.applyPhysicalNamingStrategy(
					strategySelector.resolveStrategy(
							PhysicalNamingStrategy.class,
							parsedArgs.physicalNamingStrategyImplName
					)
			);
		}

		return (MetadataImplementor) metadataBuilder.build();
	}

	private static class CommandLineArgs {
		boolean script = true;
		// If true then execute db updates, otherwise just generate and display updates
		boolean doUpdate = true;

		String propertiesFile = null;
		String cfgXmlFile = null;
		String outFile = null;

		String implicitNamingStrategyImplName = null;
		String physicalNamingStrategyImplName = null;

		List<String> hbmXmlFiles = new ArrayList<String>();
		List<String> jarFiles = new ArrayList<String>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			final CommandLineArgs parsedArgs = new CommandLineArgs();

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.equals( "--quiet" ) ) {
						parsedArgs.script = false;
					}
					else if ( arg.startsWith( "--properties=" ) ) {
						parsedArgs.propertiesFile = arg.substring( 13 );
					}
					else if ( arg.startsWith( "--config=" ) ) {
						parsedArgs.cfgXmlFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--text" ) ) {
						parsedArgs.doUpdate = false;
					}
					else if ( arg.startsWith( "--output=" ) ) {
						parsedArgs.outFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--naming=" ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyArgument();
					}
					else if ( arg.startsWith( "--implicit-naming=" ) ) {
						parsedArgs.implicitNamingStrategyImplName = arg.substring( 18 );
					}
					else if ( arg.startsWith( "--physical-naming=" ) ) {
						parsedArgs.physicalNamingStrategyImplName = arg.substring( 18 );
					}
				}
				else {
					if ( arg.endsWith( ".jar" ) ) {
						parsedArgs.jarFiles.add( arg );
					}
					else {
						parsedArgs.hbmXmlFiles.add( arg );
					}
				}
			}

			return parsedArgs;
		}
	}

	/**
	 * Intended for test usage only.  Builds a Metadata using the same algorithm  as
	 * {@link #main}
	 *
	 * @param args The "command line args"
	 *
	 * @return The built Metadata
	 *
	 * @throws Exception Problems building the Metadata
	 */
	public static MetadataImplementor buildMetadataFromMainArgs(String[] args) throws Exception {
		final CommandLineArgs commandLineArgs = CommandLineArgs.parseCommandLineArgs( args );
		StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( commandLineArgs );
		try {
			return buildMetadata( commandLineArgs, serviceRegistry );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
}
