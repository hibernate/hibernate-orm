/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.internal.exec.AbstractScriptSourceInput;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputAggregate;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromFile;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromReader;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromUrl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToUrl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CREATE_NAMESPACES;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CREATE_SCHEMAS;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.splitAtCommas;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Helper methods.
 *
 * @author Steve Ebersole
 */
public class Helper {

	public static ScriptSourceInput interpretScriptSourceSetting(
			Object scriptSourceSetting, //Reader or String URL
			ClassLoaderService classLoaderService,
			String charsetName ) {
		if ( scriptSourceSetting instanceof Reader reader ) {
			return new ScriptSourceInputFromReader( reader );
		}
		else {
			final String scriptSourceSettingString = scriptSourceSetting.toString();
			CORE_LOGGER.attemptingToResolveScriptSourceSetting( scriptSourceSettingString );
			final String[] paths = splitAtCommas( scriptSourceSettingString );
			if ( paths.length == 1 ) {
				return interpretScriptSourceSetting( scriptSourceSettingString, classLoaderService, charsetName );
			}
			final var inputs = new AbstractScriptSourceInput[paths.length];
			for ( int i = 0; i < paths.length; i++ ) {
				inputs[i] = interpretScriptSourceSetting( paths[i], classLoaderService, charsetName ) ;
			}
			return new ScriptSourceInputAggregate( inputs );
		}
	}

	private static AbstractScriptSourceInput interpretScriptSourceSetting(
			String scriptSourceSettingString,
			ClassLoaderService classLoaderService,
			String charsetName) {
		// setting could be either:
		//		1) string URL representation (i.e., "file://...")
		//		2) relative file path (resource lookup)
		//		3) absolute file path

		CORE_LOGGER.trace( "Trying as URL..." );
		// ClassLoaderService.locateResource() first tries the given resource name as url form...
		final URL url = classLoaderService.locateResource( scriptSourceSettingString );
		return url != null
				? new ScriptSourceInputFromUrl( url, charsetName )
				// assume it is a File path
				: new ScriptSourceInputFromFile( new File( scriptSourceSettingString ), charsetName );
	}

	public static ScriptTargetOutput interpretScriptTargetSetting(
			Object scriptTargetSetting,
			ClassLoaderService classLoaderService,
			String charsetName,
			boolean append) {
		if ( scriptTargetSetting == null ) {
			return null;
		}
		else if ( scriptTargetSetting instanceof Writer writer ) {
			return new ScriptTargetOutputToWriter( writer );
		}
		else {
			final String scriptTargetSettingString = scriptTargetSetting.toString();
			CORE_LOGGER.attemptingToResolveScriptSourceSetting( scriptTargetSettingString );

			// setting could be either:
			//		1) string URL representation (i.e., "file://...")
			//		2) relative file path (resource lookup)
			//		3) absolute file path

			// ClassLoaderService.locateResource() first tries the given resource name as URL form...
			final URL url = classLoaderService.locateResource( scriptTargetSettingString );
			return url != null
					? new ScriptTargetOutputToUrl( url, charsetName )
					// assume it is a File path
					: new ScriptTargetOutputToFile( new File( scriptTargetSettingString ), charsetName, append );
		}
	}

	public static boolean interpretNamespaceHandling(Map<String,Object> configurationValues) {
		warnIfConflictingPropertiesSet( configurationValues );
		// prefer the JPA setting...
		return getBoolean(
				HBM2DDL_CREATE_SCHEMAS,
				configurationValues,
				//Then try the Jakarta JPA setting:
				getBoolean(
						JAKARTA_HBM2DDL_CREATE_SCHEMAS,
						configurationValues,
						//Then try the Hibernate ORM setting:
						getBoolean(
								HBM2DDL_CREATE_NAMESPACES,
								configurationValues
						)
				)
		);
	}

	private static void warnIfConflictingPropertiesSet(Map<String, Object> configurationValues) {
		//Print a warning if multiple conflicting properties are being set:
		int count = 0;
		if ( configurationValues.containsKey( HBM2DDL_CREATE_SCHEMAS ) ) {
			count++;
		}
		if ( configurationValues.containsKey( JAKARTA_HBM2DDL_CREATE_SCHEMAS ) ) {
			count++;
		}
		if ( configurationValues.containsKey( HBM2DDL_CREATE_NAMESPACES ) ) {
			count++;
		}
		if ( count > 1 ) {
			CORE_LOGGER.multipleSchemaCreationSettingsDefined();
		}
	}

	public static boolean interpretFormattingEnabled(Map<String,Object> configurationValues) {
		return getBoolean( FORMAT_SQL, configurationValues );
	}

	public static DatabaseInformation buildDatabaseInformation(
			DdlTransactionIsolator ddlTransactionIsolator,
			SqlStringGenerationContext context,
			SchemaManagementTool tool) {
		final var serviceRegistry = ddlTransactionIsolator.getJdbcContext().getServiceRegistry();
		final var jdbcEnvironment = serviceRegistry.requireService( JdbcEnvironment.class );
		try {
			return new DatabaseInformationImpl(
					serviceRegistry,
					jdbcEnvironment,
					context,
					ddlTransactionIsolator,
					tool
			);
		}
		catch (SQLException e) {
			throw jdbcEnvironment.getSqlExceptionHelper()
					.convert( e, "Unable to build DatabaseInformation" );
		}
	}

	public static SqlStringGenerationContext createSqlStringGenerationContext(ExecutionOptions options, Metadata metadata) {
		final var database = metadata.getDatabase();
		return SqlStringGenerationContextImpl.fromConfigurationMap(
				database.getJdbcEnvironment(),
				database,
				options.getConfigurationValues()
		);
	}

	public static void applySqlStrings(
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings != null ) {
			for ( String sqlString : sqlStrings ) {
				applySqlString( sqlString, formatter, options, targets );
			}
		}
	}

	public static void applySqlString(
			String sqlString,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( !isEmpty( sqlString ) ) {
			final String sqlStringFormatted = formatter.format( sqlString );
			for ( var target : targets ) {
				try {
					target.accept( sqlStringFormatted );
				}
				catch (CommandAcceptanceException e) {
					options.getExceptionHandler().handleException( e );
				}
			}
		}
	}

	public static void applyScript(
			ExecutionOptions options,
			SqlScriptCommandExtractor commandExtractor,
			Dialect dialect,
			ScriptSourceInput scriptInput,
			Formatter formatter,
			GenerationTarget[] targets) {
		final var commands =
				scriptInput.extract( reader -> commandExtractor.extractCommands( reader, dialect ) );
		for ( var target : targets ) {
			target.beforeScript( scriptInput );
		}
		for ( String command : commands ) {
			applySqlString( command, formatter, options, targets );
		}
	}
}
