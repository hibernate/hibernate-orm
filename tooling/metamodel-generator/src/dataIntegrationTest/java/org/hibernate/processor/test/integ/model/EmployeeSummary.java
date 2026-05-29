/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.model;

import jakarta.data.repository.Select;

/**
 * A record where {@code @Select} maps component names to
 * entity attribute names that differ.
 */
public record EmployeeSummary(
		@Select("name")
		String employeeName,
		@Select("department")
		String dept,
		Double salary) {
}
