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
import static org.hibernate.tool.schema.internal.ColumnDefinitions.extractType;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.getColumnDefinition;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingLength;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingType;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.getFullColumnDeclaration;

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
			SqlStringGenerationContext context) {
		if ( table.isView() ) {
			// perhaps we could execute a 'create or replace view'
			return EMPTY_STRING_ARRAY;
		}
		else {
			return sqlAlterStrings( table, dialect, metadata, tableInfo, context )
					.toArray( EMPTY_STRING_ARRAY );
		}
	}

	@Internal
	public static List<String> sqlAlterStrings(
			Table table,
			Dialect dialect,
			Metadata metadata,
			TableInformation tableInformation,
			SqlStringGenerationContext context) throws HibernateException {

		final String tableName = context.format( new QualifiedTableName(
				Identifier.toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				Identifier.toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier() )
		);

		final String alterTable = dialect.getAlterTableString( tableName ) + ' ';

		final List<String> results = new ArrayList<>();

		for ( Column column : table.getColumns() ) {
			final ColumnInformation columnInformation = tableInformation.getColumn(
					Identifier.toIdentifier( column.getName(), column.isQuoted() )
			);
			if ( columnInformation == null ) {
				// the column doesn't exist at all.
				final String addColumn = dialect.getAddColumnString() + ' '
						+ getFullColumnDeclaration( column, table, metadata, dialect, context )
						+ dialect.getAddColumnSuffixString();
				results.add( alterTable + addColumn );
			}
			else if ( dialect.supportsAlterColumnType() ) {
				if ( !hasMatchingType( column, columnInformation, metadata, dialect )
						|| !hasMatchingLength( column, columnInformation, metadata, dialect ) ) {
					final String explicitColumnDefinition = column.getColumnDefinition();
					final String alterColumn = dialect.getAlterColumnTypeString(
							column.getQuotedName( dialect ),
							explicitColumnDefinition != null ? extractType( explicitColumnDefinition ) : column.getSqlType(metadata),
							getColumnDefinition( column, metadata, dialect )
					);
					results.add( alterTable + alterColumn );
				}
			}
		}

		if ( results.isEmpty() ) {
			log.debugf( "No alter strings for table : %s", table.getQuotedName() );
		}

		return results;
	}
}
