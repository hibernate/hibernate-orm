/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.persistence.Id;

public class Parent {

	private Integer id;
	private String name;


	private List<Child> children = new ArrayList<>();

	public Parent() {
	}

	public Parent(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
		for ( Iterator<Child> i = children.iterator(); i.hasNext(); ) {
			if ( i.next() == null ) {
				i.remove();
			}
		}
	}

	public void addChild(Child child) {
		this.children.add( child );
		child.setParent( this );
	}
}
