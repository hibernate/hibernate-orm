/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/// Includes unique declarations in `create table`, while falling back to
/// alter-table commands during schema migration.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@Internal
public class CreateTableUniqueDelegate extends AlterTableUniqueDelegate {

	public CreateTableUniqueDelegate( Dialect dialect ) {
		super( dialect );
	}

	// legacy model ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
		// It would be nice to detect that the column belongs to a named unique
		// constraint so that we could skip it here, but we don't have the Table.
		return context.isMigration()
				? super.getColumnDefinitionUniquenessFragment( column, context )
				: column.isUnique() ? " unique" : "";
	}

	@Override
	public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
		if ( context.isMigration() ) {
			return super.getTableCreationUniqueConstraintsFragment( table, context );
		}
		else {
			StringBuilder fragment = new StringBuilder();
			for ( UniqueKey uniqueKey : table.getUniqueKeys().values() ) {
				// If the unique key has a single column which is already marked unique,
				// then getColumnDefinitionUniquenessFragment() already handled it, and
				// so we don't need to bother creating a constraint. The only downside
				// to this is that if the user added a column marked unique=true to a
				// named unique constraint, then the name gets lost. Unfortunately, the
				// signature of getColumnDefinitionUniquenessFragment() doesn't let me
				// detect this case. (But that would be easy to fix!)
				if ( !isSingleColumnUnique( table, uniqueKey ) ) {
					appendUniqueConstraint( fragment, uniqueKey );
				}
			}
			return fragment.toString();
		}
	}

	protected void appendUniqueConstraint(StringBuilder fragment, UniqueKey uniqueKey) {
		fragment.append( ", " );
		if ( uniqueKey.isNameExplicit() ) {
			fragment.append( "constraint " ).append( uniqueKey.getName() ).append( " " );
		}
		fragment.append( uniqueConstraintSql( uniqueKey ) );
	}

	private static boolean isSingleColumnUnique(Table table, UniqueKey uniqueKey) {
		if ( uniqueKey.getColumns().size() == 1)  {
			// Since columns are created on demand in IndexBinder.createColumn,
			// we also have to check if the "real" column is unique to be safe
			final Column uniqueKeyColumn = uniqueKey.getColumn(0);
			if ( uniqueKeyColumn.isUnique() ) {
				return true;
			}
			else {
				final Column column = table.getColumn( uniqueKeyColumn );
				return column != null && column.isUnique();
			}
		}
		else {
			return false;
		}
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
		return context.isMigration() ? super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context ) : "";
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
		return context.isMigration() ? super.getAlterTableToDropUniqueKeyCommand(uniqueKey, metadata, context) : "";
	}

}
