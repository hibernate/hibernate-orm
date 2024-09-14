/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.classification;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
//tag::collections-name-ex[]
@Embeddable
@Access( AccessType.FIELD )
public class Name implements Comparable<Name> {
	private String first;
	private String last;

	// ...
//end::collections-name-ex[]
	private Name() {
		// used by Hibernate
	}

	public Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	@Override
	public int compareTo(Name o) {
		return NameComparator.comparator.compare( this, o );
	}
//tag::collections-name-ex[]
}
//end::collections-name-ex[]
