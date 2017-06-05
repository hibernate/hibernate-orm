/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.AssertionFailure;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardForeignKeyExporter implements Exporter<ForeignKey> {
	private static final String  COLUMN_MISMATCH_MSG = "Number of referencing columns [%s] did not " +
			"match number of referenced columns [%s] in foreign-key [%s] from [%s] to [%s]";

	private final Dialect dialect;

	public StandardForeignKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(ForeignKey foreignKey, RuntimeModelCreationContext modelCreationContext) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}
		
		if ( ! foreignKey.isExportationEnabled() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final int numberOfColumns = foreignKey.getColumnMappings().getColumnMappings().size();
		final String[] columnNames = new String[ numberOfColumns ];
		final String[] targetColumnNames = new String[ numberOfColumns ];

		final List<Column> targetItr;
		if ( foreignKey.isReferenceToPrimaryKey() ) {
			if ( numberOfColumns != foreignKey.getReferringTable().getPrimaryKey().getColumns().size() ) {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								COLUMN_MISMATCH_MSG,
								numberOfColumns,
								foreignKey.getReferringTable().getPrimaryKey().getColumns().size(),
								foreignKey.getName(),
								( (ExportableTable) foreignKey.getTargetTable() ).getTableName(),
								( (ExportableTable) foreignKey.getReferringTable() ).getTableName()
						)
				);
			}
			targetItr = foreignKey.getReferringTable().getPrimaryKey().getColumns();
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
								( (ExportableTable) foreignKey.getTargetTable() ).getTableName(),
								( (ExportableTable) foreignKey.getReferringTable() ).getTableName()
						)
				);
			}
			targetItr = foreignKey.getReferencedColumns().iterator();
		}

		int i = 0;
		final Iterator itr = foreignKey.getColumnIterator();
		while ( itr.hasNext() ) {
			columnNames[i] = ( (Column) itr.next() ).getQuotedName( dialect );
			targetColumnNames[i] = ( (Column) targetItr.next() ).getQuotedName( dialect );
			i++;
		}

		final JdbcEnvironment jdbcEnvironment = modelCreationContext.getDatabaseModel().getJdbcEnvironment();
		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) foreignKey.getTargetTable()).getQualifiedTableName(),
				dialect
		);
		final String targetTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) foreignKey.getReferringTable()).getQualifiedTableName(),
				dialect
		);

		final StringBuilder buffer = new StringBuilder( "alter table " )
				.append( sourceTableName )
				.append(
						foreignKey.getKeyDefinition() != null ?
								dialect.getAddForeignKeyConstraintString(
										foreignKey.getName(),
										foreignKey.getKeyDefinition()
								) :
								dialect.getAddForeignKeyConstraintString(
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
	public String[] getSqlDropStrings(ForeignKey foreignKey, RuntimeModelCreationContext modelCreationContext) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}

		if ( ! foreignKey.isCreationEnabled() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final JdbcEnvironment jdbcEnvironment = modelCreationContext.getDatabaseModel().getJdbcEnvironment();
		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				( (ExportableTable) foreignKey.getTargetTable()).getQualifiedTableName(),
				dialect
		);
		return new String[] {
				"alter table " + sourceTableName + dialect.getDropForeignKeyString() + foreignKey.getName()
		};
	}
}
