/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.model;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MySubclassEntity extends MyEntity {
	private String someSubProperty;

	public String getSomeSubProperty() {
		return someSubProperty;
	}

	public void setSomeSubProperty(String someSubProperty) {
		this.someSubProperty = someSubProperty;
	}
}
