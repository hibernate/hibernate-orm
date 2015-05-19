/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.cid;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * Entity with composite id
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Parent {
	@EmbeddedId
	public ParentPk id;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Parent ) ) return false;

		final Parent parent = (Parent) o;

		if ( !id.equals( parent.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}
