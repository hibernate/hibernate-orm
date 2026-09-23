/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.relational.naming.spi.PhysicalName;

/**
 * Represents a fully qualified column name for uniqueness checks.
 */
public record QualifiedColumnName(
		ColumnContainer columnContainer,
		PhysicalName columnName
) {}
