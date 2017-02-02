/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.unique;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.UniqueKey;

/**
 * Dialect-level delegate in charge of applying "uniqueness" to a column.  Uniqueness can be defined
 * in 1 of 3 ways:<ol>
 *     <li>
 *         Add a unique constraint via separate alter table statements.  See {@link #getAlterTableToAddUniqueKeyCommand}.
 *         Also, see {@link #getAlterTableToDropUniqueKeyCommand}
 *     </li>
 *     <li>
 *			Add a unique constraint via dialect-specific syntax in table create statement.  See
 *			{@link #getTableCreationUniqueConstraintsFragment}
 *     </li>
 *     <li>
 *         Add "unique" syntax to the column itself.  See {@link #getColumnDefinitionUniquenessFragment}
 *     </li>
 * </ol>
 *
 * #1 & #2 are preferred, if possible; #3 should be solely a fall-back.
 * 
 * See HHH-7797.
 * 
 * @author Brett Meyer
 */
public interface UniqueDelegate {
	/**
	 * Get the fragment that can be used to make a column unique as part of its column definition.
	 * <p/>
	 * This is intended for dialects which do not support unique constraints
	 * 
	 * @param column The column to which to apply the unique
	 *
	 * @return The fragment (usually "unique"), empty string indicates the uniqueness will be indicated using a
	 * different approach
	 */
	public String getColumnDefinitionUniquenessFragment(org.hibernate.mapping.Column column);

	/**
	 * Get the fragment that can be used to apply unique constraints as part of table creation.  The implementation
	 * should iterate over the {@link org.hibernate.mapping.UniqueKey} instances for the given table (see
	 * {@link org.hibernate.mapping.Table#getUniqueKeyIterator()} and generate the whole fragment for all
	 * unique keys
	 * <p/>
	 * Intended for Dialects which support unique constraint definitions, but just not in separate ALTER statements.
	 *
	 * @param table The table for which to generate the unique constraints fragment
	 *
	 * @return The fragment, typically in the form {@code ", unique(col1, col2), unique( col20)"}.  NOTE: The leading
	 * comma is important!
	 */
	public String getTableCreationUniqueConstraintsFragment(org.hibernate.mapping.Table table);

	/**
	 * Get the SQL ALTER TABLE command to be used to create the given UniqueKey.
	 *
	 * @param uniqueKey The UniqueKey instance.  Contains all information about the columns
	 * @param metadata Access to the bootstrap mapping information
	 *
	 * @return The ALTER TABLE command
	 */
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata);

	/**
	 * Get the SQL ALTER TABLE command to be used to drop the given UniqueKey.
	 *
	 * @param uniqueKey The UniqueKey instance.  Contains all information about the columns
	 * @param metadata Access to the bootstrap mapping information
	 *
	 * @return The ALTER TABLE command
	 */
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata);

}
