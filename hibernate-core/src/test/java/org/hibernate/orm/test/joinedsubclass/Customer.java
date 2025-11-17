/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.joinedsubclass;


/**
 * @author Gavin King
 */
public class Customer extends Person {
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
}
