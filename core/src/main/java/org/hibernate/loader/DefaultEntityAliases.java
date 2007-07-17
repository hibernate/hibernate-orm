package org.hibernate.loader;

import java.util.Map;

import org.hibernate.persister.entity.Loadable;
import org.hibernate.util.CollectionHelper;

/**
 * EntityAliases which handles the logic of selecting user provided aliases (via return-property),
 * before using the default aliases. 
 *
 * @author max
 *
 */
public class DefaultEntityAliases implements EntityAliases {

	private final String[] suffixedKeyColumns;
	private final String[] suffixedVersionColumn;
	private final String[][] suffixedPropertyColumns;
	private final String suffixedDiscriminatorColumn;
	private final String suffix;
	private final String rowIdAlias;
	private final Map userProvidedAliases;	

	public DefaultEntityAliases(Loadable persister, String suffix) {
		this(CollectionHelper.EMPTY_MAP, persister, suffix);
	}
	
	/**
	 * Calculate and cache select-clause suffixes.
	 * @param map 
	 */
	public DefaultEntityAliases(Map userProvidedAliases, Loadable persister, String suffix) {
		this.suffix = suffix;
		this.userProvidedAliases = userProvidedAliases;
		
		String[] keyColumnsCandidates = getUserProvidedAliases(
				persister.getIdentifierPropertyName(), 
				(String[]) null
			); 
		if (keyColumnsCandidates==null) {
			suffixedKeyColumns = getUserProvidedAliases(
					"id", 
					getIdentifierAliases(persister, suffix)
				);
		} 
		else {
			suffixedKeyColumns = keyColumnsCandidates;
		}
		intern(suffixedKeyColumns);
		
		suffixedPropertyColumns = getSuffixedPropertyAliases(persister);
		suffixedDiscriminatorColumn = getUserProvidedAlias(
				"class", 
				getDiscriminatorAlias(persister, suffix)
			);
		if ( persister.isVersioned() ) { 
			suffixedVersionColumn = suffixedPropertyColumns[ persister.getVersionProperty() ];
		}
		else {
			suffixedVersionColumn = null;
		}
		rowIdAlias = Loadable.ROWID_ALIAS + suffix; // TODO: not visible to the user!
	}

	protected String getDiscriminatorAlias(Loadable persister, String suffix) {
		return persister.getDiscriminatorAlias(suffix);
	}

	protected String[] getIdentifierAliases(Loadable persister, String suffix) {
		return persister.getIdentifierAliases(suffix);
	}
	
	protected String[] getPropertyAliases(Loadable persister, int j) {
		return persister.getPropertyAliases(suffix, j);
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
	
	public String[][] getSuffixedPropertyAliases(Loadable persister) {
		int size = persister.getPropertyNames().length;
		String[][] suffixedPropertyAliases = new String[size][];
		for ( int j = 0; j < size; j++ ) {
			suffixedPropertyAliases[j] = getUserProvidedAliases(
					persister.getPropertyNames()[j],
					getPropertyAliases(persister, j)
				);
			intern( suffixedPropertyAliases[j] );
		}			
		return suffixedPropertyAliases;
	}

	public String[] getSuffixedVersionAliases() {
		return suffixedVersionColumn;
	}

	public String[][] getSuffixedPropertyAliases() {
		return suffixedPropertyColumns;
	}

	public String getSuffixedDiscriminatorAlias() {
		return suffixedDiscriminatorColumn;
	}

	public String[] getSuffixedKeyAliases() {
		return suffixedKeyColumns;
	}

	public String getRowIdAlias() {
		return rowIdAlias;
	}
	
	private static void intern(String[] strings) {
		for (int i=0; i<strings.length; i++ ) {
			strings[i] = strings[i].intern();
		}
	}

}
