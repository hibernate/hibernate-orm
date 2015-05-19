/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;


/**
 * @author Gail Badner
 */

public class PersonName {
	private String first;
	private String middle;
	private String last;

	public PersonName() {}

	public PersonName(String first, String middle, String state) {
		this.first = first;
		this.middle = middle;
		this.last = state;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getMiddle() {
		return middle;
	}

	public void setMiddle(String middle) {
		this.middle = middle;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		PersonName name = ( PersonName ) o;

		if ( first != null ? !first.equals( name.first ) : name.first != null ) {
			return false;
		}
		if ( middle != null ? !middle.equals( name.middle ) : name.middle != null ) {
			return false;
		}
		if ( last != null ? !last.equals( name.last ) : name.last != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = first != null ? first.hashCode() : 0;
		result = 31 * result + ( last != null ? last.hashCode() : 0 );
		return result;
	}
}
