/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

class OrderBy {
	String fieldName;
	boolean descending;
	boolean ignoreCase;

	public OrderBy(String fieldName, boolean descending, boolean ignoreCase) {
		this.fieldName = fieldName;
		this.descending = descending;
		this.ignoreCase = ignoreCase;
	}
}
