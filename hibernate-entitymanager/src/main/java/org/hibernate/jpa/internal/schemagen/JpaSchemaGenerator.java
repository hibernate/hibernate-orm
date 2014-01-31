/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.internal.schemagen;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.SchemaGenAction;
import org.hibernate.jpa.SchemaGenSource;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_CONNECTION;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_CREATE_SOURCE;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_DATABASE_ACTION;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_DB_MAJOR_VERSION;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_DB_MINOR_VERSION;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_DB_NAME;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_DROP_SOURCE;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_SCRIPTS_ACTION;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_SCRIPTS_CREATE_TARGET;
import static org.hibernate.jpa.AvailableSettings.SCHEMA_GEN_SCRIPTS_DROP_TARGET;

/**
 * Class responsible for the JPA-defined schema generation behavior.
 *
 * @author Steve Ebersole
 */
public class JpaSchemaGenerator {
	private static final Logger log = Logger.getLogger( JpaSchemaGenerator.class );

	private JpaSchemaGenerator() {
	}

	public static void performGeneration(MetadataImplementor metadata, Map settings, ServiceRegistry serviceRegistry) {
		new Generation( serviceRegistry ).execute( metadata, settings );
	}

	/**
	 * Defines the process of performing a schema generation
	 */
	public static class Generation {
		private final ServiceRegistry serviceRegistry;
		private final ImportSqlCommandExtractor scriptCommandExtractor;
		private final ClassLoaderService classLoaderService;

		/**
		 * Constructs a generation process
		 *
		 * @param serviceRegistry The Hibernate service registry to use
		 */
		public Generation(ServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
			this.scriptCommandExtractor = serviceRegistry.getService( ImportSqlCommandExtractor.class );
			this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		}

		/**
		 * Perform the generation, as indicated by the settings
		 */
		public void execute(MetadataImplementor metadata, Map settings) {
			// First, determine the actions (if any) to be performed ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final SchemaGenAction databaseAction = SchemaGenAction.interpret( settings.get( SCHEMA_GEN_DATABASE_ACTION ) );
			final SchemaGenAction scriptsAction = SchemaGenAction.interpret( settings.get( SCHEMA_GEN_SCRIPTS_ACTION ) );

			if ( databaseAction == SchemaGenAction.NONE && scriptsAction == SchemaGenAction.NONE ) {
				// no actions needed
				log.debug( "No actions specified; doing nothing" );
				return;
			}


			// Figure out the JDBC Connection context, if any ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final JdbcConnectionContext jdbcConnectionContext = determineAppropriateJdbcConnectionContext(
					settings,
					serviceRegistry
			);

			try {
				final Dialect dialect = determineDialect( jdbcConnectionContext, settings, serviceRegistry );


				// determine sources ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				// todo : need a way to force the Dialect to use into the SchemaManagementTool

				final List<GenerationSource> createSourceList = databaseAction.includesCreate() || scriptsAction.includesCreate()
						? buildCreateSourceList( settings, metadata, dialect )
						: Collections.<GenerationSource>emptyList();

				final List<GenerationSource> dropSourceList = databaseAction.includesDrop() || scriptsAction.includesDrop()
						? buildDropSourceList( settings, metadata )
						: Collections.<GenerationSource>emptyList();


				// determine targets ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				final GenerationTarget databaseTarget = new GenerationTargetToDatabase( jdbcConnectionContext, databaseAction );

				final Object createScriptTargetSetting = settings.get( SCHEMA_GEN_SCRIPTS_CREATE_TARGET );
				final Object dropScriptTargetSetting = settings.get( SCHEMA_GEN_SCRIPTS_DROP_TARGET );
				final GenerationTarget scriptsTarget = new GenerationTargetToScript(
						interpretScriptTargetSetting(
								createScriptTargetSetting,
								scriptsAction.includesCreate(),
								SCHEMA_GEN_SCRIPTS_CREATE_TARGET
						),
						interpretScriptTargetSetting(
								dropScriptTargetSetting,
								scriptsAction.includesDrop(),
								SCHEMA_GEN_SCRIPTS_DROP_TARGET
						),
						scriptsAction
				);

				final List<GenerationTarget> targets = Arrays.asList( databaseTarget, scriptsTarget );


				// See if native Hibernate schema generation has also been requested and warn the user if so...
				final String hbm2ddl = (String) settings.get( HBM2DDL_AUTO );
				if ( StringHelper.isNotEmpty( hbm2ddl ) ) {
					log.warnf(
							"Hibernate hbm2ddl-auto setting [%s] was specified in combination with " +
									"JPA schema-generation; combination will likely cause trouble",
							hbm2ddl
					);
				}


				// finally, do the generation

				try {
					doGeneration( createSourceList, dropSourceList, targets );
				}
				finally {
					releaseTargets( targets );
					releaseSources( createSourceList );
					releaseSources( dropSourceList );
				}
			}
			finally {
				releaseJdbcConnectionContext( jdbcConnectionContext );
			}
		}

		private ScriptTargetOutput interpretScriptTargetSetting(
				Object scriptTargetSetting,
				boolean actionIndicatedScripting,
				String settingName) {
			if ( actionIndicatedScripting ) {
				if ( scriptTargetSetting == null ) {
					throw new PersistenceException( "Scripting was requested, but no target was specified" );
				}
				if ( Writer.class.isInstance( scriptTargetSetting ) ) {
					return new ScriptTargetOutputToWriter( (Writer) scriptTargetSetting );
				}
				else {
					final String scriptTargetSettingString = scriptTargetSetting.toString();
					try {
						final URL url = new URL( scriptTargetSettingString );
						return new ScriptTargetOutputToUrl( url );
					}
					catch (MalformedURLException ignore) {
					}
					return new ScriptTargetOutputToFile( new File( scriptTargetSettingString ) );
				}
			}
			else {
				if ( scriptTargetSetting != null ) {
					// the wording in the spec hints that this maybe should be an error, but does not explicitly
					// call out an exception to use.
					log.debugf(
							"Value was specified for '%s' [%s], but scripting action was not requested",
							settingName,
							scriptTargetSetting
					);
				}
				return NoOpScriptTargetOutput.INSTANCE;
			}
		}

		private static class NoOpScriptTargetOutput implements ScriptTargetOutput {
			/**
			 * Singleton access
			 */
			public static final NoOpScriptTargetOutput INSTANCE = new NoOpScriptTargetOutput();

			@Override
			public void accept(String command) {
			}

			@Override
			public void release() {
			}
		}

		private List<GenerationSource> buildCreateSourceList(
				Map configurationValues,
				MetadataImplementor metadata,
				Dialect dialect) {
			final List<GenerationSource> generationSourceList = new ArrayList<GenerationSource>();

			// If we are asked to perform CREATE SCHEMA commands do them first ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final boolean createSchemas = ConfigurationHelper.getBoolean(
					AvailableSettings.SCHEMA_GEN_CREATE_SCHEMAS,
					configurationValues,
					false
			);
			if ( createSchemas ) {
				generationSourceList.add( new CreateSchemaCommandSource( metadata, dialect ) );
			}


			// Next figure out the intended sources of generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			SchemaGenSource sourceType = SchemaGenSource.interpret( configurationValues.get( SCHEMA_GEN_CREATE_SOURCE ) );

			final Object createScriptSourceSetting = configurationValues.get( SCHEMA_GEN_CREATE_SCRIPT_SOURCE );

			if ( sourceType == null ) {
				if ( createScriptSourceSetting != null ) {
					sourceType = SchemaGenSource.SCRIPT;
				}
				else {
					sourceType = SchemaGenSource.METADATA;
				}
			}

			final boolean includesScripts = sourceType != SchemaGenSource.METADATA;
			if ( includesScripts && createScriptSourceSetting == null ) {
				throw new PersistenceException(
						"Schema generation configuration indicated to include CREATE scripts, but no script was specified"
				);
			}
			final ScriptSourceInput scriptSourceInput = includesScripts
					? interpretScriptSourceSetting( createScriptSourceSetting )
					: null;

			if ( sourceType == SchemaGenSource.METADATA ) {
				generationSourceList.add( new GenerationSourceFromMetadata( metadata, serviceRegistry, configurationValues, true ) );
			}
			else if ( sourceType == SchemaGenSource.SCRIPT ) {
				generationSourceList.add( new GenerationSourceFromScript( scriptSourceInput, scriptCommandExtractor ) );
			}
			else if ( sourceType == SchemaGenSource.METADATA_THEN_SCRIPT ) {
				generationSourceList.add( new GenerationSourceFromMetadata( metadata, serviceRegistry, configurationValues, true ) );
				generationSourceList.add( new GenerationSourceFromScript( scriptSourceInput, scriptCommandExtractor ) );
			}
			else if ( sourceType == SchemaGenSource.SCRIPT_THEN_METADATA ) {
				generationSourceList.add( new GenerationSourceFromScript( scriptSourceInput, scriptCommandExtractor ) );
				generationSourceList.add( new GenerationSourceFromMetadata( metadata, serviceRegistry, configurationValues, true ) );
			}


			// finally, see if there is an import script specified ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final Object importScriptSetting = configurationValues.get( SCHEMA_GEN_LOAD_SCRIPT_SOURCE );
			if ( importScriptSetting != null ) {
				final ScriptSourceInput importScriptInput = interpretScriptSourceSetting( importScriptSetting );
				generationSourceList.add( new ImportScriptSource( importScriptInput, scriptCommandExtractor ) );
			}

			return generationSourceList;
		}

		private ScriptSourceInput interpretScriptSourceSetting(Object scriptSourceSetting) {
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
					return new ScriptSourceInputFromUrl( url );
				}

				// assume it is a File path
				final File file = new File( scriptSourceSettingString );
				return new ScriptSourceInputFromFile( file );
			}
		}

		private List<GenerationSource> buildDropSourceList(Map configurationValues, MetadataImplementor metadata) {
			final List<GenerationSource> generationSourceList = new ArrayList<GenerationSource>();

			SchemaGenSource sourceType = SchemaGenSource.interpret( configurationValues.get( SCHEMA_GEN_DROP_SOURCE ) );

			final Object dropScriptSourceSetting = configurationValues.get( SCHEMA_GEN_DROP_SCRIPT_SOURCE );
			if ( sourceType == null ) {
				if ( dropScriptSourceSetting != null ) {
					sourceType = SchemaGenSource.SCRIPT;
				}
				else {
					sourceType = SchemaGenSource.METADATA;
				}
			}


			final boolean includesScripts = sourceType != SchemaGenSource.METADATA;
			if ( includesScripts && dropScriptSourceSetting == null ) {
				throw new PersistenceException(
						"Schema generation configuration indicated to include CREATE scripts, but no script was specified"
				);
			}
			final ScriptSourceInput scriptSourceInput = includesScripts
					? interpretScriptSourceSetting( dropScriptSourceSetting )
					: null;

			if ( sourceType == SchemaGenSource.METADATA ) {
				generationSourceList.add( new GenerationSourceFromMetadata( metadata, serviceRegistry, configurationValues, false ) );
			}
			else if ( sourceType == SchemaGenSource.SCRIPT ) {
				generationSourceList.add( new GenerationSourceFromScript( scriptSourceInput, scriptCommandExtractor ) );
			}
			else if ( sourceType == SchemaGenSource.METADATA_THEN_SCRIPT ) {
				generationSourceList.add( new GenerationSourceFromMetadata( metadata, serviceRegistry, configurationValues, false ) );
				generationSourceList.add( new GenerationSourceFromScript( scriptSourceInput, scriptCommandExtractor ) );
			}
			else if ( sourceType == SchemaGenSource.SCRIPT_THEN_METADATA ) {
				generationSourceList.add( new GenerationSourceFromScript( scriptSourceInput, scriptCommandExtractor ) );
				generationSourceList.add( new GenerationSourceFromMetadata( metadata, serviceRegistry, configurationValues, false ) );
			}

			return generationSourceList;
		}
	}

	private static JdbcConnectionContext determineAppropriateJdbcConnectionContext(
			Map configurationValues,
			ServiceRegistry serviceRegistry) {
		final SqlStatementLogger sqlStatementLogger = serviceRegistry.getService( JdbcServices.class ).getSqlStatementLogger();

		// see if a specific connection has been provided:
		final Connection providedConnection = (Connection) configurationValues.get( SCHEMA_GEN_CONNECTION );

		if ( providedConnection != null ) {
			return new JdbcConnectionContext( new ProvidedJdbcConnectionAccess( providedConnection ), sqlStatementLogger );
		}

		final ConnectionProvider connectionProvider = serviceRegistry.getService( ConnectionProvider.class );
		if ( connectionProvider != null ) {
			return new JdbcConnectionContext( new ConnectionProviderJdbcConnectionAccess( connectionProvider ), sqlStatementLogger );
		}

		// otherwise, return a no-op impl
		return new JdbcConnectionContext( null, sqlStatementLogger ) {
			@Override
			public Connection getJdbcConnection() {
				throw new PersistenceException( "No connection information supplied" );
			}
		};
	}

	private static Dialect determineDialect(
			final JdbcConnectionContext jdbcConnectionContext,
			final Map configurationValues,
			ServiceRegistry serviceRegistry) {

		return serviceRegistry.getService( DialectFactory.class ).buildDialect(
				configurationValues,
				new DialectResolutionInfoSource() {
					@Override
					public DialectResolutionInfo getDialectResolutionInfo() {

						// if the application supplied database name/version info, use that
						final String explicitDbName = (String) configurationValues.get( SCHEMA_GEN_DB_NAME );
						if ( StringHelper.isNotEmpty( explicitDbName ) ) {
							final String explicitDbMajor = (String) configurationValues.get( SCHEMA_GEN_DB_MAJOR_VERSION );
							final String explicitDbMinor = (String) configurationValues.get( SCHEMA_GEN_DB_MINOR_VERSION );

							return new DialectResolutionInfo() {
								@Override
								public String getDatabaseName() {
									return explicitDbName;
								}

								@Override
								public int getDatabaseMajorVersion() {
									return StringHelper.isEmpty( explicitDbMajor )
											? NO_VERSION
											: Integer.parseInt( explicitDbMajor );
								}

								@Override
								public int getDatabaseMinorVersion() {
									return StringHelper.isEmpty( explicitDbMinor )
											? NO_VERSION
											: Integer.parseInt( explicitDbMinor );
								}

								@Override
								public String getDriverName() {
									return null;
								}

								@Override
								public int getDriverMajorVersion() {
									return NO_VERSION;
								}

								@Override
								public int getDriverMinorVersion() {
									return NO_VERSION;
								}
							};
						}

						// otherwise look at the connection, if provided (if not provided the call to
						// getJdbcConnection will already throw a meaningful exception)
						try {
							return new DatabaseMetaDataDialectResolutionInfoAdapter(
									jdbcConnectionContext.getJdbcConnection().getMetaData()
							);
						}
						catch ( SQLException sqlException ) {
							throw new HibernateException(
									"Unable to access java.sql.DatabaseMetaData to determine appropriate Dialect to use",
									sqlException
							);
						}
					}
				}
		);
	}

	private static void doGeneration(
			List<GenerationSource> createSourceList,
			List<GenerationSource> dropSourceList,
			List<GenerationTarget> targets) {
		for ( GenerationTarget target : targets ) {
			for ( GenerationSource source : dropSourceList ) {
				target.acceptDropCommands( source.getCommands() );
			}

			for ( GenerationSource source : createSourceList ) {
				target.acceptCreateCommands( source.getCommands() );
			}
		}
	}

	private static void releaseSources(List<GenerationSource> generationSourceList ) {
		for ( GenerationSource source : generationSourceList ) {
			try {
				source.release();
			}
			catch (Exception e) {
				log.debug( "Problem releasing generation source : " + e.toString() );
			}
		}
	}

	private static void releaseTargets(List<GenerationTarget> generationTargetList) {
		for ( GenerationTarget target : generationTargetList ) {
			try {
				target.release();
			}
			catch (Exception e) {
				log.debug( "Problem releasing generation target : " + e.toString() );
			}
		}
	}

	private static void releaseJdbcConnectionContext(JdbcConnectionContext jdbcConnectionContext) {
		try {
			jdbcConnectionContext.release();
		}
		catch (Exception e) {
			log.debug( "Unable to release JDBC connection after generation" );
		}
	}


	private static class CreateSchemaCommandSource implements GenerationSource {
		private final List<String> commands = new ArrayList<String>();

		private CreateSchemaCommandSource(MetadataImplementor metadata, Dialect dialect) {
			for ( Schema schema : metadata.getDatabase().getSchemas() ) {
				commands.add( dialect.getCreateSchemaCommand( schema.getName().getSchema().getText( dialect ) ) );
				// todo : research CREATE CATALOG commands.  Always CREATE DATABASE?
			}
		}

		@Override
		public Iterable<String> getCommands() {
			return commands;
		}

		@Override
		public void release() {
			// nothing to do
		}
	}

	private static class ImportScriptSource implements GenerationSource {
		private final ScriptSourceInput sourceReader;
		private final ImportSqlCommandExtractor scriptCommandExtractor;

		public ImportScriptSource(ScriptSourceInput sourceReader, ImportSqlCommandExtractor scriptCommandExtractor) {
			this.sourceReader = sourceReader;
			this.scriptCommandExtractor = scriptCommandExtractor;
		}

		@Override
		public Iterable<String> getCommands() {
			return sourceReader.read( scriptCommandExtractor );
		}

		@Override
		public void release() {
			sourceReader.release();
		}
	}


	/**
	 * Defines access to a JDBC Connection explicitly provided to us by the application
	 */
	private static class ProvidedJdbcConnectionAccess implements JdbcConnectionAccess {
		private final Connection jdbcConnection;
		private final boolean wasInitiallyAutoCommit;

		private ProvidedJdbcConnectionAccess(Connection jdbcConnection) {
			this.jdbcConnection = jdbcConnection;

			boolean wasInitiallyAutoCommit;
			try {
				wasInitiallyAutoCommit = jdbcConnection.getAutoCommit();
				if ( ! wasInitiallyAutoCommit ) {
					try {
						jdbcConnection.setAutoCommit( true );
					}
					catch (SQLException e) {
						throw new PersistenceException(
								String.format(
										"Could not set provided connection [%s] to auto-commit mode" +
												" (needed for schema generation)",
										jdbcConnection
								),
								e
						);
					}
				}
			}
			catch (SQLException ignore) {
				wasInitiallyAutoCommit = false;
			}

			log.debugf( "wasInitiallyAutoCommit=%s", wasInitiallyAutoCommit );
			this.wasInitiallyAutoCommit = wasInitiallyAutoCommit;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return jdbcConnection;
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			// NOTE : reset auto-commit, but *do not* close the Connection.  The application handed us this connection

			if ( ! wasInitiallyAutoCommit ) {
				try {
					if ( jdbcConnection.getAutoCommit() ) {
						jdbcConnection.setAutoCommit( false );
					}
				}
				catch (SQLException e) {
					log.info( "Was unable to reset JDBC connection to no longer be in auto-commit mode" );
				}
			}
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}
	}

	/**
	 * Defines access to a JDBC Connection through the defined ConnectionProvider
	 */
	private static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final ConnectionProvider connectionProvider;
		private final Connection jdbcConnection;
		private final boolean wasInitiallyAutoCommit;

		private ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;

			try {
				this.jdbcConnection = connectionProvider.getConnection();
			}
			catch (SQLException e) {
				throw new PersistenceException( "Unable to obtain JDBC Connection", e );
			}

			boolean wasInitiallyAutoCommit;
			try {
				wasInitiallyAutoCommit = jdbcConnection.getAutoCommit();
				if ( ! wasInitiallyAutoCommit ) {
					try {
						jdbcConnection.setAutoCommit( true );
					}
					catch (SQLException e) {
						throw new PersistenceException(
								String.format(
										"Could not set provided connection [%s] to auto-commit mode" +
												" (needed for schema generation)",
										jdbcConnection
								),
								e
						);
					}
				}
			}
			catch (SQLException ignore) {
				wasInitiallyAutoCommit = false;
			}

			log.debugf( "wasInitiallyAutoCommit=%s", wasInitiallyAutoCommit );
			this.wasInitiallyAutoCommit = wasInitiallyAutoCommit;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return jdbcConnection;
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			if ( connection != this.jdbcConnection ) {
				throw new PersistenceException(
						String.format(
								"Connection [%s] passed back to %s was not the one obtained [%s] from it",
								connection,
								ConnectionProviderJdbcConnectionAccess.class.getName(),
								jdbcConnection
						)
				);
			}

			// Reset auto-commit
			if ( ! wasInitiallyAutoCommit ) {
				try {
					if ( jdbcConnection.getAutoCommit() ) {
						jdbcConnection.setAutoCommit( false );
					}
				}
				catch (SQLException e) {
					log.info( "Was unable to reset JDBC connection to no longer be in auto-commit mode" );
				}
			}

			// Release the connection
			connectionProvider.closeConnection( jdbcConnection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}
	}
}
