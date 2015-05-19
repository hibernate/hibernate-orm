/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import java.util.Collections;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * CollectionAliases which handles the logic of selecting user provided aliases (via return-property),
 * before using the default aliases.
 *
 * @author Steve Ebersole
 * @author Max Rydahl Andersen
 */
public class GeneratedCollectionAliases implements CollectionAliases {
	private final String suffix;
	private final String[] keyAliases;
	private final String[] indexAliases;
	private final String[] elementAliases;
	private final String identifierAlias;
	private Map userProvidedAliases;

	public GeneratedCollectionAliases(Map userProvidedAliases, CollectionPersister persister, String suffix) {
		this.suffix = suffix;
		this.userProvidedAliases = userProvidedAliases;

		this.keyAliases = getUserProvidedAliases(
				"key",
				persister.getKeyColumnAliases( suffix )
		);

		this.indexAliases = getUserProvidedAliases(
				"index",
				persister.getIndexColumnAliases( suffix )
		);

		this.elementAliases = getUserProvidedAliases(
				"element",
				persister.getElementColumnAliases( suffix )
		);

		this.identifierAlias = getUserProvidedAlias(
				"id",
				persister.getIdentifierColumnAlias( suffix )
		);
	}

	public GeneratedCollectionAliases(CollectionPersister persister, String string) {
		this( Collections.EMPTY_MAP, persister, string );
	}

	/**
	 * Returns the suffixed result-set column-aliases for columns making up the key for this collection (i.e., its FK to
	 * its owner).
	 *
	 * @return The key result-set column aliases.
	 */
	public String[] getSuffixedKeyAliases() {
		return keyAliases;
	}

	/**
	 * Returns the suffixed result-set column-aliases for the collumns making up the collection's index (map or list).
	 *
	 * @return The index result-set column aliases.
	 */
	public String[] getSuffixedIndexAliases() {
		return indexAliases;
	}

	/**
	 * Returns the suffixed result-set column-aliases for the columns making up the collection's elements.
	 *
	 * @return The element result-set column aliases.
	 */
	public String[] getSuffixedElementAliases() {
		return elementAliases;
	}

	/**
	 * Returns the suffixed result-set column-aliases for the column defining the collection's identifier (if any).
	 *
	 * @return The identifier result-set column aliases.
	 */
	public String getSuffixedIdentifierAlias() {
		return identifierAlias;
	}

	/**
	 * Returns the suffix used to unique the column aliases for this particular alias set.
	 *
	 * @return The uniqued column alias suffix.
	 */
	public String getSuffix() {
		return suffix;
	}

	@Override
	public String toString() {
		return super.toString() + " [suffix=" + suffix +
				", suffixedKeyAliases=[" + join( keyAliases ) +
				"], suffixedIndexAliases=[" + join( indexAliases ) +
				"], suffixedElementAliases=[" + join( elementAliases ) +
				"], suffixedIdentifierAlias=[" + identifierAlias + "]]";
	}

	private String join(String[] aliases) {
		if ( aliases == null ) {
			return null;
		}

		return StringHelper.join( ", ", aliases );
	}

	private String[] getUserProvidedAliases(String propertyPath, String[] defaultAliases) {
		String[] result = (String[]) userProvidedAliases.get( propertyPath );
		if ( result == null ) {
			return defaultAliases;
		}
		else {
			return result;
		}
	}

	private String getUserProvidedAlias(String propertyPath, String defaultAlias) {
		String[] columns = (String[]) userProvidedAliases.get( propertyPath );
		if ( columns == null ) {
			return defaultAlias;
		}
		else {
			return columns[0];
		}
	}

}
