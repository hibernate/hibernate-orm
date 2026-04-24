/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.doc;

import java.util.List;

/**
 * Wraps column metadata for a property, providing the same shape
 * as Hibernate's {@code Value} for documentation FreeMarker templates.
 * Templates access {@code property.getValue().selectables} and
 * {@code property.getValue().getColumnSpan()}.
 *
 * @author Koen Aers
 */
public class ValueDocInfo {

	private final List<ColumnDocInfo> selectables;

	public ValueDocInfo(List<ColumnDocInfo> selectables) {
		this.selectables = selectables;
	}

	public List<ColumnDocInfo> getSelectables() {
		return selectables;
	}

	public int getColumnSpan() {
		return selectables.size();
	}
}
