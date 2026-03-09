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
package org.hibernate.tool.internal.reveng.models.metadata;

import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;

/**
 * Represents metadata for a database column.
 *
 * @author Koen Aers
 */
public class ColumnMetadata {
	private final String columnName;
	private final String fieldName;
	private final Class<?> javaType;
	private boolean nullable;
	private int length;
	private int precision;
	private int scale;
	private boolean primaryKey;
	private boolean autoIncrement;
	private boolean version;
	private FetchType basicFetchType;
	private Boolean basicOptional;
	private TemporalType temporalType;
	private boolean lob;
	private String comment;
	private GenerationType generationType;
	private boolean unique;

	public ColumnMetadata(String columnName, String fieldName, Class<?> javaType) {
		this.columnName = columnName;
		this.fieldName = fieldName;
		this.javaType = javaType;
		this.nullable = true;
	}

	public ColumnMetadata primaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
		this.nullable = false;
		return this;
	}

	public ColumnMetadata autoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
		return this;
	}

	public ColumnMetadata nullable(boolean nullable) {
		this.nullable = nullable;
		return this;
	}

	public ColumnMetadata length(int length) {
		this.length = length;
		return this;
	}

	public ColumnMetadata precision(int precision) {
		this.precision = precision;
		return this;
	}

	public ColumnMetadata scale(int scale) {
		this.scale = scale;
		return this;
	}

	public ColumnMetadata version(boolean version) {
		this.version = version;
		return this;
	}

	public ColumnMetadata basicFetch(FetchType fetchType) {
		this.basicFetchType = fetchType;
		return this;
	}

	public ColumnMetadata basicOptional(boolean optional) {
		this.basicOptional = optional;
		return this;
	}

	public ColumnMetadata temporal(TemporalType temporalType) {
		this.temporalType = temporalType;
		return this;
	}

	public ColumnMetadata lob(boolean lob) {
		this.lob = lob;
		return this;
	}

	public ColumnMetadata comment(String comment) {
		this.comment = comment;
		return this;
	}

	public ColumnMetadata generationType(GenerationType generationType) {
		this.generationType = generationType;
		return this;
	}

	public ColumnMetadata unique(boolean unique) {
		this.unique = unique;
		return this;
	}

	// Getters
	public String getColumnName() { return columnName; }
	public String getFieldName() { return fieldName; }
	public Class<?> getJavaType() { return javaType; }
	public boolean isNullable() { return nullable; }
	public int getLength() { return length; }
	public int getPrecision() { return precision; }
	public int getScale() { return scale; }
	public boolean isPrimaryKey() { return primaryKey; }
	public boolean isAutoIncrement() { return autoIncrement; }
	public boolean isVersion() { return version; }
	public FetchType getBasicFetchType() { return basicFetchType; }
	public boolean isBasicOptionalSet() { return basicOptional != null; }
	public boolean isBasicOptional() { return basicOptional != null ? basicOptional : true; }
	public TemporalType getTemporalType() { return temporalType; }
	public boolean isLob() { return lob; }
	public String getComment() { return comment; }
	public GenerationType getGenerationType() { return generationType; }
	public boolean isUnique() { return unique; }
}
