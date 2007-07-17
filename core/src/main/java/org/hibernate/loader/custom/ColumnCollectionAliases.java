package org.hibernate.loader.custom;

import java.util.Map;

import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.util.StringHelper;

/**
 * CollectionAliases that uses columnnames instead of generated aliases.
 * Aliases can still be overwritten via <return-property>
 *  
 * @author Max Rydahl Andersen
 *
 */
public class ColumnCollectionAliases implements CollectionAliases {
	private final String[] keyAliases;
	private final String[] indexAliases;
	private final String[] elementAliases;
	private final String identifierAlias;
	private Map userProvidedAliases;
	
	
	public ColumnCollectionAliases(Map userProvidedAliases, SQLLoadableCollection persister) {
		this.userProvidedAliases = userProvidedAliases;

		this.keyAliases = getUserProvidedAliases(
				"key", 
				persister.getKeyColumnNames()
			);

		this.indexAliases = getUserProvidedAliases(
				"index",
				persister.getIndexColumnNames()
				);
		
		this.elementAliases = getUserProvidedAliases( "element", 
				persister.getElementColumnNames()
				);
				
		this.identifierAlias = getUserProvidedAlias( "id", 
				persister.getIdentifierColumnName()
				);
	
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
		return "";
	}

	public String toString() {
		return super.toString() + " [ suffixedKeyAliases=[" + join( keyAliases ) +
		        "], suffixedIndexAliases=[" + join( indexAliases ) +
		        "], suffixedElementAliases=[" + join( elementAliases ) +
		        "], suffixedIdentifierAlias=[" + identifierAlias + "]]";
	}

	private String join(String[] aliases) {
		if ( aliases == null) return null;

		return StringHelper.join( ", ", aliases );
	}
	
	private String[] getUserProvidedAliases(String propertyPath, String[] defaultAliases) {
		String[] result = (String[]) userProvidedAliases.get(propertyPath);
		if (result==null) {
			return defaultAliases;			
		} 
		else {
			return result;
		}
	}

	private String getUserProvidedAlias(String propertyPath, String defaultAlias) {
		String[] columns = (String[]) userProvidedAliases.get(propertyPath);
		if (columns==null) {
			return defaultAlias;
		} 
		else {
			return columns[0];
		}
	}

}
