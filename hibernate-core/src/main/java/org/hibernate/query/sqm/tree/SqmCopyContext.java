/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.IdentityHashMap;

/**
 *
 */
public interface SqmCopyContext {

	<T> T getCopy(T original);

	<T> T registerCopy(T original, T copy);

	static SqmCopyContext simpleContext() {
		final IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
		return new SqmCopyContext() {
			@Override
			public <T> T getCopy(T original) {
				//noinspection unchecked
				return (T) map.get( original );
			}

			@Override
			public <T> T registerCopy(T original, T copy) {
				final Object old = map.put( original, copy );
				if ( old != null ) {
					throw new IllegalArgumentException( "Already registered a copy: " + old );
				}
				return copy;
			}

		};
	}
}
