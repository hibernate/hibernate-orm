/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor.usertype;

public class MyCompositeValue {
	protected Long longValue;
	protected String stringValue;

	public MyCompositeValue() {
	}

	public MyCompositeValue(Long longValue, String stringValue) {
		this.longValue = longValue;
		this.stringValue = stringValue;
	}

	public Long longValue() {
		return longValue;
	}

	public String stringValue() {
		return stringValue;
	}
}
