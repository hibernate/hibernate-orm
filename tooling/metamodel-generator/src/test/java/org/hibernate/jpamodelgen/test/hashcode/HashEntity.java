/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.hashcode;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class HashEntity {
	private long id;

	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Boolean goo() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		HashEntity that = (HashEntity) o;

		if ( id != that.id ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return (int) ( id ^ ( id >>> 32 ) );
	}
}


