package org.hibernate.test.reattachment;

import java.util.Set;
import java.util.HashSet;

/**
 * Parent entity
 *
 * @author Steve Ebersole
 */
public class Parent {
	private String name;
	private Parent other;
	private Set children = new HashSet();

	public Parent() {
	}

	public Parent(String name) {
		this.name = name;
	}

	public Parent(String name, Parent other) {
		this.name = name;
		this.other = other;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Parent getOther() {
		return other;
	}

	public void setOther(Parent other) {
		this.other = other;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}
