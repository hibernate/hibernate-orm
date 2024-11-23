/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;
import java.util.HashMap;
import java.util.Map;

/**
 * todo: describe Parent
 *
 * @author Steve Ebersole
 */
public class Parent {
	private String name;
	private Map children = new HashMap();

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

	public Map getChildren() {
		return children;
	}

	public void setChildren(Map children) {
		this.children = children;
	}

	public Child addChild(String name) {
		Child child = new Child( name );
		addChild( child );
		return child;
	}

	public void addChild(Child child) {
		child.setParent( this );
		getChildren().put( child.getName(), child );
	}
}
