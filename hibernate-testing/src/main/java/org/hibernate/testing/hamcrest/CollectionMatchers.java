/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.hamcrest;

import java.util.Collection;

import org.hamcrest.Matcher;

/**
 * @author Steve Ebersole
 */
public class CollectionMatchers {
	private static final CollectionEmptinessMatcher IS_EMPTY = new CollectionEmptinessMatcher( true );
	private static final CollectionEmptinessMatcher IS_NOT_EMPTY = new CollectionEmptinessMatcher( false );

	public static Matcher<Collection> isEmpty() {
		return IS_EMPTY;
	}

	public static Matcher<Collection> isNotEmpty() {
		return IS_NOT_EMPTY;
	}

	public static Matcher<Collection> hasSize(int size) {
		return new CollectionSizeMatcher( size );
	}
}
