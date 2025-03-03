/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.schema.spi.Exporter;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * An {@link Exporter} for {@linkplain ForeignKey foreign key constraints}.
 *
 * @author Steve Ebersole
 */
public class StandardForeignKeyExporter implements Exporter<ForeignKey> {
	private static final String COLUMN_MISMATCH_MSG = "Number of referencing columns [%s] did not " +
			"match number of referenced columns [%s] in foreign-key [%s] from [%s] to [%s]";

	private final Dialect dialect;

	public StandardForeignKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context) {
		if ( !dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isCreationEnabled() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final int numberOfColumns = foreignKey.getColumnSpan();
		final String[] columnNames = new String[numberOfColumns];
		final String[] targetColumnNames = new String[numberOfColumns];

		final List<Column> targetColumns = getTargetColumns( foreignKey, numberOfColumns );
		final List<Column> columns = foreignKey.getColumns();
		for ( int i=0; i<columns.size() && i<targetColumns.size(); i++ ) {
			columnNames[i] = columns.get(i).getQuotedName( dialect );
			targetColumnNames[i] = targetColumns.get(i).getQuotedName( dialect );
		}

		final String sourceTableName = context.format( foreignKey.getTable().getQualifiedTableName() );
		final String targetTableName = context.format( foreignKey.getReferencedTable().getQualifiedTableName() );

		final StringBuilder buffer = new StringBuilder( dialect.getAlterTableString( sourceTableName ) )
				.append(
						foreignKey.getKeyDefinition() != null
								? dialect.getAddForeignKeyConstraintString(
										foreignKey.getName(),
										foreignKey.getKeyDefinition()
								)
								: dialect.getAddForeignKeyConstraintString(
										foreignKey.getName(),
										columnNames,
										targetTableName,
										targetColumnNames,
										foreignKey.isReferenceToPrimaryKey()
								)
				);

		if ( dialect.supportsCascadeDelete() ) {
			final OnDeleteAction onDeleteAction = foreignKey.getOnDeleteAction();
			if ( onDeleteAction != null && onDeleteAction != OnDeleteAction.NO_ACTION ) {
				buffer.append( " on delete " ).append( onDeleteAction.toSqlString() );
			}
		}

		if ( isNotEmpty( foreignKey.getOptions() ) ) {
			buffer.append( " " ).append( foreignKey.getOptions() );
		}

		return new String[] { buffer.toString() };
	}

	private static List<Column> getTargetColumns(ForeignKey foreignKey, int numberOfColumns) {
		if ( foreignKey.isReferenceToPrimaryKey() ) {
			if ( numberOfColumns != foreignKey.getReferencedTable().getPrimaryKey().getColumnSpan() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								foreignKey.getReferencedTable().getPrimaryKey().getColumnSpan(),
								foreignKey.getName(),
								foreignKey.getTable().getName(),
								foreignKey.getReferencedTable().getName()
						)
				);
			}
			return foreignKey.getReferencedTable().getPrimaryKey().getColumns();
		}
		else {
			if ( numberOfColumns != foreignKey.getReferencedColumns().size() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								foreignKey.getReferencedColumns().size(),
								foreignKey.getName(),
								foreignKey.getTable().getName(),
								foreignKey.getReferencedTable().getName()
						)
				);
			}
			return foreignKey.getReferencedColumns();
		}
	}

	@Override
	public String[] getSqlDropStrings(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context) {
		if ( !dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isCreationEnabled() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final String sourceTableName = context.format( foreignKey.getTable().getQualifiedTableName() );
		return new String[] { getSqlDropStrings( sourceTableName, foreignKey, dialect ) };
	}

	private String getSqlDropStrings(String tableName, ForeignKey foreignKey, Dialect dialect) {
		final StringBuilder buf = new StringBuilder( dialect.getAlterTableString( tableName ) );
		buf.append(" ").append( dialect.getDropForeignKeyString() ).append(" ");
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			buf.append( "if exists " );
		}
		buf.append( dialect.quote( foreignKey.getName() ) );
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			buf.append( " if exists" );
		}
		return buf.toString();
	}

}
