/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Informix unique-key decoration which places constraint names last.
///
/// @author Brett Meyer
/// @author Steve Ebersole
public class InformixUniqueDelegate extends DelegatingUniqueDelegate {
	private final Dialect dialect;

	public InformixUniqueDelegate(Dialect dialect) {
		super( UniqueDelegates.skipNullable( dialect ) );
		this.dialect = dialect;
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
		if ( context.isMigration() ) {
			return "";
		}
		final var fragment = new StringBuilder();
		for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
			if ( !uniqueKey.hasNullableColumn() ) {
				fragment.append( ", " ).append( uniqueConstraintSql( uniqueKey ) );
				if ( uniqueKey.isNameExplicit() ) {
					fragment.append( " constraint " ).append( uniqueKey.getName() );
				}
			}
		}
		return fragment.toString();
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(
			UniqueKey uniqueKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		if ( uniqueKey.hasNullableColumn() || !context.isMigration() ) {
			return "";
		}
		final String tableName = context.format( uniqueKey.getTable().getQualifiedTableName() );
		final String constraintName = dialect.quote( uniqueKey.getName() );
		return dialect.getAlterTableSupport().alterTableCommand(
				tableName,
				dialect.getIfExistsSupport().alterTablePlacement()
		)
				+ " add constraint " + uniqueConstraintSql( uniqueKey ) + " constraint " + constraintName;
	}

	private String uniqueConstraintSql(UniqueKey uniqueKey) {
		final var fragment = new StringBuilder( "unique" );
		if ( uniqueKey.isNullsNotDistinct() && supportsNullsNotDistinct() ) {
			fragment.append( " nulls not distinct" );
		}
		fragment.append( " (" );
		boolean first = true;
		for ( var column : uniqueKey.getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				fragment.append( ", " );
			}
			fragment.append( column.getQuotedName( dialect ) );
			final String order = uniqueKey.getColumnOrderMap().get( column );
			if ( order != null ) {
				fragment.append( ' ' ).append( order );
			}
		}
		fragment.append( ')' );
		if ( isNotEmpty( uniqueKey.getOptions() ) ) {
			fragment.append( ' ' ).append( uniqueKey.getOptions() );
		}
		return fragment.toString();
	}
}
