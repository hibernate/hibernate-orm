/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

/**
 * @author Gavin King
 */
public class Down extends Up {

	private long value;

	public long getValue() {
		return value;
	}

	public void setValue(long l) {
		value = l;
	}

}
