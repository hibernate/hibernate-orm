/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.ops;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Node {
	private String name;
	private Node parent;
	private Set children = new HashSet();
	private String description;

	public Node() {
	}

	public Node(String name) {
		this.name = name;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public Node addChild(Node child) {
		children.add( child );
		child.setParent( this );
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
