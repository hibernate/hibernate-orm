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
