/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

import org.hibernate.internal.util.compare.EqualsHelper;

/**
 * A triple of objects.
 *
 * @param <T1>
 * @param <T2>
 * @param <T3>
 *
 * @author Adam Warski (adamw@aster.pl)
 */
public class Triple<T1, T2, T3> {
	private T1 obj1;
	private T2 obj2;
	private T3 obj3;

	public Triple(T1 obj1, T2 obj2, T3 obj3) {
		this.obj1 = obj1;
		this.obj2 = obj2;
		this.obj3 = obj3;
	}

	public T1 getFirst() {
		return obj1;
	}

	public T2 getSecond() {
		return obj2;
	}

	public T3 getThird() {
		return obj3;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Triple) ) {
			return false;
		}

		final Triple other = (Triple) o;
		return EqualsHelper.equals( obj1, other.obj1 )
				&& EqualsHelper.equals( obj2, other.obj2 )
				&& EqualsHelper.equals( obj3, other.obj3 );
	}

	@Override
	public int hashCode() {
		int result;
		result = (obj1 != null ? obj1.hashCode() : 0);
		result = 31 * result + (obj2 != null ? obj2.hashCode() : 0);
		result = 31 * result + (obj3 != null ? obj3.hashCode() : 0);
		return result;
	}

	public static <T1, T2, T3> Triple<T1, T2, T3> make(T1 obj1, T2 obj2, T3 obj3) {
		return new Triple<T1, T2, T3>( obj1, obj2, obj3 );
	}
}
