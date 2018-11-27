/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.tools;

import org.hibernate.annotations.Remove;

/**
 * A pair of objects.
 *
 * @param <T1>
 * @param <T2>
 *
 * @author Adam Warski (adamw@aster.pl)
 *
 * @deprecated (since 6.0), to be removed with no replacement.
 */
@Deprecated
@Remove
public class Pair<T1, T2> extends org.hibernate.envers.internal.tools.Pair<T1, T2> {
	private Pair(T1 obj1, T2 obj2) {
		super( obj1, obj2 );
	}

	public static <T1, T2> Pair<T1, T2> make(T1 obj1, T2 obj2) {
		return new Pair<>( obj1, obj2 );
	}
}
