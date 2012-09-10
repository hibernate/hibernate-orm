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
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardUniqueKeyExporter implements Exporter<UniqueKey> {
	private final Dialect dialect;

	public StandardUniqueKeyExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(UniqueKey uniqueKey, JdbcEnvironment jdbcEnvironment) {
		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			return NO_COMMANDS;
		}

		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) uniqueKey.getTable() ).getTableName()
		);

		final StringBuilder buffer = new StringBuilder( "alter table " )
				.append( tableName )
				.append( dialect.getAddUniqueConstraintString( uniqueKey.getExportedName() ) )
				.append( '(' );

		boolean foundNullableColumns = false;
		boolean first = true;
		for ( Column column : uniqueKey.getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buffer.append( ", " );
			}
			if ( !foundNullableColumns && column.isNullable() ) {
				foundNullableColumns = true;
			}
			buffer.append( column.getColumnName().getText( dialect ) );
		}
		buffer.append( ')' );

		return foundNullableColumns && !dialect.supportsNotNullUnique()
				? NO_COMMANDS
				: new String[] { buffer.toString() };
	}

	@Override
	public String[] getSqlDropStrings(UniqueKey uniqueKey, JdbcEnvironment jdbcEnvironment) {
		if ( isCreationVetoed( uniqueKey ) ) {
			return NO_COMMANDS;
		}

		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) uniqueKey.getTable() ).getTableName()
		);
		return new String[] {
				"alter table " + tableName + " drop constraint " + uniqueKey.getExportedName()
		};
	}

	private boolean isCreationVetoed(UniqueKey uniqueKey) {
		if ( dialect.supportsNotNullUnique() ) {
			return false;
		}

		for ( Column column : uniqueKey.getColumns() ) {
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}
}
