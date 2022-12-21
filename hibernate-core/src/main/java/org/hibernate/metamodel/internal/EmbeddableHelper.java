/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.Arrays;

public class EmbeddableHelper {
	public static int[] determinePropertyMappingIndex(String[] propertyNames, String[] componentNames) {
		final int[] index = new int[propertyNames.length];
		int i = 0;
		for ( String componentName : componentNames ) {
			final int mappingIndex = Arrays.binarySearch( propertyNames, componentName );
			if ( mappingIndex != -1 ) {
				index[i++] = mappingIndex;
			}
		}
		return index;
	}

	public static boolean resolveIndex(String[] sortedComponentNames, String[] componentNames, int[] index) {
		boolean hasGaps = false;
		for ( int i = 0; i < componentNames.length; i++ ) {
			final int newIndex = Arrays.binarySearch( sortedComponentNames, componentNames[i] );
			index[i] = newIndex;
			hasGaps = hasGaps || newIndex < 0;
		}

		return hasGaps;
	}
}
