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
package org.hibernate.tool.internal.reveng.models.exporter.doc;

/**
 * Represents a database column for the table documentation templates.
 * Extracted from {@code @Column} annotations on {@code FieldDetails}.
 * <p>
 * Templates access properties like {@code column.name},
 * {@code column.nullable}, {@code column.unique}, {@code column.comment}.
 *
 * @author Koen Aers
 */
public class TableColumnDocInfo {

	private final String name;
	private final String javaTypeName;
	private final boolean nullable;
	private final boolean unique;
	private final String comment;
	private final int length;
	private final int precision;
	private final int scale;

	public TableColumnDocInfo(String name, String javaTypeName,
							  boolean nullable, boolean unique,
							  String comment,
							  int length, int precision, int scale) {
		this.name = name;
		this.javaTypeName = javaTypeName;
		this.nullable = nullable;
		this.unique = unique;
		this.comment = comment;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
	}

	public String getName() {
		return name;
	}

	public String getJavaTypeName() {
		return javaTypeName;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isUnique() {
		return unique;
	}

	public String getComment() {
		return comment;
	}

	public int getLength() {
		return length;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}
}
