package org.hibernate.test.hqlfetchscroll;

import java.util.HashSet;
import java.util.Set;

public class Parent {
	
	// A numeric id must be the <id> field.  Some databases (Sybase, etc.)
	// require identifier columns in order to support scrollable results.
	private long id;
	private String name;
	private Set children = new HashSet();

	Parent() {
	}

	public Parent(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	public Set getChildren() {
		return children;
	}

	private void setChildren(Set children) {
		this.children = children;
	}

	public void addChild(Child child) {
		children.add( child );
	}

	public String toString() {
		return name;
	}
}
