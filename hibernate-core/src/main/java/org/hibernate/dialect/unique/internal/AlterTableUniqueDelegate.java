/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Uses `alter table` commands to create and drop unique constraints.
///
/// @author Brett Meyer
/// @author Steve Ebersole
/// @since 8.0
@Internal
public class AlterTableUniqueDelegate implements UniqueDelegate {
	protected final Dialect dialect;

	/// Creates a delegate owned by the given Dialect.
	public AlterTableUniqueDelegate(Dialect dialect ) {
		this.dialect = dialect;
	}

	static String constraintName(UniqueKey uniqueKey, Database database) {
		final String uniqueKeyName = uniqueKey.getName();
		if ( uniqueKeyName == null ) {
			final List<Identifier> columnIdentifiers = new ArrayList<>();
			for ( var column : uniqueKey.getColumns() ) {
				columnIdentifiers.add( column.getNameIdentifier( database ) );
			}
			return NamingHelper.INSTANCE.generateHashedConstraintName("UK",
					uniqueKey.getTable().getNameIdentifier(), columnIdentifiers );
		}
		else {
			return database.getDialect().quote( uniqueKeyName );
		}
	}

	static String tableName(UniqueKey uniqueKey, SqlStringGenerationContext context) {
		return context.format( uniqueKey.getTable().getQualifiedTableName() );
	}

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getColumnDefinitionUniquenessFragment(Column column,
			SqlStringGenerationContext context) {
		return "";
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(Table table,
			SqlStringGenerationContext context) {
		return "";
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(
			UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		return dialect.getAlterTableSupport().alterTableCommand(
				tableName( uniqueKey, context ),
				dialect.getIfExistsSupport().alterTablePlacement()
		)
				+ " add constraint " + constraintName( uniqueKey, metadata.getDatabase() )
				+ " " + uniqueConstraintSql( uniqueKey );
	}

	protected String uniqueConstraintSql(UniqueKey uniqueKey) {
		final var fragment = new StringBuilder();
		fragment.append( "unique" );
		if ( uniqueKey.isNullsNotDistinct() && dialect.getUniqueDelegate().supportsNullsNotDistinct() ) {
			fragment.append( " nulls not distinct" );
		}
		fragment.append( " (" );
		boolean first = true;
		for ( var column : uniqueKey.getColumns() ) {
			if ( first ) {
				first = false;
			}
			else {
				fragment.append(", ");
			}
			fragment.append( column.getQuotedName( dialect ) );
			if ( uniqueKey.getColumnOrderMap().containsKey( column ) ) {
				fragment.append( " " ).append( uniqueKey.getColumnOrderMap().get( column ) );
			}
		}
		fragment.append( ')' );
		if ( isNotEmpty( uniqueKey.getOptions() ) ) {
			fragment.append( " " ).append( uniqueKey.getOptions() );
		}
		return fragment.toString();
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		final String tableName = tableName( uniqueKey, context );
		final String constraintName = constraintName( uniqueKey, metadata.getDatabase() );
		final var command = new StringBuilder( dialect.getAlterTableSupport().alterTableCommand(
				tableName,
				dialect.getIfExistsSupport().alterTablePlacement()
		) );
		command.append( ' ' );
		command.append( "drop constraint" );
		if ( dialect.getIfExistsSupport().dropConstraintPlacement() == ExistenceCheckPlacement.BEFORE_NAME ) {
			command.append( " if exists " );
			command.append( constraintName );
		}
		else if ( dialect.getIfExistsSupport().dropConstraintPlacement() == ExistenceCheckPlacement.AFTER_NAME ) {
			command.append( ' ' );
			command.append( constraintName );
			command.append( " if exists" );
		}
		else {
			command.append( ' ' );
			command.append( constraintName );
		}
		return command.toString();
	}

}
