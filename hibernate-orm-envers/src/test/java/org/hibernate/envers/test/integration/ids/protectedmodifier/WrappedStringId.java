/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.protectedmodifier;

import java.io.Serializable;
import javax.persistence.Embeddable;

@Embeddable
public class WrappedStringId implements Serializable {
	String id;

	@SuppressWarnings("unused")
	protected WrappedStringId() {
		// For JPA. Protected access modifier is essential in terms of unit test.
	}

	public WrappedStringId(String id) {
		this.id = id;
	}

	public String toString() {
		return id;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		WrappedStringId that = (WrappedStringId) o;
		return !(id != null ? !id.equals( that.id ) : that.id != null);
	}

	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
