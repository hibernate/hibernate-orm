/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

	/**
	 * Returns the suffix used to generate the aliases.
	 * @return the suffix used to generate the aliases.
	 */
	public String getSuffix();
}
