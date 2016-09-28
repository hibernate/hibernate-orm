/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections.streams;

/**
 * @author Steve Ebersole
 */
public class StreamUtils {
	public static StingArrayCollector toStringArray() {
		return StingArrayCollector.INSTANCE;
	}

	public static <T> GenericArrayCollector<T> toArray(Class<T> collectedType) {
		return new GenericArrayCollector<>( collectedType );
	}

}
