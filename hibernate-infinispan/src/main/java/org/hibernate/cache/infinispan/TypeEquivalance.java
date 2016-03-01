/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan;

import org.hibernate.type.Type;
import org.infinispan.commons.equivalence.Equivalence;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TypeEquivalance implements Equivalence<Object> {
	private final Type type;

	public TypeEquivalance(Type type) {
		this.type = type;
	}

	@Override
	public int hashCode(Object o) {
		if (type.getReturnedClass().isInstance(o)) {
			return type.getHashCode(o);
		}
		else {
			return o != null ? o.hashCode() : 0;
		}
	}

	@Override
	public boolean equals(Object x, Object y) {
		Class<?> typeClass = type.getReturnedClass();
		if (typeClass.isInstance(x) && typeClass.isInstance(y)) {
			return type.isEqual(x, y);
		}
		else {
			return (x == y) || (x != null && x.equals(y));
		}
	}

	@Override
	public String toString(Object o) {
		return String.valueOf(o);
	}

	@Override
	public boolean isComparable(Object o) {
		return true; // cannot guess from the type
	}

	@Override
	public int compare(Object x, Object y) {
		Class<?> typeClass = type.getReturnedClass();
		if (typeClass.isInstance(x) && typeClass.isInstance(y)) {
			return type.compare(x, y);
		}
		else {
			return 0;
		}
	}
}
