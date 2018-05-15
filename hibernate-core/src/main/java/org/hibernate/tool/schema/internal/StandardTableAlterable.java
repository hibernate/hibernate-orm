/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Collection;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.naming.spi.QualifiedName;
import org.hibernate.naming.spi.QualifiedNameParser;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.Alterable;

import org.jboss.logging.Logger;

/**
 * @author Andrea Boriero
 */
public class StandardTableAlterable implements Alterable<ExportableTable> {
	private static final Logger log = Logger.getLogger( StandardTableAlterable.class );

	protected final Dialect dialect;

	public StandardTableAlterable(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlAlterStrings(ExportableTable table, TableInformation tableInfo, JdbcServices jdbcServices) {
		final QualifiedName tableName = new QualifiedNameParser.NameParts(
				table.getCatalogName(),
				table.getSchemaName(),
				table.getTableName()
		);

		final StringBuilder root = new StringBuilder( dialect.getAlterTableString( tableName.render() ) )
				.append( ' ' )
				.append( dialect.getAddColumnString() );
		final Collection<PhysicalColumn> physicalColumns = table.getPhysicalColumns();
		final String[] results = new String[physicalColumns.size()];
		int i = 0;
		for ( PhysicalColumn physicalColumn : physicalColumns ) {
			final ColumnInformation columnInfo = tableInfo.getColumn( physicalColumn.getName() );

			if ( columnInfo == null ) {
				// the column doesnt exist at all.
				StringBuilder alter = new StringBuilder( root.toString() )
						.append( ' ' )
						.append( physicalColumn.getName().render( dialect ) )
						.append( ' ' )
						.append( physicalColumn.getSqlTypeName() );

				final String defaultValue = physicalColumn.getDefaultValue();
				if ( defaultValue != null ) {
					alter.append( " default " ).append( defaultValue );
				}

				if ( physicalColumn.isNullable() ) {
					alter.append( dialect.getNullColumnString() );
				}
				else {
					alter.append( " not null" );
				}

				if ( physicalColumn.isUnique() ) {
					alter.append(
							dialect.getUniqueDelegate().getColumnDefinitionUniquenessFragment( physicalColumn )
					);
				}

				if ( physicalColumn.getCheckConstraint() != null && dialect.supportsColumnCheck() ) {
					alter.append( " check(" )
							.append( physicalColumn.getCheckConstraint() )
							.append( ")" );
				}

				final String columnComment = physicalColumn.getComment();
				if ( columnComment != null ) {
					alter.append( dialect.getColumnComment( columnComment ) );
				}

				alter.append( dialect.getAddColumnSuffixString() );

				results[i] = alter.toString();
				i++;
			}
		}

		if ( results.length == 0 ) {
			log.debugf( "No alter strings for table : %s", table.getTableName().render( dialect ) );
		}
		return results;
	}
}
