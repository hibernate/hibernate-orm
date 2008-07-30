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
