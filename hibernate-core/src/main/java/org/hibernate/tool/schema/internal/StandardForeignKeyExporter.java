/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardForeignKeyExporter implements Exporter<ForeignKey> {
	private final Dialect dialect;

	public StandardForeignKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(ForeignKey foreignKey, JdbcEnvironment jdbcEnvironment) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}
		
		if ( ! foreignKey.createConstraint() ) {
			return NO_COMMANDS;
		}

		final int numberOfColumns = foreignKey.getColumnSpan();
		final String[] columnNames = new String[ numberOfColumns ];
		final String[] targetColumnNames = new String[ numberOfColumns ];

		int position = 0;
		for ( ForeignKey.ColumnMapping columnMapping : foreignKey.getColumnMappings() ) {
			columnNames[position] = columnMapping.getSourceColumn().getColumnName().getText( dialect );
			targetColumnNames[position] = columnMapping.getTargetColumn().getColumnName().getText( dialect );
			position++;
		}


		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) foreignKey.getSourceTable() ).getTableName()
		);
		final String targetTableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				((Table) foreignKey.getTargetTable()).getTableName()
		);

		final StringBuilder buffer = new StringBuilder( "alter table " )
				.append( sourceTableName )
				.append(
						dialect.getAddForeignKeyConstraintString(
								foreignKey.getName().getText( dialect ),
								columnNames,
								targetTableName,
								targetColumnNames,
								foreignKey.referencesPrimaryKey()
						)
				);

		// TODO: If a dialect does not support cascade-delete, can it support other actions? (HHH-6428)
		// For now, assume not.
		if ( dialect.supportsCascadeDelete() ) {
			if ( foreignKey.getDeleteRule() != ForeignKey.ReferentialAction.NO_ACTION ) {
				buffer.append( " on delete " ).append( foreignKey.getDeleteRule().getActionString() );
			}
			if ( foreignKey.getUpdateRule() != ForeignKey.ReferentialAction.NO_ACTION ) {
				buffer.append( " on update " ).append( foreignKey.getUpdateRule().getActionString() );
			}
		}

		return new String[] { buffer.toString() };
	}

	@Override
	public String[] getSqlDropStrings(ForeignKey foreignKey, JdbcEnvironment jdbcEnvironment) {
		if ( ! dialect.hasAlterTable() ) {
			return NO_COMMANDS;
		}
		
		if ( ! foreignKey.createConstraint() ) {
			return NO_COMMANDS;
		}

		final String sourceTableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) foreignKey.getSourceTable() ).getTableName()
		);
		return new String[] {
				"alter table " + sourceTableName + dialect.getDropForeignKeyString() + foreignKey.getName().getText( dialect )
		};
	}
}
