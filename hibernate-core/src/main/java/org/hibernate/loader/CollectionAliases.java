/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.loader;


/**
 * Type definition of CollectionAliases.
 *
 * @author Steve Ebersole
 */
public interface CollectionAliases {
	/**
	 * Returns the suffixed result-set column-aliases for columns making
	 * up the key for this collection (i.e., its FK to its owner).
	 *
	 * @return The key result-set column aliases.
	 */
	public String[] getSuffixedKeyAliases();

	/**
	 * Returns the suffixed result-set column-aliases for the collumns
	 * making up the collection's index (map or list).
	 *
	 * @return The index result-set column aliases.
	 */
	public String[] getSuffixedIndexAliases();

	/**
	 * Returns the suffixed result-set column-aliases for the columns
	 * making up the collection's elements.
	 *
	 * @return The element result-set column aliases.
	 */
	public String[] getSuffixedElementAliases();

	/**
	 * Returns the suffixed result-set column-aliases for the column
	 * defining the collection's identifier (if any).
	 *
	 * @return The identifier result-set column aliases.
	 */
	public String getSuffixedIdentifierAlias();

	/**
	 * Returns the suffix used to unique the column aliases for this
	 * particular alias set.
	 *
	 * @return The uniqued column alias suffix.
	 */
	public String getSuffix();
}
