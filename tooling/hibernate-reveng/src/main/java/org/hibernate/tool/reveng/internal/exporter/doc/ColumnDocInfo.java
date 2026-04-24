/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.doc;

/**
 * Represents column metadata for documentation templates.
 * Adapts {@code @Column} annotation data into a shape compatible
 * with the existing documentation FreeMarker templates.
 *
 * @author Koen Aers
 */
public class ColumnDocInfo {

	private final String name;
	private final boolean formula;

	public ColumnDocInfo(String name, boolean formula) {
		this.name = name;
		this.formula = formula;
	}

	public String getName() {
		return name;
	}

	public boolean isFormula() {
		return formula;
	}
}
