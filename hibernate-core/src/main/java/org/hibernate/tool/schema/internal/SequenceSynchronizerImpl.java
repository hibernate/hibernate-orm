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
import org.hibernate.tool.schema.spi.SchemaTruncator;
import org.hibernate.tool.schema.spi.SequenceSynchronizer;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;


import static org.hibernate.tool.schema.internal.Helper.applySqlStrings;
import static org.hibernate.tool.schema.internal.Helper.createSqlStringGenerationContext;
import static org.hibernate.tool.schema.internal.Helper.interpretFormattingEnabled;

/**
 * Basic implementation of {@link SchemaTruncator}.
 *
 * @author Gavin King
 */
public class SequenceSynchronizerImpl implements SequenceSynchronizer {
	private static final Logger LOG = Logger.getLogger( SequenceSynchronizerImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;

	public SequenceSynchronizerImpl(HibernateSchemaManagementTool tool, SchemaFilter truncatorFilter) {
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
		try ( var ddlTransactionIsolator = tool.getDdlTransactionIsolator( jdbcContext ) ) {
			final var connection = ddlTransactionIsolator.getIsolatedConnection();
			final var context = createSqlStringGenerationContext( options, metadata );
			for ( var namespace : metadata.getDatabase().getNamespaces() ) {
				if ( schemaFilter.includeNamespace( namespace ) ) {
					for ( var table : namespace.getTables() ) {
						for ( var command : table.getResyncCommands( context, connection ) ) {
							applySqlStrings( command.initCommands(), formatter, options, targets );
						}
					}
				}
			}
		}
	}
}
