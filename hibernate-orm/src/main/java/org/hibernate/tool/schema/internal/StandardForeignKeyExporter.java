/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Iterator;
import java.util.Locale;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
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
	public String[] getSqlCreateStrings(ForeignKey foreignKey, Metadata metadata) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}
		
		if ( ! foreignKey.isCreationEnabled() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final int numberOfColumns = foreignKey.getColumnSpan();
		final String[] columnNames = new String[ numberOfColumns ];
		final String[] targetColumnNames = new String[ numberOfColumns ];

		final Iterator targetItr;
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
			targetItr = foreignKey.getReferencedTable().getPrimaryKey().getColumnIterator();
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
			targetItr = foreignKey.getReferencedColumns().iterator();
		}

		int i = 0;
		final Iterator itr = foreignKey.getColumnIterator();
		while ( itr.hasNext() ) {
			columnNames[i] = ( (Column) itr.next() ).getQuotedName( dialect );
			targetColumnNames[i] = ( (Column) targetItr.next() ).getQuotedName( dialect );
			i++;
		}

		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				foreignKey.getTable().getQualifiedTableName(),
				dialect
		);
		final String targetTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				foreignKey.getReferencedTable().getQualifiedTableName(),
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
	public String[] getSqlDropStrings(ForeignKey foreignKey, Metadata metadata) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}

		if ( ! foreignKey.isCreationEnabled() ) {
			return NO_COMMANDS;
		}

		if ( !foreignKey.isPhysicalConstraint() ) {
			return NO_COMMANDS;
		}

		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				foreignKey.getTable().getQualifiedTableName(),
				dialect
		);
		return new String[] {
				"alter table " + sourceTableName + dialect.getDropForeignKeyString() + foreignKey.getName()
		};
	}
}
