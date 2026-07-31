/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.siblingmapping;

public abstract class SiblingBase {
	private int id;
	private String name;
	private SiblingRelated related;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SiblingRelated getRelated() {
		return related;
	}

	public void setRelated(SiblingRelated related) {
		this.related = related;
	}
}
