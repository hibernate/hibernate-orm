/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.AssertionFailure;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardForeignKeyExporter implements Exporter<ForeignKey> {
	private static final String COLUMN_MISMATCH_MSG = "Number of referencing columns [%s] did not " +
			"match number of referenced columns [%s] in foreign-key [%s] from [%s] to [%s]";

	private final Dialect dialect;

	public StandardForeignKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String[] getSqlCreateStrings(ForeignKey foreignKey, JdbcServices jdbcServices) {
		if ( !dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isExportationEnabled() ) {
			return NO_COMMANDS;
		}

		final int numberOfColumns = foreignKey.getColumnMappings().getColumnMappings().size();
		final String[] columnNames = new String[numberOfColumns];
		final String[] targetColumnNames = new String[numberOfColumns];

		List<PhysicalColumn> targetColumns;
		if ( foreignKey.isReferenceToPrimaryKey() ) {

			if ( foreignKey.getTargetTable().getPrimaryKey() == null
					|| foreignKey.getTargetTable().getPrimaryKey().getColumns().isEmpty() ) {
				throw new NotYetImplementedFor6Exception();
			}

			targetColumns = foreignKey.getTargetTable().getPrimaryKey().getColumns();
			if ( numberOfColumns != targetColumns.size() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								targetColumns.size(),
								foreignKey.getName(),
								( (ExportableTable) foreignKey.getReferringTable() ).getTableName(),
								( (ExportableTable) foreignKey.getTargetTable() ).getTableName()
						)
				);
			}
		}
		else {
			targetColumns = new ArrayList<>();
			for ( Column column : foreignKey.getColumnMappings().getTargetColumns() ) {
				if ( !PhysicalColumn.class.isInstance( column ) ) {
					// Fks with non-PhysicalColumn columns are not exportable
					return NO_COMMANDS;
				}

				targetColumns.add( (PhysicalColumn) column );
			}

			if ( numberOfColumns != targetColumns.size() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								targetColumns.size(),
								foreignKey.getName(),
								( (ExportableTable) foreignKey.getReferringTable() ).getTableName(),
								( (ExportableTable) foreignKey.getTargetTable() ).getTableName()
						)
				);
			}
		}

		int i = 0;
		for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : foreignKey.getColumnMappings().getColumnMappings() ) {
			assert columnMapping.getReferringColumn() != null;
			assert columnMapping.getTargetColumn() != null;

			columnNames[i] = ( (PhysicalColumn) columnMapping.getReferringColumn() ).getName().render( dialect );
			targetColumnNames[i] = ( (PhysicalColumn) columnMapping.getTargetColumn() ).getName().render( dialect );

			i++;
		}

		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) foreignKey.getReferringTable() ).getQualifiedTableName(),
				dialect
		);
		final String targetTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) foreignKey.getTargetTable() ).getQualifiedTableName(),
				dialect
		);

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
			if ( foreignKey.isCascadeDeleteEnabled() ) {
				buffer.append( " on delete cascade" );
			}
		}

		return new String[] { buffer.toString() };
	}

	@Override
	public String[] getSqlDropStrings(ForeignKey foreignKey, JdbcServices jdbcServices) {
		if ( !dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isExportationEnabled() ) {
			return NO_COMMANDS;
		}

		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) foreignKey.getReferringTable()).getQualifiedTableName(),
				dialect
		);
		return new String[] {
				getSqlDropStrings( sourceTableName, foreignKey, dialect )
		};
	}

	private String getSqlDropStrings(String tableName, ForeignKey foreignKey, Dialect dialect) {
		final StringBuilder buf = new StringBuilder( dialect.getAlterTableString( tableName ) );
		buf.append( dialect.getDropForeignKeyString() );
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
