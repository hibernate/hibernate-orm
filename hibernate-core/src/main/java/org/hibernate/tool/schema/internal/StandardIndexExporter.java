/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
