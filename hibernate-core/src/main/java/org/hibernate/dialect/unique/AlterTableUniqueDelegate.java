/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

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
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		final String tableName = context.format( uniqueKey.getTable().getQualifiedTableName() );
		final String constraintName = dialect.quote( uniqueKey.getName() );
		return dialect.getAlterTableString( tableName )
				+ " add constraint " + constraintName + " " + uniqueConstraintSql( uniqueKey );
	}

	protected String uniqueConstraintSql(UniqueKey uniqueKey) {
		final StringBuilder fragment = new StringBuilder();
		fragment.append( "unique (" );
		boolean first = true;
		for ( Column column : uniqueKey.getColumns() ) {
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
		final String tableName = context.format( uniqueKey.getTable().getQualifiedTableName() );
		final StringBuilder command = new StringBuilder( dialect.getAlterTableString(tableName) );
		command.append( ' ' );
		command.append( dialect.getDropUniqueKeyString() );
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			command.append( " if exists " );
			command.append( dialect.quote( uniqueKey.getName() ) );
		}
		else if ( dialect.supportsIfExistsAfterConstraintName() ) {
			command.append( ' ' );
			command.append( dialect.quote( uniqueKey.getName() ) );
			command.append( " if exists" );
		}
		else {
			command.append( ' ' );
			command.append( dialect.quote( uniqueKey.getName() ) );
		}
		return command.toString();
	}

}
