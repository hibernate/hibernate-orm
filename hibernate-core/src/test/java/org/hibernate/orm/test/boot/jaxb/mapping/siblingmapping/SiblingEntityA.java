/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.siblingmapping;

public class SiblingEntityA extends SiblingBase {
	private int id;
	private SiblingRelated related;

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public SiblingRelated getRelated() {
		return related;
	}

	@Override
	public void setRelated(SiblingRelated related) {
		this.related = related;
	}
}
