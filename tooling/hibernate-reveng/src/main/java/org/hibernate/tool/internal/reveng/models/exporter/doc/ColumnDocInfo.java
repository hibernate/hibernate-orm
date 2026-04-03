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
