/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.pack.war;
import jakarta.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
public class IncrementListener {
	private static int increment;

	public static int getIncrement() {
		return increment;
	}

	public static void reset() {
		increment = 0;
	}

	@PrePersist
	public void increment(Object entity) {
		increment++;
	}
}
