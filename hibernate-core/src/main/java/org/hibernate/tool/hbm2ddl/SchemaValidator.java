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
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.jboss.logging.Logger;

/**
 * A commandline tool to update a database schema. May also be called from
 * inside an application.
 *
 * @author Christoph Sturm
 */
public class SchemaValidator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SchemaValidator.class.getName()
	);

	private final ServiceRegistry serviceRegistry;
	private final MetadataImplementor metadata;
	private final JdbcConnectionAccess jdbcConnectionAccess;

	public SchemaValidator(MetadataImplementor metadata) {
		this( metadata.getMetadataBuildingOptions().getServiceRegistry(), metadata );
	}

	public SchemaValidator(ServiceRegistry serviceRegistry, MetadataImplementor metadata) {
		this.serviceRegistry = serviceRegistry;
		this.metadata = metadata;
		this.jdbcConnectionAccess = serviceRegistry.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
	}

	/**
	 * Perform the validations.
	 */
	public void validate() {
		LOG.runningSchemaValidator();

		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
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
					"Error creating DatabaseInformation for schema validation"
			);
		}

		serviceRegistry.getService( SchemaManagementTool.class ).getSchemaValidator( cfgService.getSettings() )
				.doValidation( metadata, databaseInformation );
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs parsedArgs = CommandLineArgs.parseCommandLineArgs( args );
			final StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( parsedArgs );

			try {
				final MetadataImplementor metadata = buildMetadata( parsedArgs, serviceRegistry );
				new SchemaValidator( serviceRegistry, metadata ).validate();
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

	private static class CommandLineArgs {
		String implicitNamingStrategy = null;
		String physicalNamingStrategy = null;

		String propertiesFile = null;
		String cfgXmlFile = null;
		List<String> hbmXmlFiles = new ArrayList<String>();
		List<String> jarFiles = new ArrayList<String>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			final CommandLineArgs parsedArgs = new CommandLineArgs();

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.startsWith( "--properties=" ) ) {
						parsedArgs.propertiesFile = arg.substring( 13 );
					}
					else if ( arg.startsWith( "--config=" ) ) {
						parsedArgs.cfgXmlFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--naming=" ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyArgument();
					}
					else if ( arg.startsWith( "--implicit-naming=" ) ) {
						parsedArgs.implicitNamingStrategy = arg.substring( 18 );
					}
					else if ( arg.startsWith( "--physical-naming=" ) ) {
						parsedArgs.physicalNamingStrategy = arg.substring( 18 );
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

	private static StandardServiceRegistry buildStandardServiceRegistry(CommandLineArgs parsedArgs) throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		if ( parsedArgs.cfgXmlFile != null ) {
			ssrBuilder.configure( parsedArgs.cfgXmlFile );
		}

		if ( parsedArgs.propertiesFile != null ) {
			Properties properties = new Properties();
			properties.load( new FileInputStream( parsedArgs.propertiesFile ) );
			ssrBuilder.applySettings( properties );
		}

		return ssrBuilder.build();
	}

	private static MetadataImplementor buildMetadata(
			CommandLineArgs parsedArgs,
			StandardServiceRegistry serviceRegistry) throws Exception {

		final MetadataSources metadataSources = new MetadataSources(serviceRegistry);

		for ( String filename : parsedArgs.hbmXmlFiles ) {
			metadataSources.addFile( filename );
		}

		for ( String filename : parsedArgs.jarFiles ) {
			metadataSources.addJar( new File( filename ) );
		}

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		if ( parsedArgs.implicitNamingStrategy != null ) {
			metadataBuilder.applyImplicitNamingStrategy(
					strategySelector.resolveStrategy( ImplicitNamingStrategy.class, parsedArgs.implicitNamingStrategy )
			);
		}
		if ( parsedArgs.physicalNamingStrategy != null ) {
			metadataBuilder.applyPhysicalNamingStrategy(
					strategySelector.resolveStrategy( PhysicalNamingStrategy.class, parsedArgs.physicalNamingStrategy )
			);
		}

		return (MetadataImplementor) metadataBuilder.build();

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
