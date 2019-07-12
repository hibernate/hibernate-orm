/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromFile;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromReader;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromUrl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToUrl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

import org.jboss.logging.Logger;

/**
 * Helper methods.
 *
 * @author Steve Ebersole
 */
public class Helper {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Helper.class );

	public static ScriptSourceInput interpretScriptSourceSetting(
			Object scriptSourceSetting,
			ClassLoaderService classLoaderService,
			String charsetName ) {
		if ( Reader.class.isInstance( scriptSourceSetting ) ) {
			return new ScriptSourceInputFromReader( (Reader) scriptSourceSetting );
		}
		else {
			final String scriptSourceSettingString = scriptSourceSetting.toString();
			log.debugf( "Attempting to resolve script source setting : %s", scriptSourceSettingString );

			// setting could be either:
			//		1) string URL representation (i.e., "file://...")
			//		2) relative file path (resource lookup)
			//		3) absolute file path

			log.trace( "Trying as URL..." );
			// ClassLoaderService.locateResource() first tries the given resource name as url form...
			final URL url = classLoaderService.locateResource( scriptSourceSettingString );
			if ( url != null ) {
				return new ScriptSourceInputFromUrl( url, charsetName );
			}

			// assume it is a File path
			final File file = new File( scriptSourceSettingString );
			return new ScriptSourceInputFromFile( file, charsetName );
		}
	}

	public static ScriptTargetOutput interpretScriptTargetSetting(
			Object scriptTargetSetting,
			ClassLoaderService classLoaderService,
			String charsetName ) {
		if ( scriptTargetSetting == null ) {
			return null;
		}
		else if ( Writer.class.isInstance( scriptTargetSetting ) ) {
			return new ScriptTargetOutputToWriter( (Writer) scriptTargetSetting );
		}
		else {
			final String scriptTargetSettingString = scriptTargetSetting.toString();
			log.debugf( "Attempting to resolve script source setting : %s", scriptTargetSettingString );

			// setting could be either:
			//		1) string URL representation (i.e., "file://...")
			//		2) relative file path (resource lookup)
			//		3) absolute file path

			log.trace( "Trying as URL..." );
			// ClassLoaderService.locateResource() first tries the given resource name as url form...
			final URL url = classLoaderService.locateResource( scriptTargetSettingString );
			if ( url != null ) {
				return new ScriptTargetOutputToUrl( url, charsetName );
			}

			// assume it is a File path
			final File file = new File( scriptTargetSettingString );
			return new ScriptTargetOutputToFile( file, charsetName );
		}
	}

	public static boolean interpretNamespaceHandling(Map configurationValues) {
		//Print a warning if multiple conflicting properties are being set:
		int count = 0;
		if ( configurationValues.containsKey( AvailableSettings.HBM2DDL_CREATE_SCHEMAS ) ) {
			count++;
		}
		if ( configurationValues.containsKey( AvailableSettings.HBM2DDL_CREATE_NAMESPACES ) ) {
			count++;
		}
		if ( configurationValues.containsKey( AvailableSettings.HBM2DLL_CREATE_NAMESPACES ) ) {
			count++;
		}
		if ( count > 1 ) {
			log.multipleSchemaCreationSettingsDefined();
		}
		// prefer the JPA setting...
		return ConfigurationHelper.getBoolean(
				AvailableSettings.HBM2DDL_CREATE_SCHEMAS,
				configurationValues,
				//Then try the Hibernate ORM setting:
				ConfigurationHelper.getBoolean(
						AvailableSettings.HBM2DDL_CREATE_NAMESPACES,
						configurationValues,
						//And finally fall back to the old name this had before we fixed the typo:
						ConfigurationHelper.getBoolean(
								AvailableSettings.HBM2DLL_CREATE_NAMESPACES,
								configurationValues,
								false
						)
				)
		);
	}

	public static boolean interpretFormattingEnabled(Map configurationValues) {
		return ConfigurationHelper.getBoolean(
				AvailableSettings.FORMAT_SQL,
				configurationValues,
				false
		);
	}

	public static DatabaseInformation buildDatabaseInformation(
			ServiceRegistry serviceRegistry,
			DdlTransactionIsolator ddlTransactionIsolator,
			Namespace.Name defaultNamespace) {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		try {
			return new DatabaseInformationImpl(
					serviceRegistry,
					jdbcEnvironment,
					ddlTransactionIsolator,
					defaultNamespace
			);
		}
		catch (SQLException e) {
			throw jdbcEnvironment.getSqlExceptionHelper().convert( e, "Unable to build DatabaseInformation" );
		}
	}
}
