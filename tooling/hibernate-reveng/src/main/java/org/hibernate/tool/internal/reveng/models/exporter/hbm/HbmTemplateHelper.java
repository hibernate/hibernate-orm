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
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import jakarta.persistence.CascadeType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;

import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Provides hbm.xml-specific helper methods for FreeMarker templates.
 *
 * @author Koen Aers
 */
public class HbmTemplateHelper {

	private final TableMetadata table;

	HbmTemplateHelper(TableMetadata table) {
		this.table = table;
	}

	/**
	 * Returns the Hibernate type name for a column, using the stored
	 * hibernate type name if available, otherwise deriving it from
	 * the Java class.
	 */
	public String getHibernateTypeName(ColumnMetadata col) {
		String typeName = col.getHibernateTypeName();
		if (typeName != null) {
			return typeName;
		}
		return JavaClassToHibernateType.toHibernateType(col.getJavaType());
	}

	/**
	 * Returns the hbm.xml generator class name for a JPA GenerationType.
	 */
	public String getGeneratorClass(GenerationType generationType) {
		if (generationType == null) {
			return "assigned";
		}
		return switch (generationType) {
			case IDENTITY -> "identity";
			case SEQUENCE -> "sequence";
			case TABLE -> "table";
			case AUTO -> "native";
			case UUID -> "uuid2";
		};
	}

	/**
	 * Returns the hbm.xml tag name for the class element based on
	 * inheritance configuration.
	 */
	public String getClassTag() {
		if (table.getParentEntityClassName() == null) {
			return "class";
		}
		if (table.getInheritance() != null
				&& table.getInheritance().getStrategy() == InheritanceType.TABLE_PER_CLASS) {
			return "union-subclass";
		}
		if (table.getPrimaryKeyJoinColumnName() != null) {
			return "joined-subclass";
		}
		return "subclass";
	}

	/**
	 * Returns the fully qualified class name for the entity.
	 */
	public String getFullClassName() {
		String pkg = table.getEntityPackage();
		if (pkg != null && !pkg.isEmpty()) {
			return pkg + "." + table.getEntityClassName();
		}
		return table.getEntityClassName();
	}

	/**
	 * Returns the fully qualified parent class name if present.
	 */
	public String getFullParentClassName() {
		String pkg = table.getParentEntityPackage();
		String parent = table.getParentEntityClassName();
		if (pkg != null && !pkg.isEmpty()) {
			return pkg + "." + parent;
		}
		return parent;
	}

	/**
	 * Returns the hbm.xml column attributes string (not-null, unique,
	 * length, precision, scale).
	 */
	public String getColumnAttributes(ColumnMetadata col) {
		StringBuilder sb = new StringBuilder();
		if (!col.isNullable()) {
			sb.append("not-null=\"true\" ");
		}
		if (col.isUnique()) {
			sb.append("unique=\"true\" ");
		}
		if (col.getLength() > 0) {
			sb.append("length=\"").append(col.getLength()).append("\" ");
		}
		if (col.getPrecision() > 0) {
			sb.append("precision=\"").append(col.getPrecision()).append("\" ");
		}
		if (col.getScale() > 0) {
			sb.append("scale=\"").append(col.getScale()).append("\" ");
		}
		return sb.toString().stripTrailing();
	}

	/**
	 * Returns the hbm.xml cascade string for a JPA CascadeType array.
	 */
	public String getCascadeString(CascadeType[] cascadeTypes) {
		if (cascadeTypes == null || cascadeTypes.length == 0) {
			return "none";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cascadeTypes.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(toHbmCascade(cascadeTypes[i]));
		}
		return sb.toString();
	}

	/**
	 * Returns true if the table is a subclass (has a parent entity).
	 */
	public boolean isSubclass() {
		return table.getParentEntityClassName() != null;
	}

	/**
	 * Returns true if the table has a discriminator element needed
	 * (root entity with SINGLE_TABLE inheritance that has a discriminator column).
	 */
	public boolean needsDiscriminator() {
		if (table.getInheritance() == null) {
			return false;
		}
		return table.getInheritance().getDiscriminatorColumnName() != null;
	}

	/**
	 * Returns the hbm.xml discriminator type string.
	 */
	public String getDiscriminatorTypeName() {
		if (table.getInheritance() == null
				|| table.getInheritance().getDiscriminatorType() == null) {
			return "string";
		}
		return switch (table.getInheritance().getDiscriminatorType()) {
			case STRING -> "string";
			case CHAR -> "character";
			case INTEGER -> "integer";
		};
	}

	private String toHbmCascade(CascadeType cascadeType) {
		return switch (cascadeType) {
			case ALL -> "all";
			case PERSIST -> "persist";
			case MERGE -> "merge";
			case REMOVE -> "delete";
			case REFRESH -> "refresh";
			case DETACH -> "evict";
		};
	}
}
