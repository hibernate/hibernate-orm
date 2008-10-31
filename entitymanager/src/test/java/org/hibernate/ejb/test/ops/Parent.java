//$Id$
package org.hibernate.ejb.test.ops;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard
 */
public class Parent {
	private String name;
	private List children = new ArrayList();

	Parent() {
	}

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
