/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.Metadata;
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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerCollectingImpl;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A commandline tool to update a database schema. May also be called from inside an application.
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public class SchemaUpdate {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SchemaUpdate.class );

	private final List<Exception> exceptions = new ArrayList<Exception>();

	boolean haltOnError = false;

	private String outputFile;
	private String delimiter;
	private boolean format;

	public void execute(EnumSet<TargetType> targetTypes, Metadata metadata) {
		execute( targetTypes, metadata, ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry() );
	}

	@SuppressWarnings("unchecked")
	public void execute(EnumSet<TargetType> targetTypes, Metadata metadata, ServiceRegistry serviceRegistry) {
		if ( targetTypes.isEmpty() ) {
			LOG.debug( "Skipping SchemaExport as no targets were specified" );
			return;
		}

		exceptions.clear();
		LOG.runningHbm2ddlSchemaUpdate();

		Map config = new HashMap( serviceRegistry.getService( ConfigurationService.class ).getSettings() );
		config.put( AvailableSettings.HBM2DDL_DELIMITER, delimiter );
		config.put( AvailableSettings.FORMAT_SQL, format );

		final SchemaManagementTool tool = serviceRegistry.getService( SchemaManagementTool.class );

		final ExceptionHandler exceptionHandler = haltOnError
				? ExceptionHandlerHaltImpl.INSTANCE
				: new ExceptionHandlerCollectingImpl();

		final ExecutionOptions executionOptions = SchemaManagementToolCoordinator.buildExecutionOptions(
				config,
				exceptionHandler
		);

		final TargetDescriptor targetDescriptor = SchemaExport.buildTargetDescriptor( targetTypes, outputFile, serviceRegistry );

		try {
			tool.getSchemaMigrator( config ).doMigration( metadata, executionOptions, targetDescriptor );
		}
		finally {
			if ( exceptionHandler instanceof ExceptionHandlerCollectingImpl ) {
				exceptions.addAll( ( (ExceptionHandlerCollectingImpl) exceptionHandler ).getExceptions() );
			}
		}
	}

	/**
	 * Returns a List of all Exceptions which occurred during the export.
	 *
	 * @return A List containing the Exceptions occurred during the export
	 */
	public List getExceptions() {
		return exceptions;
	}

	public SchemaUpdate setHaltOnError(boolean haltOnError) {
		this.haltOnError = haltOnError;
		return this;
	}

	public SchemaUpdate setFormat(boolean format) {
		this.format = format;
		return this;
	}

	public SchemaUpdate setOutputFile(String outputFile) {
		this.outputFile = outputFile;
		return this;
	}

	/**
	 * Set the end of statement delimiter
	 *
	 * @param delimiter The delimiter
	 *
	 */
	public SchemaUpdate setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs parsedArgs = CommandLineArgs.parseCommandLineArgs( args );
			final StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( parsedArgs );

			try {
				final MetadataImplementor metadata = buildMetadata( parsedArgs, serviceRegistry );

				new SchemaUpdate()
						.setOutputFile( parsedArgs.outputFile )
						.setDelimiter( parsedArgs.delimiter )
						.execute( parsedArgs.targetTypes, metadata, serviceRegistry );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
		catch (Exception e) {
			LOG.unableToRunSchemaUpdate( e );
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
		EnumSet<TargetType> targetTypes;

		String propertiesFile = null;
		String cfgXmlFile = null;
		String outputFile = null;
		String delimiter = null;

		String implicitNamingStrategyImplName = null;
		String physicalNamingStrategyImplName = null;

		List<String> hbmXmlFiles = new ArrayList<String>();
		List<String> jarFiles = new ArrayList<String>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			final CommandLineArgs parsedArgs = new CommandLineArgs();

			String targetText = null;
			boolean script = true;
			boolean doUpdate = true;

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.equals( "--quiet" ) ) {
						script = false;
					}
					else if ( arg.startsWith( "--text" ) ) {
						doUpdate = false;
					}
					else if ( arg.startsWith( "--target=" ) ) {
						targetText = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--properties=" ) ) {
						parsedArgs.propertiesFile = arg.substring( 13 );
					}
					else if ( arg.startsWith( "--config=" ) ) {
						parsedArgs.cfgXmlFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--output=" ) ) {
						parsedArgs.outputFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--naming=" ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyArgument();
					}
					else if ( arg.startsWith( "--delimiter=" ) ) {
						parsedArgs.delimiter = arg.substring( 12 );
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

			if ( targetText == null ) {
				parsedArgs.targetTypes = TargetTypeHelper.parseLegacyCommandLineOptions( script, doUpdate, parsedArgs.outputFile );
			}
			else {
				if ( !script || !doUpdate ) {
					LOG.warn( "--text or --quiet was used; prefer --target=none|(stdout|database|script)*" );
				}
				parsedArgs.targetTypes = TargetTypeHelper.parseCommandLineOptions( targetText );
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
