/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Category {

	public static final String ROOT_CATEGORY = "/";
	public static final int ROOT_ID = 42;

	private long id;
	private String name;
	private List subcategories = new ArrayList();
	private Assignable assignable;
	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Returns the subcategories.
	 * @return List
	 */
	public List getSubcategories() {
		return subcategories;
	}

	/**
	 * Sets the subcategories.
	 * @param subcategories The subcategories to set
	 */
	public void setSubcategories(List subcategories) {
		this.subcategories = subcategories;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public Assignable getAssignable() {
		return assignable;
	}

	public void setAssignable(Assignable assignable) {
		this.assignable = assignable;
	}

	public String toString() {
		return id + ":" + name;
	}

}
