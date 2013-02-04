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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseInfoDialectResolver;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.SchemaGenAction;
import org.hibernate.jpa.SchemaGenSource;
import org.hibernate.jpa.SchemaGenTarget;
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

		final SchemaGenAction action = SchemaGenAction.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_ACTION )
		);
		if ( action == SchemaGenAction.NONE ) {
			// no generation requested
			return;
		}


		// Figure out the JDBC Connection context, if any ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final JdbcConnectionContext jdbcConnectionContext = determineAppropriateJdbcConnectionContext(
				hibernateConfiguration,
				serviceRegistry
		);

		final Dialect dialect = determineDialect( jdbcConnectionContext, hibernateConfiguration, serviceRegistry );

		final ImportSqlCommandExtractor scriptCommandExtractor = serviceRegistry.getService( ImportSqlCommandExtractor.class );


		// Next, determine the targets ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final List<GenerationTarget> generationTargetList = new ArrayList<GenerationTarget>();

		SchemaGenTarget target = SchemaGenTarget.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_TARGET )
		);

		// the default is dependent upon whether script targets were also specified...
		final Object createScriptTargetSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_TARGET
		);
		final Object dropScriptTargetSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_TARGET
		);

		if ( target == null ) {
			if ( createScriptTargetSetting != null && dropScriptTargetSetting != null ) {
				target = SchemaGenTarget.SCRIPTS;
			}
			else {
				target = SchemaGenTarget.DATABASE;
			}
		}

		if ( target == SchemaGenTarget.DATABASE || target == SchemaGenTarget.BOTH ) {
			generationTargetList.add( new DatabaseTarget( jdbcConnectionContext ) );
		}
		if ( target == SchemaGenTarget.SCRIPTS || target == SchemaGenTarget.BOTH ) {
			// both create and drop scripts are expected per JPA spec
			if ( createScriptTargetSetting == null ) {
				throw new IllegalArgumentException( "For schema generation creation script target missing" );
			}
			if ( dropScriptTargetSetting == null ) {
				throw new IllegalArgumentException( "For schema generation drop script target missing" );
			}
			generationTargetList.add( new ScriptsTarget( createScriptTargetSetting, dropScriptTargetSetting ) );
		}


		// determine sources ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final List<GenerationSource> generationSourceList = new ArrayList<GenerationSource>();

		SchemaGenSource source = SchemaGenSource.interpret(
				hibernateConfiguration.getProperty( AvailableSettings.SCHEMA_GEN_SOURCE )
		);

		// the default for sources is dependent upon whether script sources were specified...
		final Object createScriptSourceSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_CREATE_SCRIPT_SOURCE
		);
		final Object dropScriptSourceSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_DROP_SCRIPT_SOURCE
		);

		if ( source == null ) {
			if ( createScriptSourceSetting != null && dropScriptSourceSetting != null ) {
				source = SchemaGenSource.SCRIPTS;
			}
			else {
				source = SchemaGenSource.METADATA;
			}
		}

		final boolean createSchemas = ConfigurationHelper.getBoolean(
				AvailableSettings.SCHEMA_GEN_CREATE_SCHEMAS,
				hibernateConfiguration.getProperties(),
				false
		);
		if ( createSchemas ) {
			// todo : does it make sense to generate schema(s) defined in metadata if only script sources are to be used?
			generationSourceList.add( new CreateSchemaCommandSource( hibernateConfiguration, dialect ) );
		}

		if ( source == SchemaGenSource.METADATA ) {
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect ) );
		}
		else if ( source == SchemaGenSource.SCRIPTS ) {
			generationSourceList.add( new ScriptSource( createScriptSourceSetting, dropScriptSourceSetting, scriptCommandExtractor ) );
		}
		else if ( source == SchemaGenSource.METADATA_THEN_SCRIPTS ) {
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect ) );
			generationSourceList.add( new ScriptSource( createScriptSourceSetting, dropScriptSourceSetting, scriptCommandExtractor ) );
		}
		else if ( source == SchemaGenSource.SCRIPTS_THEN_METADATA ) {
			generationSourceList.add( new ScriptSource( createScriptSourceSetting, dropScriptSourceSetting, scriptCommandExtractor ) );
			generationSourceList.add( new MetadataSource( hibernateConfiguration, dialect ) );
		}

		final Object importScriptSetting = hibernateConfiguration.getProperties().get(
				AvailableSettings.SCHEMA_GEN_LOAD_SCRIPT_SOURCE
		);
		if ( importScriptSetting != null ) {
			generationSourceList.add( new ImportScriptSource( importScriptSetting, scriptCommandExtractor ) );
		}


		// do the generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		try {
			doGeneration( action, generationSourceList, generationTargetList );
		}
		finally {
			releaseResources( generationSourceList, generationTargetList, jdbcConnectionContext );
		}
	}


	private static JdbcConnectionContext determineAppropriateJdbcConnectionContext(
			Configuration hibernateConfiguration,
			ServiceRegistry serviceRegistry) {
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
					}
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
					}
			);
		}

		// otherwise, return a no-op impl
		return new JdbcConnectionContext( null ) {
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

		return serviceRegistry.getService( JdbcServices.class ).getDialect();
	}

	private static void doGeneration(
			SchemaGenAction action,
			List<GenerationSource> generationSourceList,
			List<GenerationTarget> generationTargetList) {

		for ( GenerationSource source : generationSourceList ) {
			if ( action.includesCreate() ) {
				final Iterable<String> createCommands = source.getCreateCommands();
				for ( GenerationTarget target : generationTargetList ) {
					target.acceptCreateCommands( createCommands );
				}
			}

			if ( action.includesDrop() ) {
				final Iterable<String> dropCommands = source.getDropCommands();
				for ( GenerationTarget target : generationTargetList ) {
					target.acceptDropCommands( dropCommands );
				}
			}
		}
	}

	private static void releaseResources(
			List<GenerationSource> generationSourceList,
			List<GenerationTarget> generationTargetList,
			JdbcConnectionContext jdbcConnectionContext) {
		for ( GenerationTarget target : generationTargetList ) {
			try {
				target.release();
			}
			catch (Exception e) {
				log.debug( "Problem releasing generation target : " + e.toString() );
			}
		}

		for ( GenerationSource source : generationSourceList ) {
			try {
				source.release();
			}
			catch (Exception e) {
				log.debug( "Problem releasing generation source : " + e.toString() );
			}
		}

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
		public Iterable<String> getCreateCommands() {
			return commands;
		}

		@Override
		public Iterable<String> getDropCommands() {
			return Collections.emptyList();
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
		public Iterable<String> getCreateCommands() {
			return sourceReader.read( scriptCommandExtractor );
		}

		@Override
		public Iterable<String> getDropCommands() {
			return Collections.emptyList();
		}

		@Override
		public void release() {
			sourceReader.release();
		}
	}
}
