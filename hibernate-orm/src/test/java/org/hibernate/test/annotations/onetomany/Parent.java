/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.BatchSize;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Parent implements Serializable {
	@Id
	public ParentPk id;
	public int age;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
	@BatchSize(size = 5)
	@javax.persistence.OrderBy("favoriteSuperhero asc, favoriteSinger desc")
	public Set<Child> children;

	public int hashCode() {
		//a NPE can occurs, but I don't expect hashcode to be used beforeQuery pk is set
		return id.hashCode();
	}

	public boolean equals(Object obj) {
		//a NPE can occurs, but I don't expect equals to be used beforeQuery pk is set
		if ( obj != null && obj instanceof Parent ) {
			return id.equals( ( (Parent) obj ).id );
		}
		else {
			return false;
		}
	}

	public void addChild(Child child) {
		if ( children == null ) {
			children = new HashSet();
		}
		child.parent = this;
		children.add( child );
	}
}
