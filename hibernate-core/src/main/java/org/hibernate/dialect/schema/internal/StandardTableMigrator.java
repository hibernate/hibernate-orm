/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.TableMigrator;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.jboss.logging.Logger;

import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.getColumnDefinition;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.getFullColumnDeclaration;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingLength;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingType;

/// Standard table migrator which adds columns and applies supported type changes.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@Internal
public class StandardTableMigrator implements TableMigrator {
	private static final Logger LOG = Logger.getLogger( Table.class );

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
		return table.isView()
				? EMPTY_STRING_ARRAY
				: sqlAlterStrings( table, dialect, metadata, tableInfo, context ).toArray( EMPTY_STRING_ARRAY );
	}

	@Internal
	public static List<String> sqlAlterStrings(
			Table table,
			Dialect dialect,
			Metadata metadata,
			TableInformation tableInformation,
			SqlStringGenerationContext context) throws HibernateException {
		final var alterSupport = dialect.getAlterTableSupport();
		final String tableName = getTableName( table, context );
		final String alterTable = alterSupport.alterTableCommand(
				tableName,
				dialect.getIfExistsSupport().alterTablePlacement()
		) + ' ';
		final List<String> results = new ArrayList<>();

		for ( var column : table.getColumns() ) {
			final var columnInformation = tableInformation.getColumn(
					toIdentifier( column.getName(), column.isQuoted() )
			);
			if ( columnInformation == null ) {
				results.add(
						alterTable + alterSupport.addColumnPrefix() + ' '
								+ getFullColumnDeclaration( column, table, metadata, dialect, context )
								+ alterSupport.addColumnSuffix()
				);
			}
			else if ( !hasMatchingType( column, columnInformation, metadata, dialect )
					|| !hasMatchingLength( column, columnInformation, metadata, dialect ) ) {
				final String alterColumn = alterSupport.alterColumnType(
						new AlterColumnTypeRequest(
								column.getQuotedName( dialect ),
								column.getSqlType( metadata ),
								getColumnDefinition( column, metadata, dialect )
						)
				);
				if ( alterColumn != null ) {
					results.add( alterTable + alterColumn );
				}
			}
		}

		if ( results.isEmpty() ) {
			LOG.debugf( "No alter strings for table: %s", table.getQuotedName() );
		}
		return results;
	}

	private static String getTableName(Table table, SqlStringGenerationContext context) {
		return context.format( new QualifiedTableName(
				toIdentifier( table.getCatalog(), table.isCatalogQuoted() ),
				toIdentifier( table.getSchema(), table.isSchemaQuoted() ),
				table.getNameIdentifier()
		) );
	}
}
