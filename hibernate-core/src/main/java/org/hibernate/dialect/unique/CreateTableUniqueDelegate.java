/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * A {@link UniqueDelegate} which includes the unique constraint in the {@code create table}
 * statement, except when called during schema migration.
 *
 * @author Gavin King
 */
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
				// named unique constraint, then the name gets lost. Unfortunately the
				// signature of getColumnDefinitionUniquenessFragment() doesn't let me
				// detect this case. (But that would be easy to fix!)
				if ( !isSingleColumnUnique( uniqueKey ) ) {
					fragment.append( ", " );
					if ( uniqueKey.isNameExplicit() ) {
						fragment.append( "constraint " ).append( uniqueKey.getName() ).append( " " );
					}
					fragment.append( uniqueConstraintSql( uniqueKey ) );
				}
			}
			return fragment.toString();
		}
	}

	private static boolean isSingleColumnUnique(UniqueKey uniqueKey) {
		return uniqueKey.getColumns().size() == 1
			&& uniqueKey.getColumn(0).isUnique();
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
