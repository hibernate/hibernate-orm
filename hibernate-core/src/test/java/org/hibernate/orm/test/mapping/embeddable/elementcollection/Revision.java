/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.elementcollection;

/**
 * Embeddable with a single {@code value} attribute, mapped via orm.xml.
 */
public class Revision {

	private String value;

	public Revision() {
	}

	public Revision(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
