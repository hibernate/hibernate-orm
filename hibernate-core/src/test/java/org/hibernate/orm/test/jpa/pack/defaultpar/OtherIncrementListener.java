/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.pack.defaultpar;


/**
 * @author Emmanuel Bernard
 */
public class OtherIncrementListener {
	private static int increment;

	public static int getIncrement() {
		return OtherIncrementListener.increment;
	}

	public static void reset() {
		increment = 0;
	}

	public void increment(Object entity) {
		OtherIncrementListener.increment++;
	}
}
