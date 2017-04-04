/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
