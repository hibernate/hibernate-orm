/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.reader;

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
