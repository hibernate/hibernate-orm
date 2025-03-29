/*
 * SPDX-License-Identifier: Apache-2.0
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
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaTruncator;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.tool.schema.internal.Helper.applySqlString;
import static org.hibernate.tool.schema.internal.Helper.applySqlStrings;
import static org.hibernate.tool.schema.internal.Helper.createSqlStringGenerationContext;

/**
 * Basic implementation of {@link SchemaTruncator}.
 *
 * @author Gavin King
 */
public class SchemaTruncatorImpl extends AbstractSchemaPopulator implements SchemaTruncator {
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

	@Override
	ClassLoaderService getClassLoaderService() {
		return tool.getServiceRegistry().getService( ClassLoaderService.class );
	}

}
