/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

public class NullDiscSubclassCheckOther extends NullDiscSubclassCheckChild {
	private String otherProp;

	public String getOtherProp() {
		return otherProp;
	}

	public void setOtherProp(String otherProp) {
		this.otherProp = otherProp;
	}
}
