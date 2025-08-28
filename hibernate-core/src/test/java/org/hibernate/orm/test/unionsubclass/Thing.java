/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass;


/**
 * @author Gavin King
 */
public class Thing {
	private long id;
	private String description;
	private Being owner;
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
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
	 * @return Returns the owner.
	 */
	public Being getOwner() {
		return owner;
	}
	/**
	 * @param owner The owner to set.
	 */
	public void setOwner(Being owner) {
		this.owner = owner;
	}
}
