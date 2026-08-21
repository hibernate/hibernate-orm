/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import static java.util.Arrays.binarySearch;

public class EmbeddableHelper {
	public static int[] determineMappingIndex(String[] sortedNames, String[] names) {
		final int[] index = new int[sortedNames.length];
		int i = 0;
		for ( String name : names ) {
			final int mappingIndex = binarySearch( sortedNames, name );
			if ( mappingIndex != -1 ) {
				index[i++] = mappingIndex;
			}
		}
		return index;
	}

	public static boolean resolveIndex(String[] sortedComponentNames, String[] componentNames, int[] index) {
		boolean hasGaps = false;
		for ( int i = 0; i < componentNames.length; i++ ) {
			final int newIndex = binarySearch( sortedComponentNames, componentNames[i] );
			index[i] = newIndex;
			hasGaps = hasGaps || newIndex < 0;
		}
		return hasGaps;
	}
}
