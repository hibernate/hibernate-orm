/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import java.util.Objects;

public class ModelWithSelfChildrenId implements Serializable {

	private String string;
	private int integer;


	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public int getInteger() {
		return integer;
	}

	public void setInteger(int integer) {
		this.integer = integer;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ModelWithSelfChildrenId that = (ModelWithSelfChildrenId) o;
		return integer == that.integer && Objects.equals( string, that.string );
	}

	@Override
	public int hashCode() {
		return Objects.hash( string, integer );
	}

	@Override
	public String toString() {
		return "Id{" +
				"string='" + string + '\'' +
				", integer=" + integer +
				'}';
	}
}
