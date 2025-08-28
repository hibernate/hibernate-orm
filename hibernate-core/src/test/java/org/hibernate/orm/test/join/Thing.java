/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;


/**
 * @author Chris Jones
 */
public class Thing {
	private Employee salesperson;
	private String comments;

	/**
	 * @return Returns the salesperson.
	 */
	public Employee getSalesperson() {
		return salesperson;
	}
	/**
	 * @param salesperson The salesperson to set.
	 */
	public void setSalesperson(Employee salesperson) {
		this.salesperson = salesperson;
	}
	/**
	 * @return Returns the comments.
	 */
	public String getComments() {
		return comments;
	}
	/**
	 * @param comments The comments to set.
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	Long id;
	String name;
	String nameUpper;

	/**
	 * @return Returns the ID.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The ID to set.
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

	public String getNameUpper() {
		return nameUpper;
	}
	public void setNameUpper(String nameUpper) {
		this.nameUpper = nameUpper;
	}

}
