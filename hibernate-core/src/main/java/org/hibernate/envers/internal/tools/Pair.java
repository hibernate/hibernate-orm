/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

/**
 * A pair of objects.
 *
 * @param <T1> The first object.
 * @param <T2> The second object.
 *
 * @author Chris Cranford
 *
 * @deprecated since 6.0 with no replacement.
 */
@Deprecated
public class Pair<T1, T2> {
	private final T1 obj1;
	private final T2 obj2;

	protected Pair(T1 obj1, T2 obj2) {
		this.obj1 = obj1;
		this.obj2 = obj2;
	}

	public T1 getFirst() {
		return obj1;
	}

	public T2 getSecond() {
		return obj2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Pair) ) {
			return false;
		}

		final Pair pair = (Pair) o;

		if ( obj1 != null ? !obj1.equals( pair.obj1 ) : pair.obj1 != null ) {
			return false;
		}
		if ( obj2 != null ? !obj2.equals( pair.obj2 ) : pair.obj2 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = (obj1 != null ? obj1.hashCode() : 0);
		result = 31 * result + (obj2 != null ? obj2.hashCode() : 0);
		return result;
	}

	public static <T1, T2> Pair<T1, T2> make(T1 obj1, T2 obj2) {
		return new Pair<>( obj1, obj2 );
	}
}
