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
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardIndexExporter implements Exporter<Index> {
	private final Dialect dialect;

	public StandardIndexExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Index index, JdbcEnvironment jdbcEnvironment) {
		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) index.getTable() ).getTableName()
		);
		StringBuilder buf = new StringBuilder()
				.append( "create index " )
				.append( dialect.qualifyIndexName()
						? index.getName().getText( dialect ) : index.getName().getUnqualifiedText( dialect ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );

		boolean first = true;
		for ( Column column : index.getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( ( column.getColumnName().getText( dialect ) ) );
		}
		buf.append( ")" );
		return new String[] { buf.toString() };
	}

	@Override
	public String[] getSqlDropStrings(Index index, JdbcEnvironment jdbcEnvironment) {
		if ( ! dialect.dropConstraints() ) {
			return NO_COMMANDS;
		}

		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) index.getTable() ).getTableName()
		);
		return new String[] {
				"drop index " + index.getName().getQualifiedText( tableName, dialect )
		};
	}
}
