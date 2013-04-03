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

import javax.persistence.PersistenceException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseInfoDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.SchemaGenAction;
import org.hibernate.jpa.SchemaGenSource;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * Class responsible for the JPA-defined schema generation behavior.
 *
 * @author Steve Ebersole
 */
public class JpaSchemaGenerator {
	private static final Logger log = Logger.getLogger( JpaSchemaGenerator.class );

	public static void performGeneration(Configuration hibernateConfiguration, ServiceRegistry serviceRegistry) {

		// First, determine the actions (if any) to be performed ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final SchemaGenAction databaseAction = SchemaGenAction.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_DATABASE_ACTION )
		);
		final SchemaGenAction scriptsAction = SchemaGenAction.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_SCRIPTS_ACTION )
		);

		if ( databaseAction == SchemaGenAction.NONE && scriptsAction == SchemaGenAction.NONE ) {
			// no actions needed
			return;
		}


		// Figure out the JDBC Connection context, if any ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final JdbcConnectionContext jdbcConnectionContext = determineAppropriateJdbcConnectionContext(
				hibernateConfiguration,
				serviceRegistry
		);

		try {
			final Dialect dialect = determineDialect( jdbcConnectionContext, hibernateConfiguration, serviceRegistry );


			// determine sources ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final List<GenerationSource> createSourceList = databaseAction.includesCreate() || scriptsAction.includesCreate()
					? buildCreateSourceList( hibernateConfiguration, serviceRegistry, dialect )
					: Collections.<GenerationSource>emptyList();

			final List<GenerationSource> dropSourceList = databaseAction.includesDrop() || scriptsAction.includesDrop()
					? buildDropSourceList( hibernateConfiguration, serviceRegistry, dialect )
					: Collections.<GenerationSource>emptyList();


			// determine targets ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			final GenerationTarget databaseTarget = new DatabaseTarget( jdbcConnectionContext, databaseAction );

			final Object createScriptTargetSetting = hibernateConfiguration.getProperties().get(
					AvailableSettings.SCHEMA_GEN_SCRIPTS_CREATE_TARGET
			);
			final Object dropScriptTargetSetting = hibernateConfiguration.getProperties().get(
					AvailableSettings.SCHEMA_GEN_SCRIPTS_DROP_TARGET
			);
			final GenerationTarget scriptsTarget = new ScriptsTarget( createScriptTargetSetting, dropScriptTargetSetting, scriptsAction );

			final List<GenerationTarget> targets = Arrays.asList( databaseTarget, scriptsTarget );


			// See if native Hibernate schema generation has also been requested and warn the user if so...

			final String hbm2ddl = hibernateConfiguration.getProperty( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO );
			if ( StringHelper.isNotEmpty( hbm2ddl ) ) {
				log.warnf(
						"Hibernate hbm2ddl-auto setting was specified [%s] in combination with JPA schema-generation; " +
								"combination will likely cause trouble",
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

	private static List<GenerationSource> buildCreateSourceList(
			Configuration hibernateConfiguration,
			ServiceRegistry serviceRegistry,
			Dialect dialect) {
		final List<GenerationSource> generationSourceList = new ArrayList<GenerationSource>();

		final boolean createSchemas = ConfigurationHelper.getBoolean(
				AvailableSettings.SCHEMA_GEN_CREATE_SCHEMAS,
				hibernateConfiguration.getProperties(),
				false
		);
		if ( createSchemas ) {
			generationSourceList.add( new CreateSchemaCommandSource( hibernateConfiguration, dialect ) );
		}

		SchemaGenSource sourceType = SchemaGenSource.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_CREATE_SOURCE )
		);

		final Object createScriptSourceSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE
		);

		if ( sourceType == null ) {
			if ( createScriptSourceSetting != null ) {
				sourceType = SchemaGenSource.SCRIPTS;
			}
			else {
				sourceType = SchemaGenSource.METADATA;
			}
		}

		final ImportSqlCommandExtractor scriptCommandExtractor = serviceRegistry.getService( ImportSqlCommandExtractor.class );

		if ( sourceType == SchemaGenSource.METADATA ) {
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect, true ) );
		}
		else if ( sourceType == SchemaGenSource.SCRIPTS ) {
			generationSourceList.add( new ScriptSource( createScriptSourceSetting, scriptCommandExtractor ) );
		}
		else if ( sourceType == SchemaGenSource.METADATA_THEN_SCRIPTS ) {
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect, true ) );
			generationSourceList.add( new ScriptSource( createScriptSourceSetting, scriptCommandExtractor ) );
		}
		else if ( sourceType == SchemaGenSource.SCRIPTS_THEN_METADATA ) {
			generationSourceList.add( new ScriptSource( createScriptSourceSetting, scriptCommandExtractor ) );
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect, true ) );
		}

		final Object importScriptSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE
		);
		if ( importScriptSetting != null ) {
			generationSourceList.add( new ImportScriptSource( importScriptSetting, scriptCommandExtractor ) );
		}

		return generationSourceList;
	}

	private static List<GenerationSource> buildDropSourceList(
			Configuration hibernateConfiguration,
			ServiceRegistry serviceRegistry,
			Dialect dialect) {
		final List<GenerationSource> generationSourceList = new ArrayList<GenerationSource>();

		SchemaGenSource sourceType = SchemaGenSource.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_DROP_SOURCE )
		);

		final Object dropScriptSourceSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE
		);

		if ( sourceType == null ) {
			if ( dropScriptSourceSetting != null ) {
				sourceType = SchemaGenSource.SCRIPTS;
			}
			else {
				sourceType = SchemaGenSource.METADATA;
			}
		}

		final ImportSqlCommandExtractor scriptCommandExtractor = serviceRegistry.getService( ImportSqlCommandExtractor.class );

		if ( sourceType == SchemaGenSource.METADATA ) {
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect, false ) );
		}
		else if ( sourceType == SchemaGenSource.SCRIPTS ) {
			generationSourceList.add( new ScriptSource( dropScriptSourceSetting, scriptCommandExtractor ) );
		}
		else if ( sourceType == SchemaGenSource.METADATA_THEN_SCRIPTS ) {
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect, false ) );
			generationSourceList.add( new ScriptSource( dropScriptSourceSetting, scriptCommandExtractor ) );
		}
		else if ( sourceType == SchemaGenSource.SCRIPTS_THEN_METADATA ) {
			generationSourceList.add( new ScriptSource( dropScriptSourceSetting, scriptCommandExtractor ) );
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect, false ) );
		}

		return generationSourceList;
	}

	private static JdbcConnectionContext determineAppropriateJdbcConnectionContext(
			Configuration hibernateConfiguration,
			ServiceRegistry serviceRegistry) {
		final SqlStatementLogger sqlStatementLogger = serviceRegistry.getService( JdbcServices.class ).getSqlStatementLogger();

		// see if a specific connection has been provided:
		final Connection providedConnection = (Connection) hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_CONNECTION
		);

		if ( providedConnection != null ) {
			return new JdbcConnectionContext(
					new JdbcConnectionAccess() {
						@Override
						public Connection obtainConnection() throws SQLException {
							return providedConnection;
						}

						@Override
						public void releaseConnection(Connection connection) throws SQLException {
							// do nothing
						}

						@Override
						public boolean supportsAggressiveRelease() {
							return false;
						}
					},
					sqlStatementLogger
			);
		}

		final ConnectionProvider connectionProvider = serviceRegistry.getService( ConnectionProvider.class );
		if ( connectionProvider != null ) {
			return new JdbcConnectionContext(
					new JdbcConnectionAccess() {
						@Override
						public Connection obtainConnection() throws SQLException {
							return connectionProvider.getConnection();
						}

						@Override
						public void releaseConnection(Connection connection) throws SQLException {
							connectionProvider.closeConnection( connection );
						}

						@Override
						public boolean supportsAggressiveRelease() {
							return connectionProvider.supportsAggressiveRelease();
						}
					},
					sqlStatementLogger
			);
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
			JdbcConnectionContext jdbcConnectionContext,
			Configuration hibernateConfiguration,
			ServiceRegistry serviceRegistry) {
		final String explicitDbName = hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_DB_NAME );
		final String explicitDbMajor = hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_DB_MAJOR_VERSION );
		final String explicitDbMinor = hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_DB_MINOR_VERSION );

		if ( StringHelper.isNotEmpty( explicitDbName ) ) {
			serviceRegistry.getService( DatabaseInfoDialectResolver.class ).resolve(
					new DatabaseInfoDialectResolver.DatabaseInfo() {
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
					}
			);
		}

		return buildDialect( hibernateConfiguration, serviceRegistry, jdbcConnectionContext );
	}

	private static Dialect buildDialect(
			Configuration hibernateConfiguration,
			ServiceRegistry serviceRegistry,
			JdbcConnectionContext jdbcConnectionContext) {
		// todo : a lot of copy/paste from the DialectFactory impl...
		final String dialectName = hibernateConfiguration.getProperty( org.hibernate.cfg.AvailableSettings.DIALECT );
		if ( dialectName != null ) {
			return constructDialect( dialectName, serviceRegistry );
		}
		else {
			return determineDialectBasedOnJdbcMetadata( jdbcConnectionContext, serviceRegistry );
		}
	}

	private static Dialect constructDialect(String dialectName, ServiceRegistry serviceRegistry) {
		final Dialect dialect;
		final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
		try {
			dialect = strategySelector.resolveStrategy( Dialect.class, dialectName );
			if ( dialect == null ) {
				throw new HibernateException( "Unable to construct requested dialect [" + dialectName + "]" );
			}
			return dialect;
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to construct requested dialect [" + dialectName+ "]", e );
		}
	}

	private static Dialect determineDialectBasedOnJdbcMetadata(
			JdbcConnectionContext jdbcConnectionContext,
			ServiceRegistry serviceRegistry) {
		DialectResolver dialectResolver = serviceRegistry.getService( DialectResolver.class );
		try {
			final DatabaseMetaData databaseMetaData = jdbcConnectionContext.getJdbcConnection().getMetaData();
			final Dialect dialect = dialectResolver.resolveDialect( databaseMetaData );

			if ( dialect == null ) {
				throw new HibernateException(
						"Unable to determine Dialect to use [name=" + databaseMetaData.getDatabaseProductName() +
								", majorVersion=" + databaseMetaData.getDatabaseMajorVersion() +
								"]; user must register resolver or explicitly set 'hibernate.dialect'"
				);
			}

			return dialect;
		}
		catch ( SQLException sqlException ) {
			throw new HibernateException(
					"Unable to access java.sql.DatabaseMetaData to determine appropriate Dialect to use",
					sqlException
			);
		}
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
		private final List<String> commands;

		private CreateSchemaCommandSource(Configuration hibernateConfiguration, Dialect dialect) {
			// NOTES:
			//		1) catalogs are currently not handled here at all
			//		2) schemas for sequences are not handled here at all
			//	Both of these are handle-able on the metamodel codebase

			final HashSet<String> schemas = new HashSet<String>();
//			final HashSet<String> catalogs = new HashSet<String>();

			final Iterator<Table> tables = hibernateConfiguration.getTableMappings();
			while ( tables.hasNext() ) {
				final Table table = tables.next();
//				catalogs.add( table.getCatalog() );
				schemas.add( table.getSchema() );
			}

//			final Iterator<IdentifierGenerator> generators = hibernateConfiguration.iterateGenerators( dialect );
//			while ( generators.hasNext() ) {
//				final IdentifierGenerator generator = generators.next();
//				if ( PersistentIdentifierGenerator.class.isInstance( generator ) ) {
////					catalogs.add( ( (PersistentIdentifierGenerator) generator ).getCatalog() );
//					schemas.add( ( (PersistentIdentifierGenerator) generator ).getSchema() );
//				}
//			}

//			if ( schemas.isEmpty() && catalogs.isEmpty() ) {
			if ( schemas.isEmpty() ) {
				commands = Collections.emptyList();
				return;
			}

			commands = new ArrayList<String>();

			for ( String schema : schemas ) {
				commands.add( dialect.getCreateSchemaCommand( schema ) );
			}

			// generate "create catalog" commands
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
		private final SqlScriptReader sourceReader;
		private final ImportSqlCommandExtractor scriptCommandExtractor;

		public ImportScriptSource(Object scriptSourceSetting, ImportSqlCommandExtractor scriptCommandExtractor) {
			this.scriptCommandExtractor = scriptCommandExtractor;

			if ( Reader.class.isInstance( scriptSourceSetting ) ) {
				sourceReader = new ReaderScriptSource( (Reader) scriptSourceSetting );
			}
			else {
				sourceReader = new FileScriptSource( scriptSourceSetting.toString() );
			}
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

}
