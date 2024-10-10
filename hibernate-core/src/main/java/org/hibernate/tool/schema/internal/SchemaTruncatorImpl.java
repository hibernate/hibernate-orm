/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromUrl;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputNonExistentImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaTruncator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_CHARSET_NAME;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.tool.schema.internal.Helper.applyScript;
import static org.hibernate.tool.schema.internal.Helper.applySqlString;
import static org.hibernate.tool.schema.internal.Helper.applySqlStrings;
import static org.hibernate.tool.schema.internal.Helper.createSqlStringGenerationContext;
import static org.hibernate.tool.schema.internal.Helper.interpretScriptSourceSetting;

/**
 * Basic implementation of {@link SchemaTruncator}.
 *
 * @author Gavin King
 */
public class SchemaTruncatorImpl implements SchemaTruncator {
	private static final Logger log = Logger.getLogger( SchemaTruncatorImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;

	public SchemaTruncatorImpl(HibernateSchemaManagementTool tool, SchemaFilter truncatorFilter) {
		this.tool = tool;
		schemaFilter = truncatorFilter;
	}

	@Override
	public void doTruncate(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor) {

		final Map<String, Object> configurationValues = options.getConfigurationValues();
		final JdbcContext jdbcContext = tool.resolveJdbcContext(configurationValues);
		final GenerationTarget[] targets =
				tool.buildGenerationTargets( targetDescriptor, jdbcContext, configurationValues,
						true ); //we need autocommit on for DB2 at least

		doTruncate( metadata, options, contributableInclusionFilter, jdbcContext.getDialect(), targets );
	}

	@Internal
	public void doTruncate(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performTruncate( metadata, options, contributableInclusionFilter, dialect, targets );
		}
		finally {
			for ( GenerationTarget target : targets ) {
				try {
					target.release();
				}
				catch (Exception e) {
					log.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
				}
			}
		}
	}

	private void performTruncate(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			GenerationTarget... targets) {
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		truncateFromMetadata( metadata, options, schemaFilter, contributableInclusionFilter, dialect, formatter, targets );
	}

	private void truncateFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		final Database database = metadata.getDatabase();
		SqlStringGenerationContext context = createSqlStringGenerationContext( options, metadata );

		final Set<String> exportIdentifiers = CollectionHelper.setOfSize( 50 );

		for ( Namespace namespace : database.getNamespaces() ) {

			if ( ! schemaFilter.includeNamespace( namespace ) ) {
				continue;
			}

			disableConstraints( namespace, metadata, formatter, options, schemaFilter, context,
					contributableInclusionFilter, targets );
			applySqlString( dialect.getTableCleaner().getSqlBeforeString(), formatter, options,targets );

			// now it's safe to drop the tables
			List<Table> list = new ArrayList<>( namespace.getTables().size() );
			for ( Table table : namespace.getTables() ) {
				if ( ! table.isPhysicalTable() ) {
					continue;
				}
				if ( ! schemaFilter.includeTable( table ) ) {
					continue;
				}
				if ( ! contributableInclusionFilter.matches( table ) ) {
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				list.add( table );
			}
			applySqlStrings(
					dialect.getTableCleaner().getSqlTruncateStrings( list, metadata, context),
					formatter, options,targets
			);

			//TODO: reset the sequences?
//			for ( Sequence sequence : namespace.getSequences() ) {
//				if ( ! options.getSchemaFilter().includeSequence( sequence ) ) {
//					continue;
//				}
//				if ( ! contributableInclusionFilter.matches( sequence ) ) {
//					continue;
//				}
//				checkExportIdentifier( sequence, exportIdentifiers );
//
//				applySqlStrings( dialect.getSequenceExporter().getSqlDropStrings( sequence, metadata,
//						context
//				), formatter, options, targets );
//			}

			applySqlString( dialect.getTableCleaner().getSqlAfterString(), formatter, options,targets );
			enableConstraints( namespace, metadata, formatter, options, schemaFilter, context,
					contributableInclusionFilter, targets );
		}

		final SqlScriptCommandExtractor commandExtractor =
				tool.getServiceRegistry().getService( SqlScriptCommandExtractor.class );
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		applyImportSources( options, commandExtractor, format, dialect, targets );
	}

	private void disableConstraints(
			Namespace namespace,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			SqlStringGenerationContext context,
			ContributableMatcher contributableInclusionFilter,
			GenerationTarget... targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();

		for ( Table table : namespace.getTables() ) {
			if ( !table.isPhysicalTable() ) {
				continue;
			}
			if ( ! schemaFilter.includeTable( table ) ) {
				continue;
			}
			if ( ! contributableInclusionFilter.matches( table ) ) {
				continue;
			}

			for ( ForeignKey foreignKey : table.getForeignKeys().values() ) {
				if ( dialect.canDisableConstraints() ) {
					applySqlString(
							dialect.getTableCleaner().getSqlDisableConstraintString( foreignKey, metadata, context ),
							formatter,
							options,
							targets
					);
				}
				else if ( !dialect.canBatchTruncate() ) {
					applySqlStrings(
							dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, metadata, context ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private void enableConstraints(
			Namespace namespace,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			SqlStringGenerationContext context,
			ContributableMatcher contributableInclusionFilter,
			GenerationTarget... targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();

		for ( Table table : namespace.getTables() ) {
			if ( !table.isPhysicalTable() ) {
				continue;
			}
			if ( ! schemaFilter.includeTable( table ) ) {
				continue;
			}
			if ( ! contributableInclusionFilter.matches( table ) ) {
				continue;
			}

			for ( ForeignKey foreignKey : table.getForeignKeys().values() ) {
				if ( dialect.canDisableConstraints() ) {
					applySqlString(
							dialect.getTableCleaner().getSqlEnableConstraintString( foreignKey, metadata, context ),
							formatter,
							options,
							targets
					);
				}
				else if ( !dialect.canBatchTruncate() ) {
					applySqlStrings(
							dialect.getForeignKeyExporter().getSqlCreateStrings( foreignKey, metadata, context ),
							formatter,
							options,
							targets
					);
				}
			}
		}
	}

	private static void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException( "SQL strings added more than once for: " + exportIdentifier );
		}
		exportIdentifiers.add( exportIdentifier );
	}

	//Woooooo, massive copy/paste from SchemaCreatorImpl!

	private void applyImportSources(
			ExecutionOptions options,
			SqlScriptCommandExtractor commandExtractor,
			boolean format,
			Dialect dialect,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry = tool.getServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		// I have had problems applying the formatter to these imported statements.
		// and legacy SchemaExport did not format them, so doing same here
		//final Formatter formatter = format ? DDLFormatterImpl.INSTANCE : FormatStyle.NONE.getFormatter();
		final Formatter formatter = FormatStyle.NONE.getFormatter();

		Object importScriptSetting = options.getConfigurationValues().get( HBM2DDL_LOAD_SCRIPT_SOURCE );
		if ( importScriptSetting == null ) {
			importScriptSetting = options.getConfigurationValues().get( JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE );
		}
		String charsetName = (String) options.getConfigurationValues().get( HBM2DDL_CHARSET_NAME );

		if ( importScriptSetting != null ) {
			final ScriptSourceInput importScriptInput = interpretScriptSourceSetting( importScriptSetting, classLoaderService, charsetName );
			applyScript( options, commandExtractor, dialect, importScriptInput, formatter, targets );
		}

		final String importFiles = ConfigurationHelper.getString(
				AvailableSettings.HBM2DDL_IMPORT_FILES,
				options.getConfigurationValues(),
				SchemaCreatorImpl.DEFAULT_IMPORT_FILE
		);

		for ( String currentFile : StringHelper.split( ",", importFiles ) ) {
			final String resourceName = currentFile.trim();
			if ( resourceName.isEmpty() ) {
				//skip empty resource names
				continue;
			}
			final ScriptSourceInput importScriptInput = interpretLegacyImportScriptSetting( resourceName, classLoaderService, charsetName );
			applyScript( options, commandExtractor, dialect, importScriptInput, formatter, targets );
		}
	}

	private ScriptSourceInput interpretLegacyImportScriptSetting(
			String resourceName,
			ClassLoaderService classLoaderService,
			String charsetName) {
		try {
			final URL resourceUrl = classLoaderService.locateResource( resourceName );
			if ( resourceUrl == null ) {
				return ScriptSourceInputNonExistentImpl.INSTANCE;
			}
			else {
				return new ScriptSourceInputFromUrl( resourceUrl, charsetName );
			}
		}
		catch (Exception e) {
			throw new SchemaManagementException( "Error resolving legacy import resource : " + resourceName, e );
		}
	}

	/**
	 * Intended for use from tests
	 */
	@Internal
	public void doTruncate(
			Metadata metadata,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		final ServiceRegistry serviceRegistry =
				( (MetadataImplementor) metadata ).getMetadataBuildingOptions()
						.getServiceRegistry();
		doTruncate(
				metadata,
				serviceRegistry,
				serviceRegistry.requireService( ConfigurationService.class ).getSettings(),
				manageNamespaces,
				targets
		);
	}

	/**
	 * Intended for use from tests
	 */
	@Internal
	public void doTruncate(
			Metadata metadata,
			final ServiceRegistry serviceRegistry,
			final Map<String,Object> settings,
			final boolean manageNamespaces,
			GenerationTarget... targets) {
		doTruncate(
				metadata,
				new ExecutionOptions() {
					@Override
					public boolean shouldManageNamespaces() {
						return manageNamespaces;
					}

					@Override
					public Map<String,Object> getConfigurationValues() {
						return settings;
					}

					@Override
					public ExceptionHandler getExceptionHandler() {
						return ExceptionHandlerLoggedImpl.INSTANCE;
					}
				},
				(contributed) -> true,
				serviceRegistry.requireService( JdbcEnvironment.class ).getDialect(),
				targets
		);
	}
}
