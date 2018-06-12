/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Child.java 8043 2005-08-30 15:20:42Z oneovthafew $
package org.hibernate.test.onetomany2;
import java.util.Set;

public class Child {
	private Long id;
	private Parent parent;

	public Child(Parent parent) {
		setParent(parent);
	}
	
	Child() {}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Parent getParent() {
		return parent;
	}
	public void setParent(Parent parent) {
		this.parent = parent;
	}
	
}
