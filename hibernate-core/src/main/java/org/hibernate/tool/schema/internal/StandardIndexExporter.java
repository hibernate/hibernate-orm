/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

import java.util.Iterator;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
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
	public String[] getSqlCreateStrings(Index index, Metadata metadata) {
		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				index.getTable().getQualifiedTableName(),
				dialect
		);

		final String indexNameForCreation;
		if ( dialect.qualifyIndexName() ) {
			indexNameForCreation = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
					new QualifiedNameImpl(
							index.getTable().getQualifiedTableName().getCatalogName(),
							index.getTable().getQualifiedTableName().getSchemaName(),
							jdbcEnvironment.getIdentifierHelper().toIdentifier( index.getName() )
					),
					jdbcEnvironment.getDialect()
			);
		}
		else {
			indexNameForCreation = index.getName();
		}
		final StringBuilder buf = new StringBuilder()
				.append( "create index " )
				.append( indexNameForCreation )
				.append( " on " )
				.append( tableName )
				.append( " (" );

		boolean first = true;
		Iterator<Column> columnItr = index.getColumnIterator();
		while ( columnItr.hasNext() ) {
			final Column column = columnItr.next();
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( ( column.getQuotedName( dialect ) ) );
		}
		buf.append( ")" );
		return new String[] { buf.toString() };
	}

	@Override
	public String[] getSqlDropStrings(Index index, Metadata metadata) {
		if ( ! dialect.dropConstraints() ) {
			return NO_COMMANDS;
		}

		final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();
		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				index.getTable().getQualifiedTableName(),
				dialect
		);

		final String indexNameForCreation;
		if ( dialect.qualifyIndexName() ) {
			indexNameForCreation = StringHelper.qualify( tableName, index.getName() );
		}
		else {
			indexNameForCreation = index.getName();
		}

		return new String[] { "drop index " + indexNameForCreation };
	}
}
