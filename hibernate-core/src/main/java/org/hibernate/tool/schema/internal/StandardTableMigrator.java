/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.tool.schema.internal.StandardTableExporter.appendColumn;

/**
 * A {@link TableMigrator} that only knows how to add new columns.
 *
 * @author Gavin King
 */
@Incubating
public class StandardTableMigrator implements TableMigrator {

	private static final Logger log = Logger.getLogger( Table.class );

	protected final Dialect dialect;

	public StandardTableMigrator(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlAlterStrings(
			Table table,
			Metadata metadata,
			TableInformation tableInfo,
			SqlStringGenerationContext sqlStringGenerationContext) {
		return sqlAlterStrings( table, dialect, metadata, tableInfo, sqlStringGenerationContext )
				.toArray( EMPTY_STRING_ARRAY );
	}

	@Internal
	public static List<String> sqlAlterStrings(
			Table table,
			Dialect dialect,
			Metadata metadata,
			TableInformation tableInfo,
			SqlStringGenerationContext sqlStringGenerationContext) throws HibernateException {

		final String tableName = sqlStringGenerationContext.format( new QualifiedTableName(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier() )
		);

		final StringBuilder root = new StringBuilder( dialect.getAlterTableString( tableName ) )
				.append( ' ' )
				.append( dialect.getAddColumnString() );

		final List<String> results = new ArrayList<>();

		for ( Column column : table.getColumns() ) {
			final ColumnInformation columnInfo = tableInfo.getColumn(
					Identifier.toIdentifier( column.getName(), column.isQuoted() )
			);
			if ( columnInfo == null ) {
				// the column doesn't exist at all.
				final StringBuilder alterTable = new StringBuilder( root.toString() ).append( ' ' );
				appendColumn( alterTable, column, table, metadata, dialect, sqlStringGenerationContext );
				alterTable.append( dialect.getAddColumnSuffixString() );
				results.add( alterTable.toString() );
			}
		}

		if ( results.isEmpty() ) {
			log.debugf( "No alter strings for table : %s", table.getQuotedName() );
		}

		return results;
	}
}
