//$Id: Parent.java 6095 2005-03-17 05:57:29Z oneovthafew $
package org.hibernate.test.subselectfetch;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class Parent {
	private String name;
	private List children = new ArrayList();
	private List moreChildren = new ArrayList();
	
	Parent() {}
	public Parent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public List getChildren() {
		return children;
	}

	public void setChildren(List children) {
		this.children = children;
	}
	
	public List getMoreChildren() {
		return moreChildren;
	}
	
	public void setMoreChildren(List moreChildren) {
		this.moreChildren = moreChildren;
	}
	
	
}
