/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.naming.QualifiedNameImpl;
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
	public String[] getSqlCreateStrings(Index index, JdbcServices jdbcServices) {
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
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
							index.getName()
					),
					jdbcEnvironment.getDialect()
			);
		}
		else {
			indexNameForCreation = index.getName().render( jdbcEnvironment.getDialect() );
		}
		final StringBuilder buf = new StringBuilder()
				.append( "create index " )
				.append( indexNameForCreation )
				.append( " on " )
				.append( tableName )
				.append( " (" );

		boolean first = true;
		for ( PhysicalColumn column : index.getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				buf.append( ", " );
			}
			buf.append( ( column.getName().render( jdbcEnvironment.getDialect() ) ) );
			String orderMap = index.getColumnOrderMap( column );
			if ( StringHelper.isNotEmpty( orderMap ) ) {
				buf.append( " " ).append( orderMap );
			}
		}
		buf.append( ")" );
		return new String[] { buf.toString() };
	}

	@Override
	public String[] getSqlDropStrings(Index index, JdbcServices jdbcServices) {
		if ( !dialect.dropConstraints() ) {
			return NO_COMMANDS;
		}

		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				index.getTable().getQualifiedTableName(),
				dialect
		);

		final String indexNameForCreation;
		if ( dialect.qualifyIndexName() ) {
			indexNameForCreation = StringHelper.qualify( tableName, index.getName().render( dialect ) );
		}
		else {
			indexNameForCreation = index.getName().render( dialect );
		}

		return new String[] { "drop index " + indexNameForCreation };
	}
}
