/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;
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
