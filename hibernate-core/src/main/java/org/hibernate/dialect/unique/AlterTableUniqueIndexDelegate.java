/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.UniqueKey;

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
 * <li>Spanner does not allow unique column definition, but it does allow the creation of unique
 * 	   indexes instead, using {@code create unique index ...}
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
		final var dialect = context.getDialect();
		if ( needsUniqueIndex( uniqueKey, dialect ) ) {
			final String constraintName = constraintName( uniqueKey, metadata );
			final var statement =
					new StringBuilder( dialect.getCreateIndexString( true ) )
							.append( " " )
							.append( dialect.qualifyIndexName()
									? constraintName
									: unqualify( constraintName ) )
							.append( " on " )
							.append( tableName( uniqueKey, context ) )
							.append( " (" );
			final var columns = uniqueKey.getColumns();
			final var columnOrderMap = uniqueKey.getColumnOrderMap();
			boolean first = true;
			for ( var column : columns ) {
				if ( first ) {
					first = false;
				}
				else {
					statement.append(", ");
				}
				statement.append( column.getQuotedName( dialect ) );
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
		if ( needsUniqueIndex( uniqueKey, context.getDialect() ) ) {
			final var statement = new StringBuilder().append( "drop index " );
			if ( dialect.supportsIfExistsBeforeConstraintName() ) {
				statement.append( "if exists " );
			}
			statement.append( tableName( uniqueKey, context ) ).append( '.' )
					.append( constraintName( uniqueKey, metadata ) );
			return statement.toString();
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}

	private boolean needsUniqueIndex(UniqueKey uniqueKey, Dialect dialect) {
		return uniqueKey.hasNullableColumn() || !dialect.supportsUniqueConstraints();
	}

}
