/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UniqueKey;

import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * A {@link UniqueDelegate} which uses {@code create unique index} commands when necessary.
 * <ul>
 * <li>DB2 does not allow unique constraints on nullable columns, but it does allow the creation
 *     of unique indexes instead, using {@code create unique index ... exclude null keys} or
 *     {@code create unique where not null index}, depending on flavor.
 * <li>SQL Server <em>does</em> allow unique constraints on nullable columns, but the semantics
 *     are that two null values are non-unique. So here we need to jump through hoops with the
 *     {@code create unique nonclustered index ... where ...} command.
 * </ul>
 *
 * @author Brett Meyer
 */
public class AlterTableUniqueIndexDelegate extends AlterTableUniqueDelegate {
	public AlterTableUniqueIndexDelegate(Dialect dialect ) {
		super( dialect );
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( uniqueKey.hasNullableColumn() ) {
			final Dialect dialect = context.getDialect();
			final String name = uniqueKey.getName();
			final String tableName = context.format( uniqueKey.getTable().getQualifiedTableName() );
			final List<Column> columns = uniqueKey.getColumns();
			final Map<Column, String> columnOrderMap = uniqueKey.getColumnOrderMap();
			final StringBuilder statement =
					new StringBuilder( dialect.getCreateIndexString( true ) )
							.append( " " )
							.append( dialect.qualifyIndexName() ? name : unqualify( name ) )
							.append( " on " )
							.append( tableName )
							.append( " (" );
			boolean first = true;
			for ( Column column : columns ) {
				if ( first ) {
					first = false;
				}
				else {
					statement.append(", ");
				}
				statement.append( column.getQuotedName(dialect) );
				if ( columnOrderMap.containsKey( column ) ) {
					statement.append( " " ).append( columnOrderMap.get( column ) );
				}
			}
			statement.append( ")" );
			statement.append( dialect.getCreateIndexTail( true, columns ) );
			return statement.toString();
		}
		else {
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( uniqueKey.hasNullableColumn() ) {
			final String tableName = context.format( uniqueKey.getTable().getQualifiedTableName() );
			return "drop index " + qualify( tableName, uniqueKey.getName() );
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}

}
