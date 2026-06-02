/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.model;

/**
 * A record containing a subset of entity attributes.
 * Record component names match the entity attribute names.
 */
public record EmployeeInfo(
		Long id,
		String name,
		String department) {
}
