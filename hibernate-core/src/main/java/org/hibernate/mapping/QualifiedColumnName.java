/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Represents a fully qualified column name for uniqueness checks.
 */
public record QualifiedColumnName(
		Identifier catalogName,
		Identifier schemaName,
		Identifier tableName,
		Identifier columnName
) {}
