package org.hibernate.test.reattachment;
import java.util.HashSet;
import java.util.Set;

/**
 * Parent entity
 *
 * @author Steve Ebersole
 */
public class Parent {
	private String name;
	private Set children = new HashSet();

	public Parent() {
	}

	public Parent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}
