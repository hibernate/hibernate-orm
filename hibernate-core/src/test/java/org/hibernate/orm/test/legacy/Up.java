/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Up implements Serializable {

	private String id1;
	private long id2;

	public String getId1() {
		return id1;
	}

	public long getId2() {
		return id2;
	}

	public void setId1(String string) {
		id1 = string;
	}

	public void setId2(long l) {
		id2 = l;
	}

	public boolean equals(Object other) {
		if ( !(other instanceof Up) ) return false;
		Up that = (Up) other;
		return this.id1.equals(that.id1) && this.id2==that.id2;
	}
	public int hashCode() {
		return id1.hashCode();
	}

}
