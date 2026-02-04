/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * A {@link UniqueDelegate} which uses {@code alter table} commands to create and drop
 * the unique constraint. When possible, prefer {@link CreateTableUniqueDelegate}.
 *
 * @author Brett Meyer
 */
public class AlterTableUniqueDelegate implements UniqueDelegate {
	protected final Dialect dialect;

	/**
	 * @param dialect The dialect for which we are handling unique constraints
	 */
	public AlterTableUniqueDelegate(Dialect dialect ) {
		this.dialect = dialect;
	}

	static String constraintName(UniqueKey uniqueKey, Metadata metadata) {
		final String uniqueKeyName = uniqueKey.getName();
		final var database = metadata.getDatabase();
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
		return dialect.getAlterTableString( tableName( uniqueKey, context ) )
				+ " add constraint " + constraintName( uniqueKey, metadata )
				+ " " + uniqueConstraintSql( uniqueKey );
	}

	protected String uniqueConstraintSql(UniqueKey uniqueKey) {
		final var fragment = new StringBuilder();
		fragment.append( "unique (" );
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
		final String constraintName = constraintName( uniqueKey, metadata );
		final var command = new StringBuilder( dialect.getAlterTableString( tableName ) );
		command.append( ' ' );
		command.append( dialect.getDropUniqueKeyString() );
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			command.append( " if exists " );
			command.append( constraintName );
		}
		else if ( dialect.supportsIfExistsAfterConstraintName() ) {
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
