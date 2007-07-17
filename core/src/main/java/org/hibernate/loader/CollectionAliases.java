// $Id: CollectionAliases.java 6828 2005-05-19 07:28:59Z steveebersole $
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
