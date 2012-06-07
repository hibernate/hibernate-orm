package org.hibernate.test.unidir;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class Parent {
	private String name;
	private List children = new ArrayList();
	Parent() {}
	public Parent(String name) {
		this.name = name;
	}
	public List getChildren() {
		return children;
	}
	public void setChildren(List children) {
		this.children = children;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}