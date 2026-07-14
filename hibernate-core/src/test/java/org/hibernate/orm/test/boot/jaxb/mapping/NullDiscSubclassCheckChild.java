/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

public class NullDiscSubclassCheckChild extends NullDiscSubclassCheckBase {
	private String requiredProp;

	public String getRequiredProp() {
		return requiredProp;
	}

	public void setRequiredProp(String requiredProp) {
		this.requiredProp = requiredProp;
	}
}
