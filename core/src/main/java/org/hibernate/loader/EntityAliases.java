//$Id: EntityAliases.java 5699 2005-02-13 11:50:11Z oneovthafew $
package org.hibernate.loader;

import org.hibernate.persister.entity.Loadable;

/**
 * Metadata describing the SQL result set column aliases
 * for a particular entity.
 * 
 * @author Gavin King
 */
public interface EntityAliases {
	/**
	 * The result set column aliases for the primary key columns
	 */
	public String[] getSuffixedKeyAliases();
	/**
	 * The result set column aliases for the discriminator columns
	 */
	public String getSuffixedDiscriminatorAlias();
	/**
	 * The result set column aliases for the version columns
	 */
	public String[] getSuffixedVersionAliases();
	/**
	 * The result set column aliases for the property columns
	 */
	public String[][] getSuffixedPropertyAliases();
	/**
	 * The result set column aliases for the property columns of a subclass
	 */
	public String[][] getSuffixedPropertyAliases(Loadable persister);
	/**
	 * The result set column alias for the Oracle row id
	 */
	public String getRowIdAlias();

}
