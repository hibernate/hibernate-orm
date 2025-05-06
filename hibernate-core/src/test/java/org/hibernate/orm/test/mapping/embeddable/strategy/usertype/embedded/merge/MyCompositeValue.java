/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.merge;

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
