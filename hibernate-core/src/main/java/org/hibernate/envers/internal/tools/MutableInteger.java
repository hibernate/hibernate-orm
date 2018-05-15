/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MutableInteger {
	private int value;

	public MutableInteger() {
	}

	public MutableInteger(int value) {
		this.value = value;
	}

	public MutableInteger deepCopy() {
		return new MutableInteger( value );
	}

	public int getAndIncrease() {
		return value++;
	}

	public int get() {
		return value;
	}

	public void set(int value) {
		this.value = value;
	}

	public void increase() {
		++value;
	}
}
