/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for an embeddable class.
 *
 * @author Koen Aers
 */
public class EmbeddableDescriptor {
	private final String className;
	private final String packageName;
	private final List<ColumnDescriptor> columns = new ArrayList<>();
	private boolean idClass;

	public EmbeddableDescriptor(String className, String packageName) {
		this.className = className;
		this.packageName = packageName;
	}

	public EmbeddableDescriptor addColumn(ColumnDescriptor column) {
		this.columns.add(column);
		return this;
	}

	public EmbeddableDescriptor idClass(boolean idClass) {
		this.idClass = idClass;
		return this;
	}

	// Getters
	public String getClassName() { return className; }
	public String getPackageName() { return packageName; }
	public List<ColumnDescriptor> getColumns() { return columns; }
	public boolean isIdClass() { return idClass; }
}
