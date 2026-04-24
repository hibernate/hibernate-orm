/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.reader;

/**
 * Intermediate data holder for raw foreign key information
 * as read from {@code RevengDialect.getExportedKeys()}.
 * One instance per FK column row. Composite FKs share the same
 * {@code fkName} but have different {@code keySeq} values.
 *
 * @author Koen Aers
 */
public record RawForeignKeyInfo(
		String fkName,
		String fkTableName,
		String fkTableCatalog,
		String fkTableSchema,
		String fkColumnName,
		String pkColumnName,
		String referencedTableName,
		String referencedCatalog,
		String referencedSchema,
		int keySeq) { }
