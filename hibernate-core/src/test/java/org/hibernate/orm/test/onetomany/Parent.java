/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author gavin
 */
public class Parent {
	private Long id;
	private String name;
	private Collection children = new HashSet();
	/**
	 * @return Returns the children.
	 */
	public Collection getChildren() {
		return children;
	}
	/**
	 * @param children The children to set.
	 */
	public void setChildren(Collection children) {
		this.children = children;
	}
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
