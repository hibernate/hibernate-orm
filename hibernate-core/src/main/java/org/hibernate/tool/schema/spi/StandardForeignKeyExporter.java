/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.List;
import java.util.Locale;

import org.hibernate.AssertionFailure;
import org.hibernate.SPI;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyDropRequest;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Stock exporter for relational [ForeignKey] constraints.
///
/// Instantiate this class when standard foreign-key DDL is sufficient. Compose
/// or implement [Exporter] when a database must decorate or replace the complete
/// operation, and supply the result from [Dialect#getForeignKeyExporter()].
///
/// @see Dialect#getForeignKeyExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class StandardForeignKeyExporter implements Exporter<ForeignKey> {
	private static final String COLUMN_MISMATCH_MSG = "Number of referencing columns [%s] did not " +
			"match number of referenced columns [%s] in foreign-key [%s] from [%s] to [%s]";

	private final Dialect dialect;

	/// Create a standard foreign-key exporter owned by `dialect`.
	public StandardForeignKeyExporter(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must not be null" );
		}
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context) {
		final var support = dialect.getForeignKeySupport();
		if ( !support.supportsAlterTableConstraints()
				|| !foreignKey.isCreationEnabled()
				|| !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final int numberOfColumns = foreignKey.getColumnSpan();
		final List<Column> targetColumns = getTargetColumns( foreignKey, numberOfColumns );
		final List<Column> columns = foreignKey.getColumns();
		final List<String> columnNames = columns.stream().map( column -> column.getQuotedName( dialect ) ).toList();
		final List<String> targetColumnNames = targetColumns.stream()
				.map( column -> column.getQuotedName( dialect ) )
				.toList();

		final String sourceTableName = context.format( foreignKey.getTable().getQualifiedTableName() );
		final String targetTableName = context.format( foreignKey.getReferencedTable().getQualifiedTableName() );

		final var buffer =
				new StringBuilder( dialect.getAlterTableSupport().alterTableCommand(
						sourceTableName,
						dialect.getIfExistsSupport().alterTablePlacement()
				) )
						.append( ' ' )
						.append( support.renderAddConstraint( constraintRequest(
								foreignKey,
								metadata,
								columnNames,
								targetTableName,
								targetColumnNames
						) ) );

		final var onDeleteAction = foreignKey.getOnDeleteAction();
		if ( onDeleteAction != null && onDeleteAction != OnDeleteAction.NO_ACTION ) {
			if ( support.supportsOnDeleteAction( onDeleteAction ) ) {
				buffer.append( " on delete " ).append( onDeleteAction.toSqlString() );
			}
		}

		if ( isNotEmpty( foreignKey.getOptions() ) ) {
			buffer.append( " " ).append( foreignKey.getOptions() );
		}

		return new String[] { buffer.toString() };
	}

	private ForeignKeyConstraintRequest constraintRequest(
			ForeignKey foreignKey,
			Metadata metadata,
			List<String> columnNames,
			String targetTableName,
			List<String> targetColumnNames) {
		final String keyDefinition = foreignKey.getKeyDefinition();
		final String constraintName = quotedConstraintName( foreignKey, metadata );
		return keyDefinition != null
				? ForeignKeyConstraintRequest.explicit( constraintName, keyDefinition )
				: ForeignKeyConstraintRequest.structured(
						constraintName,
						columnNames,
						targetTableName,
						targetColumnNames,
						foreignKey.isReferenceToPrimaryKey()
				);
	}

	private String quotedConstraintName(ForeignKey foreignKey, Metadata metadata) {
		return metadata.getDatabase().getJdbcEnvironment().getIdentifierHelper()
				.toIdentifier( foreignKey.getName() ).render( dialect );
	}

	private static List<Column> getTargetColumns(ForeignKey foreignKey, int numberOfColumns) {
		if ( foreignKey.isReferenceToPrimaryKey() ) {
			final var primaryKey = foreignKey.getReferencedTable().getPrimaryKey();
			if ( numberOfColumns != primaryKey.getColumnSpan() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								primaryKey.getColumnSpan(),
								foreignKey.getName(),
								foreignKey.getTable().getName(),
								foreignKey.getReferencedTable().getName()
						)
				);
			}
			return primaryKey.getColumns();
		}
		else {
			final var referencedColumns = foreignKey.getReferencedColumns();
			if ( numberOfColumns != referencedColumns.size() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								referencedColumns.size(),
								foreignKey.getName(),
								foreignKey.getTable().getName(),
								foreignKey.getReferencedTable().getName()
						)
				);
			}
			return referencedColumns;
		}
	}

	@Override
	public String[] getSqlDropStrings(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context) {
		final var support = dialect.getForeignKeySupport();
		if ( !support.supportsAlterTableConstraints()
				|| !foreignKey.isCreationEnabled()
				|| !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}
		else {
			final String sourceTableName = context.format( foreignKey.getTable().getQualifiedTableName() );
			final var alterTable = dialect.getAlterTableSupport().alterTableCommand(
					sourceTableName,
					dialect.getIfExistsSupport().alterTablePlacement()
			);
			final var request = new ForeignKeyDropRequest(
					dialect.quote( foreignKey.getName() ),
					dialect.getIfExistsSupport().dropConstraintPlacement()
			);
			return new String[] { alterTable + " " + support.renderDropConstraint( request ) };
		}
	}

}
