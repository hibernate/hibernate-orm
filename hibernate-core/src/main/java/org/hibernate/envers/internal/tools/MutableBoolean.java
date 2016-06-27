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
public class MutableBoolean {
	private boolean value;

	public MutableBoolean() {
	}

	public MutableBoolean(boolean value) {
		this.value = value;
	}

	public boolean isSet() {
		return value;
	}

	public void set() {
		value = true;
	}

	public void unset() {
		value = false;
	}
}
