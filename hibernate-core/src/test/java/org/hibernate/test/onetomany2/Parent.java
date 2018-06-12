/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Child.java 8043 2005-08-30 15:20:42Z oneovthafew $
package org.hibernate.test.onetomany2;
import java.util.HashSet;
import java.util.Set;

public class Parent {
	private Long id;
	private Long collectionKey;
	private Set<Child> children = new HashSet<>();

	public Parent(Long collectionKey) {
		setCollectionKey(collectionKey);
	}
	
	Parent() {}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getCollectionKey() {
		return collectionKey;
	}
	public void setCollectionKey(Long collectionKey) {
		this.collectionKey = collectionKey;
	}
	public Set<Child> getChildren() {
		return children;
	}
	public void setChildren(Set<Child> children) {
		this.children = children;
	}
	
}
