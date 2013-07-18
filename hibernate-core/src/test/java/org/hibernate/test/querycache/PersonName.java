/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
