/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class QuerySpacesHelper {
	/**
	 * Singleton access
	 */
	public static final QuerySpacesHelper INSTANCE = new QuerySpacesHelper();

	private QuerySpacesHelper() {
	}

	public String[] toStringArray(Set spacesSet) {
		return (String[]) spacesSet.toArray( new String[0] );
	}

	public Set<String> toStringSet(String[] spacesArray) {
		final HashSet<String> set = new HashSet<>();
		Collections.addAll( set, spacesArray );
		return set;
	}

	public Set<Serializable> toSerializableSet(String[] spacesArray) {
		final HashSet<Serializable> set = new HashSet<>();
		Collections.addAll( set, spacesArray );
		return set;
	}
}
