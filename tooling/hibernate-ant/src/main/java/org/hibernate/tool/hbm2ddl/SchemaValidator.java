/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * A commandline tool to update a database schema. May also be called from
 * inside an application.
 *
 * @author Christoph Sturm
 */
public class SchemaValidator {

	public void validate(Metadata metadata) {
		validate( metadata, ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry() );
	}

	public void validate(Metadata metadata, ServiceRegistry serviceRegistry) {
		CORE_LOGGER.runningSchemaValidator();

		Map<String, Object> config =
				new HashMap<>( serviceRegistry.requireService( ConfigurationService.class ).getSettings() );

		final SchemaManagementTool tool = serviceRegistry.requireService( SchemaManagementTool.class );

		final ExecutionOptions executionOptions = SchemaManagementToolCoordinator.buildExecutionOptions(
				config,
				ExceptionHandlerHaltImpl.INSTANCE
		);

		tool.getSchemaValidator( config ).doValidation( metadata, executionOptions, ContributableMatcher.ALL );
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs parsedArgs = CommandLineArgs.parseCommandLineArgs( args );
			final StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( parsedArgs );

			try {
				final MetadataImplementor metadata = buildMetadata( parsedArgs, serviceRegistry );
				new SchemaValidator().validate( metadata, serviceRegistry );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
		catch (Exception e) {
			CORE_LOGGER.unableToRunSchemaUpdate( e );
		}
	}

	private static class CommandLineArgs {
		String implicitNamingStrategy = null;
		String physicalNamingStrategy = null;

		String propertiesFile = null;
		String cfgXmlFile = null;
		List<String> hbmXmlFiles = new ArrayList<>();
		List<String> jarFiles = new ArrayList<>();

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
			try ( final FileInputStream fis = new FileInputStream( parsedArgs.propertiesFile ) ) {
				properties.load( fis );
			}
			ssrBuilder.applySettings( properties );
		}

		return ssrBuilder.build();
	}

	private static MetadataImplementor buildMetadata(
			CommandLineArgs parsedArgs,
			StandardServiceRegistry serviceRegistry) {

		final MetadataSources metadataSources = new MetadataSources(serviceRegistry);

		for ( String filename : parsedArgs.hbmXmlFiles ) {
			metadataSources.addFile( filename );
		}

		for ( String filename : parsedArgs.jarFiles ) {
			metadataSources.addJar( new File( filename ) );
		}

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );
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
