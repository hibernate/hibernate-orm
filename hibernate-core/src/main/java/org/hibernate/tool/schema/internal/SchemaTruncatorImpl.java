/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaTruncator;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Gavin King
 */
public class SchemaTruncatorImpl implements SchemaTruncator {
	private static final Logger log = Logger.getLogger( SchemaTruncatorImpl.class );

	private final HibernateSchemaManagementTool tool;

	public SchemaTruncatorImpl(HibernateSchemaManagementTool tool, SchemaFilter truncatorFilter) {
		this.tool = tool;
	}

	@Override
	public void doTruncate(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor) {

		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
		final GenerationTarget[] targets = tool.buildGenerationTargets( targetDescriptor, jdbcContext, options.getConfigurationValues(),
				true ); //we need autocommit on for DB2 at least

		doTruncate( metadata, options, contributableInclusionFilter, jdbcContext.getDialect(), targets );
	}

	private void doTruncate(
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

		truncateFromMetadata( metadata, options, contributableInclusionFilter, dialect, formatter, targets );
	}

	private void truncateFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget... targets) {
		final Database database = metadata.getDatabase();
		SqlStringGenerationContext sqlStringGenerationContext = SqlStringGenerationContextImpl.fromConfigurationMap(
				metadata.getDatabase().getJdbcEnvironment(), database, options.getConfigurationValues() );


		final Set<String> exportIdentifiers = CollectionHelper.setOfSize( 50 );

		for ( Namespace namespace : database.getNamespaces() ) {

			if ( ! options.getSchemaFilter().includeNamespace( namespace ) ) {
				continue;
			}

			disableConstraints( namespace, metadata, formatter, options, sqlStringGenerationContext,
					contributableInclusionFilter, targets );
			applySqlString( dialect.getTableCleaner().getSqlBeforeString(), formatter, options,targets );

			// now it's safe to drop the tables
			List<Table> list = new ArrayList<>( namespace.getTables().size() );
			for ( Table table : namespace.getTables() ) {
				if ( ! table.isPhysicalTable() ) {
					continue;
				}
				if ( ! options.getSchemaFilter().includeTable( table ) ) {
					continue;
				}
				if ( ! contributableInclusionFilter.matches( table ) ) {
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				list.add( table );
			}
			applySqlStrings( dialect.getTableCleaner().getSqlTruncateStrings( list, metadata,
					sqlStringGenerationContext
			), formatter, options,targets );

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
//						sqlStringGenerationContext
//				), formatter, options, targets );
//			}

			applySqlString( dialect.getTableCleaner().getSqlAfterString(), formatter, options,targets );
			enableConstraints( namespace, metadata, formatter, options, sqlStringGenerationContext,
					contributableInclusionFilter, targets );
		}
	}

	private void disableConstraints(
			Namespace namespace,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			SqlStringGenerationContext sqlStringGenerationContext,
			ContributableMatcher contributableInclusionFilter,
			GenerationTarget... targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();

		for ( Table table : namespace.getTables() ) {
			if ( !table.isPhysicalTable() ) {
				continue;
			}
			if ( ! options.getSchemaFilter().includeTable( table ) ) {
				continue;
			}
			if ( ! contributableInclusionFilter.matches( table ) ) {
				continue;
			}

			for ( ForeignKey foreignKey : table.getForeignKeys().values() ) {
				if ( dialect.canDisableConstraints() ) {
					applySqlString(
							dialect.getTableCleaner().getSqlDisableConstraintString( foreignKey, metadata,
									sqlStringGenerationContext
							),
							formatter,
							options,
							targets
					);
				}
				else if ( !dialect.canBatchTruncate() ) {
					applySqlStrings(
							dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, metadata,
									sqlStringGenerationContext
							),
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
			SqlStringGenerationContext sqlStringGenerationContext,
			ContributableMatcher contributableInclusionFilter,
			GenerationTarget... targets) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();

		for ( Table table : namespace.getTables() ) {
			if ( !table.isPhysicalTable() ) {
				continue;
			}
			if ( ! options.getSchemaFilter().includeTable( table ) ) {
				continue;
			}
			if ( ! contributableInclusionFilter.matches( table ) ) {
				continue;
			}

			for ( ForeignKey foreignKey : table.getForeignKeys().values() ) {
				if ( dialect.canDisableConstraints() ) {
					applySqlString(
							dialect.getTableCleaner().getSqlEnableConstraintString( foreignKey, metadata,
									sqlStringGenerationContext
							),
							formatter,
							options,
							targets
					);
				}
				else if ( !dialect.canBatchTruncate() ) {
					applySqlStrings(
							dialect.getForeignKeyExporter().getSqlCreateStrings( foreignKey, metadata,
									sqlStringGenerationContext
							),
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

	private static void applySqlStrings(
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings == null ) {
			return;
		}

		for ( String sqlString : sqlStrings ) {
			applySqlString( sqlString, formatter, options, targets );
		}
	}

	private static void applySqlString(
			String sqlString,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( StringHelper.isEmpty( sqlString ) ) {
			return;
		}

		String sqlStringFormatted = formatter.format( sqlString );
		for ( GenerationTarget target : targets ) {
			try {
				target.accept( sqlStringFormatted );
			}
			catch (CommandAcceptanceException e) {
				options.getExceptionHandler().handleException( e );
			}
		}
	}
}
