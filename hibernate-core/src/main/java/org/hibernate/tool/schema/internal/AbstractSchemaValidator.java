/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;


import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import org.jboss.logging.Logger;

import java.util.Objects;

import static java.util.Locale.ROOT;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.cfg.SchemaToolingSettings.INDEX_VALIDATION;
import static org.hibernate.cfg.SchemaToolingSettings.UNIQUE_KEY_VALIDATION;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingType;
import static org.hibernate.tool.schema.internal.Helper.buildDatabaseInformation;

/**
 * Base implementation of {@link SchemaValidator}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaValidator implements SchemaValidator {
	private static final Logger LOG = Logger.getLogger( AbstractSchemaValidator.class );

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
		final var context =
				SqlStringGenerationContextImpl.fromConfigurationMap(
						tool.getServiceRegistry().requireService( JdbcEnvironment.class ),
						metadata.getDatabase(),
						options.getConfigurationValues()
				);
		final var jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );
		try ( var isolator = tool.getDdlTransactionIsolator( jdbcContext ) ) {
			final var databaseInformation =
					buildDatabaseInformation( isolator, context, tool );
			try {
				performValidation( metadata, databaseInformation, options, contributableInclusionFilter, jdbcContext.getDialect() );
			}
			finally {
				try {
					databaseInformation.cleanup();
				}
				catch (Exception e) {
					LOG.debug( "Problem releasing DatabaseInformation: " + e.getMessage() );
				}
			}
		}
	}

	public void performValidation(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			ContributableMatcher contributableInclusionFilter,
			Dialect dialect) {
		final var namespaces = metadata.getDatabase().getNamespaces();
		for ( var namespace : namespaces ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				validateTables( metadata, databaseInformation, options, contributableInclusionFilter, dialect, namespace );
			}
		}
		for ( var namespace : namespaces ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( var sequence : namespace.getSequences() ) {
					if ( schemaFilter.includeSequence( sequence )
							&& contributableInclusionFilter.matches( sequence ) ) {
						validateSequence( sequence,
								databaseInformation.getSequenceInformation( sequence.getName() ) );
					}
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
							"Schema validation: missing table [%s]",
							table.getQualifiedTableName().toString()
					)
			);
		}

		for ( var column : table.getColumns() ) {
			final var existingColumn =
					//QUESTION: should this use metadata.getDatabase().toIdentifier( column.getQuotedName() )
					tableInformation.getColumn( toIdentifier( column.getQuotedName() ) );
			if ( existingColumn == null ) {
				throw new SchemaManagementException(
						String.format(
								"Schema validation: missing column [%s] in table [%s]",
								column.getName(),
								table.getQualifiedTableName()
						)
				);
			}
			validateColumnType( table, column, existingColumn, metadata, dialect );
			validateColumnNullability( table, column, existingColumn );
		}

		validateIndexes( table, tableInformation, metadata, options, dialect );
		validateUniqueKeys( table, tableInformation, metadata, options, dialect );
	}

	protected void validateColumnType(
			Table table,
			Column column,
			ColumnInformation columnInformation,
			Metadata metadata,
			Dialect dialect) {
		if ( !hasMatchingType( column, columnInformation, metadata, dialect ) ) {
			throw new SchemaManagementException(
					String.format(
							"Schema validation: wrong column type encountered in column [%s] in " +
									"table [%s]; found [%s (Types#%s)], but expecting [%s (Types#%s)]",
							column.getName(),
							table.getQualifiedTableName(),
							columnInformation.getTypeName().toLowerCase( ROOT),
							JdbcTypeNameMapper.getTypeName( columnInformation.getTypeCode() ),
							column.getSqlType( metadata ).toLowerCase( ROOT),
							JdbcTypeNameMapper.getTypeName( column.getSqlTypeCode( metadata ) )
					)
			);
		}
	}

	private void validateColumnNullability(Table table, Column column, ColumnInformation existingColumn) {
		if ( existingColumn.getNullable() == Boolean.FALSE ) {
			// the existing schema column is defined as not-nullable
			if ( column.isNullable() ) {
				// but it is mapped in the model as nullable
				throw new SchemaManagementException(
						String.format(
								"Schema validation: column defined as not-null in the database, but nullable in model - [%s] in table [%s]",
								column.getName(),
								table.getQualifiedTableName()
						)
				);
			}
		}
	}

	private void validateIndexes(
			Table table,
			TableInformation tableInformation,
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect) {
		var validationType = ConstraintValidationType.interpret( INDEX_VALIDATION, options.getConfigurationValues() );

		table.getIndexes().forEach((rawName,index) -> {
			assert StringHelper.isNotEmpty( rawName );
			assert Objects.equals( rawName, index.getName() );
			if ( validationType == ConstraintValidationType.NONE ) {
				return;
			}
			else if ( validationType == ConstraintValidationType.NAMED ) {
				if ( rawName.startsWith( "IDX" ) ) {
					// this is not a great check as the user could very well
					// have explicitly chosen a name that starts with this as well,
					// but...
					return;
				}
			}

			var name = metadata.getDatabase().toIdentifier( rawName );
			final IndexInformation indexInformation = tableInformation.getIndex( name );

			if ( indexInformation == null ) {
				throw new SchemaManagementException(
						String.format(
								ROOT,
								"Missing index named `%s` on table `%s`",
								name.render( dialect ),
								tableInformation.getName().render()
						)
				);
			}

			var indicesMatch = true;
			assert index.getSelectables().size() == index.getColumnSpan();
			if ( index.getColumnSpan() != indexInformation.getIndexedColumns().size() ) {
				indicesMatch = false;
			}
			else {
				for ( int i = 0; i < index.getSelectables().size(); i++ ) {
					final Selectable column = index.getSelectables().get( i );
					final ColumnInformation columnInfo = indexInformation.getIndexedColumns().get( i );
					if ( !column.getText().equals( columnInfo.getColumnIdentifier().getText() ) ) {
						indicesMatch = false;
						break;
					}
				}
			}

			if ( !indicesMatch ) {
				throw new SchemaManagementException(
						String.format(
								ROOT,
								"Index mismatch - `%s` on table `%s`",
								name.render( dialect ),
								tableInformation.getName().render()
						)
				);
			}
		} );
	}

	private void validateUniqueKeys(
			Table table,
			TableInformation tableInformation,
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect) {
		var validationType = ConstraintValidationType.interpret( UNIQUE_KEY_VALIDATION, options.getConfigurationValues() );

		table.getUniqueKeys().forEach( (rawName, uk) -> {
			assert StringHelper.isNotEmpty( rawName );
			assert Objects.equals( rawName, uk.getName() );
			if ( validationType == ConstraintValidationType.NONE ) {
				return;
			}
			else if ( validationType == ConstraintValidationType.NAMED ) {
				if ( rawName.startsWith( "UK" ) ) {
					// this is not a great check as the user could very well
					// have explicitly chosen a name that starts with this as well,
					// but...
					return;
				}
			}

			var name = metadata.getDatabase().toIdentifier( rawName );
			final IndexInformation ukInfo = tableInformation.getIndex( name );

			if ( ukInfo == null ) {
				throw new SchemaManagementException(
						String.format(
								ROOT,
								"Missing unique constraint named `%s` on table `%s`",
								name.render( dialect ),
								tableInformation.getName().render()
						)
				);
			}

			var matches = true;
			assert uk.getColumns().size() == uk.getColumnSpan();
			if ( uk.getColumnSpan() != ukInfo.getIndexedColumns().size() ) {
				matches = false;
			}
			else {
				for ( int i = 0; i < uk.getColumns().size(); i++ ) {
					final Column column = uk.getColumns().get( i );
					final ColumnInformation columnInfo = ukInfo.getIndexedColumns().get( i );
					if ( !column.getName().equals( columnInfo.getColumnIdentifier().getText() ) ) {
						matches = false;
						break;
					}
				}
			}

			if ( !matches ) {
				throw new SchemaManagementException(
						String.format(
								ROOT,
								"Unique-key mismatch - `%s` on table `%s`",
								name.render( dialect ),
								tableInformation.getName().render()
						)
				);
			}
		} );
	}

	protected void validateSequence(Sequence sequence, SequenceInformation sequenceInformation) {
		if ( sequenceInformation == null ) {
			throw new SchemaManagementException(
					String.format( "Schema validation: missing sequence [%s]", sequence.getName() )
			);
		}

		final Number incrementValue = sequenceInformation.getIncrementValue();
		if ( incrementValue != null && incrementValue.intValue() > 0
				&& sequence.getIncrementSize() != incrementValue.intValue() ) {
			throw new SchemaManagementException(
					String.format(
							"Schema validation: sequence [%s] defined inconsistent increment-size; found [%s] but expecting [%s]",
							sequence.getName(),
							incrementValue,
							sequence.getIncrementSize()
					)
			);
		}
	}
}
