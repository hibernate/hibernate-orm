/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondelete.joined;

/**
 * @author Andrea Boriero
 */
public class Child extends Parent {

	private String childData;

	public String getChildData() {
		return childData;
	}

	public void setChildData(String childData) {
		this.childData = childData;
	}
}
