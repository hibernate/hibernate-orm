/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import org.jboss.logging.Logger;

import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingType;

/**
 * Base implementation of {@link SchemaValidator}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaValidator implements SchemaValidator {
	private static final Logger log = Logger.getLogger( AbstractSchemaValidator.class );

	protected HibernateSchemaManagementTool tool;
	protected SchemaFilter schemaFilter;

	public AbstractSchemaValidator(
			HibernateSchemaManagementTool tool,
			SchemaFilter validateFilter) {
		this.tool = tool;
		this.schemaFilter = validateFilter == null ? DefaultSchemaFilter.INSTANCE : validateFilter;
	}

	@Override
	public void doValidation(
			Metadata metadata,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter) {
		SqlStringGenerationContext context = SqlStringGenerationContextImpl.fromConfigurationMap(
				tool.getServiceRegistry().requireService( JdbcEnvironment.class ),
				metadata.getDatabase(),
				options.getConfigurationValues()
		);
		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );

		final DdlTransactionIsolator isolator = tool.getDdlTransactionIsolator( jdbcContext );
		final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
				tool.getServiceRegistry(),
				isolator,
				context,
				tool
		);

		try {
			performValidation( metadata, databaseInformation, options, contributableInclusionFilter, jdbcContext.getDialect() );
		}
		finally {
			try {
				databaseInformation.cleanup();
			}
			catch (Exception e) {
				log.debug( "Problem releasing DatabaseInformation: " + e.getMessage() );
			}

			isolator.release();
		}
	}

	public void performValidation(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect) {
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				validateTables( metadata, databaseInformation, options, contributableInclusionFilter, dialect, namespace );
			}
		}

		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( Sequence sequence : namespace.getSequences() ) {
					if ( !schemaFilter.includeSequence( sequence ) ) {
						continue;
					}

					if ( ! contributableInclusionFilter.matches( sequence ) ) {
						continue;
					}

					final SequenceInformation sequenceInformation = databaseInformation.getSequenceInformation( sequence.getName() );
					validateSequence( sequence, sequenceInformation );
				}
			}
		}
	}

	protected abstract void validateTables(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect, Namespace namespace);

	protected void validateTable(
			Table table,
			TableInformation tableInformation,
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect) {
		if ( tableInformation == null ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: missing table [%s]",
							table.getQualifiedTableName().toString()
					)
			);
		}

		for ( Column column : table.getColumns() ) {
			final ColumnInformation existingColumn =
					//QUESTION: should this use metadata.getDatabase().toIdentifier( column.getQuotedName() )
					tableInformation.getColumn( toIdentifier( column.getQuotedName() ) );
			if ( existingColumn == null ) {
				throw new SchemaManagementException(
						String.format(
								"Schema-validation: missing column [%s] in table [%s]",
								column.getName(),
								table.getQualifiedTableName()
						)
				);
			}
			validateColumnType( table, column, existingColumn, metadata, options, dialect );
		}
	}

	protected void validateColumnType(
			Table table,
			Column column,
			ColumnInformation columnInformation,
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect) {
		if ( !hasMatchingType( column, columnInformation, metadata, dialect ) ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: wrong column type encountered in column [%s] in " +
									"table [%s]; found [%s (Types#%s)], but expecting [%s (Types#%s)]",
							column.getName(),
							table.getQualifiedTableName(),
							columnInformation.getTypeName().toLowerCase(Locale.ROOT),
							JdbcTypeNameMapper.getTypeName( columnInformation.getTypeCode() ),
							column.getSqlType( metadata ).toLowerCase(Locale.ROOT),
							JdbcTypeNameMapper.getTypeName( column.getSqlTypeCode( metadata ) )
					)
			);
		}
	}

	protected void validateSequence(Sequence sequence, SequenceInformation sequenceInformation) {
		if ( sequenceInformation == null ) {
			throw new SchemaManagementException(
					String.format( "Schema-validation: missing sequence [%s]", sequence.getName() )
			);
		}

		if ( sequenceInformation.getIncrementValue() != null && sequenceInformation.getIncrementValue().intValue() > 0
				&& sequence.getIncrementSize() != sequenceInformation.getIncrementValue().intValue() ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: sequence [%s] defined inconsistent increment-size; found [%s] but expecting [%s]",
							sequence.getName(),
							sequenceInformation.getIncrementValue(),
							sequence.getIncrementSize()
					)
			);
		}
	}
}
