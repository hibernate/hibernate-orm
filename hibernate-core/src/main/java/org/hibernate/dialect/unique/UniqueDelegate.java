/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.dialect.unique;

import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.UniqueKey;

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
	 * Get the fragment that can be used to make a column unique as part of its column definition.
	 * <p/>
	 * This is intended for dialects which do not support unique constraints
	 *
	 * @param column The column to which to apply the unique
	 *
	 * @return The fragment (usually "unique"), empty string indicates the uniqueness will be indicated using a
	 * different approach
	 */
	public String getColumnDefinitionUniquenessFragment(Column column);

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
	public String getTableCreationUniqueConstraintsFragment(Table table);

	/**
	 * Get the SQL ALTER TABLE command to be used to create the given UniqueKey.
	 *
	 * @param uniqueKey The UniqueKey instance.  Contains all information about the columns
	 * @param defaultCatalog The default catalog
	 * @param defaultSchema The default schema
	 *
	 * @return The ALTER TABLE command
	 */
	public String getAlterTableToAddUniqueKeyCommand(
			org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog,
			String defaultSchema);

	/**
	 * Get the SQL ALTER TABLE command to be used to create the given UniqueKey.
	 *
	 * @param uniqueKey The UniqueKey instance.  Contains all information about the columns, as well as
	 * schema/catalog
	 *
	 * @return The ALTER TABLE command
	 */
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey);

	/**
	 * Get the SQL ALTER TABLE command to be used to drop the given UniqueKey.
	 *
	 * @param uniqueKey The UniqueKey instance.  Contains all information about the columns
	 * @param defaultCatalog The default catalog
	 * @param defaultSchema The default schema
	 *
	 * @return The ALTER TABLE command
	 */
	public String getAlterTableToDropUniqueKeyCommand(
			org.hibernate.mapping.UniqueKey uniqueKey,
			String defaultCatalog, String defaultSchema);

	/**
	 * Get the SQL ALTER TABLE command to be used to drop the given UniqueKey.
	 *
	 * @param uniqueKey The UniqueKey instance.  Contains all information about the columns, as well as
	 * schema/catalog
	 *
	 * @return The ALTER TABLE command
	 */
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey);
}
