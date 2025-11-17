/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.tracker;

import java.util.Arrays;

import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * small low memory class to keep track of the number of elements in a collection
 *
 * @author Ståle W. Pedersen
 */
public final class SimpleCollectionTracker implements CollectionTracker {

	private String[] names;
	private int[] sizes;

	public SimpleCollectionTracker() {
		names = ArrayHelper.EMPTY_STRING_ARRAY;
		sizes = ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public void add(String name, int size) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( names[i].equals( name ) ) {
				sizes[i] = size;
				return;
			}
		}
		names = Arrays.copyOf( names, names.length + 1 );
		names[names.length - 1] = name;
		sizes = Arrays.copyOf( sizes, sizes.length + 1 );
		sizes[sizes.length - 1] = size;
	}

	@Override
	public int getSize(String name) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( name.equals( names[i] ) ) {
				return sizes[i];
			}
		}
		return -1;
	}

}
