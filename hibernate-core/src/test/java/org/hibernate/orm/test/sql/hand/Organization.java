/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Gavin King
 */
public class Organization {
	private long id;
	private String name;
	private Collection<Employment> employments;

	public Organization(String name) {
		this.name = name;
		employments = new HashSet<>();
	}

	public Organization() {}

	/**
	 * @return Returns the employments.
	 */
	public Collection<Employment> getEmployments() {
		return employments;
	}
	/**
	 * @param employments The employments to set.
	 */
	public void setEmployments(Collection<Employment> employments) {
		this.employments = employments;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
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
