/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.GeneratorSynchronizer;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;


import static org.hibernate.tool.schema.internal.Helper.applySqlStrings;
import static org.hibernate.tool.schema.internal.Helper.createSqlStringGenerationContext;
import static org.hibernate.tool.schema.internal.Helper.interpretFormattingEnabled;

/**
 * Basic implementation of {@link GeneratorSynchronizer}.
 *
 * @author Gavin King
 */
public class GeneratorSynchronizerImpl implements GeneratorSynchronizer {
	private static final Logger LOG = Logger.getLogger( GeneratorSynchronizerImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;

	public GeneratorSynchronizerImpl(HibernateSchemaManagementTool tool, SchemaFilter truncatorFilter) {
		this.tool = tool;
		schemaFilter = truncatorFilter;
	}

	@Override
	public void doSynchronize(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			TargetDescriptor targetDescriptor) {
		final var configuration = options.getConfigurationValues();
		final var jdbcContext = tool.resolveJdbcContext( configuration );
		doSynchronize( metadata, options, contributableInclusionFilter, jdbcContext,
				tool.buildGenerationTargets( targetDescriptor, jdbcContext, configuration, true ) );
	}

	@Internal
	public void doSynchronize(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			JdbcContext dialect,
			GenerationTarget... targets) {
		for ( var target : targets ) {
			target.prepare();
		}

		try {
			performSync( metadata, options, contributableInclusionFilter, dialect, targets );
		}
		finally {
			for ( var target : targets ) {
				try {
					target.release();
				}
				catch (Exception e) {
					LOG.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
				}
			}
		}
	}

	private void performSync(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			JdbcContext dialect,
			GenerationTarget... targets) {
		final var formatter =
				interpretFormattingEnabled( options.getConfigurationValues() )
						? FormatStyle.DDL.getFormatter()
						: FormatStyle.NONE.getFormatter();
		syncFromMetadata( metadata, options, schemaFilter, contributableInclusionFilter, dialect, formatter, targets );
	}

	private void syncFromMetadata(
			Metadata metadata,
			ExecutionOptions options,
			SchemaFilter schemaFilter,
			ContributableMatcher contributableInclusionFilter,
			JdbcContext jdbcContext,
			Formatter formatter,
			GenerationTarget... targets) {
		try ( var isolator = tool.getDdlTransactionIsolator( jdbcContext ) ) {
			final var context = createSqlStringGenerationContext( options, metadata );
			for ( var namespace : metadata.getDatabase().getNamespaces() ) {
				if ( schemaFilter.includeNamespace( namespace ) ) {
					for ( var table : namespace.getTables() ) {
						if ( table.isPhysicalTable()
								&& schemaFilter.includeTable( table )
								&& contributableInclusionFilter.matches( table ) ) {
							for ( var command : table.getResyncCommands( context, isolator ) ) {
								applySqlStrings( command.initCommands(), formatter, options, targets );
							}
						}
					}
				}
			}
		}
	}
}
